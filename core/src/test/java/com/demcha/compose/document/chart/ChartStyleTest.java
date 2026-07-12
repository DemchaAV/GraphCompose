package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract of {@link ChartStyle#paintForSeries(int, List)}: a negative index is
 * rejected with a value-naming message (rather than a bare
 * {@code IndexOutOfBoundsException} from the modulo), and a valid index cycles
 * through the palette.
 */
class ChartStyleTest {

    private static final List<DocumentPaint> PALETTE = List.of(
            DocumentPaint.solid(DocumentColor.rgb(10, 20, 30)),
            DocumentPaint.solid(DocumentColor.rgb(40, 50, 60)));

    @Test
    void paintForSeriesRejectsNegativeIndex() {
        ChartStyle style = ChartDefaults.DEFAULT_THEME.toChartStyle();

        assertThatThrownBy(() -> style.paintForSeries(-1, PALETTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("series index must be >= 0");
    }

    @Test
    void paintForSeriesCyclesThroughTheFallbackPalette() {
        // A style with no palette of its own falls back to the supplied palette,
        // cycling by modulo so it never runs out of colours.
        ChartStyle style = ChartStyle.inherit();

        assertThat(style.paintForSeries(0, PALETTE)).isEqualTo(PALETTE.get(0));
        assertThat(style.paintForSeries(1, PALETTE)).isEqualTo(PALETTE.get(1));
        assertThat(style.paintForSeries(2, PALETTE)).isEqualTo(PALETTE.get(0));
    }
}
