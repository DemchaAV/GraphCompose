package com.demcha.compose.document.node;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * One styled inline text run inside a semantic paragraph.
 *
 * @param text visible text for the run
 * @param textStyle style for this run; falls back to the paragraph style when null
 * @param linkOptions optional link metadata scoped only to this run
 * @author Artem Demchyshyn
 */
public record InlineTextRun(
        String text,
        DocumentTextStyle textStyle,
        DocumentLinkOptions linkOptions
) {
    /**
     * Normalizes null text to an empty run.
     */
    public InlineTextRun {
        text = text == null ? "" : text;
    }

    /**
     * Creates a styled inline run without link metadata.
     *
     * @param text visible text
     * @param textStyle style for this run
     */
    public InlineTextRun(String text, DocumentTextStyle textStyle) {
        this(text, textStyle, null);
    }

    /**
     * Creates an unstyled inline run.
     *
     * @param text visible text
     */
    public InlineTextRun(String text) {
        this(text, null, null);
    }
}
