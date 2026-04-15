package com.demcha.compose.layout_core.system.rendering;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderingPosition;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.experimental.UtilityClass;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable renderer-neutral ordering for already-laid-out entities.
 *
 * <p>
 * This helper belongs to the rendering layer rather than pagination because it
 * only answers "in what order should resolved entities be handed to a
 * renderer?"
 * It does not decide page-breaking or parent/child pagination rules.
 * </p>
 *
 * <p>
 * The ordering contract is:
 * </p>
 * <ol>
 * <li>higher {@link RenderingPosition#y()} first (top to bottom in engine
 * coordinates)</li>
 * <li>lower {@link RenderingPosition#x()} first for stable left-to-right
 * ties</li>
 * <li>original layer order when provided</li>
 * <li>{@link UUID} as the final deterministic fallback</li>
 * </ol>
 */
@UtilityClass
public class EntityRenderOrder {

    /**
     * Resolves entities from the manager and returns them sorted by rendering
     * position.
     *
     * <p>
     * Duplicate or missing entity IDs are silently skipped. The returned map
     * preserves the computed render order.
     * </p>
     *
     * @param entityManager entity registry to resolve entities from
     * @param entityUuids   ordered list of entity IDs to include
     * @return entities sorted by the {@linkplain #renderOrderComparator rendering
     *         order}
     */
    public static LinkedHashMap<UUID, Entity> sortByRenderingPosition(EntityManager entityManager,
            List<UUID> entityUuids) {
        Objects.requireNonNull(entityManager, "entityManager must not be null");
        Objects.requireNonNull(entityUuids, "entityUuids must not be null");

        LinkedHashMap<UUID, Entity> entities = new LinkedHashMap<>();
        HashSet<UUID> seen = new HashSet<>();

        for (int i = 0; i < entityUuids.size(); i++) {
            UUID entityId = entityUuids.get(i);
            if (!seen.add(entityId)) {
                continue;
            }
            Entity entity = entityManager.getEntity(entityId).orElse(null);
            if (entity == null) {
                continue;
            }
            entities.put(entityId, entity);
        }

        if (entities.size() <= 1) {
            return entities;
        }

        List<RenderEntry> resolvedEntries = new java.util.ArrayList<>(entities.size());
        int originalIndex = 0;
        for (var entry : entities.entrySet()) {
            resolvedEntries.add(resolveRenderEntry(entry.getKey(), entry.getValue(), originalIndex++));
        }

        resolvedEntries.sort(renderOrderComparator());

        LinkedHashMap<UUID, Entity> ordered = new LinkedHashMap<>(resolvedEntries.size());
        for (RenderEntry entry : resolvedEntries) {
            ordered.put(entry.id(), entry.entity());
        }
        return ordered;
    }

    /**
     * Builds the render-order comparator using Y descending, X ascending, original
     * layer index, and UUID string as the final deterministic fallback.
     */
    private static Comparator<RenderEntry> renderOrderComparator() {
        return Comparator
                .comparingDouble(RenderEntry::y)
                .reversed()
                .thenComparingDouble(RenderEntry::x)
                .thenComparingInt(RenderEntry::originalIndex)
                .thenComparing(entry -> entry.id().toString());
    }

    private static RenderEntry resolveRenderEntry(UUID entityId, Entity entity, int originalIndex) {
        ComputedPosition computedPosition = entity.getComponent(ComputedPosition.class)
                .orElseThrow(() -> new IllegalStateException("Entity " + entity + " has no RenderingPosition"));
        Margin margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        double x = computedPosition.x() + margin.left();
        double y = computedPosition.y() + margin.bottom();
        return new RenderEntry(entityId, entity, x, y, originalIndex);
    }

    private record RenderEntry(UUID id, Entity entity, double x, double y, int originalIndex) {
    }
}
