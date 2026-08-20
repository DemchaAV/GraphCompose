package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code pageMargins(...)} overrides the page margin for ranges of pages, changing
 * the content box (both horizontally and vertically) per page. Asserted against the
 * placed layout graph, so both the horizontal origin (left margin) and the vertical
 * origin (top margin) are checked directly.
 */
class PageMarginTest {

    @TempDir
    Path tempDir;

    private static PlacedNode node(LayoutGraph graph, String semanticName) {
        return graph.nodes().stream()
                .filter(n -> semanticName.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no placed node named " + semanticName));
    }

    /** Top edge of a placed node in page coordinates (origin bottom-left). */
    private static double topEdge(PlacedNode n) {
        return n.placementY() + n.placementHeight();
    }

    @Test
    void perPageMarginShiftsTheContentBoxHorizontallyAndVertically() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.symmetric(15, 10)),   // wide cover
                    PageMarginRule.from(2, DocumentInsets.symmetric(40, 70))));  // narrow body
            session.pageFlow()
                    .addParagraph(p -> p.name("cover").text("Cover"))
                    .addPageBreak(b -> { })
                    .addParagraph(p -> p.name("body").text("Body"))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            PlacedNode cover = node(graph, "cover");
            PlacedNode body = node(graph, "body");

            // Page 1 (cover rule): left margin 10, top margin 15.
            assertThat(cover.startPage()).isZero();
            assertThat(cover.placementX()).isCloseTo(10.0, within(0.5));
            assertThat(topEdge(cover)).isCloseTo(300.0 - 15.0, within(0.5));

            // Page 2 (body rule): left margin 70, top margin 40.
            assertThat(body.startPage()).isEqualTo(1);
            assertThat(body.placementX()).isCloseTo(70.0, within(0.5));
            assertThat(topEdge(body)).isCloseTo(300.0 - 40.0, within(0.5));
        }
    }

    @Test
    void naturalOverflowResolvesEachBlockAtItsStartPageWidth() throws Exception {
        // Same vertical margin on both pages (so pagination is width-independent),
        // different horizontal margin — the fixed point must measure each block at
        // the width of the page it naturally flows onto.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.symmetric(20, 10)),
                    PageMarginRule.from(2, DocumentInsets.symmetric(20, 70))));
            var flow = session.pageFlow();
            for (int i = 0; i < 40; i++) {
                final int index = i;
                flow.addParagraph(p -> p.name("p" + index).text("Line " + index));
            }
            flow.build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);

            PlacedNode firstOnPage0 = graph.nodes().stream()
                    .filter(n -> n.startPage() == 0 && n.semanticName() != null)
                    .findFirst().orElseThrow();
            PlacedNode firstOnPage1 = graph.nodes().stream()
                    .filter(n -> n.startPage() == 1 && n.semanticName() != null)
                    .findFirst().orElseThrow();

            assertThat(firstOnPage0.placementX()).isCloseTo(10.0, within(0.5));
            assertThat(firstOnPage1.placementX()).isCloseTo(70.0, within(0.5));
        }
    }

    @Test
    void laterRuleWinsWhenRangesOverlap() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.from(1, DocumentInsets.of(10)),   // all pages, left 10
                    PageMarginRule.page(1, DocumentInsets.of(50))));  // page 1 overrides to 50
            session.pageFlow().addParagraph(p -> p.name("first").text("First")).build();

            PlacedNode first = node(session.layoutGraph(), "first");
            assertThat(first.placementX()).isCloseTo(50.0, within(0.5));
        }
    }

    @Test
    void emptyRulesLeaveTheDocumentWideMarginInPlace() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 300)
                .margin(DocumentInsets.of(33))
                .create()) {
            session.pageMargins(List.of());
            session.pageFlow().addParagraph(p -> p.name("only").text("Only")).build();

            assertThat(node(session.layoutGraph(), "only").placementX()).isCloseTo(33.0, within(0.5));
        }
    }

    @Test
    void aBlockSpanningAMarginBoundaryKeepsItsStartPageWidth() throws Exception {
        // Documented limitation: a single block is laid out at the width of the page
        // it BEGINS on, even when it overflows onto a page whose margin differs.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 150)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.symmetric(16, 10)),
                    PageMarginRule.from(2, DocumentInsets.symmetric(16, 70))));
            session.pageFlow().addParagraph(p -> p.name("long").text(
                    "This single paragraph is deliberately long enough to wrap across many "
                    + "lines and overflow from the first page onto the second, so it spans a "
                    + "per-page-margin boundary. Because a block is measured at the width of "
                    + "the page it begins on, it keeps the first page's content column for its "
                    + "whole length rather than re-wrapping at the narrower body margin.")).build();

            LayoutGraph graph = session.layoutGraph();
            PlacedNode block = node(graph, "long");
            assertThat(block.startPage()).isZero();
            assertThat(block.endPage()).isGreaterThan(0);              // it really spans the boundary
            assertThat(block.placementX()).isCloseTo(10.0, within(0.5)); // page-1 width throughout, not 70
        }
    }

    @Test
    void perPageMarginCoexistsWithPageReferences() throws Exception {
        // The unified fixed point resolves page numbers AND per-page widths together.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.symmetric(20, 10)),
                    PageMarginRule.from(2, DocumentInsets.symmetric(20, 70))));
            var flow = session.pageFlow();
            flow.addParagraph(p -> p.name("intro").text("Appendix on page"));
            flow.addPageReference("appendix", DocumentTextStyle.DEFAULT, TextAlign.LEFT);
            for (int i = 0; i < 30; i++) {
                final int n = i;
                flow.addParagraph(p -> p.text("Filler line " + n));
            }
            flow.addParagraph(p -> p.name("appendix").anchor("appendix").text("Appendix"));
            flow.build();

            // Page reference resolved to a genuine forward page...
            OptionalInt resolved = session.pageIndex().pageNumberOf("appendix");
            assertThat(resolved).isPresent();
            assertThat(resolved.getAsInt()).isGreaterThan(1);

            // ...and the per-page margin still applies on each page.
            LayoutGraph graph = session.layoutGraph();
            assertThat(node(graph, "intro").placementX()).isCloseTo(10.0, within(0.5));
            assertThat(node(graph, "appendix").placementX()).isCloseTo(70.0, within(0.5));
        }
    }

    @Test
    void ruleRejectsInvalidRangeAndNonFiniteOrNegativeInsets() {
        assertThatThrownBy(() -> new PageMarginRule(0, 2, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromPage");
        assertThatThrownBy(() -> new PageMarginRule(3, 3, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toPageExclusive");
        assertThatThrownBy(() -> PageMarginRule.page(1, new DocumentInsets(-1, 0, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bleed");
        // NaN / Infinity must be rejected at the public type, not slip through to the engine.
        assertThatThrownBy(() -> PageMarginRule.page(1, new DocumentInsets(Double.NaN, 0, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> PageMarginRule.page(1, new DocumentInsets(0, Double.POSITIVE_INFINITY, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        // The +1 factories must not overflow Integer.MAX_VALUE into a confusing message.
        assertThatThrownBy(() -> PageMarginRule.page(Integer.MAX_VALUE, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from(");
        assertThatThrownBy(() -> PageMarginRule.range(1, Integer.MAX_VALUE, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from(");
    }

    @Test
    void nullRulesClearLikeAnEmptyList() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 300)
                .margin(DocumentInsets.of(28))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.of(8))));
            session.pageMargins(null); // clears back to the document-wide margin
            session.pageFlow().addParagraph(p -> p.name("only").text("Only")).build();

            assertThat(node(session.layoutGraph(), "only").placementX()).isCloseTo(28.0, within(0.5));
        }
    }

    @Test
    void aMiddleOnlyRuleLeavesNeighbourPagesAtTheBaseMargin() throws Exception {
        // A rule for page 2 only: page 1 and page 3 keep the base margin, page 2 is overridden.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.page(2, DocumentInsets.symmetric(20, 80))));
            session.pageFlow()
                    .addParagraph(p -> p.name("p1").text("Page one"))
                    .addPageBreak(b -> { })
                    .addParagraph(p -> p.name("p2").text("Page two"))
                    .addPageBreak(b -> { })
                    .addParagraph(p -> p.name("p3").text("Page three"))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(node(graph, "p1").placementX()).isCloseTo(20.0, within(0.5));  // base
            assertThat(node(graph, "p2").placementX()).isCloseTo(80.0, within(0.5));  // override
            assertThat(node(graph, "p3").placementX()).isCloseTo(20.0, within(0.5));  // base again
        }
    }

    @Test
    void everyTopLevelBlockIsPlacedConsistentlyWithItsStartPage() throws Exception {
        // The coupled fixed point must converge to a consistent assignment: every block's
        // placement X must match the left margin of the page it actually lands on (no block
        // measured for one page but placed against another's geometry).
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 150)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.symmetric(20, 12)),
                    PageMarginRule.from(2, DocumentInsets.symmetric(20, 64))));
            var flow = session.pageFlow();
            for (int i = 0; i < 30; i++) {
                final int n = i;
                flow.addParagraph(p -> p.name("b" + n).text("Block " + n + " sitting near a boundary"));
            }
            flow.build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);
            for (PlacedNode block : graph.nodes()) {
                if (block.parentPath() != null && block.semanticName() != null
                        && block.semanticName().startsWith("b")) {
                    double expectedLeft = block.startPage() == 0 ? 12.0 : 64.0;
                    assertThat(block.placementX())
                            .as("block %s on page %d", block.semanticName(), block.startPage())
                            .isCloseTo(expectedLeft, within(0.5));
                }
            }
        }
    }

    @Test
    void eachMultiSectionSectionResolvesItsOwnSectionLocalPageMargins() throws Exception {
        DocumentSession cover = GraphCompose.document().pageSize(240, 200).margin(DocumentInsets.of(20)).create();
        cover.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.of(12))));
        cover.pageFlow().addParagraph(p -> p.name("cover").text("Cover")).build();
        assertThat(node(cover.layoutGraph(), "cover").placementX()).isCloseTo(12.0, within(0.5));

        DocumentSession body = GraphCompose.document().pageSize(240, 200).margin(DocumentInsets.of(20)).create();
        // Section-local page 1 → its own rule, independent of the cover section.
        body.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.of(72))));
        body.pageFlow().addParagraph(p -> p.name("body").text("Body")).build();
        assertThat(node(body.layoutGraph(), "body").placementX()).isCloseTo(72.0, within(0.5));

        Path out = tempDir.resolve("multi-margins.pdf");
        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
            assertThat(doc.sectionSnapshots()).hasSize(2);
        }
        assertThat(out).exists();
    }

    // -- continuation safe area ------------------------------------------

    @Test
    void aFullBleedDocumentResumesAtTheTrimmedEdgeWithoutASafeArea() throws Exception {
        // The defect the safe area exists for, stated as a measurement. A page
        // margin is the only inset applied once per page; the section's padding is
        // an edge of the section, so it holds page 1 off the top and page 2 gets
        // nothing. Without this case the safe-area test below would still pass if
        // the padding had been doing the work all along.
        try (DocumentSession session = fullBleedSession()) {
            fillPastOnePage(session);

            assertThat(firstLineTopGap(session, 1))
                    .as("page 2 opens on whatever spacing preceded the break, nowhere near "
                        + "the section's 30pt padding — that padding is an edge of the "
                        + "section and was already spent on page 1")
                    .isLessThan(12.0);
            assertThat(firstLineTopGap(session, 0))
                    .as("while page 1 sits on the section's own top padding")
                    .isCloseTo(30.0, within(0.5));
        }
    }

    @Test
    void aTopOnlyRuleInsetsEveryPageItCovers() {
        // The mechanism the templates-side safe area is built on, pinned at the
        // engine level: raising only the top margin from page 2 leaves page 1 alone.
        try (DocumentSession session = fullBleedSession()) {
            session.pageMargins(List.of(
                    PageMarginRule.from(2, new DocumentInsets(36, 0, 0, 0))));
            fillPastOnePage(session);

            assertThat(session.layoutGraph().totalPages())
                    .as("the fixture has to actually continue for this to mean anything")
                    .isGreaterThan(1);
            for (int page = 1; page < session.layoutGraph().totalPages(); page++) {
                assertThat(firstLineTopGap(session, page))
                        .as("page %d clears the safe area", page + 1)
                        .isGreaterThanOrEqualTo(36.0);
            }
            assertThat(firstLineTopGap(session, 0))
                    .as("and page 1 keeps the top edge its own design gave it")
                    .isCloseTo(30.0, within(0.5));
        }
    }

    @Test
    void aVerticalOnlyRuleStillLaysOutLikeItsSinglePassSelf() {
        // A top-only rule cannot change any page's width, so the resolver settles it
        // in one pass. This pins that the shortcut did not change the answer: the
        // same rule expressed with a horizontal difference — which does take the
        // fixed point — puts the same first line in the same place vertically.
        try (DocumentSession shortcut = fullBleedSession();
             DocumentSession fixedPoint = fullBleedSession()) {
            shortcut.pageMargins(List.of(
                    PageMarginRule.from(2, new DocumentInsets(36, 0, 0, 0))));
            fixedPoint.pageMargins(List.of(
                    PageMarginRule.from(1, new DocumentInsets(0, 0, 0, 0)),
                    PageMarginRule.from(2, new DocumentInsets(36, 0, 0, 0))));
            fillPastOnePage(shortcut);
            fillPastOnePage(fixedPoint);

            assertThat(shortcut.layoutGraph().totalPages())
                    .isEqualTo(fixedPoint.layoutGraph().totalPages());
            assertThat(firstLineTopGap(shortcut, 1))
                    .isCloseTo(firstLineTopGap(fixedPoint, 1), within(0.01));
        }
    }

    private static DocumentSession fullBleedSession() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.zero())
                .create();
    }

    /** A padded section long enough to need a second page, as a column flow does. */
    private static void fillPastOnePage(DocumentSession session) {
        session.dsl().pageFlow()
                .name("Root")
                .padding(DocumentInsets.zero())
                .addColumnFlow("Body", body -> body
                        .addColumn("Only", column -> {
                            column.padding(DocumentInsets.of(30)).spacing(6);
                            for (int index = 0; index < 60; index++) {
                                int current = index;
                                column.addParagraph(p -> p
                                        .name("line-" + current)
                                        .text("line " + current + " with enough words to occupy a line"));
                            }
                        }))
                .build();
    }

    /**
     * Distance from the trimmed top edge to the top of the highest line of text on
     * a page. Measured on text fragments rather than nodes: a paragraph that broke
     * across the page boundary still belongs to a node that started on the previous
     * page, and it is exactly that line the reader sees at the top.
     */
    private static double firstLineTopGap(DocumentSession session, int pageIndex) {
        double pageHeight = session.canvas().height();
        return session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                .mapToDouble(fragment -> pageHeight - (fragment.y() + fragment.height()))
                .min()
                .orElseThrow(() -> new AssertionError("no text on page " + (pageIndex + 1)));
    }
}
