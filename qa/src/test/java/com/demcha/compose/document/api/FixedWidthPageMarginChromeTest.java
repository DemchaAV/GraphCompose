package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What a decorated block's background does when {@code pageMargins(...)} changes
 * the content column under it.
 *
 * <p><strong>The rule the engine holds:</strong> a block is placed once, and its
 * band is that block's own box on every page it spans. Only the band's vertical
 * edges follow each page's margins; the horizontal ones do not move, because the
 * block itself does not.</p>
 *
 * <p><strong>The gap that leaves:</strong> the compiler re-seats a <em>direct
 * child of the root</em> into the column of the page that child starts on
 * ({@code boolean pageColumn = depth == 1 && state.hasPageGeometry()} in
 * {@code LayoutCompiler}), while the root around it keeps one box. A decorated
 * root can therefore have a child sitting outside its background. That is a
 * compiler-level inconsistency — a parent spanning two columns has no single box
 * to be — and it is not something the decoration band can resolve: a band that
 * chased the painted page's column instead abandons the text of any block whose
 * content merely <em>flows</em> across the boundary, which is the same defect one
 * level down.</p>
 *
 * <p><strong>What it takes to see it:</strong> both conjuncts, plus a root narrow
 * enough to be the binding constraint. Per-page margins must exist at all, the
 * overhanging node must be a direct child of the root, and the root's fixed width
 * must be narrower than the later page's column — at
 * {@code fixedWidth(340)} the root's box already covers page 2's column and the
 * child lands inside it. Both cases are pinned below, with their measured
 * numbers, so the boundary is on record and not just the failure.</p>
 *
 * <p>Page 1 keeps a 20pt margin and page 2 onwards takes 80pt, so page 1 offers a
 * 360pt column (20..380) and page 2 offers 240pt (80..320).</p>
 */
class FixedWidthPageMarginChromeTest {

    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 300;
    private static final double WIDE_MARGIN = 80;

    // --- the rule: a band is its own block's box on every page ---------------

    @Test
    void aBandKeepsItsBlocksBoxOnEveryPageItSpans() {
        try (DocumentSession document = document(true)) {
            document.pageFlow(page -> page.addSection(band -> band
                    .name("Band")
                    .fillColor(DocumentColor.rgb(220, 230, 240))
                    .addParagraph(p -> p.name("Body").text(longBody()).align(TextAlign.CENTER))));

            PlacedNode band = node(document, "Band");
            assertThat(band.endPage()).as("the band must span the margin change").isGreaterThan(0);

            for (int page = band.startPage(); page <= band.endPage(); page++) {
                PlacedFragment fill = fillOnPage(document, band, page);
                assertThat(fill.x()).as("page " + page + " x").isCloseTo(band.placementX(), within(0.01));
                assertThat(fill.width()).as("page " + page + " width")
                        .isCloseTo(band.placementWidth(), within(0.01));
            }
        }
    }

    @Test
    void aFlowingBlocksTextStaysInsideItsOwnBandOnEveryPage() {
        try (DocumentSession document = document(true)) {
            document.pageFlow(page -> page.addSection(band -> band
                    .name("Band")
                    .fillColor(DocumentColor.rgb(220, 230, 240))
                    // Padding is what makes this test say something the box test
                    // does not: without it the content's box IS the band's box and
                    // the assertion below is an identity. With it the content is
                    // inset 10pt on each side and genuinely measured against the
                    // paint.
                    .padding(DocumentInsets.of(10))
                    .addParagraph(p -> p.name("Body").text(longBody()).align(TextAlign.CENTER))));

            PlacedNode band = node(document, "Band");
            PlacedNode body = node(document, "Body");
            assertThat(band.endPage()).as("the band must span the margin change").isGreaterThan(0);
            assertThat(body.placementWidth())
                    .as("the content must be narrower than the band, or this proves nothing")
                    .isLessThan(band.placementWidth() - 1.0);

            // The content is placed once, so it must sit inside the band as PAINTED
            // on each page — not merely inside the band's own record, which would
            // hold whatever the band did. A band that followed the painted page's
            // column would narrow to 240pt on page 2 and leave this 340pt-wide
            // text hanging outside it.
            for (int page = band.startPage(); page <= band.endPage(); page++) {
                PlacedFragment fill = fillOnPage(document, band, page);
                assertThat(body.placementX())
                        .as("page " + page + " left")
                        .isGreaterThanOrEqualTo(fill.x() - 0.01);
                assertThat(body.placementX() + body.placementWidth())
                        .as("page " + page + " right")
                        .isLessThanOrEqualTo(fill.x() + fill.width() + 0.01);
            }
        }
    }

