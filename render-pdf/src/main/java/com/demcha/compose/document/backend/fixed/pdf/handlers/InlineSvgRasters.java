package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.layout.payloads.ParagraphSvgSpan;
import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * An inline SVG icon as a transparent picture.
 *
 * <p>For the backends that place a picture where an icon sits in a line: the PPTX backend,
 * for the details DrawingML cannot express — arbitrary clips, exact dashes, joins, art that
 * relies on the view box clipping it — and the Word export, which has no drawing in a line of
 * text at all. Both draw the same resolved layers the same way, so an icon cannot look one
 * way on a slide and another in a document.</p>
 *
 * <p>The picture is four pixels a point, no larger than 2048 on a side, and encoded in
 * memory: {@code ImageIO.write} to a stream caches through a temporary file by default.</p>
 */
@Internal
public final class InlineSvgRasters {

    private static final double PIXELS_PER_POINT = 4.0;
    private static final int MAX_DIMENSION = 2048;

    private InlineSvgRasters() {
    }

    /**
     * Draws an icon's resolved layers into a transparent PNG at its size.
     *
     * @param span the icon as the layout resolved it
     * @return the PNG
     * @throws IllegalArgumentException when the icon has no area
     */
    public static ImageData rasterize(ParagraphSvgSpan span) {
        return rasterize(span.layers(), span.width(), span.height());
    }

    /**
     * Draws resolved layers into a transparent PNG of the given size in points.
     *
     * @param layers the layers, geometry normalised to the unit box
     * @param width  the width in points
     * @param height the height in points
     * @return the PNG
     * @throws IllegalArgumentException when the size has no area
     */
    public static ImageData rasterize(List<ResolvedSvgLayer> layers, double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("inline SVG dimensions must be positive");
        }
        double scale = Math.min(PIXELS_PER_POINT, MAX_DIMENSION / Math.max(width, height));
        int pixelWidth = Math.max(1, (int) Math.ceil(width * scale));
        int pixelHeight = Math.max(1, (int) Math.ceil(height * scale));
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.scale(pixelWidth / width, pixelHeight / height);
            Rectangle2D viewBox = new Rectangle2D.Double(0, 0, width, height);
            graphics.clip(viewBox);
            for (ResolvedSvgLayer layer : layers) {
                paintLayer(graphics, layer, viewBox);
            }
        } finally {
            graphics.dispose();
        }
        try {
            return ImageData.create(png(image));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode inline SVG fallback", exception);
        }
    }

    /**
     * Normalised path segments scaled into a box, y growing downwards.
     *
     * @param segments the segments, in the unit box with y growing upwards
     * @param box      the box to scale them into
     * @return the path
     */
    public static Path2D path(List<DocumentPathSegment> segments, Rectangle2D box) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                path.moveTo(x(box, move.x()), y(box, move.y()));
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                path.lineTo(x(box, line.x()), y(box, line.y()));
            } else if (segment instanceof DocumentPathSegment.CubicTo curve) {
                path.curveTo(
                        x(box, curve.control1X()), y(box, curve.control1Y()),
                        x(box, curve.control2X()), y(box, curve.control2Y()),
                        x(box, curve.x()), y(box, curve.y()));
            } else if (segment instanceof DocumentPathSegment.Close) {
                path.closePath();
            }
        }
        return path;
    }

    /**
     * The Java2D stroke a layer draws with.
     *
     * @param width the stroke width in points
     * @param layer the layer, for its cap, join and dash
     * @return the stroke
     */
    public static BasicStroke awtStroke(double width, ResolvedSvgLayer layer) {
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
        float[] dash = layer.dashPattern().isSolid() ? null : toFloatArray(layer.dashPattern().segments());
        return new BasicStroke((float) width, cap, join, 10.0f, dash, 0.0f);
    }

    private static void paintLayer(Graphics2D target, ResolvedSvgLayer layer, Rectangle2D viewBox) {
        Graphics2D graphics = (Graphics2D) target.create();
        try {
            if (layer.clip() != null && !layer.clip().isEmpty()) {
                graphics.clip(path(layer.clip(), viewBox));
            }
            Path2D path = path(layer.segments(), viewBox);
            Color fill = layer.fillPaint() == null
                    ? layer.fillColor() : layer.fillPaint().primaryColor().color();
            if (fill != null) {
                graphics.setColor(fill);
                graphics.fill(path);
            }
            Stroke stroke = resolvedStroke(layer);
            if (drawable(stroke)) {
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

    private static boolean drawable(Stroke stroke) {
        return stroke != null
               && stroke.strokeColor() != null
               && stroke.strokeColor().color() != null
               && stroke.width() > 0;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(image);
            out.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static float[] toFloatArray(List<Double> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    private static double x(Rectangle2D box, double normalized) {
        return box.getX() + normalized * box.getWidth();
    }

    private static double y(Rectangle2D box, double normalized) {
        return box.getY() + (1.0 - normalized) * box.getHeight();
    }
}
