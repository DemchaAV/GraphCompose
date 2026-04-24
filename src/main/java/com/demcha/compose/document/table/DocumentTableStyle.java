package com.demcha.compose.document.table;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * Public table cell style override for the canonical DSL.
 *
 * <p>All fields are optional. Missing values inherit from the table defaults,
 * column overrides, row overrides, or backend defaults during layout/render.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentTableStyle {
    private final DocumentInsets padding;
    private final DocumentColor fillColor;
    private final DocumentStroke stroke;
    private final DocumentTextStyle textStyle;
    private final DocumentTableTextAnchor textAnchor;
    private final Double lineSpacing;

    private DocumentTableStyle(Builder builder) {
        this.padding = builder.padding;
        this.fillColor = builder.fillColor;
        this.stroke = builder.stroke;
        this.textStyle = builder.textStyle;
        this.textAnchor = builder.textAnchor;
        this.lineSpacing = builder.lineSpacing;
    }

    /**
     * Creates an empty style override.
     *
     * @return empty table style
     */
    public static DocumentTableStyle empty() {
        return builder().build();
    }

    /**
     * Starts a mutable table style builder.
     *
     * @return table style builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns optional cell padding.
     *
     * @return padding override, or {@code null}
     */
    public DocumentInsets padding() {
        return padding;
    }

    /**
     * Returns optional cell fill color.
     *
     * @return fill color override, or {@code null}
     */
    public DocumentColor fillColor() {
        return fillColor;
    }

    /**
     * Returns optional cell border stroke.
     *
     * @return stroke override, or {@code null}
     */
    public DocumentStroke stroke() {
        return stroke;
    }

    /**
     * Returns optional cell text style.
     *
     * @return text style override, or {@code null}
     */
    public DocumentTextStyle textStyle() {
        return textStyle;
    }

    /**
     * Returns optional text placement anchor.
     *
     * @return anchor override, or {@code null}
     */
    public DocumentTableTextAnchor textAnchor() {
        return textAnchor;
    }

    /**
     * Returns optional spacing between wrapped cell lines.
     *
     * @return line spacing override, or {@code null}
     */
    public Double lineSpacing() {
        return lineSpacing;
    }

    /**
     * Mutable builder for {@link DocumentTableStyle}.
     */
    public static final class Builder {
        private DocumentInsets padding;
        private DocumentColor fillColor;
        private DocumentStroke stroke;
        private DocumentTextStyle textStyle;
        private DocumentTableTextAnchor textAnchor;
        private Double lineSpacing;

        private Builder() {
        }

        /**
         * Sets the cell padding override.
         *
         * @param padding padding value
         * @return this builder
         */
        public Builder padding(DocumentInsets padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Sets equal cell padding on all sides.
         *
         * @param padding padding in points
         * @return this builder
         */
        public Builder padding(double padding) {
            return padding(DocumentInsets.of(padding));
        }

        /**
         * Sets the cell fill color override.
         *
         * @param fillColor fill color
         * @return this builder
         */
        public Builder fillColor(DocumentColor fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        /**
         * Sets the cell border stroke override.
         *
         * @param stroke border stroke
         * @return this builder
         */
        public Builder stroke(DocumentStroke stroke) {
            this.stroke = stroke;
            return this;
        }

        /**
         * Sets the cell text style override.
         *
         * @param textStyle text style
         * @return this builder
         */
        public Builder textStyle(DocumentTextStyle textStyle) {
            this.textStyle = textStyle;
            return this;
        }

        /**
         * Sets the cell text placement anchor.
         *
         * @param textAnchor text anchor
         * @return this builder
         */
        public Builder textAnchor(DocumentTableTextAnchor textAnchor) {
            this.textAnchor = textAnchor;
            return this;
        }

        /**
         * Sets spacing between wrapped cell lines.
         *
         * @param lineSpacing line spacing in points
         * @return this builder
         */
        public Builder lineSpacing(double lineSpacing) {
            this.lineSpacing = lineSpacing;
            return this;
        }

        /**
         * Builds an immutable style override.
         *
         * @return table style
         */
        public DocumentTableStyle build() {
            return new DocumentTableStyle(this);
        }
    }
}
