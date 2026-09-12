package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * A contract probe: {@code horizontal(width)} is a width in points, and nothing
 * clips it to the space it was given.
 *
 * {@code LineBuilder horizontal(double width)} is the one call in the row
 * vocabulary whose argument has a plausible second reading. Every other number
 * in the neighbourhood is a proportion — {@code weights(0.34, 0.66)},
 * {@code PageBackgroundFill.leftColumn(0.34, ...)} — so {@code horizontal(100)}
 * reads as "the full width" to anyone who has just written those, and the
 * signature cannot say otherwise: a {@code double} named {@code width} is what a
 * ratio looks like too.
 *
 * It is points. A 100pt rule in a 67.85pt slot does not shrink, does not wrap and
 * does not warn — it is drawn at 100pt, straight across whatever is beside it, and
 * the document renders clean. That combination (a wrong number, no clipping, no
 * error) is why this is a test and not a sentence.
 *
 * The counterpart is asserted in the same file because the useful answer is not
 * "points" but "here is what you wanted instead": {@code fill()} stretches a
 * horizontal line to the slot it is placed in, so a divider meets the column edge
 * without anyone computing the width.
 *
 * Then the three ways that repair goes wrong, each measured rather than argued:
 * a flex row has no slot for {@code fill()} to fill, a flex row is entered by a
 * non-{@code START} arrangement just as much as by a spacer, and an aligned
 * heading in the {@code auto()} column silently leaves the rule nothing at all.
 *
 * <p><strong>Every number here is slot arithmetic.</strong> The left-hand child
 * is a fixed-width shape rather than a heading precisely so that it is: the
 * behaviour under test is how a row distributes width, which does not care
 * whether the child is text, and pinning it to a paragraph would make these
 * assertions depend on the default font's metrics. The one test that does use a
 * paragraph asserts only the right edge of the weight column, which the column
 * absorbs whatever the text measures.</p>
 */
class LineWidthUnitsContractTest {

    private static final double PAGE_WIDTH = 175.7;
    private static final float MARGIN = 20f;
    private static final double ROW_LEFT = MARGIN;
    /** Derived, not copied: a page-size change must move the expectations with it. */
    private static final double ROW_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final double ROW_RIGHT = ROW_LEFT + ROW_WIDTH;

    private static final double GAP = 6;
    private static final double HEAD_WIDTH = 40;

    /** Larger than a slot in a two-child 135.7pt row, and deliberately readable as "100%". */
    private static final double FIXED_RULE = 100;

    private static final String HEADING = "CORE COMPETENCIES";

    /** Width the row has left for its slots once the inter-child gaps are taken. */
    private static double available(int children) {
        return ROW_WIDTH - GAP * (children - 1);
    }

    private static LayoutGraph layout(DocumentSession session, Consumer<RowBuilder> row) {
        session.compose(dsl -> dsl.pageFlow(flow -> flow.addRow(row)));
        return session.layoutGraph();
    }

    private static PlacedNode node(LayoutGraph graph, String semanticName) {
        return graph.nodes().stream()
                .filter(n -> semanticName.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no node named " + semanticName));
    }

    private static double rightEdge(PlacedNode n) {
        return n.placementX() + n.placementWidth();
    }

    private static DocumentSession session() {
        return GraphCompose.document()
                .pageSize(PAGE_WIDTH, 400)
                .margin(MARGIN, MARGIN, MARGIN, MARGIN)
                .create();
    }

    @Test
    void horizontalIsPointsAndTheLineIsNotClippedToItsSlot() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .addParagraph(p -> p.name("head").text(HEADING))
                    .addLine(l -> l.name("rule").horizontal(FIXED_RULE)));

            PlacedNode rule = node(graph, "rule");

            assertThat(rule.placementWidth())
                    .describedAs("100 is 100 points — not a percentage of the row, and not reduced to the "
                            + "slot the even split handed it")
                    .isCloseTo(FIXED_RULE, within(0.01));

