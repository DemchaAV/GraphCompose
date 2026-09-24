package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table cell's {@code lineSpacing} reaches Word as space between its lines.
 *
 * <p>The layout sizes the row as {@code lines × lineHeight + (lines − 1) × lineSpacing}.
 * Word has no space between the lines of one paragraph but a taller line, which adds it
 * above the first line too; so a cell whose lines stand apart is a paragraph per line, the
 * spacing after each but the last. A cell without spacing keeps one paragraph and line
 * breaks.</p>
 */
class DocxTableCellLineSpacingTest {

    @Test
    void aCellWithLineSpacingIsAParagraphPerLineWithTheSpacingBetweenThem() throws Exception {
        try (XWPFDocument document = export(6)) {
            List<XWPFParagraph> lines = onlyCell(document).getParagraphs();

            assertThat(lines).extracting(XWPFParagraph::getText).containsExactly("Alpha", "Bravo", "Charlie");
            assertThat(afterTwips(lines.get(0))).isEqualTo(120);
            assertThat(afterTwips(lines.get(1))).isEqualTo(120);
            assertThat(afterTwips(lines.get(2))).as("nothing after the last line").isZero();
            for (XWPFParagraph line : lines) {
                assertThat(spacing(line).getLineRule())
                        .as("each line the height the row was sized with")
                        .isEqualTo(STLineSpacingRule.EXACT);
            }
        }
    }

    @Test
    void aCellWithoutLineSpacingKeepsOneParagraphAndLineBreaks() throws Exception {
        try (XWPFDocument document = export(0)) {
            List<XWPFParagraph> lines = onlyCell(document).getParagraphs();

            assertThat(lines).hasSize(1);
            assertThat(lines.get(0).getRuns().get(0).getCTR().sizeOfBrArray()).isEqualTo(2);
        }
    }

    private static XWPFDocument export(double lineSpacing) throws Exception {
        return DocxExports.withLayout(400, 600, 20, page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(200))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(4))
                        .lineSpacing(lineSpacing)
                        .build())
                .rowCells(DocumentTableCell.lines("Alpha", "Bravo", "Charlie"))));
    }

    private static XWPFTableCell onlyCell(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        return document.getTables().get(0).getRow(0).getCell(0);
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long afterTwips(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0;
    }
}
