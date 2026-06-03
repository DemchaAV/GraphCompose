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
 * @param decoration glyph / separator tokens
 */
public record CvTheme(CvPalette palette,
                      CvTypography typography,
                      CvSpacing spacing,
                      CvDecoration decoration) {

    /** Validates that no sub-record is null. */
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
     * @param palette    colour tokens
     * @param typography font + size scale
     * @param spacing    paddings / margins / weights
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
     *
     * @return a {@code CvTheme} for the "Boxed Sections" classic look
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
     *
     * @return a {@code CvTheme} for the "Modern Professional" look
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
     *
     * @return a {@code CvTheme} for the "Centered Headline" look
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
     *
     * @return a {@code CvTheme} for the "Classic Serif" look
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
     *
     * @return a {@code CvTheme} for the "Nordic Clean" look
     */
    public static CvTheme nordicClean() {
        return new CvTheme(
                CvPalette.nordicClean(),
                CvTypography.nordicClean(),
                CvSpacing.nordicClean(),
                CvDecoration.classic());
    }

    /**
     * The "Compact Mono" look — dark command-bar header, IBM Plex
     * Mono labels, teal accents, pale left rail, and compact card
     * spacing.
     *
     * @return a {@code CvTheme} for the "Compact Mono" look
     */
    public static CvTheme compactMono() {
        return new CvTheme(
                CvPalette.compactMono(),
                CvTypography.compactMono(),
                CvSpacing.compactMono(),
                CvDecoration.compactMono());
    }

    /**
     * The "Blue Banner" look — PT Serif display name, Lato body,
     * compact spacing, blue full-width section banners, and tighter
     * pipe separators.
     *
     * @return a {@code CvTheme} for the "Blue Banner" look
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
     *
     * @return a {@code CvTheme} for the "Editorial Blue" look
     */
    public static CvTheme editorialBlue() {
        return new CvTheme(
                CvPalette.editorialBlue(),
                CvTypography.editorialBlue(),
                CvSpacing.editorialBlue(),
                CvDecoration.classic());
    }

    /**
     * The "Sidebar Portrait" look — Crimson Text serif hero, Lato
     * body, restrained grey palette. Pale-beige left sidebar carries
     * a circular portrait photo, contact stack, education + key
     * skills + languages summary; the right column carries a large
     * serif name (positioned to straddle the sidebar/main boundary
     * via a hero strip), professional profile, and experience
     * timeline. Visual signature ported from the v1
     * {@code SidebarPortraitCvTemplateComposer}.
     *
     * @return a {@code CvTheme} for the "Sidebar Portrait" look
     */
    public static CvTheme sidebarPortrait() {
        return new CvTheme(
                CvPalette.sidebarPortrait(),
                CvTypography.sidebarPortrait(),
                CvSpacing.sidebarPortrait(),
                CvDecoration.classic());
    }

    /**
     * The "Monogram Sidebar" look — Crimson Text display + Lato body,
     * pale teal-grey sidebar with a dark monogram ring badge holding
     * the subject's initials, centered icon-driven contact stack,
     * education and expertise blocks, plus a two-line spaced-caps
     * headline and main career narrative on the right. Visual
     * signature ported from the v1
     * {@code MonogramSidebarCvTemplateComposer}.
     *
     * @return a {@code CvTheme} for the "Monogram Sidebar" look
     */
    public static CvTheme monogramSidebar() {
        return new CvTheme(
                CvPalette.monogramSidebar(),
                CvTypography.monogramSidebar(),
                CvSpacing.monogramSidebar(),
                CvDecoration.classic());
    }

    /**
     * The "Engineering Resume" look — Barlow display + Lato body, deep
     * navy command header with cyan-green contact links, dark navy
     * skill rail with green accent labels, and white evidence cards
     * for Leadership Experience + Technical Evidence on the right.
     * Visual signature ported from the v1
     * {@code TechLeadCvTemplateComposer}.
     *
     * @return a {@code CvTheme} for the "Engineering Resume" look
     */
    public static CvTheme engineeringResume() {
        return new CvTheme(
                CvPalette.engineeringResume(),
                CvTypography.engineeringResume(),
                CvSpacing.engineeringResume(),
                CvDecoration.classic());
    }

    /**
     * The "Timeline Minimal" look — Barlow Condensed display + Lato
     * body, all-grey palette, spaced uppercase name, right-aligned
     * contact stack with PNG icons, and a thin vertical timeline axis
     * with three circles separating the sidebar from the main column.
     * Visual signature ported from the v1
     * {@code TimelineMinimalCvTemplateComposer}.
     *
     * @return a {@code CvTheme} for the "Timeline Minimal" look
     */
    public static CvTheme timelineMinimal() {
        return new CvTheme(
                CvPalette.timelineMinimal(),
                CvTypography.timelineMinimal(),
                CvSpacing.timelineMinimal(),
                CvDecoration.classic());
    }

    /**
     * The "Panel" look — Poppins headlines + Lato body, pale teal
     * header card and module panels with thin teal stroke, deep navy
     * masthead text, and teal section headings with a small accent
     * strip beneath each title. Visual signature ported from the v1
     * {@code PanelCvTemplateComposer} (ProductLeader tokens).
     *
     * @return a {@code CvTheme} for the "Panel" look
     */
    public static CvTheme panel() {
        return new CvTheme(
                CvPalette.panel(),
                CvTypography.panel(),
                CvSpacing.panel(),
                CvDecoration.classic());
    }

    /**
     * The "Executive" look — Poppins masthead + Lato body, deep slate
     * primary, warm bronze accent on module headings and contact
     * links, and a thin full-width muted rule under the header.
     * Visual signature ported from the legacy
     * {@code ExecutiveSlateCvTemplate}.
     *
     * @return a {@code CvTheme} for the "Executive" look
     */
    public static CvTheme executive() {
        return new CvTheme(
                CvPalette.executive(),
                CvTypography.executive(),
                CvSpacing.executive(),
                CvDecoration.classic());
    }

    /**
     * The "Mint Editorial" look — Poppins throughout, near-black ink, a
     * soft mint accent (carried in the palette {@code banner} slot) used
     * for the full-width masthead rule, the spaced-caps section
     * headings, and the centered tagline. Two-page two-column editorial
     * CV: a left sidebar (contact, interests, education, expertise,
     * skill bars, social) beside a main column (profile, experience,
     * awards, references). Paired 1:1 with the Mint Editorial cover
     * letter, which reuses this exact theme.
     *
     * @return a {@code CvTheme} for the "Mint Editorial" look
     */
    public static CvTheme mintEditorial() {
        return new CvTheme(
                CvPalette.mintEditorial(),
                CvTypography.mintEditorial(),
                CvSpacing.mintEditorial(),
                CvDecoration.classic());
    }
    // -- pre-built text-style helpers ------------------------------------
    // Renderers ask the theme for an already-composed DocumentTextStyle
    // instead of re-assembling font + size + decoration + colour every
    // call site. This is the only "computed" code in the theme — every
    // value reads from the underlying records.

    /**
     * Composed text style for the top-of-document headline — headline
     * font at the headline size in the primary ink colour.
     *
     * @return the headline text style
     */
    public DocumentTextStyle headlineStyle() {
        return style(typography.headlineFont(), typography.sizeHeadline(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    /**
     * Composed text style for the bold spaced-caps section banner
     * label — headline font at the banner size in the primary ink
     * colour.
     *
     * @return the banner text style
     */
    public DocumentTextStyle bannerStyle() {
        return style(typography.headlineFont(), typography.sizeBanner(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    /**
     * Composed text style for the contact line — body font at the
     * contact size in the primary ink colour.
     *
     * @return the contact text style
     */
    public DocumentTextStyle contactStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    /**
     * Composed text style for the separator glyph between contact
     * items — body font at the contact size in the quieter rule
     * colour.
     *
     * @return the contact-separator text style
     */
    public DocumentTextStyle contactSeparatorStyle() {
        return style(typography.bodyFont(), typography.sizeContact(),
                DocumentTextDecoration.DEFAULT, palette.rule());
    }

    /**
     * Composed text style for body prose — body font at the body size
     * in the primary ink colour.
     *
     * @return the body text style
     */
    public DocumentTextStyle bodyStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    /**
     * Composed text style for emphasised body text — body font at the
     * body size, bold, in the primary ink colour.
     *
     * @return the bold body text style
     */
    public DocumentTextStyle bodyBoldStyle() {
        return style(typography.bodyFont(), typography.sizeBody(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    /**
     * Composed text style for an entry title (job title, degree) —
     * body font at the entry-title size, bold, in the primary ink
     * colour.
     *
     * @return the entry-title text style
     */
    public DocumentTextStyle entryTitleStyle() {
        return style(typography.bodyFont(), typography.sizeEntryTitle(),
                DocumentTextDecoration.BOLD, palette.ink());
    }

    /**
     * Composed text style for the right-aligned entry date column —
     * body font at the entry-date size in the primary ink colour.
     *
     * @return the entry-date text style
     */
    public DocumentTextStyle entryDateStyle() {
        return style(typography.bodyFont(), typography.sizeEntryDate(),
                DocumentTextDecoration.DEFAULT, palette.ink());
    }

    /**
     * Composed text style for an italic entry subtitle (employer,
     * institution) — body font at the entry-subtitle size, italic, in
     * the muted secondary colour.
     *
     * @return the entry-subtitle text style
     */
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
