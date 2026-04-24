package com.demcha.compose.engine.pagination;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.core.EntityManager;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Pagination helper that propagates child-driven size and position changes to
 * parent containers.
 *
 * <p>
 * The page breaker uses these routines after a child moves or grows so parent
 * boxes can reflect the same change before later layout/render stages inspect
 * the tree.
 * </p>
 */
@Slf4j
@UtilityClass
public class ParentContainerUpdater {

    /**
     * Propagates a resolved offset from a child entity to its parent container.
     *
     * @param entity child entity whose parent should be updated
     * @param manager entity manager used to resolve parent entities
     * @param offset resolved offset object
     * @return {@code true} when a parent update was applied
     */
    public static boolean updateParentContainer(Entity entity, EntityManager manager, Offset offset) {
        if (offset == null) {
            log.error("Offset cannot be null");
            return false;
        }
        return updateParentContainer(entity, manager, offset.y());
    }

    /**
     * Propagates a vertical delta from a child entity to its parent container
     * chain.
     *
     * @param entity child entity whose parent should be updated
     * @param manager entity manager used to resolve parent entities
     * @param offsetY vertical delta to propagate
     * @return {@code true} when a parent update was applied
     */
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

    /**
     * Propagates a size-only delta from a child entity to its parent container
     * chain.
     *
     * @param entity child entity whose parent size should be updated
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to propagate
     * @return {@code true} when a parent size update was applied
     */
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

    /**
     * Resizes the target entity and, for negative offsets, also shifts its Y
     * position before propagating the change upward.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta or upward shift delta
     * @param entity entity to mutate
     * @return {@code true} when propagation reached a parent container
     */
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

    /**
     * Resizes the target entity without changing its own Y coordinate and then
     * propagates the size change upward.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to apply
     * @param entity entity to resize
     * @return {@code true} when propagation reached a parent container
     */
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

    /**
     * Resizes the current entity and propagates the size-only change to the parent
     * container chain.
     *
     * @param entity current entity to resize
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to apply
     * @return {@code true} when propagation reached a parent container
     */
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
