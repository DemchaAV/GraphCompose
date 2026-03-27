package com.demcha.components.layout;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.PaddingCoordinate;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.style.Margin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ComputedPositionTest {

    @Test
    @DisplayName("zero() returns (0,0)")
    void zero_returns00() {
        ComputedPosition cp = ComputedPosition.zero();
        assertThat(cp.x()).isZero();
        assertThat(cp.y()).isZero();
    }

    @Test
    @DisplayName("from(child, innerBox) uses existing Anchor.getComputedPosition(...)")
    void from_child_inner_usesExistingAnchor() {
        // Arrange
        Entity child = mock(Entity.class);
        Anchor anchor = mock(Anchor.class);
        InnerBoxSize inner = new InnerBoxSize(100, 200);
        ComputedPosition expected = new ComputedPosition(12.5, 34.5);

        when(child.getComponent(Anchor.class)).thenReturn(Optional.of(anchor));
        when(anchor.getComputedPosition(child, inner)).thenReturn(expected);


        // Act
        ComputedPosition actual = ComputedPosition.from(child, inner, PaddingCoordinate.zero());

        // Assert
        assertThat(actual).isEqualTo(expected);
        verify(child, never()).addComponent(any()); // already had anchor
        verify(anchor).getComputedPosition(child, inner);
    }

    @Test
    @DisplayName("from(child, innerBox) creates and adds default Anchor when missing, then delegates")
    void from_child_inner_addsDefaultAnchorWhenMissing() {
        // Arrange
        Entity child = mock(Entity.class);
        InnerBoxSize inner = new InnerBoxSize(80, 60);
        Anchor defaultAnchor = mock(Anchor.class);
        Position position  = mock(Position.class);
        ComputedPosition expected = new ComputedPosition(5, 7);

        when(child.getComponent(ContentSize.class)).thenReturn(Optional.of(new ContentSize(80,80)));

        // FIX: Simulate that the Anchor component is MISSING.
        when(child.getComponent(Anchor.class)).thenReturn(Optional.empty());
        when (child.getComponent(Position.class)).thenReturn(Optional.of(new Position(5,7)));

        // Mock the static method that is *actually* used for the default anchor.
        try (MockedStatic<Anchor> ms = mockStatic(Anchor.class)) {
            ms.when(Anchor::defaultAnchor).thenReturn(defaultAnchor);
            when(defaultAnchor.getComputedPosition(child, inner)).thenReturn(expected);


            // Act

            ComputedPosition actual = ComputedPosition.from(child, inner, PaddingCoordinate.zero());

            // Assert
            assertThat(actual).isEqualTo(expected);
            verify(child).addComponent(defaultAnchor); // This will now be called
            verify(defaultAnchor).getComputedPosition(child, inner); // This will now be called
            ms.verify(Anchor::defaultAnchor); // This will now be called
        }
    }

    @Test
    @DisplayName("from(child, parent) calls InnerBoxSize.from(parent) and delegates to anchor")
    void from_child_parent_usesInnerBoxFromParent() {
        // Arrange
        Entity child = mock(Entity.class);
        Entity parent = mock(Entity.class);
        Anchor anchor = mock(Anchor.class);
        InnerBoxSize parentInner = new InnerBoxSize(120, 240);
        ComputedPosition expected = new ComputedPosition(10, 20);

        // Anchor present on child
        when(child.getComponent(Anchor.class)).thenReturn(Optional.of(anchor));
        when(anchor.getComputedPosition(eq(child), eq(parentInner))).thenReturn(expected);

        // If PaddingCoordinate.from(...) looks up ComputedPosition on the entity, stub it:
        when(child.getComponent(ComputedPosition.class)).thenReturn(Optional.of(ComputedPosition.zero()));

        try (MockedStatic<InnerBoxSize> innerMs = mockStatic(InnerBoxSize.class);
             MockedStatic<PaddingCoordinate> padMs = mockStatic(PaddingCoordinate.class)) {

            innerMs.when(() -> InnerBoxSize.from(parent)).thenReturn(Optional.of(parentInner));

            // Return a harmless padding so the code path is satisfied
            padMs.when(() -> PaddingCoordinate.from(any())).thenReturn(new PaddingCoordinate(0, 0));

            // Act
            ComputedPosition actual = ComputedPosition.from(child, parent);

            // Assert
            assertThat(actual).isEqualTo(expected);
            innerMs.verify(() -> InnerBoxSize.from(parent));
            verify(anchor).getComputedPosition(child, parentInner);
        }
    }


    @Test
    @DisplayName("from(child, pageSize) wraps Canvas in InnerBoxSize(width,height) and delegates")
    void from_child_pageSize_wrapsIntoInnerBox() {
        // Arrange
        Entity child = mock(Entity.class);
        Anchor anchor = mock(Anchor.class);
        Canvas pageSize = mock(Canvas.class);
        when(pageSize.width()).thenReturn(595.0f);
        when(pageSize.height()).thenReturn(842.0f);
        when(pageSize.margin()).thenReturn(Margin.zero());

        when(child.getComponent(Anchor.class)).thenReturn(Optional.of(anchor));
        ComputedPosition expected = new ComputedPosition(3, 4);

        // We want to capture the InnerBoxSize passed to anchor.getComputedPosition
        ArgumentCaptor<InnerBoxSize> innerCaptor = ArgumentCaptor.forClass(InnerBoxSize.class);
        when(anchor.getComputedPosition(eq(child), innerCaptor.capture())).thenReturn(expected);

        // Act
        ComputedPosition actual = ComputedPosition.from(child, pageSize);

        // Assert
        assertThat(actual).isEqualTo(expected);

        InnerBoxSize innerUsed = innerCaptor.getValue();
        assertThat(innerUsed.width()).isEqualTo(595.0);
        assertThat(innerUsed.height()).isEqualTo(842.0);

        verify(anchor).getComputedPosition(eq(child), any(InnerBoxSize.class));
    }
}

