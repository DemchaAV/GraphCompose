package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the layout keeps on one page, Word keeps on one page.
 *
 * <p>{@code keepTogether()} moves a block to the next page whole, and {@code keepWithNext()}
 * moves a block down with the first line of the one after it. Word re-paginates on its own,
 * so it has to be told the same thing — {@code w:keepLines} and {@code w:keepNext} — or a
 * card splits across its break and a heading is left behind at the foot of a page.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxKeepTogetherTest {

    @Test
    void aBlockKeptTogetherIsChainedIntoOneUnit() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepTogether()
                        .addParagraph(p -> p.text("Invoice"))
                        .addParagraph(p -> p.text("Due in 30 days"))
                        .addParagraph(p -> p.text("Thank you")))
                .addParagraph(p -> p.text("After")))) {
            XWPFParagraph first = paragraph(document, "Invoice");
            XWPFParagraph middle = paragraph(document, "Due in 30 days");
            XWPFParagraph last = paragraph(document, "Thank you");

            assertThat(keepsNext(first)).isTrue();
            assertThat(keepsNext(middle)).isTrue();
            assertThat(keepsNext(last))
                    .as("the block ends here, and is not tied to what follows it")
                    .isFalse();
            assertThat(keepsLines(first) && keepsLines(middle) && keepsLines(last)).isTrue();
            assertThat(keepsNext(paragraph(document, "After")) || keepsLines(paragraph(document, "After")))
                    .isFalse();
        }
    }

    @Test
    void aBlockKeptWithTheNextHoldsOntoIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepWithNext().addParagraph(p -> p.text("Experience")))
                .addParagraph(p -> p.text("Senior engineer, 2019 to now")))) {
            assertThat(keepsNext(paragraph(document, "Experience"))).isTrue();
            assertThat(keepsNext(paragraph(document, "Senior engineer, 2019 to now"))).isFalse();
        }
    }

    @Test
    void aBlockThatAsksForNothingIsFreeToBreak() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s
                        .addParagraph(p -> p.text("One"))
                        .addParagraph(p -> p.text("Two"))))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                assertThat(keepsNext(paragraph) || keepsLines(paragraph)).isFalse();
            }
        }
    }

    @Test
    void aBlockTallerThanAPageIsLeftToFlowAsTheLayoutLetsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .addSection(s -> {
                    s.keepTogether();
                    for (int index = 0; index < 20; index++) {
                        int line = index;
                        s.addParagraph(p -> p.text("Line " + line));
                    }
                }))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                assertThat(keepsNext(paragraph) || keepsLines(paragraph)).isFalse();
            }
        }
    }

    @Test
    void aTableInsideAKeptBlockIsChainedRowByRow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepTogether()
                        .addParagraph(p -> p.text("Totals"))
                        .addTable(t -> t.autoColumns(2).row("Net", "100").row("Tax", "20"))))) {
            XWPFTable table = document.getTables().get(0);

            assertThat(keepsNext(paragraph(document, "Totals"))).isTrue();
            assertThat(rowKeepsNext(table.getRow(0))).isTrue();
            assertThat(rowKeepsNext(table.getRow(1)))
                    .as("the table's last row ends the block")
                    .isFalse();
        }
    }

    @Test
    void withoutALayoutNothingIsKnownToBeOnOnePage() throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(595, 842, 36, page -> page
                .addSection(s -> s.keepTogether()
                        .addParagraph(p -> p.text("One"))
                        .addParagraph(p -> p.text("Two"))))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                assertThat(keepsNext(paragraph) || keepsLines(paragraph)).isFalse();
            }
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reads " + text));
    }

    private static boolean keepsNext(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepNext();
    }

    private static boolean keepsLines(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepLines();
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
