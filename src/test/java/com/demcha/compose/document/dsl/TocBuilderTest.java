package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentLeader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * {@link TocBuilder} rejects a blank entry label or anchor at the call site with
 * a table-of-contents-scoped message, rather than letting a blank anchor surface
 * a generic link error later from {@code build()}.
 */
class TocBuilderTest {

    @Test
    void entryRowIsBottomAlignedSoTheLeaderSitsOnTheBaseline() {
        List<DocumentNode> nodes = new TocBuilder()
                .leader(DocumentLeader.DOTS)
                .entry("Introduction", "intro")
                .buildEntries();

        DocumentNode entryRow = nodes.get(nodes.size() - 1);   // no title set -> the entry row is last
        assertThat(entryRow).isInstanceOf(RowNode.class);
        assertThat(((RowNode) entryRow).verticalAlign()).isEqualTo(RowVerticalAlign.BOTTOM);
    }

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
