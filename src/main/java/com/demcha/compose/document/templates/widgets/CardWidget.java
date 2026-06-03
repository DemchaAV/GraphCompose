package com.demcha.compose.document.templates.widgets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared card/container widget for template presets.
 *
 * <p>The widget captures the reusable visual shell only: spacing,
 * padding, fill, stroke, and corner radius. The caller still supplies
 * the card body so CVs, proposals, invoices, and cover letters can
 * reuse the same shell without sharing document-specific content
 * logic.</p>
 */
public final class CardWidget {
    private CardWidget() {
    }

    /**
     * Renders the card as a nested section under {@code parent},
     * applying the visual shell and then the supplied body.
     *
     * @param parent  parent section receiving the card
     * @param name    node name used in snapshots and layout graph paths
     * @param style   visual shell options; null falls back to defaults
     * @param content callback that populates the card body
     */
    public static void render(SectionBuilder parent,
                              String name,
                              Style style,
                              Consumer<SectionBuilder> content) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(content, "content");
        Style safeStyle = style == null ? Style.builder().build() : style;

        parent.addSection(name, card -> {
            applyStyle(card, safeStyle);
            content.accept(card);
        });
    }

    /**
     * Top-level overload — renders the card as a page-flow section so
     * presets can place full-width cards directly under
     * {@link PageFlowBuilder} without wrapping them in a parent
     * section. Visual shell behaves identically to the
     * {@link #render(SectionBuilder, String, Style, Consumer)}
     * variant.
     *
     * @param flow    page flow receiving the card
     * @param name    node name used in snapshots and layout graph paths
     * @param style   visual shell options; null falls back to defaults
     * @param content callback that populates the card body
     */
    public static void render(PageFlowBuilder flow,
                              String name,
                              Style style,
                              Consumer<SectionBuilder> content) {
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(content, "content");
        Style safeStyle = style == null ? Style.builder().build() : style;

        flow.addSection(name, card -> {
            applyStyle(card, safeStyle);
            content.accept(card);
        });
    }

    private static void applyStyle(SectionBuilder card, Style style) {
        card.spacing(style.spacing())
                .padding(style.padding());
        if (style.fillColor() != null) {
            card.fillColor(style.fillColor());
        }
        if (style.stroke() != null) {
            card.stroke(style.stroke());
        }
        if (style.cornerRadius() != null) {
            card.cornerRadius(style.cornerRadius());
        }
    }

    /**
     * Visual shell options for {@link CardWidget}.
     *
     * @param spacing      vertical spacing between children
     * @param padding      inner padding
     * @param fillColor    optional background fill
     * @param stroke       optional uniform border stroke
     * @param cornerRadius optional render-only corner radius
     */
    public record Style(double spacing,
                        DocumentInsets padding,
                        DocumentColor fillColor,
                        DocumentStroke stroke,
                        DocumentCornerRadius cornerRadius) {

        /**
         * Validates that spacing is finite and non-negative and
         * normalises a null padding to zero insets.
         */
        public Style {
            if (Double.isNaN(spacing) || Double.isInfinite(spacing)
                    || spacing < 0) {
                throw new IllegalArgumentException(
                        "spacing must be finite and non-negative");
            }
            padding = padding == null ? DocumentInsets.zero() : padding;
        }

        /**
         * Creates a builder seeded with conservative defaults.
         *
         * @return a mutable builder seeded with conservative defaults
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Fluent builder for {@link Style}.
         */
        public static final class Builder {
            private double spacing;
            private DocumentInsets padding = DocumentInsets.zero();
            private DocumentColor fillColor;
            private DocumentStroke stroke;
            private DocumentCornerRadius cornerRadius;

            private Builder() {
            }

            /**
             * Sets the vertical spacing between children.
             *
             * @param value vertical spacing between children
             * @return this builder for chaining
             */
            public Builder spacing(double value) {
                this.spacing = value;
                return this;
            }

            /**
             * Sets the inner padding.
             *
             * @param value inner padding
             * @return this builder for chaining
             */
            public Builder padding(DocumentInsets value) {
                this.padding = value;
                return this;
            }

            /**
             * Sets the optional background fill.
             *
             * @param value optional background fill
             * @return this builder for chaining
             */
            public Builder fillColor(DocumentColor value) {
                this.fillColor = value;
                return this;
            }

            /**
             * Sets the optional uniform border stroke.
             *
             * @param value optional uniform border stroke
             * @return this builder for chaining
             */
            public Builder stroke(DocumentStroke value) {
                this.stroke = value;
                return this;
            }

            /**
             * Sets the optional render-only corner radius from a raw value.
             *
             * @param value corner radius in points
             * @return this builder for chaining
             */
            public Builder cornerRadius(double value) {
                this.cornerRadius = DocumentCornerRadius.of(value);
                return this;
            }

            /**
             * Sets the optional render-only corner radius.
             *
             * @param value optional render-only corner radius
             * @return this builder for chaining
             */
            public Builder cornerRadius(DocumentCornerRadius value) {
                this.cornerRadius = value;
                return this;
            }

            /**
             * Builds the configured {@link Style}.
             *
             * @return a new {@code Style} carrying the configured shell options
             */
            public Style build() {
                return new Style(spacing, padding, fillColor, stroke,
                        cornerRadius);
            }
        }
    }
}
