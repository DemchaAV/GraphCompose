package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A drawing that is all a table cell holds is anchored in that cell's paragraph, placed from its
 * top and the cell's text column, so it moves with its row wherever Word sets the rows above.
 *
 * <p>Placed from the page's edges, {@code CobaltRota}'s band icons — each alone in the first
 * column of its navy strip — stood 4pt, 9pt and 14pt above their labels in Word, the last out of
 * its strip, as the rows above them came out a little taller than the page's.</p>
 */
class DocxCellDrawingTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<path d='M0 0 L10 0 L10 10 Z' fill='#ff0000'/></svg>");

    private static final SvgIcon BLUE_ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                           + "<path d='M0 0 L10 0 L10 10 Z' fill='#0000ff'/></svg>");

    @Test
    void anIconAloneInACellComposedInATableIsAnchoredInThatCell() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(band(ICON, 12)))))) {
            XWPFParagraph carrier = iconCell(document.getTables().get(0).getRow(1).getCell(0)).getParagraphs().get(0);

            assertThat(anchors(carrier)).as("the icon, in its own cell")
                    .containsExactly(new Anchor("column", 0, "paragraph", 0, 12));
            assertThat(DocxTwips.of(carrier.getCTP().getPPr().getSpacing().getLine()))
                    .as("its paragraph as tall as the icon").isEqualTo(240);
            assertThat(shapesOutOfTheirCells(document)).as("none left out of its cell").isZero();
        }
    }

    @Test
    void iconsOfOneSizeInTwoCellsAreEachAnchoredInTheirOwn() throws Exception {
        // Each band's cell takes the drawing the layout placed inside it: the red icon in the
        // first, the blue one in the second.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(band(ICON, 12)))
                .rowCells(DocumentTableCell.text("Between"))
                .rowCells(DocumentTableCell.node(band(BLUE_ICON, 12)))))) {
            XWPFTable table = document.getTables().get(0);
            String first = iconCell(table.getRow(0).getCell(0)).getParagraphs().get(0).getCTP().xmlText();
            String second = iconCell(table.getRow(2).getCell(0)).getParagraphs().get(0).getCTP().xmlText();

            assertThat(first).as("the red icon in the first band").contains("layoutInCell=\"1\"")
                    .contains("FF0000").doesNotContain("0000FF");
            assertThat(second).as("the blue one in the second").contains("layoutInCell=\"1\"")
                    .contains("0000FF").doesNotContain("FF0000");
            assertThat(shapesOutOfTheirCells(document)).isZero();
        }
    }

    @Test
    void aHeadersIconRepeatedOnTheNextPageIsNotTakenByARowThere() throws Exception {
        // The layout draws a repeated header's icon again on every page; the export writes the
        // header once. A row takes only what the layout placed inside its own cell, so it never
        // takes the header's copy; the copies lie in the header's later boxes and are dropped.
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page.addTable(t -> {
            t.columns(DocumentTableColumn.fixed(300)).repeatHeader(1)
                    .headerCells(DocumentTableCell.node(band(ICON, 12)));
            for (int i = 0; i < 16; i++) {
                t.rowCells(DocumentTableCell.node(band(BLUE_ICON, 12)));
            }
        });
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 300).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            assertThat(session.layoutGraph().totalPages()).as("the table runs onto a second page").isGreaterThan(1);
        }
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, content)) {
            XWPFTable table = document.getTables().get(0);
            assertThat(xmlOfIcon(table, 0)).as("the header's own icon").contains("FF0000");
            for (int row = 1; row < table.getRows().size(); row++) {
                assertThat(xmlOfIcon(table, row)).as("row " + row + " holds its own icon")
                        .contains("layoutInCell=\"1\"").contains("0000FF").doesNotContain("FF0000");
            }
            // Word repeats the header row, the icon anchored in it with it, on every page: the
            // layout's copy there is not drawn again on the page.
            assertThat(table.getRow(0).getCtRow().getTrPr().sizeOfTblHeaderArray()).as("a repeated header").isPositive();
            assertThat(shapesOutOfTheirCells(document)).as("no second copy of the header's icon").isZero();
        }
    }

    @Test
    void anIconNoCellTookIsNotGivenToTheNextCell() throws Exception {
        // The first band's mark holds a line, so it stays on the page; the second band's icon,
        // of the same kind and size, takes its own fragments, not the first band's.
        DocumentNode ruledMark = new LayerStackBuilder().name("RuledMark")
                .layer(ICON.node(12))
                .layer(new LineBuilder().name("Underline").horizontal(12)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(200, 0, 0), 1)).build())
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().name("Ruled").verticalAlign(RowVerticalAlign.CENTER)
                        .weights(12, 288).add(ruledMark).addParagraph(p -> p.text("LABEL")).build()))
                .rowCells(DocumentTableCell.node(band(BLUE_ICON, 12)))))) {
            XWPFTable table = document.getTables().get(0);

            assertThat(xmlOfIcon(table, 0)).doesNotContain("layoutInCell=\"1\"");
            assertThat(xmlOfIcon(table, 1)).contains("layoutInCell=\"1\"").contains("0000FF").doesNotContain("FF0000");
            assertThat(shapesOutOfTheirCells(document)).as("the first mark and its line, out of the cell").isEqualTo(2);
        }
    }

    @Test
    void aShapeWaitingForItsPagesParagraphIsPaintedBeforeAnIconAnchoredInACellAfterIt() throws Exception {
        // The banner is drawn first and waits for a paragraph on its page, the page opening with
        // a table; the icon is anchored in its cell at once. Painted in the order drawn, the
        // banner stays under the icon.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Band", row -> row.weights(150, 12, 138).verticalAlign(RowVerticalAlign.CENTER)
                        .addShape(shape -> shape.size(150, 20).fillColor(DocumentColor.rgb(230, 230, 240)))
                        .add(ICON.node(12))
                        .addParagraph(p -> p.text("LABEL"))))) {
            List<CTAnchor> drawn = anchorsIn(document.getDocument());
            CTAnchor banner = drawn.stream().filter(anchor -> !anchor.getLayoutInCell()).findFirst().orElseThrow();
            CTAnchor icon = drawn.stream().filter(CTAnchor::getLayoutInCell).findFirst().orElseThrow();

            assertThat(banner.getRelativeHeight()).as("the banner under the icon")
                    .isLessThan(icon.getRelativeHeight());
        }
    }

    @Test
    void aTableNestedInACellDoesNotLendItsCellsToTheOuterTable() throws Exception {
        // Neither table is named, so their cells carry the same names; the inner table's rows,
        // laid out with the outer table's first row, must not stand for the outer rows below it.
        DocumentNode inner = new com.demcha.compose.document.dsl.TableBuilder()
                .columns(DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("a"))
                .rowCells(DocumentTableCell.text("b"))
                .rowCells(DocumentTableCell.text("c"))
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.text("Nested"), DocumentTableCell.node(inner))
                .rowCells(DocumentTableCell.node(band(ICON, 12)), DocumentTableCell.text("One"))
                .rowCells(DocumentTableCell.node(band(BLUE_ICON, 12)), DocumentTableCell.text("Two"))))) {
            XWPFTable table = document.getTables().get(0);

            assertThat(xmlOfIcon(table, 1)).contains("layoutInCell=\"1\"").contains("FF0000");
            assertThat(xmlOfIcon(table, 2)).contains("layoutInCell=\"1\"").contains("0000FF");
            assertThat(shapesOutOfTheirCells(document)).isZero();
        }
    }

    @Test
    void aCellHoldingTwoDrawingsLeavesBothOnThePage() throws Exception {
        // The first mark has a margin, so it stays on the page; the second, of the same kind and
        // size, would otherwise take the first's fragments, the first of those waiting in the cell.
        DocumentNode inset = new com.demcha.compose.document.node.LayerStackNode("Inset",
                List.of(new com.demcha.compose.document.node.LayerStackNode.Layer(ICON.node(12))),
                DocumentInsets.zero(), DocumentInsets.of(2));
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().name("Pair").verticalAlign(RowVerticalAlign.CENTER)
                        .weights(16, 12, 272).add(inset).add(ICON.node(12)).addParagraph(p -> p.text("LABEL"))
                        .build()))))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("layoutInCell=\"1\"");
            assertThat(shapesOutOfTheirCells(document)).as("both marks, out of the cell").isEqualTo(2);
        }
    }

    @Test
    void aPaddedStackComposedInACellStaysOnThePage() throws Exception {
        // Its padding sets the icon in from the box the layout keeps for it, which the icon's own
        // box cannot stand for.
        DocumentNode padded = new com.demcha.compose.document.node.LayerStackNode("Padded",
                List.of(new com.demcha.compose.document.node.LayerStackNode.Layer(ICON.node(12))),
                DocumentInsets.of(4), DocumentInsets.zero());
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().name("Band").verticalAlign(RowVerticalAlign.CENTER)
                        .weights(20, 280).add(padded).addParagraph(p -> p.text("LABEL")).build()))))) {
            assertThat(xmlOfIcon(document.getTables().get(0), 0)).doesNotContain("layoutInCell=\"1\"");
            assertThat(shapesOutOfTheirCells(document)).isEqualTo(1);
        }
    }

    /** The markup of the paragraph holding the icon of a band in one of a table's rows. */
    private static String xmlOfIcon(XWPFTable table, int row) {
        return iconCell(table.getRow(row).getCell(0)).getParagraphs().get(0).getCTP().xmlText();
    }

    @Test
    void aDrawingWithAMarginStaysOnThePage() throws Exception {
        // The cell's text column is taken to start where the drawing does; a margin would move it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above"))
                .addRow("Band", row -> row.weights(16, 284).verticalAlign(RowVerticalAlign.CENTER)
                        .add(new ShapeContainerBuilder().name("Inset").circle(12)
                                .fillColor(DocumentColor.rgb(26, 86, 148))
                                .center(ICON.node(8))
                                .margin(DocumentInsets.of(2))
                                .build())
                        .addParagraph(p -> p.text("LABEL"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(anchors(cell.getParagraphs().get(0))).isEmpty();
            assertThat(shapesOutOfTheirCells(document)).as("the disc and its mark, out of the cell").isEqualTo(2);
        }
    }

    @Test
    void aLineAloneInACellOfARowInTheFlowStaysOnThePage() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above"))
                .addRow("Ruled", row -> row.weights(150, 150)
                        .add(new LayerStackBuilder().name("Divider")
                                .layer(new LineBuilder().name("Rule").horizontal(150)
                                        .stroke(DocumentStroke.of(DocumentColor.rgb(200, 200, 200), 1)).build())
                                .build())
                        .addParagraph(p -> p.text("LABEL"))))) {
            String body = document.getDocument().xmlText();

            assertThat(body).as("no drawing taken into a cell").doesNotContain("layoutInCell=\"1\"");
            assertThat(shapesOutOfTheirCells(document)).as("the line, out of the cell as before").isEqualTo(1);
        }
    }

    @Test
    void aMarkHoldingALineInACellOfARowInTheFlowStaysOnThePage() throws Exception {
        // Its line is drawn among the mark's layers, not a rule; the drawing is left as it was.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above"))
                .addRow("Band", row -> row.weights(12, 288).verticalAlign(RowVerticalAlign.CENTER)
                        .add(new LayerStackBuilder().name("RuledMark")
                                .layer(ICON.node(12))
                                .layer(new LineBuilder().name("Underline").horizontal(12)
                                        .stroke(DocumentStroke.of(DocumentColor.rgb(200, 0, 0), 1)).build())
                                .build())
                        .addParagraph(p -> p.text("LABEL"))))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("layoutInCell=\"1\"");
            assertThat(shapesOutOfTheirCells(document)).as("the mark and its line, out of the cell").isEqualTo(2);
        }
    }

    @Test
    void aDrawingPaintingPastItsBoxStaysOnThePage() throws Exception {
        // Word clips a drawing anchored in a cell to the cell: one reaching past its own box is
        // left to the page whole, not split between the two.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above"))
                .addRow("Band", row -> row.weights(20, 280).verticalAlign(RowVerticalAlign.CENTER)
                        .add(new LayerStackBuilder().name("Overhanging")
                                .layer(ICON.node(12))
                                .position(BLUE_ICON.node(12), 10, 0, LayerAlign.TOP_LEFT)
                                .build())
                        .addParagraph(p -> p.text("LABEL"))))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("layoutInCell=\"1\"");
            assertThat(shapesOutOfTheirCells(document)).as("both marks, out of the cell").isEqualTo(2);
        }
    }

    @Test
    void anIconAloneInACellOfARowInTheFlowIsAnchoredInThatCell() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above"))
                .addRow("Band", row -> row.weights(12, 288).verticalAlign(RowVerticalAlign.CENTER)
                        .add(ICON.node(12))
                        .addParagraph(p -> p.text("LABEL"))))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(anchors(cell.getParagraphs().get(0))).as("the icon, in its own cell")
                    .containsExactly(new Anchor("column", 0, "paragraph", 0, 12));
            assertThat(shapesOutOfTheirCells(document)).isZero();
        }
    }

    @Test
    void aLineAloneInACellIsStillWrittenAsARule() throws Exception {
        // A line in a stack of one layer is a rule, written as a paragraph's border.
        DocumentNode divider = new LayerStackBuilder().name("Divider")
                .layer(new LineBuilder().name("Rule").horizontal(300)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(200, 200, 200), 1)).build())
                .build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.text("Above"))
                .rowCells(DocumentTableCell.node(divider))
                .rowCells(DocumentTableCell.text("Below"))))) {
            XWPFParagraph rule = document.getTables().get(0).getRow(1).getCell(0).getParagraphs().get(0);

            assertThat(anchors(rule)).as("no drawing in the cell").isEmpty();
            assertThat(rule.getCTP().getPPr().isSetPBdr()).as("a paragraph border").isTrue();
        }
    }

    /** A navy strip's row: a mark of the given size beside its label. */
    private static DocumentNode band(SvgIcon mark, double icon) {
        return new RowBuilder().name("Band").verticalAlign(RowVerticalAlign.CENTER)
                .weights(icon, 300 - icon)
                .add(mark.node(icon))
                .addParagraph(p -> p.text("LABEL"))
                .build();
    }

    /** The cell of a band's row holding its icon: the first cell of the table nested in the band's cell. */
    private static XWPFTableCell iconCell(XWPFTableCell band) {
        return band.getTables().get(0).getRow(0).getCell(0);
    }

    /**
     * A drawing anchored in a paragraph.
     *
     * @param fromH  what it is placed across from
     * @param x      how far across, in points
     * @param fromV  what it is placed down from
     * @param y      how far down, in points
     * @param height its height, in points
     */
    private record Anchor(String fromH, double x, String fromV, double y, double height) {
    }

    /** The drawings anchored in a paragraph's cell, to a tenth of a point. */
    private static List<Anchor> anchors(XWPFParagraph paragraph) {
        List<Anchor> anchors = new ArrayList<>();
        for (CTAnchor anchor : anchorsIn(paragraph.getCTP())) {
            if (anchor.getLayoutInCell()) {
                anchors.add(new Anchor(anchor.getPositionH().getRelativeFrom().toString(),
                        points(anchor.getPositionH().getPosOffset()),
                        anchor.getPositionV().getRelativeFrom().toString(),
                        points(anchor.getPositionV().getPosOffset()),
                        points(anchor.getExtent().getCy())));
            }
        }
        return anchors;
    }

    /** Every drawing anchored under an element, in document order. */
    private static List<CTAnchor> anchorsIn(XmlObject root) {
        List<CTAnchor> anchors = new ArrayList<>();
        for (XmlObject found : root.selectPath("declare namespace wp='"
                                               + "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'"
                                               + " .//wp:anchor")) {
            anchors.add((CTAnchor) found);
        }
        return anchors;
    }

    private static double points(long emu) {
        return Math.round(emu / 12700.0 * 10) / 10.0;
    }

    /**
     * How many shapes the document places across from the page's edge, out of any cell: down
     * from the page's top edge, or from the top of the body paragraph beside them.
     */
    private static long shapesOutOfTheirCells(XWPFDocument document) {
        return anchorsIn(document.getDocument()).stream()
                .filter(anchor -> anchor.getPositionH().getRelativeFrom() == STRelFromH.PAGE
                                  && !anchor.getLayoutInCell())
                .count();
    }
}
