package com.demcha.components.core;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityTest {

    // --- helper to build a mock that calls real default methods
    private static Entity boundingTestMockEntity(ContentSize contentSize, Placement placement, Margin margin) {
        Entity e = mock(Entity.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(Optional.of(placement)).when(e).getComponent(Placement.class);
        doReturn(Optional.of(contentSize)).when(e).getComponent(ContentSize.class);
        doReturn(Optional.of(margin)).when(e).getComponent(Margin.class);
        return e;
    }

    private static Margin mockMargin(double marginTop, double marginBottom, double marginLeft, double marginRight) {
        Margin margin = mock(Margin.class);
        when(margin.top()).thenReturn(marginTop);
        when(margin.bottom()).thenReturn(marginBottom);
        when(margin.left()).thenReturn(marginLeft);
        when(margin.right()).thenReturn(marginRight);
        return margin;
    }

    private static Placement mockPlacement(double x, double y, double width, double height, int startPage, int endPage) {
        Placement placement = mock(Placement.class);

        when(placement.x()).thenReturn(x);
        when(placement.y()).thenReturn(y);
        when(placement.width()).thenReturn(width);
        when(placement.height()).thenReturn(height);
        when(placement.startPage()).thenReturn(startPage);
        when(placement.endPage()).thenReturn(endPage);

        return placement;
    }

    private static Placement mockPlacement(double x, double y) {
        return mockPlacement(x, y, 0.0, 0.0, 0, 0);
    }

    private static ContentSize mockContentSize(double width, double height) {
        ContentSize contentSize = mock(ContentSize.class);

        when(contentSize.width()).thenReturn(width);
        when(contentSize.height()).thenReturn(height);
        return contentSize;
    }


    @ParameterizedTest(name = "[{index}] y={0}, h={1}, top={2} -> expected {3}")
    @DisplayName("boundingTopLine() with multiple scenarios")
    @CsvSource({
            // y,  height, marginTop, expected
            "10,  20,     10,        40",
            "0,   0,      0,         0",
            "100, 50,     5,         155",
            "-10, 30,     0,         20",
            "5.5, 12.3,   1.2,       19.0"
    })
    void boundingTopLine_param(double y, double height, double marginTop, double expected) {
        Margin mockMargin = mockMargin(marginTop, 0.0, 0.0, 0.0);
        Placement placement = mockPlacement(0.0, y);
        ContentSize size = mockContentSize(1.0, height);

        Entity e = boundingTestMockEntity(size, placement, mockMargin);
        double actual = e.boundingTopLine();
        assertEquals(expected, actual, 1e-9, "computed top line mismatch");
    }

    @ParameterizedTest(name = "[{index}] y={0}, h={1}, bottom={2} -> expected {3}")
    @DisplayName("boundingBottomLine with multiple scenarios")
    @CsvSource({
            // y,  height, marginBottom, expected
            "10,  20,     10,        0",
            "0,   0,      0,         0",
            "100, 50,     5,         95",
            "-10, 30,     0,         -10",
            "5.5, 12.3,   1.2,       4.3"
    })
    void boundingBottomLine_param(double y, double height, double marginBottom, double expected) {
        Margin mockMargin = mockMargin(0.0, marginBottom, 0.0, 0.0);

        Placement placement = mockPlacement(0.0, y);
        ContentSize size = mockContentSize(1.0, height);

        Entity e = boundingTestMockEntity(size, placement, mockMargin);
        double actual = e.boundingBottomLine();
        assertEquals(expected, actual, 1e-9, "computed Bottom line mismatch");
    }


    @ParameterizedTest(name = "[{index}] x={0}, w={1}, left={2} -> expected {3}")
    @DisplayName("boundingLeftLine() with multiple scenarios")
    @CsvSource({
            // x,  width, marginTop, expected
            "10,  20,     10,        0",
            "0,   0,      0,         0",
            "100, 50,     5,         95",
            "-10, 30,     0,         -10",
            "5.5, 12.3,   1.2,       4.3"
    })
    void boundingLeftLine_param(double x, double width, double marginLeft, double expected) {
        Margin mockMargin = mockMargin(0.0, 0.0, marginLeft, 0.0);

        Placement placement = mockPlacement(x, 0.0);
        ContentSize size = mockContentSize(width, 1.0);

        Entity e = boundingTestMockEntity(size, placement, mockMargin);
        double actual = e.boundingLeftLine();
        assertEquals(expected, actual, 1e-9, "computed top Left mismatch");
    }

    @ParameterizedTest(name = "[{index}] x={0}, w={1}, rigth={2} -> expected {3}")
    @DisplayName("boundingRightLine() with multiple scenarios")
    @CsvSource({
            // x,  width, marginBottom, expected
            "10,  20,     10,        40",
            "0,   0,      0,         0",
            "100, 50,     5,         155",
            "-10, 30,     0,         20",
            "5.5, 12.3,   1.2,       19.0"
    })
    void boundingRightLine_param(double x, double width, double marginRight, double expected) {
        Margin mockMargin = mockMargin(0.0, 0.0, 0.0, marginRight);

        Placement placement = mockPlacement(x, 0.0);
        ContentSize size = mockContentSize(width, 1.0);

        Entity e = boundingTestMockEntity(size, placement, mockMargin);
        double actual = e.boundingRightLine();
        assertEquals(expected, actual, 1e-9, "computed Right line mismatch");
    }



    @Test
    @DisplayName("All bounding lines throw NoSuchElementException if Placement is missing")
    void boundingLines_throwExceptions_whenPlacementIsMissing() {
        // 1. Arrange (Setup is the same)
        Entity e = mock(Entity.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(Optional.empty()).when(e).getComponent(Placement.class);

        // 2. Act & Assert (Grouped with assertAll)
        assertAll("All bounding methods should throw when Placement is missing",
                () -> {
                    Exception ex = assertThrows(NoSuchElementException.class,
                            () -> e.boundingTopLine(), "boundingTopLine failed to throw");
                    assertEquals("Missing component Placement.class", ex.getMessage());
                },
                () -> {
                    Exception ex = assertThrows(NoSuchElementException.class,
                            () -> e.boundingBottomLine(), "boundingBottomLine failed to throw");
                    assertEquals("Missing component Placement.class", ex.getMessage());
                },
                () -> {
                    Exception ex = assertThrows(NoSuchElementException.class,
                            () -> e.boundingLeftLine(), "boundingLeftLine failed to throw");
                    assertEquals("Missing component Placement.class", ex.getMessage());
                },
                () -> {
                    Exception ex = assertThrows(NoSuchElementException.class,
                            () -> e.boundingRightLine(), "boundingRightLine failed to throw");
                    assertEquals("Missing component Placement.class", ex.getMessage());
                }
        );
    }
}