package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The space a container puts between its children reaches Word.
 *
 * <p>The layout puts a section's {@code spacing} between every two neighbouring children, and
 * the export wrote none of it: a CV sidebar laid out with 9pt between its blocks came out with
 * its blocks touching. It is written as the space below one child and above the next. A spacer
 * is space of the same kind, and is written as its height alone.</p>
 */
class DocxContainerSpacingTest {

    private static final long NINE_POINTS = 9 * 20L;

    @Test
    void aSectionsSpacingStandsBetweenEachTwoOfItsChildren() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Sidebar", sidebar -> sidebar
                        .spacing(9)
                        .addParagraph("Contact")
                        .addParagraph("Skills")
                        .addParagraph("Languages")))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(paragraphs).extracting(XWPFParagraph::getText)
                    .containsExactly("Contact", "Skills", "Languages");
            assertThat(before(paragraphs.get(0))).as("nothing above the first child").isZero();
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
            assertThat(before(paragraphs.get(2))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void theSpacingAboveATableIsHeldBelowTheParagraphBeforeIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Entry", entry -> entry
                        .spacing(9)
                        .addParagraph("Summary")
                        .addTable(t -> t.autoColumns(2).row("Title", "2024"))))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(after(paragraphs.get(0))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aPanelsSpacingStandsBetweenTheChildrenInItsCell() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card
                        .spacing(9)
                        .fillColor(DocumentColor.rgb(240, 240, 240))
                        .padding(DocumentInsets.of(6))
                        .addParagraph("First")
                        .addParagraph("Second")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            List<XWPFParagraph> paragraphs = written(cell.getParagraphs());

            assertThat(paragraphs).extracting(XWPFParagraph::getText).containsExactly("First", "Second");
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aPageBreakTakesNoSpacingBeforeItAndTheNextPageStartsItsSpacingDown() throws Exception {
        // The layout ends the page at the break, and places the next child the section's
        // spacing below the top of the next page.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Pages", pages -> pages
                        .spacing(9)
                        .addParagraph("Last on the first page")
                        .addPageBreak(b -> b.name("Break"))
                        .addParagraph("First on the second page")))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(after(paragraphs.get(0))).isZero();
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aSpacerIsItsHeightAndNotALineOfTextAsWell() throws Exception {
        // Between two CV entries held apart by a spacer, the empty paragraph that carries the
        // spacer's height was a line of text tall on top of it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("First entry")
                .addSpacer(spacer -> spacer.name("Gap").width(100).height(4.5))
                .addParagraph("Second entry"))) {
            List<XWPFParagraph> all = document.getParagraphs();
            XWPFParagraph spacer = all.get(1);

            assertThat(spacer.getText()).isEmpty();
            var spacing = spacer.getCTP().getPPr().getSpacing();
            assertThat(spacing != null && spacing.isSetLineRule() && spacing.isSetLine())
                    .as("the spacer's line is held to a hairline").isTrue();
            assertThat(spacing.getLineRule().toString()).isEqualTo("exact");
            assertThat(DocxTwips.of(spacing.getLine())).isLessThanOrEqualTo(2L);
            assertThat(before(all.get(2))).as("the spacer's height, above the next entry").isEqualTo(90L);
        }
    }

    @Test
    void theBordersOfTwoCardsComeOutOfTheSpaceBetweenThem() throws Exception {
        // Word draws a row's top and bottom borders outside its shading, where the page strokes
        // them on the card's edge: written whole, the space between two outlined cards came out
        // their borders wider, and an invoice of three such cards ran onto a second page.
        long filled = spaceBetweenTwoCards(null);
        long outlined = spaceBetweenTwoCards(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2));

        assertThat(filled).as("filled cards: the section's spacing, less the separator's hairline")
                .isBetween(12 * 20L - 2, 12 * 20L);
        assertThat(filled - outlined)
                .as("the second card's top border and the first one's bottom border come out of it")
                .isEqualTo(4 * 20L);
    }

    @Test
    void aTableOpeningTheDocumentKeepsTheSpaceAboveIt() throws Exception {
        // Word has no space above a table: with no paragraph before it, the section's top
        // padding was lost and ClassicInvoice's header row stood against the paper's edge.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 0, page -> page
                .addSection("Page", body -> body.padding(new DocumentInsets(18, 0, 0, 0))
                        .addRow(row -> row.addParagraph("Studio").addParagraph("INVOICE"))))) {
            List<IBodyElement> body = document.getBodyElements();

            assertThat(body.get(0)).as("a paragraph before the table carries the space")
                    .isInstanceOf(XWPFParagraph.class);
            assertThat(before((XWPFParagraph) body.get(0))).isEqualTo(18 * 20L);
            assertThat(body.get(1)).isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void aTableAfterAPageBreakKeepsTheSpaceAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 0, page -> page
                .addParagraph("First page")
                .addPageBreak(b -> b.name("Break"))
                .addSection("Second", body -> body.padding(new DocumentInsets(18, 0, 0, 0))
                        .addRow(row -> row.addParagraph("Studio").addParagraph("INVOICE"))))) {
            List<IBodyElement> body = document.getBodyElements();
            int table = body.indexOf(document.getTables().get(0));

            assertThat(body.get(table - 1)).as("a paragraph after the break carries the space")
                    .isInstanceOf(XWPFParagraph.class);
            assertThat(before((XWPFParagraph) body.get(table - 1))).isGreaterThanOrEqualTo(18 * 20L);
        }
    }

    @Test
    void aTableOpeningACellIsWrittenAsBefore() throws Exception {
        // Only the body takes the paragraph: in a cell it was measured to set content lower
        // than the page does — each of ObsidianInvoice's line items 8.5pt lower.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 0, page -> page
                .addRow(outer -> outer
                        .addSection(cell -> cell.padding(new DocumentInsets(12, 0, 0, 0))
                                .addRow(inner -> inner.addParagraph("Qty").addParagraph("1")))
                        .addParagraph("Notes")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(cell.getBodyElements().get(0)).as("the nested table opens the cell")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void aCardsBottomBorderComesOutOfTheSpaceAboveTheParagraphAfterIt() throws Exception {
        // With no margin to take it from, the border stood below the card and the paragraph
        // after it landed a border lower than on the page.
        long filled = spaceAboveTheParagraphAfterACard(null, 0);
        long outlined = spaceAboveTheParagraphAfterACard(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2), 0);

        assertThat(filled - outlined).isEqualTo(2 * 20L);
    }

    @Test
    void aCardsBottomMarginTakesItsBorderFirst() throws Exception {
        long filled = spaceAboveTheParagraphAfterACard(null, 5);
        long outlined = spaceAboveTheParagraphAfterACard(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2), 5);

        assertThat(filled - outlined).as("the border, out of the margin; nothing more").isEqualTo(2 * 20L);
    }

    private static long spaceAboveTheParagraphAfterACard(DocumentStroke stroke, double marginBelow) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Page", body -> body
                        .spacing(12)
                        .addSection("Card", card -> card(card, stroke)
                                .margin(new DocumentInsets(0, 0, marginBelow, 0)).addParagraph("Card"))
                        .addParagraph("After")))) {
            XWPFParagraph after = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("After")).findFirst().orElseThrow();
            return before(after);
        }
    }

    @Test
    void aCardsBorderIsNotTakenFromItsNeighbourInTheNextCell() throws Exception {
        // Two cards side by side in a row stand at one height on the page; the first one's
        // bottom border is below it, not above the second.
        DocumentStroke stroke = DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 1);
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Pair", row -> row
                        .addSection("Left", card -> card(card, stroke)
                                .margin(new DocumentInsets(4, 0, 0, 0)).addParagraph("Left"))
                        .addSection("Right", card -> card(card, stroke)
                                .margin(new DocumentInsets(4, 0, 0, 0)).addParagraph("Right"))))) {
            List<XWPFTableCell> cells = document.getTables().get(0).getRow(0).getTableCells();

            assertThat(cells).hasSize(2);
            assertThat(spaceAboveTheCard(cells.get(1))).isEqualTo(spaceAboveTheCard(cells.get(0)));
        }
    }

    private static long spaceAboveTheCard(XWPFTableCell cell) {
        long space = 0;
        for (IBodyElement element : cell.getBodyElements()) {
            if (element instanceof XWPFTable) {
                return space;
            }
            if (element instanceof XWPFParagraph paragraph) {
                space += before(paragraph) + after(paragraph);
            }
        }
        throw new AssertionError("the cell holds no card");
    }

    private static long spaceBetweenTwoCards(DocumentStroke stroke) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Cards", cards -> cards
                        .spacing(12)
                        .addSection("First", card -> card(card, stroke).addParagraph("First"))
                        .addSection("Second", card -> card(card, stroke).addParagraph("Second"))))) {
            List<IBodyElement> body = document.getBodyElements();
            long space = 0;
            boolean between = false;
            for (IBodyElement element : body) {
                if (element instanceof XWPFTable) {
                    if (between) {
                        return space;
                    }
                    between = true;
                } else if (between && element instanceof XWPFParagraph paragraph) {
                    space += before(paragraph) + after(paragraph);
                }
            }
            throw new AssertionError("two cards were not written as two tables");
        }
    }

    private static SectionBuilder card(SectionBuilder card, DocumentStroke stroke) {
        card.fillColor(DocumentColor.rgb(240, 240, 240)).padding(DocumentInsets.of(6));
        return stroke == null ? card : card.stroke(stroke);
    }

    private static List<XWPFParagraph> written(List<XWPFParagraph> paragraphs) {
        return paragraphs.stream().filter(paragraph -> !paragraph.getText().isBlank()).toList();
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getAfter());
    }
}
