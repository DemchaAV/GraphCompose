package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;

/**
 * @deprecated Use
 * {@link com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader#banner}
 * instead — the widget groups the banner alongside its sibling
 * variants ({@code underlined}, {@code flat}) so picking a section-
 * title style becomes one choice in one place. Kept as a thin
 * delegating shim so v2 code written before the widgets layer keeps
 * compiling unchanged.
 */
@Deprecated
public final class BannerRenderer {

    private BannerRenderer() {
    }

    /**
     * @param section the section builder being populated
     * @param title   the section title text
     * @param theme   the active theme supplying palette, typography, and spacing
     * @deprecated delegates to {@link SectionHeader#banner}.
     */
    @Deprecated
    public static void render(SectionBuilder section, String title, CvTheme theme) {
        SectionHeader.banner(section, title, theme);
    }
}
