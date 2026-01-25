package com.demcha.compose.loyaut_core.components.content.shape;

import com.demcha.compose.loyaut_core.components.core.Component;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import lombok.NonNull;

import java.awt.*;

public record FillColor(Color color) implements Component {
    public static Color DEFAULT_FILL_COLOR = ComponentColor.MODULE_TITLE;

    public FillColor(@NonNull ComponentColor color) {
        this(color.color());
    }
    public static FillColor nonColor(){
        return new FillColor(Color.WHITE);
    }
    public static FillColor defaultColor(){
        return new FillColor(DEFAULT_FILL_COLOR);
    }

    public FillColor() {
        this(DEFAULT_FILL_COLOR);
    }
}
