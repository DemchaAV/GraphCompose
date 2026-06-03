package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Small module wrapper for CV side rails and card interiors: create a
 * named section, render a reusable {@link SectionHeader} variant, then
 * let the caller draw the body.
 */
public final class SectionModule {

    private SectionModule() {
    }

    /**
     * Module headed by {@link SectionHeader#tickLabel}; useful for
     * compact card/terminal layouts.
     *
     * @param parent     the section builder the module is appended to
     * @param name       node name used in snapshots and layout graph paths
     * @param title      the section title text
     * @param theme      the active theme supplying palette, typography, and spacing
     * @param color      accent/tick and label colour
     * @param tickWidth  width of the short tick above the label
     * @param titleStyle text style for the label
     * @param body       callback drawing the section body
     */
    public static void tick(SectionBuilder parent,
                            String name,
                            String title,
                            CvTheme theme,
                            DocumentColor color,
                            double tickWidth,
                            DocumentTextStyle titleStyle,
                            Consumer<SectionBuilder> body) {
        render(parent, name, host -> SectionHeader.tickLabel(host, title,
                theme, color, tickWidth, titleStyle), body);
    }

    /**
     * Module headed by {@link SectionHeader#upperRule}; useful for
     * clean side-rail layouts.
     *
     * @param parent     the section builder the module is appended to
     * @param name       node name used in snapshots and layout graph paths
     * @param title      the section title text
     * @param theme      the active theme supplying palette, typography, and spacing
     * @param titleStyle text style for the label
     * @param ruleColor  colour of the rule beneath the label
     * @param ruleWidth  width in points of the rule beneath the label
     * @param body       callback drawing the section body
     */
    public static void upperRule(SectionBuilder parent,
                                 String name,
                                 String title,
                                 CvTheme theme,
                                 DocumentTextStyle titleStyle,
                                 DocumentColor ruleColor,
                                 double ruleWidth,
                                 Consumer<SectionBuilder> body) {
        render(parent, name, host -> SectionHeader.upperRule(host, title,
                theme, titleStyle, ruleColor, ruleWidth), body);
    }

    private static void render(SectionBuilder parent,
                               String name,
                               Consumer<SectionBuilder> heading,
                               Consumer<SectionBuilder> body) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(body, "body");
        parent.addSection(name, host -> {
            heading.accept(host);
            body.accept(host);
        });
    }
}
