package com.demcha.compose.document.node;

/**
 * Marker for a single inline run inside a {@link ParagraphNode}.
 *
 * <p>An inline paragraph is a sequence of runs measured and rendered on the
 * same baseline. Today there are three kinds of run: text, image and shape.
 * All participate in the wrapping algorithm so callers can mix small icons,
 * badges or geometric figures (dots, diamonds, stars, …) with styled text
 * without resorting to nested layouts.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface InlineRun permits InlineTextRun, InlineImageRun, InlineShapeRun {
}
