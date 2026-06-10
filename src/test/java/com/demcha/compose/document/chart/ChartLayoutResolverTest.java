package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Deterministic golden tests for {@link ChartLayoutResolver}. A
 * font-independent fake {@link ChartTextMetrics} (5pt per char, 10pt line
 * height) makes the emitted geometry exact, so these assert real positions and
 * sizes with no rendering and no font dependency.
 */
class ChartLayoutResolverTest {

    /** width = 5pt per char, line height = 10pt. */
    private static final ChartTextMetrics METRICS = new ChartTextMetrics() {
        @Override public double width(DocumentTextStyle style, String text) {
            return (text == null ? 0 : text.length()) * 5.0;
        }

        @Override public double lineHeight(DocumentTextStyle style) {
            return 10.0;
        }

        @Override public double descent(DocumentTextStyle style) {
            return 2.0;
        }
    };

    private static ChartStyle baseStyle() {
        return ChartDefaults.DEFAULT_THEME.toChartStyle();
    }

    @Test
    void groupedBarGeometryIsExact() {
        ChartData data = ChartData.builder()
                .categories("A", "B")
                .series("S", 10.0, 20.0)
                .build();
        ChartSpec.Bar bar = ChartSpec.bar().data(data).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // 5 grid lines + 5 tick labels + 2 bars + 2 category labels.
        assertThat(out).hasSize(14);
        assertThat(count(out, ShapeNode.class)).isEqualTo(2);
        assertThat(count(out, LineNode.class)).isEqualTo(5);
        assertThat(count(out, ParagraphNode.class)).isEqualTo(7);

        // Frame: leftGutter = widest tick ("20" => 10) + 4 = 14; plotBottomY = 14;
        // plotTopY = 95; plotHeight = 81. Bars sit on the baseline.
        ChartPrimitive barA = byName(out, "bar_c0_s0");
        ChartPrimitive barB = byName(out, "bar_c1_s0");
        assertThat(barA.y()).isCloseTo(14.0, within(1e-6));
        assertThat(barB.y()).isCloseTo(14.0, within(1e-6));
        // value 10 -> fraction 0.5 -> height 40.5; value 20 -> full plot height 81.
        assertThat(barA.height()).isCloseTo(40.5, within(1e-6));
        assertThat(barB.height()).isCloseTo(81.0, within(1e-6));
        // Taller bar for the larger value.
        assertThat(barB.height()).isGreaterThan(barA.height());

        // Tick labels span 0..20 at evenly spaced y; each label's glyph-ink
        // centre lands on its gridline. inkCentre = descent + 0.70*size/2
        // = 2.0 + 0.70*8/2 = 4.8 for the 8pt axis style.
        double inkCenter = 2.0 + 0.70 * 8.0 / 2.0;
        ChartPrimitive tick0 = byName(out, "tick_0");
        ChartPrimitive tick4 = byName(out, "tick_4");
        assertThat(tick0.y() + inkCenter).isCloseTo(14.0, within(1e-6)); // bottom tick on baseline
        assertThat(tick4.y() + inkCenter).isCloseTo(95.0, within(1e-6)); // top tick at plotTopY
    }

    @Test
    void lineChartEmitsSegmentsAndNoBars() {
        ChartData data = ChartData.builder()
                .categories("A", "B", "C")
                .series("S", 1.0, 3.0, 2.0)
                .build();
        ChartSpec.Line line = ChartSpec.line().data(data).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // No bars; one polyline of two segments for three points.
        assertThat(count(out, ShapeNode.class)).isEqualTo(0);
        assertThat(out.stream().filter(p -> p.node().name().startsWith("line_s0_seg")).count())
                .isEqualTo(2);
        // 3 category labels present.
        assertThat(out.stream().filter(p -> p.node().name().startsWith("cat_")).count())
                .isEqualTo(3);
    }

    @Test
    void nullValueBreaksTheLine() {
        ChartData data = ChartData.builder()
                .categories("A", "B", "C")
                .series(new ChartData.Series("S", java.util.Arrays.asList(1.0, null, 2.0)))
                .build();
        ChartSpec.Line line = ChartSpec.line().data(data).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // A->null and null->C both broken: zero connecting segments.
        assertThat(out.stream().filter(p -> p.node().name().startsWith("line_s0_seg")).count())
                .isZero();
    }

