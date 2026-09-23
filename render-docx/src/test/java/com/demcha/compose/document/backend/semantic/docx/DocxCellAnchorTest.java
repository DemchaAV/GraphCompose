package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table cell's anchor — where the page places its content in the cell's box — reaches Word.
 *
 * <p>The export read neither half of it. Word's default is the top left and the engine's is
 * the vertical middle on the left, so a single line beside a taller neighbour sat at the top
 * of its row, and every column a template aligns right or centred — amounts, quantities,
 * totals — came out flush left.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxCellAnchorTest {

    @Test
    void aCellWithNoAnchorIsCentredVerticallyAndLeftAsOnThePage() throws Exception {
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .row("Plain")));

        assertThat(cell.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.CENTER);
        assertThat(cell.getParagraphs().get(0).getCTP().getPPr() == null
                   || !cell.getParagraphs().get(0).getCTP().getPPr().isSetJc())
                .as("the left is where Word starts a line anyway")
                .isTrue();
    }

    @Test
    void eachAnchorMapsToItsWordAlignment() throws Exception {
        assertAnchor(DocumentTableTextAnchor.CENTER_RIGHT, XWPFTableCell.XWPFVertAlign.CENTER, ParagraphAlignment.RIGHT);
        assertAnchor(DocumentTableTextAnchor.CENTER, XWPFTableCell.XWPFVertAlign.CENTER, ParagraphAlignment.CENTER);
        assertAnchor(DocumentTableTextAnchor.CENTER_LEFT, XWPFTableCell.XWPFVertAlign.CENTER, null);
        assertAnchor(DocumentTableTextAnchor.TOP_LEFT, null, null);
        assertAnchor(DocumentTableTextAnchor.TOP_RIGHT, null, ParagraphAlignment.RIGHT);
        assertAnchor(DocumentTableTextAnchor.BOTTOM_LEFT, XWPFTableCell.XWPFVertAlign.BOTTOM, null);
        assertAnchor(DocumentTableTextAnchor.BOTTOM_RIGHT, XWPFTableCell.XWPFVertAlign.BOTTOM, ParagraphAlignment.RIGHT);
        // DEFAULT is Anchor.defaultAnchor() in the engine: the bottom left, as the renderer draws it.
        assertAnchor(DocumentTableTextAnchor.DEFAULT, XWPFTableCell.XWPFVertAlign.BOTTOM, null);
    }

    @Test
    void theMostSpecificAnchorWins() throws Exception {
        XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .columnStyle(1, DocumentTableStyle.builder().textAnchor(DocumentTableTextAnchor.CENTER_RIGHT).build())
                .row("Item", "120.00")
                .rowCells(DocumentTableCell.text("Total"),
                        DocumentTableCell.text("150.00").withStyle(DocumentTableStyle.builder()
                                .textAnchor(DocumentTableTextAnchor.TOP_LEFT).build()))));
        try (document) {
            var table = document.getTables().get(0);

            assertThat(table.getRow(0).getCell(1).getParagraphs().get(0).getAlignment())
                    .as("the column's")
                    .isEqualTo(ParagraphAlignment.RIGHT);
            XWPFTableCell own = table.getRow(1).getCell(1);
            assertThat(own.getVerticalAlignment()).as("the cell's own").isNull();
            assertThat(own.getParagraphs().get(0).getCTP().getPPr().isSetJc()).isFalse();
        }
    }

    @Test
    void aRightToLeftCellStartsOnTheRightAndAnAuthoredLeftIsItsEnd() throws Exception {
        // Word reads jc left/right as the start and end of a bidi paragraph's flow. The
        // engine's default for a right-to-left cell is the right, which is that flow's start.
        XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().direction(TextDirection.RTL).build())
                .rowCells(DocumentTableCell.text("שלום"),
                        DocumentTableCell.text("עולם").withStyle(DocumentTableStyle.builder()
                                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT).build()))));
        try (document) {
            var row = document.getTables().get(0).getRow(0);

            assertThat(row.getCell(0).getParagraphs().get(0).getCTP().getPPr().isSetJc())
                    .as("the start of a right-to-left line is its default")
                    .isFalse();
            assertThat(row.getCell(1).getParagraphs().get(0).getAlignment())
                    .as("the left edge is the end of a right-to-left line")
                    .isEqualTo(ParagraphAlignment.RIGHT);
        }
    }

    @Test
    void aComposedCellTakesTheVerticalHalfOnly() throws Exception {
        // The engine places a composed child vertically by the anchor and never across: the
        // child is laid out at the cell's full inner width, so there is no slack to align in.
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().textAnchor(DocumentTableTextAnchor.BOTTOM_RIGHT).build())
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Composed").build()))));

        assertThat(cell.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.BOTTOM);
        assertThat(cell.getParagraphs().get(0).getAlignment())
                .as("the paragraph keeps its own alignment")
                .isEqualTo(ParagraphAlignment.LEFT);
    }

    private static void assertAnchor(DocumentTableTextAnchor anchor,
                                     XWPFTableCell.XWPFVertAlign vertical,
                                     ParagraphAlignment horizontal) throws Exception {
        XWPFTableCell cell = firstCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder().textAnchor(anchor).build())
                .row("Cell")));
        var properties = cell.getParagraphs().get(0).getCTP().getPPr();

        assertThat(cell.getVerticalAlignment()).as(anchor + " vertically").isEqualTo(vertical);
        if (horizontal == null) {
            assertThat(properties == null || !properties.isSetJc()).as(anchor + " horizontally").isTrue();
        } else {
            assertThat(cell.getParagraphs().get(0).getAlignment()).as(anchor + " horizontally").isEqualTo(horizontal);
        }
    }

    private static XWPFTableCell firstCell(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, content)) {
            return document.getTables().get(0).getRow(0).getCell(0);
        }
    }
}
