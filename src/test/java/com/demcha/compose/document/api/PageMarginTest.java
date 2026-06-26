package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

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
    void ruleRejectsInvalidRangeAndNegativeInsets() {
        assertThatThrownBy(() -> new PageMarginRule(0, 2, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromPage");
        assertThatThrownBy(() -> new PageMarginRule(3, 3, DocumentInsets.of(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toPageExclusive");
        assertThatThrownBy(() -> PageMarginRule.page(1, new DocumentInsets(-1, 0, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bleed");
    }
}
