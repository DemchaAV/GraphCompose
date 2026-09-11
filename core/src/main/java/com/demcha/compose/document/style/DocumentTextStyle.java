package com.demcha.compose.document.style;

import com.demcha.compose.font.FontName;

import java.util.Objects;

/**
 * Public text style value for paragraphs, lists, module titles, and table cells.
 *
 * <p>The DSL adapts this value into the internal engine text style during
 * composition. Instances are immutable and thread-safe.</p>
 *
 * @param fontName   font <em>family</em> name. The standard-14 face constants
 *                   ({@code HELVETICA_BOLD}, {@code TIMES_ITALIC}, …) are aliases of
 *                   their family and carry no weight or slant of their own — the face
 *                   comes from {@code decoration}. Naming a face and leaving the
 *                   decoration unset renders the regular face.
 * @param size       font size in points
 * @param decoration text decoration
 * @param color      text color
 * @param letterSpacing typographic tracking; {@link DocumentLetterSpacing#NONE}
 *                   (the default) leaves glyph advances exactly as they were
 * @author Artem Demchyshyn
 * @see DocumentLetterSpacing
 */
public record DocumentTextStyle(
        FontName fontName,
        double size,
        DocumentTextDecoration decoration,
        DocumentColor color,
        DocumentLetterSpacing letterSpacing
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
        letterSpacing = letterSpacing == null ? DocumentLetterSpacing.NONE : letterSpacing;
    }

    /**
     * Creates a normalized text style without tracking.
     *
     * <p>The signature this type carried before {@code letterSpacing} was
     * added. It stays so code compiled against the 2.0.0 surface keeps
     * linking, and so the binary-compatibility gate still finds the
     * constructor it has always found.</p>
     *
     * @param fontName   font family name
     * @param size       font size in points
     * @param decoration text decoration
     * @param color      text color
     */
    public DocumentTextStyle(FontName fontName,
                             double size,
                             DocumentTextDecoration decoration,
                             DocumentColor color) {
        this(fontName, size, decoration, color, DocumentLetterSpacing.NONE);
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
        return new DocumentTextStyle(fontName, size, decoration, color, letterSpacing);
    }

    /**
     * Creates a copy with a different color.
     *
     * @param color document color
     * @return updated text style
     */
    public DocumentTextStyle withColor(DocumentColor color) {
        return new DocumentTextStyle(fontName, size, decoration, color, letterSpacing);
    }

    /**
     * Creates a copy with different tracking.
     *
     * @param letterSpacing tracking to apply; {@code null} means
     *                      {@link DocumentLetterSpacing#NONE}
     * @return updated text style
     * @since 2.4.0
     */
    public DocumentTextStyle withLetterSpacing(DocumentLetterSpacing letterSpacing) {
        return new DocumentTextStyle(fontName, size, decoration, color, letterSpacing);
    }

    /**
     * Mutable builder for {@link DocumentTextStyle}.
     */
    public static final class Builder {
        private FontName fontName = FontName.HELVETICA;
        private double size = 14;
        private DocumentTextDecoration decoration = DocumentTextDecoration.DEFAULT;
        private DocumentColor color = DocumentColor.BLACK;
        private DocumentLetterSpacing letterSpacing = DocumentLetterSpacing.NONE;

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
         * Sets the typographic tracking &mdash; extra advance after every
         * rendered code point.
         *
         * <p>Real tracking, not inserted spaces: the text handed to the
         * backend stays the author's string, so search, copy/paste and text
         * extraction still read it as written.</p>
         *
         * @param letterSpacing tracking to apply, e.g.
         *                      {@code DocumentLetterSpacing.ofFontSize(0.12)};
         *                      {@code null} means
         *                      {@link DocumentLetterSpacing#NONE}
         * @return this builder
         * @since 2.4.0
         */
        public Builder letterSpacing(DocumentLetterSpacing letterSpacing) {
            this.letterSpacing = Objects.requireNonNullElse(letterSpacing, DocumentLetterSpacing.NONE);
            return this;
        }

        /**
         * Builds an immutable style value.
         *
         * @return text style
         */
        public DocumentTextStyle build() {
            return new DocumentTextStyle(fontName, size, decoration, color, letterSpacing);
        }
    }
}
