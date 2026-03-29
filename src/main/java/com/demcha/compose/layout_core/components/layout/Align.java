package com.demcha.compose.layout_core.components.layout;

import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.OuterBoxSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import lombok.extern.slf4j.Slf4j;

/**
 * Container-oriented alignment metadata.
 * <p>
 * {@code Align} describes how children should be arranged inside a parent
 * container and what spacing should separate siblings. It plays a role similar
 * to {@link Anchor}, but at the container layout level rather than the per-entity
 * absolute placement level.
 * </p>
 *
 * <p>Builders usually attach this component to containers, and layout utilities
 * later consume it when distributing children inside the parent's inner box.</p>
 */
@Slf4j
public record Align(HAnchor h, VAnchor v, double spacing) implements Component {
    public static Align middle(double spacing){
        var v = VAnchor.MIDDLE;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align top(double spacing){
        var v = VAnchor.TOP;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align bottom(double spacing){
        var v = VAnchor.BOTTOM;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align defaultAlign(double spacing){
        log.info("Align defaultAlign");
        var v = VAnchor.DEFAULT;
        var h = HAnchor.DEFAULT;
        return new Align(h,v,spacing);
    }
    public static Align left(double spacing){
        var v = VAnchor.DEFAULT;
        var h = HAnchor.LEFT;
        return new Align(h,v,spacing);
    }
    public static Align right(double spacing){
        var v = VAnchor.DEFAULT;
        var h = HAnchor.RIGHT;
        return new Align(h,v,spacing);
    }

    public  static void alignHorizontally(Entity entity, double boundingWidth, Align align){
        var outer = OuterBoxSize.from(entity).orElseThrow();
        var position  = entity.getComponent(Position.class).orElseThrow();

        switch (align.h()) {
            case LEFT -> {
                entity.addComponent(new Position(0.0, position.y()));
            }
            case RIGHT -> {
              double x =   boundingWidth - outer.width();
              entity.addComponent(new Position(x, position.y()));
            }
            case CENTER ->  {
              double x = (boundingWidth - outer.width())/2;
                entity.addComponent(new Position(x, position.y()));
            }

        }
    }
}

