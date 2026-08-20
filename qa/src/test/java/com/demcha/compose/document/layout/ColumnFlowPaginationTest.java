package com.demcha.compose.document.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.ColumnFlowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.dsl.ColumnFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A two-column body that outgrows its page.
 *
 * <p>A row is one band and is atomic: it must fit the page it starts on, and
 * content that does not fit has nowhere to go — the compiler says so by
 * throwing. That is correct for a row of cells and fatal for a document body,
 * which is why layouts built from a row cap their content at a page and
 * truncate the rest.</p>
 *
 * <p>The first two cases are the same content twice: through a row, which
 * throws, and through a column flow, which paginates. Everything after that
 * pins the properties the flow has to hold while doing it.</p>
 */
class ColumnFlowPaginationTest {

    @Test
    void aRowCannotHoldMoreThanOnePage() {
        assertThatThrownBy(() -> render(session -> session.pageFlow()
                .name("Root")
                .addRow("Body", row -> row
                        .gap(18)
                        .weights(1.0, 1.0)
                        .addSection("Side", side -> paragraphs(side, "side", 40))
                        .addSection("Main", main -> paragraphs(main, "main", 40)))
                .build()))
                .as("the row band is atomic, so overflowing it is an error rather than a page break")
                .isInstanceOf(AtomicNodeTooLargeException.class);
    }

