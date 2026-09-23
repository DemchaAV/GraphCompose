package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page reference is Word's own {@code PAGEREF} field, reading the page the layout resolved.
 *
 * <p>The export dropped page references, so a table of contents reached Word with its entries
 * and no page numbers. As a field the number is the page the editor counts — measured in
 * LibreOffice, a field whose cached number was replaced by 99 still showed the real pages —
 * and until an editor updates it, it reads the page the PDF shows.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageReferenceTest {

    @Test
    void aTableOfContentsCarriesAFieldPerEntryReadingTheLaidOutPage() throws Exception {
        try (XWPFDocument document = export(threeChapters())) {
            Map<String, String> fields = pageReferences(document);

            assertThat(fields).containsExactly(
                    Map.entry("PAGEREF intro \\h", "2"),
                    Map.entry("PAGEREF terms \\h", "3"),
                    Map.entry("PAGEREF prices \\h", "4"));
            assertThat(bookmarkNames(document))
                    .as("every field names a bookmark the document has, a section's anchor included")
                    .contains("intro", "terms", "prices");
        }
    }

    @Test
    void aStandalonePageReferenceIsAFieldToo() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addPageReference("terms")
                .addPageBreak(b -> { })
                .addSection(s -> s.anchor("terms").addParagraph(p -> p.text("Terms"))))) {
            assertThat(pageReferences(document)).containsExactly(Map.entry("PAGEREF terms \\h", "2"));
        }
    }

    @Test
    void aReferenceToAnAnchorTheDocumentDoesNotHaveIsPlainText() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addPageReference("nowhere")
                .addParagraph(p -> p.text("Body")))) {
            assertThat(pageReferences(document))
                    .as("Word would turn it into 'Error! Bookmark not defined.' on the first update")
                    .isEmpty();
            assertThat(document.getParagraphs().get(0).getText())
                    .as("the placeholder the page prints for an unresolved reference — empty unless set")
                    .isEmpty();
        }
    }

    @Test
    void theDocumentDoesNotAskToUpdateItsFieldsOnOpen() throws Exception {
        try (XWPFDocument document = export(threeChapters())) {
            PackagePart settings = document.getPackage()
                    .getPart(PackagingURIHelper.createPartName("/word/settings.xml"));
            String xml;
            try (InputStream input = settings.getInputStream()) {
                xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            // The fields open reading the layout's numbers; w:updateFields would only add a
            // prompt asking the reader to recompute what is already right.
            assertThat(xml).doesNotContain("updateFields");
        }
    }

    private static java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> threeChapters() {
        return page -> page
                .addTableOfContents(toc -> toc.title("Contents")
                        .entry("Intro", "intro")
                        .entry("Terms", "terms")
                        .entry("Prices", "prices"))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("Intro").anchor("intro"))
                .addPageBreak(b -> { })
                .addSection(s -> s.anchor("terms").addParagraph(p -> p.text("Terms")))
                .addPageBreak(b -> { })
                .addSection(s -> s.anchor("prices").addParagraph(p -> p.text("Prices")));
    }

    private static XWPFDocument export(
            java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 300)
                .margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.toDocxBytes();
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    /** Every PAGEREF field in the body and its tables, instruction to the text it reads. */
    private static Map<String, String> pageReferences(XWPFDocument document) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (XWPFParagraph paragraph : allParagraphs(document)) {
            for (CTSimpleField field : paragraph.getCTP().getFldSimpleList()) {
                String instruction = field.getInstr().trim();
                if (instruction.startsWith("PAGEREF")) {
                    StringBuilder text = new StringBuilder();
                    field.getRList().forEach(run -> run.getTList().forEach(t -> text.append(t.getStringValue())));
                    fields.put(instruction, text.toString());
                }
            }
        }
        return fields;
    }

    private static List<String> bookmarkNames(XWPFDocument document) {
        List<String> names = new ArrayList<>();
        for (XWPFParagraph paragraph : allParagraphs(document)) {
            for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
                names.add(bookmark.getName());
            }
        }
        return names;
    }

    private static List<XWPFParagraph> allParagraphs(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    paragraphs.addAll(cell.getParagraphs());
                }
            }
        }
        return paragraphs;
    }
}
