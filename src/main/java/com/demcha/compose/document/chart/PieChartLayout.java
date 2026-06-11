package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.chart.ChartLayoutSupport.*;

/**
 * Geometry for pie/donut charts: arc-tessellated sector polygons, mid-angle
 * slice labels, an optional donut-centre KPI, and a category-listing legend.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class PieChartLayout {

    private PieChartLayout() {
    }

    static List<ChartPrimitive> resolve(ChartSpec.Pie pie, ChartStyle style,
                                        ChartTheme theme, double width, double height,
                                        ChartTextMetrics metrics) {
        ChartData data = pie.data();
        ChartData.Series series = data.series().get(0);

        double total = 0.0;
        for (int c = 0; c < data.categoryCount(); c++) {
            Double v = series.values().get(c);
            if (v == null) {
                continue;
            }
            if (v < 0) {
                throw new IllegalArgumentException(
                        "pie slices cannot be negative: " + v
                        + " at category '" + data.categories().get(c) + "'");
            }
            total += v;
        }
        if (total <= 0) {
            throw new IllegalArgumentException("pie needs a positive total; got " + total);
        }

        List<ChartLayoutSupport.LegendEntry> legendEntries = new ArrayList<>();
        for (int c = 0; c < data.categoryCount(); c++) {
            Double v = series.values().get(c);
            if (v != null && v > 0) {
                legendEntries.add(new ChartLayoutSupport.LegendEntry(
                        data.categories().get(c),
                        style.paintForSeries(c, theme.palette()).primaryColor()));
            }
        }
        ChartLayoutSupport.LegendReserve reserve =
                ChartLayoutSupport.legendReserve(pie.legend(), legendEntries, style, metrics);

        DocumentTextStyle labelStyle = style.valueLabelTextStyle();
        double labelLineH = metrics.lineHeight(labelStyle);
        double labelGap = valueLabelGap(style);
        boolean hasLabels = pie.sliceLabels() != SliceLabelMode.NONE;
        double headroom = hasLabels ? labelLineH + labelGap + HALO_PAD_Y : 0.0;
        double availW = Math.max(8.0, width - reserve.rightW());
        double availH = Math.max(8.0, height - reserve.bottomH() - reserve.topH());
        double radius = Math.max(4.0, Math.min(availW, availH) / 2.0 - headroom);
        double cx = availW / 2.0;
        double cy = reserve.bottomH() + availH / 2.0;

        double gapDeg = style.sliceGapDegrees() == null ? 0.0 : style.sliceGapDegrees();
        DocumentStroke separator = style.sliceStroke() == null
                ? ChartDefaults.SLICE_STROKE : style.sliceStroke();
        double direction = pie.clockwise() ? -1.0 : 1.0;

        List<ChartPrimitive> out = new ArrayList<>();
        DocumentColor halo = style.valueLabelHalo() == null
                ? null : style.valueLabelHalo().primaryColor();

        // Pass 1 — sectors; pass 2 — labels (after every sector) and centre text.
        double angle = pie.startAngleDegrees();
        double[] midAngles = new double[data.categoryCount()];
        for (int c = 0; c < data.categoryCount(); c++) {
            Double v = series.values().get(c);
            midAngles[c] = Double.NaN;
            if (v == null || v <= 0) {
                continue;
            }
            double span = v / total * 360.0;
            double a0 = angle;
            double a1 = angle + direction * span;
            angle = a1;
            double trim = span > gapDeg * 1.5 ? gapDeg / 2.0 : 0.0;
            double ga0 = a0 + direction * trim;
            double ga1 = a1 - direction * trim;
            midAngles[c] = (ga0 + ga1) / 2.0;

            DocumentColor fill = style.paintForSeries(c, theme.palette()).primaryColor();
            out.add(new ChartPrimitive(
                    new PolygonNode("slice_" + c, radius * 2.0, radius * 2.0,
                            sectorPoints(ga0, ga1, pie.donutRatio()), fill, separator,
                            DocumentInsets.zero(), DocumentInsets.zero()),
                    cx - radius, cy - radius, radius * 2.0, radius * 2.0));
        }

        if (hasLabels) {
            for (int c = 0; c < data.categoryCount(); c++) {
                if (Double.isNaN(midAngles[c])) {
                    continue;
                }
                double v = series.values().get(c);
                String text = sliceLabelText(pie, data.categories().get(c), v, total);
                double labelW = Math.max(8.0, metrics.width(labelStyle, text) + 2.0);
                double rad = Math.toRadians(midAngles[c]);
                double labelR = radius + labelGap + labelLineH / 2.0;
                double lx = cx + labelR * Math.cos(rad);
                double ly = cy + labelR * Math.sin(rad);
                emitChipLabel(out, "slice_label_" + c, text, labelStyle, halo,
                        lx, ly - labelLineH / 2.0, labelW, labelLineH);
            }
        }

        if (pie.centerText() != null) {
            DocumentTextStyle centerStyle = style.donutCenterTextStyle() == null
                    ? ChartDefaults.DONUT_CENTER_TEXT_STYLE : style.donutCenterTextStyle();
            double centerLineH = metrics.lineHeight(centerStyle);
            double innerDiameter = Math.max(8.0, 2.0 * radius * pie.donutRatio());
            out.add(new ChartPrimitive(
                    label("donut_center", pie.centerText(), centerStyle, TextAlign.CENTER),
                    cx - innerDiameter / 2.0, cy - centerLineH / 2.0,
                    innerDiameter, centerLineH));
        }

        ChartLayoutSupport.emitLegend(out, pie.legend(), reserve, legendEntries, style,
                width, height, cy, metrics);
        return out;
    }

    private static String sliceLabelText(ChartSpec.Pie pie, String category, double value,
                                         double total) {
        double percent = value / total * 100.0;
        return switch (pie.sliceLabels()) {
            case VALUE -> pie.valueFormat().format(value);
            case PERCENT -> pie.percentFormat().format(percent);
            case CATEGORY -> category;
            case CATEGORY_PERCENT -> category + " " + pie.percentFormat().format(percent);
            case NONE -> "";
        };
    }

    /**
     * Tessellates one pie/donut sector into normalized polygon vertices inside
     * the unit box (the sector's bounding circle). Solid sectors close through
     * the centre; donut sectors trace the outer arc forward and the inner arc
     * back, forming a ring segment. The arc step is fixed
     * ({@link ChartDefaults#SECTOR_TESSELLATION_STEP_DEGREES}) so identical
     * inputs always produce identical vertices.
     */
    private static List<ShapePoint> sectorPoints(double a0, double a1, double donutRatio) {
        int segments = Math.max(2, (int) Math.ceil(
                Math.abs(a1 - a0) / ChartDefaults.SECTOR_TESSELLATION_STEP_DEGREES));
        List<ShapePoint> points = new ArrayList<>(segments + 3);
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / segments);
            points.add(unitPoint(0.5, a));
        }
        if (donutRatio > 0) {
            double inner = 0.5 * donutRatio;
            for (int i = segments; i >= 0; i--) {
                double a = Math.toRadians(a0 + (a1 - a0) * i / segments);
                points.add(unitPoint(inner, a));
            }
        } else {
            points.add(new ShapePoint(0.5, 0.5));
        }
        return points;
    }

    private static ShapePoint unitPoint(double r, double radians) {
        double x = Math.min(1.0, Math.max(0.0, 0.5 + r * Math.cos(radians)));
        double y = Math.min(1.0, Math.max(0.0, 0.5 + r * Math.sin(radians)));
        return new ShapePoint(x, y);
    }
}
