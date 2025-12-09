package com.demcha.font_library;

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
    /**
     * Courier font.
     */
    COURIER("Courier"), //
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
