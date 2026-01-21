package com.demcha.loyaut_core.components.content.text;

import com.demcha.loyaut_core.system.interfaces.Font;
import com.demcha.loyaut_core.utils.TextSanitizer;
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
