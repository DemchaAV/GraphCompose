package com.demcha.compose.document.templates.themes;

import com.demcha.compose.engine.components.style.Margin;

import java.util.Objects;

/**
 * One source of truth for all template spacing tokens.
 *
 * <p>Replaces the historical {@code CvTheme.spacing}/{@code CvTheme.spacingModuleName}
 * fields plus the hard-coded {@code MINIMUM_TOP_LEVEL_MODULE_SPACING}
 * constant scattered throughout the legacy composers. Templates v2 presets
 * read every spacing decision from a {@code Spacing} instance and pass it
 * through the builder.</p>
 *
 * <p>Three preset rhythms cover most cases; use {@link #builder()} for
 * fully custom spacing.</p>
 *
 * @param moduleGap         vertical gap between top-level modules (sections)
 * @param lineSpacing       extra spacing between wrapped lines inside a body block
 * @param paragraphSpacing  vertical gap between paragraphs in a multi-paragraph module
 * @param sectionTitleAbove margin above a section heading (separates it from previous module)
 * @param sectionTitleBelow margin below a section heading (separates it from its body)
 * @param headerLineSpacing vertical gap between contact rows in the document header
 * @param listItemSpacing   vertical gap between list items (bullet, numbered, indented)
 * @param contentPadding    overall padding around the document content area
 */
