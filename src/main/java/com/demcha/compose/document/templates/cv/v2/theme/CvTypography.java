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
}
