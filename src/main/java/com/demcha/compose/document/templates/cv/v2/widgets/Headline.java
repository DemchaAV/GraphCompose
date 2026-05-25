package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Top-of-document headline widget — the subject's name as the page's
 * largest text element.
 *
 * <h2>Variants</h2>
 *
 * <ul>
 *   <li>{@link #spacedCentered} — centred letter-spaced uppercase
 *       (e.g. {@code J A N E   D O E}). Used by classic /
 *       editorial presets where the name is the page's visual
 *       focal point.</li>
 *   <li>{@link #uppercaseCentered} — centred uppercase without
 *       extra letter spacing (e.g. {@code JANE DOE}). Used by
 *       compact editorial presets.</li>
 *   <li>{@link #rightAligned} — right-aligned plain bold (e.g.
 *       {@code Jane Doe}). Used by modern / corporate presets
 *       where the name sits in a header bar next to contacts.</li>
 * </ul>
 *
 * <h2>Customisation</h2>
 *
 * <p>{@link #render} is the lower-level entry point taking
 * alignment and "use spaced caps" as parameters. Any combination
 * not covered by a named factory ({@code leftAligned-spacedCaps},
 * {@code centeredPlain}, …) is reachable via {@link #render}.</p>
 *
 * <p>If even that isn't enough, inline a custom paragraph in the
 * preset's {@code compose()} — widgets are optional helpers, not
 * required wrappers.</p>
 */
public final class Headline {

    private Headline() {
    }

    /**
     * Centred letter-spaced uppercase headline. Visual signature of
     * {@code BoxedSections} and {@code MinimalUnderlined}.
     */
    public static void spacedCentered(SectionBuilder host, CvName name, CvTheme theme) {
        render(host, name, theme, TextAlign.CENTER, true);
    }

    /**
     * Centred uppercase headline without letter spacing. Visual
     * signature of compact editorial presets that want a strong
     * masthead but not the wider classic spaced-caps treatment.
     */
    public static void uppercaseCentered(SectionBuilder host, CvName name,
                                         CvTheme theme) {
        uppercaseCentered(host, name, theme, null);
    }

    /**
     * Centred uppercase headline without letter spacing and with an
     * explicit style override.
     *
     * @param styleOverride explicit style; pass {@code null} to fall
     *                      back to {@code theme.headlineStyle()}
     */
    public static void uppercaseCentered(SectionBuilder host, CvName name,
                                         CvTheme theme,
                                         DocumentTextStyle styleOverride) {
        renderText(host, name.full().toUpperCase(java.util.Locale.ROOT),
                theme, TextAlign.CENTER, styleOverride);
    }

    /**
     * Right-aligned plain headline using the theme's default
     * {@link CvTheme#headlineStyle() headline style}. Visual
     * signature of corporate / modern presets that don't need a
     * custom display colour.
     */
    public static void rightAligned(SectionBuilder host, CvName name, CvTheme theme) {
        rightAligned(host, name, theme, null);
    }

    /**
     * Right-aligned headline with an explicit {@link DocumentTextStyle}
     * override — the preset hands the widget exactly the font / size
     * / colour it wants. Used by {@code ModernProfessional} to apply
     * its preset-specific slate-blue display colour.
     *
     * @param styleOverride text style for the headline; pass {@code null}
     *                      to fall back to {@code theme.headlineStyle()}
     */
    public static void rightAligned(SectionBuilder host, CvName name, CvTheme theme,
                                    DocumentTextStyle styleOverride) {
        render(host, name, theme, TextAlign.RIGHT, false, styleOverride);
    }

    /**
     * Lower-level entry. Pick the alignment and whether the name
     * should be transformed to spaced uppercase. Text style comes
     * from {@link CvTheme#headlineStyle()}; padding from
     * {@code theme.spacing().headlinePadding()}.
     *
     * @param host       host section
     * @param name       name to render
     * @param theme      active theme
     * @param alignment  paragraph alignment
     * @param spacedCaps if true, transforms to letter-spaced
     *                   uppercase; if false, renders verbatim
     */
    public static void render(SectionBuilder host, CvName name, CvTheme theme,
                              TextAlign alignment, boolean spacedCaps) {
        render(host, name, theme, alignment, spacedCaps, null);
    }

    /**
     * Lower-level entry with explicit style override. Same shape as
     * the 5-arg {@link #render(SectionBuilder, CvName, CvTheme, TextAlign, boolean)}
     * but lets the caller supply a custom {@link DocumentTextStyle}.
     *
     * @param styleOverride explicit style; pass {@code null} to fall
     *                      back to {@code theme.headlineStyle()}
     */
    public static void render(SectionBuilder host, CvName name, CvTheme theme,
                              TextAlign alignment, boolean spacedCaps,
                              DocumentTextStyle styleOverride) {
        DocumentTextStyle style = styleOverride != null
                ? styleOverride
                : theme.headlineStyle();
        String text = spacedCaps
                ? TextOrnaments.spacedUpper(name.full())
                : name.full();

        renderText(host, text, theme, alignment, style);
    }

    private static void renderText(SectionBuilder host, String text, CvTheme theme,
                                   TextAlign alignment,
                                   DocumentTextStyle styleOverride) {
        DocumentTextStyle style = styleOverride != null
                ? styleOverride
                : theme.headlineStyle();
        host.spacing(2)
                .padding(theme.spacing().headlinePadding())
                .addParagraph(p -> p
                        .text(text)
                        .textStyle(style)
                        .align(alignment)
                        .margin(DocumentInsets.zero()));
    }
}
