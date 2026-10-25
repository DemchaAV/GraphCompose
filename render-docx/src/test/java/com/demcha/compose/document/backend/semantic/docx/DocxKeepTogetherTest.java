package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import java.util.ArrayList;
import java.util.List;

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
    void aStackOfOneLayerIsChainedIntoOneUnit() throws Exception {
        // The layout moves a stack whole. Measured: a stack of three lines the page moved to a
        // new page stood with two of them at the foot of the page above in Word and LibreOffice.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addLayerStack(stack -> stack.topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                        .addParagraph("Opening").addParagraph("Body").addParagraph("Tail").build()))
                .addParagraph(p -> p.text("After")))) {
            assertThat(keepsNext(paragraph(document, "Opening")) && keepsNext(paragraph(document, "Body"))).isTrue();
            assertThat(keepsNext(paragraph(document, "Tail")))
                    .as("the stack ends here, and is not tied to what follows it").isFalse();
            assertThat(keepsLines(paragraph(document, "Opening")) && keepsLines(paragraph(document, "Tail"))).isTrue();
            assertThat(keepsNext(paragraph(document, "After")) || keepsLines(paragraph(document, "After"))).isFalse();
        }
    }

    @Test
    void aRowInAStackOfOneLayerIsKeptAsInABlockKeptTogether() throws Exception {
        // The same keeps, row by row, and the same one-row table as a section kept together holds.
        java.util.function.Function<Boolean, java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder>>
                content = inAStack -> page -> {
                    com.demcha.compose.document.node.DocumentNode body = new com.demcha.compose.document.dsl.SectionBuilder()
                            .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                            .addParagraph("Under").build();
                    if (inAStack) {
                        page.addLayerStack(stack -> stack.topLeft(body));
                    } else {
                        page.addSection(section -> section.keepTogether().add(body));
                    }
                    page.addParagraph("After");
                };
        try (XWPFDocument stacked = DocxExports.withLayout(595, 842, 36, content.apply(true));
             XWPFDocument kept = DocxExports.withLayout(595, 842, 36, content.apply(false))) {
            XWPFTableRow stackedRow = stacked.getTables().get(0).getRow(0);
            XWPFTableRow keptRow = kept.getTables().get(0).getRow(0);

            assertThat(rowKeepsNext(stackedRow)).as("the row kept with what follows it in the stack").isTrue();
            assertThat(stackedRow.isCantSplitRow()).isEqualTo(keptRow.isCantSplitRow());
            assertThat(keepsNext(paragraph(stacked, "Under"))).isFalse();
        }
    }

    @Test
    void aStackOfOneLayerInARowsCellIsLeftToItsRow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row
                        .add(new com.demcha.compose.document.dsl.LayerStackBuilder()
                                .topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                                        .addParagraph("Opening").addParagraph("Body").build()).build())
                        .addParagraph("Beside")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                assertThat(keepsNext(paragraph)).as("a cell of a row Word keeps on its own terms").isFalse();
            }
        }
    }

    @Test
    void aBandIsChainedIntoOneUnit() throws Exception {
        // A stack of two layers is written as a band, one layer after the other. Measured: one the
        // page moved to a new page stood with its first layer at the foot of the page above in
        // Word and LibreOffice.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addLayerStack(stack -> stack
                        .topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                                .addParagraph("Opening").addParagraph("Body").build())
                        .bottomRight(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Caption").build()))
                .addParagraph(p -> p.text("After")))) {
            assertThat(keepsNext(paragraph(document, "Opening")) && keepsNext(paragraph(document, "Body")))
                    .as("the first layer kept with the second").isTrue();
            assertThat(keepsNext(paragraph(document, "Caption")))
                    .as("the band ends here, and is not tied to what follows it").isFalse();
            assertThat(keepsLines(paragraph(document, "Opening")) && keepsLines(paragraph(document, "Caption")))
                    .isTrue();
            assertThat(keepsNext(paragraph(document, "After")) || keepsLines(paragraph(document, "After"))).isFalse();
        }
    }

    @Test
    void aShapeContainerIsChainedIntoOneUnit() throws Exception {
        // The layout moves a shape container whole too. Measured: one of three lines the page moved
        // to a new page stood with two of them at the foot of the page above in Word and LibreOffice.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .add(new com.demcha.compose.document.dsl.ShapeContainerBuilder().rectangle(200, 45)
                        .topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                                .addParagraph("Opening").addParagraph("Body").addParagraph("Tail").build())
                        .build())
                .addParagraph(p -> p.text("After")))) {
            assertThat(keepsNext(paragraph(document, "Opening")) && keepsNext(paragraph(document, "Body"))).isTrue();
            assertThat(keepsNext(paragraph(document, "Tail"))).isFalse();
            assertThat(keepsLines(paragraph(document, "Tail"))).isTrue();
        }
    }

    @Test
    void aBandInARowsCellIsLeftToItsRow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row
                        .add(new com.demcha.compose.document.dsl.LayerStackBuilder()
                                .topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                                        .addParagraph("Opening").addParagraph("Body").build())
                                .bottomRight(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Caption").build())
                                .build())
                        .addParagraph("Beside")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(cell.getParagraphs()).extracting(XWPFParagraph::getText).contains("Opening", "Caption");
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                assertThat(keepsNext(paragraph) || keepsLines(paragraph))
                        .as("a cell of a row Word keeps on its own terms").isFalse();
            }
        }
    }

    @Test
    void aBlockKeptTogetherInARowsCellKeepsItsLinesAndNothingWithTheNext() throws Exception {
        // Word reads keep-with-next on the paragraph a cell opens with as keeping the row with what
        // follows the table; the row, which may not split, keeps the block whole.
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row
                        .addSection(s -> s.keepTogether().addParagraph("Opening").addParagraph("Body"))
                        .addParagraph("Beside"))
                .addParagraph(p -> p.text("After")))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            List<XWPFParagraph> kept = row.getCell(0).getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getText().isEmpty()).toList();

            assertThat(kept).extracting(XWPFParagraph::getText).containsExactly("Opening", "Body");
            for (XWPFParagraph paragraph : kept) {
                assertThat(keepsNext(paragraph)).as("kept with nothing in its cell").isFalse();
                assertThat(keepsLines(paragraph)).as("its lines kept together").isTrue();
            }
            assertThat(row.isCantSplitRow()).isTrue();
        }
    }

    @Test
    void cardsKeptTogetherInARowsCellsKeepNothingWithWhatFollowsTheRow() throws Exception {
        // ObsidianInvoice's info cards: kept with the next, the paragraph each card's cell opens with
        // kept the row with the footer divider the layout set on the next page, and Word set the
        // cards there too, 17 lines below the page's (measured, Word 16).
        DocumentColor surface = DocumentColor.LIGHT_GRAY;
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addRow(row -> row
                        .addSection(card -> card.keepTogether().fillColor(surface).padding(DocumentInsets.of(6))
                                .addParagraph("Notes").addParagraph("Thank you"))
                        .addSection(card -> card.keepTogether().fillColor(surface).padding(DocumentInsets.of(6))
                                .addParagraph("Payment")
                                .addSection(fields -> fields.addParagraph("Payee").addParagraph("Kestrel"))))
                .addParagraph(p -> p.text("After")))) {
            XWPFTable row = document.getTables().get(0);
            List<CTP> inside = paragraphsIn(row);

            assertThat(inside).extracting(DocxKeepTogetherTest::textOf)
                    .contains("Notes", "Thank you", "Payment", "Payee", "Kestrel");
            for (CTP paragraph : inside) {
                assertThat(paragraph.isSetPPr() && paragraph.getPPr().isSetKeepNext())
                        .as("a paragraph in the cards' row keeps nothing with the next").isFalse();
                if (!textOf(paragraph).isEmpty()) {
                    // Laid out, and kept: the cards' lines are kept together.
                    assertThat(paragraph.getPPr().isSetKeepLines()).as("%s keeps its lines", textOf(paragraph)).isTrue();
                }
            }
        }
    }

    @Test
    void withoutALayoutAStackIsNotKnownToBeOnOnePage() throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(595, 842, 36, page -> page
                .addLayerStack(stack -> stack.topLeft(new com.demcha.compose.document.dsl.SectionBuilder()
                        .addParagraph("Opening").addParagraph("Body").build())))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                assertThat(keepsNext(paragraph) || keepsLines(paragraph)).isFalse();
            }
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

    /** Every paragraph a table holds, at any depth. */
    private static List<CTP> paragraphsIn(XWPFTable table) {
        List<CTP> paragraphs = new ArrayList<>();
        for (org.apache.xmlbeans.XmlObject found : table.getCTTbl().selectPath(
                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:p")) {
            paragraphs.add((CTP) found);
        }
        return paragraphs;
    }

    private static String textOf(CTP paragraph) {
        try (org.apache.xmlbeans.XmlCursor cursor = paragraph.newCursor()) {
            return cursor.getTextValue();
        }
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
