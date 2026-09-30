package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table cell keeps the space the document put inside its edges.
 *
 * <p>It was never written, so Word used its own — 5.4pt at each side and <em>nothing</em>
 * above or below. Measured on the probe corpus, every row of a five-row table came out
 * 8.1pt short of the page, because a row's height is its content's box and the padding is
 * part of that box.</p>
 *
 * <p>Word holds this as {@code w:tcMar}, so it is a mapping. All four sides are written,
 * and written even when they are zero: Word's default is not zero, so a table that asked
 * for no padding would otherwise export with Word's side margins.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxCellPaddingTest {

    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void aStatedPaddingReachesAllFourSides() throws Exception {
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(9, 7, 5, 3))
                        .build())
                .row("Padded")));

        // Above and below, less the engine's default 1pt rule, which Word gives room of its own.
        assertThat(margins(cell)).containsExactly(160L, 140L, 80L, 60L);
    }

    @Test
    void aRuledCellGivesItsRulesOutOfItsPaddingWhereWordPutsThem() throws Exception {
        // Word gives a rule between two rows half to each, and the rules above and below the
        // table whole to their row: measured, a 1.5pt rule made each row 1.5pt taller and a
        // one-row table 3pt taller. The sides keep their padding.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7))
                        .stroke(DocumentStroke.of(
                                DocumentColor.BLACK, 1.5))
                        .build())
                .row("First")
                .row("Second")));

        assertThat(margins(table.getRow(0).getCell(0))).as("the rule above the table, half the one between")
                .containsExactly(110L, 140L, 125L, 140L);
        assertThat(margins(table.getRow(1).getCell(0))).as("half the rule between, the rule below the table")
                .containsExactly(125L, 140L, 110L, 140L);
    }

    @Test
    void betweenRowsRuledDifferentlyTheLowerRowsRuleIsTheOneGivenUp() throws Exception {
        // Measured, a 1.5pt header over 0.5pt rows stepped as 0.5pt rules do: Word makes room
        // at the edge for the lower row's rule.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5)).build())
                .rowStyle(0, DocumentTableStyle.builder().stroke(DocumentStroke.of(DocumentColor.BLACK, 1.5)).build())
                .row("Header")
                .row("Body")));

        assertThat(margins(table.getRow(0).getCell(0))).as("its own 1.5pt above, half the body's 0.5pt below")
                .containsExactly(110L, 140L, 135L, 140L);
        assertThat(margins(table.getRow(1).getCell(0))).as("half its own 0.5pt above, its own below")
                .containsExactly(135L, 140L, 130L, 140L);
    }

    @Test
    void aCellSpanningToTheLastRowGivesUpTheRuleBelowTheTable() throws Exception {
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7)).stroke(DocumentStroke.of(DocumentColor.BLACK, 1.5)).build())
                .rowCells(DocumentTableCell.text("Tall").rowSpan(2), DocumentTableCell.text("Top"))
                .rowCells(DocumentTableCell.text("Low"))));

        assertThat(margins(table.getRow(0).getCell(0))).as("above the table and below it, both whole")
                .containsExactly(110L, 140L, 110L, 140L);
    }

    @Test
    void aRuleOfNoWidthGivesUpNothingAndPaddingThinnerThanItsShareGivesWhatItHas() throws Exception {
        XWPFTableCell unruled = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(7)).stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .row("Unruled")));
        XWPFTableCell thin = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(1)).stroke(DocumentStroke.of(DocumentColor.BLACK, 2)).build())
                .row("Thin")));

        assertThat(margins(unruled)).containsExactly(140L, 140L, 140L, 140L);
        assertThat(margins(thin)).as("none left above and below, the sides kept").containsExactly(0L, 20L, 0L, 20L);
    }

    @Test
    void aTableThatStatesNoRuleIsRuledWithTheEnginesOwnDefault() throws Exception {
        // Not Word's thinner grid: the page draws such a table with 1pt black rules, and the
        // rows are as tall as those rules make them.
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .row("Plain")));
        var top = cell.getCTTc().getTcPr().getTcBorders().getTop();

        assertThat(top.getVal().toString()).isEqualTo("single");
        assertThat(((Number) top.getSz()).intValue()).as("1pt in eighths").isEqualTo(8);
        assertThat(top.xgetColor().getStringValue()).as("black").isEqualToIgnoringCase("000000");
        assertThat(DocxSemanticBackend.ENGINE_DEFAULT_CELL_STROKE.width())
                .isEqualTo(TableCellLayoutStyle.DEFAULT.stroke().width());
    }

    @Test
    void aTableThatStatesNothingIsWrittenWithTheEnginesOwnDefault() throws Exception {
        // Not Word's 5.4pt a side and nothing above: the page is laid out with 4pt all
        // round, and the file has to say the same or the rows come out shorter than drawn —
        // less, above and below, the engine's default 1pt rule, which Word gives room of its own.
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .row("Plain")));

        long four = Math.round(4 * TWIPS_PER_POINT);
        long three = Math.round(3 * TWIPS_PER_POINT);
        assertThat(margins(cell)).containsExactly(three, four, three, four);
    }

    @Test
    void theDefaultIsTheOneTheEngineLaysOutWith() throws Exception {
        // The engine's copy is internal and cannot be read from the backend, so the two
        // are pinned together here: if the layout's default moves, this fails rather than
        // the export quietly drawing rows of a height nothing asked for.
        assertThat(DocxSemanticBackend.ENGINE_DEFAULT_CELL_PADDING_POINTS)
                .isEqualTo(TableCellLayoutStyle.DEFAULT.padding().top())
                .isEqualTo(TableCellLayoutStyle.DEFAULT.padding().bottom())
                .isEqualTo(TableCellLayoutStyle.DEFAULT.padding().left())
                .isEqualTo(TableCellLayoutStyle.DEFAULT.padding().right());
    }

    @Test
    void aCellsOwnPaddingBeatsTheRowsAndTheTablesTest() throws Exception {
        // The same cascade the layout pipeline merges in, applied per field.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(2))
                        .build())
                .rowStyle(0, DocumentTableStyle.builder().padding(DocumentInsets.of(6)).build())
                .rowCells(DocumentTableCell.text("row wins"),
                        DocumentTableCell.text("cell wins").withStyle(
                                DocumentTableStyle.builder().padding(DocumentInsets.of(11)).build()))));

        // The first row's top, less the default 1pt rule above the table: the row's smaller top
        // as the cells' margin, the rest of the larger in the cell's first paragraph.
        assertThat(margins(table.getRow(0).getCell(0))[0]).isEqualTo(Math.round(5 * TWIPS_PER_POINT));
        assertThat(margins(table.getRow(0).getCell(1))[0] + before(table.getRow(0).getCell(1)))
                .isEqualTo(Math.round(10 * TWIPS_PER_POINT));
    }

    @Test
    void aRowsCellsShareItsSmallestVerticalMarginsTheRestWrittenInTheirParagraphs() throws Exception {
        // Word and LibreOffice give every cell of a row the row's largest top and bottom margin:
        // a cell padded 8pt beside one padded 2pt made the row as tall as if both were padded 8.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("tight"),
                        DocumentTableCell.text("loose").withStyle(DocumentTableStyle.builder()
                                .padding(new DocumentInsets(8, 2, 6, 2)).build()))));
        XWPFTableCell tight = table.getRow(0).getCell(0);
        XWPFTableCell loose = table.getRow(0).getCell(1);

        assertThat(margins(loose)[0]).as("top, as the tight cell's").isEqualTo(margins(tight)[0]);
        assertThat(margins(loose)[2]).as("bottom, as the tight cell's").isEqualTo(margins(tight)[2]);
        assertThat(margins(loose)[0] + before(loose)).as("its own 8pt above").isEqualTo(Math.round(8 * TWIPS_PER_POINT));
        assertThat(margins(loose)[2] + after(loose)).as("its own 6pt below").isEqualTo(Math.round(6 * TWIPS_PER_POINT));
    }

    @Test
    void aCellOpeningWithATableKeepsItsMarginAndTheRowComesToIt() throws Exception {
        // The nested table has no paragraph above it to hold its cell's 5pt: the row's top comes
        // to 5pt in Word, and the 3pt cell beside it is not given its padding twice.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("tight"),
                        DocumentTableCell.text("three").withStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(3)).build()),
                        DocumentTableCell.node(new com.demcha.compose.document.dsl.TableBuilder()
                                        .columns(DocumentTableColumn.auto()).row("inner").build())
                                .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(5)).build()))));
        XWPFTableCell three = table.getRow(0).getCell(1);

        assertThat(table.getRow(0).getCell(2).getBodyElements().get(0)).isInstanceOf(XWPFTable.class);
        assertThat(margins(table.getRow(0).getCell(2))[0]).as("the nested table's cell keeps its own")
                .isEqualTo(Math.round(5 * TWIPS_PER_POINT));
        assertThat(margins(three)[0]).as("left as it was").isEqualTo(Math.round(3 * TWIPS_PER_POINT));
        assertThat(before(three)).as("no padding written twice").isZero();
    }

    @Test
    void aCellInAVerticalMergeHoldsTheRowsBottomUpToItsOwn() throws Exception {
        // The merged cell keeps its 10pt below; a 6pt cell beside it in the row is left as it
        // was, not lowered to the 2pt cell's and given its padding in its paragraph as well.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("spans").rowSpan(2).withStyle(DocumentTableStyle.builder()
                                .padding(new DocumentInsets(2, 2, 10, 2)).build()),
                        DocumentTableCell.text("six").withStyle(DocumentTableStyle.builder()
                                .padding(new DocumentInsets(2, 2, 6, 2)).build()),
                        DocumentTableCell.text("two"))
                .rowCells(DocumentTableCell.text("second"), DocumentTableCell.text("third"))));
        XWPFTableCell six = table.getRow(0).getCell(1);

        assertThat(margins(six)[2]).as("left as it was").isEqualTo(Math.round(6 * TWIPS_PER_POINT));
        assertThat(after(six)).as("no padding written twice").isZero();
    }

    @Test
    void aCellInAVerticalMergeKeepsItsMargins() throws Exception {
        // It spans rows whose margins are evened apart: its bottom edge is in the last row.
        XWPFTable table = firstTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build())
                .rowCells(DocumentTableCell.text("spans").rowSpan(2).withStyle(DocumentTableStyle.builder()
                                .padding(new DocumentInsets(2, 2, 10, 2)).build()),
                        DocumentTableCell.text("first"))
                .rowCells(DocumentTableCell.text("second"))));
        XWPFTableCell spans = table.getRow(0).getCell(0);

        assertThat(margins(spans)[0]).as("its own top").isEqualTo(Math.round(2 * TWIPS_PER_POINT));
        assertThat(margins(spans)[2]).as("its own bottom").isEqualTo(Math.round(10 * TWIPS_PER_POINT));
        assertThat(after(spans)).as("none written below it").isZero();
    }

    private static long before(XWPFTableCell cell) {
        var spacing = cell.getParagraphs().get(0).getCTP().getPPr().getSpacing();
        return spacing != null && spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static long after(XWPFTableCell cell) {
        var paragraphs = cell.getParagraphs();
        var spacing = paragraphs.get(paragraphs.size() - 1).getCTP().getPPr().getSpacing();
        return spacing != null && spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0;
    }

    @Test
    void aTableThatAsksForNoPaddingGetsNoneRatherThanWords() throws Exception {
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.zero())
                        .build())
                .row("Tight")));

        assertThat(margins(cell)).containsExactly(0L, 0L, 0L, 0L);
    }

    /** Top, right, bottom, left in twips. */
    private static long[] margins(XWPFTableCell cell) {
        CTTcMar mar = cell.getCTTc().getTcPr().getTcMar();
        return new long[]{width(mar.getTop()), width(mar.getRight()),
                width(mar.getBottom()), width(mar.getLeft())};
    }

    private static long width(CTTblWidth margin) {
        return margin == null || margin.getW() == null
                ? -1
                : DocxTwips.of(margin.getW());
    }

    private static XWPFTableCell firstCell(Consumer<PageFlowBuilder> content) throws Exception {
        return firstTable(content).getRow(0).getCell(0);
    }

    private static XWPFTable firstTable(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            return document.getTables().get(0);
        }
    }
}
