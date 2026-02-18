package com.demcha.compose.font_library;

import lombok.Getter;

/**
 * Represents the standard  font names.
 */
@Getter
public enum FontName {
    /**
     * Times-Roman font.
     */
    TIMES_ROMAN("Times-Roman"), //
    /**
     * Helvetica font.
     */
    HELVETICA("Helvetica"), //
    HELVETICA_BOLD("Helvetica-Bold"),
    HELVETICA_OBLIQUE("Helvetica-Oblique"),
    HELVETICA_BOLD_OBLIQUE("Helvetica-BoldOblique"),
    /**
     * Courier font.
     */
    COURIER("Courier"), //
    COURIER_BOLD("Courier-Bold"),
    COURIER_OBLIQUE("Courier-Oblique"),
    COURIER_BOLD_OBLIQUE("Courier-BoldOblique"),
    TIMES_BOLD("Times-Bold"),
    TIMES_ITALIC("Times-Italic"),
    TIMES_BOLD_ITALIC("Times-BoldItalic"),
    /**
     * Symbol font.
     */
    SYMBOL("Symbol"),
    DEFAULT("default"),
    /**
     * ZapfDingbats font.
     */
    ZAPF_DINGBATS("ZapfDingbats");

    /**
     * The name of the font.
     */
    private final String name;

    /**
     * Constructs a FontName with the given name.
     *
     * @param name The name of the font.
     */
    FontName(String name) {
        this.name = name;
    }

    /**
     * Returns the string representation of the font name.
     *
     * @return The name of the font.
     */
    @Override
    public String toString() {
        return name;
    }
}
