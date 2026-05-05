package com.demcha.compose.document.layout.payloads;

import java.util.List;

/**
 * One measured paragraph line emitted to the PDF backend.
 *
 * @param text line text used for diagnostics and simple rendering paths
 * @param width measured line width
 * @param lineHeight resolved line height (max of text and image heights)
 * @param textLineHeight font-line-height for the dominant text style on
 *                       this line; equals {@code lineHeight} when no
 *                       inline image enlarges the line
 * @param textAscent ascent of the dominant text style on this line; used
 *                   to position image spans relative to the baseline
 * @param baselineOffsetFromBottom distance from line bottom to the text
 *                                 baseline
 * @param spans measured styled spans in source order
 */
public record ParagraphLine(
        String text,
        double width,
        double lineHeight,
        double textLineHeight,
        double textAscent,
        double baselineOffsetFromBottom,
        List<ParagraphSpan> spans
) {
    /**
     * Creates a normalized measured paragraph line.
     */
    public ParagraphLine {
        text = text == null ? "" : text;
        spans = List.copyOf(spans);
    }
}
