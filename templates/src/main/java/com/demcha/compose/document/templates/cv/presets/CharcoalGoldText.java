package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.data.CvEntry;

import java.util.ArrayList;
import java.util.List;

/** Turning model fields into the runs and lines this sheet draws. */
final class CharcoalGoldText {

    private CharcoalGoldText() {
    }

    /**
     * One item per non-blank line — the family's way of carrying a list in a
     * field the model types as a single string. The summary reads its own
     * paragraphs this way too.
     *
     * @param body the field's text
     * @return the lines, in order
     */
    static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split("\\R")) {
            String clean = MarkdownInline.plainText(line).trim();
            if (!clean.isBlank()) {
                out.add(clean);
            }
        }
        return out;
    }

    /**
     * Writes an entry's title into a paragraph, as a link when the entry
     * carries one. A link is an annotation rather than ink, so a linked
     * title and a plain one are the same sheet.
     *
     * @param paragraph the paragraph being built
     * @param entry     the entry whose title and link to write
     * @param style     the style of the title
     */
    static void title(ParagraphBuilder paragraph, CvEntry entry, DocumentTextStyle style) {
        if (entry.link().isBlank()) {
            paragraph.inlineText(entry.title(), style);
        } else {
            paragraph.inlineText(entry.title(), style, new DocumentLinkOptions(entry.link()));
        }
    }
}
