package com.demcha.compose.document.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

/**
 * A row child that carries a horizontal margin is measured and placed at one
 * and the same width.
 *
 * <p>A row band is sized in two passes over the same children.
 * {@code NodeDefinitionSupport.measureRow} prepares each child inside its slot
 * minus that child's own margin and takes the tallest result as the band
 * height; {@code LayoutCompiler} then prepares the child a second time to place
 * it. Both passes must start from the same base, or the band is sized against
 * one wrap and drawn from another.</p>
 *
 * <p>The placement pass used to hand {@code prepareForRegionWidth} a width the
 * child's margin had already been taken out of, and that helper subtracts the
 * margin itself — so placement ran at {@code slot - 2 * margin} against the
 * measure pass's {@code slot - margin}. The child wrapped into more lines than
 * the band was measured for, and the row band's own guard rejected the layout:
 * <em>child 'SectionNode' measured height … exceeds row inner height</em>.</p>
 *
 * <p>Two of the three symptoms never threw. The fixed-slot band — a row nested
 * in a LayerStack layer or a composed table cell — carries no such guard, so
 * there the child simply spilled through the top of its band; and a composite
 * row child reported one width while its own children were laid out in another.
 * The assertions below read the width base back out directly rather than
 * waiting for an exception, so they cover the silent cases too.</p>
 */
class RowChildMarginWidthTest {

    /** Page 400 wide with 20pt page margins: a 360pt content region. */
    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 300;
    private static final double PAGE_MARGIN = 20;

    /** Two children, no gap, no weights: each slot is half the region. */
    private static final double SLOT_WIDTH = (PAGE_WIDTH - 2 * PAGE_MARGIN) / 2;
    private static final double CHILD_MARGIN = 20;
    private static final double EXPECTED_CHILD_WIDTH = SLOT_WIDTH - 2 * CHILD_MARGIN;

    private static final double TOLERANCE = 0.01;

    /**
     * A paragraph that reports the width it was measured at. A LEFT-aligned
     * paragraph shrink-wraps to its longest line, which would hide the base
     * behind the text metrics; CENTER resolves to the full available width, so
     * the placed width is the measurement base itself.
     */
    private static ParagraphNode fillingParagraph(String name, DocumentInsets margin) {
        return fillingParagraph(name, "Card body", margin);
    }

    private static ParagraphNode fillingParagraph(String name, String text, DocumentInsets margin) {
        return new ParagraphNode(name, text, DocumentTextStyle.DEFAULT, TextAlign.CENTER, 0.0,
                DocumentInsets.zero(), margin);
    }

    /** A row whose first child carries a 20pt margin on each side. */
    private static RowNode rowWithAMarginCarryingChild(String name) {
        return new RowNode(name,
                List.of(fillingParagraph("Card", new DocumentInsets(0, CHILD_MARGIN, 0, CHILD_MARGIN)),
                        fillingParagraph("Filler", DocumentInsets.zero())),
                List.of(), 0.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO);
    }