            // Two children and the row's default spacing of 0: each slot is half
            // the row, so the rule starts at the midpoint and ends a fixed
            // distance past the column. Nothing here depends on what the heading
            // measures.
            double slot = ROW_WIDTH / 2;
            assertThat(rightEdge(rule))
                    .describedAs("and nothing clips it: the rule is drawn past the right edge of the column "
                            + "it lives in, which is how a sidebar divider ends up crossing the page")
                    .isCloseTo(ROW_LEFT + slot + FIXED_RULE, within(0.01));
            assertThat(rightEdge(rule)).isGreaterThan(ROW_RIGHT);
        }
    }

    @Test
    void fillStretchesTheRuleToItsColumnInsteadOfAFixedWidth() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addParagraph(p -> p.name("head").text(HEADING))
                    .addLine(l -> l.name("rule").fill()));

            // The heading takes its natural width, the weight column takes the
            // remainder, and fill() spends exactly that remainder — so the rule
            // lands on the column edge without the author knowing where it is.
            // This is the one assertion that may not name a width: the split
            // between the two columns is a font metric, their sum is not.
            assertThat(rightEdge(node(graph, "rule")))
                    .describedAs("a fill() rule in a weight column ends on the column edge, whatever the "
                            + "heading beside it measures")
                    .isCloseTo(ROW_RIGHT, within(0.01));
        }
    }

    @Test
    void aGrowSpacerDoesNotRepairAFixedWidthRule() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .addShape(s -> s.name("head").size(HEAD_WIDTH, 10))
                    .flexSpacer()
                    .addLine(l -> l.name("rule").horizontal(FIXED_RULE)));

            PlacedNode rule = node(graph, "rule");

            // The width is the claim. Asserting only the right edge would let the
            // rule lose a third of its 100pt and still pass, which is exactly the
            // regression the sentence "a spacer moves it, it does not shrink it"
            // is there to catch.
            assertThat(rule.placementWidth())
                    .describedAs("a flex spacer moves the rule, it does not shrink it — the 100pt is intact")
                    .isCloseTo(FIXED_RULE, within(0.01));

            // Spacer collapses to zero because the children already overflow, so
            // the rule sits one gap past the head and the overflow survives.
            assertThat(rightEdge(rule))
                    .describedAs("and the overflow survives the repair")
                    .isCloseTo(ROW_LEFT + HEAD_WIDTH + 2 * GAP + FIXED_RULE, within(0.01));
            assertThat(rightEdge(rule)).isGreaterThan(ROW_RIGHT);
        }
    }

    @Test
    void fillOverflowsARowThatHasAGrowSpacer() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .addShape(s -> s.name("head").size(HEAD_WIDTH, 10))
                    .flexSpacer()
                    .addLine(l -> l.name("rule").fill()));

            PlacedNode rule = node(graph, "rule");

            // The trap: the fix for the fixed width and the fix for the position
            // do not compose. In the flex path a fill line reports the row's whole
            // available width as its natural width, and it is placed after the
            // head and the spacer — so it ends further out than the fixed rule it
            // replaced. Pinning the width, not just the direction, is what keeps a
            // later partial clamp from passing here.
            assertThat(rule.placementWidth())
                    .describedAs("fill() has no slot to fill in a flex row, so it takes the row's whole "
                            + "available width")
                    .isCloseTo(available(3), within(0.01));
            assertThat(rightEdge(rule))
                    .describedAs("and is then pushed past the edge by the children before it")
                    .isCloseTo(ROW_LEFT + HEAD_WIDTH + 2 * GAP + available(3), within(0.01));
            assertThat(rightEdge(rule)).isGreaterThan(ROW_RIGHT);
        }
    }

    @Test
    void aNonStartArrangementEntersTheSameFlexPathWithNoSpacerPresent() throws Exception {
        // The spacer is the obvious way into the flex path and not the only one:
        // RowSlots.hasFlexLayout answers yes to any non-START arrangement too. A
        // reader who never writes flexSpacer() but reaches for arrangement(...) to
        // push the rule right walks into the identical overflow.
        for (RowArrangement arrangement : RowArrangement.values()) {
            if (arrangement == RowArrangement.START) {
                continue;
            }
            try (DocumentSession session = session()) {
                LayoutGraph graph = layout(session, row -> row
                        .spacing(GAP)
                        .arrangement(arrangement)
                        .addShape(s -> s.name("head").size(HEAD_WIDTH, 10))
                        .addLine(l -> l.name("rule").fill()));

                PlacedNode rule = node(graph, "rule");
                assertThat(rule.placementWidth())
                        .describedAs("%s sizes the fill line to the row's whole available width, "
                                + "exactly as a grow spacer does", arrangement)
                        .isCloseTo(available(2), within(0.01));
                assertThat(rightEdge(rule))
                        .describedAs("%s therefore overflows the column with no spacer anywhere", arrangement)
                        .isGreaterThan(ROW_RIGHT);
            }
        }
    }

    @Test
    void theWeightColumnRepairIsRefusedOutrightInAFlexRow() throws Exception {
        // The recommended repair does not silently stop working in a flex row —
        // it is rejected at build time with a message naming both strategies.
        // Worth pinning: "throws" and "silently misplaces" are very different
        // things to tell a reader, and only one of them is true.
        try (DocumentSession session = session()) {
            assertThatThrownBy(() -> layout(session, row -> row
                    .spacing(GAP)
                    .arrangement(RowArrangement.SPACE_BETWEEN)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addParagraph(p -> p.name("head").text(HEADING))
                    .addLine(l -> l.name("rule").fill())))
                    .describedAs("a weight column and a flex row are two distribution strategies, "
                            + "and the row refuses to guess between them")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot combine flex")
                    .hasMessageContaining("weights or columns");
        }
    }

    @Test
    void anAlignedHeadingLeavesTheWeightColumnNothingToFill() throws Exception {
        // The recommended recipe's own failure mode, and the reason the route
        // carries a constraint for it. A right- or centre-aligned paragraph claims
        // the full row width; an auto() column grants it, because fixed+auto still
        // fits; and the weight column is left zero. The rule does not overflow and
        // does not throw — it disappears.
        for (TextAlign align : new TextAlign[]{TextAlign.CENTER, TextAlign.RIGHT}) {
            try (DocumentSession session = session()) {
                LayoutGraph graph = layout(session, row -> row
                        .spacing(GAP)
                        .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.name("head").text(HEADING).align(align))
                        .addLine(l -> l.name("rule").fill()));

                assertThat(node(graph, "head").placementWidth())
                        .describedAs("an %s-aligned paragraph claims the whole row, so the auto column is "
                                + "the whole row", align)
                        .isCloseTo(available(2), within(0.01));
                assertThat(node(graph, "rule").placementWidth())
                        .describedAs("which leaves the weight column zero: with align(%s) the rule is not "
                                + "misplaced, it is not drawn at all", align)
                        .isCloseTo(0.0, within(0.01));
            }
        }
    }

    @Test
    void theSameRecipeWithAnUnalignedHeadingStillReachesTheEdge() throws Exception {
        // The boundary, so the test above cannot pass by the recipe being broken
        // for everyone: left-aligned, the rule is a real width ending on the edge.
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addParagraph(p -> p.name("head").text(HEADING).align(TextAlign.LEFT))
                    .addLine(l -> l.name("rule").fill()));

            assertThat(node(graph, "rule").placementWidth())
                    .describedAs("without the alignment the weight column has a remainder to give")
                    .isGreaterThan(0.0);
            assertThat(rightEdge(node(graph, "rule")))
                    .describedAs("and the rule still ends on the column edge")
                    .isCloseTo(ROW_RIGHT, within(0.01));
        }
    }
}
