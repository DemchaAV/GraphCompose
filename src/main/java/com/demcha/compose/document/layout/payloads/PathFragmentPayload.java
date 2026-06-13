package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
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
 * PDF payload for a resolved vector-path fragment (curved chart strokes,
 * decorative design shapes, imported icon paths). The normalized segments are
 * scaled to the placed fragment's size by the render handler, which emits
 * native PDF line and cubic-Bézier operators.
 *
 * <p>Solid paints never travel here — the definition normalises them into
 * {@code fillColor} / the stroke colour, so only true gradients reach the
 * handler and flat-colour output stays byte-identical.</p>
 *
 * @param segments        normalized path segments, starting with a move-to
 * @param fillColor       optional fill color (non-zero winding rule)
 * @param fillPaint       optional gradient fill; wins over {@code fillColor}
 * @param stroke          optional stroke
 * @param strokePaint     optional gradient stroke paint; the stroke still
 *                        supplies the width
 * @param linkOptions     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 * @param dashPattern     dash pattern for the stroke;
 *                        {@link DocumentDashPattern#NONE} is solid
 * @param lineCap         stroke end-cap style; {@code BUTT} is the PDF default
 * @param lineJoin        stroke corner style; {@code MITER} is the PDF default
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PathFragmentPayload(
        List<DocumentPathSegment> segments,
        Color fillColor,
        DocumentPaint fillPaint,
        Stroke stroke,
        DocumentPaint strokePaint,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentDashPattern dashPattern,
        DocumentLineCap lineCap,
        DocumentLineJoin lineJoin
) implements PdfSemanticFragmentPayload {
    /**
     * Copies the segment list defensively and normalizes dash and stroke
     * style defaults.
     */
    public PathFragmentPayload {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
        lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
        lineJoin = lineJoin == null ? DocumentLineJoin.MITER : lineJoin;
    }
}
