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
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A table text cell's lines stand on the page's baselines in Word, through the row's margins, its
 * text's position and the height of its lines.
 *
 * <p>The page sets a cell's line its face's ascent below the line's top; Word stands an exact
 * line's baseline four fifths of the way down it. Measured, a line of 8pt Poppins stood 0.58pt
 * low in Word and 0.55 in LibreOffice, of Lato 0.24 high, whatever the cell's anchor. Every cell of
 * a row takes the row's largest margins, so the margins move every cell's text at once: the top
 * made smaller by as much as the bottom is made larger, and the row keeps its height. What they
 * leave is moved by the text's position from half a point up, and under it by each line's height
 * traded for the space after it.</p>
 *
 * <p>The cells here are in faces whose lines stay exact: Poppins, which Word sets low, and
 * Ubuntu, which it sets high, both tighter than their own line, and Helvetica, which the document
 * does not ship. A cell of Lato is as tall as its face's own line and is written at a least height,
 * which stands its text where the page does ({@link DocxGrownLinesExportTest}).</p>
 */
class DocxCellSeatTest {

    /** Poppins' ascent, in ems: 1050 of 1000 units. */
    private static final double POPPINS_ASCENT = 1.05;
    /** Ubuntu's ascent, in ems: 932 of 1000 units. */
    private static final double UBUNTU_ASCENT = 0.932;
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
                .rowCells(cell("Ubuntu", ubuntu(8), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            long raise = Math.round((0.8 * line(cell) - UBUNTU_ASCENT * 8) * 20);

            assertThat(rule(cell)).as("the premise: written exact").isEqualTo(STLineSpacingRule.EXACT);
            assertThat(raise).as("the premise: Word sets Ubuntu a quarter point high").isBetween(-7L, -5L);
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
                .rowCells(cell("Header", ubuntu(8), DocumentTableTextAnchor.CENTER))
                .rowCells(cell("Body", poppins(8), DocumentTableTextAnchor.CENTER))))) {
            var table = document.getTables().get(0);
            long lowered = Math.round((0.8 * line(table.getRow(0).getCell(0)) - UBUNTU_ASCENT * 8) * 20);
            long raised = Math.round((0.8 * line(table.getRow(1).getCell(0)) - POPPINS_ASCENT * 8) * 20);

            assertThat(rule(table.getRow(0).getCell(0))).as("the premise: written exact")
                    .isEqualTo(STLineSpacingRule.EXACT);
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
                        cell("High", ubuntu(8), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            double poppins = 0.8 * line(row.getCell(0)) - POPPINS_ASCENT * 8;

            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(PADDING, PADDING);
            }
            assertThat(rule(row.getCell(1))).as("the premise: written exact").isEqualTo(STLineSpacingRule.EXACT);
            assertThat(poppins).as("the premise: half a point and more").isGreaterThanOrEqualTo(0.5);
            assertThat(position(row.getCell(0))).isEqualTo(Math.round(poppins * 2));
            assertThat(after(row.getCell(0))).as("its line as it was").isZero();
            assertThat(position(row.getCell(1))).as("Ubuntu's quarter point is left").isZero();
            assertThat(after(row.getCell(1))).as("with no space after its line to take").isZero();
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
                        cell("", ubuntu(8), DocumentTableTextAnchor.CENTER))))) {
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
                .rowCells(cell("Text", poppins(8), DocumentTableTextAnchor.CENTER), composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(margins(text)).containsExactly(PADDING, PADDING);
            assertThat(position(text)).as("the text cell moved by its position instead")
                    .isEqualTo(Math.round((0.8 * line(text) - POPPINS_ASCENT * 8) * 2));
            assertThat(after(text)).as("its line as it was").isZero();
        }
    }

    @Test
    void aCellCallingForUnderHalfAPointTradesItsLineForTheSpaceAfterIt() throws Exception {
        // Poppins at 6pt stands 0.42pt low in Word: under a half point, no position moves it.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Text", poppins(6), DocumentTableTextAnchor.CENTER), composed("Composed"))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            XWPFTableCell text = row.getCell(0);
            double low = 0.8 * written(text) - POPPINS_ASCENT * 6;

            assertThat(low).as("the premise: a tenth of a point and more, under a half").isBetween(0.1, 0.5);
            assertThat(margins(text)).containsExactly(PADDING, PADDING);
            assertThat(after(text)).as("its line traded for the space after it, the cell as tall")
                    .isEqualTo(Math.round(low * 20 / 0.8)).isPositive();
            assertThat(0.8 * line(text) - POPPINS_ASCENT * 6).as("the baseline at the page's")
                    .isCloseTo(0, within(0.02));
            assertThat(position(text)).isZero();
            assertThat(after(row.getCell(1))).as("the composed paragraph's line is its own").isZero();
        }
    }

    @Test
    void aCellWhoseLettersReachNearItsLinesFootIsLeftAsItWas() throws Exception {
        // Poppins' descenders stand nearer the foot of its line than Word's cut leaves them.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("gypsy", poppins(6), DocumentTableTextAnchor.CENTER), composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(0.8 * written(text) - POPPINS_ASCENT * 6).as("the premise").isBetween(0.1, 0.5);
            assertThat(after(text)).as("its line as it was").isZero();
            assertThat(position(text)).isZero();
        }
    }

    @Test
    void aCellOfLinesBrokenInOneParagraphIsLeftAsItWas() throws Exception {
        // A taller or shorter line would move its second line from the first.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.lines("One", "Two").withStyle(style(poppins(6), DocumentTableTextAnchor.CENTER)),
                        composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(text.getParagraphs()).as("the premise: one paragraph, its lines broken").hasSize(1);
            assertThat(0.8 * written(text) - POPPINS_ASCENT * 6).as("the premise").isBetween(0.1, 0.5);
            assertThat(after(text)).isZero();
        }
    }

    @Test
    void aCellOfSeveralLinesTradesEachForTheSpaceAfterIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.lines("One", "Two").withStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                                .textStyle(poppins(6)).textAnchor(DocumentTableTextAnchor.CENTER).lineSpacing(2).build()),
                        composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph first = text.getParagraphs().get(0);
            XWPFParagraph second = text.getParagraphs().get(1);
            // The last line has no space after it of its own: what it has is its cut.
            long cut = afterOf(second);
            double low = 0.8 * (lineOf(second) + cut) / 20.0 - POPPINS_ASCENT * 6;

            assertThat(cut).isEqualTo(Math.round(low * 20 / 0.8)).isPositive();
            assertThat(lineOf(first)).as("each line cut alike").isEqualTo(lineOf(second));
            assertThat(afterOf(first)).as("the space between the lines, and the cut").isEqualTo(40 + cut);
        }
    }

    @Test
    void aCellSetHighTakesItsTallerLineFromTheSpaceAfterIt() throws Exception {
        // Padded more than the composed cell beside it, its padding past the row's margins is
        // written above and below its line, and the line takes from what is below it.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Ubuntu").withStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(6)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                                .textStyle(ubuntu(8)).textAnchor(DocumentTableTextAnchor.CENTER).build()),
                        composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);
            CTSpacing spacing = text.getParagraphs().get(0).getCTP().getPPr().getSpacing();
            long before = DocxTwips.of(spacing.getBefore());
            long grown = before - after(text);
            double high = 0.8 * (line(text) - grown / 20.0) - UBUNTU_ASCENT * 8;

            assertThat(rule(text)).as("the premise: written exact").isEqualTo(STLineSpacingRule.EXACT);
            assertThat(before).as("the premise: the padding past the row's margins").isEqualTo(40);
            assertThat(high).as("the premise: Word sets Ubuntu high").isNegative();
            assertThat(grown).as("its line as much taller as the space after it shorter")
                    .isEqualTo(Math.round(-high * 20 / 0.8));
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
        // A tenth of a point below the text: Ubuntu calls for more.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Tight").withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(4, 4, 0.1, 4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                        .textStyle(ubuntu(8)).textAnchor(DocumentTableTextAnchor.CENTER).build()))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(rule(cell)).as("the premise: written exact").isEqualTo(STLineSpacingRule.EXACT);
            assertThat(margins(cell)).containsExactly(PADDING + 2, 0L);
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

    @Test
    void aCallUnderATenthOfAPointLeavesTheLineAsItWas() throws Exception {
        // Beside a composed cell, as above: a twip of line moves a line a step of Word's grid or
        // nothing. Ubuntu at 2.5pt stands high by a twip and more, padded so as to have space
        // after it.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Ubuntu").withStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(6)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                                .textStyle(ubuntu(2.5)).textAnchor(DocumentTableTextAnchor.CENTER).build()),
                        composed("Composed"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(rule(cell)).as("the premise: written exact").isEqualTo(STLineSpacingRule.EXACT);
            assertThat(0.8 * line(cell) - UBUNTU_ASCENT * 2.5).as("the premise: a twip and more, under a tenth")
                    .isBetween(-0.1, -0.025);
            assertThat(after(cell)).as("the space after it as its padding gives it").isEqualTo(40);
        }
    }

    @Test
    void aCellInAStandardFaceTheExportDoesNotEmbedKeepsItsLine() throws Exception {
        // Its letters are read through a font standing in for it, in that font's units: not known.
        // Hyphens, low enough that read so they would leave the line room to cut.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("---", helvetica(14), DocumentTableTextAnchor.CENTER), composed("Composed"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(0.8 * written(cell) - HELVETICA_ASCENT * 14).as("the premise: a tenth of a point and more, under a half")
                    .isBetween(0.1, 0.5);
            assertThat(after(cell)).isZero();
        }
    }

    @Test
    void whatTheMarginsLeaveUnderHalfAPointIsMovedByTheLine() throws Exception {
        // The row moves by Helvetica's 6 twips at 14pt; Poppins at 6pt calls for more.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("Text", poppins(6), DocumentTableTextAnchor.CENTER),
                        cell("LARGE", helvetica(14), DocumentTableTextAnchor.CENTER))))) {
            XWPFTableRow row = document.getTables().get(0).getRow(0);
            double left = 0.8 * written(row.getCell(0)) - POPPINS_ASCENT * 6 - DocxExports.DEFAULT_LINE_RAISE / 20.0;

            for (XWPFTableCell cell : row.getTableCells()) {
                assertThat(margins(cell)).containsExactly(PADDING - DocxExports.DEFAULT_LINE_RAISE,
                        PADDING + DocxExports.DEFAULT_LINE_RAISE);
            }
            assertThat(left).as("the premise: a tenth of a point and more left").isBetween(0.1, 0.5);
            assertThat(after(row.getCell(0))).isEqualTo(Math.round(left * 20 / 0.8)).isPositive();
            assertThat(after(row.getCell(1))).as("nothing left of Helvetica's").isZero();
        }
    }

    @Test
    void aLineIsCutNoNearerItsLettersTopsThanWordKeepsThem() throws Exception {
        // Poppins' ring above its capital A reaches 0.96 of an em: cut by all its call, the line
        // would stand the ring closer to its top than Word keeps letters on screen.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(cell("ÅÅ", poppins(6), DocumentTableTextAnchor.CENTER), composed("Composed"))))) {
            XWPFTableCell text = document.getTables().get(0).getRow(0).getCell(0);
            double low = 0.8 * written(text) - POPPINS_ASCENT * 6;

            assertThat(low).as("the premise").isBetween(0.1, 0.5);
            assertThat(after(text)).as("cut, but by less than its call").isPositive()
                    .isLessThan(Math.round(low * 20 / 0.8));
        }
    }

    private static DocumentTableCell cell(String text, DocumentTextStyle face, DocumentTableTextAnchor anchor) {
        return DocumentTableCell.text(text).withStyle(style(face, anchor));
    }

    /** A cell composed from a paragraph, in Poppins as the text cells beside it. */
    private static DocumentTableCell composed(String text) {
        return DocumentTableCell.node(new ParagraphBuilder().text(text).textStyle(poppins(8))
                .margin(DocumentInsets.zero()).build()).withStyle(style(poppins(8), DocumentTableTextAnchor.CENTER));
    }

    private static DocumentTableStyle style(DocumentTextStyle face, DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder().padding(DocumentInsets.of(4)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0))
                .textStyle(face).textAnchor(anchor).build();
    }

    private static DocumentTextStyle poppins(double size) {
        return DocumentTextStyle.builder().fontName(FontName.POPPINS).size(size).build();
    }

    private static DocumentTextStyle ubuntu(double size) {
        return DocumentTextStyle.builder().fontName(FontName.UBUNTU).size(size).build();
    }

    private static DocumentTextStyle helvetica(double size) {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(size).build();
    }

    /** The exact line a cell's first paragraph is written at, in points. */
    private static double line(XWPFTableCell cell) {
        return DocxTwips.of(cell.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    /**
     * The exact line a cell's first paragraph stood at before it was traded for the space after
     * it, in points: for a cell with no space after its line of its own.
     */
    private static double written(XWPFTableCell cell) {
        return line(cell) + after(cell) / 20.0;
    }

    /** The rule a cell's first paragraph's line is written at. */
    private static STLineSpacingRule.Enum rule(XWPFTableCell cell) {
        return cell.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLineRule();
    }

    /** The space after a cell's first paragraph, in twips; 0 for none. */
    private static long after(XWPFTableCell cell) {
        return afterOf(cell.getParagraphs().get(0));
    }

    private static long afterOf(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0;
    }

    private static long lineOf(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine());
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
