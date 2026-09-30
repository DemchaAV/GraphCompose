package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The space above a block reaches Word where Word has nowhere to write it as the page does.
 *
 * <p>{@code PaymentsInvoice}'s header rule is pulled 16.4pt up out of the space its header
 * stack leaves below itself, and its metadata grid is padded 6.2pt down its column. Word has
 * no negative space above a paragraph and no space above a table: the rule and the page under
 * it stood 16.4pt low, and the grid 6.2pt high.</p>
 */
class DocxSpaceAboveTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();

    @Test
    void aRulePulledUpTakesItsPullOutOfTheSpaceOwedAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 20, 0)))
                .addLine(line -> line.horizontal(200).thickness(1).color(DocumentColor.BLACK)
                        .margin(new DocumentInsets(-8, 0, 0, 0))))) {
            XWPFParagraph rule = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null && paragraph.getCTP().getPPr().isSetPBdr())
                    .findFirst().orElseThrow();

            assertThat(DocxTwips.of(rule.getCTP().getPPr().getSpacing().getBefore()) / 20.0)
                    .as("20pt owed less the 8pt it is pulled up").isCloseTo(12, within(0.1));
        }
    }

    @Test
    void aParagraphPulledUpFurtherThanTheSpaceAboveItHasNoSpaceAbove() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 4, 0)))
                .addParagraph(p -> p.text("Pulled").textStyle(BODY).margin(new DocumentInsets(-10, 0, 0, 0))))) {
            XWPFParagraph pulled = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Pulled")).findFirst().orElseThrow();
            var spacing = pulled.getCTP().getPPr().getSpacing();

            assertThat(spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0L).isZero();
        }
    }

    @Test
    void aParagraphPulledUpInsideASectionPulledUpAddsToItsPull() throws Exception {
        // 10pt owed below the header, the section pulls up 4pt and its paragraph 3pt more: the
        // page sets the paragraph 3pt below the header's box.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 10, 0)))
                .addSection("Pulled", section -> section
                        .margin(new DocumentInsets(-4, 0, 0, 0))
                        .addParagraph(p -> p.text("Pulled").textStyle(BODY).margin(new DocumentInsets(-3, 0, 0, 0)))))) {
            XWPFParagraph pulled = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Pulled")).findFirst().orElseThrow();

            assertThat(DocxTwips.of(pulled.getCTP().getPPr().getSpacing().getBefore()) / 20.0)
                    .as("10 less 4 less 3").isCloseTo(3, within(0.1));
        }
    }

    @Test
    void aRowOpeningAPaddedColumnKeepsThePaddingAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Split", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Issuer").textStyle(BODY))
                        .addSection("Meta", cell -> cell
                                .padding(new DocumentInsets(6, 0, 0, 0))
                                .addRow("Grid", grid -> grid
                                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                                        .addParagraph(p -> p.text("Label").textStyle(BODY))
                                        .addParagraph(p -> p.text("Value").textStyle(BODY))))))) {
            XWPFTableCell meta = document.getTables().get(0).getRow(0).getCell(1);
            List<IBodyElement> content = meta.getBodyElements();
            int grid = indexOfTheFirstTable(content);

            assertThat(grid).as("something holds the space above the grid").isPositive();
            double above = 0;
            for (int i = 0; i < grid; i++) {
                var spacing = ((XWPFParagraph) content.get(i)).getCTP().getPPr().getSpacing();
                above += (spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0) / 20.0
                         + DocxTwips.of(spacing.getLine()) / 20.0;
            }
            assertThat(above).as("the column's 6pt, and a hairline").isCloseTo(6, within(0.2));
        }
    }

    private static int indexOfTheFirstTable(List<IBodyElement> content) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) instanceof XWPFTable) {
                return i;
            }
        }
        return -1;
    }
}
