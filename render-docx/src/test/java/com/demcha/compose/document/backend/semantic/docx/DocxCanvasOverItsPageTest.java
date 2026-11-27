package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.CanvasLayerBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A canvas that places its text elsewhere than one block under another from its corner is
 * written over its page: its paragraphs in text boxes where the page sets them, its drawings
 * where the page draws them, and nothing of it in the flow, which holds its room
 * ({@code DocxCanvasRoomTest}).
 *
 * <p>Written as its contents, such a canvas's text stood one block under another from its
 * corner: on a 960 by 540pt page that is all one canvas, Word 16 set every one of its 43 lines,
 * and LibreOffice 26.8 every one it set, as much as 820pt across and 222pt down from where the
 * page sets them.</p>
 */
class DocxCanvasOverItsPageTest {

    private static final double MARGIN = 40;
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);

    @Test
    void itsParagraphsAreSetInTextBoxesWhereThePageSetsThemNotInTheFlow() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addParagraph("Above")
                .add(sideBySide())
                .addParagraph("Below"))) {
            assertThat(document.getParagraphs()).extracting(DocxCanvasOverItsPageTest::ownText)
                    .as("nothing it writes stands in the flow").doesNotContain("West", "East")
                    .contains("Above", "Below");
            CTAnchor west = box(document, "West");
            CTAnchor east = box(document, "East");

            assertThat(west.getPositionH().getRelativeFrom().toString()).isEqualTo("page");
            assertThat(west.getPositionH().getPosOffset()).as("West where the canvas places it, from the page's edge")
                    .isEqualTo(Units.toEMU(MARGIN));
            assertThat(east.getPositionH().getPosOffset() - west.getPositionH().getPosOffset())
                    .as("East 150pt right of West, as the canvas places it").isEqualTo(Units.toEMU(150));
            assertThat(east.getPositionV().getRelativeFrom()).isEqualTo(west.getPositionV().getRelativeFrom());
            assertThat(anchorOf(document, east)).as("both measured from one paragraph")
                    .isSameAs(anchorOf(document, west));
            assertThat(east.getPositionV().getPosOffset() - west.getPositionV().getPosOffset())
                    .as("East 8pt below West, beside it, as the canvas places it").isEqualTo(Units.toEMU(8));
            assertThat(west.getBehindDoc()).as("in front of the text, where no fill under it covers it").isFalse();
        }
    }

    @Test
    void theCanvasAndEachParagraphAreNamed() throws Exception {
        DocxExportReport report = DocxExports.report(400, 600, MARGIN, page -> page
                .add(sideBySide())
                .addParagraph("Below"));

        assertThat(detailOf(report, "CanvasLayerNode")).isEqualTo("written over its page: its drawings stand "
                + "where the page draws them and its paragraphs in text boxes where the page sets them, nothing "
                + "of it in the flow but its room, where the flow holds it");
        List<String> paragraphs = report.bySubject().get("ParagraphNode").stream()
                .map(DocxExportReport.Note::detail).toList();
        assertThat(paragraphs).as("West and East").hasSize(2).allSatisfy(detail -> assertThat(detail)
                .startsWith("placed by its canvas where one block under another would not stand: set in a text box "
                            + "where the page sets it, anchored in the paragraph"));
    }

    @Test
    void aCanvasStackingItsTextFromItsCornerKeepsItInTheFlow() throws Exception {
        // One block under another from its corner is where the flow sets it: written as its
        // contents, it reads in Word as on the page.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addCanvas(300, 120, canvas -> canvas
                        .position(new ShapeBuilder().size(300, 120).fillColor(INK).build(), 0, 0)
                        .position(paragraph("Caption"), 0, 0)
                        .position(paragraph("Credit"), 0, 40))
                .addParagraph("Below"))) {
            assertThat(document.getParagraphs()).extracting(DocxCanvasOverItsPageTest::ownText)
                    .contains("Caption", "Credit", "Below");
            assertThat(document.getDocument().getBody().xmlText()).doesNotContain("<w:txbxContent>");
        }
    }

    @Test
    void aCanvasWhoseTextReadsDownKeepsItInTheFlow() throws Exception {
        // Placed apart, across and down, but one block under another in reading order — a CV's
        // name over its title — it stays in the flow, where a reader of the flow reaches it, each
        // block as far down as it places it (DocxCanvasRoomTest); how far across is named.
        Consumer<PageFlowBuilder> content = page -> page
                .addCanvas(300, 120, canvas -> canvas
                        .position(new ShapeBuilder().size(300, 120).fillColor(INK).build(), 0, 0)
                        .position(paragraph("Name"), 40, 20)
                        .position(paragraph("Title"), 60, 60))
                .addParagraph("Below");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, content)) {
            assertThat(document.getParagraphs()).extracting(DocxCanvasOverItsPageTest::ownText)
                    .contains("Name", "Title", "Below");
            assertThat(document.getDocument().getBody().xmlText()).doesNotContain("<w:txbxContent>");
        }
        assertThat(detailOf(DocxExports.report(400, 600, MARGIN, content), "CanvasLayerNode"))
                .contains("what it writes is written at its left edge, each block as far down as it places it but "
                          + "not as far across");
    }

    @Test
    void aLinkOrAPictureItPlacesApartKeepsItAsItsContents() throws Exception {
        // Each is something the flow carries and a text box does not, as for a block laid over
        // the flow (DocxOverTheFlowTest): the whole canvas is written as before, and named.
        DocumentNode link = new ParagraphBuilder().name("Site").text("Site")
                .linkTarget(DocumentLinkTarget.external("https://example.org")).build();
        DocumentNode picture = new com.demcha.compose.document.dsl.ImageBuilder().name("Logo")
                .source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png())).size(20, 20).build();
        for (DocumentNode apart : List.of(link, picture)) {
            Consumer<PageFlowBuilder> content = page -> page
                    .addCanvas(300, 120, canvas -> canvas
                            .position(paragraph("West"), 0, 10)
                            .position(apart, 150, 12))
                    .addParagraph("Below");
            try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, content)) {
                assertThat(document.getDocument().getBody().xmlText()).as("no text box beside %s", apart.name())
                        .doesNotContain("<w:txbxContent>");
            }
            assertThat(detailOf(DocxExports.report(400, 600, MARGIN, content), "CanvasLayerNode"))
                    .as("named as written from its corner beside %s", apart.name())
                    .contains("what it writes is written from its corner, one block after another, not where it "
                              + "places it");
        }
    }

    @Test
    void inATableCellItIsWrittenAsItsContents() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addRow(row -> row.addSection(cell -> cell.add(sideBySide())).addParagraph("Beside")))) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, its text in the cell")
                    .doesNotContain("<w:txbxContent>").contains(">West<").contains(">East<");
        }
    }

    @Test
    void asAShapeContainersLayerItIsWrittenAsItsContents() throws Exception {
        // Another overlay round it: its overlayDepth is past the canvas's own.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .add(new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Frame")
                        .rectangle(320, 140)
                        .position(sideBySide(), 0, 0, com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                        .build())
                .addParagraph("Below"))) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, its text written as before")
                    .doesNotContain("<w:txbxContent>").contains(">West<").contains(">East<");
        }
    }

    @Test
    void inAStackOfOneLayerItIsWrittenAsItsContents() throws Exception {
        // A stack of one layer is the flow, but an overlay round the canvas all the same: what it
        // writes is written as before, and the canvas named.
        Consumer<PageFlowBuilder> content = page -> page
                .addLayerStack(stack -> stack.layer(sideBySide(), com.demcha.compose.document.node.LayerAlign.TOP_LEFT))
                .addParagraph("Below");
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, content)) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, its text written as before")
                    .doesNotContain("<w:txbxContent>").contains(">West<").contains(">East<");
        }
        assertThat(detailOf(DocxExports.report(400, 600, MARGIN, content), "CanvasLayerNode"))
                .contains("what it writes is written from its corner");
    }

    @Test
    void inAPaintedPanelItIsWrittenAsItsContents() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(230, 230, 230)).add(sideBySide()))
                .addParagraph("Below"))) {
            assertThat(document.getDocument().getBody().xmlText()).as("no text box, its text in the panel")
                    .doesNotContain("<w:txbxContent>").contains(">West<").contains(">East<");
        }
    }

    @Test
    void openingAPageTheLayoutMovesItToItHoldsItsRoomInALineThere() throws Exception {
        // Owed as space above the next paragraph, the room was dropped at the page's top: under
        // a canvas with a 14pt margin moved to a new page, Word 16 and LibreOffice set it 139.8pt
        // high. A line's height they keep; its bottom margin is in it, or the line fits at the
        // foot of the page above where the canvas did not.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, page -> {
            filler(page, 14);
            page.add(sideBySide(new DocumentInsets(14, 0, 6, 0))).addParagraph("Below");
        })) {
            XWPFParagraph line = paragraphBefore(document, "Below");
            assertThat(ownText(line)).as("an empty line").isEmpty();
            assertThat(line.getCTP().getPPr().getSpacing().getLineRule().toString()).isEqualTo("exact");
            assertThat(DocxTwips.of(line.getCTP().getPPr().getSpacing().getLine()))
                    .as("its margin above on the new page, its box and its margin below").isEqualTo((14 + 120 + 6) * 20L);
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with nothing, as on the page").isFalse();
        }
    }

    @Test
    void afterAPageBreakItHoldsItsRoomInALineThere() throws Exception {
        // Word and LibreOffice drop the space above the paragraph after a page break too: 119.8pt
        // high under a 120pt canvas.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, page -> {
            filler(page, 3);
            page.addPageBreak(b -> b.name("Break")).add(sideBySide()).addParagraph("Below");
        })) {
            XWPFParagraph line = paragraphBefore(document, "Below");
            assertThat(ownText(line)).as("an empty line").isEmpty();
            assertThat(DocxTwips.of(line.getCTP().getPPr().getSpacing().getLine())).isEqualTo(120 * 20L);
        }
    }

    @Test
    void beforeAPageBreakItHoldsNoRoom() throws Exception {
        // Nothing follows it on its page to move: neither a line nor space is written for its room,
        // which owed there would stand below the paragraph before the break.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addParagraph("Above")
                .add(sideBySide())
                .addPageBreak(b -> b.name("Break"))
                .addParagraph("Next"))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                var spacing = paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSpacing()
                        ? paragraph.getCTP().getPPr().getSpacing() : null;
                if (spacing == null) {
                    continue;
                }
                assertThat(spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0L)
                        .as("no room owed below %s", ownText(paragraph)).isLessThan(100 * 20L);
                assertThat(spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0L)
                        .as("no room owed above %s", ownText(paragraph)).isLessThan(100 * 20L);
                assertThat(spacing.isSetLine() ? DocxTwips.of(spacing.getLine()) : 0L)
                        .as("no line holding it").isLessThan(100 * 20L);
            }
        }
    }

    @Test
    void aCanvasFillingItsPageIsCarriedOnItsPage() throws Exception {
        // A cover filling the first page, with a paragraph on the second: its room held as space
        // above that paragraph, nothing stood on the first page to carry its text boxes, and both
        // were dropped. The line holding its room stands there and carries them.
        DocxExportReport report = DocxExports.report(400, 400, MARGIN, page -> page
                .add(board(320, 320, "Cover")).addParagraph("After the cover"));
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, page -> page
                .add(board(320, 320, "Cover")).addParagraph("After the cover"))) {
            XWPFParagraph line = paragraphBefore(document, "After the cover");
            assertThat(DocxTwips.of(line.getCTP().getPPr().getSpacing().getLine())).as("the page's whole height")
                    .isEqualTo(320 * 20L);
            assertThat(boxesIn(line)).as("both text boxes carried by the line on the cover's page").isEqualTo(2);
        }
        assertThat(report.notes()).as("nothing dropped")
                .noneMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED);
    }

    @Test
    void eachOfTwoCanvasesFillingAPageIsCarriedOnItsOwn() throws Exception {
        // A deck: the first is followed, the second opens the next page; each holds a line there.
        DocxExportReport report = DocxExports.report(400, 400, MARGIN, page -> page
                .add(board(320, 320, "First")).add(board(320, 320, "Second")));
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, page -> page
                .add(board(320, 320, "First")).add(board(320, 320, "Second")))) {
            List<XWPFParagraph> lines = document.getParagraphs().stream()
                    .filter(paragraph -> boxesIn(paragraph) > 0).toList();
            assertThat(lines).as("one line a page").hasSize(2);
            assertThat(lines).allSatisfy(line -> assertThat(boxesIn(line)).isEqualTo(2));
        }
        assertThat(report.notes()).as("nothing dropped")
                .noneMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aPageBreakAfterACanvasAloneOnItsPageGoesInItsLine() throws Exception {
        // A deck of pages each a canvas, split by page breaks: a break's own paragraph under a line
        // filling its page stood on the next page and broke it again, a blank page between two
        // slides in Word 16 and LibreOffice 26.8 (four pages for three).
        DocxExportReport report = DocxExports.report(400, 400, MARGIN, CanvasDeck::withBreaks);
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, CanvasDeck::withBreaks)) {
            List<XWPFParagraph> breaks = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().xmlText().contains("w:type=\"page\"")).toList();
            assertThat(breaks).as("two breaks").hasSize(2);
            assertThat(breaks).as("each in the line holding its canvas's room, which carries its text boxes")
                    .allSatisfy(line -> {
                        assertThat(line.getCTP().getPPr().getSpacing().getLineRule().toString()).isEqualTo("exact");
                        assertThat(boxesIn(line)).isEqualTo(2);
                    });
            assertThat(document.getParagraphs()).as("the three lines, and nothing closing the section after them")
                    .hasSize(3);
            assertThat(document.getParagraphs()).as("one line a page, no paragraph of a break's own")
                    .filteredOn(paragraph -> ownText(paragraph).isEmpty() && boxesIn(paragraph) == 0
                                             && paragraph.getCTP().xmlText().contains("w:type=\"page\""))
                    .isEmpty();
        }
        assertThat(report.notes()).as("nothing dropped")
                .noneMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED);
    }

    /** Three canvases each filling a page, split by page breaks. */
    private static final class CanvasDeck {
        static void withBreaks(PageFlowBuilder page) {
            page.add(board(320, 320, "One")).addPageBreak(b -> b.name("ToTwo"))
                    .add(board(320, 320, "Two")).addPageBreak(b -> b.name("ToThree"))
                    .add(board(320, 320, "Three"));
        }
    }

    @Test
    void lastInTheFlowAfterATableItsDrawingsHangBelowTheClosingParagraphsLine() throws Exception {
        // Nothing in the flow holds it there: the closing paragraph under the table stands below
        // what it draws, as below a block laid over the flow, so typing at the end does not run
        // under its text boxes.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, MARGIN, page -> page
                .addTable(table -> table.columns(com.demcha.compose.document.table.DocumentTableColumn.auto())
                        .row("Cell"))
                .add(sideBySide()))) {
            // The canvas is 120pt tall and starts at the table's foot: the line is about that.
            assertThat(document.getParagraphs())
                    .as("a line as tall as it hangs below the table, before the closing one")
                    .filteredOn(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSpacing()
                                             && paragraph.getCTP().getPPr().getSpacing().isSetLine())
                    .anySatisfy(paragraph -> {
                        var spacing = paragraph.getCTP().getPPr().getSpacing();
                        assertThat(spacing.getLineRule().toString()).isEqualTo("exact");
                        assertThat(DocxTwips.of(spacing.getLine())).isBetween(100 * 20L, 140 * 20L);
                    });
        }
    }

    @Test
    void aSidebarLaidOverTheFlowBeforeItLeavesItAloneOnItsPage() throws Exception {
        // The sidebar writes no paragraph on the page: counted, the canvas's room was owed as
        // space above the paragraph opening the next page, nothing on its own page carried what
        // either draws, and Word 16 and LibreOffice 26.8 dropped all of it, one page short.
        assertTheLineCarriesThemAll(page -> page.add(sidebar()).add(board(320, 310, "Board"))
                .addParagraph("After"));
    }

    @Test
    void aSidebarLaidOverTheFlowAfterItLeavesItAloneOnItsPage() throws Exception {
        // Written after the canvas, the sidebar is laid over the flow all the same.
        assertTheLineCarriesThemAll(page -> page.add(board(320, 310, "Board")).add(sidebar())
                .addParagraph("After"));
    }

    @Test
    void aPageBreakAfterASidebarLaidOverTheFlowGoesInTheCanvassLine() throws Exception {
        // The sidebar writes no block between the line and the break: in a paragraph of its own
        // under the line filling the page, the break stood on the next page and broke it again,
        // three pages for two in Word 16 and LibreOffice 26.8.
        Consumer<PageFlowBuilder> content = page -> page.add(board(320, 310, "Board")).add(sidebar())
                .addPageBreak(b -> b.name("Break")).addParagraph("After");
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, content)) {
            XWPFParagraph line = paragraphBefore(document, "After");
            assertThat(line.getCTP().xmlText()).as("the break in the line").contains("w:type=\"page\"");
            assertThat(boxesIn(line)).as("which carries every text box on its page").isEqualTo(3);
            assertThat(document.getParagraphs()).as("the line and the paragraph after it, nothing else").hasSize(2);
        }
    }

    @Test
    void aTableSharingItsPageStillCounts() throws Exception {
        // A table's cell paragraph carries the text boxes, placed from the page's edge, and both
        // editors set them where the page does: the canvas holds no line of its own there.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, page -> page
                .addParagraph("Intro").addPageBreak(b -> b.name("ToTwo"))
                .addTable(table -> table.columns(com.demcha.compose.document.table.DocumentTableColumn.auto())
                        .row("Cell"))
                .add(board(320, 290, "Board")).addParagraph("After"))) {
            assertThat(document.getParagraphs()).as("no paragraph carries its text boxes")
                    .allSatisfy(paragraph -> assertThat(boxesIn(paragraph)).isZero());
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0))
                    .as("the table's cell does").satisfies(cell -> assertThat(boxesIn(cell)).isEqualTo(2));
        }
    }

    private static void assertTheLineCarriesThemAll(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, MARGIN, content)) {
            XWPFParagraph line = paragraphBefore(document, "After");
            assertThat(line.getCTP().getPPr().getSpacing().getLineRule().toString()).isEqualTo("exact");
            assertThat(DocxTwips.of(line.getCTP().getPPr().getSpacing().getLine())).as("the canvas's whole height")
                    .isEqualTo(310 * 20L);
            assertThat(boxesIn(line)).as("the canvas's two text boxes and the sidebar's, on their page")
                    .isEqualTo(3);
            assertThat(before(paragraphNamed(document, "After"))).as("no room owed above the next page's paragraph")
                    .isZero();
        }
        assertThat(DocxExports.report(400, 400, MARGIN, content).notes()).as("nothing dropped")
                .noneMatch(note -> note.severity() == DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aParagraphOfSeveralLinesIsGivenRoomForItsWidestLineAsWordSetsIt() throws Exception {
        // The layout places a paragraph set from its left as wide as its widest line: 192.77pt at
        // 12pt, 197.59pt at 12.3pt, which the file writes as 12.5pt and Word sets wider. In a box
        // two points wider than the page's widest line, Word 16 and LibreOffice 26.8 broke
        // TwinOutputExample's 12.3pt lead a word sooner.
        CTAnchor box = leadBox(leadIn(TextAlign.LEFT, 12.3, LEAD));
        assertThat(box.getPositionH().getPosOffset()).as("set from where the page starts it")
                .isEqualTo(Units.toEMU(MARGIN));
        assertThat(widthOf(box)).as("its widest line as Word sets it, and the editor's couple of points")
                .isCloseTo(197.5872 * 12.5 / 12.3 + 2, within(0.01));
        assertThat(widthOf(leadBox(leadIn(TextAlign.LEFT, 12, LEAD)))).as("at a size Word sets as the page does")
                .isCloseTo(192.768 + 2, within(0.01));
    }

    @Test
    void aParagraphOfSeveralLinesIsGivenNoRoomForANextWord() throws Exception {
        // Set in 142pt, its widest line is 140.85pt, and one line the page broke stands 143.19pt
        // with the space and the word after it: the box stops a point short of that, not two
        // points past the widest line. Two points more took "wrapped" onto a line the page broke
        // 1.2pt short of it (CanvasLayerExample's citation, in Word 16 and LibreOffice 26.8).
        assertThat(widthOf(leadBox(leadIn(TextAlign.LEFT, Double.NaN, 142)))).isCloseTo(143.192 - 1, within(0.01));
    }

    @Test
    void aLineEndedAtALineBreakSetsNoLimit() throws Exception {
        // "Short" ends at a line break, which Word writes as one: the next word fits beside it,
        // and taken as a line the page broke short of that word, it held the box to the widest
        // line with no room for an editor's face.
        assertThat(widthOf(leadBox(leadIn(TextAlign.LEFT, 12, LEAD, "Lead short\n" + LEAD_TEXT))))
                .as("as without the short line: the widest line and two points")
                .isCloseTo(192.768 + 2, within(0.01));
    }

    @Test
    void aCentredParagraphOfSeveralLinesKeepsItsWidthAndSpacesItsLetters() throws Exception {
        assertThat(widthOf(leadBox(leadIn(TextAlign.RIGHT, 12.3, LEAD)))).as("a right-aligned one too")
                .isEqualTo(LEAD);
        CTAnchor box = leadBox(leadIn(TextAlign.CENTER, 12.3, LEAD));
        assertThat(widthOf(box)).as("the measure the page broke it in, so no line takes another word")
                .isEqualTo(LEAD);
        assertThat(box.getPositionH().getPosOffset()).as("not moved to centre a wider box")
                .isEqualTo(Units.toEMU(MARGIN));
        assertThat(box.xmlText()).as("its letters drawn together to the page's width").contains("<w:spacing w:val=\"-");
        assertThat(leadBox(leadIn(TextAlign.CENTER, 12, LEAD)).xmlText()).as("not at a size Word sets as the page does")
                .doesNotContain("<w:spacing w:val=");
    }

    /** The width the lead's lines are broken in, in points. */
    private static final double LEAD = 200;

    /**
     * A side-by-side canvas 400pt wide holding a lead of several lines, broken in the given
     * width, at its corner, and a label right of it that starts above its foot.
     *
     * @param size its type size, or NaN for the default style's
     */
    private static XWPFDocument leadIn(TextAlign align, double size, double measure) throws Exception {
        return leadIn(align, size, measure, LEAD_TEXT);
    }

    /** The lead's text, which opens with "Lead " for {@link #leadBox} to find it. */
    private static final String LEAD_TEXT = "Lead one composition, one resolved layout graph, three backends: a "
                                            + "print-ready file, a slide of the same geometry and a document "
                                            + "written from it";

    private static XWPFDocument leadIn(TextAlign align, double size, double measure, String text) throws Exception {
        ParagraphBuilder builder = new ParagraphBuilder().name("Lead").align(align);
        if (!Double.isNaN(size)) {
            builder.textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder().size(size).build());
        }
        DocumentNode lead = builder.text(text).margin(new DocumentInsets(0, 400 - measure, 0, 0)).build();
        return DocxExports.withLayout(500, 400, MARGIN, page -> page.add(new CanvasLayerBuilder(400, 160).name("Board")
                .position(new ShapeBuilder().name("Block").size(400, 160).fillColor(INK).build(), 0, 0)
                .position(lead, 0, 10)
                .position(paragraph("Label"), 250, 14)
                .build()));
    }

    /** The text box holding the lead, read from a document closed once it is found. */
    private static CTAnchor leadBox(XWPFDocument document) throws Exception {
        try (document) {
            return anchors(document).stream()
                    .filter(anchor -> anchor.xmlText().contains("<w:txbxContent>") && anchor.xmlText().contains(">Lead "))
                    .findFirst().orElseThrow();
        }
    }

    private static double widthOf(CTAnchor anchor) {
        return Math.round(anchor.getExtent().getCx() / (double) Units.EMU_PER_POINT * 100) / 100.0;
    }

    private static long before(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()
                ? 0 : DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static XWPFParagraph paragraphNamed(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(paragraph -> ownText(paragraph).equals(text))
                .findFirst().orElseThrow();
    }

    /** A sidebar laid over the flow, holding a line of its own. */
    private static DocumentNode sidebar() {
        return DocxExports.laidOverTheFlow(paragraph("Side"));
    }

    /** How many text boxes a paragraph carries. */
    private static int boxesIn(XWPFParagraph paragraph) {
        return paragraph.getCTP().xmlText().split("<w:txbxContent>", -1).length - 1;
    }

    /** A canvas as large as given, its block drawn and two lines of text side by side. */
    private static DocumentNode board(double width, double height, String tag) {
        return new CanvasLayerBuilder(width, height).name("Board" + tag)
                .position(new ShapeBuilder().size(width, height).fillColor(INK).build(), 0, 0)
                .position(paragraph("West " + tag), 0, 10)
                .position(paragraph("East " + tag), 150, 18)
                .build();
    }

    private static void filler(PageFlowBuilder page, int lines) {
        for (int i = 0; i < lines; i++) {
            page.addParagraph("Filler line number " + i);
        }
    }

    /** The body paragraph just before the one whose own text is given. */
    private static XWPFParagraph paragraphBefore(XWPFDocument document, String text) {
        List<XWPFParagraph> all = document.getParagraphs();
        for (int i = 1; i < all.size(); i++) {
            if (ownText(all.get(i)).equals(text)) {
                return all.get(i - 1);
            }
        }
        throw new AssertionError("no paragraph reading " + text);
    }

    /** A canvas drawing a block, with West near its top-left corner and East right of it and lower. */
    private static DocumentNode sideBySide() {
        return sideBySide(DocumentInsets.zero());
    }

    private static DocumentNode sideBySide(DocumentInsets margin) {
        return new CanvasLayerBuilder(300, 120).name("Board").margin(margin)
                .position(new ShapeBuilder().name("Block").size(300, 120).fillColor(INK).build(), 0, 0)
                .position(paragraph("West"), 0, 10)
                .position(paragraph("East"), 150, 18)
                .build();
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().name(text).text(text).build();
    }

    /** The text box holding the given text. */
    private static CTAnchor box(XWPFDocument document, String text) {
        return anchors(document).stream()
                .filter(anchor -> anchor.xmlText().contains("<w:txbxContent>") && anchor.xmlText().contains(">" + text + "<"))
                .findFirst().orElseThrow();
    }

    /** The body paragraph a drawing is anchored in. */
    private static XWPFParagraph anchorOf(XWPFDocument document, CTAnchor anchor) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (var run : paragraph.getCTP().getRList()) {
                for (var drawing : run.getDrawingList()) {
                    if (drawing.getAnchorList().stream().anyMatch(held -> held.xmlText().equals(anchor.xmlText()))) {
                        return paragraph;
                    }
                }
            }
        }
        throw new AssertionError("no paragraph holds the drawing");
    }

    /** Every drawing anchored in the body's paragraphs. */
    private static List<CTAnchor> anchors(XWPFDocument document) {
        List<CTAnchor> anchors = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (var run : paragraph.getCTP().getRList()) {
                for (var drawing : run.getDrawingList()) {
                    anchors.addAll(drawing.getAnchorList());
                }
            }
        }
        return anchors;
    }

    /** The text of a paragraph's own runs, not of the text boxes anchored in it. */
    private static String ownText(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (var run : paragraph.getCTP().getRList()) {
            for (var part : run.getTList()) {
                text.append(part.getStringValue());
            }
        }
        return text.toString();
    }

    private static String detailOf(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
