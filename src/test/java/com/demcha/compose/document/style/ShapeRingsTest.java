package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
