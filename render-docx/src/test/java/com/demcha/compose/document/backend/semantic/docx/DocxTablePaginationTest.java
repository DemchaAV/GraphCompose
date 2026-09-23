package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table breaks across Word's pages where the layout breaks it, and nowhere else.
 *
 * <p>The layout splits a table only between rows, repeats its header rows on every page it
 * continues on, and never leaves a header at the foot of a page by itself. Word holds each
 * of those — {@code w:cantSplit}, {@code w:tblHeader}, keep-with-next — and the export
 * wrote none of them.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxTablePaginationTest {

    @Test
    void aRowTheLayoutKeepsWholeIsKeptWholeInWord() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page.addTable(t -> t
                .autoColumns(2)
                .row("Item", "Amount")
                .row("Platform subscription", "1 440.00")
                .row("Support", "240.00")))) {
            for (XWPFTableRow row : onlyTable(document).getRows()) {
                assertThat(row.isCantSplitRow()).isTrue();
            }
        }
    }

    @Test
    void aRepeatedHeaderRepeatsAndStaysWithTheRowUnderIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page.addTable(t -> t
                .autoColumns(2)
                .header("Stage", "Owner")
                .header("Week", "Team")
                .repeatHeader(2)
                .row("Discovery", "Research")
                .row("Delivery", "Engineering")))) {
            XWPFTable table = onlyTable(document);

            assertThat(table.getRow(0).isRepeatHeader()).isTrue();
            assertThat(table.getRow(1).isRepeatHeader()).isTrue();
            assertThat(table.getRow(2).isRepeatHeader()).isFalse();
            assertThat(keepsWithNext(table.getRow(0))).isTrue();
            assertThat(keepsWithNext(table.getRow(1)))
                    .as("the last header row stays with the first body row")
                    .isTrue();
            assertThat(keepsWithNext(table.getRow(2)))
                    .as("a body row is free to end a page")
                    .isFalse();
        }
    }

    @Test
    void aTableWithNoRepeatedHeaderHasNoHeaderRow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page.addTable(t -> t
                .autoColumns(2)
                .header("Stage", "Owner")
                .row("Discovery", "Research")))) {
            for (XWPFTableRow row : onlyTable(document).getRows()) {
                assertThat(row.isRepeatHeader()).isFalse();
                assertThat(keepsWithNext(row)).isFalse();
            }
        }
    }

    @Test
    void aTableThatIsAllHeaderKeepsItsLastRowFreeOfWhatFollows() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addTable(t -> t.autoColumns(1).header("Only").repeatHeader())
                .addParagraph(p -> p.text("After")))) {
            XWPFTableRow row = onlyTable(document).getRow(0);

            assertThat(row.isRepeatHeader()).isTrue();
            assertThat(keepsWithNext(row))
                    .as("with no body row under it, keep-with-next would tie it to the paragraph")
                    .isFalse();
        }
    }

    @Test
    void aTableTooTallToLayOutIsLeftFreeToBreak() throws Exception {
        // The layout refuses a row taller than the page, so this document is exported without
        // one — and a row no page can hold has to stay free to break in Word.
        String[] lines = new String[30];
        java.util.Arrays.fill(lines, "Line");
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                .row("Short", "Short")
                .rowCells(DocumentTableCell.text("Notes"), DocumentTableCell.lines(lines))))) {
            for (XWPFTableRow row : onlyTable(document).getRows()) {
                assertThat(row.isCantSplitRow()).isFalse();
            }
        }
    }

    @Test
    void withoutALayoutNoRowIsMeasuredSoNoneIsHeldWhole() throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(595, 842, 36, page -> page.addTable(t -> t
                .autoColumns(2)
                .header("Stage", "Owner")
                .repeatHeader()
                .row("Discovery", "Research")))) {
            XWPFTable table = onlyTable(document);

            assertThat(table.getRow(0).isCantSplitRow()).isFalse();
            assertThat(table.getRow(0).isRepeatHeader())
                    .as("the header is the document's, not the layout's")
                    .isTrue();
        }
    }

    @Test
    void aRowOfBlocksIsKeptWholeAsTheLayoutKeepsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(r -> r
                        .addParagraph(p -> p.text("Left"))
                        .addParagraph(p -> p.text("Right"))))) {
            assertThat(onlyTable(document).getRow(0).isCantSplitRow()).isTrue();
        }
    }

    private static XWPFTable onlyTable(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        return document.getTables().get(0);
    }

    private static boolean keepsWithNext(XWPFTableRow row) {
        boolean any = false;
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                boolean keeps = paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepNext();
                if (!keeps) {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }
}
