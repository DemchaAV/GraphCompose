package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapePoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value semantics of {@link PolygonNode}: the fail-loud guards the author
 * wrote (fewer than three points, non-positive / non-finite box) and the
 * defensive copy of the vertex ring. The only production callers pass valid
 * ring data, so without these tests a future refactor could silently drop a
 * guard with nothing going red — the {@link PathNode} sibling already pins
 * the equivalent contract.
 */
class PolygonNodeTest {

    private static final DocumentStroke STROKE = DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 1.5);
    private static final List<ShapePoint> TRIANGLE =
            List.of(new ShapePoint(0, 0), new ShapePoint(1, 0), new ShapePoint(0.5, 1));

    @Test
    void validTriangleBuildsAndReportsItsKind() {
        PolygonNode node = node(TRIANGLE);

        assertThat(node.points()).hasSize(3);
        assertThat(node.nodeKind()).isEqualTo("Polygon");
    }

    @Test
    void fewerThanThreePointsIsRejected() {
        assertThatThrownBy(() -> node(List.of(new ShapePoint(0, 0), new ShapePoint(1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least three points");
    }

    @Test
    void nonPositiveOrNonFiniteWidthIsRejected() {
        assertThatThrownBy(() -> new PolygonNode("P", 0, 40, TRIANGLE, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width must be finite and positive");
        assertThatThrownBy(() -> new PolygonNode("P", Double.NaN, 40, TRIANGLE, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width must be finite and positive");
        assertThatThrownBy(() -> new PolygonNode("P", Double.POSITIVE_INFINITY, 40, TRIANGLE, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width must be finite and positive");
    }

    @Test
    void nonPositiveOrNonFiniteHeightIsRejected() {
        assertThatThrownBy(() -> new PolygonNode("P", 40, 0, TRIANGLE, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height must be finite and positive");
        assertThatThrownBy(() -> new PolygonNode("P", 40, Double.NaN, TRIANGLE, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height must be finite and positive");
    }

    @Test
    void nullPointsIsRejected() {
        assertThatThrownBy(() -> new PolygonNode("P", 40, 40, null, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("points");
    }

    @Test
    void vertexRingIsCopyProtectedAndImmutable() {
        List<ShapePoint> source = new ArrayList<>(TRIANGLE);
        PolygonNode node = node(source);
        source.add(new ShapePoint(0.25, 0.25));

        assertThat(node.points()).hasSize(3);
        assertThatThrownBy(() -> node.points().add(new ShapePoint(0.5, 0.5)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static PolygonNode node(List<ShapePoint> points) {
        return new PolygonNode("P", 120, 60, points, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero());
    }
}
