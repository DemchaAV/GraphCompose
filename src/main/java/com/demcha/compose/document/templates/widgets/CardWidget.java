package com.demcha.compose.document.templates.widgets;

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

    public static void render(SectionBuilder parent,
                              String name,
                              Style style,
                              Consumer<SectionBuilder> content) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(content, "content");
        Style safeStyle = style == null ? Style.builder().build() : style;

        parent.addSection(name, card -> {
            card.spacing(safeStyle.spacing())
                    .padding(safeStyle.padding());
            if (safeStyle.fillColor() != null) {
                card.fillColor(safeStyle.fillColor());
            }
            if (safeStyle.stroke() != null) {
                card.stroke(safeStyle.stroke());
            }
            if (safeStyle.cornerRadius() != null) {
                card.cornerRadius(safeStyle.cornerRadius());
            }
            content.accept(card);
        });
    }

    /**
     * Visual shell options for {@link CardWidget}.
     */
    public record Style(double spacing,
                        DocumentInsets padding,
                        DocumentColor fillColor,
                        DocumentStroke stroke,
                        DocumentCornerRadius cornerRadius) {

        public Style {
            if (Double.isNaN(spacing) || Double.isInfinite(spacing)
                    || spacing < 0) {
                throw new IllegalArgumentException(
                        "spacing must be finite and non-negative");
            }
            padding = padding == null ? DocumentInsets.zero() : padding;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private double spacing;
            private DocumentInsets padding = DocumentInsets.zero();
            private DocumentColor fillColor;
            private DocumentStroke stroke;
            private DocumentCornerRadius cornerRadius;

            private Builder() {
            }

            public Builder spacing(double value) {
                this.spacing = value;
                return this;
            }

            public Builder padding(DocumentInsets value) {
                this.padding = value;
                return this;
            }

            public Builder fillColor(DocumentColor value) {
                this.fillColor = value;
                return this;
            }

            public Builder stroke(DocumentStroke value) {
                this.stroke = value;
                return this;
            }

            public Builder cornerRadius(double value) {
                this.cornerRadius = DocumentCornerRadius.of(value);
                return this;
            }

            public Builder cornerRadius(DocumentCornerRadius value) {
                this.cornerRadius = value;
                return this;
            }

            public Style build() {
                return new Style(spacing, padding, fillColor, stroke,
                        cornerRadius);
            }
        }
    }
}
