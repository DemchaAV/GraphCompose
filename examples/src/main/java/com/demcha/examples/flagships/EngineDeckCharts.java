package com.demcha.examples.flagships;

import static com.demcha.examples.flagships.EngineDeckTheme.AMBER;
import static com.demcha.examples.flagships.EngineDeckTheme.INK;
import static com.demcha.examples.flagships.EngineDeckTheme.SLATE;
import static com.demcha.examples.flagships.EngineDeckTheme.SURFACE;
import static com.demcha.examples.flagships.EngineDeckTheme.SURFACE_LINE;
import static com.demcha.examples.flagships.EngineDeckTheme.VIOLET;
import static com.demcha.examples.flagships.EngineDeckTheme.VIOLET_DEEP;

import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.LineInterpolation;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.util.Locale;

/**
 * Chart-and-table layer for {@link EngineDeckExample} — the v2 "widgets" layer
 * for the data pages. Turns an {@link EngineDeckData.BenchRun} into the
 * comparison table and the native charts on pages 3–4. Colours come from
 * {@link EngineDeckTheme} (imported statically); the data is read, never
 * inlined.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class EngineDeckCharts {

    private EngineDeckCharts() {
    }

    // ── Page 3 — measured comparison ──────────────────────────────────────────

    static void benchTable(EngineDeckData.BenchRun b, TableBuilder table) {
        table.name("BenchTable")
                .columns(
                        DocumentTableColumn.fixed(120),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.symmetric(7, 10))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA).size(9).color(INK).build())
                        .stroke(DocumentStroke.of(SURFACE_LINE, 0.5))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.symmetric(8, 10))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA_BOLD).size(9).color(DocumentColor.WHITE).build())
                        .fillColor(VIOLET_DEEP)
                        .stroke(DocumentStroke.of(VIOLET_DEEP, 0.5))
                        .build())
                .headerRow("Report size", "GraphCompose", "iText 9", "JasperReports")
                .zebra(DocumentColor.WHITE, SURFACE)
                .row("1 page · 3 lines",
                        cell(b, "GraphCompose Canonical"), cell(b, "iText 9"), cell(b, "JasperReports"));
        for (int size : EngineDeckData.SIZES) {
            table.row(size + " rows",
                    cell(b, "GraphCompose (" + size + " rows)"),
                    cell(b, "iText 9 (" + size + " rows)"),
                    cell(b, "JasperReports (" + size + " rows)"));
        }
    }

    private static String cell(EngineDeckData.BenchRun b, String label) {
        return String.format(Locale.ROOT, "%.1f ms · %.1f MB", b.timeMs(label), b.heapMb(label));
    }

    /** Three coloured bars (one per library) at the 1000-row scenario. */
    static ChartSpec timeChart(EngineDeckData.BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("1000-row report")
                        .series("GraphCompose", b.timeMs("GraphCompose", 1000))
                        .series("iText 9", b.timeMs("iText 9", 1000))
                        .series("JasperReports", b.timeMs("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix(" ms")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 8))
                .build();
    }

    static ChartSpec memoryChart(EngineDeckData.BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("1000-row report")
                        .series("GraphCompose", b.heapMb("GraphCompose", 1000))
                        .series("iText 9", b.heapMb("iText 9", 1000))
                        .series("JasperReports", b.heapMb("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix(" MB")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 8))
                .build();
    }

    // ── Page 4 — scaling (real data) ──────────────────────────────────────────

    /** One series per library across the 40 / 200 / 1000-row sweep. */
    private static ChartData bySize(EngineDeckData.BenchRun b, boolean time) {
        var d = ChartData.builder().categories("40", "200", "1000");
        for (String lib : EngineDeckData.LIBS) {
            double[] v = new double[EngineDeckData.SIZES.length];
            for (int i = 0; i < EngineDeckData.SIZES.length; i++) {
                v[i] = time ? b.timeMs(lib, EngineDeckData.SIZES[i]) : b.heapMb(lib, EngineDeckData.SIZES[i]);
            }
            d = d.series(lib, v);
        }
        return d.build();
    }

    static ChartSpec scalingLineChart(EngineDeckData.BenchRun b) {
        return ChartSpec.line()
                .data(bySize(b, true))
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix(" ms")).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    static ChartSpec memoryAreaChart(EngineDeckData.BenchRun b) {
        return ChartSpec.line()
                .data(bySize(b, false))
                .interpolation(LineInterpolation.SMOOTH)
                .area(true)
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0").withSuffix(" MB")).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    static ChartSpec memoryShareDonut(EngineDeckData.BenchRun b) {
        return ChartSpec.pie()
                .data(ChartData.builder()
                        .categories("GraphCompose", "iText 9", "JasperReports")
                        .series("Heap",
                                b.heapMb("GraphCompose", 1000),
                                b.heapMb("iText 9", 1000),
                                b.heapMb("JasperReports", 1000))
                        .build())
                .donutRatio(0.58)
                .sliceLabels(SliceLabelMode.PERCENT)
                .centerText("1000 rows")
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    static ChartSpec throughputChart(EngineDeckData.BenchRun b) {
        return ChartSpec.bar()
                .data(ChartData.builder()
                        .categories("docs / sec")
                        .series("GraphCompose", 1000.0 / b.timeMs("GraphCompose", 1000))
                        .series("iText 9", 1000.0 / b.timeMs("iText 9", 1000))
                        .series("JasperReports", 1000.0 / b.timeMs("JasperReports", 1000))
                        .build())
                .valueAxis(AxisSpec.builder().baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0")).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 6.5))
                .build();
    }

    // ── Chart styles (GraphCompose = violet, iText = slate, Jasper = amber) ───

    static ChartStyle groupedStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .barCornerRadius(DocumentCornerRadius.top(2))
                .build();
    }

    static ChartStyle areaStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .lineWidth(1.6)
                .build();
    }

    static ChartStyle lineStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .lineWidth(2.0)
                .pointMarker(PointMarker.circle(5.0)
                        .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.2)))
                .build();
    }

    static ChartStyle donutStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(VIOLET))
                .seriesPaint(1, DocumentPaint.solid(SLATE))
                .seriesPaint(2, DocumentPaint.solid(AMBER))
                .sliceGapDegrees(2.0)
                .build();
    }
}
