package com.demcha.compose.layout_core.system.utils.containerUtils;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.renderable.ChunkedBlockText;
import com.demcha.compose.layout_core.components.renderable.Container;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ContainerLayoutManager {

    /**
     * Recursively aligns child entities within their parent containers (post-order).
     *
     * A parent container is aligned only after all of its container-children are aligned.
     * Non-container parents are just marked as processed (nothing to align).
     */
    public static void process(Map<UUID, Set<UUID>> childrenByParent, EntityManager entityManager) {
        if (childrenByParent == null || childrenByParent.isEmpty()) {
            log.debug("process(): nothing to do, childrenByParent is null or empty");
            return;
        }
        Objects.requireNonNull(entityManager, "entityManager");


        // 'visited' is a clearer name than doneList for graph-style traversals.
        Set<UUID> visited = new HashSet<>();

        for (UUID parentId : childrenByParent.keySet()) {
            if (!visited.contains(parentId)) {
                if (isContainer(parentId, entityManager)) {
                    alignContainer(parentId, childrenByParent, visited, entityManager, new ArrayDeque<>());
                } else {
                    log.debug("Element Not container {}", entityManager.getEntity(parentId).orElseThrow(() -> new NoSuchElementException("Entity not found: " + parentId)));
                    visited.add(parentId);
                }
            }
        }
    }

    /**
     * Depth-first post-order alignment with cycle detection.
     */
    private static void alignContainer(
            UUID parentId,
            Map<UUID, Set<UUID>> childrenByParent,
            Set<UUID> visited,
            EntityManager entityManager,
            Deque<UUID> stackForCycleCheck
    ) {
        log.debug("Align container start: {}", parentId);

        if (!stackForCycleCheck.add(parentId)) {
            // parentId is already in the current DFS path → cycle
            throw new IllegalStateException("Cycle detected in entity hierarchy: " + stackForCycleCheck);
        }

        // Iterate children safely (use empty set when absent)
        Set<UUID> children = childrenByParent.getOrDefault(parentId, Collections.emptySet());
        for (UUID childId : children) {
            if (visited.contains(childId)) continue;

            if (isContainerSafe(childId, entityManager)) {
                alignContainer(childId, childrenByParent, visited, entityManager, stackForCycleCheck);
            } else {
                // Non-container child: nothing to align here, but still mark as visited.
                visited.add(childId);
            }
        }

        // Post-order: align AFTER children
        alignElement(parentId, entityManager);
        visited.add(parentId);

        // Pop for cycle check
        UUID popped = stackForCycleCheck.removeLast();
        assert popped.equals(parentId) : "Cycle stack corrupted";

        log.debug("Align container end: {}", parentId);
    }

    private static boolean isContainer(UUID id, EntityManager em) {
        Entity e = em.getEntity(id).orElseThrow(() -> new NoSuchElementException("Entity not found: " + id));
        return e.hasAssignable(Container.class);
    }

    private static boolean isContainerSafe(UUID id, EntityManager em) {
        return em.getEntity(id)
                .map(e -> e.hasAssignable(Container.class))
                .orElse(false);
    }

    private static void alignElement(UUID id, EntityManager em) {
        Entity parent = em.getEntity(id).orElseThrow(() -> new NoSuchElementException("Entity not found: " + id));

        if (!parent.hasAssignable(Container.class)) {
            log.trace("alignElement(): {} is not a Container — skipping.", parent);
            return;
        }

        log.info("Aligning container: {}", parent);
        ContainerAligner.align(parent, em);

        // Optional horizontal text alignment when container also holds text
        if (parent.hasAssignable(ChunkedBlockText.class)) {
            horizontallyAlign(parent, em);
        }
    }

    /**
     * Align children horizontally inside parent's inner width.
     * Uses the parent's Align component as the policy.
     */
    private static void horizontallyAlign(Entity parent, EntityManager em) {
        Align parentAlign = parent.getComponent(Align.class)
                .orElseThrow(() -> new NoSuchElementException("Align component missing on parent: " + parent));

        double innerW = InnerBoxSize.from(parent)
                .orElseThrow(() -> new NoSuchElementException("InnerBoxSize missing on parent: " + parent))
                .width();

        for (UUID childId : parent.getChildren()) {
            Entity child = em.getEntity(childId)
                    .orElseThrow(() -> new NoSuchElementException("Child entity not found: " + childId));

            Align.alignHorizontally(child, innerW, parentAlign);
        }
    }
}
