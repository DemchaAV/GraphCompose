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
 * @param entrySeparation       vertical spacer (in points) inserted
 *                              <strong>between</strong> consecutive
 *                              entries in an {@code EntriesSection}
 *                              and between consecutive rows of a
 *                              {@code RowsSection} in
 *                              {@code BULLETED_STACKED} style — so
 *                              the reader can tell where one entry
 *                              ends and the next begins. Not applied
 *                              before the first entry in a section.
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
        double entryDateWeight,
        double entrySeparation) {

    public CvSpacing {
        Objects.requireNonNull(sectionBodyPadding, "sectionBodyPadding");
        Objects.requireNonNull(headlinePadding, "headlinePadding");
        Objects.requireNonNull(contactPadding, "contactPadding");
        Objects.requireNonNull(bannerMargin, "bannerMargin");
    }

    /**
     * Backward-compatible 13-arg constructor — fills
     * {@link #entrySeparation} with the canonical default
     * ({@code 6.0}) so callers built before this field was added keep
     * compiling and rendering the same density as before, plus an
     * automatic improvement: a small gap between consecutive entries.
     *
     * @deprecated since {@code entrySeparation} was introduced.
     *             Supply it explicitly via the 14-arg canonical
     *             constructor or via {@link #classic()} /
     *             {@link #modernProfessional()}.
     */
    @Deprecated
    public CvSpacing(double pageFlowSpacing,
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
        this(pageFlowSpacing, sectionBodySpacing, sectionBodyPadding,
                headlinePadding, contactPadding, bannerCornerRadius,
                bannerInnerPadding, bannerMargin, accentRuleWidth,
                paragraphMarginTop, entryHeaderRowSpacing,
                entryTitleWeight, entryDateWeight, 3.0);
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
                0.45,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }

    /**
     * Spacing for the {@code CenteredHeadline} preset — designed for a
     * classic single-column resume with thin full-width rules above
     * and below the contact line, and small inter-module rules between
     * sections. Banner-panel fields are left at sensible defaults but
     * unused (the preset uses {@code flatSpacedCaps} section headers,
     * not banners).
     */
    public static CvSpacing centeredHeadline() {
        return new CvSpacing(
                0,                                       // pageFlowSpacing (zero — rules supply visual gaps)
                1.5,                                     // sectionBodySpacing
                DocumentInsets.zero(),                   // sectionBodyPadding
                new DocumentInsets(8, 0, 0, 0),          // headlinePadding (small top breathing room)
                new DocumentInsets(7, 0, 7, 0),          // contactPadding
                0.0,                                     // bannerCornerRadius (unused)
                5.0,                                     // bannerInnerPadding (unused)
                DocumentInsets.top(0),                   // bannerMargin (unused)
                0.55,                                    // accentRuleWidth (thin v1 rule)
                1.2,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }

    /**
     * Tighter spacing for the Modern Professional preset — no banner
     * panels, denser body, single-page-friendly proportions.
     * Banner-related fields (corner radius, inner padding, margin)
     * are left non-zero so a future preset that wants to draw an MP
     * banner can read them; the canonical MP preset ignores them.
     */
    public static CvSpacing modernProfessional() {
        return new CvSpacing(
                4,                                       // pageFlowSpacing
                3,                                       // sectionBodySpacing
                new DocumentInsets(2, 0, 0, 12),         // sectionBodyPadding (left=12 → body indents from blue section title)
                new DocumentInsets(0, 0, 0, 0),          // headlinePadding
                new DocumentInsets(0, 0, 6, 0),          // contactPadding
                0.0,                                     // bannerCornerRadius (unused)
                5.0,                                     // bannerInnerPadding (unused)
                DocumentInsets.top(6),                   // bannerMargin (unused — section title margin)
                0.7,                                     // accentRuleWidth
                2.0,                                     // paragraphMarginTop
                10.0,                                    // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                2.5);                                    // entrySeparation
    }

    /**
     * Compact spacing for Blue Banner: tight body blocks, full-width
     * title banners, and no extra artificial gap between entries.
     */
    public static CvSpacing blueBanner() {
        return new CvSpacing(
                4,                                       // pageFlowSpacing
                3,                                       // sectionBodySpacing
                new DocumentInsets(3, 4, 0, 4),          // sectionBodyPadding
                new DocumentInsets(8, 0, 8, 0),          // headlinePadding
                new DocumentInsets(1.5, 0, 1.5, 0),      // contactPadding
                0.0,                                     // bannerCornerRadius
                3.2,                                     // bannerInnerPadding
                DocumentInsets.zero(),                   // bannerMargin
                0.55,                                    // accentRuleWidth
                1.2,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.4,                                     // entryDateWeight
                0.0);                                    // entrySeparation
    }
}
