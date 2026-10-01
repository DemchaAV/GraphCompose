package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel keeps the height the page gives it, and a document ending in a table does not run on
 * to a page of its own.
 *
 * @author Artem Demchyshyn
 */
class DocxPanelHeightTest {

    @Test
    void aPanelTallerThanItsTextKeepsThePagesHeight() throws Exception {
        // A card whose inner row's padding, not its text, sets its height: Word closed it round
        // the text, as MerchantInvoice's due-date card.
        try (XWPFDocument document = export(page -> page.addSection("DueCard", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addRow("DueRow", row -> row.padding(new DocumentInsets(20, 0, 20, 0))
                        .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("26 June 2026")))))) {
            assertThat(heightOf(document.getTables().get(0))).as("40pt of padding and a line of text")
                    .isGreaterThan(48 * 20);
        }
    }

    @Test
    void theEmptyParagraphClosingACellThatEndsInATableIsNotLaidOut() throws Exception {
        // LibreOffice laid it out a tenth of a point tall under every nested table.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120)).row("Inner"))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = card.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) elements.get(elements.size() - 1);

            assertThat(elements.get(elements.size() - 2)).isInstanceOf(XWPFTable.class);
            assertThat(closing.getCTP().getPPr().getRPr().sizeOfVanishArray()).isEqualTo(1);
        }
    }

    @Test
    void theEmptyParagraphClosingACellOfANestedTableIsNotLaidOutEither() throws Exception {
        // CobaltRota's shape: a table composed in a cell of a table in a card.
        DocumentNode inner = new TableBuilder()
                .columns(DocumentTableColumn.fixed(80)).row("Deep").build();
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(inner)))))) {
            XWPFTableCell middle = document.getTables().get(0).getRow(0).getCell(0)
                    .getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = middle.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) elements.get(elements.size() - 1);

            assertThat(elements.get(elements.size() - 2)).isInstanceOf(XWPFTable.class);
            assertThat(closing.getCTP().getPPr().getRPr().sizeOfVanishArray()).isEqualTo(1);
        }
    }

    @Test
    void textAfterATableInACellKeepsItsMark() throws Exception {
        // The paragraph closing the table is taken over by the text written after it.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120)).row("Inner"))
                .addParagraph(p -> p.text("After"))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = card.getBodyElements();
            assertThat(elements).as("the table, and the text in the paragraph that closed it").hasSize(2);
            assertThat(((XWPFParagraph) elements.get(1)).getText()).isEqualTo("After");
            for (XWPFParagraph paragraph : card.getParagraphs()) {
                assertThat(paragraph.getCTP().getPPr() != null && paragraph.getCTP().getPPr().isSetRPr()
                           && paragraph.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0)
                        .as("no mark hidden in a cell ending in text").isFalse();
            }
        }
    }

    @Test
    void theParagraphClosingACellKeepsItsMarkWhenItHoldsABookmark() throws Exception {
        // An anchored table's bookmark closes in the paragraph under it: that paragraph is not
        // structure alone.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120)).row("Inner").anchor("inner"))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = card.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) elements.get(elements.size() - 1);

            assertThat(closing.getCTP().getBookmarkEndList()).as("the bookmark's end").isNotEmpty();
            assertThat(closing.getCTP().getPPr().isSetRPr() && closing.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0)
                    .isFalse();
        }
    }

    @Test
    void theParagraphClosingACellKeepsItsMarkWhenItHoldsTheSpaceBelowTheTable() throws Exception {
        // A padded card ending in a table keeps its bottom padding in the paragraph under it.
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120)).row("Inner").margin(new DocumentInsets(0, 0, 12, 0)))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            List<IBodyElement> elements = card.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) elements.get(elements.size() - 1);

            assertThat(((Number) closing.getCTP().getPPr().getSpacing().getAfter()).longValue())
                    .as("the space below the table").isPositive();
            assertThat(closing.getCTP().getPPr().isSetRPr() && closing.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0)
                    .isFalse();
        }
    }

    @Test
    void theEmptyParagraphClosingADocumentThatEndsInATableIsNotLaidOut() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120)).row("Last")))) {
            List<IBodyElement> body = document.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) body.get(body.size() - 1);

            assertThat(body.get(body.size() - 2)).isInstanceOf(XWPFTable.class);
            assertThat(closing.getCTP().getPPr().getRPr().sizeOfVanishArray()).isEqualTo(1);
        }
    }

    @Test
    void aRuleClosingADocumentAfterATableStaysVisible() throws Exception {
        // A rule is a paragraph with nothing in it but its border.
        try (XWPFDocument document = export(page -> page
                .addTable(t -> t.columns(DocumentTableColumn.fixed(120)).row("Last"))
                .addLine(line -> line.horizontal(200).thickness(1).color(DocumentColor.rgb(40, 40, 40))))) {
            List<IBodyElement> body = document.getBodyElements();
            XWPFParagraph last = (XWPFParagraph) body.get(body.size() - 1);

            assertThat(last.getCTP().getPPr().isSetPBdr()).as("the rule is the last paragraph").isTrue();
            assertThat(last.getCTP().getPPr().isSetRPr() && last.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0)
                    .isFalse();
        }
    }

    @Test
    void aPanelsHeightIsWrittenLessTheMarginsItsCellHolds() throws Exception {
        // Two cards the page makes equally tall: padded by their inner row, and by the card.
        // LibreOffice adds a cell's margins to the written height, so the second is written less them.
        try (XWPFDocument document = export(page -> page
                .addSection("Inner", card -> card.fillColor(DocumentColor.rgb(247, 249, 246))
                        .addRow("InnerRow", row -> row.padding(new DocumentInsets(20, 0, 20, 0))
                                .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                                .addParagraph(p -> p.text("26 June 2026"))))
                .addSection("Outer", card -> card.fillColor(DocumentColor.rgb(247, 249, 246))
                        .padding(new DocumentInsets(10, 0, 10, 0))
                        .addRow("OuterRow", row -> row.padding(new DocumentInsets(10, 0, 10, 0))
                                .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                                .addParagraph(p -> p.text("26 June 2026")))))) {
            int inner = heightOf(document.getTables().get(0));
            int outer = heightOf(document.getTables().get(document.getTables().size() - 1));

            assertThat(inner - outer).as("the second card's 20pt of margins").isEqualTo(20 * 20);
        }
    }

    @Test
    void aPanelOpeningACellTakesItsTopBorderFromTheSpaceOverItsFirstLine() throws Exception {
        // MerchantInvoice's payment panel: first in its row's cell, with no padding and no space
        // above to take its border from. Word draws the border above the cell's content, so every
        // line inside stood the border's width low and the row as much taller.
        XWPFTableCell bordered = panelInARow(1);
        XWPFTableCell plain = panelInARow(0);

        assertThat(beforeOf(bordered)).as("the room over the heading, less the 1pt border")
                .isEqualTo(beforeOf(plain) - 20);
        // The height written is already less the border LibreOffice adds to it; it is also less
        // the border taken over the heading, which Word draws above the row's height.
        assertThat(heightOf(outerTableOf(bordered))).as("the row's height, less the border twice")
                .isEqualTo(heightOf(outerTableOf(plain)) - 20 - 20);
    }

    @Test
    void aPaddedPanelOpeningACellKeepsTheSpaceOverItsFirstLine() throws Exception {
        // Its top margin is wider than its border, which Word then draws inside the margin.
        XWPFTableCell bordered = panelInARow(1, 10);
        XWPFTableCell plain = panelInARow(0, 10);

        assertThat(beforeOf(bordered)).isEqualTo(beforeOf(plain));
        // Its margins are half a border narrower top and bottom, its border LibreOffice's
        // allowance: the height held is the same, nothing more taken off for Word.
        assertThat(heightOf(outerTableOf(bordered))).isEqualTo(heightOf(outerTableOf(plain)));
    }

    @Test
    void aPanelRuledAboveOnlyOpeningACellKeepsTheHeightItHeld() throws Exception {
        // Word draws the one border outside the row's height, where LibreOffice's allowance for it
        // already took it off; nothing more comes off for it.
        XWPFTableCell ruled = panelInARow(1, 0, true);
        XWPFTableCell plain = panelInARow(0, 0, false);

        assertThat(beforeOf(ruled)).as("the room over the heading, less the border").isEqualTo(beforeOf(plain) - 20);
        assertThat(heightOf(outerTableOf(ruled))).as("less the border once")
                .isEqualTo(heightOf(outerTableOf(plain)) - 20);
    }

    private static XWPFTableCell panelInARow(double border) throws Exception {
        return panelInARow(border, 0, false);
    }

    private static XWPFTableCell panelInARow(double border, double padding) throws Exception {
        return panelInARow(border, padding, false);
    }

    /** The cell of a painted panel opening a row's first column, its heading 7pt below its top. */
    private static XWPFTableCell panelInARow(double border, double padding, boolean aboveOnly) throws Exception {
        XWPFDocument document = export(page -> page.addRow("Settlement", row -> row
                .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1),
                        com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                .addSection("Panel", panel -> {
                    panel.keepTogether().fillColor(DocumentColor.rgb(247, 249, 246)).padding(DocumentInsets.of(padding));
                    com.demcha.compose.document.style.DocumentStroke stroke =
                            com.demcha.compose.document.style.DocumentStroke.of(DocumentColor.rgb(200, 200, 200), border);
                    if (border > 0 && aboveOnly) {
                        panel.borders(new com.demcha.compose.document.style.DocumentBorders(stroke, null, null, null));
                    } else if (border > 0) {
                        panel.stroke(stroke);
                    }
                    // A row in a row's column is laid in a layer stack, as the template lays it.
                    panel.addLayerStack(stack -> stack.name("HeadingLayer").layer(
                            new com.demcha.compose.document.dsl.RowBuilder().name("Heading")
                                    .padding(new DocumentInsets(7, 0, 0, 0))
                                    .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                                    .addParagraph(p -> p.text("PAYMENT DETAILS"))
                                    .build()));
                    panel.addParagraph(p -> p.text("Bank Name: Harbour Bank of Canada"));
                })
                .addParagraph(p -> p.text("Subtotal"))));
        return document.getTables().get(0).getRow(0).getCell(0).getTables().get(0).getRow(0).getCell(0);
    }

    private static XWPFTable outerTableOf(XWPFTableCell cell) {
        return cell.getTableRow().getTable();
    }

    /** The space above a cell's first paragraph, in twips. */
    private static long beforeOf(XWPFTableCell cell) {
        XWPFParagraph first = (XWPFParagraph) cell.getBodyElements().get(0);
        var spacing = first.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? ((Number) spacing.getBefore()).longValue() : 0;
    }

    /** A table's first row's written height, which the export writes "at least". */
    private static int heightOf(XWPFTable table) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr properties = table.getRow(0).getCtRow().getTrPr();
        assertThat(properties).isNotNull();
        assertThat(properties.sizeOfTrHeightArray()).isEqualTo(1);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHeight height = properties.getTrHeightArray(0);
        assertThat(height.getHRule()).isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
        return ((Number) height.getVal()).intValue();
    }

    @Test
    void aDocumentEndingInTextKeepsItsLastParagraphVisible() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p.text("The end")))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            XWPFParagraph last = paragraphs.get(paragraphs.size() - 1);

            assertThat(last.getCTP().isSetPPr() && last.getCTP().getPPr().isSetRPr()
                       && last.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0).isFalse();
        }
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
