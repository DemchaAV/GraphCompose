package com.demcha.components.layout.coordinator;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.*;
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
public record Placement(double x, double y, double width, double height, int startPage, int endPage) implements Component {


    /**
     * Will be assign same page for @link{ #startPage and endPage} it means Entity will be located on one page not breakable
     * @param outerBoxSize
     * @param positionWithMargins
     * @param pageNumber
     * @return
     */
    private static Placement from(OuterBoxSize outerBoxSize, Position positionWithMargins, int pageNumber) {
        Objects.requireNonNull(outerBoxSize);
        Objects.requireNonNull(positionWithMargins);
        return new Placement(positionWithMargins.x(), positionWithMargins.y(), outerBoxSize.width(), outerBoxSize.height(), pageNumber, pageNumber);
    }

    public static Placement from(Entity entity, InnerBoxSize parrentInnerBoxSize, PaddingCoordinate paddingCoordinate, int pageNumber) {
        var computedPosition = ComputedPosition.from(entity, parrentInnerBoxSize, paddingCoordinate);
        var outBoxSize = OuterBoxSize.from(entity).orElseThrow();
        var padding = entity.getComponent(Padding.class).get();
        return new Placement(computedPosition.x(), computedPosition.y(), outBoxSize.width(), outBoxSize.height(),pageNumber,pageNumber);


    }

    public static Placement fromWithDefault(Entity entity, InnerBoxSize parrentInnerBoxSize,  PaddingCoordinate paddingCoordinate, int pageNumber) {
        var position = entity.getComponent(Position.class).orElse(Position.zero());
        entity.addComponent(position);
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        entity.addComponent(margin);
        var anchor = entity.getComponent(Anchor.class).orElse(Anchor.topLeft());
        entity.addComponent(anchor);
        return from(entity, parrentInnerBoxSize,  paddingCoordinate, pageNumber);
    }

// TODO надо сделать так что бы считало позицию исходя с родителей
    public static Placement from(Entity child, Entity parent, int pageNumber) {
        var parrentInnerBoxSize = InnerBoxSize.from(parent).orElseThrow();
        var paddingCoordinate = PaddingCoordinate.from(parent);

        return from(child, parrentInnerBoxSize, paddingCoordinate, pageNumber);
    }
}
