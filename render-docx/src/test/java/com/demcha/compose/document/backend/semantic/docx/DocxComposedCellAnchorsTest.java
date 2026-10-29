package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import java.awt.Color;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A shape a table's composed cell draws is anchored in a paragraph of that cell, so it moves with
 * the cell's row, and placed from the cell's text column and the paragraph's top.
 *
 * <p>Such a shape has no node of its own — a composed cell's content is laid out under its table
 * — so the cell found by the box the layout placed it in stands for the block holding it. An
 * invoice's service tiles stood where the page had put them: edited above, every row moved and
 * they did not, and LibreOffice 24.2, which sets those rows 1.96pt shorter than the page, left
 * them further out of their row with each one.</p>
 *
 * <p>A cell set to the middle or the bottom takes one only where the page fills it, since
 * LibreOffice sets a cell carrying a drawing — and the cell holding its table — from its top:
 * measured in Word 16.0.20527 and LibreOffice 24.2, a cell with room to centre in had its text
 * set 4pt and 23.75pt higher, and the text of the cell holding its table 43.7pt higher, while a
 * cell the page fills moved no line in either editor.</p>
 */
class DocxComposedCellAnchorsTest {

    private static final String TABLE = "Flow[0]/Table[1]";
    private static final String ROW = TABLE + "/row[0]/cell[0]/Row[0]";
    private static final String TITLE = ROW + "/Text[1]/Title[0]";

    @Test
    void aComposedCellsDrawingIsAnchoredInTheCellWhoseBoxHoldsIt() {
        // A tile 40pt wide in the row's first cell, its text in the second: the tile's own cell
        // takes it, placed from that cell's column and the row's first paragraph. A paragraph of
        // the body above the table is offered first and must not take it, however near it stands:
        // only a paragraph the cell holds tells where that cell's content starts.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.outside(), "Flow[0]/Before[0]", composed.outside(), 20, 300, 98, 108);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 99)), new DocxDrawingAnchors.HoldingCell(composed.cell(), true));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.outside())).isEmpty();
        String drawing = drawingsIn(composed.tileCell()).get(0);
        assertThat(drawing).contains("layoutInCell=\"1\"")
                .contains("<wp:positionH relativeFrom=\"column\">")
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
        // The tile's cell starts 40pt left of the text's, and the tile stands 2pt into it.
        assertThat(offsetAcross(drawing)).as("from its own cell's column").isCloseTo(2, within(0.01));
        assertThat(offsetDown(drawing)).as("1pt above the row's first paragraph").isCloseTo(-1, within(0.01));
    }

