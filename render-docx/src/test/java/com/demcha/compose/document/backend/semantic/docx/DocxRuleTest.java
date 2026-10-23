package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A horizontal line or a divider is Word's own rule: the bottom border of an empty paragraph.
 *
 * <p>Both were dropped with the rest of the drawing, so every rule a template draws under a
 * heading or between entries was missing in Word. As a paragraph border the rule flows with
 * the text and a reader moves or deletes it as a line of the document.</p>
 *
 * <p>Word draws a paragraph's border past both its indents, so each indent is taken in by as
 * much: the rule ends where the page ends it in Word ({@code wordsRuleReach}).</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRuleTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);
    /** What the default text's line under a rule is raised into the space above it. */
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;
    /** How far past its left and right indents Word draws a rule, in twips. */
    private static final long REACH_LEFT = Math.round(DocxSemanticBackend.WORD_RULE_REACH_LEFT_POINTS * 20);
    private static final long REACH_RIGHT = Math.round(DocxSemanticBackend.WORD_RULE_REACH_RIGHT_POINTS * 20);

    @Test
    void aFillLineIsAParagraphBorderAcrossTheWidth() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Experience"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 2)).fill()))) {
            XWPFParagraph rule = document.getParagraphs().get(1);
            CTBorder bottom = rule.getCTP().getPPr().getPBdr().getBottom();

            assertThat(rule.getText()).isEmpty();
            assertThat(bottom.getVal()).isEqualTo(STBorder.SINGLE);
            // w:sz counts eighths of a point.
            assertThat(bottom.getSz().intValue()).isEqualTo(16);
            assertThat(hex(bottom.getColor())).isEqualTo("1A5694");
            assertThat(DocxTwips.of(rule.getCTP().getPPr().getInd().getLeft()))
                    .as("from the left margin, Word's reach in").isEqualTo(REACH_LEFT);
            assertThat(DocxTwips.of(rule.getCTP().getPPr().getInd().getRight()))
                    .as("a fill line runs to the right margin, Word's reach in")
                    .isEqualTo(REACH_RIGHT);
        }
    }

    @Test
    void aLineShorterThanTheColumnEndsWhereItEnds() throws Exception {
        // Page 400 wide with 20pt margins: 360pt of text width, a 200pt line.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1))))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(160 * 20L + REACH_RIGHT);
        }
    }

    @Test
    void wordsReachIsAPointAndFourTenthsAtTheLeftAndAPointAndAHalfAtTheRight() {
        // Measured on nine rules: 1.36 to 1.49pt past the left indent, 1.44 to 1.59pt past the right.
        assertThat(REACH_LEFT).isEqualTo(28L);
        assertThat(REACH_RIGHT).isEqualTo(30L);
    }

    @Test
    void aRuleRunningPastItsWidthIsHeldAtItsEdgeAndTakenInOnlyByWhatWordDrawsFurther() throws Exception {
        // A 355pt rule set 6pt in, in 360pt: the page ends it a point past the right margin. Held
        // at the margin, Word's 1.51pt reach would end it half a point further than the page, so
        // only that half point is taken in.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(355).stroke(DocumentStroke.of(ACCENT, 1))
                        .margin(new DocumentInsets(0, 0, 0, 6))))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(6 * 20L + REACH_LEFT);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(REACH_RIGHT - 20);
        }
    }

    @Test
    void aRuleHungLessThanWordsReachLeftOfItsCellsTextIsTakenInByWhatIsLeft() throws Exception {
        // A point left of its row column's text: Word's 1.42pt reach less that point, 0.42pt in.
        try (XWPFDocument document = export(page -> page.addRow(row -> row.spacing(12).weights(1, 1)
                .addSection(s -> s.addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1))
                        .margin(new DocumentInsets(0, 0, 0, -1))))
                .addSection(s -> s.addParagraph("Beside"))))) {
            XWPFParagraph rule = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null
                                         && paragraph.getCTP().getPPr().isSetPBdr())
                    .findFirst().orElseThrow();

            assertThat(DocxTwips.of(rule.getCTP().getPPr().getInd().getLeft())).isEqualTo(REACH_LEFT - 20);
        }
    }

    @Test
    void aRuleInACellWhoseWidthIsWordsEndsWordsReachShortOfTheCellsText() throws Exception {
        // No layout, an auto column: the table's columns are Word's, and the rule runs to the cell's
        // text edge, which it is held in from by Word's reach past it.
        try (XWPFDocument document = DocxExports.withoutLayout(400, 400, 20, page -> page.addTable(t -> t
                .autoColumns(1)
                .rowCells(DocumentTableCell.node(new LineBuilder().horizontal(100)
                        .stroke(DocumentStroke.of(ACCENT, 1)).fill().build()))))) {
            assertThat(DocxTwips.of(ruleIn(document).getCTP().getPPr().getInd().getRight())).isEqualTo(REACH_RIGHT);
        }
    }

    @Test
    void aRuleInAPaddedContainerOfACellWhoseWidthIsWordsKeepsThePaddingAndTheReach() throws Exception {
        try (XWPFDocument document = DocxExports.withoutLayout(400, 400, 20, page -> page.addTable(t -> t
                .autoColumns(1)
                .rowCells(DocumentTableCell.node(new SectionBuilder().padding(DocumentInsets.of(8))
                        .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())
                        .build()))))) {
            assertThat(DocxTwips.of(ruleIn(document).getCTP().getPPr().getInd().getRight()))
                    .as("the section's padding and Word's reach").isEqualTo(8 * 20L + REACH_RIGHT);
        }
    }

    /** The rule's paragraph in the first cell of the first table, at any depth. */
    private static XWPFParagraph ruleIn(XWPFDocument document) {
        java.util.ArrayDeque<org.apache.poi.xwpf.usermodel.XWPFTableCell> cells = new java.util.ArrayDeque<>();
        cells.add(document.getTables().get(0).getRow(0).getCell(0));
        while (!cells.isEmpty()) {
            org.apache.poi.xwpf.usermodel.XWPFTableCell cell = cells.poll();
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                if (paragraph.getCTP().getPPr() != null && paragraph.getCTP().getPPr().isSetPBdr()) {
                    return paragraph;
                }
            }
            cell.getTables().forEach(table -> table.getRows().forEach(row -> cells.addAll(row.getTableCells())));
        }
        throw new AssertionError("no rule");
    }

    @Test
    void aRuleShorterThanWordsReachAtBothEndsKeepsATenthOfAPointOfItsParagraph() throws Exception {
        // A 2pt rule: each indent taken in by 0.95pt, the paragraph 0.1pt wide, which Word draws
        // its reach past at both ends.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(2).stroke(DocumentStroke.of(ACCENT, 1))))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(19L);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(358 * 20L + 19);
        }
    }

    @Test
    void aDividerIsARuleOfItsThicknessAndColour() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Above"))
                .addDivider(d -> d.width(200).thickness(3).color(ACCENT).margin(DocumentInsets.symmetric(6, 0))))) {
            CTPPr properties = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(properties.getPBdr().getBottom().getSz().intValue()).isEqualTo(24);
            assertThat(hex(properties.getPBdr().getBottom().getColor())).isEqualTo("1A5694");
            assertThat(DocxTwips.of(properties.getSpacing().getBefore())).isEqualTo(6 * 20L);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(160 * 20L + REACH_RIGHT);
        }
    }

    @Test
    void theSpaceAroundTheStrokeInItsBoxIsKept() throws Exception {
        // A 1pt stroke centred in a 12pt box: 5.5pt above it, 5.5pt below.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.size(200, 12).from(0, 6).to(200, 6).stroke(DocumentStroke.of(ACCENT, 1)))
                .addParagraph(p -> p.text("Below")))) {
            CTPPr rule = document.getParagraphs().get(0).getCTP().getPPr();
            CTPPr below = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(DocxTwips.of(rule.getSpacing().getLine())).isEqualTo(110L);
            assertThat(DocxTwips.of(below.getSpacing().getBefore())).isEqualTo(110L - RAISE);
        }
    }

    @Test
    void aDashedLineIsDashedAndTheReportSaysTheLengthsAreWords() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1)).dashed(6, 3))
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1)).dashed(1, 2)));
        try (document) {
            assertThat(bottomOf(document, 0).getVal()).isEqualTo(STBorder.DASHED);
            assertThat(bottomOf(document, 1).getVal()).as("dashes no longer than the stroke").isEqualTo(STBorder.DOTTED);
            assertThat(report.get().bySubject().get("dash pattern")).hasSize(2);
        }
    }

    @Test
    void aStrokeThickerThanWordsThickestBorderIsDrawnAtItsThickness() throws Exception {
        // Word's thickest border is 12pt: written as one, a 20pt bar came out 12pt, and what
        // followed stood 8pt high, over it.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 12)))
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 20))))) {
            assertThat(bottomOf(document, 0).getSz().intValue()).as("the thickest border").isEqualTo(96);
            assertThat(document.getParagraphs()).filteredOn(p -> p.getCTP().getPPr() != null
                    && p.getCTP().getPPr().isSetPBdr()).as("the 20pt stroke, no border").hasSize(1);
            assertThat(document.getDocument().xmlText()).contains("<a:ln w=\""
                    + org.apache.poi.util.Units.toEMU(20) + "\"");
        }
    }

    @Test
    void aStrokeThickerThanABorderHoldsItsRoomInAStackOfOneLayerAsInTheFlow() throws Exception {
        // A line among the text of a stack of one layer is in the flow, as a rule there would be.
        long inTheFlow = spaceAboveBelow(page -> page.addParagraph("Above")
                .addLine(l -> l.horizontal(200).thickness(20).color(ACCENT)).addParagraph("Below"));
        long inAStack = spaceAboveBelow(page -> page.addLayerStack(stack -> stack.layer(
                new com.demcha.compose.document.dsl.SectionBuilder().addParagraph("Above")
                        .addLine(l -> l.horizontal(200).thickness(20).color(ACCENT)).addParagraph("Below").build())));

        assertThat(inTheFlow).as("premise: the room is held above the paragraph after it").isGreaterThan(19 * 20);
        assertThat(inAStack).isEqualTo(inTheFlow);

        // Last in its layer, it holds its room above what follows the stack, as a rule there does.
        long lastInAStack = spaceAboveBelow(page -> page.addLayerStack(stack -> stack.layer(
                new com.demcha.compose.document.dsl.SectionBuilder().addParagraph("Above")
                        .addLine(l -> l.horizontal(200).thickness(20).color(ACCENT)).build()))
                .addParagraph("Below"));
        assertThat(lastInAStack).isEqualTo(inTheFlow);
    }

    private static long spaceAboveBelow(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = export(content)) {
            XWPFParagraph below = document.getParagraphs().stream().filter(p -> p.getText().equals("Below"))
                    .findFirst().orElseThrow();
            CTPPr properties = below.getCTP().getPPr();
            return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore() ? 0
                    : DocxTwips.of(properties.getSpacing().getBefore());
        }
    }

    @Test
    void aRuleInsideACardIsInTheCardsCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(238, 243, 249))
                .addParagraph(p -> p.text("Inside"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())))) {
            List<XWPFParagraph> inCell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs();

            assertThat(inCell.get(1).getCTP().getPPr().getPBdr().getBottom().getVal()).isEqualTo(STBorder.SINGLE);
        }
    }

    @Test
    void anAnchorOnARuleIsABookmarkOnItsParagraph() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).anchor("rule")))) {
            assertThat(document.getParagraphs().get(0).getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("rule");
        }
    }

    @Test
    void whatIsNotARuleIsDrawnAsAShapeWhereThePagePutsIt() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addLine(l -> l.name("Rail").vertical(40).stroke(DocumentStroke.of(ACCENT, 1)))
                .addShape(s -> s.name("Block").size(100, 30).fillColor(ACCENT))
                .addShape(s -> s.name("Pill").size(100, 4).fillColor(ACCENT).cornerRadius(DocumentCornerRadius.of(2))));
        try (document) {
            assertThat(document.getParagraphs()).as("no rule among them")
                    .noneMatch(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetPBdr());
            assertThat(anchoredShapes(document))
                    .as("a vertical line, a box taller than a border, a rounded bar")
                    .containsExactly("line", "rect", "roundRect");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
            assertThat(report.get().count(DocxExportReport.Severity.APPROXIMATED)).isEqualTo(3);
        }
    }

    /** The preset geometry of every shape anchored in the body, in document order. */
    private static java.util.List<String> anchoredShapes(XWPFDocument document) {
        java.util.List<String> shapes = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("prstGeom prst=\"(\\w+)\"")
                .matcher(document.getDocument().xmlText());
        while (matcher.find()) {
            shapes.add(matcher.group(1));
        }
        return shapes;
    }

    @Test
    void aRuleInALayerStackOfOneLayerIsARuleInTheFlow() throws Exception {
        // One layer lays nothing over anything: its rule takes its room in the flow, where a
        // shape anchored to the page takes none and everything below moves up by it.
        try (XWPFDocument document = export(page -> page.addLayerStack(stack -> stack.layer(
                new com.demcha.compose.document.dsl.SectionBuilder().name("Entries")
                        .addParagraph(p -> p.text("First"))
                        .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())
                        .addParagraph(p -> p.text("Second"))
                        .build())))) {
            assertThat(document.getParagraphs()).as("the rule, a paragraph's bottom border")
                    .anyMatch(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetPBdr());
            assertThat(anchoredShapes(document)).as("not a shape as well").isEmpty();
        }
    }

    @Test
    void anAccentBarInALayerStackOfOneLayerStaysDrawing() throws Exception {
        // A template nests a row in a one-layer stack; a small filled bar in it is an accent
        // beside a label, not a divider across the column.
        try (XWPFDocument document = export(page -> page.addLayerStack(stack -> stack.layer(
                new com.demcha.compose.document.dsl.SectionBuilder().name("Meta")
                        .addShape(s -> s.name("Accent").size(4, 10).fillColor(ACCENT))
                        .addParagraph(p -> p.text("Issue date"))
                        .build())))) {
            assertThat(document.getParagraphs())
                    .noneMatch(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetPBdr());
            assertThat(anchoredShapes(document)).containsExactly("rect");
        }
    }

    @Test
    void aLineAloneInALayerStackOfOneLayerIsDrawingAndHasNoBookmarkToPointAt() throws Exception {
        // Only drawing, the stack is drawn; a page reference to the line's anchor has no
        // bookmark to count, and is written as text rather than as a field pointing nowhere.
        try (XWPFDocument document = export(page -> page
                .addLayerStack(stack -> stack.layer(new com.demcha.compose.document.dsl.LineBuilder()
                        .name("Alone").horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).anchor("alone").build()))
                .addPageReference("alone"))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("PAGEREF");
        }
    }

    @Test
    void linesLaidOverEachOtherInALayerStackAreNotRulesInTheFlow() throws Exception {
        // A skill meter: a track and the fill laid over it. As rules they came out as two bars,
        // one under the other.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page.addLayerStack(stack -> {
            stack.layer(new com.demcha.compose.document.dsl.LineBuilder().name("Track")
                    .horizontal(120).stroke(DocumentStroke.of(DocumentColor.rgb(200, 200, 200), 3)).anchor("track").build());
            stack.layer(new com.demcha.compose.document.dsl.LineBuilder().name("Filled")
                    .horizontal(80).stroke(DocumentStroke.of(ACCENT, 3)).build());
        }).addPageReference("track"));
        try (document) {
            assertThat(document.getParagraphs()).noneMatch(p -> p.getCTP().getPPr() != null
                                                               && p.getCTP().getPPr().isSetPBdr());
            assertThat(anchoredShapes(document)).as("the track and its fill, as lines laid over each other")
                    .containsExactly("line", "line");
            assertThat(document.getParagraphs())
                    .as("the drawn line carries no bookmark, so no field may point at one")
                    .noneMatch(p -> p.getCTP().xmlText().contains("PAGEREF"));
        }
    }

    @Test
    void aTranslucentRuleIsFlattenedAgainstWhatIsUnderIt() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 128), 1))))) {
            // Half-opaque black over white.
            assertThat(hex(bottomOf(document, 0).getColor())).isEqualTo("7F7F7F");
        }
    }

    @Test
    void anInvisibleRuleKeepsItsPlaceAndDrawsNothing() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 1)))
                .addParagraph(p -> p.text("After")))) {
            CTPPr rule = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(rule.isSetPBdr()).isFalse();
            assertThat(rule.getSpacing().isSetLine()).isTrue();
        }
    }

    @Test
    void aStrokeThickerThanItsBoxTakesItsExtraFromTheSpaceBelow() throws Exception {
        // horizontal() sizes the box from the stroke set before it: a 1pt box, a 3pt stroke. On
        // the page the stroke spills out of the box; in Word it takes room, so the room comes
        // off the 10pt margin below.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 3)).margin(DocumentInsets.bottom(10)))
                .addParagraph(p -> p.text("After")))) {
            CTPPr after = document.getParagraphs().get(1).getCTP().getPPr();

            // Box 1pt; Word takes 0.1 + 3: 2.1pt comes off the 10pt.
            assertThat(DocxTwips.of(after.getSpacing().getBefore())).isEqualTo(158L - RAISE);
        }
    }

    @Test
    void whatARulesParagraphStandsPastItsBoxComesOutOfTheSpaceUnderIt() throws Exception {
        // A stroke as tall as its box, nothing owed below it: Word's paragraph is a tenth of a
        // point of line and the border in eighths of a point, counted in whole twips.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.stroke(DocumentStroke.of(ACCENT, 1)).horizontal(100))
                .addParagraph(p -> p.text("Below").margin(DocumentInsets.top(5.5)))
                .addLine(l -> l.stroke(DocumentStroke.of(ACCENT, 1.2)).horizontal(100))
                .addParagraph(p -> p.text("Below").margin(DocumentInsets.top(5.5))))) {
            CTPPr afterTheWholeEighths = document.getParagraphs().get(1).getCTP().getPPr();
            CTPPr afterTheRoundedEighths = document.getParagraphs().get(3).getCTP().getPPr();

            assertThat(bottomOf(document, 2).getSz().intValue()).as("1.2pt drawn 1.25pt wide").isEqualTo(10);
            // 1pt: the line's tenth. 1.2pt: the tenth and the 0.05pt Word draws it wider.
            assertThat(DocxTwips.of(afterTheWholeEighths.getSpacing().getBefore())).isEqualTo(110L - 2 - RAISE);
            assertThat(DocxTwips.of(afterTheRoundedEighths.getSpacing().getBefore())).isEqualTo(110L - 3 - RAISE);
        }
    }

    @Test
    void whatARulesParagraphStandsPastItsBoxComesOutOfTheSpaceOwedBelowItFirst() throws Exception {
        // The rule's own margin below takes it, and the paragraph under it keeps its space whole.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.stroke(DocumentStroke.of(ACCENT, 1.2)).horizontal(100).margin(DocumentInsets.bottom(4)))
                .addParagraph(p -> p.text("Below").margin(DocumentInsets.top(5.5))))) {
            CTPPr below = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(DocxTwips.of(below.getSpacing().getBefore())).isEqualTo(80L - 3 + 110 - RAISE);
        }
    }

    @Test
    void whatARulesParagraphStandsPastItsBoxIsNamedWhereNoSpaceUnderItTakesIt() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = withReport(report, page -> page
                .addLine(l -> l.stroke(DocumentStroke.of(ACCENT, 1.2)).horizontal(100))
                .addParagraph(p -> p.name("Below").text("Below")))) {
            assertThat(report.get().bySubject().get("space above")).singleElement().satisfies(note -> {
                assertThat(note.path()).contains("Below");
                assertThat(note.detail()).isEqualTo("it stands 0.15pt lower than the page sets it, and what follows "
                                                    + "with it: the space the page leaves above it does not hold the "
                                                    + "line hanging below the block above it (0.15pt)");
            });
        }
    }

    @Test
    void anInvisibleRulesParagraphHangsNothingItDoesNotDraw() throws Exception {
        // No border is written: Word's paragraph is its tenth of a point of line alone, inside the
        // 1pt box, and the paragraph under it keeps its space whole.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 1)).horizontal(100))
                .addParagraph(p -> p.text("Below").margin(DocumentInsets.top(5.5))))) {
            CTPPr below = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(DocxTwips.of(below.getSpacing().getBefore())).isEqualTo(110L - RAISE);
        }
    }

    @Test
    void aRuleInAPaddedSectionStartsAndEndsInsideIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Plain", s -> s
                .padding(DocumentInsets.of(20))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(20 * 20L + REACH_LEFT);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(20 * 20L + REACH_RIGHT);
        }
    }

    @Test
    void aSolidPaintedBarIsARuleToo() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addShape(s -> s.size(100, 2).fill(com.demcha.compose.document.style.DocumentPaint.solid(ACCENT))))) {
            assertThat(hex(bottomOf(document, 0).getColor())).isEqualTo("1A5694");
        }
    }

    @Test
    void aLinkOnARuleIsReportedAsNotCarried() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addParagraph(p -> p.text("Top").anchor("top"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).linkTo("top")));
        try (document) {
            assertThat(report.get().bySubject()).containsKey("rule link");
        }
    }

    @Test
    void theStrokeIsPlacedFromTheTopOfTheBoxAsThePageMeasuresItFromTheBottom() {
        var node = new com.demcha.compose.document.dsl.LineBuilder()
                .size(100, 10).from(0, 8).to(100, 8)
                .stroke(DocumentStroke.of(ACCENT, 2)).build();

        assertThat(DocxRules.of(node).centreFromTop()).isEqualTo(2.0);
    }

    private static CTBorder bottomOf(XWPFDocument document, int paragraph) {
        return document.getParagraphs().get(paragraph).getCTP().getPPr().getPBdr().getBottom();
    }

    private static String hex(Object value) {
        if (value instanceof byte[] bytes) {
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02X", b & 0xFF));
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, content);
    }

    private static XWPFDocument withReport(AtomicReference<DocxExportReport> report,
                                           Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new XWPFDocument(new java.io.ByteArrayInputStream(docx));
        }
    }
}
