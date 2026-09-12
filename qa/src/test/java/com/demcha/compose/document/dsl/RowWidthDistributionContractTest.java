package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A contract probe: what a row does with its width when nobody tells it.
 *
 * A row with no {@code columns(...)}, no {@code weights(...)}, no grow spacer and
 * the default {@code START} arrangement splits its inner width into equal shares
 * — one per child, content playing no part. That is the whole rule, and nothing
 * in {@code addRow(...)} hints at it: the natural reading of "put a 13pt icon
 * next to a paragraph" is that the icon takes 13pt, and instead it takes half the
 * row while the text is squeezed into the other half.
 *
 * The failure mode is what makes this worth pinning. Nothing throws and nothing
 * looks wrong in the builder — the document renders, the icon is the right size,
 * and only the paragraph's line count says anything happened. A reader comparing
 * against a design sees "the text wraps too much" and goes looking at fonts.
 *
 * Three forms are asserted together rather than one, because the interesting
 * fact is the difference between them. The even split is the default; an
 * {@code auto()} + {@code weight(1)} column pair is the fix; and a grow spacer
 * reaches the same widths by a different route — it switches the row to
 * intrinsic sizing, which is why the same row laid out twice, once with a
 * trailing {@code flexSpacer()} and once without, does not agree.
 *
 * Every number asserted here is slot arithmetic over the row's inner width, the
 * gap and the child count — never a font metric. That rules two things out on
 * purpose. The paragraph's height is font-dependent, so it is asserted as a
 * comparison between two forms rather than as a number. And under a non-START
 * arrangement the children's x coordinates are font-dependent too, because
 * {@code RowSlots.flexJustify} spreads the leftover — the row minus the
 * paragraph's measured width — as a leading offset; there only the widths are
 * asserted.
 */
class RowWidthDistributionContractTest {

    /** Page and margins chosen so the row's inner width is exactly 135.7pt — a CV sidebar column. */
    private static final double PAGE_WIDTH = 175.7;
    private static final float MARGIN = 20f;
    private static final double ROW_LEFT = MARGIN;
    /** Derived, not copied: a page-size change must move the expectations with it. */
    private static final double ROW_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final double GAP = 8;
    private static final double ICON = 13;

    private static final String TEXT = "Delivered 120+ events annually with 98% client satisfaction.";

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

    private static DocumentSession session() {
        return GraphCompose.document()
                .pageSize(PAGE_WIDTH, 400)
                .margin(MARGIN, MARGIN, MARGIN, MARGIN)
                .create();
    }

