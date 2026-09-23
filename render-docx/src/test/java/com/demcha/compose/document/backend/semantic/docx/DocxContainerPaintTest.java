package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A container that paints — a fill, borders, an outline — is written as a table of one cell.
 *
 * <p>Word has no element that wraps a run of paragraphs. Painted paragraph by paragraph, a
 * card came apart in an editor: its accent bar broke beside every row and table inside it,
 * its band had gaps where the space between blocks sat, and its padding above and below lay
 * outside it. A cell carries all of it: the shading behind everything inside, the borders at
 * the card's full height, and the padding as the cell's margins.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxContainerPaintTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor STRIPE = DocumentColor.rgb(200, 200, 200);
    private static final String LONG = "A paragraph long enough to wrap, so the card takes the whole "
            + "width it is given rather than the width of a few words.";

    @Test
    void aFilledSectionIsACellHoldingItsParagraphs() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("First"))
                .addParagraph(p -> p.text("Second"))))) {
            XWPFTableCell cell = onlyCell(document);

            assertThat(fill(cell)).isEqualTo("EEF3F9");
            assertThat(cell.getParagraphs()).extracting(XWPFParagraph::getText).containsExactly("First", "Second");
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                CTPPr properties = paragraph.getCTP().getPPr();
                assertThat(properties == null || !properties.isSetShd())
                        .as("the cell's shading is behind the paragraph; it carries none of its own")
                        .isTrue();
            }
        }
    }

    @Test
    void anAccentIsTheCellsBorderOnThatSideAndNoOther() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .accentLeft(ACCENT, 3)
                .addParagraph(p -> p.text("Bordered"))))) {
            var edges = onlyCell(document).getCTTc().getTcPr().getTcBorders();

            assertThat(edges.getLeft().getVal()).isEqualTo(STBorder.SINGLE);
            assertThat(hex(edges.getLeft().getColor())).isEqualTo("1A5694");
            // w:sz counts eighths of a point, so a 3pt accent is 24.
            assertThat(edges.getLeft().getSz().intValue()).isEqualTo(24);
            for (CTBorder side : List.of(edges.getTop(), edges.getRight(), edges.getBottom())) {
                assertThat(side.getVal())
                        .as("a side the card does not draw is stated as none, over any table style")
                        .isEqualTo(STBorder.NIL);
            }
        }
    }

    @Test
    void aUniformStrokeDrawsAllFourSides() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .stroke(DocumentStroke.of(ACCENT, 1))
                .addParagraph(p -> p.text("Outlined"))))) {
            var edges = onlyCell(document).getCTTc().getTcPr().getTcBorders();

            for (CTBorder side : List.of(edges.getTop(), edges.getRight(), edges.getBottom(), edges.getLeft())) {
                assertThat(side.getVal()).isEqualTo(STBorder.SINGLE);
                assertThat(side.getSz().intValue()).isEqualTo(8);
            }
        }
    }

    @Test
    void anUnpaintedSectionIsNotATable() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Plain", plain -> plain
                .padding(DocumentInsets.of(8))
                .addParagraph(p -> p.text("Nothing to paint"))))) {
            assertThat(document.getTables()).isEmpty();
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();
            assertThat(properties.isSetShd()).isFalse();
            assertThat(properties.isSetPBdr()).isFalse();
        }
    }

    @Test
    void thePaddingIsTheCellsMarginsAndTheTableStartsAtTheCardsEdge() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(new DocumentInsets(10, 14, 12, 16))
                .margin(new DocumentInsets(0, 0, 0, 6))
                .addParagraph(p -> p.text(LONG))))) {
            XWPFTable table = document.getTables().get(0);
            var margins = onlyCell(document).getCTTc().getTcPr().getTcMar();

            assertThat(DocxTwips.of(margins.getTop().getW())).isEqualTo(10 * 20L);
            assertThat(DocxTwips.of(margins.getRight().getW())).isEqualTo(14 * 20L);
            assertThat(DocxTwips.of(margins.getBottom().getW())).isEqualTo(12 * 20L);
            assertThat(DocxTwips.of(margins.getLeft().getW())).isEqualTo(16 * 20L);
            // The editor measures w:tblInd to the cell's text, so the cell's left margin is in it.
            assertThat(DocxTwips.of(table.getCTTbl().getTblPr().getTblInd().getW()))
                    .as("the card's left margin, then its left padding")
                    .isEqualTo((6 + 16) * 20L);
        }
    }

    @Test
    void aCardIsAsWideAsTheLayoutPlacedIt() throws Exception {
        // The layout sizes a card round its content, and the page draws it that wide: a card
        // holding a word is narrow, and one holding a wrapped paragraph as wide as its
        // longest line.
        for (String text : List.of("Short", LONG)) {
            Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                    .fillColor(SURFACE)
                    .padding(DocumentInsets.of(10))
                    .addParagraph(p -> p.text(text)));
            double placed = placedCardWidth(content);
            try (XWPFDocument document = export(content)) {
                long width = DocxTwips.of(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW());

                assertThat(width)
                        .as(text + ": as placed, and the point of slack an editor's face needs")
                        .isEqualTo(Math.round((placed + DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS) * 20));
            }
        }
    }

    @Test
    void aBorderStraddlesThePanelsEdgeAsOnThePage() throws Exception {
        // The page centres a border on the panel's edge; the editor keeps it inside the cell.
        // Half of it comes off the margin, the table widens by half of each side's, and the
        // table moves out by half the left one, so the text and the border land on the page's.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .accentLeft(ACCENT, 3)
                .padding(DocumentInsets.of(14))
                .addParagraph(p -> p.text(LONG)));
        double placed = placedCardWidth(content);
        try (XWPFDocument document = export(content)) {
            XWPFTable table = document.getTables().get(0);
            var margins = onlyCell(document).getCTTc().getTcPr().getTcMar();

            assertThat(DocxTwips.of(margins.getLeft().getW())).isEqualTo(250L);
            assertThat(DocxTwips.of(margins.getTop().getW())).as("no border on top").isEqualTo(280L);
            assertThat(DocxTwips.of(table.getCTTbl().getTblPr().getTblW().getW()))
                    .isEqualTo(Math.round((placed + DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS + 1.5) * 20));
            assertThat(DocxTwips.of(table.getCTTbl().getTblPr().getTblInd().getW()))
                    .as("a body table is placed by its text: the padding, less half the border")
                    .isEqualTo(250L);
        }
    }

    @Test
    void aNestedPanelIsPlacedByItsBordersOuterEdge() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(10))
                .addSection("Inner", inner -> inner
                        .accentLeft(ACCENT, 2)
                        .padding(DocumentInsets.of(8))
                        .margin(new DocumentInsets(0, 0, 0, 5))
                        .addParagraph(p -> p.text("Inner text")))))) {
            XWPFTable nested = onlyCell(document).getTables().get(0);

            assertThat(DocxTwips.of(nested.getCTTbl().getTblPr().getTblInd().getW()))
                    .as("its margin, less the half of the border outside its edge")
                    .isEqualTo(4 * 20L);
        }
    }

    private static double placedCardWidth(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().nodes().stream()
                    .filter(node -> node.nodeKind().equals("SectionNode"))
                    .findFirst()
                    .orElseThrow()
                    .placementWidth();
        }
    }

    @Test
    void theCardsTopAndBottomMarginAreTheSpaceAroundTheTable() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead"))
                .addSection("Card", card -> card
                        .fillColor(SURFACE)
                        .margin(DocumentInsets.symmetric(9, 0))
                        .addParagraph(p -> p.text("Inside")))
                .addParagraph(p -> p.text("After")))) {
            XWPFParagraph lead = document.getParagraphs().get(0);
            XWPFParagraph after = document.getParagraphs().get(1);

            assertThat(after.getText()).isEqualTo("After");
            assertThat(DocxTwips.of(lead.getCTP().getPPr().getSpacing().getAfter()))
                    .as("Word has no space above a table, so the paragraph before it holds it")
                    .isEqualTo(9 * 20L);
            assertThat(DocxTwips.of(after.getCTP().getPPr().getSpacing().getBefore())).isEqualTo(9 * 20L);
            CTPPr properties = after.getCTP().getPPr();
            assertThat(properties.isSetShd()).as("the paint ends where the card ends").isFalse();
        }
    }

    @Test
    void aCardKeptTogetherOnOnePageIsARowThatDoesNotSplit() throws Exception {
        try (XWPFDocument kept = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE).keepTogether().addParagraph(p -> p.text("Kept"))));
             XWPFDocument free = export(page -> page.addSection("Card", card -> card
                     .fillColor(SURFACE).addParagraph(p -> p.text("Free"))))) {
            assertThat(kept.getTables().get(0).getRow(0).isCantSplitRow()).isTrue();
            assertThat(free.getTables().get(0).getRow(0).isCantSplitRow()).isFalse();
        }
    }

    @Test
    void aPanelInsideAPanelIsATableInsideItsCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Outer text"))
                .addSection("Inner", in -> in
                        .fillColor(DocumentColor.rgb(255, 240, 200))
                        .addParagraph(p -> p.text("Inner text")))))) {
            XWPFTableCell outer = onlyCell(document);
            XWPFTableCell inner = outer.getTables().get(0).getRow(0).getCell(0);

            assertThat(fill(outer)).isEqualTo("EEF3F9");
            assertThat(fill(inner)).isEqualTo("FFF0C8");
            assertThat(inner.getParagraphs().get(0).getText()).isEqualTo("Inner text");
        }
    }

    @Test
    void aRowInAPanelIsATableInsideItsCellThatShowsThePanelThrough() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .stroke(DocumentStroke.of(ACCENT, 1))
                .addParagraph(p -> p.text("Above"))
                .addRow(r -> r
                        .addParagraph(p -> p.text("Left"))
                        .addParagraph(p -> p.text("Right")))
                .addParagraph(p -> p.text("Below"))))) {
            XWPFTableCell card = onlyCell(document);

            assertThat(document.getTables()).as("one table in the body: the card").hasSize(1);
            for (XWPFTableCell cell : card.getTables().get(0).getRow(0).getTableCells()) {
                var properties = cell.getCTTc().getTcPr();
                assertThat(properties == null || !properties.isSetShd())
                        .as("the row has no fill of its own, and the card's shows through")
                        .isTrue();
                CTPPr text = cell.getParagraphs().get(0).getCTP().getPPr();
                assertThat(text == null || !text.isSetPBdr())
                        .as("the card's outline is not drawn again around each cell's text")
                        .isTrue();
            }
        }
    }

    @Test
    void aTablesUnfilledCellsInAFilledPanelAreTheEnginesWhite() throws Exception {
        try (XWPFDocument inPanel = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addTable(t -> t.autoColumns(1).row("Cell"))));
             XWPFDocument onPage = export(page -> page.addTable(t -> t.autoColumns(1).row("Cell")))) {
            XWPFTableCell cell = onlyCell(inPanel).getTables().get(0).getRow(0).getCell(0);

            assertThat(fill(cell))
                    .as("the page draws the cell white on the card, not in the card's colour")
                    .isEqualTo("FFFFFF");
            var properties = onPage.getTables().get(0).getRow(0).getCell(0).getCTTc().getTcPr();
            assertThat(properties == null || !properties.isSetShd())
                    .as("on the page white is what an unshaded cell shows anyway")
                    .isTrue();
        }
    }

    @Test
    void theDefaultFillIsTheEnginesOwn() {
        assertThat(DocxSemanticBackend.ENGINE_DEFAULT_CELL_FILL.color())
                .isEqualTo(TableCellLayoutStyle.DEFAULT.fillColor());
    }

    @Test
    void aZebraStripeInAPanelKeepsItsOwnColourInAComposedCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addTable(t -> t.autoColumns(2)
                        .zebra(DocumentColor.rgb(255, 255, 255), STRIPE)
                        .row("First", "1")
                        .rowCells(DocumentTableCell.text("Second"),
                                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                        .text("Composed").build())))))) {
            XWPFTableCell composed = onlyCell(document).getTables().get(0).getRow(1).getCell(1);

            assertThat(fill(composed)).isEqualTo("C8C8C8");
            CTPPr properties = composed.getParagraphs().get(0).getCTP().getPPr();
            assertThat(properties == null || !properties.isSetShd()).isTrue();
        }
    }

    @Test
    void aPanelInsideAComposedCellIsATableInThatCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t.autoColumns(1)
                .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                        .fillColor(ACCENT)
                        .addParagraph(p -> p.text("Inner"))
                        .build()))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFTableCell panel = cell.getTables().get(0).getRow(0).getCell(0);

            assertThat(fill(panel)).isEqualTo("1A5694");
            assertThat(panel.getParagraphs().get(0).getText()).isEqualTo("Inner");
        }
    }

    @Test
    void aParagraphAfterATableInACardTakesOverTheParagraphClosingIt() throws Exception {
        // Word ends a cell with a paragraph, so one follows every nested table. Left in place
        // under the paragraph written next, it was an empty line the page does not have.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addTable(t -> t.autoColumns(1).row("Row"))
                .addParagraph(p -> p.text("After the table"))))) {
            List<IBodyElement> inside = onlyCell(document).getBodyElements();

            assertThat(inside).hasSize(2);
            assertThat(inside.get(0)).isInstanceOf(XWPFTable.class);
            XWPFParagraph after = (XWPFParagraph) inside.get(1);
            assertThat(after.getText()).isEqualTo("After the table");
            var spacing = after.getCTP().getPPr() == null ? null : after.getCTP().getPPr().getSpacing();
            assertThat(spacing == null || !spacing.isSetLine() || DocxTwips.of(spacing.getLine()) > 2L)
                    .as("and it is a line of text again, not a hairline")
                    .isTrue();
        }
    }

    @Test
    void aCardEndingInATableClosesItWithAHairline() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Title"))
                .addTable(t -> t.autoColumns(1).row("Last"))))) {
            List<IBodyElement> inside = onlyCell(document).getBodyElements();
            XWPFParagraph closer = (XWPFParagraph) inside.get(inside.size() - 1);

            assertThat(closer.getText()).isEmpty();
            assertThat(DocxTwips.of(closer.getCTP().getPPr().getSpacing().getLine()))
                    .as("a tenth of a point, not an empty line at the bottom of the card")
                    .isEqualTo(2L);
        }
    }

    @Test
    void twoTablesInACardAreKeptApartByTheParagraphClosingTheFirst() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .stroke(DocumentStroke.of(ACCENT, 1))
                .addTable(t -> t.autoColumns(1).row("Above"))
                .addTable(t -> t.autoColumns(1).row("Below"))))) {
            List<IBodyElement> inside = onlyCell(document).getBodyElements();

            assertThat(inside).hasSize(4);
            assertThat(inside.get(0)).isInstanceOf(XWPFTable.class);
            assertThat(inside.get(2)).isInstanceOf(XWPFTable.class);
            CTPPr separator = ((XWPFParagraph) inside.get(1)).getCTP().getPPr();
            assertThat(DocxTwips.of(separator.getSpacing().getLine())).isEqualTo(2L);
            assertThat(separator.isSetKeepNext()).isTrue();
            assertThat(separator.isSetPBdr()).isFalse();
        }
    }

    private static XWPFTableCell onlyCell(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        XWPFTable table = document.getTables().get(0);
        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRow(0).getTableCells()).hasSize(1);
        return table.getRow(0).getCell(0);
    }

    private static String fill(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return properties == null || !properties.isSetShd() ? null : hex(properties.getShd().getFill());
    }

    /**
     * Reads an {@code ST_HexColor} back as the six hex digits it was written as.
     *
     * <p>The type is an xmlbeans union, so a concrete colour comes back as the three
     * bytes rather than as the string the writer passed in.</p>
     */
    private static String hex(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02X", b & 0xFF));
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, content);
    }
}