    @Test
    void theBandsVerticalEdgesStillFollowEachPagesMargins() {
        try (DocumentSession document = document(true)) {
            document.pageFlow(page -> page.addSection(band -> band
                    .name("Band")
                    .fillColor(DocumentColor.rgb(220, 230, 240))
                    // Long enough for a MIDDLE page. On the band's last page the
                    // bottom edge is the content's own, so the per-page clamp never
                    // binds there and an assertion on it passes with 88pt of slack
                    // however the clamp is written. Only a page the band crosses
                    // entirely exercises both edges.
                    .addParagraph(longBody().repeat(3))));

            PlacedNode band = node(document, "Band");
            assertThat(band.endPage()).as("the band needs a page it crosses entirely").isGreaterThan(1);

            // The horizontal axis is frozen to the block's box; the vertical one is
            // not, and clamps to the page being painted.
            for (int page = band.startPage() + 1; page < band.endPage(); page++) {
                PlacedFragment middle = fillOnPage(document, band, page);
                assertThat(middle.y() + middle.height())
                        .as("page " + page + " top")
                        .isCloseTo(PAGE_HEIGHT - WIDE_MARGIN, within(0.01));
                assertThat(middle.y())
                        .as("page " + page + " bottom")
                        .isCloseTo(WIDE_MARGIN, within(0.01));
            }
        }
    }

    @Test
    void declaringPageMarginsDoesNotMoveABandHorizontally() {
        double[] withRule = bandBox(true);
        double[] withoutRule = bandBox(false);

        // Whatever the rules say, the horizontal band geometry is the block's own.
        assertThat(withRule[0]).isCloseTo(withoutRule[0], within(0.01));
        assertThat(withRule[1]).isCloseTo(withoutRule[1], within(0.01));
    }

    // --- the gap: a re-seated child can leave its root's band ----------------

    @Test
    void aReSeatedChildCanSitOutsideItsDecoratedRootsBand() {
        try (DocumentSession document = document(true)) {
            compose(document, page -> page.fixedWidth(200));

            // `PageTwo` is the direct child of the root — the node the compiler
            // actually re-seats. `Body` is one level further down and merely
            // inherits the region, so asserting on it would name the wrong
            // mechanism.
            PlacedNode reSeated = node(document, "PageTwo");
            PlacedFragment fill = fillOnPage(document, node(document, "Flow"), 1);

            assertThat(reSeated.startPage()).as("the child must start on page 2").isEqualTo(1);

            // Documented gap, not an aspiration, and the numbers are the record:
            // the root keeps one box while its child is re-seated into page 2's
            // column, so the child ends up beside its own background. When the
            // seating rule is fixed this test should fail — and the fix belongs in
            // LayoutCompiler, not in the decoration band.
            assertThat(fill.x()).as("band left").isCloseTo(20.0, within(0.01));
            assertThat(fill.width()).as("band width").isCloseTo(200.0, within(0.01));
            assertThat(reSeated.placementX()).as("child left").isCloseTo(80.0, within(0.01));
            assertThat(reSeated.placementWidth()).as("child width").isCloseTo(200.0, within(0.01));
            assertThat(reSeated.placementX() + reSeated.placementWidth() - (fill.x() + fill.width()))
                    .as("the child overhangs its root's band on the right")
                    .isCloseTo(60.0, within(0.01));
        }
    }

    @Test
    void aRootWideEnoughToCoverTheNarrowerPageHasNoOverhang() {
        try (DocumentSession document = document(true)) {
            compose(document, page -> page.fixedWidth(340));

            PlacedNode reSeated = node(document, "PageTwo");
            PlacedFragment fill = fillOnPage(document, node(document, "Flow"), 1);

            assertThat(reSeated.startPage()).as("the child must start on page 2").isEqualTo(1);

            // The other side of the boundary. The same re-seating happens — the
            // child is still placed against page 2's column at x=80 — but the root's
            // box (20..360) already covers that column, and the child is capped to
            // it (240pt), so it lands inside its own background. The gap needs a
            // root NARROWER than the later page's column; the seating rule is
            // inconsistent either way.
            assertThat(fill.x()).as("band left").isCloseTo(20.0, within(0.01));
            assertThat(fill.width()).as("band width").isCloseTo(340.0, within(0.01));
            assertThat(reSeated.placementX()).as("child left").isCloseTo(80.0, within(0.01));
            assertThat(reSeated.placementWidth()).as("child width").isCloseTo(240.0, within(0.01));
            assertThat(reSeated.placementX() + reSeated.placementWidth())
                    .as("the child stays inside the band")
                    .isLessThanOrEqualTo(fill.x() + fill.width() + 0.01);
        }
    }

