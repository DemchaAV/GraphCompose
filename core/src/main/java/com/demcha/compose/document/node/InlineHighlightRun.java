package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.InlineBackground;

import java.util.Objects;

/**
 * One inline text run drawn on a rounded background "chip" — styled text on a
 * padded fill, seated on the text baseline and flowing inside a paragraph, e.g.
 * a GitHub-style inline {@code code} span or a status badge.
 *
 * <p>Unlike the image/shape/SVG runs it is <em>text</em>: it wraps with the
 * surrounding line. The background is a PDF decoration — text-only backends keep
 * the text and drop the fill (see {@link ParagraphNode#inlineTextRuns()}).</p>
 *
 * @param text       visible text for the run
 * @param textStyle  style for the glyphs; falls back to the paragraph style when {@code null}
 * @param background the chip fill, corner radius and padding; must not be {@code null}
 * @param linkTarget optional link target (external URI or internal anchor) scoped to this run
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record InlineHighlightRun(
        String text,
        DocumentTextStyle textStyle,
        InlineBackground background,
        DocumentLinkTarget linkTarget
) implements InlineRun {
    /**
     * Normalizes null text to an empty run and validates the background.
     */
    public InlineHighlightRun {
        text = text == null ? "" : text;
        Objects.requireNonNull(background, "background");
    }

    /**
     * Creates a highlight run with external link metadata.
     *
     * @param text        visible text
     * @param textStyle   style for this run
     * @param background  chip fill / radius / padding
     * @param linkOptions external link metadata, wrapped into an {@link ExternalLinkTarget}
     */
    public InlineHighlightRun(String text, DocumentTextStyle textStyle, InlineBackground background,
                              DocumentLinkOptions linkOptions) {
        this(text, textStyle, background, linkOptions == null ? null : new ExternalLinkTarget(linkOptions));
    }

    /**
     * Creates a highlight run without link metadata.
     *
     * @param text       visible text
     * @param textStyle  style for this run
     * @param background chip fill / radius / padding
     */
    public InlineHighlightRun(String text, DocumentTextStyle textStyle, InlineBackground background) {
        this(text, textStyle, background, (DocumentLinkTarget) null);
    }
}
