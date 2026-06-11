package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.engine.components.content.text.TextStyle;

/**
 * Measured text span inside a paragraph line.
 *
 * @param text        visible text for the span
 * @param textStyle   resolved text style
 * @param width       measured span width
 * @param height      font line height contribution
 * @param linkOptions optional link metadata for the span
 */
public record ParagraphTextSpan(
        String text,
        TextStyle textStyle,
        double width,
        double height,
        DocumentLinkOptions linkOptions
) implements ParagraphSpan {
    /**
     * Creates a normalized measured paragraph text span.
     */
    public ParagraphTextSpan {
        text = text == null ? "" : text;
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
    }

    /**
     * Convenience constructor without link metadata.
     *
     * @param text      visible text for the span
     * @param textStyle resolved text style
     * @param width     measured span width
     * @param height    font line height contribution
     */
    public ParagraphTextSpan(String text, TextStyle textStyle, double width, double height) {
        this(text, textStyle, width, height, null);
    }
}
