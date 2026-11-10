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
 * A shape a node draws is anchored in a paragraph the nearest block holding it writes on its page
 * — the one it stands below that places it at the smallest offset — and placed from there however
 * far off it stands; one no block carries goes to the paragraph nearest it.
 *
 * <p>Chosen by where it stood, an icon laid over a card's corner stood 14pt above the card's
 * text and was left to the page, as was every skill bar of a sidebar's rows and its portrait:
 * edited above, the text moved and they did not. Anchored by what holds them, they move with
 * it. Measured in Word 16.0.20527 and LibreOffice 24.2, a shape in a table cell's paragraph stands
 * where both say at any offset — above its row, below its table, out of its cell — unless the
 * cell sets its content from the middle or the bottom, whose text LibreOffice moves for it.</p>
 */
class DocxDrawingOwnerAnchorsTest {

    private static final String STACK = "Flow[0]/Stack[1]";
    private static final String ICON = STACK + "/Icon[1]";

    @Test
    void aShapeIsAnchoredInAParagraphItsHolderWritesNotTheOneNearestItOnThePage() {
        // The paragraph above the stack stands 6pt from the icon; the card's text, the stack's
        // first paragraph, 14pt below it.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph before = text(document, "Before");
        XWPFParagraph card = text(document, "Card");
        XWPFParagraph more = text(document, "More");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, before, true);
        anchors.seat(0, before, "Flow[0]/Before[0]", before, 40, 300, 94, 104);
        anchors.seat(0, card, STACK + "/Card[0]/Text[0]", card, 54, 300, 114, 124);
        anchors.seat(0, more, STACK + "/Card[0]/More[1]", more, 54, 300, 130, 140);

        queue(anchors, ICON, List.of(dot(290, 100)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(before)).isEmpty();
        assertThat(drawingsIn(more)).isEmpty();
        String drawing = drawingsIn(card).get(0);
        assertThat(drawing).contains("<wp:positionV relativeFrom=\"paragraph\">")
                .contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>" + emu(290) + "<");
        assertThat(offsetDown(drawing)).as("14pt above the card's text").isCloseTo(-14, within(0.01));
    }

    @Test
    void howFarFromItsParagraphItStandsGivesOnlyItsOffset() {
        // Further below than any nearest paragraph's reach, and further above than a disc rises.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "Heading");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, STACK + "/Heading[0]", first, 40, 300, 200, 210);

        queue(anchors, ICON, List.of(dot(30, 320), dot(30, 140)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(first)).extracting(DocxDrawingOwnerAnchorsTest::offsetDown)
                .containsExactly(120.0, -60.0);
    }

    @Test
    void inACellItRisesAboveItsParagraphAsFarAsItStands() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph entry = cellParagraph(document, 200, "Entry");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, text(document, "Body"), true);
        anchors.seat(0, entry, STACK + "/Entry[0]", entry, 110, 290, 100, 110);

        queue(anchors, ICON, List.of(dot(120, 80)));
        anchors.endSection(0, null, document::createParagraph);

