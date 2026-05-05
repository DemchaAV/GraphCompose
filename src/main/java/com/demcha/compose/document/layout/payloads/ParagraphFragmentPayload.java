package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

/**
 * PDF payload for a resolved paragraph fragment.
 *
 * @param textStyle base text style for the fragment
 * @param align horizontal text alignment
 * @param padding fragment padding
 * @param lineHeight resolved line height
 * @param lineGap extra spacing between lines
 * @param baselineOffset offset from line bottom to baseline
 * @param lines measured lines contained by the fragment
 * @param linkOptions optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record ParagraphFragmentPayload(
        TextStyle textStyle,
        TextAlign align,
        Padding padding,
        double lineHeight,
        double lineGap,
        double baselineOffset,
        List<ParagraphLine> lines,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
    /**
     * Creates an immutable paragraph fragment payload.
     */
    public ParagraphFragmentPayload {
        lines = List.copyOf(lines);
    }
}
