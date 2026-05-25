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
}
