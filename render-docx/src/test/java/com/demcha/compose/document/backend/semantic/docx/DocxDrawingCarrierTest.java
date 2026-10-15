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
