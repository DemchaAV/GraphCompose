package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table text cell's lines stand on the page's baselines in Word, through the row's margins.
 *
 * <p>The page sets a cell's line its face's ascent below the line's top; Word stands an exact
 * line's baseline four fifths of the way down it. Measured, a line of 8pt Poppins stood 0.58pt
 * low in Word and 0.55 in LibreOffice, of Lato 0.24 high, whatever the cell's anchor. Every cell of
 * a row takes the row's largest margins, so the margins move every cell's text at once: the top
 * made smaller by as much as the bottom is made larger, and the row keeps its height.</p>
 */
class DocxCellSeatTest {

    /** Poppins' ascent, in ems: 1050 of 1000 units. */
    private static final double POPPINS_ASCENT = 1.05;
    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;
    /** Helvetica's ascent, in ems: 718 of 1000 units, as its font metrics state it. */
    private static final double HELVETICA_ASCENT = 0.718;
    /** Gothic A1's ascent, in ems: 798 of 1000 units, its own line one em. */
    private static final double GOTHIC_A1_ASCENT = 0.798;
    /** The padding the tables here are laid out with, in twips: 4pt all round, and no rules. */
    private static final long PADDING = 80;

    @Test
    void aRowOfTextCellsSetLowInWordIsRaisedThroughItsMargins() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Middle", poppins(8), DocumentTableTextAnchor.CENTER),
                        cell("Top", poppins(8), DocumentTableTextAnchor.TOP_LEFT),
                        cell("Bottom", poppins(8), DocumentTableTextAnchor.BOTTOM_LEFT))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            long raise = Math.round((0.8 * line(row.getCell(0)) - POPPINS_ASCENT * 8) * 20);

            assertThat(raise).as("the premise: Word sets Poppins half a point low").isBetween(10L, 12L);
            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).as("each cell, whatever its anchor, its room moved up and as tall")
                        .containsExactly(PADDING - raise, PADDING + raise);
            }
        }
    }

    @Test
    void aRowOfTextCellsSetHighInWordIsLoweredThroughItsMargins() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(cell("Lato", lato(8), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            long raise = Math.round((0.8 * line(cell) - LATO_ASCENT * 8) * 20);

            assertThat(raise).as("the premise: Word sets Lato a quarter point high").isBetween(-5L, -3L);
            assertThat(margins(cell)).containsExactly(PADDING - raise, PADDING + raise);
        }
    }

    @Test
    void aRowIsMovedByTheLeastItsCellsCallFor() throws Exception {
        // Helvetica at 14, 7 and 10pt stands 6, 3 and 4 twips low: none is moved past the page's.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Large", helvetica(14), DocumentTableTextAnchor.CENTER),
                        cell("Small", helvetica(7), DocumentTableTextAnchor.CENTER),
                        cell("Middle", helvetica(10), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            long large = Math.round((0.8 * line(row.getCell(0)) - HELVETICA_ASCENT * 14) * 20);
            long small = Math.round((0.8 * line(row.getCell(1)) - HELVETICA_ASCENT * 7) * 20);
            long middle = Math.round((0.8 * line(row.getCell(2)) - HELVETICA_ASCENT * 10) * 20);

            assertThat(small).as("the premise: the least is the 7pt cell's, apart from the others")
                    .isLessThan(middle).isLessThan(large).isPositive();
            assertThat(large).as("the default face's, as a paragraph of it is raised").isEqualTo(DocxExports.DEFAULT_LINE_RAISE);
            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(PADDING - small, PADDING + small);
            }
        }
    }

    @Test
    void aCellOfSeveralLinesMovesThemTogetherAndKeepsTheSpaceBetweenThem() throws Exception {
        // A paragraph a line, the cell's line spacing after each but the last.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.lines("One", "Two").withStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                        .textStyle(poppins(8)).textAnchor(DocumentTableTextAnchor.BOTTOM_LEFT).lineSpacing(2).build()))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            long raise = Math.round((0.8 * line(cell) - POPPINS_ASCENT * 8) * 20);

            assertThat(cell.getParagraphs()).as("the premise: a paragraph a line").hasSize(2);
            assertThat(margins(cell)).containsExactly(PADDING - raise, PADDING + raise);
            assertThat(DocxTwips.of(cell.getParagraphs().get(0).getCTP().getPPr().getSpacing().getAfter()))
                    .as("the space between the lines").isEqualTo(40);
        }
    }

    @Test
    void eachRowIsMovedOnItsOwn() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(cell("Header", lato(8), DocumentTableTextAnchor.CENTER))
                .rowCells(cell("Body", poppins(8), DocumentTableTextAnchor.CENTER))))) {
            var table = document.getTables().get(0);
            long lowered = Math.round((0.8 * line(table.getRow(0).getCell(0)) - LATO_ASCENT * 8) * 20);
            long raised = Math.round((0.8 * line(table.getRow(1).getCell(0)) - POPPINS_ASCENT * 8) * 20);

            assertThat(margins(table.getRow(0).getCell(0))).containsExactly(PADDING - lowered, PADDING + lowered);
            assertThat(margins(table.getRow(1).getCell(0))).containsExactly(PADDING - raised, PADDING + raised);
        }
    }

    @Test
    void aTableWithNoLayoutKeepsItsMargins() throws Exception {
        // Nothing measured the lines: there is no seat of the page's to move them to.
        try (XWPFDocument document = DocxExports.withoutLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(cell("Poppins", poppins(8), DocumentTableTextAnchor.CENTER))))) {
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0))).containsExactly(PADDING, PADDING);
        }
    }

    @Test
    void aRowWhoseCellsCallForOppositeWaysIsLeftAsItWas() throws Exception {
        // Its margins are not moved; the cell calling for half a point and more is moved by its
        // text's position, the other is left.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Low", poppins(8), DocumentTableTextAnchor.CENTER),
                        cell("High", lato(8), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            double poppins = 0.8 * line(row.getCell(0)) - POPPINS_ASCENT * 8;

            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(PADDING, PADDING);
            }
            assertThat(poppins).as("the premise: half a point and more").isGreaterThanOrEqualTo(0.5);
            assertThat(position(row.getCell(0))).isEqualTo(Math.round(poppins * 2));
            assertThat(position(row.getCell(1))).as("Lato's quarter point is left").isZero();
        }
    }

    @Test
    void whatTheMarginsLeaveOfHalfAPointAndMoreIsLeftToThePosition() throws Exception {
        // The row moves by Helvetica's 6 twips; Poppins at 14pt calls for nearly a point.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Poppins", poppins(14), DocumentTableTextAnchor.CENTER),
                        cell("Helvetica", helvetica(14), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            double left = 0.8 * line(row.getCell(0)) - POPPINS_ASCENT * 14 - DocxExports.DEFAULT_LINE_RAISE / 20.0;

            assertThat(margins(row.getCell(1))).containsExactly(PADDING - DocxExports.DEFAULT_LINE_RAISE,
                    PADDING + DocxExports.DEFAULT_LINE_RAISE);
            assertThat(left).as("the premise: half a point and more left").isGreaterThanOrEqualTo(0.5);
            assertThat(position(row.getCell(0))).isEqualTo(Math.round(left * 2));
            assertThat(position(row.getCell(1))).as("nothing left of Helvetica's").isZero();
        }
    }

    @Test
    void aBlankCellDoesNotHoldItsRowBack() throws Exception {
        // In a face Word sets high: measured, it would call for the opposite way and hold the row.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Text", poppins(8), DocumentTableTextAnchor.CENTER),
                        cell("", lato(8), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            long raise = Math.round((0.8 * line(row.getCell(0)) - POPPINS_ASCENT * 8) * 20);

            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(PADDING - raise, PADDING + raise);
            }
        }
    }

    @Test
    void aRowHoldingAComposedCellIsLeftAsItWas() throws Exception {
        // The composed paragraph is seated by its own writer; the margins would move it again.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Text", poppins(8), DocumentTableTextAnchor.CENTER),
                        DocumentTableCell.node(new ParagraphBuilder().text("Composed").textStyle(poppins(8))
                                .margin(DocumentInsets.zero()).build()).withStyle(style(poppins(8),
                                DocumentTableTextAnchor.CENTER)))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(margins(text)).containsExactly(PADDING, PADDING);
            assertThat(position(text)).as("the text cell moved by its position instead")
                    .isEqualTo(Math.round((0.8 * line(text) - POPPINS_ASCENT * 8) * 2));
        }
    }

    @Test
    void aRowInAVerticalMergeIsLeftAsItWas() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Spans", poppins(8), DocumentTableTextAnchor.CENTER).rowSpan(2),
                        cell("First", poppins(8), DocumentTableTextAnchor.CENTER))
                .rowCells(cell("Second", poppins(8), DocumentTableTextAnchor.CENTER))))) {
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(1))).containsExactly(PADDING, PADDING);
        }
    }

    @Test
    void aRaiseTakesNoMoreThanTheTopMarginHolds() throws Exception {
        // A quarter point above the text: Poppins calls for more.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Tight").withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(0.25, 4, 4, 4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                        .textStyle(poppins(8)).textAnchor(DocumentTableTextAnchor.CENTER).build()))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(margins(cell)).containsExactly(0L, PADDING + 5);
            assertThat(position(cell)).as("the third of a point left is under half a point").isZero();
        }
    }

    @Test
    void aLoweringTakesNoMoreThanTheBottomMarginHolds() throws Exception {
        // A tenth of a point below the text: Lato calls for more.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Tight").withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(4, 4, 0.1, 4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                        .textStyle(lato(8)).textAnchor(DocumentTableTextAnchor.CENTER).build()))))) {
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0))).containsExactly(PADDING + 2, 0L);
        }
    }

    @Test
    void aMoveUnderATenthOfAPointIsLeftAsItWas() throws Exception {
        // Word sets lines on a grid 0.12pt apart: a twip of margin moves a line a step or nothing.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(cell("Gothic", DocumentTextStyle.builder().fontName(FontName.GOTHIC_A1).size(14).build(),
                        DocumentTableTextAnchor.CENTER))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            double below = 0.8 * line(cell) - GOTHIC_A1_ASCENT * 14;

            assertThat(below).as("the premise: Word sets Gothic A1 low by half a twip and more, which rounds to a "
                                 + "twip, under a tenth")
                    .isBetween(0.025, 0.1);
            assertThat(margins(cell)).containsExactly(PADDING, PADDING);
        }
    }

    private static DocumentTableCell cell(String text, DocumentTextStyle face, DocumentTableTextAnchor anchor) {
        return DocumentTableCell.text(text).withStyle(style(face, anchor));
    }

    private static DocumentTableStyle style(DocumentTextStyle face, DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder().padding(DocumentInsets.of(4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                .textStyle(face).textAnchor(anchor).build();
    }

    private static DocumentTextStyle poppins(double size) {
        return DocumentTextStyle.builder().fontName(FontName.POPPINS).size(size).build();
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    private static DocumentTextStyle helvetica(double size) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(size).build();
    }

    /** The exact line a cell's first paragraph is written at, in points. */
    private static double line(XWPFTableCell cell) {
        return DocxTwips.of(cell.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    /** The {@code w:position} of a cell's first run, in half points; 0 for none. */
    private static long position(XWPFTableCell cell) {
        var properties = cell.getParagraphs().get(0).getCTP().getRArray(0).getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).longValue();
    }

    /** A cell's top and bottom margins, in twips. */
    private static long[] margins(XWPFTableCell cell) {
        CTTcMar mar = cell.getCTTc().getTcPr().getTcMar();
        return new long[]{DocxTwips.of(mar.getTop().getW()), DocxTwips.of(mar.getBottom().getW())};
    }

    private static XWPFDocument export(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
