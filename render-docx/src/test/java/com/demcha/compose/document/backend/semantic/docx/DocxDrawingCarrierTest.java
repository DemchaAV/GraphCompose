package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Word anchors a drawing in a paragraph on its page. A page break's paragraph stands on the page
 * the break ends, and carries what is drawn there; a drawing on a page no paragraph is written
 * on is named as not in the document, where its note had said it was drawn.
 */
class DocxDrawingCarrierTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);

    @Test
    void aShapeAloneBeforeAPageBreakIsAnchoredInTheBreaksParagraph() throws Exception {
        Export export = export(page -> page.addCircle(120, INK).addPageBreak(b -> { }).addParagraph("Text"));

        String breaking = export.document().getParagraphs().get(0).getCTP().xmlText();
        assertThat(breaking).contains("<wp:anchor").contains("w:type=\"page\"");
        assertThat(export.report().notes()).noneMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aShapeOnAPageNoParagraphIsWrittenOnIsNamedNotInTheDocument() throws Exception {
        // The shape fills its page and the paragraph after it opens the next: no break, nothing
        // written there to carry it.
        Export export = export(page -> page.addShape(300, 560, INK).addParagraph("After"));

        assertThat(export.document().getDocument().xmlText()).doesNotContain("<wp:anchor");
        assertThat(export.report().bySubject().get("ShapeNode")).extracting(DocxExportReport.Note::severity,
                        DocxExportReport.Note::detail)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(DocxExportReport.Severity.DROPPED,
                        "it stands on a page no paragraph is written on, and Word anchors a drawing in a "
                        + "paragraph on its page: nothing carries it, so it is not in the document"));
        assertThat(export.report().bySubject()).as("named on the shape, not on its page").doesNotContainKey("drawing");
    }

    @Test
    void aNodeDrawnOverThreePagesIsNamedForThePartNothingCarries() throws Exception {
        // A node kind of the caller's own, split by its definition: one shape on each of three
        // pages. Pages 1 and 3 have a paragraph to carry theirs; page 2 has none.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .create()) {
            session.registerNodeDefinition(new BarDefinition());
            session.pageFlow(page -> page.addParagraph("Intro").add(new Bar("Bar", 1300)).addParagraph("After"));
            assertThat(session.layoutGraph().fragments())
                    .filteredOn(fragment -> fragment.path().endsWith("Bar[1]"))
                    .extracting(com.demcha.compose.document.layout.PlacedFragment::pageIndex)
                    .as("the premise: one shape on each page")
                    .containsExactly(0, 1, 2);
            document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend(report::set))));
        }

        assertThat(document.getDocument().xmlText().split("<wp:anchor", -1))
                .as("the shapes on pages 1 and 3, carried").hasSize(3);
        assertThat(report.get().bySubject().get("Bar"))
                .filteredOn(note -> note.severity() == DocxExportReport.Severity.DROPPED)
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).isEqualTo("ContainerNode[0]/Bar[1]");
                    assertThat(note.detail()).isEqualTo("part of what it is drawn as stands on a page no paragraph "
                                                        + "is written on, and Word anchors a drawing in a paragraph "
                                                        + "on its page: nothing carries that part, so it is not in "
                                                        + "the document");
                });
        assertThat(report.get().bySubject()).as("named on the node, not on its page").doesNotContainKey("drawing");
    }

    /** A bar of a given height that its definition splits wherever a page ends. */
    private record Bar(String name, double height) implements com.demcha.compose.document.node.DocumentNode {
    }

    private static final class BarDefinition implements com.demcha.compose.document.layout.NodeDefinition<Bar> {
        @Override
        public Class<Bar> nodeType() {
            return Bar.class;
        }

        @Override
        public com.demcha.compose.document.layout.PreparedNode<Bar> prepare(Bar node,
                com.demcha.compose.document.layout.PrepareContext ctx,
                com.demcha.compose.document.layout.BoxConstraints constraints) {
            return com.demcha.compose.document.layout.PreparedNode.leaf(node,
                    new com.demcha.compose.document.layout.MeasureResult(200, node.height()));
        }

        @Override
        public com.demcha.compose.document.layout.PaginationPolicy paginationPolicy(Bar node) {
            return com.demcha.compose.document.layout.PaginationPolicy.SPLITTABLE;
        }

        @Override
        public com.demcha.compose.document.layout.PreparedSplitResult<Bar> split(
                com.demcha.compose.document.layout.PreparedNode<Bar> prepared,
                com.demcha.compose.document.layout.SplitRequest request) {
            double height = prepared.node().height();
            double room = request.remainingHeight();
            if (height <= room) {
                return com.demcha.compose.document.layout.PreparedSplitResult.whole(prepared);
            }
            return new com.demcha.compose.document.layout.PreparedSplitResult<>(
                    com.demcha.compose.document.layout.PreparedNode.leaf(new Bar(prepared.node().name(), room),
                            new com.demcha.compose.document.layout.MeasureResult(200, room)),
                    com.demcha.compose.document.layout.PreparedNode.leaf(new Bar(prepared.node().name(), height - room),
                            new com.demcha.compose.document.layout.MeasureResult(200, height - room)));
        }

        @Override
        public java.util.List<com.demcha.compose.document.layout.LayoutFragment> emitFragments(
                com.demcha.compose.document.layout.PreparedNode<Bar> prepared,
                com.demcha.compose.document.layout.FragmentContext ctx,
                com.demcha.compose.document.layout.FragmentPlacement placement) {
            return java.util.List.of(new com.demcha.compose.document.layout.LayoutFragment(placement.path(), 0, 0, 0,
                    placement.width(), placement.height(),
                    new com.demcha.compose.document.layout.payloads.EllipseFragmentPayload(INK.color(), null, null, null)));
        }
    }

    private record Export(XWPFDocument document, DocxExportReport report) {
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            byte[] bytes = session.export(new DocxSemanticBackend(report::set));
            return new Export(new XWPFDocument(new ByteArrayInputStream(bytes)), report.get());
        }
    }
}
