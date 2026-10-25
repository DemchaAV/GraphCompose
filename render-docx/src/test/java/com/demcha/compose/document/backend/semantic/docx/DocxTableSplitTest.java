package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A body table is split in two at the row a page starts with: the rows from it on go to a table of
 * their own, under copies of the header rows, a paragraph between the two.
 */
class DocxTableSplitTest {

    @Test
    void theRowsFromTheSplitRowOnGoToATableOfTheirOwnUnderTheHeaderRows() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Before");
            XWPFTable table = table(document, 1, 5);
            document.createParagraph().createRun().setText("After");

            XWPFParagraph between = DocxTableSplit.split(document, table, 3);

            List<IBodyElement> body = document.getBodyElements();
            assertThat(body).hasSize(5);
            assertThat(body.get(1)).isSameAs(table);
            assertThat(body.get(2)).isSameAs(between);
            assertThat(texts(table)).containsExactly("Header", "Row 1", "Row 2");
            XWPFTable second = (XWPFTable) body.get(3);
            assertThat(texts(second)).containsExactly("Header", "Row 3", "Row 4", "Row 5");
            assertThat(second.getRow(0).isRepeatHeader()).as("repeated on the pages it runs onto").isTrue();
            // The passes after it read the document's lists: the second part and the paragraph are in them.
            assertThat(document.getTables()).containsExactly(table, second);
            assertThat(document.getParagraphs()).element(1).isSameAs(between);

