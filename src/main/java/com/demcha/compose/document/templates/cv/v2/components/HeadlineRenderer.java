package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Draws the centred letter-spaced uppercase name headline at the very
 * top of a CV — the first thing a reader sees.
 */
public final class HeadlineRenderer {

    private HeadlineRenderer() {
    }

    /**
     * @param section host section that will host the headline paragraph
     * @param name    full name to render
     * @param theme   active theme
     */
    public static void render(SectionBuilder section, CvName name, CvTheme theme) {
        section.spacing(2)
                .padding(theme.spacing().headlinePadding())
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(name.full()))
                        .textStyle(theme.headlineStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }
}
