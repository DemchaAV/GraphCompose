package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code RowBuilder.verticalAlign(...)} seats a row's children on the cross axis
 * within the row band (height = tallest child). A short child sitting beside a
 * 60pt-tall one moves down by the band slack as the alignment changes; TOP (the
 * default) leaves it flush with the band top.
 */
class RowVerticalAlignTest {

    /** Bottom (PDF y) of the named placed node. */
    private static double bottomOf(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .map(PlacedNode::placementY)
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not placed"));
    }

    private static DocumentSession rowWith(RowVerticalAlign align) {
        DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create();
        session.pageFlow(page -> page.addRow(row -> row.verticalAlign(align)
                .addSpacer(s -> s.name("tall").size(10, 60))
                .addSpacer(s -> s.name("short").size(10, 20))));
        return session;
    }

    @Test
    void topAlignSeatsTheShortChildFlushWithTheBandTop() {
        try (DocumentSession session = rowWith(RowVerticalAlign.TOP)) {
            // Band is 60 tall; the 20-tall child sits at the top, so its bottom is
            // 40 above the 60-tall child's bottom.
            assertThat(bottomOf(session, "short") - bottomOf(session, "tall")).isCloseTo(40.0, within(0.5));
        }
    }

    @Test
    void centerAlignCentersTheShortChildInTheBand() {
        try (DocumentSession session = rowWith(RowVerticalAlign.CENTER)) {
            assertThat(bottomOf(session, "short") - bottomOf(session, "tall")).isCloseTo(20.0, within(0.5));
        }
    }

    @Test
    void bottomAlignSeatsTheShortChildOnTheBandBottom() {
        try (DocumentSession session = rowWith(RowVerticalAlign.BOTTOM)) {
            // Bottoms align.
            assertThat(bottomOf(session, "short") - bottomOf(session, "tall")).isCloseTo(0.0, within(0.5));
        }
    }

    @Test
    void centerAlignAlsoSeatsCompositeChildren() {
        // A section is a composite (container) row child, placed through the
        // compileNodeInFixedSlot path rather than the leaf path.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addRow(row -> row.verticalAlign(RowVerticalAlign.CENTER)
                    .addSpacer(s -> s.name("tall").size(10, 60))
                    .addSection(sec -> sec.name("box").addSpacer(s -> s.height(20)))));
            assertThat(bottomOf(session, "box") - bottomOf(session, "tall")).isCloseTo(20.0, within(0.5));
        }
    }

    @Test
    void bottomAlignAccountsForTheChildsOwnMargin() {
        // The short child carries an 8pt bottom margin, so BOTTOM seats its
        // content 8pt above the band bottom — exercising the childMargin.vertical()
        // term of the slack (without it the delta would be 0).
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addRow(row -> row.verticalAlign(RowVerticalAlign.BOTTOM)
                    .addSpacer(s -> s.name("tall").size(10, 60))
                    .addSpacer(s -> s.name("short").size(10, 20).margin(DocumentInsets.bottom(8)))));
            assertThat(bottomOf(session, "short") - bottomOf(session, "tall")).isCloseTo(8.0, within(0.5));
        }
    }

    @Test
    void unalignedRowDefaultsToTop() {
        RowNode row = new RowBuilder()
                .addSpacer(s -> s.name("a").size(10, 10))
                .build();
        assertThat(row.verticalAlign()).isEqualTo(RowVerticalAlign.TOP);
    }

    @Test
    void backwardCompatibleConstructorDefaultsToTop() {
        // The pre-verticalAlign 11-arg constructor still resolves and defaults TOP.
        RowNode row = new RowNode("r", List.of(), List.of(), 0.0, DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, null, null, List.of());
        assertThat(row.verticalAlign()).isEqualTo(RowVerticalAlign.TOP);
    }
}
