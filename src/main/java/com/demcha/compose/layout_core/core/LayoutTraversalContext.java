package com.demcha.compose.layout_core.core;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Immutable, deterministic view of the current entity hierarchy for one layout pass.
 *
 * <p>This helper keeps traversal-specific structure out of {@link EntityManager} so
 * the manager can remain the registry of entities and systems, while layout and
 * pagination reuse one canonical hierarchy snapshot.</p>
 *
 * <p>The context is built from the existing entity graph using two inputs:</p>
 * <ul>
 *   <li>{@link ParentComponent} as the authoritative parent reference</li>
 *   <li>{@link Entity#getChildren()} as the canonical sibling order when it is consistent</li>
 * </ul>
 *
 * <p>If those two sources disagree, the context appends the missing child in a
 * deterministic fallback position and logs a warning so the inconsistency stays
 * visible during maintenance and backend work.</p>
 */
@Slf4j
public final class LayoutTraversalContext {
    private final Map<UUID, UUID> parentById;
    private final Map<UUID, List<UUID>> childrenByParent;
    private final Set<UUID> roots;

    private LayoutTraversalContext(Map<UUID, UUID> parentById,
                                   Map<UUID, List<UUID>> childrenByParent,
                                   Set<UUID> roots) {
        this.parentById = parentById;
        this.childrenByParent = childrenByParent;
        this.roots = roots;
    }

    /**
     * Clears layout-owned traversal state on the manager before a new layout pass.
     *
     * <p>The resolved hierarchy is recomputed every pass, so stale layers and depth
     * metadata must not survive between runs on the same composer.</p>
     */
    public static void resetTraversalState(EntityManager entityManager) {
        entityManager.setLayers(new TreeMap<>());
        entityManager.setDepthById(new LinkedHashMap<>());
    }

    /**
     * Builds a deterministic hierarchy snapshot from the current entity graph.
     */
    public static LayoutTraversalContext from(EntityManager entityManager) {
        Map<UUID, Entity> entities = entityManager.getEntities();
        Map<UUID, UUID> parentById = buildParentById(entities);
        Map<UUID, List<UUID>> childrenByParent = buildChildrenByParent(entities, parentById);
        Set<UUID> roots = buildRoots(entities, parentById);

        return new LayoutTraversalContext(
                Collections.unmodifiableMap(parentById),
                Collections.unmodifiableMap(childrenByParent),
                Collections.unmodifiableSet(roots));
    }

    /**
     * Returns an unmodifiable map from each child entity ID to its parent entity ID.
     *
     * <p>Only entities that carry a {@link ParentComponent} appear as keys.</p>
     *
     * @return child → parent mapping, never {@code null}
     */
    public Map<UUID, UUID> parentById() {
        return parentById;
    }

    /**
     * Returns an unmodifiable map from each parent entity ID to its ordered list of
     * child entity IDs.
     *
     * <p>The child order follows {@link Entity#getChildren()} when consistent with
     * {@link ParentComponent}. Inconsistencies are resolved deterministically and
     * logged as warnings.</p>
     *
     * @return parent → ordered children mapping, never {@code null}
     */
    public Map<UUID, List<UUID>> childrenByParent() {
        return childrenByParent;
    }

    /**
     * Returns the set of root entity IDs — entities that have no resolved parent.
     *
     * <p>If no roots can be computed (e.g. due to a cycle), the set falls back to all
     * known entities so the cycle becomes visible at traversal time.</p>
     *
     * @return root entity IDs in insertion order, never {@code null} or empty
     */
    public Set<UUID> roots() {
        return roots;
    }

    /**
     * Walks all entities and indexes child → parent relationships from
     * {@link ParentComponent}. Insertion order mirrors the entity map.
     */
    private static Map<UUID, UUID> buildParentById(Map<UUID, Entity> entities) {
        Map<UUID, UUID> parentById = new LinkedHashMap<>();

        for (Map.Entry<UUID, Entity> entry : entities.entrySet()) {
            entry.getValue()
                    .getComponent(ParentComponent.class)
                    .ifPresent(parentComponent -> parentById.put(entry.getKey(), parentComponent.uuid()));
        }

        return parentById;
    }

    /**
     * Builds the ordered children map by reconciling {@link Entity#getChildren()}
     * with {@link ParentComponent}. Stale or inconsistent references are logged and
     * handled deterministically.
     */
    private static Map<UUID, List<UUID>> buildChildrenByParent(Map<UUID, Entity> entities,
                                                               Map<UUID, UUID> parentById) {
        Map<UUID, List<UUID>> childrenByParent = new LinkedHashMap<>();
        Set<UUID> assignedChildren = new LinkedHashSet<>();

        for (Entity parent : entities.values()) {
            List<UUID> orderedChildren = new ArrayList<>();

            for (UUID childId : parent.getChildren()) {
                UUID actualParentId = parentById.get(childId);
                if (actualParentId == null) {
                    log.warn("Traversal context skipped stale child reference {} from parent {} because the child has no ParentComponent.",
                            childId, parent.getUuid());
                    continue;
                }
                if (!actualParentId.equals(parent.getUuid())) {
                    log.warn("Traversal context skipped stale child reference {} from parent {} because ParentComponent points to {}.",
                            childId, parent.getUuid(), actualParentId);
                    continue;
                }
                if (!entities.containsKey(childId)) {
                    log.warn("Traversal context skipped missing child {} referenced by parent {}.", childId, parent.getUuid());
                    continue;
                }
                orderedChildren.add(childId);
                assignedChildren.add(childId);
            }

            if (!orderedChildren.isEmpty()) {
                childrenByParent.put(parent.getUuid(), List.copyOf(orderedChildren));
            }
        }

        for (Map.Entry<UUID, UUID> entry : parentById.entrySet()) {
            UUID childId = entry.getKey();
            UUID parentId = entry.getValue();

            if (assignedChildren.contains(childId)) {
                continue;
            }
            if (!entities.containsKey(childId) || !entities.containsKey(parentId)) {
                continue;
            }

            log.warn("Traversal context appended child {} to parent {} because ParentComponent exists but the parent's children list is missing it.",
                    childId, parentId);
            childrenByParent
                    .computeIfAbsent(parentId, key -> new ArrayList<>())
                    .add(childId);
        }

        Map<UUID, List<UUID>> immutableChildren = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<UUID>> entry : childrenByParent.entrySet()) {
            immutableChildren.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return immutableChildren;
    }

    /**
     * Identifies root entities — those with no parent or whose parent is missing
     * from the entity set. Falls back to all entities if no roots are found.
     */
    private static Set<UUID> buildRoots(Map<UUID, Entity> entities, Map<UUID, UUID> parentById) {
        Set<UUID> roots = new LinkedHashSet<>();

        for (UUID entityId : entities.keySet()) {
            UUID parentId = parentById.get(entityId);
            if (parentId == null) {
                roots.add(entityId);
                continue;
            }
            if (!entities.containsKey(parentId)) {
                roots.add(entityId);
                log.warn("Traversal context treats entity {} as root because parent {} is missing.", entityId, parentId);
            }
        }

        if (roots.isEmpty()) {
            log.warn("Traversal context computed no roots; falling back to all entities to expose the cycle at traversal time.");
            roots.addAll(entities.keySet());
        }

        return new LinkedHashSet<>(roots);
    }
}
