package com.demcha.compose.document.templates.cv.v2.theme;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.Objects;

/**
 * Layout / spacing tokens for a {@link CvTheme}.
 *
 * <p>Every magic numeric literal that used to live inside a renderer
 * (padding, margin, gap, weight, accent width) now lives here, so a
 * "compact" or "spacious" variant of the same visual is a new
 * {@code CvSpacing} record — not a forked renderer.</p>
 *
 * @param pageFlowSpacing       gap between top-level page-flow rows
 * @param sectionBodySpacing    gap between paragraphs inside a
 *                              section body
 * @param sectionBodyPadding    inset around section body content
 * @param headlinePadding       inset around the top headline
 * @param contactPadding        inset around the contact line
 * @param bannerCornerRadius    corner radius of the section-title
 *                              banner panel
 * @param bannerInnerPadding    padding inside the banner panel
 * @param bannerMargin          margin around the banner panel
 * @param accentRuleWidth       stroke width of the thin horizontal
 *                              rules under headline / contact
 * @param paragraphMarginTop    top margin applied to body paragraphs
 *                              and rows so consecutive items breathe
 * @param entryHeaderRowSpacing horizontal gap between an entry's
 *                              title column and date column
 * @param entryTitleWeight      flex weight of the entry title column
 * @param entryDateWeight       flex weight of the entry date column
 */
public record CvSpacing(
        double pageFlowSpacing,
        double sectionBodySpacing,
        DocumentInsets sectionBodyPadding,
        DocumentInsets headlinePadding,
        DocumentInsets contactPadding,
        double bannerCornerRadius,
        double bannerInnerPadding,
        DocumentInsets bannerMargin,
        double accentRuleWidth,
        double paragraphMarginTop,
        double entryHeaderRowSpacing,
        double entryTitleWeight,
        double entryDateWeight) {

    public CvSpacing {
        Objects.requireNonNull(sectionBodyPadding, "sectionBodyPadding");
        Objects.requireNonNull(headlinePadding, "headlinePadding");
        Objects.requireNonNull(contactPadding, "contactPadding");
        Objects.requireNonNull(bannerMargin, "bannerMargin");
    }

    /**
     * The classic spacing used by the original Boxed Sections preset.
     */
    public static CvSpacing classic() {
        return new CvSpacing(
                7,                                       // pageFlowSpacing
                4,                                       // sectionBodySpacing
                new DocumentInsets(4, 4, 0, 4),          // sectionBodyPadding
                new DocumentInsets(0, 0, 4, 0),          // headlinePadding
                new DocumentInsets(4, 0, 4, 0),          // contactPadding
                0.0,                                     // bannerCornerRadius
                5.0,                                     // bannerInnerPadding
                DocumentInsets.top(4),                   // bannerMargin
                0.7,                                     // accentRuleWidth
                2.0,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45);                                   // entryDateWeight
    }
}
