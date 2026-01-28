package com.demcha.compose.loyaut_core.system.interfaces;

import com.demcha.compose.loyaut_core.components.content.text.TextDecoration;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;

public interface Font<T> {


    T defaultFont();

    T bold();

    T italic();

    T boldItalic();

    T underline();

    T strikethrough();

    default T fontType(TextDecoration textDecoration) {
        switch (textDecoration) {
            case BOLD:
                return bold();
            case ITALIC:
                return italic();
            case UNDERLINE:
                return underline();
            case BOLD_ITALIC:
                return boldItalic();
            case STRIKETHROUGH:
                return strikethrough();
            case null, default:
                return defaultFont();
        }
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

