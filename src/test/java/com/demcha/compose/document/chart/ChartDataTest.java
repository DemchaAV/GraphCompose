package com.demcha.compose.document.chart;

import com.demcha.compose.document.chart.ChartData.Series;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value-level contract of {@link ChartData} and its {@link Series}: null
 * entries are tolerated as gaps, but non-finite values are rejected at
 * authoring time (rather than poisoning axis derivation and surfacing as a
 * misleading "height must be finite" failure deep in the layout pass).
 */
class ChartDataTest {

    @Test
    void nullValuesAreAllowedAsGaps() {
        Series s = new Series("revenue", Arrays.asList(1.0, null, 3.0));

        assertThat(s.values()).containsExactly(1.0, null, 3.0);
    }

    @Test
    void nanValueIsRejectedNamingTheSeriesAndIndex() {
        assertThatThrownBy(() -> new Series("revenue", Arrays.asList(1.0, Double.NaN, 3.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenue")
                .hasMessageContaining("index 1 must be finite");
    }

    @Test
    void infiniteValuesAreRejected() {
        assertThatThrownBy(() -> Series.of("a", 1.0, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> Series.of("b", Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void finiteSeriesBuildsCleanly() {
        ChartData data = ChartData.builder()
                .categories("Q1", "Q2")
                .series("revenue", 10.0, 20.0)
                .build();

        assertThat(data.seriesCount()).isEqualTo(1);
        assertThat(data.categoryCount()).isEqualTo(2);
        assertThat(data.series().get(0).values()).containsExactly(10.0, 20.0);
    }

    @Test
    void raggedSeriesIsRejected() {
        assertThatThrownBy(() -> new ChartData(List.of("Q1", "Q2"),
                List.of(Series.of("revenue", 10.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("values but there are");
    }
}
