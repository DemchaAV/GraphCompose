package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Draws the pale-grey banner row holding a centred, letter-spaced
 * uppercase section title (e.g. {@code P R O J E C T S}).
 *
 * <p>The host {@link SectionBuilder} becomes the banner panel —
 * theme tokens drive the fill colour, corner radius, inner padding,
 * and surrounding margin.</p>
 */
public final class BannerRenderer {

    private BannerRenderer() {
    }

    public static void render(SectionBuilder section, String title, CvTheme theme) {
        section.softPanel(theme.palette().banner(),
                        theme.spacing().bannerCornerRadius(),
                        theme.spacing().bannerInnerPadding())
                .margin(theme.spacing().bannerMargin())
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(theme.bannerStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }
}
