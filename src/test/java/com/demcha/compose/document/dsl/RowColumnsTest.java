package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.style.DocumentRowColumn.auto;
import static com.demcha.compose.document.style.DocumentRowColumn.fixed;
import static com.demcha.compose.document.style.DocumentRowColumn.weight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link RowBuilder#columns(DocumentRowColumn...)}: fixed columns take their point
 * width, intrinsic ({@code auto()}) columns size to their content, and weight
 * columns share the remainder — the layout behind a dot-leader row. A weight-only
 * column list resolves identically to plain {@code weights(...)}.
 *
 * <p>Inner content width is {@code 240 - 2*20 = 200pt}; fill lines report their
 * slot width and named text reports its content width, so both make the resolved
 * column widths observable.</p>
 */
class RowColumnsTest {

    @Test
    void fixedColumnTakesItsPointWidthAndWeightTakesTheRemainder() {
        render(page -> page.addRow(r -> r.gap(0).columns(fixed(40), weight(1))
                .addLine(l -> l.name("a").fill())
                .addLine(l -> l.name("b").fill())), session -> {
            assertThat(widthOf(session, "a")).isCloseTo(40.0, within(0.5));
            assertThat(widthOf(session, "b")).isCloseTo(160.0, within(0.5));
        });
    }

    @Test
    void weightOnlyColumnsResolveLikePlainWeights() {
        render(page -> page.addRow(r -> r.gap(0).columns(weight(1), weight(3))
                .addLine(l -> l.name("a").fill())
                .addLine(l -> l.name("b").fill())), session -> {
            // weights(1, 3) over 200pt -> 50 / 150; columns(weight(1), weight(3)) must match.
            assertThat(widthOf(session, "a")).isCloseTo(50.0, within(0.5));
            assertThat(widthOf(session, "b")).isCloseTo(150.0, within(0.5));
        });
    }

    @Test
    void intrinsicColumnSizesToContentAndWeightTakesRemainder() {
        render(page -> page.addRow(r -> r.gap(0).columns(auto(), weight(1))
                .addParagraph(p -> p.name("label").text("Hi"))
                .addLine(l -> l.name("rest").fill())), session -> {
            double label = widthOf(session, "label");
            double rest = widthOf(session, "rest");
            assertThat(label).isGreaterThan(0.0).isLessThan(100.0);   // content-sized, not a half split
            assertThat(rest).isCloseTo(200.0 - label, within(0.5));   // weight took the remainder
        });
    }

    @Test
    void overConstrainedFixedColumnsThrow() {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addRow(r -> r.gap(0).columns(fixed(150), fixed(150))
                    .addLine(l -> l.name("a").fill())
                    .addLine(l -> l.name("b").fill())));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(session::layoutGraph)
                    .withMessageContaining("available");
        }
    }

    @Test
    void builderRejectsColumnCountMismatch() {
        try (DocumentSession session = smallPage()) {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> session.pageFlow(page -> page.addRow(r -> r.columns(auto())
                            .addParagraph("a")
                            .addParagraph("b"))))
                    .withMessageContaining("columns size");
        }
    }

    @Test
    void rowNodeRejectsBothWeightsAndColumns() {
        SpacerNode a = new SpacerNode("a", 10, 10, DocumentInsets.zero(), DocumentInsets.zero());
        SpacerNode b = new SpacerNode("b", 10, 10, DocumentInsets.zero(), DocumentInsets.zero());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RowNode("r", List.of(a, b), List.of(1.0, 1.0),
                        0, null, null, null, null, null, null, List.of(auto(), auto())))
                .withMessageContaining("both");
    }

    @Test
    void dotLeaderRowFillsBetweenTheLabelAndTheNumber() {
        double gap = 4;
        render(page -> page.addRow(r -> r.gap(gap).columns(auto(), weight(1), auto())
                .addParagraph(p -> p.name("label").text("Chapter 1"))
                .addLine(l -> l.name("leader").fill())
                .addParagraph(p -> p.name("num").text("12"))), session -> {
            double label = widthOf(session, "label");
            double leader = widthOf(session, "leader");
            double num = widthOf(session, "num");
            // The leader fills exactly what is left after the two content columns and the gaps.
            assertThat(leader).isGreaterThan(0.0);
            assertThat(label + leader + num + 2 * gap).isCloseTo(200.0, within(1.0));
        });
    }

    @Test
    void weightColumnsAreByteIdenticalToPlainWeights() {
        double[] viaWeights = twoFillWidths(r -> r.gap(0).weights(1, 3));
        double[] viaColumns = twoFillWidths(r -> r.gap(0).columns(weight(1), weight(3)));
        // The weight-only column path must resolve to exactly the legacy weights result.
        assertThat(viaColumns[0]).isEqualTo(viaWeights[0]);
        assertThat(viaColumns[1]).isEqualTo(viaWeights[1]);
    }

    @Test
    void longAutoLabelClampsToTheRowWithoutOverflowing() {
        // An auto column whose content is wider than the row clamps to the row
        // width (the content wraps) instead of overflowing or throwing.
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addRow(r -> r.gap(0).columns(auto())
                    .addParagraph(p -> p.name("wide")
                            .text("a very long single column label that far exceeds the row width"))));
            assertThat(widthOf(session, "wide")).isLessThanOrEqualTo(200.0 + 0.5);
        }
    }

    private double[] twoFillWidths(Consumer<RowBuilder> spec) {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addRow(r -> {
                spec.accept(r);
                r.addLine(l -> l.name("a").fill());
                r.addLine(l -> l.name("b").fill());
            }));
            return new double[]{widthOf(session, "a"), widthOf(session, "b")};
        }
    }

    private DocumentSession smallPage() {
        return GraphCompose.document().pageSize(240, 200).margin(DocumentInsets.of(20)).create();
    }

    private void render(Consumer<PageFlowBuilder> flow, Consumer<DocumentSession> assertions) {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(flow);
            assertions.accept(session);
        }
    }

    private double widthOf(DocumentSession session, String name) {
        PlacedNode node = session.layoutGraph().nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not placed"));
        return node.placementWidth();
    }
}
