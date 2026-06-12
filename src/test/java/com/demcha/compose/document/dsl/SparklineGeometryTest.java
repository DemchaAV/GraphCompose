package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.style.ShapePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Vertex math for inline sparklines, pinned without rendering, plus the
 * RichText entry points.
 */
class SparklineGeometryTest {

    @Test
    void areaRingClosesToTheBaselineWithNormalizedExtremes() {
        List<ShapePoint> pts = SparklineGeometry.areaPoints(new double[] {2.0, 8.0, 5.0});

        // 2 spans x 12 smooth sub-segments + start point + 2 baseline corners.
        assertThat(pts).hasSize(2 * 12 + 1 + 2);
        assertThat(pts.get(0).y()).isCloseTo(0.0, within(1e-12)); // min -> bottom
        // The original data points survive at the span boundaries.
        assertThat(pts.get(12).y()).isCloseTo(1.0, within(1e-12)); // max -> top
        assertThat(pts.get(12).x()).isCloseTo(0.5, within(1e-12)); // evenly spaced
        assertThat(pts.get(pts.size() - 2)).isEqualTo(new ShapePoint(1.0, 0.0));
        assertThat(pts.get(pts.size() - 1)).isEqualTo(new ShapePoint(0.0, 0.0));
    }

    @Test
    void flatRunCentresAndRibbonKeepsConstantThickness() {
        List<ShapePoint> flat = SparklineGeometry.areaPoints(new double[] {4.0, 4.0});
        assertThat(flat.get(0).y()).isCloseTo(0.5, within(1e-12));

        List<ShapePoint> ribbon = SparklineGeometry.ribbonPoints(
                new double[] {1.0, 3.0, 2.0}, 0.2);
        int curve = 2 * 12 + 1; // smoothed samples per edge
        assertThat(ribbon).hasSize(curve * 2);
        // Top edge runs forward, bottom edge runs back: pair i with (2n-1-i).
        for (int i = 0; i < curve; i++) {
            ShapePoint top = ribbon.get(i);
            ShapePoint bottom = ribbon.get(ribbon.size() - 1 - i);
            assertThat(top.x()).isCloseTo(bottom.x(), within(1e-12));
            assertThat(top.y() - bottom.y()).isCloseTo(0.2, within(1e-9));
        }
    }

    @Test
    void invalidInputIsRejectedLoudly() {
        assertThatThrownBy(() -> SparklineGeometry.areaPoints(new double[] {1.0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two values");
        assertThatThrownBy(() -> SparklineGeometry.ribbonPoints(new double[] {1, 2}, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thicknessFraction");
        assertThatThrownBy(() -> SparklineGeometry.areaPoints(new double[] {1, Double.NaN}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void richTextSparklineBecomesAPolygonInlineRun() {
        ParagraphNode paragraph = new ParagraphBuilder()
                .rich(r -> r.plain("Trend ")
                        .sparkline(36, 9, DocumentColor.ROYAL_BLUE, 65.2, 69.8, 74.1, 81.3, 88.2))
                .build();

        InlineShapeRun run = (InlineShapeRun) paragraph.inlineRuns().get(1);
        ShapeOutline.Polygon polygon = (ShapeOutline.Polygon) run.layers().get(0).outline();
        assertThat(polygon.width()).isEqualTo(36.0);
        assertThat(polygon.height()).isEqualTo(9.0);
        // 4 spans x 12 sub-segments + start + 2 baseline corners.
        assertThat(polygon.points()).hasSize(4 * 12 + 1 + 2);

        assertThatThrownBy(() -> RichText.text("x")
                .sparklineLine(36, 9, 12, DocumentColor.ROYAL_BLUE, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thickness");
    }
}
