package com.demcha.components.content.rectangle;

import com.demcha.components.core.Component;
import com.demcha.components.style.ColorComponent;
import lombok.NonNull;

import java.awt.*;

public record FillColor(Color color) implements Component {
    public static Color DEFAULT_FILL_COLOR = ColorComponent.MODULE_TITLE;

    public FillColor(@NonNull ColorComponent color) {
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
