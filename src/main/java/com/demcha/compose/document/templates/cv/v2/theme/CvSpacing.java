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
     * Spacing for Classic Serif: a measured editorial flow with a
     * framed profile band, quiet cover skills module, and compact
     * detail modules.
     */
    public static CvSpacing classicSerif() {
        return new CvSpacing(
                8,                                       // pageFlowSpacing
                4,                                       // sectionBodySpacing
                DocumentInsets.zero(),                   // sectionBodyPadding
                new DocumentInsets(8, 0, 7, 0),          // headlinePadding
                DocumentInsets.zero(),                   // contactPadding
                0.0,                                     // bannerCornerRadius (unused)
                5.0,                                     // bannerInnerPadding (unused)
                DocumentInsets.zero(),                   // bannerMargin (unused)
                0.65,                                    // accentRuleWidth
                1.0,                                     // paragraphMarginTop
                12.0,                                    // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.36,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }

    /**
     * Spacing for Nordic Clean: compact top header, soft profile band,
     * and a dense two-column body with a tinted sidebar rail.
     */
    public static CvSpacing nordicClean() {
        return new CvSpacing(
                7,                                       // pageFlowSpacing
                3,                                       // sectionBodySpacing
                DocumentInsets.zero(),                   // sectionBodyPadding
                new DocumentInsets(1, 0, 2, 0),          // headlinePadding
                new DocumentInsets(3, 0, 0, 0),          // contactPadding
                4.0,                                     // bannerCornerRadius
                5.0,                                     // bannerInnerPadding
                DocumentInsets.zero(),                   // bannerMargin
                1.1,                                     // accentRuleWidth
                1.5,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }

    /**
     * Spacing for Compact Mono: command-bar header, dense rail
     * modules, and same-width cards in the right column.
     */
    public static CvSpacing compactMono() {
        return new CvSpacing(
                9,                                       // pageFlowSpacing
                3.5,                                     // sectionBodySpacing
                DocumentInsets.zero(),                   // sectionBodyPadding
                DocumentInsets.zero(),                   // headlinePadding
                DocumentInsets.zero(),                   // contactPadding
                3.0,                                     // bannerCornerRadius
                0.0,                                     // bannerInnerPadding
                DocumentInsets.zero(),                   // bannerMargin
                2.2,                                     // accentRuleWidth
                1.0,                                     // paragraphMarginTop
                7.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.4,                                     // entryDateWeight
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

    /**
     * Compact spacing for Editorial Blue: section headers own their
     * rule/title rhythm, while bodies start close to the lower rule.
     */
    public static CvSpacing editorialBlue() {
        return new CvSpacing(
                0,                                       // pageFlowSpacing
                2,                                       // sectionBodySpacing
                new DocumentInsets(8, 0, 0, 0),          // sectionBodyPadding
                new DocumentInsets(2, 0, 2, 0),          // headlinePadding
                new DocumentInsets(1, 0, 0, 0),          // contactPadding
                0.0,                                     // bannerCornerRadius
                0.0,                                     // bannerInnerPadding
                DocumentInsets.zero(),                   // bannerMargin
                0.6,                                     // accentRuleWidth
                1.0,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }

    /**
     * Spacing for the Panel preset: card-led layout that has to fit
     * Header / Profile / two-column row / Additional on one A4 page,
     * so paddings and inter-card gaps are tight by design. Corner
     * radius and accent rule width match the V1 ProductLeader tokens.
     */
    public static CvSpacing panel() {
        return new CvSpacing(
                6,                                       // pageFlowSpacing (tight inter-card gap)
                3,                                       // sectionBodySpacing (inside a card)
                DocumentInsets.zero(),                   // sectionBodyPadding (the card supplies its own padding)
                DocumentInsets.zero(),                   // headlinePadding
                DocumentInsets.zero(),                   // contactPadding
                7.0,                                     // bannerCornerRadius (V1 CORNER_RADIUS)
                8.0,                                     // bannerInnerPadding (compact card padding)
                DocumentInsets.zero(),                   // bannerMargin
                2.2,                                     // accentRuleWidth (V1 ACCENT_HEIGHT)
                1.0,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                2.0);                                    // entrySeparation
    }

    /**
     * Spacing for the Executive preset: generous executive feel with
     * an 8pt page-flow rhythm, compact module bodies, and a 1.1pt
     * full-width rule under the masthead.
     */
    public static CvSpacing executive() {
        return new CvSpacing(
                8,                                       // pageFlowSpacing
                3,                                       // sectionBodySpacing
                DocumentInsets.zero(),                   // sectionBodyPadding
                DocumentInsets.zero(),                   // headlinePadding
                DocumentInsets.top(2),                   // contactPadding (unused — preset composes header inline)
                0.0,                                     // bannerCornerRadius (unused)
                5.0,                                     // bannerInnerPadding (unused)
                DocumentInsets.zero(),                   // bannerMargin (unused)
                1.1,                                     // accentRuleWidth (V1 header rule)
                2.0,                                     // paragraphMarginTop
                8.0,                                     // entryHeaderRowSpacing
                1.0,                                     // entryTitleWeight
                0.45,                                    // entryDateWeight
                3.0);                                    // entrySeparation
    }
}
