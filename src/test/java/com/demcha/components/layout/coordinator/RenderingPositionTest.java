package com.demcha.components.layout.coordinator;

import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.components.layout.coordinator.Position;
import com.demcha.compose.engine.components.layout.coordinator.RenderingPosition;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class RenderingPositionTest {

    static Entity createEntity(RenderingPosition renderingPosition, ComputedPosition computedPosition) {
        Entity e = mock(Entity.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(Optional.of(computedPosition)).when(e).getComponent(ComputedPosition.class);
        doReturn(Optional.of(renderingPosition)).when(e).getComponent(Position.class);
        return e;
    }

    static Component createComponent(double x, double y) {
        ComputedPosition position = mock(ComputedPosition.class);
        when(position.x()).thenReturn(x);
        when(position.y()).thenReturn(y);
        return position;
    }

    static RenderingPosition createRenderingPosition(double x, double y) {
        RenderingPosition position = mock(RenderingPosition.class);
        when(position.x()).thenReturn(x);
        when(position.y()).thenReturn(y);
        return position;
    }

    @Test
    void fromEntity() {

    }



}