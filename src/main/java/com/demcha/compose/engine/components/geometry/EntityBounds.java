package com.demcha.compose.engine.components.geometry;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import lombok.experimental.UtilityClass;

/**
 * Geometry helper for reading an entity's outer bounds from resolved layout
 * state.
 *
 * <p>
 * Bounds are derived from {@link Placement}, {@link ContentSize}, and optional
 * {@link Margin}. This helper keeps geometry reads out of {@code Entity}
 * itself.
 * </p>
 */
@UtilityClass
public class EntityBounds {

    /**
     * Returns the entity's top outer line including top margin.
     *
     * @param entity entity with resolved placement and content size
     * @return top line in document coordinates
     */
    public static double topLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        ContentSize size = entity.require(ContentSize.class);
        Margin margin = marginOf(entity);
        return placement.y() + size.height() + margin.top();
    }

    /**
     * Returns the entity's bottom outer line including bottom margin.
     *
     * @param entity entity with resolved placement
     * @return bottom line in document coordinates
     */
    public static double bottomLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        Margin margin = marginOf(entity);
        return placement.y() - margin.bottom();
    }

    /**
     * Returns the entity's right outer line including right margin.
     *
     * @param entity entity with resolved placement and content size
     * @return right line in document coordinates
     */
    public static double rightLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        ContentSize size = entity.require(ContentSize.class);
        Margin margin = marginOf(entity);
        return placement.x() + size.width() + margin.right();
    }

    /**
     * Returns the entity's left outer line including left margin.
     *
     * @param entity entity with resolved placement
     * @return left line in document coordinates
     */
    public static double leftLine(Entity entity) {
        Placement placement = entity.require(Placement.class);
        Margin margin = marginOf(entity);
        return placement.x() - margin.left();
    }

    private static Margin marginOf(Entity entity) {
        return entity.getComponent(Margin.class).orElse(Margin.zero());
    }
}
