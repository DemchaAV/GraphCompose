package com.demcha.compose.document.node;

/**
 * Marker for a single inline run inside a {@link ParagraphNode}.
 *
 * <p>An inline paragraph is a sequence of runs measured and rendered on the
 * same baseline. Today there are two kinds of run: text and image. Both
 * participate in the wrapping algorithm so callers can mix small icons or
 * badges with styled text without resorting to nested layouts.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface InlineRun permits InlineTextRun, InlineImageRun {
}
