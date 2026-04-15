package com.demcha.compose.layout_core.system.rendering;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityRenderOrderTest {

    @Test
    void shouldSortByResolvedCoordinatesUsingDefaultZeroMargin() {
        EntityManager manager = new EntityManager();
        Entity baseEntity = positionedEntity(10, 50, null);
        Entity shiftedRightEntity = positionedEntity(10, 50, Margin.left(5));
        Entity raisedEntity = positionedEntity(10, 49, Margin.bottom(2));

        manager.putEntity(baseEntity);
        manager.putEntity(shiftedRightEntity);
        manager.putEntity(raisedEntity);

        LinkedHashMap<UUID, Entity> ordered = EntityRenderOrder.sortByRenderingPosition(
                manager,
                List.of(shiftedRightEntity.getUuid(), baseEntity.getUuid(), raisedEntity.getUuid()));

        assertThat(ordered.keySet())
                .containsExactly(raisedEntity.getUuid(), baseEntity.getUuid(), shiftedRightEntity.getUuid());
    }

    @Test
    void shouldPreserveOriginalLayerOrderWhenRenderingCoordinatesMatch() {
        EntityManager manager = new EntityManager();
        Entity firstEntity = positionedEntity(10, 50, null);
        Entity secondEntity = positionedEntity(10, 50, Margin.zero());

        manager.putEntity(firstEntity);
        manager.putEntity(secondEntity);

        LinkedHashMap<UUID, Entity> ordered = EntityRenderOrder.sortByRenderingPosition(
                manager,
                List.of(secondEntity.getUuid(), firstEntity.getUuid()));

        assertThat(ordered.keySet())
                .containsExactly(secondEntity.getUuid(), firstEntity.getUuid());
    }

    @Test
    void shouldSkipMissingAndDuplicateEntityIds() {
        EntityManager manager = new EntityManager();
        Entity lowerEntity = positionedEntity(0, 0, null);
        Entity upperEntity = positionedEntity(0, 10, null);
        UUID missingEntityId = UUID.randomUUID();

        manager.putEntity(lowerEntity);
        manager.putEntity(upperEntity);

        LinkedHashMap<UUID, Entity> ordered = EntityRenderOrder.sortByRenderingPosition(
                manager,
                List.of(missingEntityId, lowerEntity.getUuid(), lowerEntity.getUuid(), upperEntity.getUuid()));

        assertThat(ordered.keySet())
                .containsExactly(upperEntity.getUuid(), lowerEntity.getUuid());
        assertThat(ordered).hasSize(2);
    }

    private static Entity positionedEntity(double x, double y, Margin margin) {
        Entity entity = new Entity();
        entity.addComponent(new ComputedPosition(x, y));
        if (margin != null) {
            entity.addComponent(margin);
        }
        return entity;
    }
}
