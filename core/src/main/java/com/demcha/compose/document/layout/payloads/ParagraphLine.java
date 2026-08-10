package com.demcha.compose.document.layout.payloads;

import java.util.List;

/**
 * One measured paragraph line emitted to the PDF backend.
 *
 * @param text                     line text used for diagnostics and simple rendering paths
 * @param width                    measured line width
 * @param lineHeight               resolved line height (max of text and image heights)
 * @param textLineHeight           font-line-height for the dominant text style on
 *                                 this line; equals {@code lineHeight} when no
 *                                 inline image enlarges the line
 * @param textAscent               ascent of the dominant text style on this line; used
 *                                 to position image spans relative to the baseline
 * @param baselineOffsetFromBottom distance from line bottom to the text
 *                                 baseline
 * @param spans                    measured styled spans in source order
 * @param visualOrder              indices into {@code spans} in the order they are
 *                                 drawn, left to right; empty when that is the source
 *                                 order, which is every line of left-to-right text
 */
public record ParagraphLine(
        String text,
        double width,
        double lineHeight,
        double textLineHeight,
        double textAscent,
        double baselineOffsetFromBottom,
        List<ParagraphSpan> spans,
        List<Integer> visualOrder
) {
    /**
     * Creates a normalized measured paragraph line.
     */
    public ParagraphLine {
        text = text == null ? "" : text;
        spans = List.copyOf(spans);
        visualOrder = visualOrder == null ? List.of() : List.copyOf(visualOrder);
    }

    /**
     * Creates a line whose spans are drawn in source order.
     *
     * <p>Keeps the previous shape available for the paths that never reorder, and for
     * callers written before direction existed.</p>
     */
    public ParagraphLine(String text,
                         double width,
                         double lineHeight,
                         double textLineHeight,
                         double textAscent,
                         double baselineOffsetFromBottom,
                         List<ParagraphSpan> spans) {
        this(text, width, lineHeight, textLineHeight, textAscent, baselineOffsetFromBottom,
                spans, List.of());
    }

    /**
     * Returns the spans in the order they are drawn, left to right.
     *
     * <p>Spans stay in logical order in {@link #spans()} — the order the text is read
     * and the order the semantic backends need — so the visual order is expressed as a
     * permutation rather than by rearranging them.</p>
     *
     * @return the spans in drawing order
     */
    public List<ParagraphSpan> spansInVisualOrder() {
        if (visualOrder.isEmpty()) {
            return spans;
        }
        List<ParagraphSpan> ordered = new java.util.ArrayList<>(visualOrder.size());
        for (int index : visualOrder) {
            ordered.add(spans.get(index));
        }
        return List.copyOf(ordered);
    }
}