    private static double placedWidthOf(DocumentNode root, String semanticName) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(PAGE_MARGIN))
                .create()) {
            session.add(root);
            return placedNode(session.layoutGraph(), semanticName).placementWidth();
        }
    }

    private static PlacedNode placedNode(LayoutGraph graph, String semanticName) {
        return graph.nodes().stream()
                .filter(node -> semanticName.equals(node.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(semanticName + " was not placed"));
    }

    /** Top edge of a placed node in PDF coordinates, where y grows upward. */
    private static double topOf(PlacedNode node) {
        return node.placementY() + node.placementHeight();
    }

    @Test
    void aRowChildWithAHorizontalMarginIsPlacedAtItsSlotMinusThatMarginOnce() throws Exception {
        assertThat(placedWidthOf(rowWithAMarginCarryingChild("Band"), "Card"))
                .describedAs("a %spt slot less one %spt horizontal margin", SLOT_WIDTH, 2 * CHILD_MARGIN)
                .isCloseTo(EXPECTED_CHILD_WIDTH, within(TOLERANCE));
    }

    @Test
    void aRowChildInsideAFixedRectangleUsesTheSameBase() throws Exception {
        // The fixed-slot row band (a LayerStack layer, a composed table cell) is a
        // second copy of the same seating loop, and carried the same double
        // subtraction. A row fills its region and a stack shrink-wraps to its
        // widest layer, so the nested row band is the same 360pt wide.
        DocumentNode stack = new LayerStackNode(
                "Stack",
                List.of(new LayerStackNode.Layer(rowWithAMarginCarryingChild("LayerBand"))),
                DocumentInsets.zero(), DocumentInsets.zero());

        assertThat(placedWidthOf(stack, "Card"))
                .describedAs("a row band in a fixed rectangle measures its children like a page-level row")
                .isCloseTo(EXPECTED_CHILD_WIDTH, within(TOLERANCE));
    }

    @Test
    void aCompositeRowChildAgreesWithTheRegionItsOwnChildrenAreLaidOutIn() throws Exception {
        // A composite row child is seated by compileNodeInFixedSlot, which derives
        // the region for its children from the whole slot on its own. So the
        // composite used to disagree with itself: its own reported width came from
        // the double-subtracted prepare (slot - 2 * margin) while its children were
        // laid out in slot - margin. Reading the section and the paragraph inside it
        // is what exposes that — the child alone was always right.
        DocumentNode row = new RowNode("Band",
                List.of(new SectionNode("Card",
                                List.of(fillingParagraph("Body", DocumentInsets.zero())),
                                0.0, DocumentInsets.zero(),
                                new DocumentInsets(0, CHILD_MARGIN, 0, CHILD_MARGIN),
                                null, null, DocumentCornerRadius.ZERO, DocumentBorders.NONE, false),
                        fillingParagraph("Filler", DocumentInsets.zero())),
                List.of(), 0.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(PAGE_MARGIN))
                .create()) {
            session.add(row);
            LayoutGraph graph = session.layoutGraph();
            double cardWidth = placedNode(graph, "Card").placementWidth();

            assertThat(cardWidth)
                    .describedAs("the composite row child takes its slot less one margin")
                    .isCloseTo(EXPECTED_CHILD_WIDTH, within(TOLERANCE));
            assertThat(cardWidth)
                    .describedAs("the composite's own width matches the region its children were given")
                    .isCloseTo(placedNode(graph, "Body").placementWidth(), within(TOLERANCE));
        }
    }

    @Test
    void aMarginCarryingChildStaysInsideAFixedSlotBandThatHasNoGuard() throws Exception {
        // The page-level row band throws when a child outgrows it. The fixed-slot
        // band has no such guard, so there the same disagreement was silent: the
        // tallest child, measured wider than it was placed, came out taller than
        // the band sized for it, the cross-axis slack went negative, and a
        // non-TOP alignment pushed the child up and out through the band's top.
        RowNode band = new RowNode("LayerBand",
                List.of(fillingParagraph("Card",
                                "A fixed width pins the horizontal axis and leaves the height to the content.",
                                new DocumentInsets(0, CHILD_MARGIN, 0, CHILD_MARGIN)),
                        fillingParagraph("Filler", DocumentInsets.zero())),
                List.of(), 0.0,
                DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, DocumentCornerRadius.ZERO, DocumentBorders.NONE, List.of(),
                RowVerticalAlign.CENTER);
        DocumentNode stack = new LayerStackNode(
                "Stack",
                List.of(new LayerStackNode.Layer(band)),
                DocumentInsets.zero(), DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(PAGE_MARGIN))
                .create()) {
            session.add(stack);
            LayoutGraph graph = session.layoutGraph();

            assertThat(topOf(placedNode(graph, "Card")))
                    .describedAs("the tallest row child must not be seated above the band that holds it")
                    .isLessThanOrEqualTo(topOf(placedNode(graph, "Stack")) + TOLERANCE);
        }
    }

    @Test
    void aMarginCarryingSectionDoesNotOutgrowTheBandItWasMeasuredFor() {
        // The reported repro. The section is measured at slot - margin, wraps to
        // the band height that produced, and was then placed at slot - 2 * margin —
        // where the same text needs one line more than the band has room for, and
        // the row band's own guard throws.
        assertThatCode(() -> {
            try (DocumentSession session = GraphCompose.document()
                    .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                    .margin(DocumentInsets.of(PAGE_MARGIN))
                    .create()) {
                session.pageFlow(page -> page.addRow(row -> row
                        .name("Band")
                        .addSection(s -> s
                                .name("Card")
                                .margin(new DocumentInsets(0, CHILD_MARGIN, 0, CHILD_MARGIN))
                                .addSection(inner -> inner.name("Body").addParagraph(
                                        "A fixed width pins the horizontal axis and leaves the height to the content.")))
                        .addSection(s -> s.name("Filler").addParagraph("filler"))));
                session.layoutGraph();
            }
        })
                .describedAs("a row child's wrap must not disagree with the band measured for it")
                .doesNotThrowAnyException();
    }
}
