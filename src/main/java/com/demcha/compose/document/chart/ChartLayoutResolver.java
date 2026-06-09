package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure geometry engine: {@code (spec, style, theme, w, h, metrics) →
 * List<ChartPrimitive>}. Deterministic by contract — no randomness, no system
 * fonts read outside {@code metrics}, no I/O, no hidden state — so identical
 * inputs yield identical positioned primitives and therefore byte-identical
 * output, exactly like the rest of the engine.
 *
 * <p>This is the ONLY genuinely new layout logic charts introduce. Everything it
 * emits is an existing primitive node ({@link ShapeNode} bars/swatches,
 * {@link LineNode} grid lines and polyline segments, {@link ParagraphNode}
 * labels) wrapped in a {@link ChartPrimitive}; those then flow through the normal
 * layout + render path via {@code ChartDefinition}.</p>
 *
 * <p>All coordinates are <b>bottom-up</b> in the chart's inner content box:
 * {@code (0,0)} is bottom-left, {@code y} grows up. See {@link ChartPrimitive}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class ChartLayoutResolver {

    private static final double GAP = 4.0;
    private static final double DEFAULT_LINE_WIDTH = 1.5;
    private static final double SWATCH_SIZE = 9.0;
    private static final double SWATCH_LABEL_GAP = 4.0;
    private static final double LEGEND_ENTRY_GAP = 14.0;
    private static final double MIN_SEGMENT = 0.25;
    private static final double MIN_BAR_HEIGHT = 0.3;
    private static final double DEFAULT_VALUE_LABEL_OFFSET = 2.0;
    private static final double HALO_PAD_X = 2.0;
    private static final double HALO_PAD_Y = 1.0;
    /** Cap height as a fraction of font size (Helvetica-class fonts ≈ 0.72em). */
    private static final double CAP_HEIGHT_RATIO = 0.70;

    private ChartLayoutResolver() {
    }

    /**
     * Resolves a chart spec into positioned primitive children.
     *
     * @param spec what to plot
     * @param style fully-coalesced style (theme already merged in)
     * @param theme active chart theme (supplies the fallback palette)
     * @param width chart inner box width in points
     * @param height chart inner box height in points
     * @param metrics text measurement seam for label sizing
     * @return positioned primitive nodes filling the chart box; never null
     */
    public static List<ChartPrimitive> resolve(ChartSpec spec,
                                               ChartStyle style,
                                               ChartTheme theme,
                                               double width,
                                               double height,
                                               ChartTextMetrics metrics) {
        // Dispatch on the sealed ChartSpec. A new kind adds a branch here; the
        // final throw guards against an unhandled permitted subtype.
        if (spec instanceof ChartSpec.Bar bar) {
            return resolveBar(bar, style, theme, width, height, metrics);
        }
        if (spec instanceof ChartSpec.Line line) {
            return resolveLine(line, style, theme, width, height, metrics);
        }
        throw new IllegalStateException("unsupported chart spec: " + spec.getClass().getName());
    }

    // ------------------------------------------------------------------
    // Shared plot frame
    // ------------------------------------------------------------------

    /** Plot rectangle and scale, all in bottom-up inner-box coordinates. */
    private record Frame(NiceScale scale, double plotLeftX, double plotRightX,
                         double plotBottomY, double plotTopY,
                         double leftGutter, double legendH,
                         double axisLineH, double legendLineH, double valueLineH) {
        double plotWidth() {
            return plotRightX - plotLeftX;
        }

        double plotHeight() {
            return plotTopY - plotBottomY;
        }

        double yForValue(double value) {
            return plotBottomY + scale.fractionOf(value) * plotHeight();
        }
    }

    private static Frame computeFrame(ChartData data, AxisSpec axis, LegendPosition legend,
                                      ValueLabelMode valueLabels, boolean stacked,
                                      boolean showCategoryLabels,
                                      ChartStyle style, double width, double height,
                                      ChartTextMetrics metrics) {
        DocumentTextStyle axisStyle = style.axisTextStyle();
        double axisLineH = metrics.lineHeight(axisStyle);
        double legendLineH = metrics.lineHeight(style.legendTextStyle());
        double valueLineH = metrics.lineHeight(style.valueLabelTextStyle());

        double[] domain = domain(data, stacked, axis.baselineAtZero());
        // Explicit axis bounds override the data-derived domain; NiceScale still
        // rounds outward so ticks stay on nice values.
        if (axis.min() != null) {
            domain[0] = axis.min();
        }
        if (axis.max() != null) {
            domain[1] = axis.max();
        }
        NiceScale scale = NiceScale.compute(domain[0], domain[1],
                axis.baselineAtZero() && axis.min() == null, ChartDefaults.TARGET_TICKS);

        // The value axis reserves a left gutter only when its tick labels show.
        double leftGutter = 0.0;
        if (axis.showTickLabels()) {
            double widestTick = 0.0;
            for (int i = 0; i < scale.tickCount(); i++) {
                double tickValue = scale.niceMin() + i * scale.tickStep();
                widestTick = Math.max(widestTick, metrics.width(axisStyle, axis.format().format(tickValue)));
            }
            leftGutter = widestTick + GAP;
        }

        double bottomGutter = showCategoryLabels ? axisLineH + GAP : 0.0;
        double legendH = legend == LegendPosition.BOTTOM ? legendLineH + GAP : 0.0;
        // Top headroom: value labels and/or the top half of a point marker must
        // not clip when the highest data point touches the plot top.
        double markerHalf = style.pointMarker() == null ? 0.0 : style.pointMarker().height() / 2.0;
        double labelGap = style.valueLabelOffset() == null
                ? DEFAULT_VALUE_LABEL_OFFSET : style.valueLabelOffset();
        double topGutter = valueLabels == ValueLabelMode.OUTSIDE
                ? markerHalf + labelGap + valueLineH + GAP
                : Math.max(axisLineH / 2.0, markerHalf);

        double plotBottomY = legendH + bottomGutter;
        double plotTopY = Math.max(plotBottomY + 1.0, height - topGutter);
        double plotLeftX = leftGutter;
        double plotRightX = Math.max(plotLeftX + 1.0, width);

        return new Frame(scale, plotLeftX, plotRightX, plotBottomY, plotTopY,
                leftGutter, legendH, axisLineH, legendLineH, valueLineH);
    }

    private static double[] domain(ChartData data, boolean stacked, boolean includeZero) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        if (stacked) {
            for (int c = 0; c < data.categoryCount(); c++) {
                double sum = 0.0;
                for (ChartData.Series s : data.series()) {
                    Double v = s.values().get(c);
                    if (v != null && v > 0) {
                        sum += v;
                    }
                }
                min = Math.min(min, sum);
                max = Math.max(max, sum);
            }
        } else {
            for (ChartData.Series s : data.series()) {
                for (Double v : s.values()) {
                    if (v != null) {
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                    }
                }
            }
        }
        if (Double.isInfinite(min) || Double.isInfinite(max)) {
            min = 0.0;
            max = 1.0;
        }
        if (includeZero) {
            min = Math.min(min, 0.0);
            max = Math.max(max, 0.0);
        }
        return new double[] {min, max};
    }

    private static void emitGridAndTicks(List<ChartPrimitive> out, Frame f, AxisSpec axis,
                                         ChartStyle style, ChartTextMetrics metrics) {
        DocumentStroke gridStroke = style.grid() == null ? null : style.grid().horizontal();
        DocumentTextStyle axisStyle = style.axisTextStyle();
        // Place the label box so the glyph ink centre lands on the gridline.
        double inkCenter = inkCenter(metrics, axisStyle);
        for (int i = 0; i < f.scale().tickCount(); i++) {
            double tickValue = f.scale().niceMin() + i * f.scale().tickStep();
            double y = f.yForValue(tickValue);
            if (axis.showGridLines() && gridStroke != null) {
                double sw = Math.max(gridStroke.width(), 0.1);
                out.add(new ChartPrimitive(
                        new LineNode("grid_" + i, f.plotWidth(), sw,
                                0.0, sw / 2.0, f.plotWidth(), sw / 2.0,
                                gridStroke, null, null, DocumentInsets.zero(), DocumentInsets.zero()),
                        f.plotLeftX(), y - sw / 2.0, f.plotWidth(), sw));
            }
            if (axis.showTickLabels()) {
                double boxW = Math.max(1.0, f.leftGutter() - GAP);
                out.add(new ChartPrimitive(
                        label("tick_" + i, axis.format().format(tickValue), axisStyle, TextAlign.RIGHT),
                        0.0, y - inkCenter, boxW, f.axisLineH()));
            }
        }
    }

    /** Emits one vertical category-separator grid line per inner slot boundary. */
    private static void emitVerticalGrid(List<ChartPrimitive> out, Frame f, int categoryCount,
                                         ChartStyle style) {
        DocumentStroke stroke = style.grid() == null ? null : style.grid().vertical();
        if (stroke == null || categoryCount < 2) {
            return;
        }
        double sw = Math.max(stroke.width(), 0.1);
        double slotW = f.plotWidth() / categoryCount;
        for (int c = 1; c < categoryCount; c++) {
            double x = f.plotLeftX() + c * slotW;
            out.add(new ChartPrimitive(
                    new LineNode("vgrid_" + c, sw, f.plotHeight(),
                            sw / 2.0, 0.0, sw / 2.0, f.plotHeight(),
                            stroke, null, null, DocumentInsets.zero(), DocumentInsets.zero()),
                    x - sw / 2.0, f.plotBottomY(), sw, f.plotHeight()));
        }
    }

    /**
     * Vertical offset from a label box's bottom to the optical centre of its
     * glyph ink (baseline + half cap height), so adjacent graphics can align to
     * the visible text rather than to the line box (which sits high for digits).
     */
    private static double inkCenter(ChartTextMetrics metrics, DocumentTextStyle style) {
        return metrics.descent(style) + CAP_HEIGHT_RATIO * style.size() / 2.0;
    }

    private static void emitCategoryLabels(List<ChartPrimitive> out, Frame f, ChartData data,
                                           ChartStyle style) {
        double slotW = f.plotWidth() / data.categoryCount();
        DocumentTextStyle axisStyle = style.axisTextStyle();
        for (int c = 0; c < data.categoryCount(); c++) {
            out.add(new ChartPrimitive(
                    label("cat_" + c, data.categories().get(c), axisStyle, TextAlign.CENTER),
                    f.plotLeftX() + c * slotW, f.legendH(), Math.max(1.0, slotW), f.axisLineH()));
        }
    }

    private static void emitLegend(List<ChartPrimitive> out, Frame f, ChartData data,
                                   ChartStyle style, ChartTheme theme, double width,
                                   ChartTextMetrics metrics) {
        DocumentTextStyle legendStyle = style.legendTextStyle();
        int n = data.seriesCount();
        double[] labelWidths = new double[n];
        double total = 0.0;
        for (int s = 0; s < n; s++) {
            labelWidths[s] = Math.max(1.0, metrics.width(legendStyle, data.series().get(s).name()));
            total += SWATCH_SIZE + SWATCH_LABEL_GAP + labelWidths[s];
            if (s < n - 1) {
                total += LEGEND_ENTRY_GAP;
            }
        }
        double curX = Math.max(0.0, (width - total) / 2.0);
        double labelY = Math.max(0.0, (f.legendH() - f.legendLineH()) / 2.0);
        // Centre the swatch on the label's glyph-ink centre, not the line box
        // centre — digits have no descenders, so the line box centre sits low.
        double swatchY = labelY + inkCenter(metrics, legendStyle) - SWATCH_SIZE / 2.0;
        for (int s = 0; s < n; s++) {
            DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
            out.add(new ChartPrimitive(
                    new ShapeNode("legend_swatch_" + s, SWATCH_SIZE, SWATCH_SIZE, color, null,
                            DocumentCornerRadius.of(1.5), null, null,
                            DocumentInsets.zero(), DocumentInsets.zero()),
                    curX, swatchY, SWATCH_SIZE, SWATCH_SIZE));
            double labelX = curX + SWATCH_SIZE + SWATCH_LABEL_GAP;
            out.add(new ChartPrimitive(
                    label("legend_label_" + s, data.series().get(s).name(), legendStyle, TextAlign.LEFT),
                    labelX, labelY, labelWidths[s], f.legendLineH()));
            curX = labelX + labelWidths[s] + LEGEND_ENTRY_GAP;
        }
    }

    // ------------------------------------------------------------------
    // Bar
    // ------------------------------------------------------------------

    private static List<ChartPrimitive> resolveBar(ChartSpec.Bar bar, ChartStyle style,
                                                   ChartTheme theme, double width, double height,
                                                   ChartTextMetrics metrics) {
        if (bar.horizontal()) {
            throw new UnsupportedOperationException(
                    "horizontal bar charts are not yet supported; use a vertical bar chart");
        }
        requireSupportedLegend(bar.legend());
        requireSupportedValueLabels(bar.valueLabels());
        if (bar.grouping() == BarGrouping.STACKED && bar.valueLabels() == ValueLabelMode.OUTSIDE) {
            throw new UnsupportedOperationException(
                    "value labels for stacked bars are not yet supported; use ValueLabelMode.NONE");
        }
        ChartData data = bar.data();
        boolean stacked = bar.grouping() == BarGrouping.STACKED;
        Frame f = computeFrame(data, bar.valueAxis(), bar.legend(), bar.valueLabels(),
                stacked, bar.showCategoryLabels(), style, width, height, metrics);

        List<ChartPrimitive> out = new ArrayList<>();
        emitGridAndTicks(out, f, bar.valueAxis(), style, metrics);
        emitVerticalGrid(out, f, data.categoryCount(), style);

        int cats = data.categoryCount();
        int sCount = data.seriesCount();
        double slotW = f.plotWidth() / cats;
        double ratio = style.barWidthRatio() == null ? ChartDefaults.BAR_WIDTH_RATIO : style.barWidthRatio();
        double groupW = slotW * ratio;
        DocumentCornerRadius barRadius = style.barCornerRadius();
        DocumentTextStyle valueStyle = style.valueLabelTextStyle();
        double range = Math.max(1e-9, f.scale().niceMax() - f.scale().niceMin());

        for (int c = 0; c < cats; c++) {
            double slotX = f.plotLeftX() + c * slotW;
            double groupX = slotX + (slotW - groupW) / 2.0;
            if (stacked) {
                double cum = 0.0;
                for (int s = 0; s < sCount; s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null || v <= 0) {
                        continue;
                    }
                    double segH = (v / range) * f.plotHeight();
                    if (segH < MIN_BAR_HEIGHT) {
                        continue;
                    }
                    DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, groupW, segH, color, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero()),
                            groupX, f.plotBottomY() + cum, groupW, segH));
                    cum += segH;
                }
            } else {
                double barW = groupW / sCount;
                double innerBarW = Math.max(0.5, barW * 0.86);
                for (int s = 0; s < sCount; s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null) {
                        continue;
                    }
                    double h = f.scale().fractionOf(v) * f.plotHeight();
                    if (h < MIN_BAR_HEIGHT) {
                        continue;
                    }
                    double bx = groupX + s * barW + (barW - innerBarW) / 2.0;
                    DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, innerBarW, h, color, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero()),
                            bx, f.plotBottomY(), innerBarW, h));
                    if (bar.valueLabels() == ValueLabelMode.OUTSIDE) {
                        String text = bar.valueAxis().format().format(v);
                        double labelW = Math.max(innerBarW, metrics.width(valueStyle, text) + 2.0);
                        double center = bx + innerBarW / 2.0;
                        double labelGap = style.valueLabelOffset() == null
                                ? DEFAULT_VALUE_LABEL_OFFSET : style.valueLabelOffset();
                        out.add(new ChartPrimitive(
                                label("value_c" + c + "_s" + s, text, valueStyle, TextAlign.CENTER),
                                center - labelW / 2.0, f.plotBottomY() + h + labelGap,
                                labelW, f.valueLineH()));
                    }
                }
            }
        }

        if (bar.showCategoryLabels()) {
            emitCategoryLabels(out, f, data, style);
        }
        if (bar.legend() == LegendPosition.BOTTOM) {
            emitLegend(out, f, data, style, theme, width, metrics);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Line
    // ------------------------------------------------------------------

    private static List<ChartPrimitive> resolveLine(ChartSpec.Line line, ChartStyle style,
                                                    ChartTheme theme, double width, double height,
                                                    ChartTextMetrics metrics) {
        requireSupportedLegend(line.legend());
        requireSupportedValueLabels(line.valueLabels());
        if (line.smooth()) {
            throw new UnsupportedOperationException(
                    "smooth (curved) line charts are not yet supported; use straight segments");
        }
        ChartData data = line.data();
        Frame f = computeFrame(data, line.valueAxis(), line.legend(), line.valueLabels(),
                false, line.showCategoryLabels(), style, width, height, metrics);

        List<ChartPrimitive> out = new ArrayList<>();
        emitGridAndTicks(out, f, line.valueAxis(), style, metrics);
        emitVerticalGrid(out, f, data.categoryCount(), style);

        int cats = data.categoryCount();
        double slotW = f.plotWidth() / cats;
        PointMarker marker = style.pointMarker();
        double labelGap = style.valueLabelOffset() == null
                ? DEFAULT_VALUE_LABEL_OFFSET : style.valueLabelOffset();
        double strokeWidth = style.lineWidth() == null ? DEFAULT_LINE_WIDTH : style.lineWidth();

        // Pass 1 — every series' segments. Pass 2 — markers and labels, so a
        // joint where lines meet always shows its markers on top of all strokes.
        for (int s = 0; s < data.seriesCount(); s++) {
            ChartData.Series series = data.series().get(s);
            DocumentColor color = style.paintForSeries(s, theme.palette()).primaryColor();
            DocumentStroke stroke = DocumentStroke.of(color, strokeWidth);
            double prevX = Double.NaN;
            double prevY = Double.NaN;
            for (int c = 0; c < cats; c++) {
                Double v = series.values().get(c);
                if (v == null) {
                    prevX = Double.NaN;
                    prevY = Double.NaN;
                    continue;
                }
                double px = f.plotLeftX() + (c + 0.5) * slotW;
                double py = f.yForValue(v);
                if (!Double.isNaN(prevX)) {
                    out.add(segment("line_s" + s + "_seg" + c, prevX, prevY, px, py, stroke));
                }
                prevX = px;
                prevY = py;
            }
        }

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

        // Pass 3 — value labels, drawn above strokes AND markers. Per category,
        // a label whose box would overlap an already-placed one flips below its
        // point, so close series stay individually legible. An optional halo
        // chip behind each label keeps digits readable where strokes cross.
        if (line.valueLabels() == ValueLabelMode.OUTSIDE) {
            emitLineValueLabels(out, f, line, style, metrics, slotW, markerHalfH, labelGap);
        }

        if (line.showCategoryLabels()) {
            emitCategoryLabels(out, f, data, style);
        }
        if (line.legend() == LegendPosition.BOTTOM) {
            emitLegend(out, f, data, style, theme, width, metrics);
        }
        return out;
    }

    /**
     * Emits per-point value labels with deterministic collision handling: per
     * category, candidates are processed top-down; a label whose box intersects
     * an already-placed label at the same category flips below its point. When
     * a halo paint is styled, a padded chip is emitted right under each label.
     */
    private static void emitLineValueLabels(List<ChartPrimitive> out, Frame f,
                                            ChartSpec.Line line, ChartStyle style,
                                            ChartTextMetrics metrics, double slotW,
                                            double markerHalfH, double labelGap) {
        ChartData data = line.data();
        DocumentTextStyle valueStyle = style.valueLabelTextStyle();
        DocumentColor halo = style.valueLabelHalo() == null
                ? null : style.valueLabelHalo().primaryColor();

        for (int c = 0; c < data.categoryCount(); c++) {
            double px = f.plotLeftX() + (c + 0.5) * slotW;

            // Candidates sorted by point y descending (then series index for ties).
            List<int[]> order = new ArrayList<>();
            for (int s = 0; s < data.seriesCount(); s++) {
                if (data.series().get(s).values().get(c) != null) {
                    order.add(new int[] {s});
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
                placedBoxes.add(new double[] {yBottom, yBottom + f.valueLineH()});

                if (halo != null) {
                    out.add(new ChartPrimitive(
                            new ShapeNode("value_halo_s" + s + "_c" + c,
                                    labelW + 2 * HALO_PAD_X, f.valueLineH() + 2 * HALO_PAD_Y,
                                    halo, null, DocumentCornerRadius.of(2.0), null, null,
                                    DocumentInsets.zero(), DocumentInsets.zero()),
                            px - labelW / 2.0 - HALO_PAD_X, yBottom - HALO_PAD_Y,
                            labelW + 2 * HALO_PAD_X, f.valueLineH() + 2 * HALO_PAD_Y));
                }
                out.add(new ChartPrimitive(
                        label("value_s" + s + "_c" + c, text, valueStyle, TextAlign.CENTER),
                        px - labelW / 2.0, yBottom, labelW, f.valueLineH()));
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

    private static ChartPrimitive segment(String name, double x0, double y0, double x1, double y1,
                                          DocumentStroke stroke) {
        double minX = Math.min(x0, x1);
        double minY = Math.min(y0, y1);
        double w = Math.max(MIN_SEGMENT, Math.abs(x1 - x0));
        double h = Math.max(MIN_SEGMENT, Math.abs(y1 - y0));
        LineNode node = new LineNode(name, w, h,
                x0 - minX, y0 - minY, x1 - minX, y1 - minY,
                stroke, null, null, DocumentInsets.zero(), DocumentInsets.zero());
        return new ChartPrimitive(node, minX, minY, w, h);
    }

    private static void requireSupportedLegend(LegendPosition legend) {
        if (legend == LegendPosition.RIGHT || legend == LegendPosition.TOP) {
            throw new UnsupportedOperationException(
                    "legend position " + legend + " is not yet supported; use BOTTOM or NONE");
        }
    }

    private static void requireSupportedValueLabels(ValueLabelMode mode) {
        if (mode == ValueLabelMode.INSIDE) {
            throw new UnsupportedOperationException(
                    "ValueLabelMode.INSIDE is not yet supported; use OUTSIDE or NONE");
        }
    }

    private static ParagraphNode label(String name, String text, DocumentTextStyle style, TextAlign align) {
        return new ParagraphNode(name, text, style, align, 0.0,
                DocumentInsets.zero(), DocumentInsets.zero());
    }
}
