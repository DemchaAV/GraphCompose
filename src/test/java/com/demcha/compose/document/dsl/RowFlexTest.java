package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Row flex layout: a {@code flexSpacer()} spring absorbs the row's leftover width
 * (pushing following children right), and {@code arrangement(...)} justifies the
 * leftover across content-sized children. Page is 240pt wide with 20pt margins,
 * so the row's inner width is 200pt (left edge x=20, right edge x=220).
 */
class RowFlexTest {

    private static PlacedNode placed(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not placed"));
    }

    private static double leftOf(DocumentSession session, String name) {
        return placed(session, name).placementX();
    }

    private static double rightOf(DocumentSession session, String name) {
        PlacedNode n = placed(session, name);
        return n.placementX() + n.placementWidth();
    }

    private static DocumentSession row(Consumer<RowBuilder> spec) {
        DocumentSession session = GraphCompose.document().pageSize(240, 120).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page.addRow(spec));
        return session;
    }

    @Test
    void flexSpacerPushesFollowingChildrenToTheRightEdge() {
        try (DocumentSession session = row(r -> r.gap(0)
                .addSpacer(s -> s.name("left").size(30, 10))
                .pushRight()
                .addSpacer(s -> s.name("right").size(30, 10)))) {
            assertThat(leftOf(session, "left")).isCloseTo(20.0, within(0.5));   // flush left
            assertThat(rightOf(session, "right")).isCloseTo(220.0, within(0.5)); // flush right
        }
    }

    @Test
    void twoFlexSpacersShareTheLeftoverByGrow() {
        // grow 1 then grow 3 → leftover (200-20 = 180) splits 45 / 135.
        try (DocumentSession session = row(r -> r.gap(0)
                .addSpacer(s -> s.name("a").size(20, 10))
                .flexSpacer(1.0)
                .flexSpacer(3.0)
                .addSpacer(s -> s.name("b").size(0, 10)))) {
            // a ends at 40; first spring 45 → b starts at 40+45+135 = 220 (right edge).
            assertThat(leftOf(session, "b")).isCloseTo(220.0, within(0.5));
        }
    }

    @Test
    void spaceBetweenPinsFirstAndLastToTheEdges() {
        try (DocumentSession session = row(r -> r.gap(0).arrangement(RowArrangement.SPACE_BETWEEN)
                .addSpacer(s -> s.name("a").size(20, 10))
                .addSpacer(s -> s.name("b").size(20, 10))
                .addSpacer(s -> s.name("c").size(20, 10)))) {
            assertThat(leftOf(session, "a")).isCloseTo(20.0, within(0.5));    // first flush left
            assertThat(rightOf(session, "c")).isCloseTo(220.0, within(0.5));  // last flush right
            assertThat(leftOf(session, "b")).isCloseTo(110.0, within(0.5));   // middle centred
        }
    }

    @Test
    void centerArrangementCentersTheContent() {
        // 3 x 20 = 60 content, leftover 140, leading 70 → a at 20+70 = 90.
        try (DocumentSession session = row(r -> r.gap(0).arrangement(RowArrangement.CENTER)
                .addSpacer(s -> s.name("a").size(20, 10))
                .addSpacer(s -> s.name("b").size(20, 10))
                .addSpacer(s -> s.name("c").size(20, 10)))) {
            assertThat(leftOf(session, "a")).isCloseTo(90.0, within(0.5));
            assertThat(rightOf(session, "c")).isCloseTo(150.0, within(0.5));
        }
    }

    @Test
    void endArrangementPushesTheContentRight() {
        try (DocumentSession session = row(r -> r.gap(0).arrangement(RowArrangement.END)
                .addSpacer(s -> s.name("a").size(20, 10))
                .addSpacer(s -> s.name("c").size(20, 10)))) {
            assertThat(rightOf(session, "c")).isCloseTo(220.0, within(0.5));  // flush right
            assertThat(leftOf(session, "a")).isCloseTo(180.0, within(0.5));   // 220 - 40
        }
    }

    @Test
    void spaceAroundPutsEqualSpaceAroundEachChild() {
        // 3 x 20 = 60, leftover 140, gap 140/3 ≈ 46.67, leading half-gap ≈ 23.33
        // → a at 43.33, b centred at 110.
        try (DocumentSession session = row(r -> r.gap(0).arrangement(RowArrangement.SPACE_AROUND)
                .addSpacer(s -> s.name("a").size(20, 10))
                .addSpacer(s -> s.name("b").size(20, 10))
                .addSpacer(s -> s.name("c").size(20, 10)))) {
            assertThat(leftOf(session, "a")).isCloseTo(43.33, within(0.5));
            assertThat(leftOf(session, "b")).isCloseTo(110.0, within(0.5));
        }
    }

    @Test
    void spaceEvenlyPutsEqualGapsIncludingTheEdges() {
        // four equal 35pt gaps → a at 55, b at 110, c at 165.
        try (DocumentSession session = row(r -> r.gap(0).arrangement(RowArrangement.SPACE_EVENLY)
                .addSpacer(s -> s.name("a").size(20, 10))
                .addSpacer(s -> s.name("b").size(20, 10))
                .addSpacer(s -> s.name("c").size(20, 10)))) {
            assertThat(leftOf(session, "a")).isCloseTo(55.0, within(0.5));
            assertThat(leftOf(session, "c")).isCloseTo(165.0, within(0.5));
        }
    }

    @Test
    void growRejectsNonFiniteFactors() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new SpacerBuilder().grow(Double.NaN).build())
                .withMessageContaining("grow");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new SpacerBuilder().grow(Double.POSITIVE_INFINITY).build())
                .withMessageContaining("grow");
    }

    @Test
    void rowRejectsFlexCombinedWithWeights() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RowBuilder()
                        .arrangement(RowArrangement.CENTER)
                        .addSpacer(s -> s.size(10, 10))
                        .weights(1)
                        .build())
                .withMessageContaining("flex");
    }

    @Test
    void rowRejectsFlexCombinedWithColumns() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new RowBuilder()
                        .arrangement(RowArrangement.CENTER)
                        .addSpacer(s -> s.size(10, 10))
                        .columns(com.demcha.compose.document.style.DocumentRowColumn.auto())
                        .build())
                .withMessageContaining("flex");
    }

    @Test
    void rigidSpacerHasZeroGrowByDefault() {
        SpacerNode spacer = new SpacerNode("s", 10, 10, DocumentInsets.zero(), DocumentInsets.zero());
        assertThat(spacer.grow()).isZero();
    }
}
