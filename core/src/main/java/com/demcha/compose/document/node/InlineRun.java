package com.demcha.compose.document.node;

import java.util.List;

/**
 * Marker for a single inline run inside a {@link ParagraphNode}.
 *
 * <p>An inline paragraph is a sequence of runs measured and rendered on the
 * same baseline. Today there are five kinds of run: text, image, shape, SVG
 * icon and highlight (text on a background chip). All participate in the
 * wrapping algorithm so callers can mix small icons, badges, vector glyphs
 * (e.g. colour emoji) or geometric figures (dots, diamonds, stars, …) with
 * styled text without resorting to nested layouts.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface InlineRun
        permits InlineTextRun, InlineImageRun, InlineShapeRun, InlineSvgRun, InlineHighlightRun {

    /**
     * Returns what a sequence of runs reads as in plain text.
     *
     * <p>Text runs and highlight chips contribute their text; image, shape and
     * SVG runs contribute nothing, because they are not text and inventing a
     * placeholder for them would put characters in the reading that the author
     * never wrote.</p>
     *
     * <p>This lives next to the {@code permits} clause on purpose. Which
     * variants read as text is knowledge that has to be revisited every time a
     * variant is added, and a second copy of it elsewhere is a copy that will
     * be missed: the reduction is here once, and both
     * {@link ParagraphNode#text()} and a rich
     * {@link com.demcha.compose.document.node.ListItem#label()} are derived
     * from it.</p>
     *
     * @param runs runs in source order; {@code null} reads as empty
     * @return the concatenated text of the runs that carry text
     * @since 2.4.0
     */
    static String plainText(List<InlineRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (InlineRun run : runs) {
            if (run instanceof InlineTextRun textRun) {
                out.append(textRun.text());
            } else if (run instanceof InlineHighlightRun highlight) {
                out.append(highlight.text());
            }
        }
        return out.toString();
    }

    /**
     * Returns the runs that carry text, in source order, for the surfaces that
     * consume text and its styling but cannot draw a picture — the semantic DOCX
     * export, and text-only tests.
     *
     * <p>Image, shape and SVG runs are dropped (emoji lower to SVG runs, so they
     * drop too). A highlight chip degrades to a plain text run, because its
     * background is a fixed-layout decoration while its text is content, and its
     * newlines collapse to spaces to match how the PDF tokenizer lowers a chip —
     * one line — so both surfaces read a chip the same way.</p>
     *
     * <p>That collapsing is the one thing this does not share with
     * {@link #plainText(List)}, which concatenates a chip's text as authored.</p>
     *
     * @param runs runs in source order; {@code null} reads as empty
     * @return the text-carrying runs, chips degraded to plain runs
     * @since 2.4.0
     */
    static List<InlineTextRun> textRuns(List<InlineRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return List.of();
        }
        List<InlineTextRun> textRuns = new java.util.ArrayList<>(runs.size());
        for (InlineRun run : runs) {
            if (run instanceof InlineTextRun textRun) {
                textRuns.add(textRun);
            } else if (run instanceof InlineHighlightRun highlight) {
                String chipText = highlight.text()
                        .replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
                textRuns.add(new InlineTextRun(chipText, highlight.textStyle(), highlight.linkTarget()));
            }
        }
        return List.copyOf(textRuns);
    }
}
