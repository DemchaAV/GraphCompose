package com.demcha.compose.engine.components.content.text;

import com.demcha.compose.engine.font.Font;
import org.jetbrains.annotations.NotNull;

public record TextDataBody(String text, TextStyle textStyle) {
    public  TextDataBody(String text, TextStyle textStyle){
//        this.text = TextSanitizer.sanitize(text);
        this.text = text;
        this.textStyle = textStyle;
    }
    public double width(@NotNull Font<?> font) {
        return font.getTextWidth(textStyle, text);
    }
}
