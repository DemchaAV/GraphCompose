package com.demcha.loyaut_core.components.content.text;

import com.demcha.loyaut_core.system.interfaces.Font;
import org.jetbrains.annotations.NotNull;

public record TextDataBody(String text, TextStyle textStyle) {
    public double width(@NotNull Font<?> font) {
        return font.getTextWidth(textStyle, text);
    }
}
