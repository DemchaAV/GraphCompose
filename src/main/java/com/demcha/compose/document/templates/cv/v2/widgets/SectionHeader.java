package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.Locale;

/**
 * Section-header widget — the title drawn above each section's body.
 *
 * <h2>Variants — each visually distinct, each with its own factory</h2>
 *
 * <ul>
 *   <li>{@link #banner} — pale-grey panel with centred spaced-caps
 *       title inside. Visual signature of {@code BoxedSections}.</li>
 *   <li>{@link #fullWidthBanner} — full-width fill banner with
 *       centred spaced-caps title. Used by compact editorial presets
 *       such as {@code BlueBanner}; surrounding rules stay in the
 *       preset's page-flow composition.</li>
 *   <li>{@link #underlined} — small left-aligned spaced-caps title
 *       with a thin accent rule beneath. Visual signature of
 *       {@code MinimalUnderlined}.</li>
 *   <li>{@link #flat} — large left-aligned bold title in a given
 *       colour, no panel, no rule. Visual signature of
 *       {@code ModernProfessional}.</li>
 *   <li>{@link #flatSpacedCaps} — small left-aligned spaced-caps bold
 *       title in a given colour, no panel, no rule. Quieter than
 *       {@link #flat}; visual signature of {@code CenteredHeadline}
 *       (and likely {@code NordicClean} / {@code ClassicSerif} when
 *       ported).</li>
 *   <li>{@link #tickLabel} — a short accent tick above a compact
 *       uppercase label. Used by command-card presets such as
 *       {@code CompactMono}.</li>
 * </ul>
 *
 * <p>Unlike {@link Headline} (one rendering shape, two text
 * transforms), section headers are <strong>structurally</strong>
 * different per variant — soft-panel vs accentBottom vs plain
 * paragraph. That's why each variant gets its own factory method
 * rather than a parameterised {@code render(spec)} entry point: the
 * underlying DSL calls share little.</p>
 *
 * <p>If you need a fourth variant (chip, numbered, gradient, …),
 * add a new factory method here. The signature pattern is
 * {@code (host, title, theme[, extras])} — keep it tight.</p>
 */
public final class SectionHeader {

    private SectionHeader() {
    }

