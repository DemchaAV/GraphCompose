package com.demcha.legacy.components.data.text.block;

import com.demcha.legacy.components.data.text.TextStyle;
import com.demcha.components.core.Component;

// Блочный текст с wrapWidth (ширина обтекания)
// Если wrapWidth <= 0, используем то, что пришло в MeasureCtx
public record TextBlock(String text, TextStyle style, double wrapWidth, double lineSpacingFactor) implements Component {
    public static TextBlock of(String text, TextStyle style, double wrapWidth) {
        return new TextBlock(text, style, wrapWidth, 1.2); // дефолтный межстрочный интервал 120%
    }
}

