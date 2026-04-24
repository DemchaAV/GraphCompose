package com.demcha.compose.document.style;

import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Public text style value for paragraphs, lists, module titles, and table cells.
 *
 * <p>The DSL adapts this value into the internal engine text style during
 * composition. Instances are immutable and thread-safe.</p>
 *
 * @param fontName font family name
 * @param size font size in points
 * @param decoration text decoration
 * @param color text color
 * @author Artem Demchyshyn
 */
public record DocumentTextStyle(
        FontName fontName,
        double size,
        DocumentTextDecoration decoration,
        DocumentColor color
) {
    public static final DocumentTextStyle DEFAULT = builder().build();

    /**
     * Creates a normalized canonical text style.
     */
    public DocumentTextStyle {
        fontName = fontName == null ? FontName.HELVETICA : fontName;
        size = size <= 0 ? 14 : size;
        decoration = decoration == null ? DocumentTextDecoration.DEFAULT : decoration;
        color = color == null ? DocumentColor.BLACK : color;
    }

    /**
     * Starts a mutable builder with canonical defaults.
     *
     * @return text style builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy with a different font size.
     *
     * @param size font size in points
     * @return updated text style
     */
    public DocumentTextStyle withSize(double size) {
        return new DocumentTextStyle(fontName, size, decoration, color);
    }

    /**
     * Creates a copy with a different color.
     *
     * @param color document color
     * @return updated text style
     */
    public DocumentTextStyle withColor(DocumentColor color) {
        return new DocumentTextStyle(fontName, size, decoration, color);
    }

    /**
     * Mutable builder for {@link DocumentTextStyle}.
     */
    public static final class Builder {
        private FontName fontName = FontName.HELVETICA;
        private double size = 14;
        private DocumentTextDecoration decoration = DocumentTextDecoration.DEFAULT;
        private DocumentColor color = DocumentColor.BLACK;

        private Builder() {
        }

        /**
         * Sets the font family.
         *
         * @param fontName font family name
         * @return this builder
         */
        public Builder fontName(FontName fontName) {
            this.fontName = Objects.requireNonNullElse(fontName, FontName.HELVETICA);
            return this;
        }

        /**
         * Sets the font size.
         *
         * @param size font size in points
         * @return this builder
         */
        public Builder size(double size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the text decoration.
         *
         * @param decoration decoration mode
         * @return this builder
         */
        public Builder decoration(DocumentTextDecoration decoration) {
            this.decoration = Objects.requireNonNullElse(decoration, DocumentTextDecoration.DEFAULT);
            return this;
        }

        /**
         * Sets the text color.
         *
         * @param color document color
         * @return this builder
         */
        public Builder color(DocumentColor color) {
            this.color = Objects.requireNonNullElse(color, DocumentColor.BLACK);
            return this;
        }

        /**
         * Builds an immutable style value.
         *
         * @return text style
         */
        public DocumentTextStyle build() {
            return new DocumentTextStyle(fontName, size, decoration, color);
        }
    }
}
