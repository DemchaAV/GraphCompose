package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table of one row kept with what follows may break across pages, so that LibreOffice keeps it
 * with what follows.
 *
 * <p>LibreOffice reads a one-row table whose row may not break as a table that may not split, and
 * keeps such a table with nothing: it reads keep-with-next off a table's last row, from the
 * paragraph its first cell opens with, only in a table that may split. Word keeps the row with
 * what follows either way.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxOneRowTableKeptWithTheNextTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aRowKeptWithTheNextMayBreakAndHoldsOntoIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepWithNext().addRow(row -> row.addParagraph("Northline").addParagraph("Proposal")))
                .addParagraph("Deliverables"))) {
            XWPFTable masthead = document.getTables().get(0);

            assertThat(masthead.getRows()).hasSize(1);
            assertThat(rowKeepsNext(masthead.getRow(0))).isTrue();
            assertThat(masthead.getRow(0).isCantSplitRow())
                    .as("LibreOffice keeps a one-row table with what follows only where its row may break")
                    .isFalse();
            assertThat(masthead.getCTTbl().xmlText()).doesNotContain("cantSplit");
        }
    }

    @Test
    void aRowNotKeptWithTheNextStaysWhole() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row.addParagraph("Northline").addParagraph("Proposal"))
                .addParagraph("Deliverables"))) {
            assertThat(document.getTables().get(0).getRow(0).isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aRowKeptTogetherAndNotWithTheNextStaysWhole() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepTogether().addRow(row -> row.addParagraph("Northline").addParagraph("Proposal")))
                .addParagraph("Deliverables"))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(rowKeepsNext(row)).as("the block ends with the row").isFalse();
            assertThat(row.isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aRowWhoseHeadingAloneIsKeptWithTheNextStaysWhole() throws Exception {
        // The heading is kept with the line under it in its column, which the row holds whole: kept
        // with the next in the cell it opens, it would keep the row with the paragraph after it in
        // Word, which reads the keep off the paragraph a cell opens with.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row
                        .addSection(column -> column
                                .addSection(heading -> heading.keepWithNext().addParagraph("Summary"))
                                .addParagraph("The project in brief."))
                        .addParagraph("Facts"))
                .addParagraph("Deliverables"))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(keepsNext(paragraph(row, "Summary"))).isFalse();
            assertThat(paragraph(row, "Summary").getCTP().getPPr().isSetKeepLines()).isTrue();
            assertThat(row.isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aTableOfTwoRowsKeptWithTheNextStaysWhole() throws Exception {
        // LibreOffice splits a table of more rows between them, and keeps its last with what follows.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepWithNext().addTable(t -> t.autoColumns(2).row("Net", "100").row("Tax", "20")))
                .addParagraph("Total"))) {
            XWPFTable table = document.getTables().get(0);

            assertThat(rowKeepsNext(table.getRow(1))).isTrue();
            assertThat(table.getRow(0).isCantSplitRow()).isTrue();
            assertThat(table.getRow(1).isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aKeptRowOpeningWithATableStaysWhole() throws Exception {
        // A painted row is a panel whose cell opens with the table of its columns: LibreOffice
        // reads no keep off a row whose first cell opens with a table, so freeing it keeps nothing.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepWithNext().addRow(row -> row.fillColor(SURFACE)
                        .addParagraph("Northline").addParagraph("Proposal")))
                .addParagraph("Deliverables"))) {
            XWPFTable panel = document.getTables().get(0);
            XWPFTableCell cell = panel.getRow(0).getCell(0);

            assertThat(cell.getBodyElements().get(0)).isInstanceOf(XWPFTable.class);
            assertThat(rowKeepsNext(panel.getRow(0))).isTrue();
            assertThat(panel.getRow(0).isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aKeptRowHoldingATableStaysWhole() throws Exception {
        // The block's keep chains a cell's own paragraphs, not the rows of a table in it: only
        // w:cantSplit holds those together.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepWithNext().addRow(row -> row
                        .addParagraph("Totals")
                        .addSection(column -> column.addTable(t -> t.autoColumns(2)
                                .row("Net", "100").row("Tax", "20").row("Total", "120")))))
                .addParagraph("Deliverables"))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getCell(0).getBodyElements().get(0)).isInstanceOf(XWPFParagraph.class);
            assertThat(row.getCell(1).getTables()).hasSize(1);
            assertThat(rowKeepsNext(row)).isTrue();
            assertThat(row.isCantSplitRow()).isTrue();
        }
    }

    @Test
    void aRowKeptWithTheNextInsideACellStaysWhole() throws Exception {
        // A table in a cell goes where its cell's row goes. Kept with the next, the paragraph the
        // card's cell opens with would keep the card with what follows it in Word.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(card -> card.fillColor(SURFACE)
                        .addSection(s -> s.keepWithNext().addRow(row -> row.addParagraph("Northline").addParagraph("Proposal")))
                        .addParagraph("Deliverables")))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            XWPFTableRow row = card.getTables().get(0).getRow(0);

            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    assertThat(keepsNext(paragraph)).as("nothing in the card keeps with the next").isFalse();
                }
            }
            assertThat(row.isCantSplitRow()).isTrue();
        }
    }

    private static XWPFParagraph paragraph(XWPFTableRow row, String text) {
        XWPFParagraph found = find(row, text);
        if (found == null) {
            throw new AssertionError("no paragraph reads " + text);
        }
        return found;
    }

    private static XWPFParagraph find(XWPFTableRow row, String text) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                if (paragraph.getText().equals(text)) {
                    return paragraph;
                }
            }
            for (XWPFTable nested : cell.getTables()) {
                for (XWPFTableRow inner : nested.getRows()) {
                    XWPFParagraph found = find(inner, text);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private static boolean keepsNext(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepNext();
    }

    private static boolean rowKeepsNext(XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                if (!keepsNext(paragraph)) {
                    return false;
                }
            }
        }
        return true;
    }
}