    /**
     * Pale-grey banner panel with centred spaced-caps title. Visual
     * signature of {@code BoxedSections}.
     */
    public static void banner(SectionBuilder host, String title, CvTheme theme) {
        host.softPanel(theme.palette().banner(),
                        theme.spacing().bannerCornerRadius(),
                        theme.spacing().bannerInnerPadding())
                .margin(theme.spacing().bannerMargin())
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(theme.bannerStyle())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Full-width filled banner with centred spaced-caps title. This
     * variant is for presets where the section title is a solid band
     * in the main page flow (for example {@code BlueBanner}), not a
     * soft inset panel. Any rules above or below the band are
     * page-flow ornaments and should be composed by the preset.
     */
    public static void fullWidthBanner(SectionBuilder host, String title, CvTheme theme) {
        fullWidthBanner(host, title, theme, null);
    }

    /**
     * Full-width filled banner with an explicit title style override.
     *
     * @param titleStyleOverride text style for the banner label; pass
     *                           {@code null} to use
     *                           {@link CvTheme#bannerStyle()}
     */
    public static void fullWidthBanner(SectionBuilder host, String title,
                                       CvTheme theme,
                                       DocumentTextStyle titleStyleOverride) {
        DocumentTextStyle titleStyle = titleStyleOverride != null
                ? titleStyleOverride
                : theme.bannerStyle();
        host.fillColor(theme.palette().banner())
                .padding(new DocumentInsets(theme.spacing().bannerInnerPadding(),
                        0, theme.spacing().bannerInnerPadding(), 0))
                .margin(theme.spacing().bannerMargin())
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(titleStyle)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Small left-aligned spaced-caps title with a thin accent rule
     * beneath. Visual signature of {@code MinimalUnderlined}.
     */
    public static void underlined(SectionBuilder host, String title, CvTheme theme) {
        DocumentTextStyle titleStyle = theme.entryTitleStyle();
        host.accentBottom(theme.palette().rule(),
                        theme.spacing().accentRuleWidth())
                .padding(new DocumentInsets(8, 0, 2, 0))
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(titleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Large left-aligned bold title in a given colour. No panel,
     * no rule, no transform. Visual signature of
     * {@code ModernProfessional}.
     *
     * @param color the title colour — typically the preset's
     *              accent colour
     */
    public static void flat(SectionBuilder host, String title,
                            DocumentColor color, CvTheme theme) {
        DocumentTextStyle titleStyle = DocumentTextStyle.builder()
                .fontName(theme.typography().headlineFont())
                .size(theme.typography().sizeBanner())
                .decoration(DocumentTextDecoration.BOLD)
                .color(color)
                .build();
        host.padding(new DocumentInsets(8, 0, 2, 0))
                .addParagraph(p -> p
                        .text(title)
                        .textStyle(titleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Small left-aligned spaced-caps bold title in a given colour. No
     * panel, no rule — flat like {@link #flat} but typographically
     * quieter: body font, body-sized, transformed to letter-spaced
     * uppercase via {@link TextOrnaments#spacedUpper(String)}. Visual
     * signature of {@code CenteredHeadline}.
     *
     * <p>If the {@code titleStyle} parameter is {@code null} the widget
     * derives a style from the theme's body typography
     * ({@code bodyFont} at {@code sizeBanner} weight, bold, the given
     * colour). Pass an explicit style when the preset needs a specific
     * font / size combination that doesn't map to a theme slot.</p>
     *
     * @param host       host section
     * @param title      verbatim title text (transformed to spaced caps
     *                   by the widget)
     * @param color      title colour — typically a soft / muted accent
     * @param theme      active theme (used as the style default and
     *                   for the padding token)
     * @param titleStyle explicit style override; pass {@code null} to
     *                   fall back to the theme-derived default
     */
    public static void flatSpacedCaps(SectionBuilder host, String title,
                                      DocumentColor color, CvTheme theme,
                                      DocumentTextStyle titleStyle) {
        DocumentTextStyle resolved = titleStyle != null
                ? titleStyle
                : DocumentTextStyle.builder()
                        .fontName(theme.typography().bodyFont())
                        .size(theme.typography().sizeBanner())
                        .decoration(DocumentTextDecoration.BOLD)
                        .color(color)
                        .build();
        host.padding(new DocumentInsets(0, 0, 0, 0))
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(resolved)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * Short accent tick above a compact uppercase label. This is the
     * section-header shape used by terminal/card presets: the tick
     * supplies the accent, while the label stays small and readable.
     *
     * @param host      host section
     * @param title     label text, transformed to uppercase
     * @param theme     active theme; supplies the default mono label
     *                  font and size
     * @param color     accent/tick and label colour
     * @param tickWidth width of the short rule above the label
     */
    public static void tickLabel(SectionBuilder host, String title,
                                 CvTheme theme, DocumentColor color,
                                 double tickWidth) {
        tickLabel(host, title, theme, color, tickWidth, null);
    }

    /**
     * Short accent tick above a compact uppercase label with an
     * explicit label style override.
     *
     * @param titleStyle explicit style override; pass {@code null} to
     *                   derive a bold style from the theme headline
     *                   font and banner size
     */
    public static void tickLabel(SectionBuilder host, String title,
                                 CvTheme theme, DocumentColor color,
                                 double tickWidth,
                                 DocumentTextStyle titleStyle) {
        DocumentTextStyle resolved = titleStyle != null
                ? titleStyle
                : DocumentTextStyle.builder()
                        .fontName(theme.typography().headlineFont())
                        .size(theme.typography().sizeBanner())
                        .decoration(DocumentTextDecoration.BOLD)
                        .color(color)
                        .build();
        host.spacing(3)
                .addShape(shape -> shape
                        .name("CvV2SectionHeaderTick")
                        .size(tickWidth, theme.spacing().accentRuleWidth())
                        .fillColor(color)
                        .cornerRadius(theme.spacing().accentRuleWidth() / 2.0)
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(title.toUpperCase(Locale.ROOT))
                        .textStyle(resolved)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }
}
