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
 * A table is as wide as the document says — in the cases where the document says.
 *
 * <p>Nothing used to write a width at all, so Word sized every table to its own content
 * and a table of short values came out far narrower than the reference draws it. But the
 * fix is not "as wide as the page": with no stated width the engine gives a table the sum
 * of its natural column widths, and an {@code auto} column's natural width is its widest
 * unwrapped cell. That is a measurement, and this backend has no font runtime to make it,
 * so the content width would be a guess that happens to be right for a table whose text
 * fills the line and wrong for one with three short values in it.</p>
 *
 * <p>What is knowable without measuring is written: a width the author stated, and the
 * column widths when every column is fixed. What is not stays Word's until resolved
 * layout can supply the measured widths.</p>
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

    private static XWPFTable onlyTable(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, 600)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }
}
