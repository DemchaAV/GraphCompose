package com.demcha.compose.document.dsl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * {@link TocBuilder} rejects a blank entry label or anchor at the call site with
 * a table-of-contents-scoped message, rather than letting a blank anchor surface
 * a generic link error later from {@code build()}.
 */
class TocBuilderTest {

    @Test
    void entryRejectsBlankLabel() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new TocBuilder().entry("  ", "anchor"))
                .withMessageContaining("label");
    }

    @Test
    void entryRejectsBlankAnchor() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new TocBuilder().entry("Introduction", ""))
                .withMessageContaining("anchor");
    }
}
