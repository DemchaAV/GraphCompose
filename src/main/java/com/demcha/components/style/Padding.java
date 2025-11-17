package com.demcha.components.style;


import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public record Padding(double top, double right, double bottom, double left) implements Component, RenderCoordinate {
    public double horizontal() {
        return right + left;
    }

    public double vertical() {
        return top + bottom;
    }
    public static Padding zero() {
        log.debug("Getting zero padding");
        return new Padding(0.0, 0.0, 0.0, 0.0);
    }
    public static Padding of( double trbl) {
        return new Padding(trbl, trbl, trbl, trbl);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Padding padding = (Padding) o;
        return Double.compare(top, padding.top) == 0 && Double.compare(left, padding.left) == 0 && Double.compare(right, padding.right) == 0 && Double.compare(bottom, padding.bottom) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(top);
        result = 31 * result + Double.hashCode(right);
        result = 31 * result + Double.hashCode(bottom);
        result = 31 * result + Double.hashCode(left);
        return result;
    }

    @Override
    public Optional<RenderCoordinateContext> renderCoordinate(Entity entity) {
        return Optional.empty();
    }
}
