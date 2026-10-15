package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
    void aReferenceToAnAnchorATablesCellIsBuiltFromIsAFieldToo() throws Exception {
        // A paragraph or a section a cell is built from writes its bookmark in the cell. Read by
        // the table's children alone, the anchors were taken to have none: the reference was
        // written as a number an edit does not update, and its note said the bookmark was missing.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(page -> page
                .addPageReference("fees")
                .addPageReference("terms")
                .addTable(t -> t.autoColumns(2).rowCells(
                        DocumentTableCell.node(new ParagraphBuilder().text("Fees").anchor("fees").build()),
                        DocumentTableCell.node(new SectionBuilder().anchor("terms").addParagraph("Terms").build()))),
                report)) {

            assertThat(pageReferences(document)).containsExactly(
                    Map.entry("PAGEREF fees \\h", "1"),
                    Map.entry("PAGEREF terms \\h", "1"));
            assertThat(bookmarkNames(document))
                    .as("each names a bookmark its cell holds")
                    .contains("fees", "terms");
            assertThat(report.get().notes())
                    .noneMatch(note -> note.detail().contains("has no bookmark"));
        }
    }

    @Test
    void aReferenceToARuleInAStackOfOneLayerInATablesCellIsAFieldToo() throws Exception {
        // A stack holding only drawing is an overlay of its own where the layout places it; in a
        // composed cell it is a stack of one layer, and the rule in it is written with its
        // bookmark. Read off the tree as an overlay, its anchor was taken to have none.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(page -> page
                .addPageReference("rule")
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new LayerStackBuilder()
                        .layer(new LineBuilder().horizontal(100).thickness(1).anchor("rule").build())
                        .build()))),
                report)) {

            assertThat(pageReferences(document)).containsExactly(Map.entry("PAGEREF rule \\h", "1"));
            assertThat(bookmarkNames(document)).contains("rule");
            assertThat(report.get().notes())
                    .noneMatch(note -> note.detail().contains("has no bookmark"));
        }
    }

    @Test
    void aReferenceWhoseBookmarkTheBodyDoesNotWriteIsTakenBackToItsText() throws Exception {
        // A section in a table's cell holding only a shape the export draws writes no paragraph to
        // put its bookmark round. A field on the bookmark the file lacks would read "Error!
        // Bookmark not defined." once Word updates it.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(page -> page
                .addPageReference("logo", DocumentTextStyle.DEFAULT.withSize(9), TextAlign.LEFT)
                // One its margin already has a note for: the note gains what the field lost.
                .add(new PageReferenceNode("", "logo", null, TextAlign.LEFT, "", DocumentInsets.zero(),
                        new DocumentInsets(0, 0, 0, 12)))
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new SectionBuilder()
                        .anchor("logo").addShape(shape -> shape.size(40, 20).fillColor(DocumentColor.BLACK))
                        .build()))),
                report)) {

            assertThat(bookmarkNames(document)).doesNotContain("logo");
            assertThat(pageReferences(document)).as("no field on the missing bookmark").isEmpty();
            for (XWPFParagraph reference : document.getParagraphs().subList(0, 2)) {
                assertThat(reference.getText()).as("the page the layout resolved, as text").isEqualTo("1");
                assertThat(reference.getCTP().xmlText()).doesNotContain("fldChar").doesNotContain("instrText");
            }
            // The paragraph also carries the cell's drawing, anchored on its page.
            assertThat(document.getParagraphs().get(0).getRuns())
                    .as("the number keeps its run and the run its style")
                    .filteredOn(run -> run.text() != null && !run.text().isEmpty())
                    .singleElement()
                    .satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(9.0));
            String noBookmark = "its anchor has no bookmark in the Word file, so its page number is written as "
                                + "text that an edit does not update";
            assertThat(report.get().notes())
                    .filteredOn(note -> note.subject().equals("PageReferenceNode"))
                    .extracting(DocxExportReport.Note::detail)
                    .satisfiesExactly(
                            plain -> assertThat(plain).isEqualTo("written as a paragraph; " + noBookmark),
                            held -> assertThat(held).startsWith("written as a paragraph; " + noBookmark + "; ")
                                    .contains("left"));
        }
    }

    @Test
    void aReferenceNamesNoBookmarkOfItsOwnAndTakesNoNameFromAnother() throws Exception {
        // "a." cleans to the name "a" has. Had the reference been given a name, "a." would have
        // taken "a" and pushed the anchor "a" to "a_2" — the name the anchor "a_2" is given too.
        try (XWPFDocument document = export(page -> page
                .addPageReference("a.")
                .addParagraph(p -> p.text("One").anchor("a"))
                .addParagraph(p -> p.text("Two").anchor("a_2")))) {

            assertThat(bookmarkNames(document))
                    .as("each anchor its own name, none taken twice")
                    .containsExactly("a", "a_2");
            assertThat(pageReferences(document))
                    .as("no anchor \"a.\" in the file: no field, whatever bookmark shares its name")
                    .isEmpty();
        }
    }

    @Test
    void aReferenceToALayerStacksColumnIsWrittenAsTheNumberItReads() throws Exception {
        // A column of a layer stack is written as what it holds, with no bookmark round it.
        try (XWPFDocument document = export(page -> page
                .addPageReference("left")
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().anchor("left")
                                .margin(new DocumentInsets(0, 140, 0, 0)).addParagraph("Left").build(),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .margin(new DocumentInsets(0, 0, 0, 130)).addParagraph("Right").build(),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT)))) {

            assertThat(bookmarkNames(document)).doesNotContain("left");
            assertThat(pageReferences(document)).isEmpty();
            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("1");
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
        return export(content, new AtomicReference<>());
    }

    private static XWPFDocument export(
            java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content,
            AtomicReference<DocxExportReport> report) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 300)
                .margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    /** Every PAGEREF field in the body and its tables, instruction to the text it reads. */
    private static Map<String, String> pageReferences(XWPFDocument document) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (XWPFParagraph paragraph : allParagraphs(document)) {
            // A complex field: its instruction in one run, what it reads in the runs between the
            // separator and the end.
            String instruction = null;
            boolean reading = false;
            StringBuilder text = new StringBuilder();
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : paragraph.getCTP().getRList()) {
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText code : run.getInstrTextList()) {
                    instruction = code.getStringValue().trim();
                }
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar mark : run.getFldCharList()) {
                    if (mark.getFldCharType() == org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.SEPARATE) {
                        reading = true;
                    } else if (mark.getFldCharType() == org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.END) {
                        if (instruction != null && instruction.startsWith("PAGEREF")) {
                            fields.put(instruction, text.toString());
                        }
                        instruction = null;
                        reading = false;
                        text.setLength(0);
                    }
                }
                if (reading) {
                    run.getTList().forEach(t -> text.append(t.getStringValue()));
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
