package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.engine.components.content.text.TextStyle;

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
        TextStyle textStyle,
        PdfLinkOptions linkOptions
) {
    public InlineTextRun {
        text = text == null ? "" : text;
    }

    public InlineTextRun(String text, TextStyle textStyle) {
        this(text, textStyle, null);
    }

    public InlineTextRun(String text) {
        this(text, null, null);
    }
}
