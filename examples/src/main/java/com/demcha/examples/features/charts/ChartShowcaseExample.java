package com.demcha.examples.features.charts;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.DocumentPaint;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the chart subsystem: a grouped bar chart with a legend
 * and value labels, and a multi-series line chart. Both are deterministic vector
 * composites — the layout pass compiles them into shapes, lines, and labels, so
 * they render through the standard pipeline with no chart-specific render code.
 */
public final class ChartShowcaseExample {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private ChartShowcaseExample() {
    }

    /**
     * Builds the chart showcase PDF.
     *
     * @return the generated file path
     * @throws Exception when rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/charts", "chart-showcase.pdf");

        ChartData revenue = ChartData.builder()
                .categories("Q1", "Q2", "Q3", "Q4")
                .series("2024", 12.4, 15.1, 9.8, 14.2)
                .series("2025", 14.0, 18.2, 11.3, 16.9)
                .build();

        ChartSpec barSpec = ChartSpec.bar()
                .data(revenue)
                .valueAxis(AxisSpec.builder()
                        .baselineAtZero(true)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix("k"))
                        .build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 7))
                .build();

        ChartStyle barStyle = ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(DocumentColor.rgb(20, 80, 95)))
                .seriesPaint(1, DocumentPaint.solid(DocumentColor.rgb(196, 153, 76)))
                .barCornerRadius(DocumentCornerRadius.top(2))
                .build();

        // Minimal chart: no grid, no axis tick labels, no category labels —
        // only the bars and their value numbers (e.g. 12.4k).
        ChartSpec minimalSpec = ChartSpec.bar()
                .data(revenue)
                .valueAxis(AxisSpec.builder()
                        .showGridLines(false)
                        .showTickLabels(false)
                        .format(NumberFormatSpec.pattern("#,##0.0").withSuffix("k"))
                        .build())
                .showCategoryLabels(false)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 6))
                .build();

        ChartSpec lineSpec = ChartSpec.line()
                .data(revenue)
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 7))
                .build();

        // Point markers: white-ringed dots keep joints legible where lines meet;
        // the ring colour and the ellipse axes are fully configurable.
        ChartStyle lineStyle = ChartStyle.builder()
                .lineWidth(1.8)
                .pointMarker(PointMarker.circle(5.5)
                        .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.2)))
                .valueLabelOffset(3)
                .build();

        // Smooth area: Catmull-Rom curves with a translucent fill to the baseline.
        ChartSpec areaSpec = ChartSpec.line()
                .data(revenue)
                .smooth(true)
                .area(true)
                .legend(LegendPosition.TOP)
                .size(ChartSize.aspectRatio(16, 7))
                .build();

        // Horizontal bars: categories on Y, values on X, legend as a right column.
        ChartSpec horizontalSpec = ChartSpec.bar()
                .data(revenue)
                .horizontal(true)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .legend(LegendPosition.RIGHT)
                .size(ChartSize.aspectRatio(16, 8))
                .build();

        // Pie / donut: one slice per category from a single series.
        ChartData regions = ChartData.builder()
                .categories("EMEA", "Americas", "APAC", "Other")
                .series("Share", 38.0, 31.0, 22.0, 9.0)
                .build();

        ChartSpec pieSpec = ChartSpec.pie()
                .data(regions)
                .sliceLabels(SliceLabelMode.CATEGORY_PERCENT)
                .size(ChartSize.fixedHeight(190))
                .build();

        ChartSpec donutSpec = ChartSpec.pie()
                .data(regions)
                .donutRatio(0.58)
                .sliceLabels(SliceLabelMode.PERCENT)
                .centerText("58.4k")
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.fixedHeight(200))
                .build();

        ChartStyle donutStyle = ChartStyle.builder()
                .sliceGapDegrees(2.0)
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("ChartShowcase")
                    .spacing(16)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Chart showcase")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text("Deterministic, theme-aware vector charts compiled into engine primitives.")
                                    .textStyle(THEME.text().body())
                                    .margin(DocumentInsets.zero()))
                            // Inline sparklines: mini-charts on the text baseline.
                            .addRich(r -> r
                                    .plain("Revenue trend ")
                                    .sparkline(42, 9, DocumentColor.rgb(20, 80, 95),
                                            65.2, 69.8, 74.1, 81.3, 88.2)
                                    .plain("   profit ")
                                    .sparklineLine(42, 9, 1.6, DocumentColor.rgb(196, 153, 76),
                                            28.1, 30.7, 32.9, 36.4, 39.5)))
                    .addSection("BarCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Quarterly revenue — grouped bar")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(barSpec, barStyle))
                    .addSection("MinimalCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Minimal — only bars and value numbers")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(minimalSpec, barStyle))
                    .addSection("LineCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Quarterly revenue — line")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(lineSpec, lineStyle))
                    .addSection("AreaCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Quarterly revenue — smooth area, legend on top")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(areaSpec))
                    .addSection("HorizontalCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Quarterly revenue — horizontal bars, legend on the right")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(horizontalSpec))
                    .addSection("PieCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Regional share — pie")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(pieSpec))
                    .addSection("DonutCard", section -> section
                            .keepTogether()
                            .softPanel(DocumentColor.WHITE, 8, 16)
                            .spacing(10)
                            .addParagraph(p -> p
                                    .text("Regional share — donut with centre KPI and slice gaps")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .chart(donutSpec, donutStyle))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    /**
     * CLI entry point.
     *
     * @param args ignored
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
