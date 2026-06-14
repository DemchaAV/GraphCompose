package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.style.DocumentPathSegment.close;
import static com.demcha.compose.document.style.DocumentPathSegment.cubicTo;
import static com.demcha.compose.document.style.DocumentPathSegment.lineTo;
import static com.demcha.compose.document.style.DocumentPathSegment.moveTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value semantics of {@link PathNode} and {@link DocumentPathSegment}:
 * segment-order validation, finite-coordinate checks, defensive copying,
 * and the deliberate freedom of Bézier control points to overshoot the
 * unit box.
 */
class PathNodeTest {

    private static final DocumentStroke STROKE = DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 1.5);

    @Test
    void pathMustStartWithAMoveTo() {
        assertThatThrownBy(() -> node(List.of(lineTo(0.2, 0.2), lineTo(0.8, 0.8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with a MoveTo");
    }

    @Test
    void pathNeedsAtLeastOneDrawingSegment() {
        assertThatThrownBy(() -> node(List.of(moveTo(0.1, 0.1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least a MoveTo and one drawing segment");
    }

    @Test
    void coordinatesMustBeFinite() {
        assertThatThrownBy(() -> cubicTo(Double.NaN, 0, 0, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be finite");
        assertThatThrownBy(() -> moveTo(Double.POSITIVE_INFINITY, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be finite");
    }

    @Test
    void controlPointsMayOvershootTheUnitBox() {
        // Catmull-Rom→Bézier conversions and expressive design curves push
        // control points outside [0, 1]; the segment type must allow it.
        PathNode node = node(List.of(
                moveTo(0.5, 1.0),
                cubicTo(1.15, 0.9, 0.95, -0.1, 0.5, 0.0),
                close()));

        assertThat(node.segments()).hasSize(3);
    }

    @Test
    void segmentListIsCopyProtectedAndImmutable() {
        List<DocumentPathSegment> source = new ArrayList<>(List.of(moveTo(0, 0), lineTo(1, 1)));
        PathNode node = node(source);
        source.add(close());

        assertThat(node.segments()).hasSize(2);
        assertThatThrownBy(() -> node.segments().add(close()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void boxDimensionsMustBeFiniteAndPositive() {
        assertThatThrownBy(() -> new PathNode("Bad", 0, 40,
                List.of(moveTo(0, 0), lineTo(1, 1)), null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width must be finite and positive");
    }

    @Test
    void dashPatternDefaultsToSolid() {
        assertThat(node(List.of(moveTo(0, 0), lineTo(1, 1))).dashPattern())
                .isEqualTo(DocumentDashPattern.NONE);
    }

    @Test
    void nodeKindIsPath() {
        assertThat(node(List.of(moveTo(0, 0), lineTo(1, 1))).nodeKind()).isEqualTo("Path");
    }

    private static PathNode node(List<DocumentPathSegment> segments) {
        return new PathNode("P", 120, 60, segments, null, STROKE,
                DocumentInsets.zero(), DocumentInsets.zero(), null);
    }
}
