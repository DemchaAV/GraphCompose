package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.engine.components.content.shape.Stroke;

/**
 * PDF payload for a resolved line fragment.
 *
 * @param stroke          line stroke
 * @param startX          line start x offset inside the fragment
 * @param startY          line start y offset inside the fragment
 * @param endX            line end x offset inside the fragment
 * @param endY            line end y offset inside the fragment
 * @param linkTarget     optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 * @param dashPattern     dash pattern for the stroke; {@link DocumentDashPattern#NONE} is solid
 * @param lineCap         end-cap style; {@link DocumentLineCap#BUTT} is the default
 */
public record LineFragmentPayload(
        Stroke stroke,
        double startX,
        double startY,
        double endX,
        double endY,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentDashPattern dashPattern,
        DocumentLineCap lineCap
) implements PdfSemanticFragmentPayload {

    /**
     * Normalizes the dash pattern and cap, defaulting to a solid butt-capped stroke.
     */
    public LineFragmentPayload {
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
        lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
    }

    /**
     * Backward-compatible constructor without the cap — defaults to
     * {@link DocumentLineCap#BUTT}.
     *
     * @param stroke          line stroke
     * @param startX          line start x offset inside the fragment
     * @param startY          line start y offset inside the fragment
     * @param endX            line end x offset inside the fragment
     * @param endY            line end y offset inside the fragment
     * @param linkTarget     optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     * @param dashPattern     dash pattern for the stroke
     */
    public LineFragmentPayload(Stroke stroke,
                               double startX,
                               double startY,
                               double endX,
                               double endY,
                               DocumentLinkTarget linkTarget,
                               DocumentBookmarkOptions bookmarkOptions,
                               DocumentDashPattern dashPattern) {
        this(stroke, startX, startY, endX, endY, linkTarget, bookmarkOptions, dashPattern, DocumentLineCap.BUTT);
    }

    /**
     * Backward-compatible constructor for a solid line fragment.
     *
     * @param stroke          line stroke
     * @param startX          line start x offset inside the fragment
     * @param startY          line start y offset inside the fragment
     * @param endX            line end x offset inside the fragment
     * @param endY            line end y offset inside the fragment
     * @param linkTarget     optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public LineFragmentPayload(Stroke stroke,
                               double startX,
                               double startY,
                               double endX,
                               double endY,
                               DocumentLinkTarget linkTarget,
                               DocumentBookmarkOptions bookmarkOptions) {
        this(stroke, startX, startY, endX, endY, linkTarget, bookmarkOptions, DocumentDashPattern.NONE, DocumentLineCap.BUTT);
    }
}
