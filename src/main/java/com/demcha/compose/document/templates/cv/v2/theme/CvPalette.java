package com.demcha.compose.document.templates.cv.v2.theme;

import com.demcha.compose.document.style.DocumentColor;

import java.util.Objects;

/**
 * Colour tokens for a {@link CvTheme}.
 *
 * @param ink    primary text colour — headlines, body, entry titles
 * @param muted  secondary text colour — italic subtitles (employer,
 *               institution)
 * @param rule   thin horizontal rules + the contact-line pipe glyph
 * @param banner pale fill behind section title banners
 */
public record CvPalette(DocumentColor ink,
                        DocumentColor muted,
                        DocumentColor rule,
                        DocumentColor banner) {

    public CvPalette {
        Objects.requireNonNull(ink, "ink");
        Objects.requireNonNull(muted, "muted");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(banner, "banner");
    }

    /**
     * The classic dark-grey / pale-grey palette used by the original
     * Boxed Sections preset.
     */
    public static CvPalette classic() {
        return new CvPalette(
                DocumentColor.rgb(34, 34, 34),
                DocumentColor.rgb(120, 120, 120),
                DocumentColor.rgb(170, 170, 170),
                DocumentColor.rgb(220, 226, 230));
    }

    /**
     * Soft greyscale palette ported from the original
     * {@code CenteredHeadline} v1 preset — slightly warmer than
     * {@link #classic()} with a higher-contrast headline tone and a
     * paler rule colour suited to thin full-width separators.
     *
     * <p>The {@code banner} slot is required by the record but unused
     * by the Centered Headline visual signature (no banner panels);
     * we reuse the classic banner colour so themes can swap the body
     * style without leaving an obvious gap if a future preset reuses
     * this palette with a banner-style section header.</p>
     */
    public static CvPalette centeredHeadline() {
        return new CvPalette(
                DocumentColor.rgb(54, 54, 54),     // ink (#363636)
                DocumentColor.rgb(105, 105, 105),  // muted / soft (#696969)
                DocumentColor.rgb(188, 188, 188),  // rule (#BCBCBC)
                DocumentColor.rgb(220, 226, 230)); // banner (inherits classic)
    }

    /**
     * Warm serif palette ported from the original {@code ClassicSerif}
     * v1 preset. The banner slot carries the soft cream fill used by
     * its profile band; the bronze accent is preset-local because no
     * other preset shares that fifth colour token today.
     */
    public static CvPalette classicSerif() {
        return new CvPalette(
                DocumentColor.rgb(45, 43, 40),
                DocumentColor.rgb(105, 101, 94),
                DocumentColor.rgb(187, 177, 160),
                DocumentColor.rgb(250, 247, 241));
    }

    /**
     * Nordic Clean palette: deep blue-green ink, muted blue-grey
     * metadata, pale teal rules, and the soft profile band fill. The
     * stronger teal accent and rail fill are preset-local fifth/sixth
     * colours because no other preset shares those tokens yet.
     */
    public static CvPalette nordicClean() {
        return new CvPalette(
                DocumentColor.rgb(18, 39, 52),
                DocumentColor.rgb(82, 104, 116),
                DocumentColor.rgb(188, 219, 222),
                DocumentColor.rgb(226, 244, 245));
    }

    /**
     * Compact Mono palette: near-black body ink, blue-grey metadata,
     * quiet card rules, and the pale rail fill used by the compact
     * left column.
     */
    public static CvPalette compactMono() {
        return new CvPalette(
                DocumentColor.rgb(28, 34, 42),
                DocumentColor.rgb(102, 117, 132),
                DocumentColor.rgb(188, 204, 215),
                DocumentColor.rgb(236, 244, 242));
    }

    /**
     * Blue Banner palette: compact dark ink, blue section fills, and
     * darker blue separator rules.
     */
    public static CvPalette blueBanner() {
        return new CvPalette(
                DocumentColor.rgb(20, 25, 35),
                DocumentColor.rgb(85, 85, 85),
                DocumentColor.rgb(58, 82, 118),
                DocumentColor.rgb(112, 146, 190));
    }

    /**
     * Editorial Blue palette: deep blue-grey body text, muted
     * subtitles, vivid blue rules, and a neutral border token reused
     * by compact skill grids.
     */
    public static CvPalette editorialBlue() {
        return new CvPalette(
                DocumentColor.rgb(60, 72, 106),
                DocumentColor.rgb(150, 158, 178),
                DocumentColor.rgb(86, 136, 255),
                DocumentColor.rgb(193, 201, 211));
    }

    /**
     * Timeline Minimal palette: an all-grey scale ported from the v1
     * {@code TimelineMinimalCvTemplateComposer} — medium-grey ink,
     * softer grey for metadata + body bullets, pale rule for the
     * timeline axis and module underlines, and the dot token reused
     * for the three circles of the central timeline axis.
     */
    public static CvPalette timelineMinimal() {
        return new CvPalette(
                DocumentColor.rgb(74, 74, 74),     // ink — V1 INK
                DocumentColor.rgb(122, 122, 122),  // muted — V1 SOFT
                DocumentColor.rgb(195, 195, 195),  // rule — V1 RULE
                DocumentColor.rgb(170, 170, 170)); // banner — V1 DOT (reused as "timeline accent")
    }

    /**
     * Panel palette ported from the v1 {@code PanelCvTemplateComposer}
     * (ProductLeader tokens): body slate ink, slightly lighter slate
     * for italic subtitles, the pale teal stroke used by every panel
     * border, and the pale teal header card fill. The deeper header
     * navy (rgb(20,44,66)), teal accent (rgb(0,128,128)), and white
     * panel fill are preset-local because they are the fifth/sixth/
     * seventh tokens — other v2 presets do not share them today.
     */
    public static CvPalette panel() {
        return new CvPalette(
                DocumentColor.rgb(54, 68, 84),     // ink — V1 BODY_TEXT/HEADER_META slate
                DocumentColor.rgb(105, 117, 132),  // muted — slightly lighter slate
                DocumentColor.rgb(179, 214, 211),  // rule — V1 PANEL_STROKE pale teal
                DocumentColor.rgb(231, 246, 244)); // banner — V1 HEADER_FILL pale teal
    }

    /**
     * Executive palette ported from the v1 {@code ExecutiveSlateCvTemplate}:
     * mid-slate body ink, soft muted slate for italic subtitles, the
     * V1 muted-rule grey for thin separators, and a fallback banner
     * tone inherited from the classic palette (the preset does not
     * draw banner panels). The display name colour (deeper slate
     * rgb(24,35,51)) and bronze accent (rgb(172,112,55)) are
     * preset-local because they are the fifth and sixth tokens —
     * other v2 presets do not share them today.
     */
    public static CvPalette executive() {
        return new CvPalette(
                DocumentColor.rgb(49, 58, 72),     // ink — V1 BODY slate
                DocumentColor.rgb(105, 115, 130),  // muted — slightly lighter slate
                DocumentColor.rgb(193, 201, 211),  // rule — V1 MUTED_RULE
                DocumentColor.rgb(220, 226, 230)); // banner — unused, inherits classic
    }
}
