package com.demcha.compose.loyaut_core.core;

import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.style.Margin;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class CanvasBox implements Canvas {
    private final float width;
    private final float height;
    private final float x;
    private final float y;
    private  Margin margin;

    public CanvasBox(float width, float height, float x, float y, Margin margin) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.margin = margin;
    }

    public CanvasBox(float width, float height, float x, float y) {
        this(width, height, x, y, null);
    }

    @Override
    public Margin margin() {
        if (this.margin == null) {
            return Margin.zero();
        }
        return margin;
    }

    @Override
    public void addMargin(Margin margin) {
        this.margin = margin;
    }
}
