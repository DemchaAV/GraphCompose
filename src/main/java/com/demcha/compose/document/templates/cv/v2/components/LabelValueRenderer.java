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

    /**
     * Renders one {@code "Label: value"} row, with the value parsed as
     * inline markdown.
     *
     * @param host        host section receiving the row
     * @param label       the label text; a trailing colon is normalised away
     * @param value       the value text; blank values render the label alone
     * @param labelStyle  text style for the label
     * @param valueStyle  text style for the value
     * @param lineSpacing extra space between wrapped lines
     * @param margin      outer margin of the paragraph
     */
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
