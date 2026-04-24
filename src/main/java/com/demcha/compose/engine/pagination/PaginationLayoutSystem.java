package com.demcha.compose.engine.pagination;

import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.core.SystemECS;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

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
    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.pagination");


    /**
     * Converts each positioned entity into a basic single-page placement.
     *
     * @param entityManager registry containing entities with computed positions
     */
    @Override
    public void process(EntityManager entityManager) {
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("pagination.fallback.start entities={}", entityManager.getEntities().size());
        entityManager.getEntities().forEach((id, entity) -> {
            if (entity.has(ComputedPosition.class)) {
                double x = entity.getComponent(ComputedPosition.class).get().x();
                double y = entity.getComponent(ComputedPosition.class).get().y();
                ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();
                Placement placement = new Placement(x, y, contentSize.width(), contentSize.height(), 0, 0);
                entity.addComponent(placement);
            }
        });
        LIFECYCLE_LOG.debug(
                "pagination.fallback.end entities={} durationMs={}",
                entityManager.getEntities().size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

    }

}
