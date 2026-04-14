package com.demcha.compose.layout_core.system.rendering;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderingPosition;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.experimental.UtilityClass;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stable renderer-neutral ordering for already-laid-out entities.
 *
 * <p>This helper belongs to the rendering layer rather than pagination because it
 * only answers "in what order should resolved entities be handed to a renderer?"
 * It does not decide page-breaking or parent/child pagination rules.</p>
 *
 * <p>The ordering contract is:</p>
 * <ol>
 *   <li>higher {@link RenderingPosition#y()} first (top to bottom in engine coordinates)</li>
 *   <li>lower {@link RenderingPosition#x()} first for stable left-to-right ties</li>
 *   <li>original layer order when provided</li>
 *   <li>{@link UUID} as the final deterministic fallback</li>
 * </ol>
 */
@UtilityClass
public class EntityRenderOrder {

    /**
     * Resolves entities from the manager and returns them sorted by rendering position.
     *
     * <p>Duplicate or missing entity IDs are silently skipped. The returned map
     * preserves the computed render order.</p>
     *
     * @param entityManager entity registry to resolve entities from
     * @param entityUuids   ordered list of entity IDs to include
     * @return entities sorted by the {@linkplain #renderOrderComparator rendering order}
     */
    public static LinkedHashMap<UUID, Entity> sortByRenderingPosition(EntityManager entityManager, List<UUID> entityUuids) {
        Objects.requireNonNull(entityManager, "entityManager must not be null");
        Objects.requireNonNull(entityUuids, "entityUuids must not be null");

        Map<UUID, Entity> entities = new LinkedHashMap<>();
        Map<UUID, Integer> originalOrder = new HashMap<>();

        for (int i = 0; i < entityUuids.size(); i++) {
            UUID entityId = entityUuids.get(i);
            Entity entity = entityManager.getEntity(entityId).orElse(null);
            if (entity == null || entities.containsKey(entityId)) {
                continue;
            }
            entities.put(entityId, entity);
            originalOrder.put(entityId, i);
        }

        return entities.entrySet().stream()
                .sorted(renderOrderComparator(originalOrder))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * Builds the render-order comparator using Y descending, X ascending, original
     * layer index, and UUID string as the final deterministic fallback.
     */
    private static Comparator<Map.Entry<UUID, Entity>> renderOrderComparator(Map<UUID, Integer> originalOrder) {
        return Comparator
                .comparingDouble((Map.Entry<UUID, Entity> entry) -> renderingPosition(entry.getValue()).y())
                .reversed()
                .thenComparingDouble(entry -> renderingPosition(entry.getValue()).x())
                .thenComparingInt(entry -> originalOrder.getOrDefault(entry.getKey(), Integer.MAX_VALUE))
                .thenComparing(entry -> entry.getKey().toString());
    }

    private static RenderingPosition renderingPosition(Entity entity) {
        return RenderingPosition.from(entity)
                .orElseThrow(() -> new IllegalStateException("Entity " + entity + " has no RenderingPosition"));
    }
}
