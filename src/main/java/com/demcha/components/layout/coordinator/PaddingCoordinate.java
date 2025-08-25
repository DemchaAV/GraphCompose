package com.demcha.components.layout.coordinator;

import com.demcha.components.core.Entity;
import com.demcha.components.style.Padding;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a coordinate adjusted by padding relative to a computed position (x, y).
 * <p>
 * Typical usage:
 * <ul>
 *   <li>Apply the <b>parent's</b> padding to a child's computed position (most common in layout engines).</li>
 *   <li>Apply an entity's own padding to its computed position (less common, but supported).</li>
 * </ul>
 */
@Slf4j
public record PaddingCoordinate(double x, double y) {

    /**
     * Creates a {@link PaddingCoordinate} by using the entity's already-present {@link ComputedPosition}
     * and applying the entity's own {@link Padding}.
     * <p>
     * ⚠️ This assumes the entity already has a {@link ComputedPosition} stored.
     * If not, compute one first (e.g., {@link ComputedPosition#from(Entity, Entity)}).
     *
     * @param entity the entity that already stores its {@link ComputedPosition}
     * @return padding-adjusted coordinate based on the entity's own padding
     * @throws IllegalStateException if {@code entity} has no {@link ComputedPosition}
     */
    public static PaddingCoordinate from(@NonNull Entity entity) {

        var renderingPos = RenderingPosition.from(entity);

        var padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var result = from(renderingPos, padding);
        log.debug("PaddingCoordinate.from(entity={}) -> {}", entity, result);
        return result;
    }

    /**
     * Low-level factory: apply the given {@link Padding} to a provided {@link ComputedPosition}.
     * <p>
     * This is useful when you have already computed the position, and you explicitly
     * know which parrentPadding (parent's or entity's) you want to apply.
     *
     * @param position the base position
     * @param parrentPadding  the parrentPadding to apply
     * @return parrentPadding-adjusted coordinate
     */
    public static PaddingCoordinate from(@NonNull RenderingPosition position, @NonNull Padding parrentPadding) {
        double px = position.x() + parrentPadding.left() ;
        double py = position.y() + parrentPadding.bottom() ;
        var result = new PaddingCoordinate(px, py);
        log.debug("PaddingCoordinate.from(position={}, parrentPadding={}) -> {}", position, parrentPadding, result);
        return result;
    }
    public  static PaddingCoordinate zero(){
        return new PaddingCoordinate(0, 0);
    }
}
