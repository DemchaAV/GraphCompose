package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * Reusable rich paragraph primitive for CV presets that need explicit
 * style, line spacing, and margin while still honouring inline markdown.
 */
public final class RichParagraphRenderer {
    private RichParagraphRenderer() {
    }

    public static void render(SectionBuilder host,
                              String text,
                              DocumentTextStyle style,
                              double lineSpacing,
                              DocumentInsets margin) {
        render(host, text, style, lineSpacing, margin, TextAlign.LEFT);
    }

    public static void render(SectionBuilder host,
                              String text,
                              DocumentTextStyle style,
                              double lineSpacing,
                              DocumentInsets margin,
                              TextAlign align) {
        if (text == null || text.isBlank()) {
            return;
        }
        host.addParagraph(paragraph -> paragraph
                .textStyle(style)
                .lineSpacing(lineSpacing)
                .align(align)
                .margin(margin)
                .rich(rich -> MarkdownInline.appendTrimmed(rich, text, style)));
    }
}
