package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
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
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                com.demcha.compose.document.style.DocumentColor.BLACK, 1.5))
                        .build())
                .row("First")
                .row("Second")));

        assertThat(margins(table.getRow(0).getCell(0))).as("the rule above the table, half the one between")
                .containsExactly(110L, 140L, 125L, 140L);
        assertThat(margins(table.getRow(1).getCell(0))).as("half the rule between, the rule below the table")
                .containsExactly(125L, 140L, 110L, 140L);
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

        // The first row's top, less the default 1pt rule above the table.
        assertThat(margins(table.getRow(0).getCell(0))[0]).isEqualTo(Math.round(5 * TWIPS_PER_POINT));
        assertThat(margins(table.getRow(0).getCell(1))[0]).isEqualTo(Math.round(10 * TWIPS_PER_POINT));
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
