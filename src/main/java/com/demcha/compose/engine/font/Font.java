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
        return switch (textDecoration) {
            case BOLD -> bold();
            case ITALIC -> italic();
            case UNDERLINE -> underline();
            case BOLD_ITALIC -> boldItalic();
            case STRIKETHROUGH -> strikethrough();
            case null, default -> defaultFont();
        };
    }

    double getTextWidth(TextStyle style, String text);

    double getTextWidthNoSanitize(TextStyle style, String text);


    double getLineHeight(TextStyle style);

    double getTextHeight(TextStyle style);

    double getCapHeight(TextStyle style);

    double scale(double size);

    public TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth);

    ContentSize getTightBounds(String text, TextStyle style);
}

