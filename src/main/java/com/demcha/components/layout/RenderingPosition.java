package com.demcha.components.layout;

import com.demcha.components.core.Entity;
import com.demcha.components.style.Margin;
import lombok.NonNull;

public record RenderingPosition(double x, double y) {

    public static RenderingPosition from(@NonNull ComputedPosition computed, @NonNull Margin margin) {
        return new RenderingPosition(computed.x() + margin.left(), computed.y() + margin.bottom());
    }

    public static RenderingPosition from(Entity entity) {
        var computedPosition = entity.getComponent(ComputedPosition.class).get();
        var margin = entity.getComponent(Margin.class).get();
        return from(computedPosition, margin);
    }
}
