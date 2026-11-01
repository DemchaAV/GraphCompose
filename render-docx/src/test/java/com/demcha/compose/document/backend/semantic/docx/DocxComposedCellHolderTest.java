package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which cell of a table takes a drawing its composed cells paint: the one whose box the layout
 * placed it in, found by geometry, since such a drawing has no node of its own.
 *
 * <p>A service line's tile — a bordered square round an icon, drawn as several shapes and so not
 * the one drawing its cell is all of — stood where the page had put it: edited above, every row
 * of the invoice moved and the tiles did not.</p>
 */
class DocxComposedCellHolderTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<path d='M0 0 L10 0 L10 10 Z' fill='#ff0000'/></svg>");

    @Test
    void aTilesDrawingsAreAnchoredInTheCellOfTheRowThatHoldsThem() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(line()))))) {
            XWPFTableCell composed = document.getTables().get(0).getRow(1).getCell(0);
            XWPFParagraph carrier = composed.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(placedFrom(carrier)).as("the tile and its icon, in the tile's own cell")
                    .isNotEmpty().allMatch(from -> from == STRelFromV.PARAGRAPH);
            assertThat(onThePage(document)).as("none left to the page").isZero();
        }
    }

    @Test
    void aCellTheRowLeavesRoomToCentreInTakesNone() throws Exception {
        // The same cell and the same tile, with six lines of text in the cell beside it: the row
        // is then far taller than the tile's cell and the page leaves that cell room to centre
        // in, where LibreOffice 24.2 would set its row's text from the top for a drawing.
        try (XWPFDocument document = DocxExports.withLayout(500, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Above"), DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(line()),
                        new DocumentTableCell(List.of("One", "Two", "Three", "Four", "Five", "Six"), null))))) {
            XWPFTableCell composed = document.getTables().get(0).getRow(1).getCell(0);
            XWPFParagraph carrier = composed.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(placedFrom(carrier)).as("nothing in the tile's cell").isEmpty();
            assertThat(onThePage(document)).as("left to the page, as before").isPositive();
        }
    }

    @Test
    void oneNodeComposedIntoTwoCellsTakesNoCell() throws Exception {
        // The same node value in two cells. The layout lays such a node out once, so the lines of
        // the paragraphs written for it in the second cell are the first cell's, and a cell that
        // cannot say where its own content stands cannot be given a drawing to place from it —
        // the second cell's drawings would follow the first cell's row. Both cells' drawings keep
        // to the text nearest them, as they did before this rule, which is where the page puts
        // them.
        DocumentNode shared = line();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(shared))
                .rowCells(DocumentTableCell.node(shared))))) {
            for (int row = 1; row <= 2; row++) {
                XWPFParagraph carrier = document.getTables().get(0).getRow(row).getCell(0)
                        .getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
                assertThat(placedFrom(carrier)).as("row " + row + " takes none").isEmpty();
            }
            assertThat(onThePage(document)).as("left where the page puts them").isPositive();
        }
    }

    @Test
    void aComposedCellOfARepeatedHeaderRowTakesNone() throws Exception {
        // Word repeats such a row, and what is anchored in it, on every page the table reaches,
        // so the drawing would stand on each of them beside the layout's own copy there.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(line()))
                .rowCells(DocumentTableCell.text("Below"))
                .repeatHeader()))) {
            XWPFParagraph carrier = document.getTables().get(0).getRow(0).getCell(0)
                    .getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(placedFrom(carrier)).as("the header's own cell takes none").isEmpty();
            assertThat(onThePage(document)).as("left where the page puts them").isPositive();
        }
    }

    @Test
    void theReportSaysWhereWhatACellsDrawGoes() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(line()))));

        assertThat(report.notes()).filteredOn(note -> "cell drawing".equals(note.subject()))
                .singleElement().extracting(DocxExportReport.Note::detail).asString()
                .contains("anchored in the paragraph of the cell whose box holds it that places it at the "
                          + "smallest offset, the paragraphs whose line it stands level with or below taken first")
                .contains("moves with that cell's row when the text above is edited");
    }

    /** One service line: a tile of a bordered square round an icon, and the line's text beside it. */
    private static DocumentNode line() {
        return new RowBuilder().name("Line").verticalAlign(RowVerticalAlign.CENTER)
                .weights(24, 276)
                .add(new ShapeContainerBuilder().name("Tile").rectangle(24, 24)
                        .stroke(DocumentStroke.of(DocumentColor.of(java.awt.Color.GRAY), 1))
                        .layer(ICON.node(12), com.demcha.compose.document.node.LayerAlign.CENTER)
                        .build())
                .addParagraph(p -> p.name("Title").text("Compute Instances"))
                .build();
    }

    /** What each drawing anchored in a paragraph of a cell is placed down from. */
    private static List<STRelFromV.Enum> placedFrom(XWPFParagraph paragraph) {
        List<STRelFromV.Enum> from = new ArrayList<>();
        for (CTAnchor anchor : anchorsIn(paragraph.getCTP())) {
            from.add(anchor.getPositionV().getRelativeFrom());
        }
        return from;
    }

    /** How many of the document's drawings are placed down from the page's edge. */
    private static long onThePage(XWPFDocument document) {
        return anchorsIn(document.getDocument()).stream()
                .filter(anchor -> anchor.getPositionV().getRelativeFrom() == STRelFromV.PAGE)
                .count();
    }

    private static List<CTAnchor> anchorsIn(XmlObject root) {
        List<CTAnchor> anchors = new ArrayList<>();
        for (XmlObject found : root.selectPath("declare namespace wp='"
                                               + "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'"
                                               + " .//wp:anchor")) {
            anchors.add((CTAnchor) found);
        }
        return anchors;
    }
}
