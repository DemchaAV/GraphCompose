package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
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
 * @param segments        normalized path segments, starting with a move-to
 * @param fillColor       optional fill color (non-zero winding rule)
 * @param stroke          optional stroke
 * @param linkOptions     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PathFragmentPayload(
        List<DocumentPathSegment> segments,
        Color fillColor,
        Stroke stroke,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
    /**
     * Copies the segment list defensively.
     */
    public PathFragmentPayload {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
    }
}
