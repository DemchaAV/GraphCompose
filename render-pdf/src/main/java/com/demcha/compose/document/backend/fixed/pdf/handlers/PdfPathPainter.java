package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * Shared kernel that paints one resolved vector-path layer into a PDF content
 * stream with native line / cubic-Bézier operators, scaling normalized segments
 * to a target {@code (x, y, width, height)} box.
 *
 * <p>Gradient fills clip to the path and paint a native shading; gradient
 * strokes set a shading-pattern stroking colour (pattern type 2). Flat-colour
 * paths bypass both and take the exact pre-gradient code path, byte for byte.
 * Extracted from {@code PdfPathFragmentRenderHandler} so both the block path
 * fragment and the inline SVG-icon span paint identically.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
final class PdfPathPainter {

    private PdfPathPainter() {
    }

    /**
     * Paints {@code segments} (normalized to the unit box) at the target box,
     * applying flat colours and/or gradients for fill and stroke.
     *
     * @param stream      content stream to draw into
     * @param environment render environment (for page resources on gradient strokes)
     * @param pageIndex   target page index
     * @param x           target box left edge in PDF points
     * @param y           target box bottom edge in PDF points
     * @param width       target box width in points
     * @param height      target box height in points
     * @param segments    normalized path segments, starting with a move-to
     * @param fillColor   optional flat fill colour
     * @param fillPaint   optional gradient fill; wins over {@code fillColor}
     * @param stroke      optional stroke (supplies the width)
     * @param strokePaint optional gradient stroke paint
     * @param dashPattern stroke dash pattern; {@link DocumentDashPattern#NONE} is solid
     * @param lineCap     stroke end-cap style
     * @param lineJoin    stroke corner style
     * @param clip        optional clip region (normalized to the same box); the
     *                    paint is confined to it. {@code null} means no clipping.
     * @throws IOException if the content stream rejects an operator
     */
    static void paintPath(PDPageContentStream stream,
                          PdfRenderEnvironment environment,
                          int pageIndex,
                          float x,
                          float y,
                          float width,
                          float height,
                          List<DocumentPathSegment> segments,
                          Color fillColor,
                          DocumentPaint fillPaint,
                          Stroke stroke,
                          DocumentPaint strokePaint,
                          DocumentDashPattern dashPattern,
                          DocumentLineCap lineCap,
                          DocumentLineJoin lineJoin,
                          List<DocumentPathSegment> clip) throws IOException {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (clip == null || clip.isEmpty()) {
            fillStrokePath(stream, environment, pageIndex, x, y, width, height, segments,
                    fillColor, fillPaint, stroke, strokePaint, dashPattern, lineCap, lineJoin);
            return;
        }
        // Confine the paint to the clip region (e.g. a Noto emoji clips its
        // shadow/detail layers to the icon silhouette).
        stream.saveGraphicsState();
        try {
            PdfShapeGeometry.addPathSegments(stream, x, y, width, height, clip);
            stream.clip();
            fillStrokePath(stream, environment, pageIndex, x, y, width, height, segments,
                    fillColor, fillPaint, stroke, strokePaint, dashPattern, lineCap, lineJoin);
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void fillStrokePath(PDPageContentStream stream,
                                       PdfRenderEnvironment environment,
                                       int pageIndex,
                                       float x,
                                       float y,
                                       float width,
                                       float height,
                                       List<DocumentPathSegment> segments,
                                       Color fillColor,
                                       DocumentPaint fillPaint,
                                       Stroke stroke,
                                       DocumentPaint strokePaint,
                                       DocumentDashPattern dashPattern,
                                       DocumentLineCap lineCap,
                                       DocumentLineJoin lineJoin) throws IOException {
        if (fillPaint == null && strokePaint == null) {
            PdfShapeGeometry.fillAndStrokePath(stream, fillColor, stroke,
                    dashPattern, lineCap, lineJoin,
                    s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, segments));
            return;
        }

        // Gradient route: fill and stroke are separate passes because each
        // may independently be a flat colour or a shading.
        stream.saveGraphicsState();
        try {
            if (fillPaint != null) {
                // Clip in a nested state so the clip never leaks into the
                // stroke pass (mirrors the shape handler).
                stream.saveGraphicsState();
                try {
                    PdfShapeGeometry.addPathSegments(stream, x, y, width, height, segments);
                    stream.clip();
                    stream.shadingFill(PdfShadingSupport.build(fillPaint, x, y, width, height));
                } finally {
                    stream.restoreGraphicsState();
                }
            } else if (fillColor != null) {
                PdfShapeGeometry.fillAndStrokePath(stream, fillColor, null, null,
                        s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, segments));
            }

            boolean hasStrokeWidth = stroke != null && stroke.width() > 0;
            if (strokePaint != null && hasStrokeWidth) {
                // The shading pattern must register on the SAME physical page the
                // stroke draws on. In a multi-section document that page is offset
                // from the section-local pageIndex, so resolve it through the
                // environment's page-offset mapping (identity for a single section).
                PDResources resources = environment.documentPage(pageIndex).getResources();
                stream.setStrokingColor(PdfShadingSupport.strokePattern(
                        strokePaint, resources, x, y, width, height));
                stream.setLineWidth((float) stroke.width());
                PdfShapeGeometry.applyDashPattern(stream, dashPattern);
                PdfShapeGeometry.applyStrokeStyle(stream, lineCap, lineJoin);
                PdfShapeGeometry.addPathSegments(stream, x, y, width, height, segments);
                stream.stroke();
            } else if (hasStrokeWidth && stroke.strokeColor() != null) {
                PdfShapeGeometry.fillAndStrokePath(stream, null, stroke,
                        dashPattern, lineCap, lineJoin,
                        s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, segments));
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
