package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DocumentListNodeTest {

    @Test
    void listShortcutShouldBuildBulletListWithoutManualParagraphLoop() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(260, 200))
                .margin(Margin.of(12))
                .create()) {

            ContainerNode root = session.pageFlow()
                    .name("ListShortcutRoot")
                    .addList("Java", "SQL", "Kotlin")
                    .build();

            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(ListNode.class);

            List<BuiltInNodeDefinitions.ParagraphFragmentPayload> payloads = paragraphPayloads(session.layoutGraph());
            assertThat(payloads).hasSize(3);
            assertThat(firstLineTexts(payloads)).containsExactly(
                    "\u2022 Java",
                    "\u2022 SQL",
                    "\u2022 Kotlin");

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                String extracted = normalizeWhitespace(new PDFTextStripper().getText(document));
                assertThat(extracted).contains("\u2022 Java");
                assertThat(extracted).contains("\u2022 SQL");
                assertThat(extracted).contains("\u2022 Kotlin");
            }
        }
    }

    @Test
    void listBuilderShouldSupportDashPlainAndCustomMarkers() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(320, 260))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("MarkerRoot")
                    .spacing(4)
                    .addList(list -> list
                            .name("DashList")
                            .dash()
                            .items("- Java", "+ SQL", "* Kotlin"))
                    .addList(list -> list
                            .name("PlainList")
                            .noMarker()
                            .items("Aligned plain row"))
                    .addList(list -> list
                            .name("CustomList")
                            .marker(">")
                            .items("Custom row"))
                    .build();

            assertThat(firstLineTexts(paragraphPayloads(session.layoutGraph()))).containsExactly(
                    "- Java",
                    "- SQL",
                    "- Kotlin",
                    "Aligned plain row",
                    "> Custom row");
        }
    }

    @Test
    void wrappedListItemShouldIndentContinuationWithoutRepeatingMarker() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(165, 220))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("WrappedRoot")
                    .addList(list -> list
                            .name("WrappedList")
                            .lineSpacing(1)
                            .items(
                                    "Long item text should wrap across several visual lines while keeping only one visible bullet marker.",
                                    "Tail"))
                    .build();

            List<BuiltInNodeDefinitions.ParagraphFragmentPayload> payloads = paragraphPayloads(session.layoutGraph());
            List<String> firstItemLines = payloads.getFirst().lines().stream()
                    .map(BuiltInNodeDefinitions.ParagraphLine::text)
                    .toList();

            assertThat(firstItemLines).hasSizeGreaterThan(1);
            assertThat(countOccurrences(String.join("\n", firstItemLines), "\u2022")).isEqualTo(1);
            assertThat(payloads.get(1).lines().getFirst().text()).isEqualTo("\u2022 Tail");
        }
    }

    @Test
    void markerlessListShouldIndentWrappedContinuationWithoutIndentingEveryItem() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(150, 220))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("MarkerlessRoot")
                    .addList(list -> list
                            .name("MarkerlessRows")
                            .noMarker()
                            .continuationIndent("  ")
                            .items(
                                    "Alpha row should wrap onto a continuation line while keeping the first line aligned.",
                                    "Beta row starts aligned with alpha."))
                    .build();

            List<BuiltInNodeDefinitions.ParagraphFragmentPayload> payloads = paragraphPayloads(session.layoutGraph());
            List<String> firstItemLines = payloads.getFirst().lines().stream()
                    .map(BuiltInNodeDefinitions.ParagraphLine::text)
                    .toList();

            assertThat(payloads).hasSize(2);
            assertThat(firstItemLines).hasSizeGreaterThan(1);
            assertThat(firstItemLines.getFirst()).startsWith("Alpha");
            assertThat(firstItemLines.get(1)).startsWith("  ");
            assertThat(payloads.get(1).lines().getFirst().text()).startsWith("Beta");
        }
    }

    @Test
    void listShouldSplitAcrossPages() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(180, 120))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("PagedListRoot")
                    .addList(list -> list
                            .name("PagedList")
                            .items(List.of(
                                    "Item 1", "Item 2", "Item 3", "Item 4", "Item 5",
                                    "Item 6", "Item 7", "Item 8", "Item 9", "Item 10")))
                    .build();

            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isGreaterThan(1);
            assertThat(graph.nodes()).anySatisfy(node -> {
                assertThat(node.semanticName()).isEqualTo("PagedList");
                assertThat(node.nodeKind()).isEqualTo("ListNode");
                assertThat(node.endPage()).isGreaterThan(node.startPage());
            });
        }
    }

    @Test
    void listLayoutSnapshotShouldCoverMarkerStyles() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(320, 260))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("ListSnapshotRoot")
                    .spacing(6)
                    .addList(list -> list
                            .name("BulletList")
                            .items("Java", "SQL")
                            .padding(Padding.of(2)))
                    .addList(list -> list
                            .name("DashList")
                            .dash()
                            .items("Spring", "Hibernate"))
                    .addList(list -> list
                            .name("PlainList")
                            .noMarker()
                            .items("No visible marker"))
                    .addList(list -> list
                            .name("CustomList")
                            .marker(">")
                            .items("Custom marker"))
                    .build();

            LayoutSnapshotAssertions.assertMatches(session, "document/list_markers");
        }
    }

    @Test
    void listFragmentsShouldCarryPaddingThroughParagraphPayload() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(320, 220))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow()
                    .name("PaddingRoot")
                    .addList(list -> list
                            .name("PaddedList")
                            .items("Alpha", "Beta", "Gamma")
                            .padding(new Padding(3, 5, 7, 11)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            PlacedNode listNode = graph.nodes().stream()
                    .filter(node -> node.semanticName().equals("PaddedList"))
                    .findFirst()
                    .orElseThrow();
            List<PlacedFragment> fragments = graph.fragments().stream()
                    .filter(fragment -> fragment.path().equals(listNode.path()))
                    .toList();

            assertThat(fragments).hasSize(3);
            assertThat(fragments).allSatisfy(fragment -> {
                assertThat(fragment.x()).isCloseTo(listNode.placementX(), within(0.01));
                assertThat(fragment.width()).isCloseTo(listNode.placementWidth(), within(0.01));
            });

            BuiltInNodeDefinitions.ParagraphFragmentPayload first = paragraphPayload(fragments.getFirst());
            BuiltInNodeDefinitions.ParagraphFragmentPayload middle = paragraphPayload(fragments.get(1));
            BuiltInNodeDefinitions.ParagraphFragmentPayload last = paragraphPayload(fragments.getLast());

            assertPadding(first.padding(), 3, 5, 0, 11);
            assertPadding(middle.padding(), 0, 5, 0, 11);
            assertPadding(last.padding(), 0, 5, 7, 11);
        }
    }

    private static List<BuiltInNodeDefinitions.ParagraphFragmentPayload> paragraphPayloads(LayoutGraph graph) {
        return graph.fragments().stream()
                .map(PlacedFragment::payload)
                .filter(BuiltInNodeDefinitions.ParagraphFragmentPayload.class::isInstance)
                .map(BuiltInNodeDefinitions.ParagraphFragmentPayload.class::cast)
                .toList();
    }

    private static BuiltInNodeDefinitions.ParagraphFragmentPayload paragraphPayload(PlacedFragment fragment) {
        assertThat(fragment.payload()).isInstanceOf(BuiltInNodeDefinitions.ParagraphFragmentPayload.class);
        return (BuiltInNodeDefinitions.ParagraphFragmentPayload) fragment.payload();
    }

    private static void assertPadding(Padding padding, double top, double right, double bottom, double left) {
        assertThat(padding.top()).isCloseTo(top, within(0.01));
        assertThat(padding.right()).isCloseTo(right, within(0.01));
        assertThat(padding.bottom()).isCloseTo(bottom, within(0.01));
        assertThat(padding.left()).isCloseTo(left, within(0.01));
    }

    private static List<String> firstLineTexts(List<BuiltInNodeDefinitions.ParagraphFragmentPayload> payloads) {
        return payloads.stream()
                .map(payload -> payload.lines().getFirst().text())
                .toList();
    }

    private static int countOccurrences(String text, String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
