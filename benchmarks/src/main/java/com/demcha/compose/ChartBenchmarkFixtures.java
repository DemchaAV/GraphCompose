package com.demcha.compose;

import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.BarGrouping;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;

/**
 * Shared fixtures for the v1.8 chart benchmarks: a non-trivial grouped bar and
 * multi-series line (12 categories × 3 series) plus a 6-slice pie. Charts
 * compile at layout time into ordinary shapes / lines / polygons / labels, so
 * these stress {@code ChartLayoutResolver} + per-primitive geometry + label
 * text-metrics — the cost no text/table bench exercises.
 *
 * @author Artem Demchyshyn
 */
public final class ChartBenchmarkFixtures {

    private ChartBenchmarkFixtures() {
    }

    /** 12 categories × 3 series — a representative grouped-bar / line workload. */
    public static ChartData monthlySeries() {
        return ChartData.builder()
                .categories("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                .series("2023", 12.4, 15.1, 9.8, 14.2, 16.0, 13.3, 17.1, 18.4, 15.9, 14.0, 19.2, 21.1)
                .series("2024", 14.0, 18.2, 11.3, 16.9, 17.5, 15.0, 19.0, 20.2, 17.1, 16.4, 21.0, 23.5)
                .series("2025", 15.5, 19.0, 12.0, 18.0, 19.1, 16.2, 20.5, 22.0, 18.9, 17.7, 22.8, 25.0)
                .build();
    }

    /** 6-slice single-series data for the pie. */
    public static ChartData regionShare() {
        return ChartData.builder()
                .categories("EMEA", "Americas", "APAC", "LATAM", "MEA", "Other")
                .series("Share", 31.0, 27.0, 19.0, 10.0, 8.0, 5.0)
                .build();
    }

    public static ChartSpec barSpec() {
        return ChartSpec.bar()
                .data(monthlySeries())
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .legend(LegendPosition.BOTTOM)
                .valueLabels(ValueLabelMode.OUTSIDE)
                .size(ChartSize.aspectRatio(16, 7))
                .build();
    }

    public static ChartStyle barStyle() {
        return ChartStyle.builder()
                .seriesPaint(0, DocumentPaint.solid(DocumentColor.rgb(20, 80, 95)))
                .seriesPaint(1, DocumentPaint.solid(DocumentColor.rgb(196, 153, 76)))
                .seriesPaint(2, DocumentPaint.solid(DocumentColor.rgb(120, 60, 140)))
                .build();
    }

    public static ChartSpec lineSpec() {
        return ChartSpec.line()
                .data(monthlySeries())
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 7))
                .build();
    }

    public static ChartStyle lineStyle() {
        return ChartStyle.builder()
                .lineWidth(1.8)
                .pointMarker(PointMarker.circle(5.0)
                        .withStroke(DocumentStroke.of(DocumentColor.WHITE, 1.0)))
                .build();
    }

    public static ChartSpec pieSpec() {
        return ChartSpec.pie()
                .data(regionShare())
                .sliceLabels(SliceLabelMode.CATEGORY_PERCENT)
                .size(ChartSize.fixedHeight(190))
                .build();
    }

    /** Horizontal grouped bar — exercises the transposed (category-on-Y) layout branch. */
    public static ChartSpec horizontalBarSpec() {
        return ChartSpec.bar()
                .data(monthlySeries())
                .horizontal(true)
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 9))
                .build();
    }

    /** Stacked bar — exercises the cumulative-stacking layout branch. */
    public static ChartSpec stackedBarSpec() {
        return ChartSpec.bar()
                .data(monthlySeries())
                .grouping(BarGrouping.STACKED)
                .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 7))
                .build();
    }

    /** Bar with a non-zero value-axis minimum — exercises the lifted-baseline branch. */
    public static ChartSpec axisMinBarSpec() {
        return ChartSpec.bar()
                .data(monthlySeries())
                .valueAxis(AxisSpec.builder().min(8.0).build())
                .legend(LegendPosition.BOTTOM)
                .size(ChartSize.aspectRatio(16, 7))
                .build();
    }

    /** Donut — exercises the pie's donut-ratio (inner-radius) branch. */
    public static ChartSpec donutSpec() {
        return ChartSpec.pie()
                .data(regionShare())
                .donutRatio(0.55)
                .sliceLabels(SliceLabelMode.CATEGORY_PERCENT)
                .size(ChartSize.fixedHeight(190))
                .build();
    }
}
