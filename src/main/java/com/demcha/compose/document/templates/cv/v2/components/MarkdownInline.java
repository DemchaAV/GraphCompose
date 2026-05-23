package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.components.MarkdownText;

/**
 * Tiny adapter that pushes inline-markdown-parsed runs of {@code text}
 * into a {@link RichText} builder using {@code baseStyle} for plain
 * (non-emphasised) segments.
 *
 * <p>Honours {@code **bold**}, {@code *italic*}, {@code _italic_} via
 * the shared {@link MarkdownText} parser. Lives in the components
 * layer because every body / row / entry renderer calls it.</p>
 */
public final class MarkdownInline {

    private MarkdownInline() {
    }

    /**
     * Appends {@code text} to {@code rich}, expanding inline markdown.
     *
     * @param rich      target rich-text builder
     * @param text      source string; null treated as empty
     * @param baseStyle style applied to plain runs
     */
    public static void append(RichText rich, String text,
                              DocumentTextStyle baseStyle) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (InlineRun run : MarkdownText.parse(text, baseStyle)) {
            if (!(run instanceof InlineTextRun textRun)) {
                continue;
            }
            DocumentTextStyle runStyle = textRun.textStyle() == null
                    ? baseStyle
                    : textRun.textStyle();
            rich.style(textRun.text(), runStyle);
        }
    }
}
