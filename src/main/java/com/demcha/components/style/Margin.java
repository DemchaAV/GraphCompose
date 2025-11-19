package com.demcha.components.style;


import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Slf4j
public record Margin(double top, double right, double bottom, double left) implements Component, RenderCoordinate {

    public static Margin of(double value) {
        return new Margin(value, value, value, value);
    }

    public static Margin zero() {
        return new Margin(0, 0, 0, 0);
    }

    public static Margin bottom(double value) {
        return new Margin(0, 0, value, 0);
    }

    public static Margin top(double value) {
        return new Margin(value, 0, 0, 0);
    }

    public static Margin right(double value) {
        return new Margin(0, value, 0, 0);
    }

    public static Margin left(double value) {
        return new Margin(0, 0, 0, value);
    }

    public  Optional<RenderCoordinateContext> renderCoordinate(Entity entity) {
        if (this.equals(Margin.zero())) {
            log.warn("Margin is zero, return empty");
            return Optional.empty();
        }
        double x;
        double y;
        double width;
        double height;
        int startPage;
        int endPage;

        var placement = entity.getComponent(Placement.class).orElseThrow();
        startPage = placement.startPage();
        endPage = placement.endPage();
        width = placement.width() + horizontal();
        height = placement.height() + vertical();
//        x = placement.x() ;
        //TODO должно быть так но нужно зафиксить пока временно будет без минус
        x = placement.x() - left();
        y = placement.y() - bottom();


        return Optional.of(new RenderCoordinateContext(x, y, width, height, startPage, endPage));

    }

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Margin margin = (Margin) o;
        return Double.compare(top, margin.top) == 0 && Double.compare(left, margin.left) == 0 && Double.compare(right, margin.right) == 0 && Double.compare(bottom, margin.bottom) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(top);
        result = 31 * result + Double.hashCode(right);
        result = 31 * result + Double.hashCode(bottom);
        result = 31 * result + Double.hashCode(left);
        return result;
    }
}


