package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.awt.Color;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Which paragraph a shape is anchored in, and where it is placed from.
 *
 * <p>A shape anchored to the page stays where the page put it when a reader edits the text
 * above it; the text moves and the shape does not. A shape standing beside a paragraph's text is
 * anchored in that paragraph and placed down from its top, so Word moves it with the paragraph.
 * The paragraph's top is where Word places the shape from: the space written above its first
 * line included, and its line where Word stands it, not where the page draws it.</p>
 */
class DocxDrawingAnchorsTest {

    @Test
    void aShapeIsAnchoredInTheParagraphWhoseTextItStandsNearest() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "First");
        XWPFParagraph second = text(document, "Second");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        // Both within reach, and the dot hangs from both: 32pt from the first, 2pt from the second.
        anchors.seat(0, first, first, 40, 300, 70, 80);
        anchors.seat(0, second, second, 40, 300, 100, 110);

        anchors.queue(List.of(dot(30, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(first)).isEmpty();
        String drawing = drawingsIn(second).get(0);
        assertThat(drawing).contains("layoutInCell=\"0\"")
                .contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>" + emu(30) + "<")
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
        // No space above the paragraph and no exact line: Word's top is the page's line top.
        assertThat(offsetDown(drawing)).isCloseTo(2, within(0.01));
    }

    @Test
    void reachIsFortyEightPointsAcrossAndDownTogether() {
        // 40pt down and 8pt across is in reach; 40pt down and 9pt across is not. The dot is 6pt
        // wide, so one 8pt left of the text starts at 40 - 8 - 6.
        assertThat(seated(dot(26, 140), 300)).as("48pt").isTrue();
        assertThat(seated(dot(25, 140), 300)).as("49pt").isFalse();
        // Across alone: a dot level with the text, 49pt right of where it ends.
        assertThat(seated(dot(349, 100), 300)).as("49pt across").isFalse();
    }

    /** Whether a dot is anchored beside a paragraph whose text starts at (40, 100). */
    private static boolean seated(DocxDrawings.Shape dot, double right) {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = text(document, "Text");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, true);
        anchors.seat(0, paragraph, paragraph, 40, right, 100, 110);
        anchors.queue(List.of(dot));
        anchors.endSection(0, null, document::createParagraph);
        return drawingsIn(paragraph).get(0).contains("<wp:positionV relativeFrom=\"paragraph\">");
    }

    @Test
    void aParagraphWithSomethingAboveItsLineOffersNoTop() {
        // Word sets a top border, contextual spacing and space reckoned in lines or by itself
        // between the paragraph's top and its line: its line's top does not tell where it starts.
        List<java.util.function.Consumer<CTPPr>> above = List.of(
                properties -> properties.addNewPBdr().addNewTop().setVal(
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE),
                properties -> properties.addNewContextualSpacing(),
                properties -> properties.addNewSpacing().setBeforeLines(BigInteger.valueOf(100)),
                properties -> properties.addNewSpacing().setBeforeAutospacing(
                        org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STOnOff1.ON));
        for (java.util.function.Consumer<CTPPr> something : above) {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph paragraph = text(document, "Text");
            something.accept(paragraph.getCTP().addNewPPr());
            DocxDrawingAnchors anchors = anchors();
            anchors.paragraphOn(0, paragraph, true);
            anchors.seat(0, paragraph, paragraph, 40, 300, 100, 110);
            anchors.queue(List.of(dot(30, 102)));
            anchors.endSection(0, null, document::createParagraph);

            assertThat(drawingsIn(paragraph).get(0)).as(paragraph.getCTP().getPPr().xmlText())
                    .contains("<wp:positionV relativeFrom=\"page\">");
        }
    }

    @Test
    void aShapeOutOfReachOfEveryParagraphStaysWhereThePagePutsIt() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "First");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, first, 40, 300, 40, 50);

        anchors.queue(List.of(dot(30, 200)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(first)).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\"><wp:posOffset>" + emu(200) + "<");
    }

    @Test
    void aShapeIsNeverPlacedAboveTheTopOfItsParagraph() {
        // The ring stands 20pt above the only paragraph near it. Placed from that paragraph, it
        // would hang above it, which LibreOffice does not keep in a table cell.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "First");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, first, 40, 300, 120, 130);

        anchors.queue(List.of(dot(30, 100)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(first)).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aShapeReachingAStrokePastItsParagraphsTopIsPlacedAtThatTop() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "First");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, first, 40, 300, 100, 110);

        anchors.queue(List.of(dot(30, 99.5)));
        anchors.endSection(0, null, document::createParagraph);

        String drawing = drawingsIn(first).get(0);
        assertThat(drawing).contains("<wp:positionV relativeFrom=\"paragraph\">");
        assertThat(offsetDown(drawing)).isZero();
    }

    @Test
    void anExactLinesTopIsWhereWordStandsItsBaselineLessTheSpaceAbove() {
        // A 15pt exact line, 12pt above it, its text lowered a point: Word stands the baseline
        // four fifths of the way down the line, raised by the run's position. The page draws the
        // baseline at 172, so Word's line starts at 172 - (12 - -1) = 159, its paragraph at 147.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = text(document, "Languages");
        spacing(paragraph, 240, 300);
        paragraph.getRuns().get(0).getCTR().addNewRPr().addNewPosition().setVal(BigInteger.valueOf(-2));
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, true);
        anchors.seat(0, paragraph, paragraph, 40, 300, 163.5, 172);

        anchors.queue(List.of(dot(30, 150)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(offsetDown(drawingsIn(paragraph).get(0))).isCloseTo(150 - 147, within(0.01));
    }

    @Test
    void aShapeInACellIsPlacedFromTheCellsTextColumn() {
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText("Entry");
        paragraph.getCTP().addNewPPr().addNewInd().setLeft(BigInteger.valueOf(200));
        cell.getCTTc().getTcPr().addNewTcMar().addNewLeft().setW(BigInteger.valueOf(0));
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, false);
        // The text starts 10pt into a cell whose left edge the page puts at 100.
        anchors.seat(0, paragraph, paragraph, 110, 290, 60, 70);

        anchors.queue(List.of(dot(103, 62)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph).get(0)).contains("layoutInCell=\"1\"")
                .contains("<wp:positionH relativeFrom=\"column\"><wp:posOffset>" + emu(3) + "<")
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
    }

    @Test
    void aCellThatCannotHoldTheShapeDoesNotCarryIt() {
        // The dot stands left of the cell, in the gap between two columns.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText("Entry");
        cell.getCTTc().getTcPr().addNewTcMar().addNewLeft().setW(BigInteger.valueOf(0));
        DocxDrawingAnchors anchors = anchors();
        XWPFParagraph body = text(document, "Body");
        anchors.paragraphOn(0, body, true);
        anchors.seat(0, paragraph, paragraph, 100, 290, 60, 70);

        anchors.queue(List.of(dot(80, 62)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph)).isEmpty();
        assertThat(drawingsIn(body)).singleElement().asString().contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aCellStatingNoMarginTakesWordsOwn() {
        // Neither the cell nor its table states a left margin: Word's own 5.4pt stands between
        // the cell's edge and its column, so a cell 200pt wide from 100 holds a dot at 100.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        if (table.getCTTbl().getTblPr().isSetTblCellMar()) {
            table.getCTTbl().getTblPr().unsetTblCellMar();
        }
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText("Entry");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, false);
        anchors.seat(0, paragraph, paragraph, 105.4, 290, 60, 70);

        anchors.queue(List.of(dot(100, 62)));
        anchors.endSection(0, null, document::createParagraph);

        String drawing = drawingsIn(paragraph).get(0);
        assertThat(offsetAcross(drawing)).isCloseTo(100 - 105.4, within(0.01));
        assertThat(offsetDown(drawing)).isCloseTo(2, within(0.01));
    }

    @Test
    void aShapeItsNearestParagraphCannotHoldIsNotGivenToOneFurtherOff() {
        // An icon centred on a note's line stands a little above the note's top; the totals
        // table's last cell above is within reach too, but it is not the text the icon is by.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        cell.getCTTc().getTcPr().addNewTcMar().addNewLeft().setW(BigInteger.valueOf(0));
        XWPFParagraph total = cell.getParagraphs().get(0);
        total.createRun().setText("Total due");
        XWPFParagraph note = text(document, "Thank you");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, note, true);
        anchors.seat(0, total, total, 110, 290, 60, 70);
        anchors.seat(0, note, note, 110, 290, 106, 116);

        anchors.queue(List.of(dot(120, 104)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(total)).isEmpty();
        assertThat(drawingsIn(note)).singleElement().asString().contains("<wp:positionV relativeFrom=\"page\">");
    }

    @Test
    void aCellOfARepeatedHeaderRowDoesNotCarryAShape() {
        // Word repeats the row on every page, and what is anchored in it with it.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        table.getRow(0).setRepeatHeader(true);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText("Header");
        DocxDrawingAnchors anchors = anchors();
        XWPFParagraph body = text(document, "Body");
        anchors.paragraphOn(0, body, true);
        anchors.seat(0, paragraph, paragraph, 110, 290, 60, 70);

        anchors.queue(List.of(dot(120, 62)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph)).isEmpty();
    }

    @Test
    void theParagraphHoldingTheTextTakesThePlaceNotTheEmptyLineBeforeIt() {
        // A paragraph the layout starts on a new page is written after an empty line holding its
        // top edge there; both are written for the same text, and only the second holds it.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph spacer = document.createParagraph();
        XWPFParagraph entry = text(document, "Entry");
        Object owner = new Object();
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, spacer, true);
        anchors.seat(0, owner, spacer, 40, 300, 100, 110);
        anchors.seat(0, owner, entry, 40, 300, 100, 110);

        anchors.queue(List.of(dot(30, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(spacer)).isEmpty();
        assertThat(drawingsIn(entry)).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
    }

    @Test
    void aLineCentredInACellDoesNotCarryAShape() {
        // Its indent is written short of the page's by the editor's slack, so its text's left
        // edge does not tell where the cell's column starts.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 1);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, 200);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText("Centred");
        paragraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        cell.getCTTc().getTcPr().addNewTcMar().addNewLeft().setW(BigInteger.valueOf(0));
        DocxDrawingAnchors anchors = anchors();
        XWPFParagraph body = text(document, "Body");
        anchors.paragraphOn(0, body, true);
        anchors.seat(0, paragraph, paragraph, 110, 290, 60, 70);

        anchors.queue(List.of(dot(120, 62)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph)).isEmpty();
    }

    @Test
    void aLinksTextRaisedInItsLineMovesTheTopWithIt() {
        // POI leaves an internal link's runs out of a paragraph's runs; the raise is read from
        // them all the same. As in the exact-line case: 172 - (12 - -1) - 12 = 147.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        spacing(paragraph, 240, 300);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run =
                paragraph.getCTP().addNewHyperlink().addNewR();
        run.addNewT().setStringValue("Linked");
        run.addNewRPr().addNewPosition().setVal(BigInteger.valueOf(-2));
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, true);
        anchors.seat(0, paragraph, paragraph, 40, 300, 163.5, 172);

        anchors.queue(List.of(dot(30, 150)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(offsetDown(drawingsIn(paragraph).get(0))).isCloseTo(150 - 147, within(0.01));
    }

    @Test
    void contextualSpacingSwitchedOffLeavesTheTopWhereItIs() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = text(document, "Entry");
        paragraph.getCTP().addNewPPr().addNewContextualSpacing().setVal("0");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, true);
        anchors.seat(0, paragraph, paragraph, 40, 300, 100, 110);

        anchors.queue(List.of(dot(30, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph)).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"paragraph\">");
    }

    @Test
    void shapesBesideOneParagraphAreWrittenInTheOrderTheyAreDrawn() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = text(document, "Title");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, paragraph, true);
        anchors.seat(0, paragraph, paragraph, 40, 300, 40, 50);

        anchors.queue(List.of(dot(10, 42), dot(20, 42)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(paragraph)).extracting(DocxDrawingAnchorsTest::offsetAcross)
                .containsExactly(10.0, 20.0);
    }

    private static DocxDrawingAnchors anchors() {
        AtomicLong ids = new AtomicLong(1);
        return new DocxDrawingAnchors(ids::getAndIncrement);
    }

    private static DocxDrawings.Shape dot(double x, double top) {
        return new DocxDrawings.Shape(DocxDrawings.Kind.ELLIPSE, x, top, 6, 6, Color.BLUE, null, 0, 0, false, 0);
    }

    private static XWPFParagraph text(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return paragraph;
    }

    private static void spacing(XWPFParagraph paragraph, long before, long line) {
        CTPPr properties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = properties.addNewSpacing();
        spacing.setBefore(BigInteger.valueOf(before));
        spacing.setLine(BigInteger.valueOf(line));
        spacing.setLineRule(STLineSpacingRule.EXACT);
    }

    private static void width(XWPFTableCell cell, double points) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcW().setType(STTblWidth.DXA);
        properties.getTcW().setW(BigInteger.valueOf(Math.round(points * 20)));
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
        assertThat(matcher.find()).isTrue();
        try {
            return Long.parseLong(matcher.group(1)) / 12700.0;
        } catch (NumberFormatException notAnOffset) {
            throw new AssertionError("not an offset in EMU: " + matcher.group(1), notAnOffset);
        }
    }

    private static long emu(double points) {
        return org.apache.poi.util.Units.toEMU(points);
    }
}
