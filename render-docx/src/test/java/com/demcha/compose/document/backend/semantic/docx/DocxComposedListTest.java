package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A list composed in a table's cell is written on the lines its table laid out for it, as a
 * paragraph composed there is, and as a list in the flow is on its own.
 *
 * <p>A composed cell's content is laid out under its table's path, so a list there has no lines at
 * a path of its own. Matched to none, its items were written at no height at all, and Word set each
 * at its face's own line: {@code SlateOrange}'s certifications, a list beside the education column,
 * stood 0.3pt lower with every item in Word and LibreOffice, the fourth 1.2pt low in Word.</p>
 */
class DocxComposedListTest {

    @Test
    void aListInACellIsWrittenAtTheHeightThePageSetsItsLines() throws Exception {
        try (XWPFDocument flow = DocxExports.withLayout(400, 600, 20, page -> page.add(list())
                .addParagraph(p -> p.text("After")));
             XWPFDocument cell = inACellBeside(list())) {
            for (String item : List.of("Setup", "Training", "Support")) {
                XWPFParagraph composed = inACell(cell, item);

                assertThat(spacing(composed).getLineRule()).as(item).hasToString("exact");
                assertThat(line(composed)).as(item + ", as in the flow").isEqualTo(line(inTheBody(flow, item)));
            }
        }
    }

    @Test
    void aWrappedItemInACellTakesTheListsLineSpacingBetweenItsLines() throws Exception {
        // As in the flow: the gap goes into the item's line and comes off the space above it.
        String wrapped = "Training that runs long enough to wrap onto a second line of the cell";
        try (XWPFDocument document = inACellBeside(new ListBuilder().name("Items").bullet().itemSpacing(3).lineSpacing(2)
                .textStyle(size(9)).items("Setup", wrapped, "Support").build())) {
            XWPFParagraph one = inACell(document, "Setup");
            XWPFParagraph two = inACell(document, wrapped);
            XWPFParagraph three = inACell(document, "Support");

            assertThat(DocxTwips.of(spacing(two).getLine()) - DocxTwips.of(spacing(one).getLine()))
                    .as("2pt between its lines").isEqualTo(40);
            assertThat(before(two)).as("3pt above it, less the gap").isEqualTo(60 - 40);
            assertThat(before(three)).isEqualTo(60);
        }
    }

    @Test
    void aListAndAParagraphOfOneTextInATableEachTakeTheirOwnLines() throws Exception {
        // Matched in the order the table laid them out: neither takes the other's line.
        try (XWPFDocument document = inACellBeside(
                new ListBuilder().name("Amounts").noMarker().textStyle(size(16)).items("0.00").build(),
                new ParagraphBuilder().text("0.00").textStyle(size(8)).build())) {
            List<XWPFParagraph> amounts = cellParagraphs(document, "0.00");

            assertThat(amounts).hasSize(2);
            assertThat(line(amounts.get(0))).as("the list's, at 16pt").isEqualTo(2 * line(amounts.get(1)));
        }
        try (XWPFDocument document = inACellBeside(
                new ParagraphBuilder().text("0.00").textStyle(size(8)).build(),
                new ListBuilder().name("Amounts").noMarker().textStyle(size(16)).items("0.00").build())) {
            List<XWPFParagraph> amounts = cellParagraphs(document, "0.00");

            assertThat(amounts).hasSize(2);
            assertThat(line(amounts.get(1))).as("the list's, at 16pt").isEqualTo(2 * line(amounts.get(0)));
        }
    }

