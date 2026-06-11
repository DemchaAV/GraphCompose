package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared geometry helpers for the per-kind chart layouts: the axis frame with
 * legend-strip/column reservations, legend emission, axis ticks and grid,
 * category labels, and halo-backed text chips. All pure and deterministic.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class ChartLayoutSupport {

    static final double GAP = 4.0;
    static final double DEFAULT_LINE_WIDTH = 1.5;
    static final double SWATCH_SIZE = 9.0;
    static final double SWATCH_LABEL_GAP = 4.0;
    static final double LEGEND_ENTRY_GAP = 14.0;
    static final double MIN_SEGMENT = 0.25;
    static final double MIN_BAR_HEIGHT = 0.3;
    static final double DEFAULT_VALUE_LABEL_OFFSET = 2.0;
    static final double HALO_PAD_X = 2.0;
    static final double HALO_PAD_Y = 1.0;
    /**
     * Cap height as a fraction of font size (Helvetica-class fonts ≈ 0.72em).
     */
    static final double CAP_HEIGHT_RATIO = 0.70;

    private ChartLayoutSupport() {
    }

    static List<LegendEntry> seriesLegendEntries(ChartData data, ChartStyle style,
                                                 ChartTheme theme) {
        List<LegendEntry> entries = new ArrayList<>(data.seriesCount());
        for (int s = 0; s < data.seriesCount(); s++) {
            entries.add(new LegendEntry(
                    data.series().get(s).name(),
                    style.paintForSeries(s, theme.palette()).primaryColor()));
        }
        return entries;
    }

    /**
     * Computes the strip/column space the legend reserves around the plot.
     */
    static LegendReserve legendReserve(LegendPosition position, List<LegendEntry> entries,
                                       ChartStyle style, ChartTextMetrics metrics) {
        if (position == LegendPosition.NONE || entries.isEmpty()) {
            return LegendReserve.NONE;
        }
        double legendLineH = metrics.lineHeight(style.legendTextStyle());
        return switch (position) {
            case BOTTOM -> new LegendReserve(legendLineH + GAP, 0, 0);
            case TOP -> new LegendReserve(0, legendLineH + GAP, 0);
            case RIGHT -> {
                double maxLabel = 1.0;
                for (LegendEntry entry : entries) {
                    maxLabel = Math.max(maxLabel,
                            metrics.width(style.legendTextStyle(), entry.name()));
                }
                yield new LegendReserve(0, 0,
                        LEGEND_ENTRY_GAP + SWATCH_SIZE + SWATCH_LABEL_GAP + maxLabel);
            }
            case NONE -> LegendReserve.NONE;
        };
    }

    static Frame computeFrame(ChartData data, AxisSpec axis, LegendReserve reserve,
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
                widestTick = Math.max(widestTick,
                        metrics.width(axisStyle, axis.format().format(tickValue)));
            }
            leftGutter = widestTick + GAP;
        }

        double bottomGutter = showCategoryLabels ? axisLineH + GAP : 0.0;
        // Top headroom: value labels and/or the top half of a point marker must
        // not clip when the highest data point touches the plot top.
        double markerHalf = style.pointMarker() == null ? 0.0 : style.pointMarker().height() / 2.0;
        double labelGap = style.valueLabelOffset() == null
                ? DEFAULT_VALUE_LABEL_OFFSET : style.valueLabelOffset();
        double topGutter = valueLabels == ValueLabelMode.OUTSIDE
                ? markerHalf + labelGap + valueLineH + GAP
                : Math.max(axisLineH / 2.0, markerHalf);
        topGutter += reserve.topH();

        double plotBottomY = reserve.bottomH() + bottomGutter;
        double plotTopY = Math.max(plotBottomY + 1.0, height - topGutter);
        double plotLeftX = leftGutter;
        double plotRightX = Math.max(plotLeftX + 1.0, width - reserve.rightW());

        return new Frame(scale, plotLeftX, plotRightX, plotBottomY, plotTopY,
                leftGutter, reserve.bottomH(), axisLineH, legendLineH, valueLineH);
    }

    static double[] domain(ChartData data, boolean stacked, boolean includeZero) {
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
        return new double[]{min, max};
    }

    static void emitGridAndTicks(List<ChartPrimitive> out, Frame f, AxisSpec axis,
                                 ChartStyle style, ChartTextMetrics metrics) {
        DocumentStroke gridStroke = style.grid() == null ? null : style.grid().horizontal();
        DocumentTextStyle axisStyle = style.axisTextStyle();
        // Place the label box so the glyph ink centre lands on the gridline.
        double ink = inkCenter(metrics, axisStyle);
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
                        0.0, y - ink, boxW, f.axisLineH()));
            }
        }
    }

    /**
     * Emits one vertical category-separator grid line per inner slot boundary.
     */
    static void emitVerticalGrid(List<ChartPrimitive> out, Frame f, int categoryCount,
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

    static void emitCategoryLabels(List<ChartPrimitive> out, Frame f, ChartData data,
                                   ChartStyle style) {
        double slotW = f.plotWidth() / data.categoryCount();
        DocumentTextStyle axisStyle = style.axisTextStyle();
        for (int c = 0; c < data.categoryCount(); c++) {
            out.add(new ChartPrimitive(
                    label("cat_" + c, data.categories().get(c), axisStyle, TextAlign.CENTER),
                    f.plotLeftX() + c * slotW, f.legendH(), Math.max(1.0, slotW), f.axisLineH()));
        }
    }

    /**
     * Emits the legend: a centred horizontal strip for BOTTOM/TOP, or a vertical
     * column right of the plot for RIGHT.
     *
     * @param plotCenterY vertical centre the RIGHT column aligns to
     */
    static void emitLegend(List<ChartPrimitive> out, LegendPosition position,
                           LegendReserve reserve, List<LegendEntry> entries,
                           ChartStyle style, double width, double height,
                           double plotCenterY, ChartTextMetrics metrics) {
        if (position == LegendPosition.NONE || entries.isEmpty()) {
            return;
        }
        DocumentTextStyle legendStyle = style.legendTextStyle();
        double legendLineH = metrics.lineHeight(legendStyle);
        if (position == LegendPosition.RIGHT) {
            double rowH = Math.max(SWATCH_SIZE, legendLineH) + GAP;
            double totalH = entries.size() * rowH - GAP;
            double x0 = width - reserve.rightW() + LEGEND_ENTRY_GAP;
            double top = plotCenterY + totalH / 2.0;
            for (int s = 0; s < entries.size(); s++) {
                double labelBottom = top - s * rowH - legendLineH;
                emitLegendEntry(out, entries.get(s), s, x0, labelBottom, legendStyle,
                        legendLineH, metrics);
            }
            return;
        }
        double stripH = position == LegendPosition.BOTTOM ? reserve.bottomH() : reserve.topH();
        double stripBase = position == LegendPosition.BOTTOM ? 0.0 : height - stripH;
        double labelBottom = stripBase + Math.max(0.0, (stripH - legendLineH) / 2.0);

        double[] labelWidths = new double[entries.size()];
        double total = 0.0;
        for (int s = 0; s < entries.size(); s++) {
            labelWidths[s] = Math.max(1.0, metrics.width(legendStyle, entries.get(s).name()));
            total += SWATCH_SIZE + SWATCH_LABEL_GAP + labelWidths[s];
            if (s < entries.size() - 1) {
                total += LEGEND_ENTRY_GAP;
            }
        }
        double curX = Math.max(0.0, (width - total) / 2.0);
        for (int s = 0; s < entries.size(); s++) {
            emitLegendEntry(out, entries.get(s), s, curX, labelBottom, legendStyle,
                    legendLineH, metrics);
            curX += SWATCH_SIZE + SWATCH_LABEL_GAP + labelWidths[s] + LEGEND_ENTRY_GAP;
        }
    }

    private static void emitLegendEntry(List<ChartPrimitive> out, LegendEntry entry, int index,
                                        double x, double labelBottom,
                                        DocumentTextStyle legendStyle, double legendLineH,
                                        ChartTextMetrics metrics) {
        // Centre the swatch on the label's glyph-ink centre, not the line box
        // centre — digits have no descenders, so the line box centre sits low.
        double swatchY = labelBottom + inkCenter(metrics, legendStyle) - SWATCH_SIZE / 2.0;
        out.add(new ChartPrimitive(
                new ShapeNode("legend_swatch_" + index, SWATCH_SIZE, SWATCH_SIZE,
                        entry.color(), null, DocumentCornerRadius.of(1.5), null, null,
                        DocumentInsets.zero(), DocumentInsets.zero()),
                x, swatchY, SWATCH_SIZE, SWATCH_SIZE));
        double labelW = Math.max(1.0, metrics.width(legendStyle, entry.name()));
        out.add(new ChartPrimitive(
                label("legend_label_" + index, entry.name(), legendStyle, TextAlign.LEFT),
                x + SWATCH_SIZE + SWATCH_LABEL_GAP, labelBottom, labelW, legendLineH));
    }

    /**
     * Emits an optional halo chip followed by a centred label box.
     */
    static void emitChipLabel(List<ChartPrimitive> out, String name, String text,
                              DocumentTextStyle textStyle, DocumentColor halo,
                              double centerX, double yBottom, double labelW, double lineH) {
        if (halo != null) {
            out.add(new ChartPrimitive(
                    new ShapeNode(name + "_halo", labelW + 2 * HALO_PAD_X, lineH + 2 * HALO_PAD_Y,
                            halo, null, DocumentCornerRadius.of(2.0), null, null,
                            DocumentInsets.zero(), DocumentInsets.zero()),
                    centerX - labelW / 2.0 - HALO_PAD_X, yBottom - HALO_PAD_Y,
                    labelW + 2 * HALO_PAD_X, lineH + 2 * HALO_PAD_Y));
        }
        out.add(new ChartPrimitive(
                label(name, text, textStyle, TextAlign.CENTER),
                centerX - labelW / 2.0, yBottom, labelW, lineH));
    }

    /**
     * Vertical offset from a label box's bottom to the optical centre of its
     * glyph ink (baseline + half cap height), so adjacent graphics can align to
     * the visible text rather than to the line box (which sits high for digits).
     */
    static double inkCenter(ChartTextMetrics metrics, DocumentTextStyle style) {
        return metrics.descent(style) + CAP_HEIGHT_RATIO * style.size() / 2.0;
    }

    static ChartPrimitive segment(String name, double x0, double y0, double x1, double y1,
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

    static ParagraphNode label(String name, String text, DocumentTextStyle style, TextAlign align) {
        return new ParagraphNode(name, text, style, align, 0.0,
                DocumentInsets.zero(), DocumentInsets.zero());
    }

    static double valueLabelGap(ChartStyle style) {
        return style.valueLabelOffset() == null
                ? DEFAULT_VALUE_LABEL_OFFSET : style.valueLabelOffset();
    }

    static void requireSupportedValueLabels(ValueLabelMode mode) {
        if (mode == ValueLabelMode.INSIDE) {
            throw new UnsupportedOperationException(
                    "ValueLabelMode.INSIDE is not yet supported; use OUTSIDE or NONE");
        }
    }

    /**
     * One legend entry: a swatch colour plus its label.
     */
    record LegendEntry(String name, DocumentColor color) {
    }

    /**
     * Space reserved for the legend on each side of the plot.
     */
    record LegendReserve(double bottomH, double topH, double rightW) {
        static final LegendReserve NONE = new LegendReserve(0, 0, 0);
    }

    /**
     * Plot rectangle and scale, all in bottom-up inner-box coordinates.
     */
    record Frame(NiceScale scale, double plotLeftX, double plotRightX,
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

        double plotCenterY() {
            return (plotBottomY + plotTopY) / 2.0;
        }
    }
}
