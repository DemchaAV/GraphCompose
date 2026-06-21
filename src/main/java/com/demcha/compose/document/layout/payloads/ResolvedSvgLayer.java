package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * One resolved paint layer of a {@link ParagraphSvgSpan}: normalized vector
 * geometry whose fill colour and stroke are already lowered to AWT / engine
 * primitives, ready for the PDF backend. Solid paints are collapsed to flat
 * colours during resolution so the inline render path matches the block path
 * fragment for fragment; only true gradients travel as {@link DocumentPaint}.
 *
 * <p>The geometry is normalized to the icon's unit box; the render handler
 * scales it to the span's measured {@code width} / {@code height}, exactly like
 * {@link PathFragmentPayload}.</p>
 *
 * @param segments    normalized path segments, starting with a move-to
 * @param fillColor   optional resolved flat fill colour
 * @param fillPaint   optional gradient fill; wins over {@code fillColor}
 * @param stroke      optional resolved outline stroke (width in points)
 * @param strokePaint optional gradient stroke paint; the stroke still supplies the width
 * @param dashPattern stroke dash pattern; {@link DocumentDashPattern#NONE} is solid
 * @param lineCap     stroke end-cap style; {@code BUTT} is the PDF default
 * @param lineJoin    stroke corner style; {@code MITER} is the PDF default
 * @param clip        optional clip region (normalized to the same box); the layer
 *                    paints only inside it. {@code null} means no clipping.
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record ResolvedSvgLayer(
        List<DocumentPathSegment> segments,
        Color fillColor,
        DocumentPaint fillPaint,
        Stroke stroke,
        DocumentPaint strokePaint,
        DocumentDashPattern dashPattern,
        DocumentLineCap lineCap,
        DocumentLineJoin lineJoin,
        List<DocumentPathSegment> clip
) {
    /**
     * Copies the segment lists defensively and normalizes dash and stroke style
     * defaults.
     */
    public ResolvedSvgLayer {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
        clip = clip == null ? null : List.copyOf(clip);
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
        lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
        lineJoin = lineJoin == null ? DocumentLineJoin.MITER : lineJoin;
    }
}
