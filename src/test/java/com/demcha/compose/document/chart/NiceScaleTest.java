package com.demcha.compose.document.chart;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Golden-table tests for {@link NiceScale} — the deterministic axis-scale
 * keystone. These run with no rendering and pin the exact rounded bounds, step,
 * and tick count for representative ranges.
 */
class NiceScaleTest {

    @Test
    void zeroToTwentyish() {
        NiceScale s = NiceScale.compute(0.0, 18.2, true, 5);
        assertThat(s.niceMin()).isEqualTo(0.0);
        assertThat(s.niceMax()).isEqualTo(20.0);
        assertThat(s.tickStep()).isEqualTo(5.0);
        assertThat(s.tickCount()).isEqualTo(5);
    }

    @Test
    void zeroToHundred() {
        NiceScale s = NiceScale.compute(0.0, 100.0, true, 5);
        assertThat(s.niceMin()).isEqualTo(0.0);
        assertThat(s.niceMax()).isEqualTo(100.0);
        assertThat(s.tickStep()).isEqualTo(20.0);
        assertThat(s.tickCount()).isEqualTo(6);
    }

    @Test
    void nonZeroBaselineWhenNotIncludingZero() {
        NiceScale s = NiceScale.compute(3.0, 9.8, false, 5);
        assertThat(s.niceMin()).isEqualTo(2.0);
        assertThat(s.niceMax()).isEqualTo(10.0);
        assertThat(s.tickStep()).isEqualTo(2.0);
        assertThat(s.tickCount()).isEqualTo(5);
    }

    @Test
    void includeZeroPullsRangeDown() {
        NiceScale s = NiceScale.compute(40.0, 95.0, true, 5);
        assertThat(s.niceMin()).isEqualTo(0.0);
        assertThat(s.niceMax()).isGreaterThanOrEqualTo(95.0);
    }

    @Test
    void degenerateFlatRangeStillPlots() {
        NiceScale s = NiceScale.compute(7.0, 7.0, false, 5);
        assertThat(s.niceMax()).isGreaterThan(s.niceMin());
        assertThat(s.tickCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void fractionOfIsLinear() {
        NiceScale s = NiceScale.compute(0.0, 20.0, true, 5);
        assertThat(s.fractionOf(0.0)).isCloseTo(0.0, within(1e-9));
        assertThat(s.fractionOf(10.0)).isCloseTo(0.5, within(1e-9));
        assertThat(s.fractionOf(20.0)).isCloseTo(1.0, within(1e-9));
    }
}
