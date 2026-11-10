package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A ruled table's rows are held at their content's height in Word, the first and the last as
 * well as those between.
 *
 * <p>Word reads a row's written height as its cells' content and grows the row by their margins
 * and its rules: half of a rule between two rows, and the rule above the table and the one below
 * it whole. Measured, a table of four rows ruled at 0.75pt, held at the page's height less one
 * rule, stood 0.46pt taller in its first row and 0.36pt in its last. {@code EditorialProposal}'s
 * timeline and investment tables each stood that much taller in Word, and the page under them
 * that much low.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRuledRowHeightTest {

    @Test
    void everyRowOfATableRuledAlikeIsHeldAtItsContentsHeight() throws Exception {
        try (XWPFDocument document = table(3, 1.5)) {
            var table = document.getTables().get(0);
            long content = line(table.getRow(1));

            assertThat((long) table.getRow(1).getHeight()).as("a row between two others: its line").isEqualTo(content);
            assertThat((long) table.getRow(0).getHeight()).as("the first row, its rule and a half given").isEqualTo(content);
            assertThat((long) table.getRow(2).getHeight()).as("the last row, its rule and a half given").isEqualTo(content);
        }
    }

    @Test
    void everyRowOfATableRuledAtAnOddNumberOfTwipsOrAHalfOneIsHeldAtItsContentsHeight() throws Exception {
        // Five, six and seven eighths of a point: 12.5, 15 and 17.5 twips, which a row counts as 12,
        // 15 and 17, a rule between two rows shared 6 and 6, 7 and 8, 8 and 9. Halved and each
        // half rounded, a rule of 15 twips came off as 7 and 7, and its rows were held a twip short.
        for (double rule : new double[] {0.625, 0.7, 0.875}) {
            try (XWPFDocument document = table(3, rule)) {
                var table = document.getTables().get(0);
                long content = line(table.getRow(1));

                for (int i = 0; i < 3; i++) {
                    assertThat((long) table.getRow(i).getHeight()).as("row %s at a %spt rule", i, rule)
                            .isEqualTo(content);
                }
            }
        }
    }

    @Test
    void aTablesRowIsHeldAtThePagesHeightToTheNearestTwipAsItsLineIs() throws Exception {
        // Only a row inside a row that holds its own height is rounded down: a table's own row,
        // unpadded and unruled, is held at its line, both rounded to the nearest twip once.
        for (double size : new double[] {9.3, 10.7, 11.9, 13.1, 14.3}) {
            try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.auto())
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.zero())
                            .stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                            .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(size))
                            .lineSpacing(0)
                            .build())
                    .row("Row")))) {
                XWPFTableRow row = document.getTables().get(0).getRow(0);

                assertThat((long) row.getHeight()).as("at %spt", size).isEqualTo(line(row));
            }
        }
    }

    @Test
    void aOneRowTableGivesBothItsRulesWhole() throws Exception {
        // A 0.75pt rule is 15 twips: its halves are not floored away, the two adding to the rule.
        for (double rule : new double[] {1.5, 0.75}) {
            try (XWPFDocument alone = table(1, rule)) {
                XWPFTableRow row = alone.getTables().get(0).getRow(0);

                assertThat((long) row.getHeight()).as("its line, at a %spt rule", rule).isEqualTo(line(row));
            }
        }
    }

    @Test
    void aHeaderRuledHeavierThanItsRowsGivesHalfTheirRuleNotItsOwn() throws Exception {
        // Word gives the edge under the header to the lower row's 0.5pt rule, half to each: the
        // header carries its own 1.5pt above and 0.25pt below.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5)).build())
                .rowStyle(0, DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, 1.5)).build())
                .row("Header")
                .row("Body")
                .row("Body")))) {
            XWPFTableRow header = document.getTables().get(0).getRow(0);

            assertThat((long) header.getHeight()).as("its line").isEqualTo(line(header));
        }
    }

    @Test
    void aRowRuledUnlikeTheNextIsHeldAtItsContentsHeight() throws Exception {
        // A row between two others gives the rest of its own rule above it and half the next row's
        // below. Measured, a row ruled at 0.5pt over one ruled at 1.5pt, held less its own rule
        // alone, stood half a point taller in Word, and all under it half a point lower; the row
        // ruled at 1.5pt was held half a point short, its content holding it. Either way round, in
        // the middle of five rows.
        for (double[] rules : new double[][] {{0.5, 1.5}, {1.5, 0.5}}) {
            try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> {
                t.columns(DocumentTableColumn.auto())
                        .defaultCellStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(7))
                                .stroke(DocumentStroke.of(DocumentColor.BLACK, rules[0]))
                                .build())
                        .rowStyle(2, DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, rules[1])).build());
                for (int i = 0; i < 5; i++) {
                    t.row("Row");
                }
            }))) {
                var table = document.getTables().get(0);
                long content = line(table.getRow(0));

                for (int i = 0; i < 5; i++) {
                    assertThat((long) table.getRow(i).getHeight()).as("row %s, ruled %s with %s in the middle", i,
                            rules[0], rules[1]).isEqualTo(content);
                }
            }
        }
    }

    @Test
    void aTableRuledColumnByColumnHoldsEveryRowAsOneRuledAlike() throws Exception {
        // Each cell gives half the rule of the cell under it, not of the heaviest in the next row:
        // a column ruled at 1.5pt beside one at 0.5pt, alike from row to row, holds its rows where
        // a table ruled alike does.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> {
            t.columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.of(7))
                            .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))
                            .build())
                    .columnStyle(1, DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, 1.5)).build());
            for (int i = 0; i < 4; i++) {
                t.row("Row", "Row");
            }
        }))) {
            var table = document.getTables().get(0);
            long content = line(table.getRow(0));

            for (int i = 0; i < 4; i++) {
                assertThat((long) table.getRow(i).getHeight()).as("row %s", i).isEqualTo(content);
            }
        }
    }

    @Test
    void aCellAfterASpanGivesHalfTheRuleOfTheCellUnderIt() throws Exception {
        // The second row's last cell starts the third column of the grid, past a cell spanning two:
        // the cell under it is the third column's, ruled at 0.5pt, not the second's at 1.5pt. The
        // spanning cell takes the rule under its first column, at 0.5pt, as its padding gives up
        // half of it (clearOfTheRules).
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))
                        .build())
                .columnStyle(1, DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, 1.5)).build())
                .row("Row", "Row", "Row")
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.text("Row").colSpan(2),
                        com.demcha.compose.document.table.DocumentTableCell.text("Row"))
                .row("Row", "Row", "Row")
                .row("Row", "Row", "Row")))) {
            XWPFTableRow spanned = document.getTables().get(0).getRow(1);

            assertThat((long) spanned.getHeight()).isEqualTo(line(spanned));
        }
    }

    @Test
    void aRowRuledCellByCellIsHeldLessWhatItsMostDemandingCellTakes() throws Exception {
        // Cells ruled 1.5pt and 0.5pt over cells ruled the other way round: each gives 20 twips of
        // rules and keeps 250 of margins, its other 10 of padding moved into its paragraph. The
        // row is held less 270, the most any one cell takes, not the 280 the heavier cell's rule
        // counts and the 10 more the lighter one holds beyond its own, taken from different cells.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))
                        .build())
                .row("Row", "Row")
                .rowCells(ruled("Row", 1.5), ruled("Row", 0.5))
                .rowCells(ruled("Row", 0.5), ruled("Row", 1.5))
                .row("Row", "Row")))) {
            XWPFTableRow row = document.getTables().get(0).getRow(1);

            assertThat((long) row.getHeight()).as("its line and the padding in its paragraphs").isEqualTo(line(row) + 10);
        }
    }

    private static com.demcha.compose.document.table.DocumentTableCell ruled(String text, double rule) {
        return com.demcha.compose.document.table.DocumentTableCell.text(text)
                .withStyle(DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, rule)).build());
    }

    @Test
    void aRowItsRulesFillIsStillHeldAtATwip() throws Exception {
        // A one-row table no taller than its two 1pt rules: the height left for its content is
        // nothing, and is written all the same, at a twip — so a blank line in such a row is still
        // cut to the room it leaves (fitBlankLinesToTheRow) rather than left at its font's height.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.zero())
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(1.5))
                        .lineSpacing(0)
                        .build())
                .row("x")))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);

            assertThat(row.getHeight()).as("held at a twip").isEqualTo(1);
        }
    }

    private static long line(XWPFTableRow row) {
        return DocxTwips.of(row.getCell(0).getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine());
    }

    private static XWPFDocument table(int rows, double rule) throws Exception {
        return DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> {
            t.columns(DocumentTableColumn.auto())
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.of(7))
                            .stroke(DocumentStroke.of(DocumentColor.BLACK, rule))
                            .build());
            for (int i = 0; i < rows; i++) {
                t.row("Row");
            }
        }));
    }
}
