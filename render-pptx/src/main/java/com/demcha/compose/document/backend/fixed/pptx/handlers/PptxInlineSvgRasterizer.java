package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pdf.handlers.InlineSvgRasters;
import com.demcha.compose.document.layout.payloads.ParagraphSvgSpan;
import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.engine.components.content.ImageData;

import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Transparent raster fallback for inline SVG details DrawingML cannot express
 * faithfully: arbitrary clip paths, exact point dashes, line joins, and art
 * that relies on the SVG viewBox clipping off-canvas geometry.
 *
 * <p>Deciding when to fall back is this backend's; drawing the picture is shared with the
 * Word export through {@link InlineSvgRasters}, so an icon is the same picture in both.</p>
 */
final class PptxInlineSvgRasterizer {

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
        return InlineSvgRasters.rasterize(span);
    }

    static BasicStroke awtStroke(double width, ResolvedSvgLayer layer) {
        return InlineSvgRasters.awtStroke(width, layer);
    }
}