    @Test
    void aListsMarkerColumnInACellIsTheOneThePageSets() throws Exception {
        // Its marker set in a column of its own, the list's lines say how far its text stands past it.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            // One item: its marker is the only one the column is measured by.
            session.pageFlow(beside(new ListBuilder().name("Steps").bullet().hangingIndent(true).markerGap(8)
                    .items("Setup").build())::accept);
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject().getOrDefault("ListNode", List.of())).isEmpty();
        try (XWPFDocument flow = DocxExports.withLayout(400, 600, 20, page -> page.add(hanging()));
             XWPFDocument cell = inACellBeside(hanging())) {
            XWPFParagraph composed = inACell(cell, "Setup");

            assertThat(composed.getNumID()).as("the premise: a Word list's item").isNotNull();
            assertThat(hanging(cell, composed)).as("its level's column, as in the flow")
                    .isEqualTo(hanging(flow, inTheBody(flow, "Setup")));
        }
    }

    private static DocumentNode hanging() {
        return new ListBuilder().name("Steps").bullet().hangingIndent(true).markerGap(8).textStyle(size(9))
                .items("Setup", "Support").build();
    }

    /** The hanging indent of the numbering level an item is written at, in twips. */
    private static long hanging(XWPFDocument document, XWPFParagraph item) {
        var numbering = document.getNumbering();
        var abstractId = numbering.getNum(item.getNumID()).getCTNum().getAbstractNumId().getVal();
        return DocxTwips.of(numbering.getAbstractNum(abstractId).getCTAbstractNum().getLvlArray(0).getPPr().getInd()
                .getHanging());
    }

    @Test
    void aMarkdownParagraphLevelWithAListKeepsItsOwnLine() throws Exception {
        // The page reads the paragraph's markdown, so its line waits for the markdown pass after
        // the list is matched. A hanging list whose marker draws nothing lays out no line for it:
        // the line level before its item's, the paragraph's in the cell before, is not its marker.
        try (XWPFDocument document = inACellBeside(
                new ParagraphBuilder().text("**Languages**").textStyle(size(9)).build(),
                new ListBuilder().name("Items").noMarker().hangingIndent(true).textStyle(size(9))
                        .items("Python").build())) {
            assertThat(spacing(inACell(document, "Languages")).getLineRule()).hasToString("exact");
            assertThat(spacing(inACell(document, "Python")).getLineRule()).hasToString("exact");
        }
    }

    @Test
    void aListDoesNotTakeTheLineOfAParagraphWhoseTextEndsWithItsItem() throws Exception {
        // The paragraph is read as markdown and matched last; the list's item is its line's whole text.
        try (XWPFDocument document = inACellBeside(
                new ParagraphBuilder().text("**Paid** in full").textStyle(size(8)).build(),
                new ListBuilder().name("Terms").noMarker().textStyle(size(16)).items("in full").build())) {
            XWPFParagraph paragraph = cellParagraphs(document, "Paid in full").get(0);
            XWPFParagraph item = inACell(document, "in full");

            assertThat(line(item)).as("the list's own line, at 16pt").isEqualTo(2 * line(paragraph));
        }
    }

    @Test
    void aListWhoseItemsNoRunOfItsTablesLinesHoldsTakesNoneOfThem() throws Exception {
        // The page reads the second item as markdown, its marks dropped: the list's own lines do not
        // hold its items as written. A later cell's paragraph of its first item's text keeps its line.
        try (XWPFDocument document = inACellBeside(
                new ListBuilder().name("Items").bullet().textStyle(size(16)).items("Setup", "**Support**").build(),
                new ParagraphBuilder().text("Setup").textStyle(size(8)).build())) {
            List<XWPFParagraph> setups = cellParagraphs(document, "Setup");
            CTSpacing item = spacing(setups.get(0));

            assertThat(setups).hasSize(2);
            assertThat(item == null || !item.isSetLineRule()).as("the list's, matched to none").isTrue();
            assertThat(line(setups.get(1))).as("the paragraph's own, at 8pt").isLessThan(10);
        }
    }

    @Test
    void aHangingListWithABlankItemInACellIsLeftAsWordSetsIt() throws Exception {
        // The page draws a blank item's marker as a row of its own, which the export does not write:
        // as in the flow, the list's items are not matched to its lines.
        try (XWPFDocument document = inACellBeside(new ListBuilder().name("Items").bullet().hangingIndent(true)
                .textStyle(size(9)).items("Setup", "Support", " ").build())) {
            CTSpacing spacing = spacing(inACell(document, "Setup"));

            assertThat(spacing(inACell(document, "Mon")).getLineRule()).as("the premise: a layout behind the export")
                    .hasToString("exact");
            assertThat(spacing == null || !spacing.isSetLineRule()).isTrue();
        }
    }

    @Test
    void aListHoldingAnItemOfRunsInACellIsLeftAsWordSetsIt() throws Exception {
        // An item of runs is not read here: no run of the table's lines is known to be the list's.
        try (XWPFDocument document = inACellBeside(new ListBuilder().name("Items").noMarker().hangingIndent(true)
                .textStyle(size(9)).items("Setup")
                .addItem(text -> text.style("Support", size(9)))
                .build())) {
            CTSpacing spacing = spacing(inACell(document, "Setup"));

            assertThat(spacing(inACell(document, "Mon")).getLineRule()).as("the premise: a layout behind the export")
                    .hasToString("exact");
            assertThat(spacing == null || !spacing.isSetLineRule()).isTrue();
        }
    }

    private static DocumentNode list() {
        return new ListBuilder().name("Items").bullet().itemSpacing(3).textStyle(size(9))
                .items("Setup", "Training", "Support").build();
    }

    private static DocumentTextStyle size(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    /** A table of one row: a text cell, then a cell composed of each node. */
    private static XWPFDocument inACellBeside(DocumentNode... composed) throws Exception {
        return DocxExports.withLayout(400, 600, 20, beside(composed));
    }

    private static Consumer<PageFlowBuilder> beside(DocumentNode... composed) {
        return page -> page.addTable(table -> {
            DocumentTableColumn[] columns = new DocumentTableColumn[composed.length + 1];
            java.util.Arrays.fill(columns, DocumentTableColumn.fixed(160));
            columns[0] = DocumentTableColumn.fixed(40);
            List<DocumentTableCell> cells = new java.util.ArrayList<>();
            cells.add(DocumentTableCell.text("Mon"));
            for (DocumentNode node : composed) {
                cells.add(DocumentTableCell.node(node));
            }
            table.columns(columns).rowCells(cells);
        });
    }

    private static XWPFParagraph inTheBody(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph inACell(XWPFDocument document, String text) {
        List<XWPFParagraph> found = cellParagraphs(document, text);
        assertThat(found).as("a cell holding " + text).isNotEmpty();
        return found.get(0);
    }

    /** Every paragraph of a table's cell, at any depth, holding exactly {@code text}, in order. */
    private static List<XWPFParagraph> cellParagraphs(XWPFDocument document, String text) {
        List<XWPFParagraph> found = new java.util.ArrayList<>();
        java.util.ArrayDeque<XWPFTable> tables = new java.util.ArrayDeque<>(document.getTables());
        while (!tables.isEmpty()) {
            for (var row : tables.pop().getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            found.add(paragraph);
                        }
                    }
                    tables.addAll(cell.getTables());
                }
            }
        }
        return found;
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(spacing(paragraph).getLine()) / 20.0;
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }
}
