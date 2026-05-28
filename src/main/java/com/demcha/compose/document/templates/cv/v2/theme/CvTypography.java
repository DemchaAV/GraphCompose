package com.demcha.compose.document.templates.cv.v2.theme;

import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Font + size scale tokens for a {@link CvTheme}.
 *
 * <p>Each field names <strong>what</strong> renders at that size, not
 * an abstract "small / medium / large" — that way the call site
 * ({@code theme.typography().sizeEntryDate()}) reads as the renderer's
 * intent and a future scale-rebalance affects only the values, never
 * the field names.</p>
 *
 * @param headlineFont      font for the top-of-document spaced-caps
 *                          name
 * @param bodyFont          font for everything else (body prose,
 *                          subtitles, banner labels, …)
 * @param sizeHeadline      top-of-document name size
 * @param sizeContact       contact-line size (phone / email / links)
 * @param sizeBanner        spaced-caps section banner label size
 * @param sizeEntryTitle    bold entry title (job title, degree) size
 * @param sizeEntryDate     right-aligned date column size
 * @param sizeEntrySubtitle italic subtitle (employer, institution)
 *                          size
 * @param sizeBody          paragraph / bullet / key-value body size
 * @param bodyLineSpacing   line-spacing multiplier for body
 *                          paragraphs (typically 1.4)
 */