            try (XWPFDocument read = new XWPFDocument(new ByteArrayInputStream(bytes(document)))) {
                assertThat(read.getTables()).hasSize(2);
                assertThat(texts(read.getTables().get(1))).containsExactly("Header", "Row 3", "Row 4", "Row 5");
            }
        }
    }

    @Test
    void theRowAPageStartsWithIsTheFirstTheLayoutPlacedOnIt() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 1, 5);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row < 4 ? 0 : 1)).isEqualTo(4);
            assertThat(DocxTableSplit.rowStarting(table, 2, row -> row < 4 ? 0 : 1)).as("no row on page 3")
                    .isEqualTo(-1);
        }
    }

    @Test
    void aTableIsNotSplitWhereOnlyItsHeaderRowsStandAboveThatRow() throws Exception {
        // The first part would hold the header alone at the foot of the page.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 1, 5);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row == 0 ? 0 : 1)).isEqualTo(-1);
        }
    }

    @Test
    void aTableIsNotSplitWhereACellSpansRowsAcrossThatRow() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 1, 5);
            table.getRow(3).getCell(0).getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.RESTART);
            table.getRow(4).getCell(0).getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.CONTINUE);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row < 3 ? 0 : 1)).as("where the merge starts")
                    .isEqualTo(3);
        }
    }

    @Test
    void aTableIsNotSplitWhereAHeaderRowHoldsWhatTheFileHoldsOnce() throws Exception {
        // A bookmark's name marks one place, and a drawing's id one drawing: the copy would hold them twice.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable marked = table(document, 1, 5);
            marked.getRow(0).getCell(0).getParagraphs().get(0).getCTP().addNewBookmarkStart().setName("top");
            XWPFTable drawn = table(document, 1, 5);
            drawn.getRow(0).getCell(0).getParagraphs().get(0).createRun().getCTR().addNewDrawing();

            assertThat(DocxTableSplit.rowStarting(marked, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
            assertThat(DocxTableSplit.rowStarting(drawn, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
        }
    }

    @Test
    void aPanelsPartAfterACutCopiesItsHeaderRowsWithoutTheirDrawings() throws Exception {
        // A shape the export anchors in a header row once a cut is decided is the table's first
        // page's: the first part keeps it, and the copy of the row opening the next part has none.
        try (XWPFDocument written = new XWPFDocument()) {
            XWPFTable items = table(written, 1, 4);
            items.getRow(0).getCell(0).getParagraphs().get(0).createRun().getCTR().addNewDrawing();
            XWPFTable outer = written.createTable(1, 1);
            try (XmlCursor at = outer.getRow(0).getCell(0).getParagraphArray(0).getCTP().newCursor();
                 XmlCursor table = items.getCTTbl().newCursor()) {
                table.moveXml(at);
            }
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes(written)))) {
                XWPFTable panel = document.getTables().get(0);
                XWPFTable inner = panel.getRow(0).getCell(0).getTables().get(0);

                DocxTableSplit.PanelPart part = DocxTableSplit.splitInItsPanel(document, panel, inner, 3);

                XWPFTable rest = part.second().getRow(0).getCell(0).getTables().get(0);
                assertThat(texts(rest)).containsExactly("Header", "Row 3", "Row 4");
                assertThat(drawings(rest.getRow(0))).as("the copy of the header row").isZero();
                assertThat(drawings(inner.getRow(0))).as("the first part's header row").isEqualTo(1);
            }
        }
    }

    private static int drawings(XWPFTableRow row) {
        return row.getCtRow().selectPath(
                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:drawing").length;
    }

    @Test
    void aTableIsNotSplitWhereAHeaderRowHoldsANumberedItem() throws Exception {
        // Word numbers the copy on from the original: the page's "1." would read "2." there.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 1, 5);
            table.getRow(0).getCell(0).getParagraphs().get(0).setNumID(java.math.BigInteger.ONE);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
        }
    }

    @Test
    void aTableIsNotSplitWhereARowItMovesOrCopiesHoldsAField() throws Exception {
        // The export settles a page reference in the paragraph it wrote it in, once the body is written.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable moved = table(document, 1, 5);
            field(moved, 5);
            XWPFTable copied = table(document, 1, 5);
            field(copied, 0);
            XWPFTable kept = table(document, 1, 5);
            field(kept, 2);

            assertThat(DocxTableSplit.rowStarting(moved, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
            assertThat(DocxTableSplit.rowStarting(copied, 1, row -> row < 4 ? 0 : 1)).isEqualTo(-1);
            assertThat(DocxTableSplit.rowStarting(kept, 1, row -> row < 4 ? 0 : 1)).as("above the split")
                    .isEqualTo(4);
        }
    }

    @Test
    void aTableWithNoHeaderRowsIsSplitIntoTwoPartsOfItsOwnRows() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 0, 5);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row < 2 ? 0 : 1)).isEqualTo(2);
            DocxTableSplit.split(document, table, 2);

            assertThat(texts(table)).containsExactly("Row 1", "Row 2");
            assertThat(texts(document.getTables().get(1))).containsExactly("Row 3", "Row 4", "Row 5");
        }
    }

    private static void field(XWPFTable table, int row) {
        table.getRow(row).getCell(0).getParagraphs().get(0).createRun().getCTR().addNewInstrText()
                .setStringValue(" PAGEREF ");
    }

    @Test
    void aTableIsNotSplitAtARowAfterOneTheLayoutDidNotPlace() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 1, 5);

            assertThat(DocxTableSplit.rowStarting(table, 1, row -> row == 2 ? -1 : row < 4 ? 0 : 1)).isEqualTo(-1);
        }
    }

    @Test
    void theHeaderRowsAreTheLeadingRowsWordRepeats() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = table(document, 2, 4);
            table.getRow(4).setRepeatHeader(true);

            assertThat(DocxTableSplit.headerRows(table)).isEqualTo(2);
        }
    }

    /** A table of so many header rows, then rows "Row 1" on. */
    private static XWPFTable table(XWPFDocument document, int header, int rows) {
        XWPFTable table = document.createTable(header + rows, 1);
        for (int index = 0; index < header + rows; index++) {
            XWPFTableRow row = table.getRow(index);
            row.getCell(0).setText(index < header ? "Header" : "Row " + (index - header + 1));
            row.setRepeatHeader(index < header);
        }
        return table;
    }

    private static List<String> texts(XWPFTable table) {
        return table.getRows().stream().map(row -> row.getCell(0).getText()).toList();
    }

    private static byte[] bytes(XWPFDocument document) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.write(out);
            return out.toByteArray();
        }
    }
}
