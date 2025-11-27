package com.demcha.system.utils.page_breaker;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.core.EntityManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class EntitySorter {
    // === Sorting =============================================================

    /**
     * Sorts the given map of entities by their Y-position in **descending** order (top to bottom).
     * <p>
     * Sorting is based on the {@code RenderingPosition} component of each {@link Entity}.
     * The result is a {@link Set} of {@link Map.Entry} objects, with order preserved
     * by collecting into a {@link LinkedHashSet}.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects to be sorted. Must not be null.
     * @return A {@code Set} of map entries, sorted by Y-position in descending order.
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity is missing a {@code RenderingPosition}.
     */
    public static Set<Map.Entry<UUID, Entity>> sortByYPosition(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .peek(entry -> {
                    Optional<RenderingPosition> pos = RenderingPosition.from(entry.getValue());
                    log.debug("{} -> {}", entry.getValue(), pos.orElse(null));
                })

                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Sorts the given map of entities by their Y-position in **descending** order
     * and returns the result as a new {@link LinkedHashMap}.
     * <p>
     * This method provides a convenient map-based view where the iteration order matches the sorted order.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects to be sorted. Must not be null.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity is missing a {@code RenderingPosition}.
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Loads entities by their UUID from an {@link EntityManager} and sorts them by Y-position.
     *
     * @param entityManager The entity manager to retrieve {@link Entity} objects from.
     * @param entityUuids   A set of UUIDs for the entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, Set<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(entityUuids));
    }

    /**
     * Loads entities by their UUID from an {@link EntityManager} and sorts them by Y-position.
     *
     * @param entityManager The entity manager to retrieve {@link Entity} objects from.
     * @param entityUuids   A list of UUIDs for the entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, List<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(new HashSet<>(entityUuids)));
    }

    /**
     * Sorts the given set of entities by their Y-position and returns the result as a {@link LinkedHashMap}.
     *
     * @param entities A set of entities to be sorted.
     * @return A {@code LinkedHashMap} with entities ordered by Y-position, descending.
     * @see #sortByYPositionToMap(Map)
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Set<Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.stream()
                .sorted(Comparator.comparingDouble(EntitySorter::yOf).reversed())
                .collect(Collectors.toMap(
                        Entity::getUuid,           // key mapper → UUID
                        e -> e,                  // value mapper → Entity
                        (a, b) -> a,             // merge function (shouldn't happen)
                        LinkedHashMap::new       // preserve order
                ));
    }

    /**
     * A helper method to extract the Y-coordinate from an entity's {@link RenderingPosition} component.
     *
     * @param e The entity from which to extract the coordinate.
     * @return The Y-coordinate value.
     * @throws IllegalStateException if the entity is missing a {@code RenderingPosition}.
     */
    private static double yOf(Entity e) {
        return RenderingPosition.from(e)
                .map(RenderingPosition::y)
                .orElseThrow(() -> new IllegalStateException(
                        "Entity " + e + " has no RenderingPosition"));
    }


}