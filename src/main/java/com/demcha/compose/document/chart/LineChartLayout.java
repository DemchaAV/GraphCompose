package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.style.*;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.chart.ChartLayoutSupport.*;

/**
 * Geometry for line charts: straight polylines or native cubic-Bézier
 * smoothed curves, optional translucent area fills (curved to match in
 * smooth mode), point markers, and collision-aware value labels.
 *
 * <p>Smooth runs compile into a single {@code PathNode} per run whose
 * Catmull-Rom-derived control points are pure arithmetic on the data points
 * — the exact continuous curve the pre-1.8 fixed-step sampler approximated,
 * now rendered with native PDF curve operators and zero tessellation.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class LineChartLayout {

    private static final double DEFAULT_AREA_OPACITY = 0.35;

    private LineChartLayout() {
    }

    static List<ChartPrimitive> resolve(ChartSpec.Line line, ChartStyle style,
                                        ChartTheme theme, double width, double height,
                                        ChartTextMetrics metrics) {
        ChartLayoutSupport.requireSupportedValueLabels(line.valueLabels());
        ChartData data = line.data();
        List<ChartLayoutSupport.LegendEntry> legendEntries =
                ChartLayoutSupport.seriesLegendEntries(data, style, theme);
        ChartLayoutSupport.LegendReserve reserve =
                ChartLayoutSupport.legendReserve(line.legend(), legendEntries, style, metrics);
        ChartLayoutSupport.Frame f = ChartLayoutSupport.computeFrame(
                data, line.valueAxis(), reserve, line.valueLabels(), false,
                line.showCategoryLabels(), style, width, height, metrics);

        List<ChartPrimitive> out = new ArrayList<>();
        ChartLayoutSupport.emitGridAndTicks(out, f, line.valueAxis(), style, metrics);
        ChartLayoutSupport.emitVerticalGrid(out, f, data.categoryCount(), style);

        int cats = data.categoryCount();
        double slotW = f.plotWidth() / cats;
        PointMarker marker = style.pointMarker();
        double labelGap = valueLabelGap(style);
        double strokeWidth = style.lineWidth() == null ? DEFAULT_LINE_WIDTH : style.lineWidth();
        double areaOpacity = style.areaOpacity() == null ? DEFAULT_AREA_OPACITY : style.areaOpacity();

        // Per-series contiguous non-null runs of original data points. Smooth
        // mode compiles each run into native Bézier primitives; markers and
        // labels stay on the original data points either way.
        List<List<List<double[]>>> seriesRuns = new ArrayList<>();
        for (int s = 0; s < data.seriesCount(); s++) {
            seriesRuns.add(sampleSeries(data.series().get(s), f, slotW));
        }
        boolean smooth = line.smooth();

        // Pass 0 — area fills, under every stroke. Smooth runs close the
        // exact stroke curve down to the baseline so fill and stroke edges
        // coincide.
        if (line.area()) {
            for (int s = 0; s < data.seriesCount(); s++) {
                DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
                DocumentColor fill = color.withOpacity(areaOpacity);
                int runIndex = 0;
                for (List<double[]> run : seriesRuns.get(s)) {
                    if (run.size() < 2) {
                        runIndex++;
                        continue;
                    }
                    String name = "area_s" + s + "_r" + runIndex;
                    if (smooth && run.size() >= 3) {
                        emitCurvedArea(out, name, run, f.plotBottomY(), fill);
                    } else {
                        emitAreaPolygon(out, name, run, f.plotBottomY(), fill);
                    }
                    runIndex++;
                }
            }
        }

        // Pass 1 — series strokes: one native Bézier path per smooth run
        // (three or more points), straight segments otherwise.
        for (int s = 0; s < data.seriesCount(); s++) {
            DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
            DocumentStroke stroke = DocumentStroke.of(color, strokeWidth);
            int n = 0;
            int runIndex = 0;
            for (List<double[]> run : seriesRuns.get(s)) {
                if (smooth && run.size() >= 3) {
                    out.add(bezierRun("line_s" + s + "_curve" + runIndex, run, stroke));
                } else {
                    for (int i = 1; i < run.size(); i++) {
                        out.add(segment("line_s" + s + "_seg" + n++,
                                run.get(i - 1)[0], run.get(i - 1)[1],
                                run.get(i)[0], run.get(i)[1], stroke));
                    }
                }
                runIndex++;
            }
        }

        // Pass 2 — markers above all strokes, so joints stay legible.
        double markerHalfH = marker == null ? 0.0 : marker.height() / 2.0;
        if (marker != null) {
            for (int s = 0; s < data.seriesCount(); s++) {
                ChartData.Series series = data.series().get(s);
                DocumentColor seriesColor = style.paintForSeries(s, theme.palette()).primaryColor();
                DocumentColor fill = marker.fill() == null
                        ? seriesColor : marker.fill().primaryColor();
                for (int c = 0; c < cats; c++) {
                    Double v = series.values().get(c);
                    if (v == null) {
                        continue;
                    }
                    double px = f.plotLeftX() + (c + 0.5) * slotW;
                    double py = f.yForValue(v);
                    out.add(new ChartPrimitive(
                            new EllipseNode("point_s" + s + "_c" + c,
                                    marker.width(), marker.height(), fill, marker.stroke(),
                                    null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                                    null),
                            px - marker.width() / 2.0, py - markerHalfH,
                            marker.width(), marker.height()));
                }
            }
        }

        // Pass 3 — value labels above strokes AND markers, with per-category
        // collision flipping and the configurable halo chip.
        if (line.valueLabels() == ValueLabelMode.OUTSIDE) {
            emitValueLabels(out, f, line, style, metrics, slotW, markerHalfH, labelGap);
        }

        if (line.showCategoryLabels()) {
            ChartLayoutSupport.emitCategoryLabels(out, f, data, style);
        }
        ChartLayoutSupport.emitLegend(out, line.legend(), reserve, legendEntries, style,
                width, height, f.plotCenterY(), metrics);
        return out;
    }

    /**
     * Splits a series into contiguous non-null runs of (x, y) data points.
     */
    private static List<List<double[]>> sampleSeries(ChartData.Series series,
                                                     ChartLayoutSupport.Frame f,
                                                     double slotW) {
        List<List<double[]>> runs = new ArrayList<>();
        List<double[]> current = new ArrayList<>();
        for (int c = 0; c < series.values().size(); c++) {
            Double v = series.values().get(c);
            if (v == null) {
                if (!current.isEmpty()) {
                    runs.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(new double[]{
                    f.plotLeftX() + (c + 0.5) * slotW, f.yForValue(v)});
        }
        if (!current.isEmpty()) {
            runs.add(current);
        }
        return runs;
    }

    /**
     * Uniform Catmull-Rom control points (tension 0.5, clamped endpoints)
     * for every span of a run: {@code c1 = p1 + (p2 - p0) / 6},
     * {@code c2 = p2 - (p3 - p1) / 6}. Pure arithmetic on the data points —
     * the exact continuous curve the pre-1.8 fixed-step sampler approximated.
     * Returns one {@code [c1, c2]} pair per span.
     */
    private static List<double[][]> catmullRomControls(List<double[]> points) {
        List<double[][]> controls = new ArrayList<>(points.size() - 1);
        for (int i = 0; i < points.size() - 1; i++) {
            double[] p0 = points.get(Math.max(0, i - 1));
            double[] p1 = points.get(i);
            double[] p2 = points.get(i + 1);
            double[] p3 = points.get(Math.min(points.size() - 1, i + 2));
            double[] c1 = {p1[0] + (p2[0] - p0[0]) / 6.0, p1[1] + (p2[1] - p0[1]) / 6.0};
            double[] c2 = {p2[0] - (p3[0] - p1[0]) / 6.0, p2[1] - (p3[1] - p1[1]) / 6.0};
            controls.add(new double[][]{c1, c2});
        }
        return controls;
    }

    /**
     * One stroked native-Bézier {@code PathNode} primitive covering a whole
     * smooth run. The box is the bounding box of the data points and every
     * control point, so normalized coordinates stay within the unit box by
     * construction.
     */
    private static ChartPrimitive bezierRun(String name, List<double[]> run, DocumentStroke stroke) {
        List<double[][]> controls = catmullRomControls(run);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (double[] p : run) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        for (double[][] c : controls) {
            for (double[] p : c) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        double w = Math.max(MIN_SEGMENT, maxX - minX);
        double h = Math.max(MIN_SEGMENT, maxY - minY);

        List<DocumentPathSegment> segments = new ArrayList<>(run.size());
        segments.add(DocumentPathSegment.moveTo(
                (run.get(0)[0] - minX) / w, (run.get(0)[1] - minY) / h));
        for (int i = 0; i < controls.size(); i++) {
            double[][] c = controls.get(i);
            double[] end = run.get(i + 1);
            segments.add(DocumentPathSegment.cubicTo(
                    (c[0][0] - minX) / w, (c[0][1] - minY) / h,
                    (c[1][0] - minX) / w, (c[1][1] - minY) / h,
                    (end[0] - minX) / w, (end[1] - minY) / h));
        }
        PathNode node = new PathNode(name, w, h, segments, null, stroke,
                DocumentInsets.zero(), DocumentInsets.zero());
        return new ChartPrimitive(node, minX, minY, w, h);
    }

    /**
     * Curved area fill for a smooth run: the exact stroke curve closed down
     * to the plot baseline with two straight edges, emitted as one filled
     * {@code PathNode}.
     */
    private static void emitCurvedArea(List<ChartPrimitive> out, String name,
                                       List<double[]> run, double baselineY,
                                       DocumentColor fill) {
        List<double[][]> controls = catmullRomControls(run);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = baselineY;
        double maxY = baselineY;
        for (double[] p : run) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        for (double[][] c : controls) {
            for (double[] p : c) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        double w = Math.max(1.0, maxX - minX);
        double h = Math.max(1.0, maxY - minY);

        List<DocumentPathSegment> segments = new ArrayList<>(run.size() + 3);
        segments.add(DocumentPathSegment.moveTo(
                (run.get(0)[0] - minX) / w, (run.get(0)[1] - minY) / h));
        for (int i = 0; i < controls.size(); i++) {
            double[][] c = controls.get(i);
            double[] end = run.get(i + 1);
            segments.add(DocumentPathSegment.cubicTo(
                    (c[0][0] - minX) / w, (c[0][1] - minY) / h,
                    (c[1][0] - minX) / w, (c[1][1] - minY) / h,
                    (end[0] - minX) / w, (end[1] - minY) / h));
        }
        double baselineNorm = (baselineY - minY) / h;
        segments.add(DocumentPathSegment.lineTo(
                (run.get(run.size() - 1)[0] - minX) / w, baselineNorm));
        segments.add(DocumentPathSegment.lineTo(
                (run.get(0)[0] - minX) / w, baselineNorm));
        segments.add(DocumentPathSegment.close());

        PathNode node = new PathNode(name, w, h, segments, fill, null,
                DocumentInsets.zero(), DocumentInsets.zero());
        out.add(new ChartPrimitive(node, minX, minY, w, h));
    }

    /**
     * Closes a sampled run down to the plot baseline and emits it as a polygon.
     */
    private static void emitAreaPolygon(List<ChartPrimitive> out, String name,
                                        List<double[]> run, double baselineY,
                                        DocumentColor fill) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = baselineY;
        for (double[] p : run) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        double w = Math.max(1.0, maxX - minX);
        double h = Math.max(1.0, maxY - baselineY);
        List<ShapePoint> pts = new ArrayList<>(run.size() + 2);
        for (double[] p : run) {
            pts.add(new ShapePoint(
                    clamp01((p[0] - minX) / w),
                    clamp01((p[1] - baselineY) / h)));
        }
        pts.add(new ShapePoint(clamp01((run.get(run.size() - 1)[0] - minX) / w), 0.0));
        pts.add(new ShapePoint(clamp01((run.get(0)[0] - minX) / w), 0.0));
        out.add(new ChartPrimitive(
                new PolygonNode(name, w, h, pts, fill, null,
                        DocumentInsets.zero(), DocumentInsets.zero()),
                minX, baselineY, w, h));
    }

    private static double clamp01(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }

    /**
     * Emits per-point value labels with deterministic collision handling: per
     * category, candidates are processed top-down; a label whose box intersects
     * an already-placed label at the same category flips below its point.
     */
    private static void emitValueLabels(List<ChartPrimitive> out, ChartLayoutSupport.Frame f,
                                        ChartSpec.Line line, ChartStyle style,
                                        ChartTextMetrics metrics, double slotW,
                                        double markerHalfH, double labelGap) {
        ChartData data = line.data();
        DocumentTextStyle valueStyle = style.valueLabelTextStyle();
        DocumentColor halo = style.valueLabelHalo() == null
                ? null : style.valueLabelHalo().primaryColor();

        for (int c = 0; c < data.categoryCount(); c++) {
            double px = f.plotLeftX() + (c + 0.5) * slotW;

            List<int[]> order = new ArrayList<>();
            for (int s = 0; s < data.seriesCount(); s++) {
                if (data.series().get(s).values().get(c) != null) {
                    order.add(new int[]{s});
                }
            }
            int catIndex = c;
            order.sort((a, b) -> {
                double ya = f.yForValue(data.series().get(a[0]).values().get(catIndex));
                double yb = f.yForValue(data.series().get(b[0]).values().get(catIndex));
                int cmp = Double.compare(yb, ya);
                return cmp != 0 ? cmp : Integer.compare(a[0], b[0]);
            });

            List<double[]> placedBoxes = new ArrayList<>();
            for (int[] entry : order) {
                int s = entry[0];
                double v = data.series().get(s).values().get(c);
                double py = f.yForValue(v);
                String text = line.valueAxis().format().format(v);
                double labelW = Math.max(8.0, metrics.width(valueStyle, text) + 2.0);

                double yBottom = py + markerHalfH + labelGap;
                if (intersectsAny(placedBoxes, yBottom, yBottom + f.valueLineH())) {
                    // Flip below the point; close series read better split around it.
                    yBottom = py - markerHalfH - labelGap - f.valueLineH();
                }
                placedBoxes.add(new double[]{yBottom, yBottom + f.valueLineH()});
                emitChipLabel(out, "value_s" + s + "_c" + c, text, valueStyle, halo,
                        px, yBottom, labelW, f.valueLineH());
            }
        }
    }

    private static boolean intersectsAny(List<double[]> boxes, double yBottom, double yTop) {
        for (double[] box : boxes) {
            if (yBottom < box[1] && yTop > box[0]) {
                return true;
            }
        }
        return false;
    }
}
