package com.demcha.components.core;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

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

    @ParameterizedTest(name = "[{index}] x={0}, y={1}, w={2}, h={3}, sp={4}, ep={5} ")
    @CsvSource({
            // x,   y  height, width, starPage, endsPage
            "10,  20,     100,       90 , 1 , 2",
            "0,  2,     200,       35 , 1 , 1"
    })
    private static Placement mockPlacement(double x, double y) {
        return mockPlacement(x, y, 0.0, 0.0, 0, 0);
    }

    private static ContentSize mockContentSize(double width, double height) {
        ContentSize contentSize = mock(ContentSize.class);

        when(contentSize.width()).thenReturn(width);
        when(contentSize.height()).thenReturn(height);
        return contentSize;
    }

    static Stream<Arguments> marginPlacementParam() {
        return Stream.of(
                Arguments.of(0.0, 0.0, 0.0, 0.0),
                Arguments.of(2, 5, 2, 3),
                Arguments.of(5.5, 2.1, 0.0, 0.0),
                Arguments.of(10, 12, -3, 2)
        );

    }

    private static Margin mockMargin(double marginTop, double marginBottom, double marginLeft, double marginRight) {
        Margin margin = mock(Margin.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(marginTop).when(margin.top());
        doReturn(marginBottom).when(margin.bottom());
        doReturn(marginRight).when(margin.right());
        doReturn(marginLeft).when(margin.left());

        return margin;
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

    @ParameterizedTest(name = "[{index}] t={0}, b={1}, l={2}, r{3}")
    @DisplayName("Margin mock factory provides expected getters")
    @MethodSource("marginPlacementParam")
    void testMargin(double top, double left, double right, double bottom) {
        Margin m = mockMargin(top, left, right, bottom);
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

    @ParameterizedTest(name = "[{index}]  pps={2}, ppe={3}," +
                              "e1pps = {4}, e1ppe={5}, btl={6}, bll={7}, brl={8}, bbl{9} " +
                              "e1pps = {10}, e1ppe={11}, btl={12}, bll={13}, brl={14}, bbl{15} " +
                              "e1pps = {16}, e1ppe={17}, btl={18}, bll={19}, brl={20}, bbl{21} " +
                              "-> expected {22},{23}"
    )
    @CsvSource({
            // ,  width, marginBottom, expected
            "1, 2 ," +
            "1, 2, "
    })
    void updateSize(int pps, int ppe, int e1pps, int btl, int bll, int brl, int bbl, int expected) {
        Placement mockPlacement = mockPlacement(0.0, 0.0, 0.0, 0.0, pps, ppe);
        Placement mockPlacement1 = mockPlacement(0.0, 0.0, 0.0, 0.0, pps, ppe);
        Placement mockPlacement2 = mockPlacement(0.0, 0.0, 0.0, 0.0, pps, ppe);
        Placement mockPlacement3 = mockPlacement(0.0, 0.0, 0.0, 0.0, pps, ppe);
        Entity e1 = mockEntity(btl, bbl, bll, brl, mockPlacement1);
        Entity e2 = mockEntity(btl, bbl, bll, brl, mockPlacement2);
    }

    private Entity mockEntity(double top, double bottom, double left, double right, Placement placement) {
        Entity e = mock(Entity.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(top).when(e).boundingTopLine();
        doReturn(left).when(e).boundingLeftLine();
        doReturn(right).when(e).boundingRightLine();
        doReturn(bottom).when(e).boundingBottomLine();
        doReturn(placement).when(e).getComponent(Placement.class);
        return e;

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