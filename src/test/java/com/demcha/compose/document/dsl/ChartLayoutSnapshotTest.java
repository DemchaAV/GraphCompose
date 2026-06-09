package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Layout snapshot + structural coverage for the chart subsystem. The snapshot
 * pins the compiled primitive geometry; the graph assertion confirms a chart
 * lowers into ordinary shape fragments (bars) that reach the renderer with no
 * chart-specific payload.
 */
class ChartLayoutSnapshotTest {

    private static ChartData revenue() {
        return ChartData.builder()
                .categories("Q1", "Q2", "Q3", "Q4")
                .series("2024", 12.4, 15.1, 9.8, 14.2)
                .series("2025", 14.0, 18.2, 11.3, 16.9)
                .build();
    }

    @Test
    void groupedBarMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            ChartSpec spec = ChartSpec.bar()
                    .data(revenue())
                    .legend(LegendPosition.BOTTOM)
                    .valueLabels(ValueLabelMode.OUTSIDE)
                    .size(ChartSize.fixedHeight(150))
                    .build();
            document.pageFlow().name("ChartBarFixture").chart(spec).build();

            LayoutSnapshotAssertions.assertMatches(document, "charts/grouped_bar_basic");
        }
    }

    @Test
    void lineMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            ChartSpec spec = ChartSpec.line()
                    .data(revenue())
                    .valueAxis(AxisSpec.builder().baselineAtZero(true).build())
                    .legend(LegendPosition.BOTTOM)
                    .size(ChartSize.fixedHeight(150))
                    .build();
            document.pageFlow().name("ChartLineFixture").chart(spec).build();

            LayoutSnapshotAssertions.assertMatches(document, "charts/line_basic");
        }
    }

    @Test
    void barChartLowersIntoShapeFragments() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            ChartSpec spec = ChartSpec.bar()
                    .data(revenue())
                    .size(ChartSize.fixedHeight(150))
                    .build();
            document.pageFlow().name("ChartFragmentFixture").chart(spec).build();

            LayoutGraph graph = document.layoutGraph();
            long shapeFragments = graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ShapeFragmentPayload)
                    .count();
            // Eight bars (4 categories x 2 series), all real shape fragments.
            assertThat(shapeFragments).isGreaterThanOrEqualTo(8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