    @Test
    void minimalChartHidesGridAxesButKeepsBarsAndValueLabels() {
        ChartData data = ChartData.builder()
                .categories("A", "B")
                .series("S", 10.0, 20.0)
                .build();
        ChartSpec.Bar bar = ChartSpec.bar()
                .data(data)
                .valueAxis(AxisSpec.builder().showGridLines(false).showTickLabels(false).build())
                .showCategoryLabels(false)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // No grid lines, no numeric tick labels, no category labels.
        assertThat(count(out, LineNode.class)).isZero();
        assertThat(out.stream().anyMatch(p -> p.node().name().startsWith("tick_"))).isFalse();
        assertThat(out.stream().anyMatch(p -> p.node().name().startsWith("cat_"))).isFalse();
        // Only the bars and their value labels remain.
        assertThat(count(out, ShapeNode.class)).isEqualTo(2);
        assertThat(out.stream().filter(p -> p.node().name().startsWith("value_")).count())
                .isEqualTo(2);
        // With no tick labels the value axis reserves no left gutter: bars start at x≈0.
        ChartPrimitive barA = byName(out, "bar_c0_s0");
        assertThat(barA.x()).isLessThan(20.0);
    }

    @Test
    void valueLabelsNoneEmitsNoValueText() {
        ChartData data = ChartData.builder().categories("A").series("S", 5.0).build();
        ChartSpec.Bar bar = ChartSpec.bar().data(data).valueLabels(ValueLabelMode.NONE).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        assertThat(out.stream().anyMatch(p -> p.node().name().startsWith("value_"))).isFalse();
    }

