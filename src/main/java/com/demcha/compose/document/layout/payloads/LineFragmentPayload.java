package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.engine.components.content.shape.Stroke;

/**
 * PDF payload for a resolved line fragment.
 *
 * @param stroke          line stroke
 * @param startX          line start x offset inside the fragment
 * @param startY          line start y offset inside the fragment
 * @param endX            line end x offset inside the fragment
 * @param endY            line end y offset inside the fragment
 * @param linkOptions     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 * @param dashPattern     dash pattern for the stroke; {@link DocumentDashPattern#NONE} is solid
 */
public record LineFragmentPayload(
        Stroke stroke,
        double startX,
        double startY,
        double endX,
        double endY,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentDashPattern dashPattern
) implements PdfSemanticFragmentPayload {

    /**
     * Normalizes the dash pattern, defaulting to a solid stroke.
     */
    public LineFragmentPayload {
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
    }

    /**
     * Backward-compatible constructor for a solid line fragment.
     *
     * @param stroke          line stroke
     * @param startX          line start x offset inside the fragment
     * @param startY          line start y offset inside the fragment
     * @param endX            line end x offset inside the fragment
     * @param endY            line end y offset inside the fragment
     * @param linkOptions     optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public LineFragmentPayload(Stroke stroke,
                               double startX,
                               double startY,
                               double endX,
                               double endY,
                               DocumentLinkOptions linkOptions,
                               DocumentBookmarkOptions bookmarkOptions) {
        this(stroke, startX, startY, endX, endY, linkOptions, bookmarkOptions, DocumentDashPattern.NONE);
    }
}
