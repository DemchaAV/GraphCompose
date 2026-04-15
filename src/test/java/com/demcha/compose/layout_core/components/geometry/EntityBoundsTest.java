package com.demcha.compose.layout_core.components.geometry;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Margin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityBoundsTest {

    @Test
    void shouldCalculateBoundsFromPlacementContentSizeAndMargin() {
        Entity entity = new Entity();
        entity.addComponent(new Placement(20, 40, 100, 60, 0, 0));
        entity.addComponent(new ContentSize(100, 60));
        entity.addComponent(new Margin(7, 11, 13, 17));

        assertThat(EntityBounds.topLine(entity)).isEqualTo(107.0);
        assertThat(EntityBounds.bottomLine(entity)).isEqualTo(27.0);
        assertThat(EntityBounds.rightLine(entity)).isEqualTo(131.0);
        assertThat(EntityBounds.leftLine(entity)).isEqualTo(3.0);
    }

    @SuppressWarnings("deprecation")
    @Test
    void shouldExposeSameBoundsThroughDeprecatedEntityWrappers() {
        Entity entity = new Entity();
        entity.addComponent(new Placement(12, 25, 40, 30, 0, 0));
        entity.addComponent(new ContentSize(40, 30));
        entity.addComponent(new Margin(2, 3, 5, 7));

        assertThat(entity.boundingTopLine()).isEqualTo(EntityBounds.topLine(entity));
        assertThat(entity.boundingBottomLine()).isEqualTo(EntityBounds.bottomLine(entity));
        assertThat(entity.boundingRightLine()).isEqualTo(EntityBounds.rightLine(entity));
        assertThat(entity.boundingLeftLine()).isEqualTo(EntityBounds.leftLine(entity));
    }
}
