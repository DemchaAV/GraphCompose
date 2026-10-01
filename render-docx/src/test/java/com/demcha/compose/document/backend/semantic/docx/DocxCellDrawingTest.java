package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            assertThat(pageAnchoredShapes(document)).as("none left on the page").isZero();
        }
    }

    @Test
    void iconsOfOneSizeInTwoCellsAreEachAnchoredInTheirOwn() throws Exception {
        // The table's drawings are taken in the order its cells are written: the first band's
        // icon, the red one, is the first of the two.
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
            assertThat(pageAnchoredShapes(document)).isZero();
        }
    }

    @Test
    void aHeadersIconRepeatedOnTheNextPageIsNotTakenByARowThere() throws Exception {
        // The layout draws a repeated header's icon again on every page; the export writes the
        // header once. On the second page its copy comes first among the table's drawings, and
        // matched by kind and size alone the next row took it and passed its own on, row by row.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page.addTable(t -> {
            t.columns(DocumentTableColumn.fixed(300)).repeatHeader(1)
                    .headerCells(DocumentTableCell.node(band(ICON, 12)));
            for (int i = 0; i < 16; i++) {
                t.rowCells(DocumentTableCell.node(band(BLUE_ICON, 12)));
            }
        }))) {
            XWPFTable table = document.getTables().get(0);
            assertThat(xmlOfIcon(table, 0)).as("the header's own icon").contains("FF0000");
            for (int row = 1; row < table.getRows().size(); row++) {
                assertThat(xmlOfIcon(table, row)).as("row " + row + " holds its own icon")
                        .contains("layoutInCell=\"1\"").contains("0000FF").doesNotContain("FF0000");
            }
            assertThat(pageAnchoredShapes(document)).as("the header's copy on the second page, left to the page")
                    .isEqualTo(1);
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
            String body = document.getDocument().xmlText();
            Matcher banner = Pattern.compile("relativeHeight=\"(\\d+)\"[^>]*layoutInCell=\"0\"").matcher(body);
            Matcher icon = Pattern.compile("relativeHeight=\"(\\d+)\"[^>]*layoutInCell=\"1\"").matcher(body);

            assertThat(banner.find()).isTrue();
            assertThat(icon.find()).isTrue();
            assertThat(Long.parseLong(banner.group(1))).as("the banner under the icon")
                    .isLessThan(Long.parseLong(icon.group(1)));
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
            assertThat(pageAnchoredShapes(document)).as("the disc and its mark, on the page").isEqualTo(2);
        }
    }

    @Test
    void aLineAloneInACellOfARowInTheFlowStaysARule() throws Exception {
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
            assertThat(pageAnchoredShapes(document)).isZero();
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

    private static final Pattern ANCHOR = Pattern.compile(
            "layoutInCell=\"1\".*?positionH relativeFrom=\"(\\w+)\"><wp:posOffset>(-?\\d+)</wp:posOffset>.*?"
            + "positionV relativeFrom=\"(\\w+)\"><wp:posOffset>(-?\\d+)</wp:posOffset>.*?"
            + "<wp:extent cx=\"\\d+\" cy=\"(\\d+)\"", Pattern.DOTALL);

    /** The drawings anchored in a paragraph's cell, to a tenth of a point. */
    private static List<Anchor> anchors(XWPFParagraph paragraph) {
        List<Anchor> anchors = new ArrayList<>();
        Matcher matcher = ANCHOR.matcher(paragraph.getCTP().xmlText());
        while (matcher.find()) {
            anchors.add(new Anchor(matcher.group(1), points(matcher.group(2)), matcher.group(3),
                    points(matcher.group(4)), points(matcher.group(5))));
        }
        return anchors;
    }

    private static double points(String emu) {
        return Math.round(Long.parseLong(emu) / 12700.0 * 10) / 10.0;
    }

    /** How many shapes the document places from the page's edges. */
    private static int pageAnchoredShapes(XWPFDocument document) {
        Matcher matcher = Pattern.compile("positionV relativeFrom=\"page\"").matcher(document.getDocument().xmlText());
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
