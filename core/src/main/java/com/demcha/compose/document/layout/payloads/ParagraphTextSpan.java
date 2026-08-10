package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.engine.components.content.text.TextStyle;

/**
 * Measured text span inside a paragraph line.
 *
 * @param text       visible text for the span
 * @param textStyle  resolved text style
 * @param width      measured span width (includes the chip's horizontal padding when {@code background} is set)
 * @param height     font line height contribution
 * @param linkTarget optional link metadata for the span
 * @param background optional rounded background "chip" painted behind the glyphs, or {@code null}
 * @param rightToLeft whether the span's characters are drawn right to left; the text
 *                    itself stays in logical order, so a backend that draws characters
 *                    in the order given has to reverse them ({@code @since 2.2.0})
 */
public record ParagraphTextSpan(
        String text,
        TextStyle textStyle,
        double width,
        double height,
        DocumentLinkTarget linkTarget,
        InlineBackground background,
        boolean rightToLeft
) implements ParagraphSpan {
    /**
     * Creates a normalized measured paragraph text span.
     */
    public ParagraphTextSpan {
        text = text == null ? "" : text;
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
    }

    /**
     * Creates a left-to-right span.
     *
     * @param text       visible text for the span
     * @param textStyle  resolved text style
     * @param width      measured span width
     * @param height     font line height contribution
     * @param linkTarget optional link metadata for the span
     * @param background optional rounded background chip
     */
    public ParagraphTextSpan(String text, TextStyle textStyle, double width, double height,
                             DocumentLinkTarget linkTarget, InlineBackground background) {
        this(text, textStyle, width, height, linkTarget, background, false);
    }

    /**
     * Convenience constructor without a background chip.
     *
     * @param text       visible text for the span
     * @param textStyle  resolved text style
     * @param width      measured span width
     * @param height     font line height contribution
     * @param linkTarget optional link metadata for the span
     */
    public ParagraphTextSpan(String text, TextStyle textStyle, double width, double height,
                             DocumentLinkTarget linkTarget) {
        this(text, textStyle, width, height, linkTarget, null);
    }

    /**
     * Convenience constructor without link metadata or background.
     *
     * @param text      visible text for the span
     * @param textStyle resolved text style
     * @param width     measured span width
     * @param height    font line height contribution
     */
    public ParagraphTextSpan(String text, TextStyle textStyle, double width, double height) {
        this(text, textStyle, width, height, null, null);
    }
}
