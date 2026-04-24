package com.demcha.compose.engine.pagination;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.core.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParentContainerUpdaterTest {

    @Test
    void shouldIncreaseParentSizesWithoutMovingParents() {
        Graph graph = graph();

        boolean updated = ParentContainerUpdater.updateParentContainerSize(graph.child(), graph.manager(), -15);

        assertThat(updated).isFalse();
        assertThat(graph.parent().require(ContentSize.class).height()).isEqualTo(45.0);
        assertThat(graph.parent().require(ComputedPosition.class).y()).isEqualTo(80.0);
        assertThat(graph.grandParent().require(ContentSize.class).height()).isEqualTo(55.0);
        assertThat(graph.grandParent().require(ComputedPosition.class).y()).isEqualTo(100.0);
    }

    @Test
    void shouldIncreaseParentSizesAndMoveAncestorsWhenUpdatingPosition() {
        Graph graph = graph();

        boolean updated = ParentContainerUpdater.updateParentContainer(graph.child(), graph.manager(), -15);

        assertThat(updated).isFalse();
        assertThat(graph.parent().require(ContentSize.class).height()).isEqualTo(45.0);
        assertThat(graph.parent().require(ComputedPosition.class).y()).isEqualTo(65.0);
        assertThat(graph.grandParent().require(ContentSize.class).height()).isEqualTo(55.0);
        assertThat(graph.grandParent().require(ComputedPosition.class).y()).isEqualTo(85.0);
    }

    @SuppressWarnings("deprecation")
    @Test
    void shouldDelegateDeprecatedEntityParentUpdateWrappers() {
        Graph graph = graph();
        Offset offset = new Offset();
        offset.incrementY(-10);

        boolean updated = graph.child().updateParentContainer(graph.manager(), offset);

        assertThat(updated).isFalse();
        assertThat(graph.parent().require(ContentSize.class).height()).isEqualTo(40.0);
        assertThat(graph.parent().require(ComputedPosition.class).y()).isEqualTo(70.0);
        assertThat(graph.grandParent().require(ContentSize.class).height()).isEqualTo(50.0);
        assertThat(graph.grandParent().require(ComputedPosition.class).y()).isEqualTo(90.0);
    }

    @SuppressWarnings("deprecation")
    @Test
    void shouldDelegateDeprecatedEntitySelfResizeWrapper() {
        Graph graph = graph();

        boolean updated = graph.child().updateEntitySize(graph.manager(), -12);

        assertThat(updated).isFalse();
        assertThat(graph.child().require(ContentSize.class).height()).isEqualTo(22.0);
        assertThat(graph.child().require(ComputedPosition.class).y()).isEqualTo(60.0);
        assertThat(graph.parent().require(ContentSize.class).height()).isEqualTo(42.0);
        assertThat(graph.parent().require(ComputedPosition.class).y()).isEqualTo(80.0);
        assertThat(graph.grandParent().require(ContentSize.class).height()).isEqualTo(52.0);
    }

    private static Graph graph() {
        EntityManager manager = new EntityManager();

        Entity grandParent = sizedEntity(0, 100, 200, 40);
        Entity parent = sizedEntity(10, 80, 120, 30);
        Entity child = sizedEntity(20, 60, 60, 10);

        parent.addComponent(new ParentComponent(grandParent.getUuid()));
        child.addComponent(new ParentComponent(parent.getUuid()));
        grandParent.getChildren().add(parent.getUuid());
        parent.getChildren().add(child.getUuid());

        manager.putEntity(grandParent);
        manager.putEntity(parent);
        manager.putEntity(child);
        return new Graph(manager, grandParent, parent, child);
    }

    private static Entity sizedEntity(double x, double y, double width, double height) {
        Entity entity = new Entity();
        entity.addComponent(new ComputedPosition(x, y));
        entity.addComponent(new ContentSize(width, height));
        return entity;
    }

    private record Graph(EntityManager manager, Entity grandParent, Entity parent, Entity child) {
    }
}
