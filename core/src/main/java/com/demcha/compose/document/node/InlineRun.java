package com.demcha.compose.document.node;

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
}
