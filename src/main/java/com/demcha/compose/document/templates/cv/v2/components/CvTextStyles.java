package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Small factory for preset-local text styles.
 */
public final class CvTextStyles {
    private CvTextStyles() {
    }

    /**
     * Builds a text style from the supplied font, size, decoration, and colour.
     *
     * @param font       the font family
     * @param size       the font size in points
     * @param decoration the text decoration (underline, strike-through, none)
     * @param color      the text colour
     * @return a {@code DocumentTextStyle} carrying the supplied attributes
     */
    public static DocumentTextStyle of(FontName font,
                                       double size,
                                       DocumentTextDecoration decoration,
                                       DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
