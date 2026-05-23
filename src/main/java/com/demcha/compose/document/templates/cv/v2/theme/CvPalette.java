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
}