    @Test
    void aCellTheLayoutDoesNotFillCarriesNoneWhileItIsSetToTheMiddle() {
        // The same cell, the page leaving it room to centre in: LibreOffice would set its row's
        // text from the top for the drawing, so the drawing keeps the page.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 99)), new DocxDrawingAnchors.HoldingCell(composed.cell(), false));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.tileCell())).isEmpty();
        assertThat(drawingsIn(composed.outside())).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aCellSetToTheMiddleHoldingTextCarriesNoneOfItsOwn() {
        // The tile over the text's own cell, which is set to the middle and holds text: that cell
        // cannot carry it, since LibreOffice would set its text from the cell's top. The drawing
        // goes no further than the rule the export had before, which seats it beside that text —
        // read off its own offset, which that rule measures from the text and this one would
        // measure from the row's first paragraph.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(70, 100.5)), new DocxDrawingAnchors.HoldingCell(composed.cell(), true));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.tileCell())).isEmpty();
        String drawing = drawingsIn(composed.title()).get(0);
        assertThat(offsetAcross(drawing)).as("beside the text, from its own column").isCloseTo(10, within(0.01));
        assertThat(offsetDown(drawing)).as("half a point below the text's top").isCloseTo(0.5, within(0.01));
    }

    @Test
    void aDrawingNoCellsBoxHoldsKeepsToTheTextItStandsNearest() {
        // The table paints it across its cells: no cell's box holds it, and the rule the export
        // had before takes over — the paragraph whose text it stands nearest, within reach.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(70, 100.5)), null);
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.title())).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
    }

    @Test
    void aDrawingBesideNoTextOnItsPageStillKeepsThePage() {
        // No cell holds it and no text stands within reach: the page keeps it, as before.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 400)), null);
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.outside())).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aDrawingNoCellHoldsIsNotGivenToACellItRisesAbove() {
        // No cell's box holds it, so the text nearest it takes it — but only as far above that
        // text's paragraph as the shape is then placed at its top: a shape given a cell to carry
        // it from further above moved that cell's row 1.8pt up in the Linux LibreOffice the build
        // checks with, and its offset is now written rather than squared to nothing.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(70, 99.5)), null);
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.title())).isEmpty();
        assertThat(drawingsIn(composed.outside())).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aCellHoldingOnlyAPictureCarriesNoDrawing() {
        // Nothing to set lower means nothing at all: a picture is moved with the text, so a cell
        // holding one is not excused its vertical alignment.
        Composed composed = composed(new XWPFDocument());
        composed.tileCell().createRun().getCTR().addNewDrawing();
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 99)), new DocxDrawingAnchors.HoldingCell(composed.cell(), true));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(drawingsIn(composed.outside())).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aRiseWordWouldNotReadBackIsPlacedAtTheParagraphsTop() {
        // Word reports an offset of 0.05 to 0.29pt above a paragraph as one of its position
        // keywords rather than as a distance, and the editing protocol reads a drawing's place
        // through that: such a shape is placed at the paragraph's top instead.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 99.71)), new DocxDrawingAnchors.HoldingCell(composed.cell(), true));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(offsetDown(drawingsIn(composed.tileCell()).get(0))).isZero();
    }

    @Test
    void aRiseWordReadsBackKeepsItsOwnOffset() {
        // Measured in Word 16.0.20527: 0.29pt came back as a keyword and 0.30pt as itself, so the
        // two tests stand either side of the limit. 0.30pt itself has no exact double, and the
        // page's own arithmetic would decide it.
        Composed composed = composed(new XWPFDocument());
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, composed.outside(), true);
        anchors.seat(0, composed.title(), TITLE, composed.title(), 60, 300, 100, 110);

        queue(anchors, List.of(tile(22, 99.69)), new DocxDrawingAnchors.HoldingCell(composed.cell(), true));
        anchors.endSection(0, null, composed.document()::createParagraph);

        assertThat(offsetDown(drawingsIn(composed.tileCell()).get(0))).isCloseTo(-0.31, within(0.01));
    }

    /**
     * A table of one cell holding a row of two — a tile's cell and a text cell, both set to the
     * middle, as an invoice's service lines are written — and a paragraph of the body beside it.
     *
     * @param document the document
     * @param outside  a paragraph of the body, which carries what nothing else does
     * @param tileCell the first cell's paragraph, holding no text
     * @param title    the second cell's paragraph, holding the line's title
     * @param cell     the composed cell itself, which holds them both
     */
    private record Composed(XWPFDocument document, XWPFParagraph outside, XWPFParagraph tileCell,
                            XWPFParagraph title, XWPFTableCell cell) {
    }

    private static Composed composed(XWPFDocument document) {
        XWPFParagraph outside = text(document, "Before");
        XWPFTable outer = document.createTable(1, 1);
        XWPFTableCell composed = outer.getRow(0).getCell(0);
        width(composed, 300);
        margin(composed, 0);
        middle(composed);
        XWPFTable inner = new XWPFTable(composed.getCTTc().addNewTbl(), composed, 1, 1);
        composed.insertTable(composed.getTables().size(), inner);
        XWPFTableCell tile = inner.getRow(0).getCell(0);
        width(tile, 40);
        margin(tile, 0);
        middle(tile);
        XWPFTableCell beside = inner.getRow(0).createCell();
        width(beside, 260);
        margin(beside, 0);
        middle(beside);
        XWPFParagraph title = beside.getParagraphs().get(0);
        title.createRun().setText("Compute Instances");
        return new Composed(document, outside, tile.getParagraphs().get(0), title, composed);
    }

    private static DocxDrawingAnchors anchors() {
        AtomicLong ids = new AtomicLong(1);
        return new DocxDrawingAnchors(ids::getAndIncrement, DocxGrownLines.NONE::lineOf, path -> List.of());
    }

    /** Queues shapes a table's composed cell draws, held by a cell or by none. */
    private static void queue(DocxDrawingAnchors anchors, List<DocxDrawings.Shape> shapes,
                              DocxDrawingAnchors.HoldingCell held) {
        anchors.queueOrdered(anchors.ordered(shapes, -1),
                new DocxDrawingAnchors.Owner("cell drawing", TABLE, true), held);
    }

    /** A 20pt tile, as a service line's mark is. */
    private static DocxDrawings.Shape tile(double x, double top) {
        return new DocxDrawings.Shape(DocxDrawings.Kind.ROUND_RECT, x, top, 20, 20, Color.WHITE, Color.GRAY, 1, 4,
                false, 0);
    }

    private static XWPFParagraph text(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return paragraph;
    }

    private static void width(XWPFTableCell cell, double points) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcW().setType(STTblWidth.DXA);
        properties.getTcW().setW(BigInteger.valueOf(Math.round(points * 20)));
    }

    private static void margin(XWPFTableCell cell, double points) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcMar().addNewLeft().setW(BigInteger.valueOf(Math.round(points * 20)));
    }

    private static void middle(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewVAlign().setVal(STVerticalJc.CENTER);
    }

    private static List<String> drawingsIn(XWPFParagraph paragraph) {
        Matcher matcher = Pattern.compile("<wp:anchor .*?</wp:anchor>", Pattern.DOTALL)
                .matcher(paragraph.getCTP().xmlText());
        List<String> drawings = new java.util.ArrayList<>();
        while (matcher.find()) {
            drawings.add(matcher.group());
        }
        return drawings;
    }

    private static double offsetDown(String drawing) {
        return offset(drawing, "positionV");
    }

    private static double offsetAcross(String drawing) {
        return offset(drawing, "positionH");
    }

    private static double offset(String drawing, String axis) {
        Matcher matcher = Pattern.compile("<wp:" + axis + " relativeFrom=\"\\w+\"><wp:posOffset>(-?\\d+)<")
                .matcher(drawing);
        assertThat(matcher.find()).as("an offset on " + axis).isTrue();
        try {
            return Long.parseLong(matcher.group(1)) / 12700.0;
        } catch (NumberFormatException notAnOffset) {
            throw new AssertionError("not an offset in EMU: " + matcher.group(1), notAnOffset);
        }
    }
}