        String drawing = drawingsIn(entry).get(0);
        assertThat(drawing).contains("layoutInCell=\"1\"").contains("<wp:positionH relativeFrom=\"column\">");
        assertThat(offsetAcross(drawing)).as("from the cell's text column").isCloseTo(10, within(0.01));
        assertThat(offsetDown(drawing)).isCloseTo(-20, within(0.01));
    }

    @Test
    void aCellNotHoldingTheShapeAcrossKeepsItOutOfItsColumn() {
        // Its row has no other cell: the shape, left of the cell, is placed from the cell's column.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph entry = cellParagraph(document, 200, "Entry");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, text(document, "Body"), true);
        anchors.seat(0, entry, STACK + "/Entry[0]", entry, 110, 290, 100, 110);

        queue(anchors, ICON, List.of(dot(60, 104)));
        anchors.endSection(0, null, document::createParagraph);

        String drawing = drawingsIn(entry).get(0);
        assertThat(drawing).contains("layoutInCell=\"1\"");
        assertThat(offsetAcross(drawing)).isCloseTo(-50, within(0.01));
    }

    @Test
    void aShapeHoldingTextIsKeptOnlyByACellHoldingItAcross() {
        // A text box left of the cell, as the dot of the case above: LibreOffice 26.8 pulled one
        // anchored so back into the column. The body paragraph under the table places it instead.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph entry = cellParagraph(document, 200, "Entry");
        XWPFParagraph below = text(document, "Below");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, below, true);
        anchors.seat(0, entry, STACK + "/Entry[0]", entry, 110, 290, 100, 110);
        anchors.seat(0, below, STACK + "/Below[1]", below, 40, 300, 160, 170);

        queue(anchors, ICON, List.of(DocxDrawings.Shape.textBox(60, 104, 30, 12, 0, "<w:p/>")));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(entry)).isEmpty();
        String box = drawingsIn(below).get(0);
        assertThat(box).contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>" + emu(60) + "<");
        assertThat(offsetDown(box)).isCloseTo(-56, within(0.01));
    }

    @Test
    void itsOwnCellInTheRowOfItsHoldersParagraphTakesIt() {
        // A dot in an empty column beside its entry's text, as a timeline's: the dot's cell takes
        // it, placed from that cell's column and the row's top.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 2);
        XWPFTableCell dotCell = table.getRow(0).getCell(0);
        XWPFTableCell textCell = table.getRow(0).getCell(1);
        width(dotCell, 20);
        width(textCell, 200);
        margin(dotCell, 0);
        margin(textCell, 0);
        XWPFParagraph entry = textCell.getParagraphs().get(0);
        entry.createRun().setText("Entry");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, text(document, "Body"), true);
        anchors.seat(0, entry, "Flow[0]/Row[1]/Text[1]", entry, 120, 320, 100, 110);

        queue(anchors, "Flow[0]/Row[1]/Rail[0]/Dot[0]", List.of(dot(107, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(entry)).isEmpty();
        String drawing = drawingsIn(dotCell.getParagraphs().get(0)).get(0);
        assertThat(offsetAcross(drawing)).as("from the dot's own cell, 100pt across").isCloseTo(7, within(0.01));
        assertThat(offsetDown(drawing)).isCloseTo(2, within(0.01));
    }

    @Test
    void aLaterParagraphOfItsHolderNearerTheShapeDoesNotTakeItFromTheFirst() {
        // The card's second paragraph stands 2pt from the icon, its first 14pt.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph card = text(document, "Card");
        XWPFParagraph more = text(document, "More");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, card, true);
        anchors.seat(0, card, STACK + "/Card[0]/Text[0]", card, 54, 300, 114, 124);
        anchors.seat(0, more, STACK + "/Card[0]/More[1]", more, 54, 300, 130, 140);

        queue(anchors, ICON, List.of(dot(290, 128)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(more)).isEmpty();
        assertThat(offsetDown(drawingsIn(card).get(0))).isCloseTo(14, within(0.01));
    }

    @Test
    void theParagraphPlacingItAtTheSmallestOffsetTakesItNotTheOneWhoseLineIsNearest() {
        // Both paragraphs of the card stand above the dot: the second's line 10pt above it, the
        // first's 50pt. The second is written 60pt of space below the first, so Word places out of
        // it from 70pt above the dot, and out of the first from 50pt — and the offset is what the
        // file carries.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph card = text(document, "Card");
        XWPFParagraph spaced = text(document, "Spaced");
        spaced.setSpacingBefore(60 * 20);
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, card, true);
        anchors.seat(0, card, STACK + "/Card[0]/Text[0]", card, 54, 300, 150, 160);
        anchors.seat(0, spaced, STACK + "/Card[0]/Spaced[1]", spaced, 54, 300, 190, 200);

        queue(anchors, ICON, List.of(dot(290, 200)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(spaced)).as("its line is nearer, and it places from further off").isEmpty();
        assertThat(offsetDown(drawingsIn(card).get(0))).isCloseTo(50, within(0.01));
    }

    @Test
    void aParagraphOfAnotherCellOfTheRowTakesItWhereItPlacesItNearest() {
        // The holder writes both cells of the row. The right cell's own text stands 60pt above the
        // dot; the left cell's second paragraph, 4pt above it, takes it and places it out of the
        // left cell's column — the right cell takes a shape into its column only from the
        // paragraph that opens it. The offset and the column carry where the page puts the dot.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 2);
        XWPFTableCell left = table.getRow(0).getCell(0);
        XWPFTableCell right = table.getRow(0).getCell(1);
        width(left, 200);
        width(right, 100);
        margin(left, 0);
        margin(right, 0);
        XWPFParagraph opens = left.getParagraphs().get(0);
        opens.createRun().setText("Opens the left cell");
        XWPFParagraph later = left.addParagraph();
        later.createRun().setText("Later in the left cell");
        XWPFParagraph entry = right.getParagraphs().get(0);
        entry.createRun().setText("In the right cell");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, text(document, "Body"), true);
        anchors.seat(0, opens, "Flow[0]/Row[1]/Left[0]/Text[0]", opens, 40, 240, 100, 110);
        anchors.seat(0, later, "Flow[0]/Row[1]/Left[0]/Text[1]", later, 40, 240, 196, 206);
        anchors.seat(0, entry, "Flow[0]/Row[1]/Right[1]/Text[0]", entry, 240, 340, 140, 150);

        queue(anchors, "Flow[0]/Row[1]/Rail[2]/Dot[0]", List.of(dot(300, 200)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(opens)).as("the left cell's opening paragraph, 100pt above").isEmpty();
        assertThat(drawingsIn(entry)).as("the right cell's own text, 60pt above").isEmpty();
        String drawing = drawingsIn(later).get(0);
        assertThat(offsetDown(drawing)).as("4pt below the left cell's later line").isCloseTo(4, within(0.01));
        assertThat(offsetAcross(drawing)).as("out of the left cell's column, where the page puts it")
                .isCloseTo(260, within(0.01));
    }

    @Test
    void aParagraphWhoseLineStandsBelowItDoesNotTakeItThoughItWouldPlaceItNearer() {
        // The card's second paragraph is written below the dot and 20pt of space under its own
        // line, so it would place the dot 10pt down against the first paragraph's 50pt. It is
        // still the first that carries it: a shape laid over a block's top has to move with that
        // top, and an edit inside the block moves the text under it away.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph card = text(document, "Card");
        XWPFParagraph below = text(document, "Below");
        below.setSpacingBefore(20 * 20);
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, card, true);
        anchors.seat(0, card, STACK + "/Card[0]/Text[0]", card, 54, 300, 150, 160);
        anchors.seat(0, below, STACK + "/Card[0]/Below[1]", below, 54, 300, 210, 220);

        queue(anchors, ICON, List.of(dot(290, 200)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(below)).as("written below the dot, however near it would place it").isEmpty();
        assertThat(offsetDown(drawingsIn(card).get(0))).isCloseTo(50, within(0.01));
    }

    @Test
    void aHolderWritingNoParagraphOnItsPagePassesItUp() {
        // The stack's text is all on the page before. On the shape's page its section's heading
        // stands 30pt above it, and a note of another block 2pt below its top.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph earlier = text(document, "Card");
        XWPFParagraph heading = text(document, "Heading");
        XWPFParagraph note = text(document, "Note");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, earlier, true);
        anchors.paragraphOn(1, heading, true);
        anchors.seat(0, earlier, "Flow[0]/Section[2]/Stack[0]/Card[0]", earlier, 40, 300, 700, 710);
        anchors.seat(1, heading, "Flow[0]/Section[2]/Heading[1]", heading, 40, 300, 60, 70);
        anchors.seat(1, note, "Flow[0]/Aside[3]/Note[0]", note, 40, 300, 92, 102);

        queue(anchors, "Flow[0]/Section[2]/Stack[0]/Icon[1]", List.of(onPage(1, dot(30, 90))));
        anchors.endSection(1, null, document::createParagraph);

        assertThat(drawingsIn(earlier)).as("a paragraph on another page").isEmpty();
        assertThat(drawingsIn(note)).as("nearer, but no holder's").isEmpty();
        assertThat(offsetDown(drawingsIn(heading).get(0))).as("from the section's heading").isCloseTo(30, within(0.01));
    }

    @Test
    void aCellSetToTheMiddleOrTheBottomCarriesNoneAndTheNextHolderUpDoes() {
        for (STVerticalJc.Enum alignment : List.of(STVerticalJc.CENTER, STVerticalJc.BOTTOM)) {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph heading = text(document, "Heading");
            XWPFParagraph entry = cellParagraph(document, 200, "Entry");
            ((XWPFTableCell) entry.getBody()).getCTTc().getTcPr().addNewVAlign().setVal(alignment);
            XWPFParagraph note = text(document, "Note");
            DocxDrawingAnchors anchors = anchors();
            anchors.paragraphOn(0, heading, true);
            anchors.seat(0, heading, "Flow[0]/Section[0]/Heading[0]", heading, 40, 300, 60, 70);
            anchors.seat(0, entry, "Flow[0]/Section[0]/Row[1]/Entry[0]", entry, 110, 290, 100, 110);
            // Another block's paragraph, nearer the bar than the heading.
            anchors.seat(0, note, "Flow[0]/Aside[1]/Note[0]", note, 40, 300, 104, 114);

            queue(anchors, "Flow[0]/Section[0]/Row[1]/Bar[1]", List.of(dot(120, 102)));
            anchors.endSection(0, null, document::createParagraph);

            assertThat(drawingsIn(entry)).as(alignment.toString()).isEmpty();
            assertThat(drawingsIn(note)).as(alignment + ": no holder's").isEmpty();
            assertThat(drawingsIn(heading)).as(alignment + ": the section's").singleElement().asString()
                    .contains("<wp:positionV relativeFrom=\"paragraph\">");
        }
    }

    @Test
    void aHoldersNextParagraphTakesItWhereItsFirstCannot() {
        // The card's first paragraph stands in a cell set to the middle; its caption under the
        // table is the card's too. Another block's note stands nearer the icon than the caption.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph entry = cellParagraph(document, 200, "Entry");
        ((XWPFTableCell) entry.getBody()).getCTTc().getTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
        XWPFParagraph caption = text(document, "Caption");
        XWPFParagraph note = text(document, "Note");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, caption, true);
        anchors.seat(0, entry, "Flow[0]/Card[1]/Entry[0]", entry, 110, 290, 100, 110);
        anchors.seat(0, caption, "Flow[0]/Card[1]/Caption[1]", caption, 40, 300, 160, 170);
        anchors.seat(0, note, "Flow[0]/Aside[2]/Note[0]", note, 40, 300, 106, 116);

        queue(anchors, "Flow[0]/Card[1]/Icon[2]", List.of(dot(120, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(entry)).isEmpty();
        assertThat(drawingsIn(note)).isEmpty();
        assertThat(offsetDown(drawingsIn(caption).get(0))).isCloseTo(-58, within(0.01));
    }

    @Test
    void itsOwnCellMergedFromAboveLeavesItToTheParagraphsCellOutOfItsColumn() {
        // As a timeline's dot in a column of its own, but that cell continues one from the row
        // above: the text's cell keeps the dot, placed from its own column.
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable(1, 2);
        XWPFTableCell dotCell = table.getRow(0).getCell(0);
        XWPFTableCell textCell = table.getRow(0).getCell(1);
        width(dotCell, 20);
        width(textCell, 200);
        margin(dotCell, 0);
        margin(textCell, 0);
        dotCell.getCTTc().getTcPr().addNewVMerge()
                .setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.CONTINUE);
        XWPFParagraph entry = textCell.getParagraphs().get(0);
        entry.createRun().setText("Entry");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, text(document, "Body"), true);
        anchors.seat(0, entry, "Flow[0]/Row[1]/Text[1]", entry, 120, 320, 100, 110);

        queue(anchors, "Flow[0]/Row[1]/Rail[0]/Dot[0]", List.of(dot(107, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(dotCell.getParagraphs().get(0))).isEmpty();
        String drawing = drawingsIn(entry).get(0);
        assertThat(drawing).contains("layoutInCell=\"1\"");
        assertThat(offsetAcross(drawing)).as("left of the text's column").isCloseTo(-13, within(0.01));
    }

    @Test
    void aCellHeldInACellSetToTheMiddleCarriesNoneEither() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph heading = text(document, "Heading");
        XWPFTable outer = document.createTable(1, 1);
        XWPFTableCell holding = outer.getRow(0).getCell(0);
        width(holding, 300);
        holding.getCTTc().getTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
        XWPFTable inner = new XWPFTable(holding.getCTTc().addNewTbl(), holding, 1, 1);
        holding.insertTable(holding.getTables().size(), inner);
        XWPFTableCell cell = inner.getRow(0).getCell(0);
        width(cell, 200);
        margin(cell, 0);
        XWPFParagraph entry = cell.getParagraphs().get(0);
        entry.createRun().setText("Entry");
        XWPFParagraph note = text(document, "Note");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, heading, true);
        anchors.seat(0, heading, "Flow[0]/Section[0]/Heading[0]", heading, 40, 300, 60, 70);
        anchors.seat(0, entry, "Flow[0]/Section[0]/Row[1]/Entry[0]", entry, 110, 290, 100, 110);
        anchors.seat(0, note, "Flow[0]/Aside[1]/Note[0]", note, 40, 300, 104, 114);

        queue(anchors, "Flow[0]/Section[0]/Row[1]/Bar[1]", List.of(dot(120, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(entry)).isEmpty();
        assertThat(drawingsIn(note)).isEmpty();
        assertThat(drawingsIn(heading)).hasSize(1);
    }

    @Test
    void aShapeNoNodeDrawsGoesToTheParagraphNearestItHoweverFar() {
        // A timeline's rail, 80pt from the nearer of two entries.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "First");
        XWPFParagraph second = text(document, "Second");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, "Flow[0]/First[0]", first, 40, 300, 100, 110);
        anchors.seat(0, second, "Flow[0]/Second[1]", second, 40, 300, 400, 410);

        anchors.queue(List.of(dot(30, 180)), -1, new DocxDrawingAnchors.Owner("timeline rail", "@timeline-rail"));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(second)).isEmpty();
        assertThat(offsetDown(drawingsIn(first).get(0))).isCloseTo(80, within(0.01));
    }

    @Test
    void aShapeWhoseHoldersWriteNoParagraphOnItsPageGoesToTheNearest() {
        // A badge beside a table holding the stack's text, which offers no paragraph: the heading
        // under it is nearer than the page's first paragraph.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph name = text(document, "Name");
        XWPFParagraph heading = text(document, "Heading");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, name, true);
        anchors.seat(0, name, "Flow[0]/Name[0]", name, 40, 300, 60, 70);
        anchors.seat(0, heading, "Flow[0]/Heading[3]", heading, 40, 300, 500, 510);

        queue(anchors, "Flow[0]/Bodies[4]/Body[0]/Badge[1]", List.of(dot(300, 530)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(name)).as("not the page's first paragraph").isEmpty();
        assertThat(offsetDown(drawingsIn(heading).get(0))).isCloseTo(30, within(0.01));
    }

    @Test
    void aParagraphThatCannotPlaceItIsPassedOverForTheNextNearest() {
        // The nearer paragraph has a border above its line, which hides where it starts.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph bordered = text(document, "Bordered");
        bordered.getCTP().addNewPPr().addNewPBdr().addNewTop()
                .setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
        XWPFParagraph plain = text(document, "Plain");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, bordered, true);
        anchors.seat(0, bordered, "Flow[0]/Bordered[0]", bordered, 40, 300, 100, 110);
        anchors.seat(0, plain, "Flow[0]/Plain[1]", plain, 40, 300, 200, 210);

        anchors.queue(List.of(dot(30, 104)), -1, null);
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(bordered)).isEmpty();
        assertThat(offsetDown(drawingsIn(plain).get(0))).isCloseTo(-96, within(0.01));
    }

    @Test
    void whereNoParagraphOnItsPageCanPlaceItItStaysWhereThePagePutsIt() {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph header = cellParagraph(document, 200, "Header");
        ((XWPFTableCell) header.getBody()).getTableRow().setRepeatHeader(true);
        XWPFParagraph body = text(document, "Body");
        body.getCTP().addNewPPr().addNewContextualSpacing();
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, body, true);
        anchors.seat(0, header, STACK + "/Header[0]", header, 110, 290, 100, 110);
        anchors.seat(0, body, STACK + "/Body[1]", body, 40, 300, 200, 210);

        queue(anchors, ICON, List.of(dot(120, 102)));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(header)).as("a repeated header row, which Word repeats with it").isEmpty();
        assertThat(drawingsIn(body)).singleElement().asString()
                .contains("<wp:positionV relativeFrom=\"page\"><wp:posOffset>" + emu(102) + "<");
    }

    @Test
    void aParagraphIsWithinItsNodeAndTheBlocksHoldingItOnly() {
        assertThat(seatOf("Flow[0]/Stack[1]/Text[0]").isWithin("Flow[0]/Stack[1]")).isTrue();
        assertThat(seatOf("Flow[0]/Stack[1]").isWithin("Flow[0]/Stack[1]")).as("its own node").isTrue();
        assertThat(seatOf("Flow[0]/Stack[10]/Text[0]").isWithin("Flow[0]/Stack[1]")).as("a sibling sharing a prefix")
                .isFalse();
        assertThat(seatOf("Flow[0]/Stack[1]x/Text[0]").isWithin("Flow[0]/Stack[1]")).isFalse();
        assertThat(seatOf("Flow[0]").isWithin("Flow[0]/Stack[1]")).as("a block holding it").isFalse();
        assertThat(seatOf(null).isWithin("Flow[0]")).as("text no node writes").isFalse();
    }

    private static DocxDrawingAnchors.Seat seatOf(String path) {
        return new DocxDrawingAnchors.Seat(null, 0, 0, 0, 0, path);
    }

    @Test
    void aComposedCellsShapeStillGoesOnlyToTheTextNearestItWithinReach() {
        // The same shape, 120pt below the holder's paragraph: a node's goes there, a composed
        // cell's — which has no node — stays where the page puts it.
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph first = text(document, "Heading");
        DocxDrawingAnchors anchors = anchors();
        anchors.paragraphOn(0, first, true);
        anchors.seat(0, first, STACK + "/Heading[0]", first, 40, 300, 200, 210);

        anchors.queue(List.of(dot(30, 320)), -1, new DocxDrawingAnchors.Owner("cell drawing", STACK, true));
        anchors.endSection(0, null, document::createParagraph);

        assertThat(drawingsIn(first)).singleElement().asString().contains("<wp:positionV relativeFrom=\"page\">");
    }

    private static DocxDrawingAnchors anchors() {
        AtomicLong ids = new AtomicLong(1);
        return new DocxDrawingAnchors(ids::getAndIncrement, DocxGrownLines.NONE::lineOf,
                DocxDrawingOwnerAnchorsTest::holders);
    }

    /**
     * A path and the paths above it, nearest first, as the layout names a node's holders: the
     * page's flow, where every path here starts, left out ({@link DocxLayoutMetrics#holdersOf}).
     */
    private static List<String> holders(String path) {
        List<String> holders = new java.util.ArrayList<>();
        for (String holder = path; holder != null && holder.contains("/"); holder = holder.substring(0,
                holder.lastIndexOf('/'))) {
            holders.add(holder);
        }
        return holders;
    }

    /** Queues shapes a node at a path draws. */
    private static void queue(DocxDrawingAnchors anchors, String path, List<DocxDrawings.Shape> shapes) {
        anchors.queue(shapes, -1, new DocxDrawingAnchors.Owner("ShapeNode", path));
    }

    /** A paragraph opening the only cell of a new table, its cell 200pt wide at no stated margin. */
    private static XWPFParagraph cellParagraph(XWPFDocument document, double cellWidth, String text) {
        XWPFTable table = document.createTable(1, 1);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        width(cell, cellWidth);
        margin(cell, 0);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.createRun().setText(text);
        return paragraph;
    }

    private static DocxDrawings.Shape onPage(int page, DocxDrawings.Shape shape) {
        return new DocxDrawings.Shape(shape.kind(), shape.x(), shape.top(), shape.width(), shape.height(), shape.fill(),
                shape.stroke(), shape.strokeWidth(), shape.radius(), shape.flipH(), page);
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

    private static void width(XWPFTableCell cell, double points) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcW().setType(STTblWidth.DXA);
        properties.getTcW().setW(BigInteger.valueOf(Math.round(points * 20)));
    }

    private static void margin(XWPFTableCell cell, double points) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcMar().addNewLeft().setW(BigInteger.valueOf(Math.round(points * 20)));
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
