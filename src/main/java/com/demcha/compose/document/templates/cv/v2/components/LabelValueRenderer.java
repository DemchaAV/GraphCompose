package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * Renders compact "Label: rich markdown value" rows used by CV detail sections.
 */
public final class LabelValueRenderer {
    private LabelValueRenderer() {
    }

    public static void render(SectionBuilder host,
                              String label,
                              String value,
                              DocumentTextStyle labelStyle,
                              DocumentTextStyle valueStyle,
                              double lineSpacing,
                              DocumentInsets margin) {
        host.addParagraph(paragraph -> paragraph
                .textStyle(valueStyle)
                .lineSpacing(lineSpacing)
                .align(TextAlign.LEFT)
                .margin(margin)
                .rich(rich -> {
                    rich.style(normalizedLabel(label) + ":", labelStyle);
                    if (value != null && !value.isBlank()) {
                        rich.style(" ", valueStyle);
                        MarkdownInline.append(rich, value, valueStyle);
                    }
                }));
    }

    static String normalizedLabel(String label) {
        String value = MarkdownInline.plainText(label).trim();
        while (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }
}
