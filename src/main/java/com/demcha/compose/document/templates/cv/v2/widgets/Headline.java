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
     * Right-aligned plain bold headline. Visual signature of
     * {@code ModernProfessional}.
     */
    public static void rightAligned(SectionBuilder host, CvName name, CvTheme theme) {
        render(host, name, theme, TextAlign.RIGHT, false);
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
        DocumentTextStyle style = theme.headlineStyle();
        String text = spacedCaps
                ? TextOrnaments.spacedUpper(name.full())
                : name.full();

        host.spacing(2)
                .padding(theme.spacing().headlinePadding())
                .addParagraph(p -> p
                        .text(text)
                        .textStyle(style)
                        .align(alignment)
                        .margin(DocumentInsets.zero()));
    }
}
