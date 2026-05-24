package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Section-header widget — the title drawn above each section's body.
 *
 * <h2>Variants — each visually distinct, each with its own factory</h2>
 *
 * <ul>
 *   <li>{@link #banner} — pale-grey panel with centred spaced-caps
 *       title inside. Visual signature of {@code BoxedSections}.</li>
 *   <li>{@link #underlined} — small left-aligned spaced-caps title
 *       with a thin accent rule beneath. Visual signature of
 *       {@code MinimalUnderlined}.</li>
 *   <li>{@link #flat} — large left-aligned bold title in a given
 *       colour, no panel, no rule. Visual signature of
 *       {@code ModernProfessional}.</li>
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
}
