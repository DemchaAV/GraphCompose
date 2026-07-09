package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.LayerAlign;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Engine-level contract for the {@code LayerStackNode} placement geometry
 * extracted from {@link LayoutCompiler}. Whole-document stacking is guarded by
 * the qa snapshot / visual-regression suites; these pin the z-order permutation
 * and the align offsets in isolation.
 */
class LayerStackGeometryTest {

    private static final double EPS = 1e-9;

    @Test
    void emptyAndSingletonYieldIdentityOrder() {
        assertThat(LayerStackGeometry.zOrder(List.of())).isEmpty();
        assertThat(LayerStackGeometry.zOrder(List.of(7))).containsExactly(0);
    }

    @Test
    void allEqualZIndicesPreserveSourceOrder() {
        assertThat(LayerStackGeometry.zOrder(List.of(0, 0, 0, 0))).containsExactly(0, 1, 2, 3);
        assertThat(LayerStackGeometry.zOrder(List.of(5, 5, 5))).containsExactly(0, 1, 2);
    }

    @Test
    void distinctZIndicesSortAscendingStableOnTies() {
        // z per index: [2, 0, 1, 0] → ascending, ties keep source order → [1, 3, 2, 0].
        assertThat(LayerStackGeometry.zOrder(List.of(2, 0, 1, 0))).containsExactly(1, 3, 2, 0);
    }

    @Test
    void negativeZIndicesOrderBelowZero() {
        assertThat(LayerStackGeometry.zOrder(List.of(0, -1, 3))).containsExactly(1, 0, 2);
    }

    @Test
    void horizontalOffsetIsZeroForLeftAligns() {
        for (LayerAlign a : List.of(LayerAlign.TOP_LEFT, LayerAlign.CENTER_LEFT, LayerAlign.BOTTOM_LEFT)) {
            assertThat(LayerStackGeometry.horizontalOffset(a, 100, 40)).as("%s", a).isZero();
        }
    }

    @Test
    void horizontalOffsetHalvesSlackForCenterAndFillsForRight() {
        assertThat(LayerStackGeometry.horizontalOffset(LayerAlign.CENTER, 100, 40)).isCloseTo(30, within(EPS));
        assertThat(LayerStackGeometry.horizontalOffset(LayerAlign.TOP_RIGHT, 100, 40)).isCloseTo(60, within(EPS));
    }

    @Test
    void horizontalOffsetClampsNonNegativeWhenChildWiderThanInner() {
        assertThat(LayerStackGeometry.horizontalOffset(LayerAlign.CENTER, 40, 100)).isZero();
        assertThat(LayerStackGeometry.horizontalOffset(LayerAlign.BOTTOM_RIGHT, 40, 100)).isZero();
    }

    @Test
    void verticalOffsetIsZeroForTopAligns() {
        for (LayerAlign a : List.of(LayerAlign.TOP_LEFT, LayerAlign.TOP_CENTER, LayerAlign.TOP_RIGHT)) {
            assertThat(LayerStackGeometry.verticalOffset(a, 100, 40)).as("%s", a).isZero();
        }
    }

    @Test
    void verticalOffsetHalvesSlackForCenterAndFillsForBottom() {
        assertThat(LayerStackGeometry.verticalOffset(LayerAlign.CENTER, 100, 40)).isCloseTo(30, within(EPS));
        assertThat(LayerStackGeometry.verticalOffset(LayerAlign.BOTTOM_LEFT, 100, 40)).isCloseTo(60, within(EPS));
    }

    @Test
    void verticalOffsetClampsNonNegativeWhenChildTallerThanInner() {
        assertThat(LayerStackGeometry.verticalOffset(LayerAlign.CENTER, 40, 100)).isZero();
        assertThat(LayerStackGeometry.verticalOffset(LayerAlign.BOTTOM_LEFT, 40, 100)).isZero();
    }
}