public record CvTypography(
        FontName headlineFont,
        FontName bodyFont,
        double sizeHeadline,
        double sizeContact,
        double sizeBanner,
        double sizeEntryTitle,
        double sizeEntryDate,
        double sizeEntrySubtitle,
        double sizeBody,
        double bodyLineSpacing) {

    public CvTypography {
        Objects.requireNonNull(headlineFont, "headlineFont");
        Objects.requireNonNull(bodyFont, "bodyFont");
    }

    /**
     * The classic PT-Serif scale used by the original Boxed Sections
     * preset.
     */
    public static CvTypography classic() {
        return new CvTypography(
                FontName.PT_SERIF, FontName.PT_SERIF,
                21.5,    // headline
                8.5,     // contact
                9.6,     // banner
                9.2,     // entry title
                8.8,     // entry date
                8.4,     // entry subtitle
                8.6,     // body
                1.4);    // line spacing
    }

    /**
     * Helvetica scale for the Modern Professional preset — larger
     * display name, larger section titles, comfortable body size.
     */
    public static CvTypography modernProfessional() {
        return new CvTypography(
                FontName.HELVETICA_BOLD, FontName.HELVETICA,
                28.0,    // headline (display name)
                9.0,     // contact
                17.4,    // banner (used as section title here)
                10.5,    // entry title
                10.0,    // entry date
                9.5,     // entry subtitle
                10.0,    // body
                1.35);   // line spacing
    }

    /**
     * Poppins headline + Lato body scale ported from the original
     * {@code CenteredHeadline} v1 preset. The headline is the page's
     * loudest element (24pt spaced caps); everything else is at
     * 8-9pt for a classic-resume density.
     *
     * <p>{@code sizeBanner} feeds the
     * {@link com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader#flatSpacedCaps}
     * variant — small bold spaced-caps title in the soft palette
     * tone.</p>
     */
    public static CvTypography centeredHeadline() {
        return new CvTypography(
                FontName.POPPINS, FontName.LATO,
                24.0,    // headline (spaced-caps name)
                8.3,     // contact
                9.5,     // banner (used as small spaced-caps section title)
                8.8,     // entry title
                8.6,     // entry date
                8.4,     // entry subtitle
                8.7,     // body
                1.45);   // line spacing
    }

    /**
     * PT-Serif scale for the {@code ClassicSerif} preset: large
     * editorial masthead, quiet metadata, and compact detail entries.
     * Preset-local body variants still override this when the visual
     * needs a distinct summary size.
     */
    public static CvTypography classicSerif() {
        return new CvTypography(
                FontName.PT_SERIF, FontName.PT_SERIF,
                27.0,    // headline
                8.7,     // contact
                9.2,     // section title
                9.2,     // entry title
                8.7,     // entry date
                8.7,     // entry subtitle
                9.0,     // body
                1.35);   // line spacing
    }

    /**
     * Barlow headline + Lato body scale for the {@code NordicClean}
     * preset. Compact sizes keep the two-column rail/body layout
     * single-page friendly while preserving the crisp editorial feel.
     */
    public static CvTypography nordicClean() {
        return new CvTypography(
                FontName.BARLOW, FontName.LATO,
                27.0,    // headline
                7.4,     // contact stack
                7.6,     // section title
                8.0,     // entry title
                7.35,    // entry date
                7.2,     // entry subtitle
                7.45,    // body
                1.12);   // line spacing
    }

    /**
     * IBM Plex Mono headline + Lato body scale for the Compact Mono
     * preset. The section-title slot also uses the mono headline font
     * so tick labels keep the terminal/card visual signature.
     */
    public static CvTypography compactMono() {
        return new CvTypography(
                FontName.IBM_PLEX_MONO, FontName.LATO,
                23.5,    // headline
                8.3,     // contact
                8.0,     // section tick label
                8.45,    // entry title
                7.8,     // entry date
                7.65,    // entry subtitle
                8.1,     // body
                1.16);   // line spacing
    }

    /**
     * Compact PT-Serif headline + Lato body scale used by the Blue
     * Banner preset.
     */
    public static CvTypography blueBanner() {
        return new CvTypography(
                FontName.PT_SERIF, FontName.LATO,
                20.0,    // headline
                7.5,     // contact
                7.3,     // banner
                8.0,     // entry title
                7.7,     // entry date
                7.45,    // entry subtitle
                7.7,     // body
                1.3);    // line spacing
    }

    /**
     * Compact Helvetica scale for the Editorial Blue preset.
     */
    public static CvTypography editorialBlue() {
        return new CvTypography(
                FontName.HELVETICA_BOLD, FontName.HELVETICA,
                22.0,    // headline
                9.0,     // contact
                11.0,    // section title
                10.6,    // entry title
                10.0,    // entry date
                9.2,     // entry subtitle
                9.4,     // body
                1.45);   // line spacing
    }

    /**
     * Crimson Text headline + Lato body scale ported from the v1
     * {@code SidebarPortraitCvTemplateComposer}: 28pt serif hero name,
     * 12pt main section title, 10pt body, and 8-9pt sidebar metadata.
     * The sample data is information-dense, so the body sizes are
     * compact and the line spacing trends to 1.35 for readability.
     */
    public static CvTypography sidebarPortrait() {
        return new CvTypography(
                FontName.CRIMSON_TEXT, FontName.LATO,
                28.0,    // headline (hero name)
                8.3,     // contact (sidebar contact stack)
                12.0,    // banner / main section title
                10.0,    // entry title (experience position)
                8.4,     // entry date (subtitle slot also)
                8.0,     // entry subtitle
                9.4,     // body (profile / experience body)
                1.35);   // line spacing
    }

    /**
     * Crimson Text headline + Lato body scale ported from the v1
     * {@code MonogramSidebarCvTemplateComposer}: 30pt spaced-caps
     * name in the main column, 9pt section title rule, and 7.2-7.8pt
     * body sizes across the pale-teal sidebar and the right column.
     * The PT-Serif monogram font is preset-local because no other v2
     * preset uses it for a circle-ring badge.
     */
    public static CvTypography monogramSidebar() {
        return new CvTypography(
                FontName.CRIMSON_TEXT, FontName.LATO,
                30.0,    // headline (spaced-caps name lines)
                7.4,     // contact (sidebar contact stack)
                9.0,     // banner / main section title
                7.8,     // entry title (experience position)
                7.4,     // entry date (accent gold)
                7.4,     // entry subtitle
                7.5,     // body (profile + experience description)
                1.35);   // line spacing
    }

    /**
     * Barlow headline + Lato body scale ported from the v1
     * {@code TechLeadCvTemplateComposer}: 24.5pt UPPERCASE masthead,
     * 7.8pt main section headings, 7.25pt body, and a 1.08 line
     * spacing tuned for dense engineering-resume cards.
     */
    public static CvTypography engineeringResume() {
        return new CvTypography(
                FontName.BARLOW, FontName.LATO,
                24.5,    // headline (UPPERCASE name in navy header)
                7.2,     // contact (right-aligned navy header stack)
                7.8,     // banner / main section heading
                8.0,     // entry title (role title in experience cards)
                7.1,     // entry date
                7.0,     // entry subtitle (employer)
                7.25,    // body
                1.08);   // line spacing
    }

    /**
     * Barlow Condensed headline + Lato body scale ported from the v1
     * {@code TimelineMinimalCvTemplateComposer}: 28pt spaced-caps
     * masthead, 12.5pt sidebar module titles, 13.5pt main module
     * titles, 7.5-7.9pt body. Compact sizes squeeze the 3-column
     * sidebar / axis / main layout onto one page.
     */
    public static CvTypography timelineMinimal() {
        return new CvTypography(
                FontName.BARLOW_CONDENSED, FontName.LATO,
                28.0,    // headline (spaced uppercase masthead)
                7.8,     // contact (right-aligned contact stack)
                13.5,    // banner / main module title
                12.5,    // entry title (reused as sidebar module title size)
                7.9,     // entry date (body size in main)
                7.5,     // entry subtitle (sidebar body size)
                7.8,     // body
                1.2);    // line spacing
    }

    /**
     * Poppins headline + Lato body scale ported from the v1
     * {@code PanelCvTemplateComposer} (ProductLeader tokens): a 22pt
     * uppercase name in the tinted header card, a 10.4pt section-title
     * slot for the teal module headings, and a 9.4pt body with 1.2
     * line spacing tuned for the dense card layout.
     */
    public static CvTypography panel() {
        return new CvTypography(
                FontName.POPPINS, FontName.LATO,
                22.0,    // headline (centered uppercase name)
                8.9,     // contact (V1 META_SIZE = body - 0.5)
                10.4,    // banner / module title (V1 SECTION_SIZE)
                9.4,     // entry title
                9.4,     // entry date
                9.0,     // entry subtitle (italic)
                9.4,     // body (V1 BODY_SIZE)
                1.2);    // line spacing
    }

    /**
     * Poppins headline + Lato body scale ported from the v1
     * {@code ExecutiveSlateCvTemplate}: a 24pt uppercase masthead, a
     * 10.8pt section-title slot driving the bronze module headings,
     * and a 9.5pt body with 1.25 line-spacing tuned for an executive
     * single-column resume density.
     */
    public static CvTypography executive() {
        return new CvTypography(
                FontName.POPPINS, FontName.LATO,
                24.0,    // headline (uppercase masthead)
                9.1,     // contact meta (V1 META_SIZE = body - 0.4)
                10.8,    // banner / section title (V1 SECTION_SIZE)
                9.5,     // entry title
                9.5,     // entry date
                9.0,     // entry subtitle (italic)
                9.5,     // body (V1 BODY_SIZE)
                1.25);   // line spacing
    }
}
