package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentColor;

import java.awt.*;
import java.util.Objects;

/**
 * Semantic color palette for business documents (invoice, proposal, report).
 *
 * <p>Tokens are referenced by role rather than by value so a single template can
 * be re-skinned by swapping {@link BusinessTheme}s.</p>
 *
 * @param primary      headline color (titles, primary buttons, brand accents)
 * @param accent       secondary accent (status keywords, hyperlinks, highlights)
 * @param surface      dominant background tone (page, default panel)
 * @param surfaceMuted subdued background (soft panels, table zebra)
 * @param textPrimary  primary body text color
 * @param textMuted    secondary/caption text color
 * @param rule         rule/divider/border color used by tables and accent strips
 * @author Artem Demchyshyn
 */
public record DocumentPalette(
        DocumentColor primary,
        DocumentColor accent,
        DocumentColor surface,
        DocumentColor surfaceMuted,
        DocumentColor textPrimary,
        DocumentColor textMuted,
        DocumentColor rule
) {
    /**
     * Validates required references and freezes the palette tokens.
     */
    public DocumentPalette {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(accent, "accent");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(surfaceMuted, "surfaceMuted");
        Objects.requireNonNull(textPrimary, "textPrimary");
        Objects.requireNonNull(textMuted, "textMuted");
        Objects.requireNonNull(rule, "rule");
    }

    /**
     * Returns a fluent builder for {@link DocumentPalette}.
     *
     * <p>Prefer this entry point over the positional {@link #of(Color, Color,
     * Color, Color, Color, Color, Color)} factory: named setters avoid
     * argument-order mistakes when adding or reorganising tokens, and the
     * builder is the migration target before {@code of(...)} is removed in
     * v2.0.</p>
     *
     * @return a fresh, empty palette builder
     * @since 1.6.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory accepting AWT colors.
     *
     * @param primary      primary color
     * @param accent       accent color
     * @param surface      surface color
     * @param surfaceMuted muted surface color
     * @param textPrimary  primary text color
     * @param textMuted    muted text color
     * @param rule         rule/border color
     * @return palette built from raw AWT colors
     * @deprecated since 1.6.0; prefer {@link #builder()} so adding or
     * reorganising palette tokens does not silently shift other
     * values via argument-order. Scheduled for removal in v2.0.
     */
    @Deprecated(since = "1.6.0", forRemoval = true)
    public static DocumentPalette of(Color primary,
                                     Color accent,
                                     Color surface,
                                     Color surfaceMuted,
                                     Color textPrimary,
                                     Color textMuted,
                                     Color rule) {
        return new DocumentPalette(
                DocumentColor.of(primary),
                DocumentColor.of(accent),
                DocumentColor.of(surface),
                DocumentColor.of(surfaceMuted),
                DocumentColor.of(textPrimary),
                DocumentColor.of(textMuted),
                DocumentColor.of(rule));
    }

    /**
     * Fluent builder for {@link DocumentPalette}. Each setter accepts either a
     * {@link DocumentColor} or a raw AWT {@link Color}; calling either form
     * sets the same underlying token.
     *
     * <p>The builder is single-use: {@link #build()} validates that every
     * token is set and constructs the immutable palette. A second call to
     * {@code build()} produces an equivalent palette as long as no setter
     * has been called in between.</p>
     *
     * @since 1.6.0
     */
    public static final class Builder {
        private DocumentColor primary;
        private DocumentColor accent;
        private DocumentColor surface;
        private DocumentColor surfaceMuted;
        private DocumentColor textPrimary;
        private DocumentColor textMuted;
        private DocumentColor rule;

        private Builder() {
        }

        /**
         * Sets the headline color.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder primary(DocumentColor color) {
            this.primary = Objects.requireNonNull(color, "primary");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the headline token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder primary(Color color) {
            return primary(DocumentColor.of(color));
        }

        /**
         * Sets the accent color.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder accent(DocumentColor color) {
            this.accent = Objects.requireNonNull(color, "accent");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the accent token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder accent(Color color) {
            return accent(DocumentColor.of(color));
        }

        /**
         * Sets the dominant surface background.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder surface(DocumentColor color) {
            this.surface = Objects.requireNonNull(color, "surface");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the surface token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder surface(Color color) {
            return surface(DocumentColor.of(color));
        }

        /**
         * Sets the muted surface background (soft panels, zebra rows).
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder surfaceMuted(DocumentColor color) {
            this.surfaceMuted = Objects.requireNonNull(color, "surfaceMuted");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the muted surface token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder surfaceMuted(Color color) {
            return surfaceMuted(DocumentColor.of(color));
        }

        /**
         * Sets the primary body text color.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder textPrimary(DocumentColor color) {
            this.textPrimary = Objects.requireNonNull(color, "textPrimary");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the primary text token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder textPrimary(Color color) {
            return textPrimary(DocumentColor.of(color));
        }

        /**
         * Sets the muted (secondary/caption) text color.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder textMuted(DocumentColor color) {
            this.textMuted = Objects.requireNonNull(color, "textMuted");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the muted text token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder textMuted(Color color) {
            return textMuted(DocumentColor.of(color));
        }

        /**
         * Sets the rule/divider/border color used by tables and accent strips.
         *
         * @param color non-{@code null} color
         * @return this builder
         */
        public Builder rule(DocumentColor color) {
            this.rule = Objects.requireNonNull(color, "rule");
            return this;
        }

        /**
         * Convenience setter accepting an AWT color for the rule token.
         *
         * @param color non-{@code null} AWT color
         * @return this builder
         */
        public Builder rule(Color color) {
            return rule(DocumentColor.of(color));
        }

        /**
         * Validates that every token has been set and constructs the palette.
         *
         * @return immutable palette
         * @throws IllegalStateException if any required token was never set;
         *                               the message names every missing token so the caller can
         *                               add them in one fix-up.
         */
        public DocumentPalette build() {
            StringBuilder missing = null;
            missing = appendIfMissing(missing, "primary", primary);
            missing = appendIfMissing(missing, "accent", accent);
            missing = appendIfMissing(missing, "surface", surface);
            missing = appendIfMissing(missing, "surfaceMuted", surfaceMuted);
            missing = appendIfMissing(missing, "textPrimary", textPrimary);
            missing = appendIfMissing(missing, "textMuted", textMuted);
            missing = appendIfMissing(missing, "rule", rule);
            if (missing != null) {
                throw new IllegalStateException(
                        "DocumentPalette.builder().build() requires every token to be set; missing: "
                        + missing);
            }
            return new DocumentPalette(primary, accent, surface, surfaceMuted, textPrimary, textMuted, rule);
        }

        private static StringBuilder appendIfMissing(StringBuilder sink, String name, Object value) {
            if (value != null) {
                return sink;
            }
            StringBuilder buffer = sink == null ? new StringBuilder() : sink;
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(name);
            return buffer;
        }
    }
}
