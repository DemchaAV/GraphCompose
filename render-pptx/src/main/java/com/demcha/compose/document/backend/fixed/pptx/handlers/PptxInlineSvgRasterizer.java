package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.layout.payloads.ParagraphSvgSpan;
import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Transparent raster fallback for inline SVG details DrawingML cannot express
 * faithfully: arbitrary clip paths, exact point dashes, line joins, and art
 * that relies on the SVG viewBox clipping off-canvas geometry.
 */
final class PptxInlineSvgRasterizer {

    private static final double PIXELS_PER_POINT = 4.0;
    private static final int MAX_DIMENSION = 2048;
    private static final double BOUNDS_EPSILON = 0.0001;

    private PptxInlineSvgRasterizer() {
    }

    static boolean requiresRaster(ParagraphSvgSpan span) {
        if (span.width() <= 0 || span.height() <= 0) {
            return false;
        }
        Rectangle2D viewBox = new Rectangle2D.Double(0, 0, span.width(), span.height());
        for (ResolvedSvgLayer layer : span.layers()) {
            if (layer.clip() != null && !layer.clip().isEmpty()) {
                return true;
            }
            if (!layer.dashPattern().isSolid()
                    || layer.lineCap() != DocumentLineCap.BUTT
                    || layer.lineJoin() != DocumentLineJoin.MITER) {
                return true;
            }
            Path2D path = PptxInlineGeometry.path(layer.segments(), viewBox);
            Rectangle2D bounds = path.getBounds2D();
            double strokePad = PptxShapeStyle.drawable(layer.stroke())
                    ? layer.stroke().width() / 2.0 : 0.0;
            if (bounds.getMinX() - strokePad < -BOUNDS_EPSILON
                    || bounds.getMinY() - strokePad < -BOUNDS_EPSILON
                    || bounds.getMaxX() + strokePad > span.width() + BOUNDS_EPSILON
                    || bounds.getMaxY() + strokePad > span.height() + BOUNDS_EPSILON) {
                return true;
            }
        }
        return false;
    }

    static ImageData rasterize(ParagraphSvgSpan span) {
        if (span.width() <= 0 || span.height() <= 0) {
            throw new IllegalArgumentException("inline SVG dimensions must be positive");
        }
        double scale = Math.min(PIXELS_PER_POINT,
                MAX_DIMENSION / Math.max(span.width(), span.height()));
        int pixelWidth = Math.max(1, (int) Math.ceil(span.width() * scale));
        int pixelHeight = Math.max(1, (int) Math.ceil(span.height() * scale));
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.scale(pixelWidth / span.width(), pixelHeight / span.height());
            Rectangle2D viewBox = new Rectangle2D.Double(0, 0, span.width(), span.height());
            graphics.clip(viewBox);
            for (ResolvedSvgLayer layer : span.layers()) {
                paintLayer(graphics, layer, viewBox);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return ImageData.create(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode inline SVG fallback", exception);
        }
    }

    private static void paintLayer(Graphics2D target,
                                   ResolvedSvgLayer layer,
                                   Rectangle2D viewBox) {
        Graphics2D graphics = (Graphics2D) target.create();
        try {
            if (layer.clip() != null && !layer.clip().isEmpty()) {
                graphics.clip(PptxInlineGeometry.path(layer.clip(), viewBox));
            }
            Path2D path = PptxInlineGeometry.path(layer.segments(), viewBox);
            Color fill = layer.fillPaint() == null
                    ? layer.fillColor() : layer.fillPaint().primaryColor().color();
            if (fill != null) {
                graphics.setColor(fill);
                graphics.fill(path);
            }

            Stroke stroke = resolvedStroke(layer);
            if (PptxShapeStyle.drawable(stroke)) {
                graphics.setColor(stroke.strokeColor().color());
                graphics.setStroke(awtStroke(stroke.width(), layer));
                graphics.draw(path);
            }
        } finally {
            graphics.dispose();
        }
    }

    private static Stroke resolvedStroke(ResolvedSvgLayer layer) {
        if (layer.stroke() == null || layer.strokePaint() == null) {
            return layer.stroke();
        }
        return new Stroke(layer.strokePaint().primaryColor().color(), layer.stroke().width());
    }

    static BasicStroke awtStroke(double width, ResolvedSvgLayer layer) {
        int cap = switch (layer.lineCap()) {
            case BUTT -> BasicStroke.CAP_BUTT;
            case ROUND -> BasicStroke.CAP_ROUND;
            case SQUARE -> BasicStroke.CAP_SQUARE;
        };
        int join = switch (layer.lineJoin()) {
            case MITER -> BasicStroke.JOIN_MITER;
            case ROUND -> BasicStroke.JOIN_ROUND;
            case BEVEL -> BasicStroke.JOIN_BEVEL;
        };
        float[] dash = layer.dashPattern().isSolid() ? null
                : toFloatArray(layer.dashPattern().segments());
        return new BasicStroke((float) width, cap, join, 10.0f, dash, 0.0f);
    }

    private static float[] toFloatArray(java.util.List<Double> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}
