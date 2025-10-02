package com.demcha.components.layout.coordinator;

import com.demcha.components.core.Entity;
import com.demcha.components.style.Margin;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Position for rendering BoxElement
 *
 * @param x
 * @param y
 */
@Slf4j
public record RenderingPosition(double x, double y) {

    public static RenderingPosition from(@NonNull ComputedPosition computed, @NonNull Margin margin) {
        return new RenderingPosition(computed.x() + margin.left(), computed.y() + margin.bottom());
    }

    public static Optional<RenderingPosition> from(Entity entity) {
        log.debug("Computation a Rendering position for {}", entity);
        var computedPosition = entity.getComponent(ComputedPosition.class);
        if (computedPosition.isEmpty()) {
            log.warn("TextComponent has no ComputedPosition; skipping: {}", entity);
            return Optional.empty();
        }
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        RenderingPosition from = from(computedPosition.get(), margin);
        log.debug("Rendering position {}", from);
        return Optional.of(from);
    }

    @Override
    public String toString() {
        return "RenderingPosition[" + "x: " + x() + ", " + "y: " + y() + "]";

    }
}
