package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit coverage for the page-flow cursor arithmetic on {@link CompilerState}
 * (the {@code advanceSpace} / {@code closeBottomSpace} helpers pulled off
 * {@link LayoutCompiler}). A 200×100 canvas with 10pt margins gives an 80pt
 * content height, so the page fills at {@code usedHeight == 80}.
 */
class CompilerStateTest {

    private static final double EPS = 1e-9;

    private static CompilerState state() {
        return new CompilerState(LayoutCanvas.from(200, 100, Margin.of(10)));
    }

    @Test
    void advanceSpaceConsumesHeightWhenItFits() {
        CompilerState state = state();
        state.advanceSpace(30);
        assertThat(state.usedHeight).isCloseTo(30, within(EPS));
        assertThat(state.pageIndex).isZero();
        assertThat(state.maxTouchedPage).isZero();
    }

    @Test
    void advanceSpaceSpillsToANewPageWhenItOverflowsAUsedPage() {
        CompilerState state = state();
        state.usedHeight = 70;              // 10pt left on the page
        state.advanceSpace(20);             // doesn't fit → new page, then consume
        assertThat(state.pageIndex).isEqualTo(1);
        assertThat(state.usedHeight).isCloseTo(20, within(EPS));
    }

    @Test
    void advanceSpaceDropsANonPositiveAmount() {
        CompilerState state = state();
        state.usedHeight = 15;
        state.advanceSpace(0);
        state.advanceSpace(-5);
        assertThat(state.usedHeight).isCloseTo(15, within(EPS));
        assertThat(state.pageIndex).isZero();
    }

    @Test
    void advanceSpaceDoesNotPaginateAFreshPageAndCapsAtContentHeight() {
        CompilerState state = state();      // usedHeight 0 → the "already used" guard is false
        state.advanceSpace(1000);           // over-tall on an empty page: stays, capped
        assertThat(state.pageIndex).isZero();
        assertThat(state.usedHeight).isCloseTo(80, within(EPS));
    }

    @Test
    void closeBottomSpacePositiveAdvancesLikeAdvanceSpace() {
        CompilerState state = state();
        state.usedHeight = 10;
        state.closeBottomSpace(20);
        assertThat(state.usedHeight).isCloseTo(30, within(EPS));
    }

    @Test
    void closeBottomSpaceNegativePullsTheCursorUpClampedAtZero() {
        CompilerState state = state();
        state.usedHeight = 30;
        state.closeBottomSpace(-10);
        assertThat(state.usedHeight).isCloseTo(20, within(EPS));

        state.usedHeight = 5;
        state.closeBottomSpace(-10);        // would go negative → clamped to 0
        assertThat(state.usedHeight).isZero();
    }
}