    @Test
    void aRowWithNoColumnSpecSplitsItsWidthEvenlyRegardlessOfContent() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT)));

            // (135.7 - 8) / 2 = 63.85 per slot. The icon draws at its own 13pt,
            // but it *occupies* 63.85 — so the text starts at 20 + 63.85 + 8.
            double share = (ROW_WIDTH - GAP) / 2;
            assertThat(node(graph, "text").placementX())
                    .describedAs(
                            "a 13pt icon and a paragraph each get half the row: the paragraph is pushed to "
                                    + "the midpoint even though the icon needs 13pt of the 63.85pt it holds")
                    .isCloseTo(ROW_LEFT + share + GAP, within(0.01));

            assertThat(node(graph, "icon").placementWidth())
                    .describedAs("the icon still draws at its own size — only its slot is oversized, "
                            + "which is why nothing looks wrong until the text is read")
                    .isCloseTo(ICON, within(0.01));
        }
    }

    @Test
    void anAutoColumnGivesTheIconItsWidthAndTheWeightColumnTakesTheRest() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT)));

            assertThat(node(graph, "text").placementX())
                    .describedAs("the auto column is the icon's own 13pt, so the text starts one gap after it")
                    .isCloseTo(ROW_LEFT + ICON + GAP, within(0.01));
        }
    }

    @Test
    void theEvenSplitCostsTheParagraphItsLineCount() throws Exception {
        try (DocumentSession even = session(); DocumentSession sized = session()) {
            double evenHeight = node(layout(even, row -> row
                    .spacing(GAP)
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT))), "text").placementHeight();

            double sizedHeight = node(layout(sized, row -> row
                    .spacing(GAP)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT))), "text").placementHeight();

            // The whole visible symptom, in one assertion. Compared rather than
            // pinned to a number: the line *count* is the contract, and the
            // height of a line is a font metric this test has no business fixing.
            assertThat(evenHeight)
                    .describedAs(
                            "the even split is not cosmetic — the same paragraph in the same row needs "
                                    + "strictly more vertical space, which is how this defect is first noticed")
                    .isGreaterThan(sizedHeight);
        }
    }

    @Test
    void theRuleAndTheRepairHoldForARealSvgIconNode() throws Exception {
        // The three tests above use a ShapeNode as the fixed-size child, which
        // keeps them independent of the SVG reader. The reported defect was an
        // SvgIcon, and icon.node(13) is a LayerStackNode — a different branch of
        // the row's allowed-child list and a different intrinsic measurement. If
        // an auto() column could not measure a layer stack, the recommendation
        // would be right about rows and wrong about icons, so it is asserted on
        // the type the route actually sends people to.
        SvgIcon icon = SvgIcon.parse(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                        + "<path d=\"M2 2 L22 2 L22 22 L2 22 Z\" fill=\"#000000\"/></svg>");

        try (DocumentSession even = session(); DocumentSession sized = session()) {
            double share = (ROW_WIDTH - GAP) / 2;
            assertThat(node(layout(even, row -> row
                    .spacing(GAP)
                    .add(icon.node(ICON))
                    .addParagraph(p -> p.name("text").text(TEXT))), "text").placementX())
                    .describedAs("a real icon node is no exception: with no column spec it holds half the row")
                    .isCloseTo(ROW_LEFT + share + GAP, within(0.01));

            assertThat(node(layout(sized, row -> row
                    .spacing(GAP)
                    .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                    .add(icon.node(ICON))
                    .addParagraph(p -> p.name("text").text(TEXT))), "text").placementX())
                    .describedAs("and the auto column measures the layer stack at its own 13pt, "
                            + "so the route's recommendation holds for the icon it was written for")
                    .isCloseTo(ROW_LEFT + ICON + GAP, within(0.01));
        }
    }

    @Test
    void aGrowSpacerSizesEveryOtherChildToItsContent() throws Exception {
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT))
                    .flexSpacer());

            // The same row as the first test with one more child, and the answer
            // changes completely: a grow spacer routes the row through the flex
            // path, where every non-grow child takes its natural width instead of
            // an equal share. This is why two rows that differ only by a trailing
            // flexSpacer() do not lay out alike.
            assertThat(node(graph, "text").placementX())
                    .describedAs("with a grow spacer present the icon is sized to its content, "
                            + "not to a third of the row — the text starts one gap after 13pt")
                    .isCloseTo(ROW_LEFT + ICON + GAP, within(0.01));
        }
    }

    @Test
    void aNonStartArrangementSizesThemTheSameWayWithNoSpacerPresent() throws Exception {
        // A grow spacer is the obvious way into the flex path and not the only
        // one: RowSlots.hasFlexLayout answers yes to any non-START arrangement
        // too. Pinned for every value, because a claim that named only the
        // spacer would send a reader who reaches for arrangement(...) back to
        // the even split they were trying to escape — or, worse, let them think
        // the even-split rule still applies when it no longer does.
        for (RowArrangement arrangement : RowArrangement.values()) {
            if (arrangement == RowArrangement.START) {
                continue;
            }
            try (DocumentSession session = session()) {
                LayoutGraph graph = layout(session, row -> row
                        .spacing(GAP)
                        .arrangement(arrangement)
                        .addShape(s -> s.name("icon").size(ICON, ICON))
                        .addParagraph(p -> p.name("text").text(TEXT)));

                // The icon's *width* is the contract and is arithmetic: in the
                // flex path a non-grow child is its own content, whatever that
                // content is. Its x is not asserted here, and neither is the
                // text's: a non-START arrangement distributes the leftover as a
                // leading offset (RowSlots.flexJustify), and the leftover is the
                // row minus the paragraph's measured width — a font metric. The
                // earlier version of this loop pinned that x and passed only
                // because this particular string overflows the row and clamps
                // the leftover to zero; a shorter string moved it by 10-20pt.
                assertThat(node(graph, "icon").placementWidth())
                        .describedAs("%s sizes the icon to its content, not to half the row", arrangement)
                        .isCloseTo(ICON, within(0.01));
                assertThat(node(graph, "icon").placementWidth())
                        .describedAs("%s is not the even split — that would hand the icon %.2fpt",
                                arrangement, (ROW_WIDTH - GAP) / 2)
                        .isLessThan((ROW_WIDTH - GAP) / 2);
            }
        }
    }

    @Test
    void theEvenSplitIsWhatTheStartArrangementDoes() throws Exception {
        // The boundary for the loop above: START is the default and is the one
        // arrangement that does NOT take the flex path, so the icon is back to
        // holding half the row. Without this, the loop would pass just as well
        // if the even split had been removed altogether.
        try (DocumentSession session = session()) {
            LayoutGraph graph = layout(session, row -> row
                    .spacing(GAP)
                    .arrangement(RowArrangement.START)
                    .addShape(s -> s.name("icon").size(ICON, ICON))
                    .addParagraph(p -> p.name("text").text(TEXT)));

            assertThat(node(graph, "text").placementX())
                    .describedAs("START keeps the even split — the icon still holds half the row")
                    .isCloseTo(ROW_LEFT + (ROW_WIDTH - GAP) / 2 + GAP, within(0.01));
        }
    }
}
