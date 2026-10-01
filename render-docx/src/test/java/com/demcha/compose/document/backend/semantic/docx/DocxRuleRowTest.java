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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table row that is only a rule — an empty cell whose font sets its thickness — keeps the
 * page's height in Word.
 *
 * <p>The page draws the rule's borders across the empty line; Word keeps a cell's borders
 * outside its content and grows the row to the line. {@code CobaltRota} opens and closes its
 * header with two such rows, and its whole sheet stood 1.8pt low in Word.</p>
 */
class DocxRuleRowTest {

    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(16, 32, 80), 0.9);

    @Test
    void aRuleRowsEmptyLineIsCutToTheRoomItsRowLeaves() throws Exception {
        try (XWPFDocument document = export(DocumentTextStyle.DEFAULT.withSize(2))) {
            XWPFTableRow rule = document.getTables().get(0).getRow(0);
            long row = DocxTwips.of(rule.getCtRow().getTrPr().getTrHeightArray(0).getVal());
            CTSpacing spacing = rule.getCell(0).getParagraphs().get(0).getCTP().getPPr().getSpacing();

            assertThat(row).as("the row is written less its border").isPositive();
            assertThat(DocxTwips.of(spacing.getLine())).as("the line stands inside the row").isEqualTo(row);
        }
    }

    @Test
    void aLineWithLettersKeepsItsHeight() throws Exception {
        try (XWPFDocument document = export(DocumentTextStyle.DEFAULT.withSize(2))) {
            XWPFTableRow text = document.getTables().get(0).getRow(1);
            long row = DocxTwips.of(text.getCtRow().getTrPr().getTrHeightArray(0).getVal());
            CTSpacing spacing = text.getCell(0).getParagraphs().get(0).getCTP().getPPr().getSpacing();

            assertThat(DocxTwips.of(spacing.getLine())).as("its letters would be cut").isGreaterThan(row);
        }
    }

    private static XWPFDocument export(DocumentTextStyle thickness) throws Exception {
        DocumentTableStyle rule = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .stroke(RULE)
                .textStyle(thickness)
                .lineSpacing(0)
                .build();
        DocumentTableStyle tight = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .stroke(RULE)
                .lineSpacing(0)
                .build();
        return DocxExports.withLayout(400, 300, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("").withStyle(rule), DocumentTableCell.text("").withStyle(rule))
                .rowCells(DocumentTableCell.text("Text").withStyle(tight), DocumentTableCell.text("More").withStyle(tight))));
    }
}
