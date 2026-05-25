package com.demcha.compose.document.templates.cv.v2.theme;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Aggregate cosmetic theme — palette + typography + spacing — passed
 * to every component renderer in {@code cv/v2/components}.
 *
 * <p>This is the <strong>only</strong> place a CV preset reads colour,
 * font, size, or spacing values from. Renderers never inline literal
 * RGB tuples, font names, or magic numbers.</p>
 *
 * <p>To define a new visual flavour: add a static factory here
 * returning a fresh {@code CvTheme} with custom sub-records. The
 * existing preset code keeps working — only the theme handed to
 * {@code BoxedSections.create(theme)} changes.</p>
 *
 * @param palette    colour tokens
 * @param typography font + size scale
 * @param spacing    paddings / margins / weights
 */
public record CvTheme(CvPalette palette,
                      CvTypography typography,
                      CvSpacing spacing,
                      CvDecoration decoration) {

    public CvTheme {
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(typography, "typography");
        Objects.requireNonNull(spacing, "spacing");
        Objects.requireNonNull(decoration, "decoration");
    }

    /**
     * Backward-compatible 3-arg constructor that fills the
     * {@link CvDecoration} slot with {@link CvDecoration#classic()}.
     * Retained so callers built before the decoration token landed
     * keep compiling and behaving identically.
     *
     * @deprecated since the introduction of {@link CvDecoration} —
     *             pass an explicit decoration so callers can choose
     *             a different bullet glyph or contact separator
     *             without forking the renderer.
     */
    @Deprecated
    public CvTheme(CvPalette palette, CvTypography typography, CvSpacing spacing) {
        this(palette, typography, spacing, CvDecoration.classic());
    }

    // -- canonical factories ---------------------------------------------

    /**
     * The "Boxed Sections" classic look — PT-Serif, near-black ink,
     * pale-grey section banners, round bullets, pipe contact
     * separators. Visual signature of the original
     * {@code cv-boxed-sections.pdf} reference output.
     */
    public static CvTheme boxedClassic() {
        return new CvTheme(
                CvPalette.classic(),
                CvTypography.classic(),
                CvSpacing.classic(),
                CvDecoration.classic());
    }

    /**
     * The "Modern Professional" look — Helvetica throughout, larger
     * scale, tighter spacing. Body palette is the classic ink/muted
     * pair; the preset itself adds the slate-blue name and
     * bright-blue section title accents because those colours are not
     * shared with any other v2 preset today.
     *
     * <p>When (or if) a second preset wants the same accent palette,
     * extract those colours into a new field on {@link CvPalette} and
     * point both presets at it.</p>
     */
    public static CvTheme modernProfessional() {
        return new CvTheme(
                CvPalette.classic(),
                CvTypography.modernProfessional(),
                CvSpacing.modernProfessional(),
                CvDecoration.classic());
    }

    /**
     * The "Centered Headline" classic look ported from the v1 preset
     * of the same name — Poppins headline, Lato body, soft greyscale
     * palette, thin full-width rules separating headline / contact /
     * each module. Pipe contact separator matches the classic
     * decoration.
     */
    public static CvTheme centeredHeadline() {
        return new CvTheme(
                CvPalette.centeredHeadline(),
                CvTypography.centeredHeadline(),
                CvSpacing.centeredHeadline(),
                CvDecoration.classic());
    }

    /**
     * The "Classic Serif" look — PT Serif throughout, warm dark ink,
     * tan rules, cream profile band, and the roomy pipe separator
     * from the classic decoration.
     */
    public static CvTheme classicSerif() {
        return new CvTheme(
                CvPalette.classicSerif(),
                CvTypography.classicSerif(),
                CvSpacing.classicSerif(),
                CvDecoration.classic());
    }

    /**
     * The "Nordic Clean" look — Barlow display typography, Lato body,
     * deep blue-green ink, pale teal profile band/rules, and compact
     * two-column spacing.
     */
    public static CvTheme nordicClean() {
        return new CvTheme(
                CvPalette.nordicClean(),
                CvTypography.nordicClean(),
                CvSpacing.nordicClean(),
                CvDecoration.classic());
    }

    /**
     * The "Blue Banner" look — PT Serif display name, Lato body,
     * compact spacing, blue full-width section banners, and tighter
     * pipe separators.
     */
    public static CvTheme blueBanner() {
        return new CvTheme(
                CvPalette.blueBanner(),
                CvTypography.blueBanner(),
                CvSpacing.blueBanner(),
                CvDecoration.blueBanner());
    }

    /**
     * The "Editorial Blue" look — compact Helvetica, vivid blue
     * section rules, centred editorial header, and dense body
     * spacing.
     */
    public static CvTheme editorialBlue() {
        return new CvTheme(
                CvPalette.editorialBlue(),
                CvTypography.editorialBlue(),
                CvSpacing.editorialBlue(),
                CvDecoration.classic());
    }
    // -- pre-built text-style helpers ------------------------------------
    // Renderers ask the theme for an already-composed DocumentTextStyle
    // instead of re-assembling font + size + decoration + colour every
    // call site. This is the only "computed" code in the theme — every
    // value reads from the underlying records.

    public DocumentTextStyle headlineStyle() {
        return style(typography.headlineFont(), typography.sizeHeadline(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle bannerStyle() {
        return style(typography.headlineFont(), typography.sizeBanner(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle contactStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle contactSeparatorStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.rule());
    }

    public DocumentTextStyle bodyStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle bodyBoldStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle entryTitleStyle() {
        return style(typography.bodyFont(), typography.sizeEntryTitle(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    public DocumentTextStyle entryDateStyle() {
        return style(typography.bodyFont(), typography.sizeEntryDate(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    public DocumentTextStyle entrySubtitleStyle() {
        return style(typography.bodyFont(), typography.sizeEntrySubtitle(),
                DocumentTextDecoration.ITALIC, palette.muted());
    }

    private static DocumentTextStyle style(FontName font, double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
