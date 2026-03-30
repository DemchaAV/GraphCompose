package com.demcha.compose.layout_core.system;

import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.SystemECS;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple pagination fallback that converts resolved positions into single-page
 * {@code Placement} components.
 * <p>
 * This implementation does not perform advanced page breaking. Instead it maps
 * each entity's existing {@code ComputedPosition} and {@code ContentSize} to a
 * placement on page {@code 0}. It is therefore mainly useful as a minimal
 * placement bridge when the full page-breaker path is not used.
 * </p>
 */
@Slf4j
public class PaginationLayoutSystem implements SystemECS {


    /**
     * Converts each positioned entity into a basic single-page placement.
     *
     * @param entityManager registry containing entities with computed positions
     */
    @Override
    public void process(EntityManager entityManager) {
        entityManager.getEntities().forEach((id, entity) -> {
            if (entity.has(ComputedPosition.class)) {
                double x = entity.getComponent(ComputedPosition.class).get().x();
                double y = entity.getComponent(ComputedPosition.class).get().y();
                ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();
                Placement placement = new Placement(x, y, contentSize.width(), contentSize.height(), 0, 0);
                entity.addComponent(placement);
            }
        });

    }

}
