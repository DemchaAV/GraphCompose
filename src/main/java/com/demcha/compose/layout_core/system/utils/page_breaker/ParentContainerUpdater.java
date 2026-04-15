package com.demcha.compose.layout_core.system.utils.page_breaker;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ParentContainerUpdater {

    public static boolean updateParentContainer(Entity entity, EntityManager manager, Offset offset) {
        if (offset == null) {
            log.error("Offset cannot be null");
            return false;
        }
        return updateParentContainer(entity, manager, offset.y());
    }

    public static boolean updateParentContainer(Entity entity, EntityManager manager, double offsetY) {
        if (offsetY == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping parent container position update because offset is zero for {}", entity.getUuid());
            }
            return false;
        }

        ParentComponent parentComponent = entity.getComponent(ParentComponent.class).orElse(null);
        if (parentComponent == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent component missing for entity [{}]; parent position update skipped.", entity.getUuid());
            }
            return false;
        }

        Entity parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        if (parent == null) {
            log.error("Parent entity not found in manager for UUID {}", parentComponent.uuid());
            return false;
        }
        return updateEntitySizeAndPosition(manager, offsetY, parent);
    }

    public static boolean updateParentContainerSize(Entity entity, EntityManager manager, double offsetY) {
        if (offsetY == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping parent container size update because offset is zero for {}", entity.getUuid());
            }
            return false;
        }

        ParentComponent parentComponent = entity.getComponent(ParentComponent.class).orElse(null);
        if (parentComponent == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent component missing for entity [{}]; parent size update skipped.", entity.getUuid());
            }
            return false;
        }

        Entity parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        if (parent == null) {
            log.error("Parent entity not found in manager for UUID {}", parentComponent.uuid());
            return false;
        }
        return updateEntitySize(manager, offsetY, parent);
    }

    public static boolean updateEntitySizeAndPosition(EntityManager manager, double offsetY, @NonNull Entity entity) {
        ComputedPosition computedPosition = entity.require(ComputedPosition.class);
        ContentSize size = entity.require(ContentSize.class);

        if (offsetY < 0) {
            double newY = computedPosition.y() + offsetY;
            double newHeight = size.height() + Math.abs(offsetY);
            entity.addComponent(new ComputedPosition(computedPosition.x(), newY));
            entity.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            double newHeight = size.height() + offsetY;
            entity.addComponent(new ContentSize(size.width(), newHeight));
        }

        return updateParentContainer(entity, manager, offsetY);
    }

    public static boolean updateEntitySize(EntityManager manager, double offsetY, @NonNull Entity entity) {
        entity.require(ComputedPosition.class);
        ContentSize size = entity.require(ContentSize.class);

        if (offsetY < 0) {
            double newHeight = size.height() + Math.abs(offsetY);
            entity.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            double newHeight = size.height() + offsetY;
            entity.addComponent(new ContentSize(size.width(), newHeight));
        }

        return updateParentContainerSize(entity, manager, offsetY);
    }

    public static boolean updateCurrentEntitySize(Entity entity, EntityManager manager, double offsetY) {
        entity.require(ComputedPosition.class);
        ContentSize size = entity.require(ContentSize.class);

        if (offsetY < 0) {
            double newHeight = size.height() + Math.abs(offsetY);
            entity.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            double newHeight = size.height() + offsetY;
            entity.addComponent(new ContentSize(size.width(), newHeight));
        }

        return updateParentContainerSize(entity, manager, offsetY);
    }
}
