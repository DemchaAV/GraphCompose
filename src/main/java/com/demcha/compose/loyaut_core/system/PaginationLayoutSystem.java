package com.demcha.compose.loyaut_core.system;

import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.interfaces.SystemECS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaginationLayoutSystem implements SystemECS {


    /**
     * @param entityManager
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