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
}
