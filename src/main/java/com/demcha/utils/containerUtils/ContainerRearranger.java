package com.demcha.utils.containerUtils;

import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.renderable.ChunkedBlockText;
import com.demcha.components.renderable.Container;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ContainerRearranger {

    /**
     * Recursively aligns child entities within their parent containers.
     *
     * <p>This utility performs a post-order traversal (bottom-up) of the entity tree. It ensures that
     * a container's children are aligned before the container itself is aligned. This is crucial for layouts
     * where a parent's size depends on its children's dimensions.</p>
     *
     * <p>It delegates the primary alignment logic to {@link ContainerAligner} and performs additional
     * horizontal alignment for containers that also have a {@link ChunkedBlockText} component.</p>
     *
     * @param childrenByParent A map representing the entity hierarchy, where keys are parent UUIDs
     * and values are the set of their direct children's UUIDs.
     * @param entityManager The manager to retrieve entity and component data.
     */
    public static void containerAligner(Map<UUID, Set<UUID>> childrenByParent, EntityManager entityManager) {
        Set<UUID> doneList = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> parentUuid : childrenByParent.entrySet()) {
            if (doneList.contains(parentUuid.getKey())) {
                continue;
            }
            if (isContainer(parentUuid.getKey(), entityManager)) {
                alignContainer(parentUuid.getKey(), childrenByParent, doneList, entityManager);
            } else {
                doneList.add(parentUuid.getKey());
            }

        }
    }

    private static void alignContainer(UUID elementUuid, Map<UUID, Set<UUID>> childrenByParent, Set<UUID> doneList, EntityManager entityManager) {
        log.info("{} aligner container", elementUuid);
        log.debug("{} chek children if they a also container", childrenByParent.size());
        for (UUID childUuid : childrenByParent.get(elementUuid)) {
            if (doneList.contains(childUuid)) {
                continue;
            }
            if (isContainer(childUuid, entityManager)) {
                alignContainer(childUuid, childrenByParent, doneList, entityManager);
            }
        }
        log.info("{} aligner container", elementUuid);
        alignElement(elementUuid, entityManager);
        doneList.add(elementUuid);
    }


    private static boolean isContainer(UUID uuid, EntityManager entityManager) {
        Entity entity = entityManager.getEntity(uuid).orElseThrow();
        return entity.hasAssignable(Container.class);
    }


    private static void alignElement(UUID uuid, EntityManager entityManager) {
        var entityParent = entityManager.getEntity(uuid).orElseThrow();
        if (entityParent.hasAssignable(Container.class)) {
            log.info("{} is a container with component Container", entityParent);
            ContainerAligner.align(entityParent, entityManager);
            if (entityParent.hasAssignable(ChunkedBlockText.class)) {
                horizontallyAlign(entityParent, entityManager);
            }
        }
    }

    private static void horizontallyAlign(Entity parent, EntityManager entityManager) {
        Align align = parent.getComponent(Align.class).orElseThrow();
        for (UUID uuid : parent.getChildren()) {
            Entity child = entityManager.getEntity(uuid).orElseThrow();
            Align.alignHorizontally(child, InnerBoxSize.from(parent).orElseThrow().innerW(), align);
        }
    }
}