public record Spacing(
        double moduleGap,
        double lineSpacing,
        double paragraphSpacing,
        double sectionTitleAbove,
        double sectionTitleBelow,
        double headerLineSpacing,
        double listItemSpacing,
        Margin contentPadding) {

    /**
     * Compact constructor that validates non-negativity and finiteness of every
     * numeric token, and rejects {@code null} {@code contentPadding}.
     *
     * @throws IllegalArgumentException if any numeric token is negative,
     *         {@code NaN}, or infinite
     * @throws NullPointerException     if {@code contentPadding} is {@code null}
     */
    public Spacing {
        Objects.requireNonNull(contentPadding, "contentPadding");
        validate(moduleGap, "moduleGap");
        validate(lineSpacing, "lineSpacing");
        validate(paragraphSpacing, "paragraphSpacing");
        validate(sectionTitleAbove, "sectionTitleAbove");
        validate(sectionTitleBelow, "sectionTitleBelow");
        validate(headerLineSpacing, "headerLineSpacing");
        validate(listItemSpacing, "listItemSpacing");
    }

    /**
     * Returns a tight spacing rhythm suitable for single-page CV templates
     * where vertical real estate is at a premium.
     *
     * @return compact spacing tokens
     */
    public static Spacing compact() {
        return new Spacing(
                /* moduleGap         */ 7.0,
                /* lineSpacing       */ 2.0,
                /* paragraphSpacing  */ 4.0,
                /* sectionTitleAbove */ 4.0,
                /* sectionTitleBelow */ 2.0,
                /* headerLineSpacing */ 1.0,
                /* listItemSpacing   */ 2.0,
                /* contentPadding    */ Margin.of(28));
    }

    /**
     * Returns a balanced spacing rhythm — the default for most templates
     * where readability outweighs single-page density.
     *
     * @return comfortable spacing tokens
     */
    public static Spacing comfortable() {
        return new Spacing(
                /* moduleGap         */ 12.0,
                /* lineSpacing       */ 3.0,
                /* paragraphSpacing  */ 6.0,
                /* sectionTitleAbove */ 8.0,
                /* sectionTitleBelow */ 4.0,
                /* headerLineSpacing */ 2.0,
                /* listItemSpacing   */ 3.0,
                /* contentPadding    */ Margin.of(36));
    }

    /**
     * Returns a generous spacing rhythm suitable for proposals, reports,
     * and other documents where breathing room signals quality.
     *
     * @return airy spacing tokens
     */
    public static Spacing airy() {
        return new Spacing(
                /* moduleGap         */ 18.0,
                /* lineSpacing       */ 4.0,
                /* paragraphSpacing  */ 10.0,
                /* sectionTitleAbove */ 12.0,
                /* sectionTitleBelow */ 6.0,
                /* headerLineSpacing */ 3.0,
                /* listItemSpacing   */ 4.0,
                /* contentPadding    */ Margin.of(48));
    }

    /**
     * Returns a fresh builder seeded with {@link #comfortable()} defaults.
     *
     * @return new spacing builder
     */
    public static Builder builder() {
        return new Builder(comfortable());
    }

    /**
     * Returns a builder seeded with this instance's tokens, suitable for
     * creating slight variants of an existing rhythm.
     *
     * @return new spacing builder pre-populated from this instance
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    private static void validate(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative: " + value);
        }
    }

    /**
     * Mutable builder for {@link Spacing}. Every setter returns {@code this}
     * for chaining; call {@link #build()} to produce an immutable record.
     */
    public static final class Builder {
        private double moduleGap;
        private double lineSpacing;
        private double paragraphSpacing;
        private double sectionTitleAbove;
        private double sectionTitleBelow;
        private double headerLineSpacing;
        private double listItemSpacing;
        private Margin contentPadding;

        private Builder(Spacing seed) {
            this.moduleGap = seed.moduleGap;
            this.lineSpacing = seed.lineSpacing;
            this.paragraphSpacing = seed.paragraphSpacing;
            this.sectionTitleAbove = seed.sectionTitleAbove;
            this.sectionTitleBelow = seed.sectionTitleBelow;
            this.headerLineSpacing = seed.headerLineSpacing;
            this.listItemSpacing = seed.listItemSpacing;
            this.contentPadding = seed.contentPadding;
        }

        /**
         * Sets the gap between top-level modules.
         *
         * @param value non-negative finite gap in points
         * @return this builder
         */
        public Builder moduleGap(double value) {
            this.moduleGap = value;
            return this;
        }

        /**
         * Sets the extra spacing between wrapped lines inside a body block.
         *
         * @param value non-negative finite spacing in points
         * @return this builder
         */
        public Builder lineSpacing(double value) {
            this.lineSpacing = value;
            return this;
        }

        /**
         * Sets the gap between paragraphs in a multi-paragraph module.
         *
         * @param value non-negative finite gap in points
         * @return this builder
         */
        public Builder paragraphSpacing(double value) {
            this.paragraphSpacing = value;
            return this;
        }

        /**
         * Sets the margin above a section heading.
         *
         * @param value non-negative finite margin in points
         * @return this builder
         */
        public Builder sectionTitleAbove(double value) {
            this.sectionTitleAbove = value;
            return this;
        }

        /**
         * Sets the margin below a section heading.
         *
         * @param value non-negative finite margin in points
         * @return this builder
         */
        public Builder sectionTitleBelow(double value) {
            this.sectionTitleBelow = value;
            return this;
        }

        /**
         * Sets the gap between contact rows in the document header.
         *
         * @param value non-negative finite gap in points
         * @return this builder
         */
        public Builder headerLineSpacing(double value) {
            this.headerLineSpacing = value;
            return this;
        }

        /**
         * Sets the gap between list items.
         *
         * @param value non-negative finite gap in points
         * @return this builder
         */
        public Builder listItemSpacing(double value) {
            this.listItemSpacing = value;
            return this;
        }

        /**
         * Sets the overall content padding.
         *
         * @param value non-null margin
         * @return this builder
         */
        public Builder contentPadding(Margin value) {
            this.contentPadding = Objects.requireNonNull(value, "contentPadding");
            return this;
        }

        /**
         * Builds an immutable {@link Spacing} record. All numeric tokens
         * are validated for non-negativity and finiteness here.
         *
         * @return new spacing record
         * @throws IllegalArgumentException if any numeric token is invalid
         * @throws NullPointerException     if content padding is {@code null}
         */
        public Spacing build() {
            return new Spacing(
                    moduleGap,
                    lineSpacing,
                    paragraphSpacing,
                    sectionTitleAbove,
                    sectionTitleBelow,
                    headerLineSpacing,
                    listItemSpacing,
                    contentPadding);
        }
    }
}
