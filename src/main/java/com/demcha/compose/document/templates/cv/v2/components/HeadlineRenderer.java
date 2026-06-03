package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;

/**
 * @deprecated Use
 * {@link com.demcha.compose.document.templates.cv.v2.widgets.Headline#spacedCentered}
 * instead — the widget gives you a named API plus alignment +
 * spaced-caps variants, while this class only ever did the
 * centred-spaced-caps form. Kept as a thin delegating shim so v2
 * code written before the widgets layer keeps compiling unchanged.
 */
@Deprecated
public final class HeadlineRenderer {

    private HeadlineRenderer() {
    }

    /**
     * @param section the section builder being populated
     * @param name    the candidate name to render as the headline
     * @param theme   the active theme supplying palette, typography, and spacing
     * @deprecated delegates to {@link Headline#spacedCentered}.
     */
    @Deprecated
    public static void render(SectionBuilder section, CvName name, CvTheme theme) {
        Headline.spacedCentered(section, name, theme);
    }
}
