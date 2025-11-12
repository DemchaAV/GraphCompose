package com.demcha.core;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.style.Margin;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@Getter
@Accessors(fluent = true)
public final class CanvasSize implements Canvas {
    private final float width;
    private final float height;
    private final float x;
    private final float y;
    private  Margin margin;

    public CanvasSize(float width, float height, float x, float y, Margin margin) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.margin = margin;
    }

    public CanvasSize(float width, float height, float x, float y) {
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
