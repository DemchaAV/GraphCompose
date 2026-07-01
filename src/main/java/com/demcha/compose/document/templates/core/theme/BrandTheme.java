package com.demcha.compose.document.templates.core.theme;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Aggregate cosmetic theme — palette + typography + spacing — passed
 * to every component renderer in {@code cv/components}.
 *
 * <p>This is the <strong>only</strong> place a CV preset reads colour,
 * font, size, or spacing values from. Renderers never inline literal
 * RGB tuples, font names, or magic numbers.</p>
 *
 * <p>To define a new visual flavour: add a static factory here
 * returning a fresh {@code BrandTheme} with custom sub-records. The
 * existing preset code keeps working — only the theme handed to
 * {@code BoxedSections.create(theme)} changes.</p>
 *
 * @param palette    colour tokens
 * @param typography font + size scale
 * @param spacing    paddings / margins / weights
 * @param decoration glyph / separator tokens
 */
public record BrandTheme(Palette palette,
                      Typography typography,
                      Spacing spacing,
                      Decoration decoration) {

    /**
     * Validates that no sub-record is null.
     */
    public BrandTheme {
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(typography, "typography");
        Objects.requireNonNull(spacing, "spacing");
        Objects.requireNonNull(decoration, "decoration");
    }

    /**
     * Backward-compatible 3-arg constructor that fills the
     * {@link Decoration} slot with {@link Decoration#classic()}.
     * Retained so callers built before the decoration token landed
     * keep compiling and behaving identically.
     *
     * @param palette    colour tokens
     * @param typography font + size scale
     * @param spacing    paddings / margins / weights
     * @deprecated since the introduction of {@link Decoration} —
     * pass an explicit decoration so callers can choose
     * a different bullet glyph or contact separator
     * without forking the renderer.
     */
    @Deprecated
    public BrandTheme(Palette palette, Typography typography, Spacing spacing) {
        this(palette, typography, spacing, Decoration.classic());
    }

    // -- canonical factories ---------------------------------------------

    /**
     * The "Boxed Sections" classic look — PT-Serif, near-black ink,
     * pale-grey section banners, round bullets, pipe contact
     * separators. Visual signature of the original
     * {@code cv-boxed-sections.pdf} reference output.
     *
     * @return a {@code BrandTheme} for the "Boxed Sections" classic look
     */
    public static BrandTheme boxedClassic() {
        return new BrandTheme(
                Palette.classic(),
                Typography.classic(),
                Spacing.classic(),
                Decoration.classic());
    }

    /**
     * The "Modern Professional" look — Helvetica throughout, larger
     * scale, tighter spacing. Body palette is the classic ink/muted
     * pair; the preset itself adds the slate-blue name and
     * bright-blue section title accents because those colours are not
     * shared with any other v2 preset today.
     *
     * <p>When (or if) a second preset wants the same accent palette,
     * extract those colours into a new field on {@link Palette} and
     * point both presets at it.</p>
     *
     * @return a {@code BrandTheme} for the "Modern Professional" look
     */
    public static BrandTheme modernProfessional() {
        return new BrandTheme(
                Palette.classic(),
                Typography.modernProfessional(),
                Spacing.modernProfessional(),
                Decoration.classic());
    }

    /**
     * The "Centered Headline" classic look ported from the v1 preset
     * of the same name — Poppins headline, Lato body, soft greyscale
     * palette, thin full-width rules separating headline / contact /
     * each module. Pipe contact separator matches the classic
     * decoration.
     *
     * @return a {@code BrandTheme} for the "Centered Headline" look
     */
    public static BrandTheme centeredHeadline() {
        return new BrandTheme(
                Palette.centeredHeadline(),
                Typography.centeredHeadline(),
                Spacing.centeredHeadline(),
                Decoration.classic());
    }

    /**
     * The "Classic Serif" look — PT Serif throughout, warm dark ink,
     * tan rules, cream profile band, and the roomy pipe separator
     * from the classic decoration.
     *
     * @return a {@code BrandTheme} for the "Classic Serif" look
     */
    public static BrandTheme classicSerif() {
        return new BrandTheme(
                Palette.classicSerif(),
                Typography.classicSerif(),
                Spacing.classicSerif(),
                Decoration.classic());
    }

    /**
     * The "Nordic Clean" look — Barlow display typography, Lato body,
     * deep blue-green ink, pale teal profile band/rules, and compact
     * two-column spacing.
     *
     * @return a {@code BrandTheme} for the "Nordic Clean" look
     */
    public static BrandTheme nordicClean() {
        return new BrandTheme(
                Palette.nordicClean(),
                Typography.nordicClean(),
                Spacing.nordicClean(),
                Decoration.classic());
    }

    /**
     * The "Compact Mono" look — dark command-bar header, IBM Plex
     * Mono labels, teal accents, pale left rail, and compact card
     * spacing.
     *
     * @return a {@code BrandTheme} for the "Compact Mono" look
     */
    public static BrandTheme compactMono() {
        return new BrandTheme(
                Palette.compactMono(),
                Typography.compactMono(),
                Spacing.compactMono(),
                Decoration.compactMono());
    }

    /**
     * The "Blue Banner" look — PT Serif display name, Lato body,
     * compact spacing, blue full-width section banners, and tighter
     * pipe separators.
     *
     * @return a {@code BrandTheme} for the "Blue Banner" look
     */
    public static BrandTheme blueBanner() {
        return new BrandTheme(
                Palette.blueBanner(),
                Typography.blueBanner(),
                Spacing.blueBanner(),
                Decoration.blueBanner());
    }

    /**
     * The "Editorial Blue" look — compact Helvetica, vivid blue
     * section rules, centred editorial header, and dense body
     * spacing.
     *
     * @return a {@code BrandTheme} for the "Editorial Blue" look
     */
    public static BrandTheme editorialBlue() {
        return new BrandTheme(
                Palette.editorialBlue(),
                Typography.editorialBlue(),
                Spacing.editorialBlue(),
                Decoration.classic());
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
     * @return a {@code BrandTheme} for the "Sidebar Portrait" look
     */
    public static BrandTheme sidebarPortrait() {
        return new BrandTheme(
                Palette.sidebarPortrait(),
                Typography.sidebarPortrait(),
                Spacing.sidebarPortrait(),
                Decoration.classic());
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
     * @return a {@code BrandTheme} for the "Monogram Sidebar" look
     */
    public static BrandTheme monogramSidebar() {
        return new BrandTheme(
                Palette.monogramSidebar(),
                Typography.monogramSidebar(),
                Spacing.monogramSidebar(),
                Decoration.classic());
    }

    /**
     * The "Engineering Resume" look — Barlow display + Lato body, deep
     * navy command header with cyan-green contact links, dark navy
     * skill rail with green accent labels, and white evidence cards
     * for Leadership Experience + Technical Evidence on the right.
     * Visual signature ported from the v1
     * {@code TechLeadCvTemplateComposer}.
     *
     * @return a {@code BrandTheme} for the "Engineering Resume" look
     */
    public static BrandTheme engineeringResume() {
        return new BrandTheme(
                Palette.engineeringResume(),
                Typography.engineeringResume(),
                Spacing.engineeringResume(),
                Decoration.classic());
    }

    /**
     * The "Timeline Minimal" look — Barlow Condensed display + Lato
     * body, all-grey palette, spaced uppercase name, right-aligned
     * contact stack with PNG icons, and a thin vertical timeline axis
     * with three circles separating the sidebar from the main column.
     * Visual signature ported from the v1
     * {@code TimelineMinimalCvTemplateComposer}.
     *
     * @return a {@code BrandTheme} for the "Timeline Minimal" look
     */
    public static BrandTheme timelineMinimal() {
        return new BrandTheme(
                Palette.timelineMinimal(),
                Typography.timelineMinimal(),
                Spacing.timelineMinimal(),
                Decoration.classic());
    }

    /**
     * The "Panel" look — Poppins headlines + Lato body, pale teal
     * header card and module panels with thin teal stroke, deep navy
     * masthead text, and teal section headings with a small accent
     * strip beneath each title. Visual signature ported from the v1
     * {@code PanelCvTemplateComposer} (ProductLeader tokens).
     *
     * @return a {@code BrandTheme} for the "Panel" look
     */
    public static BrandTheme panel() {
        return new BrandTheme(
                Palette.panel(),
                Typography.panel(),
                Spacing.panel(),
                Decoration.classic());
    }

    /**
     * The "Executive" look — Poppins masthead + Lato body, deep slate
     * primary, warm bronze accent on module headings and contact
     * links, and a thin full-width muted rule under the header.
     * Visual signature ported from the legacy
     * {@code ExecutiveSlateCvTemplate}.
     *
     * @return a {@code BrandTheme} for the "Executive" look
     */
    public static BrandTheme executive() {
        return new BrandTheme(
                Palette.executive(),
                Typography.executive(),
                Spacing.executive(),
                Decoration.classic());
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
     * @return a {@code BrandTheme} for the "Mint Editorial" look
     */
    public static BrandTheme mintEditorial() {
        return new BrandTheme(
                Palette.mintEditorial(),
                Typography.mintEditorial(),
                Spacing.mintEditorial(),
                Decoration.classic());
    }

    /**
     * The "Modern Invoice" look — Helvetica on a cream page, a soft-tan
     * rounded hero panel with a gold accent strip, a deep-teal title and
     * table header, and light table rules. Mirrors the cinematic business
     * "modern" theme. The first layered <em>invoice</em> flavour: the
     * invoice presets read it exactly the way the CV presets read their
     * own flavours, so the two families share one theme model.
     *
     * @return a {@code BrandTheme} for the "Modern Invoice" look
     */
    public static BrandTheme invoiceModern() {
        return new BrandTheme(
                Palette.invoiceModern(),
                Typography.invoiceModern(),
                Spacing.invoiceModern(),
                Decoration.classic());
    }

    /**
     * The "Modern Proposal" look — the same cinematic "modern business"
     * surfaces as {@link #invoiceModern()} (cream page, soft-tan panels,
     * deep-teal title + table headers, gold accent) with the richer h1 /
     * h2 / h3 type scale a proposal needs. Drives the cinematic
     * {@code ModernProposal} preset.
     *
     * <p>Reuses the invoice palette + spacing tokens — the two families
     * share one modern business look; a future cleanup may rename those
     * shared factories to a neutral {@code businessModern()}.</p>
     *
     * @return a {@code BrandTheme} for the "Modern Proposal" look
     */
    public static BrandTheme proposalModern() {
        return new BrandTheme(
                Palette.invoiceModern(),
                Typography.proposalModern(),
                Spacing.invoiceModern(),
                Decoration.classic());
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
