package com.demcha.compose.engine.font;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;

public interface Font<T> {


    T defaultFont();

    T bold();

    T italic();

    T boldItalic();

    T underline();

    T strikethrough();

    default T fontType(TextDecoration textDecoration) {
        if (textDecoration == null) {
            return defaultFont();
        }
        return switch (textDecoration) {
            case BOLD -> bold();
            case ITALIC -> italic();
            case UNDERLINE -> underline();
            case BOLD_ITALIC -> boldItalic();
            case STRIKETHROUGH -> strikethrough();
            default -> defaultFont();
        };
    }

    double getTextWidth(TextStyle style, String text);

    double getTextWidthNoSanitize(TextStyle style, String text);


    double getLineHeight(TextStyle style);

    double getTextHeight(TextStyle style);

    double getCapHeight(TextStyle style);

    double scale(double size);

    /**
     * @param text           the text to fit
     * @param style          the starting style
     * @param availableWidth the width to fit within
     * @return a re-sized style
     * @deprecated Unused and incorrect: the only real implementation re-measures
     *     with the unchanged {@code style}, so the loop never converges and the
     *     result is always the minimum size. Canonical auto-size is resolved by the
     *     layout compiler ({@code TextFlowSupport.resolveAutoSizeTextStyle}); this
     *     method has no callers and is kept only for binary compatibility,
     *     scheduled for removal in the next major.
     */
    @Deprecated
    TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth);

    ContentSize getTightBounds(String text, TextStyle style);
}
