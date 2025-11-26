package com.demcha.components.style;


import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Optional;

@Slf4j
public record Padding(double top, double right, double bottom, double left) implements Component, RenderCoordinate {
    public static Padding zero() {
        log.debug("Getting zero padding");
        return new Padding(0.0, 0.0, 0.0, 0.0);
    }

    public static Padding of(double trbl) {
        return new Padding(trbl, trbl, trbl, trbl);
    }

    public double horizontal() {
        return right + left;
    }

    public double vertical() {
        return top + bottom;
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
    public <S>Optional<RenderCoordinateContext> renderCoordinate(Entity entity, RenderingSystemECS<S> renderingSystem) {
        if (this.equals(zero())) {
            log.info("Padding is zero, return empty");
            return Optional.empty();
        }
        var inner = InnerBoxSize.from(entity).orElseThrow();
        var placement = entity.getComponent(Placement.class).orElseThrow();
        double x;
        double y;
        double width;
        double height;
        int startPage;
        int endPage;

        startPage = placement.startPage();
        endPage = placement.endPage();

        x = placement.x() + left();
        y = placement.y()+ bottom();
        width = inner.width();
        height = inner.height();
        Color color = renderingSystem.guidLineSettings().PADDING_COLOR();
        Stroke stroke = renderingSystem.guidLineSettings().PADDING_STROKE();
        return Optional.of(new RenderCoordinateContext(x, y, width, height, startPage, endPage,stroke, color));
    }
}