    @Test
    void unsupportedFeaturesAreRejectedLoudly() {
        ChartData data = ChartData.builder().categories("A").series("S", 1.0).build();

        ChartSpec.Bar inside = ChartSpec.bar().data(data).valueLabels(ValueLabelMode.INSIDE).build();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> ChartLayoutResolver.resolve(inside, baseStyle(),
                        ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS));
    }

    @Test
    void horizontalBarsGrowRightFromTheLeftEdge() {
        ChartData data = ChartData.builder().categories("A", "B").series("S", 10.0, 20.0).build();
        ChartSpec.Bar bar = ChartSpec.bar().data(data).horizontal(true).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        ChartPrimitive barA = byName(out, "bar_c0_s0");
        ChartPrimitive barB = byName(out, "bar_c1_s0");
        // Both start at the plot's left edge; the 20-bar is twice the 10-bar.
        assertThat(barA.x()).isEqualTo(barB.x());
        assertThat(barB.width()).isCloseTo(barA.width() * 2.0, within(1e-6));
        // First category sits ABOVE the second (reading order).
        assertThat(barA.y()).isGreaterThan(barB.y());
    }

    @Test
    void stackedBarsLabelTheCategoryTotal() {
        ChartData data = ChartData.builder().categories("A")
                .series("S1", 10.0).series("S2", 20.0).build();
        ChartSpec.Bar bar = ChartSpec.bar().data(data)
                .grouping(BarGrouping.STACKED).valueLabels(ValueLabelMode.OUTSIDE).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        ParagraphNode total = (ParagraphNode) byName(out, "total_c0").node();
        assertThat(total.text()).isEqualTo("30");
    }

    @Test
    void areaFillsRenderUnderEveryStroke() {
        ChartData data = ChartData.builder().categories("A", "B", "C")
                .series("S1", 1.0, 3.0, 2.0).series("S2", 2.0, 1.0, 3.0).build();
        ChartSpec.Line line = ChartSpec.line().data(data).area(true).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        long areas = out.stream()
                .filter(p -> p.node() instanceof com.demcha.compose.document.node.PolygonNode)
                .count();
        assertThat(areas).isEqualTo(2);
        // Every area polygon is emitted before the first stroke segment.
        int firstSegment = Integer.MAX_VALUE;
        int lastArea = -1;
        for (int i = 0; i < out.size(); i++) {
            String name = out.get(i).node().name();
            if (name.startsWith("line_s")) {
                firstSegment = Math.min(firstSegment, i);
            }
            if (name.startsWith("area_s")) {
                lastArea = Math.max(lastArea, i);
            }
        }
        assertThat(lastArea).isLessThan(firstSegment);
        // The fill is the translucent series colour.
        com.demcha.compose.document.node.PolygonNode area =
                (com.demcha.compose.document.node.PolygonNode) byName(out, "area_s0_r0").node();
        assertThat(area.fillColor().color().getAlpha()).isLessThan(255);
    }

    @Test
    void smoothLineSubdividesEachSpan() {
        ChartData data = ChartData.builder().categories("A", "B", "C")
                .series("S", 1.0, 3.0, 2.0).build();
        ChartSpec.Line line = ChartSpec.line().data(data).smooth(true).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        long segments = out.stream()
                .filter(p -> p.node().name().startsWith("line_s0_seg")).count();
        // Two spans, eight sub-segments each.
        assertThat(segments).isEqualTo(16);
    }

    @Test
    void rightLegendReservesAColumnOutsideThePlot() {
        ChartData data = ChartData.builder().categories("A", "B")
                .series("Alpha", 1.0, 2.0).build();
        ChartSpec.Bar bar = ChartSpec.bar().data(data).legend(LegendPosition.RIGHT).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        // The grid line spans the plot; the legend swatch starts right of it.
        ChartPrimitive grid = byName(out, "grid_0");
        ChartPrimitive swatch = byName(out, "legend_swatch_0");
        assertThat(swatch.x()).isGreaterThan(grid.x() + grid.width());
    }

    @Test
    void topLegendPlacesTheStripAboveThePlot() {
        ChartData data = ChartData.builder().categories("A", "B")
                .series("Alpha", 1.0, 2.0).build();
        ChartSpec.Line line = ChartSpec.line().data(data).legend(LegendPosition.TOP).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        ChartPrimitive legendLabel = byName(out, "legend_label_0");
        ChartPrimitive topGrid = byName(out, "grid_4"); // highest tick gridline
        assertThat(legendLabel.y()).isGreaterThan(topGrid.y());
    }

    @Test
    void explicitAxisBoundsAreHonored() {
        ChartData data = ChartData.builder().categories("A", "B").series("S", 42.0, 58.0).build();
        ChartSpec.Bar bar = ChartSpec.bar()
                .data(data)
                .valueAxis(AxisSpec.builder().baselineAtZero(false).min(40.0).max(60.0).build())
                .build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                bar, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // With domain forced to [40,60], value 42 sits near the plot bottom — its
        // bar is much shorter than 58's (with [0,60] they would be nearly equal).
        ChartPrimitive barA = byName(out, "bar_c0_s0");
        ChartPrimitive barB = byName(out, "bar_c1_s0");
        assertThat(barA.height()).isLessThan(barB.height() * 0.25);
    }

    @Test
    void pointMarkersRenderAsEllipsesAboveAllSegments() {
        ChartData data = ChartData.builder()
                .categories("A", "B", "C")
                .series("S1", 1.0, 3.0, 2.0)
                .series("S2", 2.0, 1.0, 3.0)
                .build();
        ChartStyle style = baseStyle().mergedUnder(ChartStyle.builder()
                .pointMarker(PointMarker.ellipse(6.0, 4.0))
                .valueLabelOffset(3.0)
                .build());
        ChartSpec.Line line = ChartSpec.line().data(data)
                .valueLabels(ValueLabelMode.OUTSIDE).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, style, ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        // One ellipse marker per data point, with the configured ellipse axes.
        List<ChartPrimitive> markers = out.stream()
                .filter(p -> p.node() instanceof com.demcha.compose.document.node.EllipseNode)
                .toList();
        assertThat(markers).hasSize(6);
        assertThat(markers.get(0).width()).isEqualTo(6.0);
        assertThat(markers.get(0).height()).isEqualTo(4.0);

        // Joint legibility: every marker is emitted after every line segment.
        int lastSegment = -1;
        int firstMarker = Integer.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            String name = out.get(i).node().name();
            if (name.startsWith("line_s")) {
                lastSegment = Math.max(lastSegment, i);
            }
            if (name.startsWith("point_s")) {
                firstMarker = Math.min(firstMarker, i);
            }
        }
        assertThat(firstMarker).isGreaterThan(lastSegment);

        // Value label sits at marker top + the configured offset.
        ChartPrimitive marker = markers.get(0);
        ChartPrimitive valueLabel = byName(out, "value_s0_c0");
        double markerCenterY = marker.y() + marker.height() / 2.0;
        assertThat(valueLabel.y()).isCloseTo(markerCenterY + 4.0 / 2.0 + 3.0, within(1e-6));
    }

    @Test
    void closeSeriesLabelsFlipBelowAndGetHaloChips() {
        // Two series nearly touching at the same category: their above-point
        // labels would overlap, so the lower one must flip below its point.
        ChartData data = ChartData.builder()
                .categories("A")
                .series("S1", 10.0)
                .series("S2", 10.8)
                .build();
        ChartStyle style = baseStyle().mergedUnder(ChartStyle.builder()
                .pointMarker(PointMarker.circle(4.0))
                .valueLabelOffset(3.0)
                .build());
        ChartSpec.Line line = ChartSpec.line().data(data)
                .valueLabels(ValueLabelMode.OUTSIDE).build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                line, style, ChartDefaults.DEFAULT_THEME, 200.0, 120.0, METRICS);

        ChartPrimitive marker1 = byName(out, "point_s0_c0");
        ChartPrimitive label1 = byName(out, "value_s0_c0");
        ChartPrimitive marker2 = byName(out, "point_s1_c0");
        ChartPrimitive label2 = byName(out, "value_s1_c0");
        // Higher point keeps its label above; the lower one flips below.
        assertThat(label2.y()).isGreaterThan(marker2.y());
        assertThat(label1.y() + label1.height()).isLessThan(marker1.y());
        // The flipped boxes no longer overlap.
        boolean overlap = label1.y() < label2.y() + label2.height()
                && label1.y() + label1.height() > label2.y();
        assertThat(overlap).isFalse();
        // Theme default paints a halo chip behind each label, just before it.
        assertThat(out.stream().filter(p -> p.node().name().endsWith("_halo")).count())
                .isEqualTo(2);
        // Labels (and their halos) are emitted after every marker.
        int lastMarker = -1;
        int firstHalo = Integer.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            String name = out.get(i).node().name();
            if (name.startsWith("point_s")) {
                lastMarker = Math.max(lastMarker, i);
            }
            if (name.endsWith("_halo")) {
                firstHalo = Math.min(firstHalo, i);
            }
        }
        assertThat(firstHalo).isGreaterThan(lastMarker);
    }

    @Test
    void markerFillAndStrokeOverridesApply() {
        ChartData data = ChartData.builder().categories("A").series("S", 1.0).build();
        DocumentPaint white = DocumentPaint.solid(
                com.demcha.compose.document.style.DocumentColor.WHITE);
        com.demcha.compose.document.style.DocumentStroke ring =
                com.demcha.compose.document.style.DocumentStroke.of(
                        com.demcha.compose.document.style.DocumentColor.BLACK, 1.2);
        ChartStyle style = baseStyle().mergedUnder(ChartStyle.builder()
                .pointMarker(PointMarker.circle(5.0).withFill(white).withStroke(ring))
                .build());

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                ChartSpec.line().data(data).build(), style,
                ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        com.demcha.compose.document.node.EllipseNode markerNode = out.stream()
                .filter(p -> p.node() instanceof com.demcha.compose.document.node.EllipseNode)
                .map(p -> (com.demcha.compose.document.node.EllipseNode) p.node())
                .findFirst().orElseThrow();
        assertThat(markerNode.fillColor().color())
                .isEqualTo(com.demcha.compose.document.style.DocumentColor.WHITE.color());
        assertThat(markerNode.stroke()).isEqualTo(ring);
    }

    @Test
    void verticalGridLinesEmitWhenStyled() {
        ChartData data = ChartData.builder().categories("A", "B", "C").series("S", 1.0, 2.0, 3.0).build();
        ChartStyle style = ChartDefaults.DEFAULT_THEME.toChartStyle().mergedUnder(
                ChartStyle.builder()
                        .grid(new ChartStyle.GridStyle(
                                ChartDefaults.DEFAULT_GRID_STROKE, ChartDefaults.DEFAULT_GRID_STROKE))
                        .build());

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                ChartSpec.bar().data(data).build(), style,
                ChartDefaults.DEFAULT_THEME, 200.0, 100.0, METRICS);

        // Two inner boundaries for three categories.
        assertThat(out.stream().filter(p -> p.node().name().startsWith("vgrid_")).count())
                .isEqualTo(2);
    }

    @Test
    void pieEmitsOneSectorPolygonPerSliceWithPercentLabels() {
        ChartData data = ChartData.builder()
                .categories("A", "B", "C")
                .series("S", 25.0, 25.0, 50.0)
                .build();
        ChartSpec.Pie pie = ChartSpec.pie().data(data)
                .sliceLabels(SliceLabelMode.PERCENT)
                .legend(LegendPosition.BOTTOM)
                .build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                pie, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 200.0, METRICS);

        List<ChartPrimitive> sectors = out.stream()
                .filter(p -> p.node() instanceof com.demcha.compose.document.node.PolygonNode)
                .toList();
        assertThat(sectors).hasSize(3);
        // Solid pie sectors close through the centre point.
        com.demcha.compose.document.node.PolygonNode first =
                (com.demcha.compose.document.node.PolygonNode) sectors.get(0).node();
        assertThat(first.points().get(first.points().size() - 1).x()).isEqualTo(0.5);
        assertThat(first.points().get(first.points().size() - 1).y()).isEqualTo(0.5);
        // The 50% slice tessellates roughly twice as many arc vertices as a 25% one.
        com.demcha.compose.document.node.PolygonNode big =
                (com.demcha.compose.document.node.PolygonNode) sectors.get(2).node();
        assertThat(big.points().size()).isGreaterThan(first.points().size() + 20);
        // Percent labels with the default percent format.
        assertThat(out.stream().map(p -> p.node())
                .filter(n -> n instanceof ParagraphNode pn && pn.name().startsWith("slice_label_"))
                .map(n -> ((ParagraphNode) n).text()))
                .containsExactlyInAnyOrder("25%", "25%", "50%");
        // Legend lists CATEGORIES for a pie.
        assertThat(out.stream().map(p -> p.node())
                .filter(n -> n instanceof ParagraphNode pn && pn.name().startsWith("legend_label_"))
                .map(n -> ((ParagraphNode) n).text()))
                .containsExactly("A", "B", "C");
    }

    @Test
    void donutSectorsAreRingsAndCenterTextRenders() {
        ChartData data = ChartData.builder()
                .categories("A", "B")
                .series("S", 60.0, 40.0)
                .build();
        ChartSpec.Pie donut = ChartSpec.pie().data(data)
                .donutRatio(0.5)
                .centerText("100k")
                .build();

        List<ChartPrimitive> out = ChartLayoutResolver.resolve(
                donut, baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 200.0, METRICS);

        com.demcha.compose.document.node.PolygonNode ring = out.stream()
                .filter(p -> p.node() instanceof com.demcha.compose.document.node.PolygonNode)
                .map(p -> (com.demcha.compose.document.node.PolygonNode) p.node())
                .findFirst().orElseThrow();
        // A ring sector has no centre vertex: every point sits at >= inner radius.
        double minDistance = ring.points().stream()
                .mapToDouble(pt -> Math.hypot(pt.x() - 0.5, pt.y() - 0.5))
                .min().orElseThrow();
        assertThat(minDistance).isGreaterThan(0.24);
        // Centre KPI text present.
        assertThat(out.stream().map(p -> p.node())
                .anyMatch(n -> n instanceof ParagraphNode pn && pn.text().equals("100k")))
                .isTrue();
    }

    @Test
    void pieRejectsInvalidInputLoudly() {
        ChartData twoSeries = ChartData.builder()
                .categories("A").series("S1", 1.0).series("S2", 2.0).build();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ChartSpec.pie().data(twoSeries).build());

        ChartData negative = ChartData.builder().categories("A", "B").series("S", 5.0, -1.0).build();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ChartLayoutResolver.resolve(ChartSpec.pie().data(negative).build(),
                        baseStyle(), ChartDefaults.DEFAULT_THEME, 200.0, 200.0, METRICS));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ChartSpec.pie().data(ChartData.builder().categories("A").series("S", 1.0).build())
                        .centerText("x").build());  // centerText without a donut hole
    }

    private static long count(List<ChartPrimitive> out, Class<?> type) {
        return out.stream().filter(p -> type.isInstance(p.node())).count();
    }

    private static ChartPrimitive byName(List<ChartPrimitive> out, String name) {
        return out.stream().filter(p -> p.node().name().equals(name)).findFirst().orElseThrow();
    }
}
