package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ShapeOutlineTest {

    private static final double EPS = 1e-6;

    @Test
    void circleIsAnEqualSidedEllipse() {
        ShapeOutline.Ellipse circle = ShapeOutline.circle(10);
        assertThat(circle.width()).isEqualTo(10.0, within(EPS));
        assertThat(circle.height()).isEqualTo(10.0, within(EPS));
    }

    @Test
    void diamondHasFourVerticesAndKeepsSize() {
        ShapeOutline.Polygon diamond = ShapeOutline.diamond(12, 8);
        assertThat(diamond.width()).isEqualTo(12.0, within(EPS));
        assertThat(diamond.height()).isEqualTo(8.0, within(EPS));
        assertThat(diamond.points()).hasSize(4);
    }

    @Test
    void triangleHasThreeVertices() {
        assertThat(ShapeOutline.triangle(10, 10).points()).hasSize(3);
    }

    @Test
    void starHasTwiceThePointCountVertices() {
        assertThat(ShapeOutline.star(10, 10).points()).hasSize(10);
        assertThat(ShapeOutline.star(10, 10, 6).points()).hasSize(12);
    }

    @Test
    void starVerticesStayWithinUnitBox() {
        for (ShapePoint point : ShapeOutline.star(10, 10, 7).points()) {
            assertThat(point.x()).isBetween(0.0, 1.0);
            assertThat(point.y()).isBetween(0.0, 1.0);
        }
    }

    @Test
    void polygonRejectsFewerThanThreePoints() {
        assertThatThrownBy(() -> ShapeOutline.polygon(10, 10,
                List.of(new ShapePoint(0, 0), new ShapePoint(1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3");
    }

    @Test
    void polygonCopiesItsVertexRingDefensively() {
        List<ShapePoint> mutable = new ArrayList<>(List.of(
                new ShapePoint(0, 0), new ShapePoint(1, 0), new ShapePoint(0.5, 1)));
        ShapeOutline.Polygon polygon = ShapeOutline.polygon(10, 10, mutable);
        mutable.clear();
        assertThat(polygon.points()).hasSize(3);
    }

    @Test
    void starRejectsFewerThanThreePoints() {
        assertThatThrownBy(() -> ShapeOutline.star(10, 10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3");
    }

    @Test
    void arrowAndChevronRejectNullDirection() {
        assertThatThrownBy(() -> ShapeOutline.arrow(10, 10, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ShapeOutline.chevron(10, 10, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rectangleRejectsNonPositiveDimensions() {
        assertThatThrownBy(() -> new ShapeOutline.Rectangle(0, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shapePointRejectsOutOfRangeOrNonFiniteCoordinates() {
        assertThatThrownBy(() -> new ShapePoint(1.5, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShapePoint(-0.1, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShapePoint(0.5, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void arrowHasSevenVerticesAndDirectionalTip() {
        assertThat(ShapeOutline.arrowRight(10, 10).points()).hasSize(7);
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.RIGHT).points())
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(1.0, within(EPS));
                    assertThat(p.y()).isEqualTo(0.5, within(EPS));
                });
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.LEFT).points())
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(0.0, within(EPS));
                    assertThat(p.y()).isEqualTo(0.5, within(EPS));
                });
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.UP).points())
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(0.5, within(EPS));
                    assertThat(p.y()).isEqualTo(1.0, within(EPS));
                });
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.DOWN).points())
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(0.5, within(EPS));
                    assertThat(p.y()).isEqualTo(0.0, within(EPS));
                });
    }

    @Test
    void chevronCheckmarkAndPlusHaveExpectedVertexCounts() {
        assertThat(ShapeOutline.chevron(10, 10, ShapeOutline.Direction.RIGHT).points()).hasSize(6);
        assertThat(ShapeOutline.checkmark(10, 10).points()).hasSize(6);
        assertThat(ShapeOutline.plus(10, 10).points()).hasSize(12);
    }

    @Test
    void regularPolygonHasRequestedSidesAndRejectsTooFew() {
        assertThat(ShapeOutline.regularPolygon(10, 10, 6).points()).hasSize(6);
        assertThatThrownBy(() -> ShapeOutline.regularPolygon(10, 10, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyPolygonFactoryStaysWithinUnitBox() {
        List<ShapeOutline.Polygon> shapes = List.of(
                ShapeOutline.arrow(10, 10, ShapeOutline.Direction.DOWN),
                ShapeOutline.chevron(10, 10, ShapeOutline.Direction.UP),
                ShapeOutline.checkmark(10, 10),
                ShapeOutline.plus(10, 10),
                ShapeOutline.regularPolygon(10, 10, 7));
        for (ShapeOutline.Polygon shape : shapes) {
            for (ShapePoint point : shape.points()) {
                assertThat(point.x()).isBetween(0.0, 1.0);
                assertThat(point.y()).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void checkmarkDefaultEqualsClassicStyle() {
        assertThat(ShapeOutline.checkmark(10, 10).points())
                .isEqualTo(ShapeOutline.checkmark(10, 10, ShapeOutline.CheckmarkStyle.CLASSIC).points());
    }

    @Test
    void heavyCheckmarkDiffersFromClassicButKeepsSixVerticesInBox() {
        ShapeOutline.Polygon classic = ShapeOutline.checkmark(10, 10, ShapeOutline.CheckmarkStyle.CLASSIC);
        ShapeOutline.Polygon heavy = ShapeOutline.checkmark(10, 10, ShapeOutline.CheckmarkStyle.HEAVY);

        assertThat(heavy.points()).hasSize(6);
        assertThat(heavy.points()).isNotEqualTo(classic.points());
        for (ShapePoint point : heavy.points()) {
            assertThat(point.x()).isBetween(0.0, 1.0);
            assertThat(point.y()).isBetween(0.0, 1.0);
        }
    }

    @Test
    void arrowDefaultEqualsBlockStyle() {
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.RIGHT).points())
                .isEqualTo(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.RIGHT,
                        ShapeOutline.ArrowStyle.BLOCK).points());
    }

    @Test
    void triangleArrowHasThreeVerticesAndDirectionalTip() {
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.RIGHT, ShapeOutline.ArrowStyle.TRIANGLE).points())
                .hasSize(3)
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(1.0, within(EPS));
                    assertThat(p.y()).isEqualTo(0.5, within(EPS));
                });
        assertThat(ShapeOutline.arrow(10, 10, ShapeOutline.Direction.UP, ShapeOutline.ArrowStyle.TRIANGLE).points())
                .anySatisfy(p -> {
                    assertThat(p.x()).isEqualTo(0.5, within(EPS));
                    assertThat(p.y()).isEqualTo(1.0, within(EPS));
                });
    }

    @Test
    void checkmarkAndArrowStyleOverloadsRejectNullStyle() {
        assertThatThrownBy(() -> ShapeOutline.checkmark(10, 10, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ShapeOutline.arrow(10, 10, ShapeOutline.Direction.RIGHT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
