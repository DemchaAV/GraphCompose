package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Draws one prose paragraph (markdown-aware) in body style. Used by
 * {@code ParagraphSection} bodies and as a shared primitive by other
 * renderers (e.g. the description line under an entry).
 */
public final class ParagraphRenderer {

    private ParagraphRenderer() {
    }

    /**
     * @param section host
     * @param text    paragraph text; blank inputs are silently skipped
     * @param theme   active theme
     */
    public static void render(SectionBuilder section, String text, CvTheme theme) {
        if (text == null || text.isBlank()) {
            return;
        }
        DocumentTextStyle base = theme.bodyStyle();
        section.addParagraph(p -> p
                .textStyle(base)
                .lineSpacing(theme.typography().bodyLineSpacing())
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop()))
                .rich(rich -> MarkdownInline.append(rich, text.trim(), base)));
    }
}
