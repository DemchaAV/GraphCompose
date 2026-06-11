package com.demcha.compose.document.templates.themes;

import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Typography token set used by Templates v2 presets.
 *
 * <p>Pairs a header font and a body font with five logical type sizes
 * (display name, primary heading, secondary heading, body, small body,
 * caption). Presets read these tokens directly rather than hard-coding
 * font names or sizes, so a preset can swap typography by swapping a
 * {@code Typography} value without touching layout or styling code.</p>
 *
 * <p>Five built-in presets cover the common sans-serif / serif / monospace
 * / display combinations; use {@link #builder()} for fully custom typography.</p>
 *
 * @param headerFont     font name applied to the document name and section headings
 * @param bodyFont       font name applied to body text and list items
 * @param nameSize       type size for the top-of-document name (largest)
 * @param headingSize    type size for primary section headings
 * @param subHeadingSize type size for secondary headings (e.g. role titles)
 * @param bodySize       type size for paragraph body text
 * @param smallBodySize  type size for compact body text (contact lines, captions)
 * @param captionSize    type size for the smallest auxiliary text
 */
public record Typography(
        FontName headerFont,
        FontName bodyFont,
        double nameSize,
        double headingSize,
        double subHeadingSize,
        double bodySize,
        double smallBodySize,
        double captionSize) {

    /**
     * Compact constructor that validates non-null fonts and positive type sizes.
     *
     * @throws NullPointerException     if {@code headerFont} or {@code bodyFont} is null
     * @throws IllegalArgumentException if any type size is non-positive,
     *                                  {@code NaN}, or infinite
     */
    public Typography {
        Objects.requireNonNull(headerFont, "headerFont");
        Objects.requireNonNull(bodyFont, "bodyFont");
        validate(nameSize, "nameSize");
        validate(headingSize, "headingSize");
        validate(subHeadingSize, "subHeadingSize");
        validate(bodySize, "bodySize");
        validate(smallBodySize, "smallBodySize");
        validate(captionSize, "captionSize");
    }

    /**
     * Returns the default Helvetica-based typography matching the historical
     * {@code CvTheme.defaultTheme()} sizing.
     *
     * @return Helvetica typography preset
     */
    public static Typography helvetica() {
        return new Typography(
                FontName.HELVETICA,
                FontName.HELVETICA,
                /* nameSize       */ 28.0,
                /* headingSize    */ 17.4,
                /* subHeadingSize */ 12.0,
                /* bodySize       */ 10.0,
                /* smallBodySize  */ 9.0,
                /* captionSize    */ 8.0);
    }

    /**
     * Returns Times-Roman typography for templates that read formal /
     * editorial.
     *
     * @return Times-Roman typography preset
     */
    public static Typography timesRoman() {
        return new Typography(
                FontName.TIMES_ROMAN,
                FontName.TIMES_ROMAN,
                /* nameSize       */ 28.0,
                /* headingSize    */ 17.4,
                /* subHeadingSize */ 12.0,
                /* bodySize       */ 10.0,
                /* smallBodySize  */ 9.0,
                /* captionSize    */ 8.0);
    }

    /**
     * Returns a serif-paired typography (Times-Roman header, Helvetica body)
     * for templates that want a formal heading on a clean body.
     *
     * @return mixed serif/sans typography preset
     */
    public static Typography serifMixed() {
        return new Typography(
                FontName.TIMES_ROMAN,
                FontName.HELVETICA,
                /* nameSize       */ 28.0,
                /* headingSize    */ 17.4,
                /* subHeadingSize */ 12.0,
                /* bodySize       */ 10.0,
                /* smallBodySize  */ 9.0,
                /* captionSize    */ 8.0);
    }

    /**
     * Returns a Courier-based monospace typography for tech / engineering
     * resume templates that want a code-flavoured aesthetic.
     *
     * @return monospace typography preset
     */
    public static Typography monospace() {
        return new Typography(
                FontName.COURIER,
                FontName.COURIER,
                /* nameSize       */ 26.0,
                /* headingSize    */ 14.0,
                /* subHeadingSize */ 11.0,
                /* bodySize       */ 9.5,
                /* smallBodySize  */ 8.5,
                /* captionSize    */ 8.0);
    }

    /**
     * Returns a display typography with larger heading sizes — suitable for
     * banner-style or hero-heading templates that want headings to dominate.
     *
     * @return display typography preset
     */
    public static Typography display() {
        return new Typography(
                FontName.HELVETICA,
                FontName.HELVETICA,
                /* nameSize       */ 34.0,
                /* headingSize    */ 20.0,
                /* subHeadingSize */ 13.0,
                /* bodySize       */ 10.0,
                /* smallBodySize  */ 9.0,
                /* captionSize    */ 8.0);
    }

    /**
     * Returns a fresh builder seeded with {@link #helvetica()} defaults.
     *
     * @return new typography builder
     */
    public static Builder builder() {
        return new Builder(helvetica());
    }

    /**
     * Returns a builder seeded with this instance's tokens, suitable for
     * creating slight variants of an existing typography.
     *
     * @return new typography builder pre-populated from this instance
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    private static void validate(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }

    /**
     * Mutable builder for {@link Typography}. Every setter returns {@code this}
     * for chaining; call {@link #build()} to produce an immutable record.
     */
    public static final class Builder {
        private FontName headerFont;
        private FontName bodyFont;
        private double nameSize;
        private double headingSize;
        private double subHeadingSize;
        private double bodySize;
        private double smallBodySize;
        private double captionSize;

        private Builder(Typography seed) {
            this.headerFont = seed.headerFont;
            this.bodyFont = seed.bodyFont;
            this.nameSize = seed.nameSize;
            this.headingSize = seed.headingSize;
            this.subHeadingSize = seed.subHeadingSize;
            this.bodySize = seed.bodySize;
            this.smallBodySize = seed.smallBodySize;
            this.captionSize = seed.captionSize;
        }

        /**
         * Sets the font used for the document name and section headings.
         *
         * @param font non-null font name
         * @return this builder
         */
        public Builder headerFont(FontName font) {
            this.headerFont = Objects.requireNonNull(font, "headerFont");
            return this;
        }

        /**
         * Sets the font used for body text and list items.
         *
         * @param font non-null font name
         * @return this builder
         */
        public Builder bodyFont(FontName font) {
            this.bodyFont = Objects.requireNonNull(font, "bodyFont");
            return this;
        }

        /**
         * Sets both header and body fonts to the same value, for
         * single-font typography.
         *
         * @param font non-null font name applied to both header and body
         * @return this builder
         */
        public Builder uniformFont(FontName font) {
            Objects.requireNonNull(font, "font");
            this.headerFont = font;
            this.bodyFont = font;
            return this;
        }

        /**
         * Sets the size for the top-of-document name.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder nameSize(double value) {
            this.nameSize = value;
            return this;
        }

        /**
         * Sets the size for primary section headings.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder headingSize(double value) {
            this.headingSize = value;
            return this;
        }

        /**
         * Sets the size for secondary headings.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder subHeadingSize(double value) {
            this.subHeadingSize = value;
            return this;
        }

        /**
         * Sets the size for paragraph body text.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder bodySize(double value) {
            this.bodySize = value;
            return this;
        }

        /**
         * Sets the size for compact body text such as contact lines.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder smallBodySize(double value) {
            this.smallBodySize = value;
            return this;
        }

        /**
         * Sets the size for the smallest auxiliary text.
         *
         * @param value positive finite size in points
         * @return this builder
         */
        public Builder captionSize(double value) {
            this.captionSize = value;
            return this;
        }

        /**
         * Builds an immutable {@link Typography} record. Fonts are checked
         * for non-nullness and sizes for positivity here.
         *
         * @return new typography record
         * @throws IllegalArgumentException if any size is non-positive
         * @throws NullPointerException     if a font is {@code null}
         */
        public Typography build() {
            return new Typography(
                    headerFont,
                    bodyFont,
                    nameSize,
                    headingSize,
                    subHeadingSize,
                    bodySize,
                    smallBodySize,
                    captionSize);
        }
    }
}
