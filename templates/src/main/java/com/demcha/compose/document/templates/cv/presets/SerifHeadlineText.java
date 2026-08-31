package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.core.text.MarkdownInline;

import java.util.ArrayList;
import java.util.List;

/** Turning a model field into the lines this sheet draws. */
final class SerifHeadlineText {

    private SerifHeadlineText() {
    }

    /**
     * One item per non-blank line — the family's way of carrying a list in a
     * field the model types as a single string.
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
}
