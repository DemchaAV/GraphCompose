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
