package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table is as wide as the document says — and, when a layout was compiled, as wide as it
 * came out.
 *
 * <p>These pin the export with nothing measured behind it, which is what a caller holding
 * a graph and a canvas gets, and what a document the engine cannot lay out falls back to.
 * There the rule is: write what is knowable and nothing else. A width the author stated and
 * a grid of fixed columns are the document's own numbers; an {@code auto} column's width is
 * its widest unwrapped cell, and measuring is what this backend has no font runtime for. So
 * "as wide as the page" is not a fallback — it would be right for a table whose text fills
 * the line and wrong for one holding three short values, which is how the old
 * shrink-to-content was wrong in the other direction.</p>
 *
 * <p>{@link #aMeasuredTableTakesEveryColumnFromTheLayout()} is the other half: given the
 * layout, the same table stops guessing entirely.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxTableWidthTest {

    /** A5-ish page with a 20pt margin: 360pt of content width. */
    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    /** Word counts table and column widths in twentieths of a point. */
    private static final double TWIPS_PER_POINT = 20.0;

    @Test
    void anAutoTableWithNoStatedWidthKeepsWordsOwnSizing() throws Exception {
        // The honest answer here is no answer: the width is the columns' natural widths,
        // which nothing in this backend can measure. Writing the content width instead
        // would be wrong in exactly the way the old shrink-to-fit was, in the other
        // direction.
        XWPFTable table = onlyTable(page -> page.addTable(t -> t
                .autoColumns(3)
                .headerRow("Item", "Qty", "Amount")
                .row("A", "1", "2")));

        assertThat(widthType(table)).as("POI's own size-to-content default, untouched").isEqualTo("auto");
        assertThat(widthTwips(table)).isZero();
        assertThat(table.getCTTbl().getTblGrid() == null
                   || table.getCTTbl().getTblGrid().sizeOfGridColArray() == 0)
                .as("and no grid either")
                .isTrue();
    }

    @Test
    void anAuthoredWidthIsWrittenEvenWhenTheColumnsAreAuto() throws Exception {
        // The total is stated, so it needs no measuring. How it divides still does.
        XWPFTable table = onlyTable(page -> page.addTable(t -> t
                .autoColumns(2)
                .width(200)
                .row("A", "B")));

        assertThat(widthTwips(table)).isEqualTo(Math.round(200 * TWIPS_PER_POINT));
        assertThat(widthType(table)).isEqualTo("dxa");
        assertThat(table.getCTTbl().getTblGrid() == null
                   || table.getCTTbl().getTblGrid().sizeOfGridColArray() == 0)
                .as("the split is Word's")
                .isTrue();
    }

    @Test
    void columnsThatAllStateAWidthAreWrittenExactlyAndSumToTheTable() throws Exception {
        XWPFTable table = onlyTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(60))
                .row("A", "B")));

        assertThat(widthTwips(table))
                .as("160pt of columns, not 360pt of page: the engine gives an unstated "
                    + "width the columns' natural sum")
                .isEqualTo(3200);
        var grid = table.getCTTbl().getTblGrid();
        assertThat(grid).isNotNull();
        assertThat(grid.sizeOfGridColArray()).isEqualTo(2);
        assertThat(twips(grid.getGridColArray(0).getW())).isEqualTo(2000);
        assertThat(twips(grid.getGridColArray(1).getW())).isEqualTo(1200);
    }

    @Test
    void aStatedWidthWiderThanFixedColumnsGoesToTheLastOne() throws Exception {
        // Mirrors resolveFinalColumnWidths: with no auto column to absorb the surplus,
        // the engine hands all of it to the last column. Spreading it evenly instead
        // would move every column edge but the first away from where the PDF draws it.
        XWPFTable table = onlyTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(60))
                .width(200)
                .row("A", "B")));

        assertThat(widthTwips(table)).isEqualTo(4000);
        var grid = table.getCTTbl().getTblGrid();
        assertThat(twips(grid.getGridColArray(0).getW())).as("untouched").isEqualTo(2000);
        assertThat(twips(grid.getGridColArray(1).getW()))
                .as("60pt plus the whole 40pt surplus")
                .isEqualTo(2000);
    }

    @Test
    void oneAutoColumnLeavesTheWholeTableToWord() throws Exception {
        // Mixing a measured column with stated ones cannot be written honestly: the auto
        // column's width is its content's, so neither the split nor the total is known.
        XWPFTable table = onlyTable(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.auto())
                .row("A", "B")));

        assertThat(widthType(table)).isEqualTo("auto");
        assertThat(table.getCTTbl().getTblGrid() == null
                   || table.getCTTbl().getTblGrid().sizeOfGridColArray() == 0)
                .isTrue();
    }

    @Test
    void aMeasuredTableTakesEveryColumnFromTheLayout() throws Exception {
        // The same table the first case leaves to Word. Given the layout there is nothing
        // to decide: the resolved cells carry the width each column came out at, auto
        // included, and the grid is written from them.
        XWPFTable table = measuredTable(page -> page.addTable(t -> t
                .autoColumns(3)
                .headerRow("Item", "Qty", "Amount")
                .row("Platform subscription", "12", "1 440.00")));

        var grid = table.getCTTbl().getTblGrid();
        assertThat(grid).isNotNull();
        assertThat(grid.sizeOfGridColArray()).isEqualTo(3);
        assertThat(twips(grid.getGridColArray(0).getW()))
                .as("the widest column is the one holding the longest text")
                .isGreaterThan(twips(grid.getGridColArray(1).getW()));
        assertThat(sumOfColumns(table))
                .as("the columns tile the table exactly")
                .isEqualTo(widthTwips(table));
        assertThat(widthTwips(table))
                .as("and the table is narrower than the page, because its content is")
                .isLessThan(Math.round(CONTENT_WIDTH * TWIPS_PER_POINT));
        assertThat(table.getCTTbl().getTblPr().getTblLayout().getType().toString())
                .as("fixed, so Word does not re-fit what was measured")
                .isEqualTo("fixed");
    }

    private static long sumOfColumns(XWPFTable table) {
        return table.getCTTbl().getTblGrid().getGridColList().stream()
                .mapToLong(column -> twips(column.getW()))
                .sum();
    }

    @Test
    void aRowCarriedAsATableSpansTheContentWidth() throws Exception {
        // A row takes the whole width it is offered whatever its children measure —
        // measureRow returns the available width unconditionally — so this one needs no
        // measuring either. POI's createTable writes w:tblW as w=0 type=auto, which
        // collapsed the pair around its text and moved both columns away from where the
        // fixed-layout render puts them.
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("Right"))));

        assertThat(widthType(table))
                .as("stated, not POI's size-to-content default")
                .isEqualTo("dxa");
        assertThat(widthTwips(table)).isEqualTo(Math.round(CONTENT_WIDTH * TWIPS_PER_POINT));
    }

    private static long widthTwips(XWPFTable table) {
        return twips(table.getCTTbl().getTblPr().getTblW().getW());
    }

    private static String widthType(XWPFTable table) {
        return table.getCTTbl().getTblPr().getTblW().getType().toString();
    }

    /** {@code ST_TwipsMeasure} is an xmlbeans union, so the accessor is typed Object. */
    private static long twips(Object measure) {
        return Long.parseLong(String.valueOf(measure));
    }

    /** The table as written with nothing measured behind it. */
    private static XWPFTable onlyTable(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(PAGE_WIDTH, 600, MARGIN, content)) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }

    /** The table as written when the compiled layout is there to read. */
    private static XWPFTable measuredTable(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, content)) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }
}
