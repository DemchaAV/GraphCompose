package com.demcha.components.geometry;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.*;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Represents the final size and position of a component, including any calculated margins.
 * <p>
 * This component stores the absolute coordinates and dimensions (the "bounding box")
 * for an entity after all layout calculations, such as margins and parent container
 * constraints, have been applied.
 *
 * @param x      The final horizontal position (X-coordinate), including margins.
 * @param y      The final vertical position (Y-coordinate), including margins.
 * @param width  The final width, including any horizontal margins.
 * @param height The final height, including any vertical margins.
 */
@Slf4j
public record Placement(double x, double y, double width, double height) implements Component {


    private static Placement from(OuterBoxSize outerBoxSize, Position positionWithMargins) {
        Objects.requireNonNull(outerBoxSize);
        Objects.requireNonNull(positionWithMargins);
        return new Placement(positionWithMargins.x(), positionWithMargins.y(), outerBoxSize.width(), outerBoxSize.height());
    }

    public static Placement from(Entity entity, InnerBoxSize parrentInnerBoxSize,  PaddingCoordinate paddingCoordinate) {
        var computedPosition = ComputedPosition.from(entity, parrentInnerBoxSize, paddingCoordinate);
        var outBoxSize = OuterBoxSize.from(entity).orElseThrow();
        var padding = entity.getComponent(Padding.class).get();
        return new Placement(computedPosition.x(), computedPosition.y(), outBoxSize.width(), outBoxSize.height());


    }

    public static Placement fromWithDefault(Entity entity, InnerBoxSize parrentInnerBoxSize,  PaddingCoordinate paddingCoordinate) {
        var position = entity.getComponent(Position.class).orElse(Position.zero());
        entity.addComponent(position);
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        entity.addComponent(margin);
        var anchor = entity.getComponent(Anchor.class).orElse(Anchor.topLeft());
        entity.addComponent(anchor);
        return from(entity, parrentInnerBoxSize,  paddingCoordinate);
    }


    public static Placement from(Entity child, Entity parent) {
        var parrentInnerBoxSize = InnerBoxSize.from(parent).orElseThrow();
        var paddingCoordinate = PaddingCoordinate.from(parent);

        return from(child, parrentInnerBoxSize, paddingCoordinate);
    }
}
