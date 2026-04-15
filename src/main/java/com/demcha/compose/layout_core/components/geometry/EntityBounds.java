package com.demcha.compose.layout_core.components.geometry;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Margin;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityBounds {

    public static double topLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        ContentSize size = entity.require(ContentSize.class);
        Margin margin = marginOf(entity);
        return placement.y() + size.height() + margin.top();
    }

    public static double bottomLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        Margin margin = marginOf(entity);
        return placement.y() - margin.bottom();
    }

    public static double rightLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        ContentSize size = entity.require(ContentSize.class);
        Margin margin = marginOf(entity);
        return placement.x() + size.width() + margin.right();
    }

    public static double leftLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        Margin margin = marginOf(entity);
        return placement.x() - margin.left();
    }

    private static Margin marginOf(Entity entity) {
        return entity.getComponent(Margin.class).orElse(Margin.zero());
    }
}
