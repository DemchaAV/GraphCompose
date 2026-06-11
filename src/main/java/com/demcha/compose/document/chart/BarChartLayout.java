package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.chart.ChartLayoutSupport.GAP;
import static com.demcha.compose.document.chart.ChartLayoutSupport.MIN_BAR_HEIGHT;
import static com.demcha.compose.document.chart.ChartLayoutSupport.emitChipLabel;
import static com.demcha.compose.document.chart.ChartLayoutSupport.inkCenter;
import static com.demcha.compose.document.chart.ChartLayoutSupport.label;
import static com.demcha.compose.document.chart.ChartLayoutSupport.valueLabelGap;

/**
 * Geometry for vertical and horizontal bar charts, grouped or stacked, with
 * per-bar value labels and stacked-total labels.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class BarChartLayout {

    private static final double INNER_BAR_RATIO = 0.86;

    private BarChartLayout() {
    }

    static List<ChartPrimitive> resolve(ChartSpec.Bar bar, ChartStyle style,
                                        ChartTheme theme, double width, double height,
                                        ChartTextMetrics metrics) {
        ChartLayoutSupport.requireSupportedValueLabels(bar.valueLabels());
        return bar.horizontal()
                ? resolveHorizontal(bar, style, theme, width, height, metrics)
                : resolveVertical(bar, style, theme, width, height, metrics);
    }

    // ------------------------------------------------------------------
    // Vertical
    // ------------------------------------------------------------------

    private static List<ChartPrimitive> resolveVertical(ChartSpec.Bar bar, ChartStyle style,
                                                        ChartTheme theme, double width, double height,
                                                        ChartTextMetrics metrics) {
        ChartData data = bar.data();
        boolean stacked = bar.grouping() == BarGrouping.STACKED;
        List<ChartLayoutSupport.LegendEntry> legendEntries =
                ChartLayoutSupport.seriesLegendEntries(data, style, theme);
        ChartLayoutSupport.LegendReserve reserve =
                ChartLayoutSupport.legendReserve(bar.legend(), legendEntries, style, metrics);
        ChartLayoutSupport.Frame f = ChartLayoutSupport.computeFrame(
                data, bar.valueAxis(), reserve, bar.valueLabels(), stacked,
                bar.showCategoryLabels(), style, width, height, metrics);

        List<ChartPrimitive> out = new ArrayList<>();
        ChartLayoutSupport.emitGridAndTicks(out, f, bar.valueAxis(), style, metrics);
        ChartLayoutSupport.emitVerticalGrid(out, f, data.categoryCount(), style);

        int cats = data.categoryCount();
        int sCount = data.seriesCount();
        double slotW = f.plotWidth() / cats;
        double ratio = style.barWidthRatio() == null ? ChartDefaults.BAR_WIDTH_RATIO : style.barWidthRatio();
        double groupW = slotW * ratio;
        DocumentCornerRadius barRadius = style.barCornerRadius();
        DocumentTextStyle valueStyle = style.valueLabelTextStyle();
        DocumentColor halo = style.valueLabelHalo() == null
                ? null : style.valueLabelHalo().primaryColor();
        double labelGap = valueLabelGap(style);
        double range = Math.max(1e-9, f.scale().niceMax() - f.scale().niceMin());

        for (int c = 0; c < cats; c++) {
            double slotX = f.plotLeftX() + c * slotW;
            double groupX = slotX + (slotW - groupW) / 2.0;
            if (stacked) {
                double cum = 0.0;
                double totalValue = 0.0;
                for (int s = 0; s < sCount; s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null || v <= 0) {
                        continue;
                    }
                    totalValue += v;
                    double segH = (v / range) * f.plotHeight();
                    if (segH < MIN_BAR_HEIGHT) {
                        continue;
                    }
                    DocumentPaint paint = style.paintForSeries(s, theme.palette());
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, groupW, segH, null, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                                    null, paint),
                            groupX, f.plotBottomY() + cum, groupW, segH));
                    cum += segH;
                }
                if (bar.valueLabels() == ValueLabelMode.OUTSIDE && totalValue > 0) {
                    String text = bar.valueAxis().format().format(totalValue);
                    double labelW = Math.max(8.0, metrics.width(valueStyle, text) + 2.0);
                    emitChipLabel(out, "total_c" + c, text, valueStyle, halo,
                            groupX + groupW / 2.0, f.plotBottomY() + cum + labelGap,
                            labelW, f.valueLineH());
                }
            } else {
                double barW = groupW / sCount;
                double innerBarW = Math.max(0.5, barW * INNER_BAR_RATIO);
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
                    DocumentPaint paint = style.paintForSeries(s, theme.palette());
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, innerBarW, h, null, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                                    null, paint),
                            bx, f.plotBottomY(), innerBarW, h));
                    if (bar.valueLabels() == ValueLabelMode.OUTSIDE) {
                        String text = bar.valueAxis().format().format(v);
                        double labelW = Math.max(innerBarW, metrics.width(valueStyle, text) + 2.0);
                        emitChipLabel(out, "value_c" + c + "_s" + s, text, valueStyle, null,
                                bx + innerBarW / 2.0, f.plotBottomY() + h + labelGap,
                                labelW, f.valueLineH());
                    }
                }
            }
        }

        if (bar.showCategoryLabels()) {
            ChartLayoutSupport.emitCategoryLabels(out, f, data, style);
        }
        ChartLayoutSupport.emitLegend(out, bar.legend(), reserve, legendEntries, style,
                width, height, f.plotCenterY(), metrics);
        return out;
    }

    // ------------------------------------------------------------------
    // Horizontal (categories on Y, values on X)
    // ------------------------------------------------------------------

    private static List<ChartPrimitive> resolveHorizontal(ChartSpec.Bar bar, ChartStyle style,
                                                          ChartTheme theme, double width, double height,
                                                          ChartTextMetrics metrics) {
        ChartData data = bar.data();
        boolean stacked = bar.grouping() == BarGrouping.STACKED;
        AxisSpec axis = bar.valueAxis();
        List<ChartLayoutSupport.LegendEntry> legendEntries =
                ChartLayoutSupport.seriesLegendEntries(data, style, theme);
        ChartLayoutSupport.LegendReserve reserve =
                ChartLayoutSupport.legendReserve(bar.legend(), legendEntries, style, metrics);

        DocumentTextStyle axisStyle = style.axisTextStyle();
        DocumentTextStyle valueStyle = style.valueLabelTextStyle();
        double axisLineH = metrics.lineHeight(axisStyle);
        double valueLineH = metrics.lineHeight(valueStyle);
        double labelGap = valueLabelGap(style);

        double[] domain = ChartLayoutSupport.domain(data, stacked, axis.baselineAtZero());
        if (axis.min() != null) {
            domain[0] = axis.min();
        }
        if (axis.max() != null) {
            domain[1] = axis.max();
        }
        NiceScale scale = NiceScale.compute(domain[0], domain[1],
                axis.baselineAtZero() && axis.min() == null, ChartDefaults.TARGET_TICKS);

        // Left gutter: widest category label; bottom gutter: value tick labels.
        double leftGutter = 0.0;
        if (bar.showCategoryLabels()) {
            for (String category : data.categories()) {
                leftGutter = Math.max(leftGutter, metrics.width(axisStyle, category));
            }
            leftGutter += GAP;
        }
        double bottomGutter = axis.showTickLabels() ? axisLineH + GAP : 0.0;
        double rightHeadroom = GAP;
        if (bar.valueLabels() == ValueLabelMode.OUTSIDE) {
            double widest = 0.0;
            for (int c = 0; c < data.categoryCount(); c++) {
                double total = 0.0;
                for (int s = 0; s < data.seriesCount(); s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null) {
                        continue;
                    }
                    total += Math.max(0, v);
                    if (!stacked) {
                        widest = Math.max(widest,
                                metrics.width(valueStyle, axis.format().format(v)));
                    }
                }
                if (stacked) {
                    widest = Math.max(widest,
                            metrics.width(valueStyle, axis.format().format(total)));
                }
            }
            rightHeadroom = widest + labelGap + GAP;
        }

        double plotLeftX = leftGutter;
        double plotRightX = Math.max(plotLeftX + 1.0, width - reserve.rightW() - rightHeadroom);
        double plotBottomY = reserve.bottomH() + bottomGutter;
        double plotTopY = Math.max(plotBottomY + 1.0, height - reserve.topH() - GAP);
        double plotW = plotRightX - plotLeftX;
        double plotH = plotTopY - plotBottomY;
        double range = Math.max(1e-9, scale.niceMax() - scale.niceMin());

        List<ChartPrimitive> out = new ArrayList<>();
        // Category separators (GridStyle.vertical) run horizontally here — the
        // category axis is vertical in a horizontal bar chart.
        DocumentStroke separatorStroke = style.grid() == null ? null : style.grid().vertical();
        if (separatorStroke != null && data.categoryCount() >= 2) {
            double sw = Math.max(separatorStroke.width(), 0.1);
            double slotHsep = plotH / data.categoryCount();
            for (int c = 1; c < data.categoryCount(); c++) {
                double y = plotTopY - c * slotHsep;
                out.add(new ChartPrimitive(
                        new LineNode("csep_" + c, plotW, sw, 0.0, sw / 2.0, plotW, sw / 2.0,
                                separatorStroke, null, null,
                                DocumentInsets.zero(), DocumentInsets.zero()),
                        plotLeftX, y - sw / 2.0, plotW, sw));
            }
        }
        DocumentStroke gridStroke = style.grid() == null ? null : style.grid().horizontal();
        for (int i = 0; i < scale.tickCount(); i++) {
            double tickValue = scale.niceMin() + i * scale.tickStep();
            double x = plotLeftX + scale.fractionOf(tickValue) * plotW;
            if (axis.showGridLines() && gridStroke != null) {
                double sw = Math.max(gridStroke.width(), 0.1);
                out.add(new ChartPrimitive(
                        new LineNode("grid_" + i, sw, plotH, sw / 2.0, 0.0, sw / 2.0, plotH,
                                gridStroke, null, null, DocumentInsets.zero(), DocumentInsets.zero()),
                        x - sw / 2.0, plotBottomY, sw, plotH));
            }
            if (axis.showTickLabels()) {
                String text = axis.format().format(tickValue);
                double boxW = Math.max(8.0, metrics.width(axisStyle, text) + 2.0);
                out.add(new ChartPrimitive(
                        label("tick_" + i, text, axisStyle, TextAlign.CENTER),
                        x - boxW / 2.0, reserve.bottomH(), boxW, axisLineH));
            }
        }

        int cats = data.categoryCount();
        int sCount = data.seriesCount();
        double slotH = plotH / cats;
        double ratio = style.barWidthRatio() == null ? ChartDefaults.BAR_WIDTH_RATIO : style.barWidthRatio();
        double groupH = slotH * ratio;
        DocumentCornerRadius barRadius = style.barCornerRadius();
        DocumentColor halo = style.valueLabelHalo() == null
                ? null : style.valueLabelHalo().primaryColor();
        double catInk = inkCenter(metrics, axisStyle);
        double valueInk = inkCenter(metrics, valueStyle);

        for (int c = 0; c < cats; c++) {
            // First category at the top — reading order.
            double slotTop = plotTopY - c * slotH;
            double groupTop = slotTop - (slotH - groupH) / 2.0;
            if (stacked) {
                double cum = 0.0;
                double totalValue = 0.0;
                for (int s = 0; s < sCount; s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null || v <= 0) {
                        continue;
                    }
                    totalValue += v;
                    double segW = (v / range) * plotW;
                    if (segW < MIN_BAR_HEIGHT) {
                        continue;
                    }
                    DocumentPaint paint = style.paintForSeries(s, theme.palette());
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, segW, groupH, null, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                                    null, paint),
                            plotLeftX + cum, groupTop - groupH, segW, groupH));
                    cum += segW;
                }
                if (bar.valueLabels() == ValueLabelMode.OUTSIDE && totalValue > 0) {
                    emitEndLabel(out, "total_c" + c, axis.format().format(totalValue), valueStyle,
                            halo, plotLeftX + cum + labelGap,
                            groupTop - groupH / 2.0 - valueInk, valueLineH, metrics);
                }
            } else {
                double barH = groupH / sCount;
                double innerBarH = Math.max(0.5, barH * INNER_BAR_RATIO);
                for (int s = 0; s < sCount; s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v == null) {
                        continue;
                    }
                    double w = scale.fractionOf(v) * plotW;
                    if (w < MIN_BAR_HEIGHT) {
                        continue;
                    }
                    double barTop = groupTop - s * barH - (barH - innerBarH) / 2.0;
                    DocumentPaint paint = style.paintForSeries(s, theme.palette());
                    out.add(new ChartPrimitive(
                            new ShapeNode("bar_c" + c + "_s" + s, w, innerBarH, null, null,
                                    barRadius, null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                                    null, paint),
                            plotLeftX, barTop - innerBarH, w, innerBarH));
                    if (bar.valueLabels() == ValueLabelMode.OUTSIDE) {
                        emitEndLabel(out, "value_c" + c + "_s" + s, axis.format().format(v),
                                valueStyle, null, plotLeftX + w + labelGap,
                                barTop - innerBarH / 2.0 - valueInk, valueLineH, metrics);
                    }
                }
            }
            if (bar.showCategoryLabels()) {
                double boxW = Math.max(1.0, leftGutter - GAP);
                out.add(new ChartPrimitive(
                        label("cat_" + c, data.categories().get(c), axisStyle, TextAlign.RIGHT),
                        0.0, slotTop - slotH / 2.0 - catInk, boxW, axisLineH));
            }
        }

        ChartLayoutSupport.emitLegend(out, bar.legend(), reserve, legendEntries, style,
                width, height, (plotBottomY + plotTopY) / 2.0, metrics);
        return out;
    }

    private static void emitEndLabel(List<ChartPrimitive> out, String name, String text,
                                     DocumentTextStyle style, DocumentColor halo,
                                     double x, double yBottom, double lineH,
                                     ChartTextMetrics metrics) {
        double labelW = Math.max(8.0, metrics.width(style, text) + 2.0);
        // Left-aligned at the bar end; reuse the chip helper by centring on x + w/2.
        emitChipLabel(out, name, text, style, halo, x + labelW / 2.0, yBottom, labelW, lineH);
    }
}
