package com.demcha.components.layout;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.Position;
import lombok.extern.slf4j.Slf4j;

/**
 * Align = like Anchor, but used when local Position is absent.
 * If Position is present, Align acts as a base; Position is an offset from that base.
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

