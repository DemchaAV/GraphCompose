package com.demcha.compose.engine.components.content.header_footer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code {date}} token resolves from the clock, unless a build pins it.
 *
 * <p>A document that prints today is a different document tomorrow, which is why the repository
 * could not hold the example preview that demonstrates this token to a byte comparison: the guard
 * went red each morning on a tree nobody had touched. Pinning is the same need
 * {@code SOURCE_DATE_EPOCH} answers for archives, and it is what lets that preview be compared
 * like every other.</p>
 */
class HeaderFooterRenderDateTest {

    private static final String PROPERTY = "graphcompose.renderDate";

    @AfterEach
    void releaseTheProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void withoutThePropertyTheTokenResolvesToToday() {
        System.clearProperty(PROPERTY);

        assertThat(HeaderFooterConfig.resolvePlaceholders("{date}", 1, 1))
                .describedAs("a render nobody pinned prints the day it ran")
                .isEqualTo(LocalDate.now().toString());
    }

    @Test
    void aPinnedDateIsWhatTheTokenPrints() {
        System.setProperty(PROPERTY, "2026-01-15");

        assertThat(HeaderFooterConfig.resolvePlaceholders("Issued {date}", 1, 1))
                .describedAs("the pinned date is what makes a document with a date in it "
                        + "reproducible")
                .isEqualTo("Issued 2026-01-15");
    }

    /**
     * A mistyped property renders rather than throws.
     *
     * <p>Failing the render would turn a typo in a build flag into a document that cannot be
     * produced at all. Falling back to the clock produces the document and lets the drift it
     * causes surface where drift is checked.</p>
     */
    @Test
    void aValueThatIsNotADateFallsBackToTheClock() {
        System.setProperty(PROPERTY, "last Tuesday");

        assertThat(HeaderFooterConfig.resolvePlaceholders("{date}", 1, 1))
                .isEqualTo(LocalDate.now().toString());
    }

    @Test
    void theZoneResolverPinsTheSameWay() {
        System.setProperty(PROPERTY, "2026-01-15");

        assertThat(HeaderFooterConfig.builder().build()
                .resolveTokens("{date} · {page}/{pages}", 2, 7))
                .describedAs("both resolvers read the same date; one pinned and one not would "
                        + "print two days in one document")
                .startsWith("2026-01-15");
    }
}
