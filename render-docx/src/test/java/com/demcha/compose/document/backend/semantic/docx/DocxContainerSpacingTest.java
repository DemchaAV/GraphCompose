package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.ClipPolicy;
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
            assertThat(DocxTwips.of(spacing.getLine())).as("a tenth of a point").isEqualTo(2L);
            assertThat(DocxTwips.of(spacing.getLine()) + before(all.get(2)))
                    .as("the hairline and the space above the next entry, the spacer's height").isEqualTo(90L);
        }
    }

    @Test
    void theSpaceBelowASpacerOfNoHeightTakesItsHairline() throws Exception {
        // Nothing on the page, it is a hairline in Word: the space below takes that back, or the
        // next entry stood a tenth of a point lower than the page sets it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Entries", entries -> entries
                        .spacing(9)
                        .addParagraph("First entry")
                        .addSpacer(spacer -> spacer.name("Anchor").width(100).height(0))
                        .addParagraph("Second entry")))) {
            List<XWPFParagraph> all = document.getParagraphs();
            XWPFParagraph spacer = all.get(1);

            assertThat(spacer.getText()).isEmpty();
            assertThat(before(spacer) + DocxTwips.of(spacer.getCTP().getPPr().getSpacing().getLine()) + before(all.get(2)))
                    .as("the section's spacing either side of the spacer, its hairline included")
                    .isEqualTo(2 * NINE_POINTS);
        }
    }

    @Test
    void theSpaceUnderACardsOpeningSpacerOfNoHeightTakesItsHairline() throws Exception {
        // A width anchor: Panel's cards open with one, and each card stood a tenth of a point
        // taller in Word than on the page.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card
                        .spacing(9)
                        .fillColor(DocumentColor.rgb(240, 240, 240))
                        .padding(DocumentInsets.of(6))
                        .addSpacer(spacer -> spacer.name("Anchor").width(200).height(0))
                        .addParagraph("Title")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph spacer = cell.getParagraphs().get(0);

            assertThat(cell.getParagraphs()).extracting(XWPFParagraph::getText).containsExactly("", "Title");
            assertThat(DocxTwips.of(spacer.getCTP().getPPr().getSpacing().getLine()) + before(cell.getParagraphs().get(1)))
                    .as("the card's spacing under the anchor, its hairline included").isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aCardsPaddingBelowTakesTheHairlineOfASpacerOfNoHeightEndingIt() throws Exception {
        // Nothing owed below the spacer in the card: the card's padding below takes the tenth,
        // as it takes a line hanging below its last block.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card
                        .fillColor(DocumentColor.rgb(240, 240, 240))
                        .padding(DocumentInsets.of(6))
                        .addParagraph("Title")
                        .addSpacer(spacer -> spacer.name("Anchor").width(200).height(0))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getBottom().getW()))
                    .as("its 6pt padding less the hairline").isEqualTo(6 * 20 - 2);
        }
    }

    @Test
    void aSpacerOverATableHoldsTheRestOfItsHeightBelowItsHairline() throws Exception {
        // A table has no space above it in Word, so the spacer writes the rest of its height
        // below its own line; nothing stands between them to take the hairline out again.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Above")
                .addSpacer(spacer -> spacer.name("Gap").width(100).height(10))
                .addTable(t -> t.autoColumns(1).row("Cell")))) {
            List<IBodyElement> body = document.getBodyElements();
            XWPFTable table = document.getTables().get(0);
            XWPFParagraph spacer = (XWPFParagraph) body.get(body.indexOf(table) - 1);

            assertThat(spacer.getText()).isEmpty();
            assertThat(DocxTwips.of(spacer.getCTP().getPPr().getSpacing().getLine()) + after(spacer))
                    .as("the hairline and the space below it, the spacer's height").isEqualTo(200L);
        }
    }

    @Test
    void aSpacerUnderATableInACellIsTheTablesCloserAndItsHeightOnce() throws Exception {
        // In a cell the paragraph closing the table above becomes the spacer's: one hairline,
        // and no separator taking a second tenth out of the gap.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(230, 230, 230))
                        .addTable(t -> t.autoColumns(1).row("Cell"))
                        .addSpacer(spacer -> spacer.name("Gap").width(100).height(10))
                        .addParagraph("After")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> content = cell.getBodyElements();
            XWPFParagraph after = cell.getParagraphs().stream()
                    .filter(p -> p.getText().equals("After")).findFirst().orElseThrow();
            XWPFParagraph spacer = (XWPFParagraph) content.get(content.indexOf(after) - 1);

            assertThat(content.get(content.indexOf(spacer) - 1)).as("right under the table")
                    .isInstanceOf(XWPFTable.class);
            assertThat(DocxTwips.of(spacer.getCTP().getPPr().getSpacing().getLine()) + after(spacer) + before(after))
                    .as("the hairline and the space below it, the spacer's height").isEqualTo(200L);
        }
    }

    @Test
    void theBordersOfTwoCardsComeOutOfTheSpaceBetweenThem() throws Exception {
        // Word draws a card's top and bottom borders past its shading, where the page strokes them
        // on the card's edge: written whole, the space between two outlined cards came out wider,
        // and an invoice of three such cards ran onto a second page. Half a border reaches past
        // the card whatever its padding, the inner half only where the padding does not hold it
        // (outsideTheRow): with 6pt of padding, half of each 2pt border.
        long filled = spaceBetweenTwoCards(null);
        long outlined = spaceBetweenTwoCards(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2));

        assertThat(filled).as("filled cards: the section's spacing, less the separator's hairline")
                .isBetween(12 * 20L - 2, 12 * 20L);
        assertThat(filled - outlined)
                .as("half of the second card's top border and of the first one's bottom border come out of it")
                .isEqualTo(2 * 20L);
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
    void aShapeContainersEdgesAreSpaceAroundWhatItHolds() throws Exception {
        // A section heading set as a row in a container 18pt below the block above: written
        // layer by layer, the container's edges were never written and the heading stood high.
        DocumentNode heading = new ShapeContainerBuilder().name("Heading")
                .rectangle(300, 20).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(18, 0, 6, 0))
                .padding(new DocumentInsets(4, 0, 2, 0))
                .position(new RowBuilder().name("HeadingRow")
                                .addParagraph("—").addParagraph("SKILLS").build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Los Angeles, CA")
                .add(heading)
                .addParagraph("Java"))) {
            assertThat(after(paragraph(document, "Los Angeles, CA")))
                    .as("its top margin and padding, held below the paragraph before its row")
                    .isEqualTo(22 * 20L);
            // The row stands at the top of the 20pt outline, which holds 7.05pt more under it.
            assertThat(before(paragraph(document, "Java")))
                    .as("the outline under its row, its bottom padding and margin, above the paragraph after it")
                    .isEqualTo(8 * 20L + Math.round(7.05 * 20));
        }
    }

    @Test
    void aShapeContainerThatWritesNothingIsNoSpace() throws Exception {
        // A container in an overlay holding only a drawing writes no block: its edges would
        // stand the heading written after it that much lower than the page does.
        DocumentNode rule = new ShapeContainerBuilder().name("Rule")
                .rectangle(300, 2).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(18, 0, 0, 0))
                .position(new ShapeBuilder().name("Line").size(300, 2)
                        .fillColor(DocumentColor.rgb(26, 86, 148)).build(), 0, 0, LayerAlign.TOP_LEFT)
                .build();
        DocumentNode heading = new ShapeContainerBuilder().name("Heading")
                .rectangle(300, 60).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(10, 0, 0, 0))
                .position(new SectionBuilder().name("Body").add(rule).addParagraph("SKILLS").build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Los Angeles, CA")
                .add(heading))) {
            long between = after(paragraph(document, "Los Angeles, CA")) + before(paragraph(document, "SKILLS"));

            assertThat(between).as("the outer container's margin, not the drawn rule's as well")
                    .isEqualTo(10 * 20L);
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
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
    void aRowOpeningATablesCellIsWrittenAsBefore() throws Exception {
        // A table's cell takes no paragraph: the table holds its row at least as tall as the
        // page makes it, and one there was measured to set content lower than the page does —
        // each of ObsidianInvoice's line items 6pt lower. A row's column does (DocxSpaceAboveTest).
        com.demcha.compose.document.dsl.SectionBuilder composed = new com.demcha.compose.document.dsl.SectionBuilder();
        composed.padding(new DocumentInsets(12, 0, 0, 0))
                .addRow(inner -> inner.addParagraph("Qty").addParagraph("1"));
        com.demcha.compose.document.node.DocumentNode item = composed.build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 0, page -> page
                .addTable(t -> t
                        .columns(com.demcha.compose.document.table.DocumentTableColumn.auto(),
                                com.demcha.compose.document.table.DocumentTableColumn.auto())
                        .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(item),
                                com.demcha.compose.document.table.DocumentTableCell.text("Notes"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(cell.getBodyElements().get(0)).as("the nested table opens the cell")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void aCardsBottomBorderComesOutOfTheSpaceAboveTheParagraphAfterIt() throws Exception {
        // With no margin to take it from, the border stood below the card and the paragraph
        // after it landed lower than on the page: by half the 2pt border, the 6pt padding holding
        // its inner half (outsideTheRow). Taking the whole border stood the paragraph a point high.
        long filled = spaceAboveTheParagraphAfterACard(null, 0);
        long outlined = spaceAboveTheParagraphAfterACard(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2), 0);

        assertThat(filled - outlined).isEqualTo(20L);
    }

    @Test
    void aCardsBottomMarginTakesItsBorderFirst() throws Exception {
        long filled = spaceAboveTheParagraphAfterACard(null, 5);
        long outlined = spaceAboveTheParagraphAfterACard(DocumentStroke.of(DocumentColor.rgb(90, 90, 90), 2), 5);

        assertThat(filled - outlined).as("what reaches past the card, out of the margin; nothing more").isEqualTo(20L);
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
