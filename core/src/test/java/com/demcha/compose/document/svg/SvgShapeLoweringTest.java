package com.demcha.compose.document.svg;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the basic-shape → path-data lowering: each synthesized
 * {@code d} string must parse back through {@link SvgPath} and produce the
 * expected geometry.
 */
class SvgShapeLoweringTest {

    @Test
    void plainRectLowersToAClosedRectangleSubpath() {
        String d = SvgShapeLowering.rect(1, 2, 8, 6, 0, 0);
        assertThat(d).isEqualTo("M1.0 2.0 h8.0 v6.0 h-8.0 Z");
        // M + 3 drawing ops + close → 5 segments through the real parser.
        assertThat(SvgPath.parse(d).segments()).hasSize(5);
    }

    @Test
    void roundedRectMirrorsASingleRadiusAndArcsTheCorners() {
        // Only rx given → ry mirrors it; the result carries four arc corners,
        // each an arc that lowers to cubic Béziers.
        String d = SvgShapeLowering.rect(0, 0, 10, 10, 3, 0);
        long cubics = SvgPath.parse(d).segments().stream()
                .filter(com.demcha.compose.document.style.DocumentPathSegment.CubicTo.class::isInstance)
                .count();
        assertThat(cubics).isGreaterThanOrEqualTo(4);
    }

    @Test
    void ellipseLowersToTwoArcsAndNullsAZeroRadius() {
        assertThat(SvgShapeLowering.ellipse(5, 5, 4, 3)).startsWith("M");
        assertThat(SvgPath.parse(SvgShapeLowering.ellipse(5, 5, 4, 3)).segments())
                .anyMatch(com.demcha.compose.document.style.DocumentPathSegment.CubicTo.class::isInstance);
        assertThat(SvgShapeLowering.ellipse(5, 5, 0, 3)).isNull();
        assertThat(SvgShapeLowering.ellipse(5, 5, 4, 0)).isNull();
    }

    @Test
    void pointsClosesForPolygonAndStaysOpenForPolyline() {
        assertThat(SvgShapeLowering.points("0,0 4,0 2,3", true)).isEqualTo("M0,0 4,0 2,3 Z");
        assertThat(SvgShapeLowering.points("0,0 4,0 2,3", false)).isEqualTo("M0,0 4,0 2,3");
        assertThat(SvgShapeLowering.points("   ", true)).isNull();
        assertThat(SvgShapeLowering.points(null, false)).isNull();
    }
}