    @Test
    void aColumnFlowWithTheSameContentPaginatesInstead() throws Exception {
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .weights(1.0, 1.0)
                            .addColumn("Side", side -> paragraphs(side, "side", 40))
                            .addColumn("Main", main -> paragraphs(main, "main", 40)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages())
                    .as("the same content that overflowed a row now spans pages")
                    .isGreaterThan(1);
            assertThat(paragraphPages(graph, "side"))
                    .as("the left column continues past the first page")
                    .contains(0, 1);
            assertThat(paragraphPages(graph, "main"))
                    .as("the right column continues past the first page")
                    .contains(0, 1);
        }
    }

    @Test
    void everyLineSurvivesTheBreak() throws Exception {
        // The failures a paginator must not have: content that vanished, content
        // that came back in the wrong order, and content that skipped a page.
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .addColumn("Side", side -> paragraphs(side, "side", 40))
                            .addColumn("Main", main -> paragraphs(main, "main", 40)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            for (String column : List.of("side", "main")) {
                PlacedNode previous = null;
                for (int index = 0; index < 40; index++) {
                    String text = column + "-" + index;
                    PlacedNode current = graph.nodes().stream()
                            .filter(placed -> placed.semanticName().equals(text))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError(text + " was dropped by the break"));
                    if (previous != null) {
                        assertThat(current.startPage())
                                .as("%s must not move backwards or skip a page", text)
                                .isBetween(previous.startPage(), previous.startPage() + 1);
                        if (current.startPage() == previous.startPage()) {
                            assertThat(current.placementY())
                                    .as("%s must sit below its predecessor on the same page", text)
                                    .isLessThan(previous.placementY());
                        }
                    }
                    previous = current;
                }
            }
        }
    }

    @Test
    void columnsAreIndependentSoAShortColumnDoesNotStretch() throws Exception {
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .addColumn("Short", side -> paragraphs(side, "short", 2))
                            .addColumn("Long", main -> paragraphs(main, "long", 40)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(paragraphPages(graph, "short"))
                    .as("a column that fits stays on the first page")
                    .containsExactly(0);
            assertThat(paragraphPages(graph, "long"))
                    .as("its neighbour keeps flowing")
                    .contains(0, 1);
        }
    }

    @Test
    void whatFollowsTheFlowStartsBelowTheLongestColumn() throws Exception {
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .addColumn("Short", side -> paragraphs(side, "short", 2))
                            .addColumn("Long", main -> paragraphs(main, "long", 40)))
                    .addParagraph(p -> p.name("after").text("after the flow"))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            PlacedNode after = node(graph, "after");
            PlacedNode lastLong = lastNodeNamed(graph, "long");

            assertThat(after.startPage())
                    .as("the flow ends where its longest column ended, not its shortest")
                    .isEqualTo(lastLong.startPage());
            assertThat(after.placementY())
                    .as("and the next block sits below that column, not on top of it")
                    .isLessThan(lastLong.placementY());
        }
    }

    @Test
    void aFlowThatFitsOnOnePageLaysOutLikeARowWould() throws Exception {
        // The new node must not cost anything for the ordinary case: content
        // that fits produces one page and side-by-side columns.
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .weights(1.0, 1.0)
                            .addColumn("Side", side -> paragraphs(side, "side", 2))
                            .addColumn("Main", main -> paragraphs(main, "main", 2)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            PlacedNode side = node(graph, "side-0");
            PlacedNode main = node(graph, "main-0");
            assertThat(side.placementX())
                    .as("columns sit side by side")
                    .isLessThan(main.placementX());
            assertThat(side.placementY())
                    .as("and start at the same height")
                    .isCloseTo(main.placementY(), org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Test
    void anEmptyFlowIsHarmless() {
        assertThatCode(() -> render(session -> session.pageFlow()
                .name("Root")
                .addColumnFlow("Body", body -> body.gap(12))
                .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void aColumnMustBeAVerticalContainer() {
        // A column is a vertical flow — that is what paginates. Anything else
        // beside its siblings belongs inside a column.
        assertThatThrownBy(() -> new ColumnFlowBuilderProbe().addNonColumn())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vertical container");
    }

    @Test
    void weightsMustMatchTheColumns() {
        assertThatThrownBy(() -> new ColumnFlowNode("Body",
                List.of(emptySection("a"), emptySection("b")),
                List.of(1.0), 0.0, DocumentInsets.zero(), DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match children size");
    }

    @Test
    void aColumnFlowIsRejectedInsideARowSlot() {
        // A row slot is pinned to one page; a column flow advances pages. Placed
        // here it would stack its columns down the slot and run past the band,
        // overlapping whatever follows.
        assertThatThrownBy(() -> render(session -> session.pageFlow()
                .name("Root")
                .addRow("Band", row -> row
                        .addSection("Left", left -> left
                                .addColumnFlow("Nested", body -> body
                                        .addColumn("A", a -> paragraphs(a, "a", 2))
                                        .addColumn("B", b -> paragraphs(b, "b", 2))))
                        .addSection("Right", right -> paragraphs(right, "right", 2)))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot contain a column flow")
                .hasMessageContaining("row slot");
    }

    @Test
    void aColumnFlowIsRejectedInsideAStackLayer() {
        assertThatThrownBy(() -> render(session -> session.pageFlow()
                .name("Root")
                .addLayerStack(stack -> stack.layer(new SectionBuilder()
                        .name("Layer")
                        .addColumnFlow("Nested", body -> body
                                .addColumn("A", a -> paragraphs(a, "a", 2))
                                .addColumn("B", b -> paragraphs(b, "b", 2)))
                        .build()))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot contain a column flow")
                .hasMessageContaining("stack layer");
    }

    @Test
    void aHeadingAboveTheFlowIsNotHoistedToTheNextPage() throws Exception {
        // keepWithNext asks "does my run plus the first line of what follows fit
        // here?". A flow answers with its own height unless the lookahead reads
        // its first column -- and a heading above a body that is going to break
        // anyway would then jump to the next page and strand the rest of this one.
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .spacing(4)
                    .addSection("Filler", filler -> paragraphs(filler, "filler", 38))
                    .addSection("Heading", heading -> heading
                            .keepWithNext()
                            .addParagraph(p -> p.name("heading").text("Experience")))
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .addColumn("Side", side -> paragraphs(side, "side", 10))
                            .addColumn("Main", main -> paragraphs(main, "main", 10)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(node(graph, "heading").startPage())
                    .as("the heading stays on the page it was reached on")
                    .isEqualTo(lastNodeNamed(graph, "filler").startPage());
            assertThat(node(graph, "side-0").startPage())
                    .as("and its body starts there too, using the rest of the page")
                    .isEqualTo(node(graph, "heading").startPage());
        }
    }

    @Test
    void aColumnPanelIsRepaintedOnEveryPageItSpans() throws Exception {
        // The flow has no chrome of its own; a column that wants a panel is a
        // section with a fill, and a section's fill already repeats per page.
        // That is the whole decoration story, so it has to actually hold.
        try (DocumentSession session = newSession()) {
            session.pageFlow()
                    .name("Root")
                    .addColumnFlow("Body", body -> body
                            .gap(18)
                            .addColumn("Side", side -> {
                                side.fillColor(DocumentColor.rgb(240, 240, 240));
                                paragraphs(side, "side", 40);
                            })
                            .addColumn("Main", main -> paragraphs(main, "main", 40)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            List<Integer> panelPages = graph.fragments().stream()
                    .filter(fragment -> fragment.path().contains("Side"))
                    .filter(fragment -> fragment.payload() instanceof ShapeFragmentPayload)
                    .map(PlacedFragment::pageIndex)
                    .distinct()
                    .sorted()
                    .toList();

            assertThat(graph.totalPages()).isGreaterThan(1);
            assertThat(panelPages)
                    .as("the panel is painted on each page the column reached")
                    .containsExactlyElementsOf(
                            IntStream.range(0, graph.totalPages()).boxed().toList());
        }
    }

    // -- helpers ---------------------------------------------------------

    /** Probe for the builder's column-type rejection. */
    private static final class ColumnFlowBuilderProbe {
        void addNonColumn() {
            new ColumnFlowBuilder().addColumn(new SpacerNode("spacer", 10, 10,
                    DocumentInsets.zero(), DocumentInsets.zero(), 0.0));
        }
    }

    private static SectionNode emptySection(String name) {
        return new SectionBuilder().name(name).build();
    }

    private static void paragraphs(SectionBuilder section,
                                   String prefix, int count) {
        section.spacing(4);
        for (int index = 0; index < count; index++) {
            int current = index;
            section.addParagraph(p -> p
                    .name(prefix + "-" + current)
                    .text(prefix + " paragraph " + current + " with enough words to occupy a line"));
        }
    }

    private static DocumentSession newSession() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create();
    }

    private static void render(java.util.function.Consumer<DocumentSession> body) {
        try (DocumentSession session = newSession()) {
            body.accept(session);
            session.layoutGraph();
        }
    }

    private static List<Integer> paragraphPages(LayoutGraph graph, String prefix) {
        List<Integer> pages = new ArrayList<>();
        for (PlacedNode node : graph.nodes()) {
            if (node.semanticName().startsWith(prefix + "-") && !pages.contains(node.startPage())) {
                pages.add(node.startPage());
            }
        }
        return pages;
    }

    private static PlacedNode node(LayoutGraph graph, String semanticName) {
        return graph.nodes().stream()
                .filter(node -> node.semanticName().equals(semanticName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no node named " + semanticName));
    }

    private static PlacedNode lastNodeNamed(LayoutGraph graph, String prefix) {
        PlacedNode last = null;
        for (PlacedNode node : graph.nodes()) {
            if (node.semanticName().startsWith(prefix + "-")) {
                last = node;
            }
        }
        if (last == null) {
            throw new AssertionError("no node starting with " + prefix);
        }
        return last;
    }
}
