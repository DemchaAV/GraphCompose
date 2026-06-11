package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.style.*;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.chart.ChartLayoutSupport.*;

/**
 * Geometry for line charts: straight or Catmull-Rom-smoothed polylines,
 * optional translucent area fills, point markers, and collision-aware value
 * labels.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class LineChartLayout {

    /**
     * Sub-segments per Catmull-Rom span; fixed so geometry stays deterministic.
     */
    private static final int SMOOTH_SUBDIVISIONS = 8;
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

        // Per-series sampled polylines: contiguous non-null runs, optionally
        // Catmull-Rom smoothed. Samples drive area fills and stroke segments;
        // markers and labels stay on the original data points.
        List<List<List<double[]>>> sampledRuns = new ArrayList<>();
        for (int s = 0; s < data.seriesCount(); s++) {
            sampledRuns.add(sampleSeries(data.series().get(s), f, slotW, line.smooth()));
        }

        // Pass 0 — area fills, under every stroke.
        if (line.area()) {
            for (int s = 0; s < data.seriesCount(); s++) {
                DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
                DocumentColor fill = color.withOpacity(areaOpacity);
                int runIndex = 0;
                for (List<double[]> run : sampledRuns.get(s)) {
                    if (run.size() < 2) {
                        runIndex++;
                        continue;
                    }
                    emitAreaPolygon(out, "area_s" + s + "_r" + runIndex, run,
                            f.plotBottomY(), fill);
                    runIndex++;
                }
            }
        }

        // Pass 1 — every series' stroke segments.
        for (int s = 0; s < data.seriesCount(); s++) {
            DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
            DocumentStroke stroke = DocumentStroke.of(color, strokeWidth);
            int n = 0;
            for (List<double[]> run : sampledRuns.get(s)) {
                for (int i = 1; i < run.size(); i++) {
                    out.add(segment("line_s" + s + "_seg" + n++,
                            run.get(i - 1)[0], run.get(i - 1)[1],
                            run.get(i)[0], run.get(i)[1], stroke));
                }
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
     * Splits a series into contiguous non-null runs of (x, y) samples.
     */
    private static List<List<double[]>> sampleSeries(ChartData.Series series,
                                                     ChartLayoutSupport.Frame f,
                                                     double slotW, boolean smooth) {
        List<List<double[]>> runs = new ArrayList<>();
        List<double[]> current = new ArrayList<>();
        for (int c = 0; c < series.values().size(); c++) {
            Double v = series.values().get(c);
            if (v == null) {
                if (!current.isEmpty()) {
                    runs.add(smooth ? smoothRun(current) : current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(new double[]{
                    f.plotLeftX() + (c + 0.5) * slotW, f.yForValue(v)});
        }
        if (!current.isEmpty()) {
            runs.add(smooth ? smoothRun(current) : current);
        }
        return runs;
    }

    /**
     * Subdivides a run with a centripetal-style Catmull-Rom spline (uniform
     * parameterisation, clamped endpoints) into {@link #SMOOTH_SUBDIVISIONS}
     * sub-segments per span. Pure arithmetic on the input points.
     */
    private static List<double[]> smoothRun(List<double[]> points) {
        if (points.size() < 3) {
            return points;
        }
        List<double[]> samples = new ArrayList<>();
        samples.add(points.get(0));
        for (int i = 0; i < points.size() - 1; i++) {
            double[] p0 = points.get(Math.max(0, i - 1));
            double[] p1 = points.get(i);
            double[] p2 = points.get(i + 1);
            double[] p3 = points.get(Math.min(points.size() - 1, i + 2));
            for (int t = 1; t <= SMOOTH_SUBDIVISIONS; t++) {
                double u = (double) t / SMOOTH_SUBDIVISIONS;
                samples.add(new double[]{
                        catmullRom(p0[0], p1[0], p2[0], p3[0], u),
                        catmullRom(p0[1], p1[1], p2[1], p3[1], u)});
            }
        }
        return samples;
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2 * p1)
                      + (-p0 + p2) * t
                      + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                      + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
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
