package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table cell that holds no letters — a row that is only a rule, its thickness an empty
 * cell's font — keeps the page's height in Word.
 *
 * <p>The page draws the rule's borders across the empty line; Word keeps a cell's borders
 * outside its content and grows the row to the line. {@code CobaltRota} opens and closes its
 * header with two such rows, and its whole sheet stood 1.8pt low in Word.</p>
 */
class DocxRuleRowTest {

    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(16, 32, 80), 0.9);

    /** The 0.9pt border, in twips. */
    private static final long BORDER = 18;

    @Test
    void aRuleRowsEmptyLineIsCutToTheRoomItsRowLeaves() throws Exception {
        try (XWPFDocument document = export(DocumentInsets.zero(), DocumentTextStyle.DEFAULT.withSize(2))) {
            XWPFTableRow rule = document.getTables().get(0).getRow(0);

            assertThat(line(rule)).as("the line stands inside the row").isEqualTo(rowHeight(rule));
        }
    }

    @Test
    void aLineWithLettersKeepsItsHeight() throws Exception {
        try (XWPFDocument document = export(DocumentInsets.zero(), DocumentTextStyle.DEFAULT.withSize(2))) {
            XWPFTableRow text = document.getTables().get(0).getRow(1);

            // The row is written less its rules, the last row's a rule and a half (the rule
            // below the table whole, half the one above it); the line keeps the page's height.
            assertThat(line(text)).as("its letters would be cut").isEqualTo(rowHeight(text) + BORDER + BORDER / 2);
        }
    }

    @Test
    void aPaddedBlankLineKeepsItsHeight() throws Exception {
        // The padding already gives up the room the border takes: the blank line fits as it is.
        try (XWPFDocument document = export(DocumentInsets.of(4), DocumentTextStyle.DEFAULT)) {
            var table = document.getTables().get(0);

            assertThat(line(table.getRow(0))).isEqualTo(line(table.getRow(1)));
        }
    }

    private static long rowHeight(XWPFTableRow row) {
        return DocxTwips.of(row.getCtRow().getTrPr().getTrHeightArray(0).getVal());
    }

    private static long line(XWPFTableRow row) {
        return DocxTwips.of(row.getCell(0).getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine());
    }

    private static XWPFDocument export(DocumentInsets padding, DocumentTextStyle blank) throws Exception {
        DocumentTableStyle rule = DocumentTableStyle.builder()
                .padding(padding)
                .stroke(RULE)
                .textStyle(blank)
                .lineSpacing(0)
                .build();
        DocumentTableStyle text = DocumentTableStyle.builder()
                .padding(padding)
                .stroke(RULE)
                .lineSpacing(0)
                .build();
        return DocxExports.withLayout(400, 300, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("").withStyle(rule), DocumentTableCell.text("").withStyle(rule))
                .rowCells(DocumentTableCell.text("Text").withStyle(text), DocumentTableCell.text("More").withStyle(text))));
    }
}