    @Test
    void decoratingASectionInsideTheRootAvoidsTheOverhang() {
        try (DocumentSession document = document(true)) {
            // The remedy the recipe prescribes, held to the same numbers as the gap
            // above: keep the fixed width on the root, move the background off it and
            // onto a section inside. That section is re-seated as one box together
            // with the content it decorates, so the band and its children agree —
            // which is exactly what a decorated ROOT cannot do.
            document.pageFlow(page -> page
                    .name("Flow")
                    .fixedWidth(200)
                    .addSection(a -> a.name("PageOne").addParagraph("First page."))
                    .addPageBreak(br -> br.name("ToPageTwo"))
                    .addSection(b -> b
                            .name("Card")
                            .fillColor(DocumentColor.rgb(220, 230, 240))
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER))));

            PlacedNode card = node(document, "Card");
            PlacedNode body = node(document, "Body");
            PlacedFragment fill = fillOnPage(document, card, 1);

            assertThat(card.startPage()).as("the card must start on page 2").isEqualTo(1);
            assertThat(fill.x()).as("band left").isCloseTo(card.placementX(), within(0.01));
            assertThat(fill.width()).as("band width").isCloseTo(card.placementWidth(), within(0.01));
            assertThat(body.placementX()).as("content left").isGreaterThanOrEqualTo(fill.x() - 0.01);
            assertThat(body.placementX() + body.placementWidth())
                    .as("content stays inside its own background")
                    .isLessThanOrEqualTo(fill.x() + fill.width() + 0.01);
        }
    }

    // --- bleed ---------------------------------------------------------------

    @Test
    void bleedStillReachesThePageEdgeOnEveryPageItSpans() {
        try (DocumentSession document = document(true)) {
            document.pageFlow(page -> page.addSection(band -> band
                    .name("Band")
                    .fillColor(DocumentColor.rgb(220, 230, 240))
                    .bleedToEdge(DocumentEdge.LEFT, DocumentEdge.RIGHT)
                    .addParagraph(longBody())));

            PlacedNode band = node(document, "Band");
            assertThat(band.endPage()).as("the band must span the margin change").isGreaterThan(0);

            for (int page = band.startPage(); page <= band.endPage(); page++) {
                PlacedFragment fill = fillOnPage(document, band, page);
                assertThat(fill.x()).as("page " + page + " left").isCloseTo(0.0, within(0.01));
                assertThat(fill.width()).as("page " + page + " width").isCloseTo(PAGE_WIDTH, within(0.01));
            }
        }
    }

    // --- helpers ------------------------------------------------------------

    private static String longBody() {
        return ("A band long enough to run from the first page onto the second, where "
                + "the margins are wider. ").repeat(14);
    }

    /** `{x, width}` of the band's fill on its second page, with or without page-margin rules. */
    private static double[] bandBox(boolean declarePageMargins) {
        try (DocumentSession document = document(declarePageMargins)) {
            document.pageFlow(page -> page.addSection(band -> band
                    .name("Band")
                    .fillColor(DocumentColor.rgb(220, 230, 240))
                    .addParagraph(longBody())));
            PlacedFragment fill = fillOnPage(document, node(document, "Band"), 1);
            return new double[] {fill.x(), fill.width()};
        }
    }

    private static DocumentSession document(boolean widenFromPageTwo) {
        DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create();
        if (widenFromPageTwo) {
            session.pageMargins(List.of(PageMarginRule.from(2, DocumentInsets.of(WIDE_MARGIN))));
        }
        return session;
    }

    /** A filled root flow, a page break, then a centred paragraph on page 2. */
    private static void compose(DocumentSession document, Consumer<PageFlowBuilder> shape) {
        document.pageFlow(page -> {
            page.name("Flow").fillColor(DocumentColor.rgb(220, 230, 240));
            shape.accept(page);
            page.addSection(a -> a.name("PageOne").addParagraph("First page."))
                    .addPageBreak(br -> br.name("ToPageTwo"))
                    .addSection(b -> b.name("PageTwo")
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER)));
        });
    }

    /**
     * The decoration band {@code owner} paints on {@code pageIndex}. Keyed on the
     * owner's path: several nodes paint a shape on one page, and taking whichever
     * comes first would let a test pass by measuring somebody else's background.
     */
    private static PlacedFragment fillOnPage(DocumentSession document, PlacedNode owner, int pageIndex) {
        LayoutGraph graph = document.layoutGraph();
        List<PlacedFragment> fills = graph.fragments().stream()
                .filter(f -> f.pageIndex() == pageIndex
                        && owner.path().equals(f.path())
                        && f.payload() instanceof ShapeFragmentPayload)
                .toList();
        assertThat(fills)
                .as("fills painted by '" + owner.semanticName() + "' on page " + pageIndex)
                .hasSize(1);
        return fills.get(0);
    }

    private static PlacedNode node(DocumentSession document, String semanticName) {
        return document.layoutGraph().nodes().stream()
                .filter(n -> semanticName.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no node named '" + semanticName + "'"));
    }
}
