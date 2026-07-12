package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ShapeRingsTest {

    @Test
    void checkmarkBandReturnsSixTwoCoordinateVertices() {
        double[][] ring = ShapeRings.checkmarkBand(0.13);

        assertThat(ring.length).isEqualTo(6);
        for (double[] vertex : ring) {
            assertThat(vertex.length).isEqualTo(2);
        }
    }

    @Test
    void thickerBandPushesTheOuterElbowLower() {
        // Vertex 0 is the outer elbow (bottom of the tick); a thicker band must
        // push it lower — a stable proxy that {@code half} actually widens it.
        double[][] thin = ShapeRings.checkmarkBand(0.08);
        double[][] thick = ShapeRings.checkmarkBand(0.16);

        assertThat(thick[0][1]).isLessThan(thin[0][1]);
    }

    @Test
    void starHasTwoVerticesPerPointWithFirstFacingUp() {
        double[][] ring = ShapeRings.star(5);

        assertThat(ring.length).isEqualTo(10);
        // First outer vertex faces up: centred x, top y.
        assertThat(ring[0][0]).isCloseTo(0.5, within(1e-9));
        assertThat(ring[0][1]).isCloseTo(1.0, within(1e-9));
        // Outer vertices (even indices) sit farther from centre than inner ones.
        double outer = Math.hypot(ring[0][0] - 0.5, ring[0][1] - 0.5);
        double inner = Math.hypot(ring[1][0] - 0.5, ring[1][1] - 0.5);
        assertThat(outer).isGreaterThan(inner);
    }

    @Test
    void regularPolygonInscribesNVerticesFirstFacingUp() {
        double[][] hex = ShapeRings.regularPolygon(6);

        assertThat(hex.length).isEqualTo(6);
        assertThat(hex[0][0]).isCloseTo(0.5, within(1e-9));
        assertThat(hex[0][1]).isCloseTo(1.0, within(1e-9));
        for (double[] v : hex) {
            assertThat(Math.hypot(v[0] - 0.5, v[1] - 0.5)).isCloseTo(0.5, within(1e-9));
        }
    }

    @Test
    void directionalMirrorsForLeftAndTransposesForUp() {
        double[][] base = {{0.0, 0.35}, {1.0, 0.5}, {0.0, 0.65}};

        double[][] left = ShapeRings.directional(base, ShapeOutline.Direction.LEFT);
        assertThat(left[1][0]).isCloseTo(0.0, within(1e-9));   // tip mirrored to the left edge

        double[][] up = ShapeRings.directional(base, ShapeOutline.Direction.UP);
        assertThat(up[1][1]).isCloseTo(1.0, within(1e-9));     // tip transposed to the top edge

        // null defaults to RIGHT (identity).
        assertThat(ShapeRings.directional(base, null)[1][0]).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void toPointsClampsOutOfBoxCoordinates() {
        var points = ShapeRings.toPoints(new double[][]{{-0.2, 0.5}, {1.3, 0.5}, {0.5, 0.5}});

        assertThat(points.get(0).x()).isCloseTo(0.0, within(1e-9));
        assertThat(points.get(1).x()).isCloseTo(1.0, within(1e-9));
        assertThat(points.get(2).x()).isCloseTo(0.5, within(1e-9));
    }
}
