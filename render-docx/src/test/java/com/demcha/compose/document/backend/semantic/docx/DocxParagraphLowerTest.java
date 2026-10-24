package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's line Word sets high, under half a point, is lowered by the space between it and
 * what follows: its space above grows by what that space gives, and the block under it stands
 * where it did.
 *
 * <p>Word and LibreOffice set an exact line's baseline four fifths of the way down it, the page
 * its face's ascent below the line's top: Ubuntu's ascent is 0.83 of its line, and at 10pt Word
 * sets it about a third of a point high. Under half a point its position moves nothing, and a raise
 * only moves a line up. What nothing under the line gives back, it is not lowered by, so nothing
 * under it ever stands lower. Ubuntu's own line, its Windows ascent and descent, is taller than
 * the page's, so its lines are written exact.</p>
 */
class DocxParagraphLowerTest {

    /** Ubuntu's ascent, in ems: 932 of 1000 units. */
    private static final double UBUNTU_ASCENT = 0.932;
    /** A line Word sets high, by under half a point. */
    private static final DocumentTextStyle HIGH = DocumentTextStyle.builder().fontName(FontName.UBUNTU).size(10)
            .build();
    /** A paragraph that wraps onto two lines of the 360pt the page gives it. */
    private static final String WRAPPED = "A line long enough to wrap onto a second one in a column three hundred and "
                                          + "sixty points wide, set in Gothic A1 at ten points";
    /** A line Word seats where the page does, to a few hundredths: neither lowered nor raised. */
    private static final DocumentTextStyle EVEN = DocumentTextStyle.builder().fontName(FontName.GOTHIC_A1).size(10).build();

    @Test
    void aLineSetHighIsLoweredByTheSpaceOwedAboveTheParagraphUnderIt() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));
        long lower = lowering(paragraph(document, "High"));

        assertThat(before(paragraph(document, "High"))).as("lowered").isEqualTo(lower);
        assertThat(before(paragraph(document, "Next"))).as("the space under it, less the lowering").isEqualTo(200 - lower);
    }

    @Test
    void aLineSetHighIsLoweredByTheTopEdgeOfTheParagraphUnderIt() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        long lower = lowering(paragraph(document, "High"));

        assertThat(before(paragraph(document, "High"))).as("lowered").isEqualTo(lower);
        assertThat(before(paragraph(document, "Next"))).as("its own top edge, less the lowering").isEqualTo(200 - lower);
    }

    @Test
    void aLineSetHighIsLoweredByAsMuchAsTheSpaceUnderItGives() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(0.1))));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: lower than the space under it").isGreaterThan(2);
        assertThat(before(paragraph(document, "High"))).as("the tenth of a point under it").isEqualTo(2);
        assertThat(before(paragraph(document, "Next"))).isZero();
    }

    @Test
    void twoLinesSetHighOneUnderTheOtherEachTakeTheirOwnFromTheSpaceUnderThem() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("First").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addParagraph(p -> p.text("Second").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));
        long first = lowering(paragraph(document, "First"));
        long second = lowering(paragraph(document, "Second"));

        assertThat(before(paragraph(document, "First"))).isEqualTo(first);
        assertThat(before(paragraph(document, "Second"))).as("the gap, less the first's lowering, and its own")
                .isEqualTo(200 - first + second);
        assertThat(before(paragraph(document, "Next"))).isEqualTo(200 - second);
    }

    @Test
    void aLineSetHighAboveATableIsLoweredByTheSpaceAboveTheTable() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addTable(table -> table.autoColumns(1).row("Cell")));
        XWPFParagraph high = paragraph(document, "High");
        long lower = lowering(high);

        assertThat(before(high)).as("lowered").isEqualTo(lower);
        assertThat(after(high)).as("the space above the table, written below it, less the lowering").isEqualTo(200 - lower);
    }

    @Test
    void aCellsLastLineSetHighIsLoweredByTheSpaceTheCellOwesUnderIt() throws Exception {
        XWPFDocument document = export(page -> page
                .addRow(row -> row
                        .addSection("Left", left -> left.padding(DocumentInsets.bottom(10))
                                .addParagraph(p -> p.text("High").textStyle(HIGH)))
                        .addSection("Right", right -> right.addParagraph(p -> p.text("Beside").textStyle(EVEN))))
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));
        XWPFParagraph high = inACell(document, "High");
        long lower = lowering(high);

        assertThat(before(high)).as("lowered").isEqualTo(lower);
        assertThat(after(high)).as("the cell's padding, less the lowering").isEqualTo(200 - lower);
    }

    @Test
    void aCellsLastLineSetHighIsTakenBackWhereTheSectionsEndLeavesTheCellsSpaceOut() throws Exception {
        // A row ending the document: its cell's space below is left out, and the lowering with it.
        XWPFDocument document = export(page -> page
                .addRow(row -> row
                        .addSection("Left", left -> left.padding(DocumentInsets.bottom(10))
                                .addParagraph(p -> p.text("High").textStyle(HIGH)))
                        .addSection("Right", right -> right.addParagraph(p -> p.text("Beside").textStyle(EVEN)))));
        XWPFParagraph high = inACell(document, "High");
        XWPFDocument followed = export(page -> page
                .addRow(row -> row
                        .addSection("Left", left -> left.padding(DocumentInsets.bottom(10))
                                .addParagraph(p -> p.text("High").textStyle(HIGH)))
                        .addSection("Right", right -> right.addParagraph(p -> p.text("Beside").textStyle(EVEN))))
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));

        assertThat(before(inACell(followed, "High"))).as("the premise: lowered where the row does not end the section")
                .isEqualTo(lowering(inACell(followed, "High")));
        assertThat(after(high)).as("the premise: the cell's space below left out").isZero();
        assertThat(before(high)).as("taken back").isZero();
    }

    @Test
    void theParagraphUnderTheLineTakesTheGapBetweenItsOwnLinesFirst() throws Exception {
        // The gap between the lines of the paragraph under it comes out of the space above that
        // paragraph: all of it here, and the line takes what is left, nothing.
        XWPFDocument lowered = export(page -> page.spacing(2)
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addParagraph(p -> p.text(WRAPPED).textStyle(EVEN).lineSpacing(2)));
        XWPFDocument control = export(page -> page.spacing(2)
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN))
                .addParagraph(p -> p.text(WRAPPED).textStyle(EVEN).lineSpacing(2)));
        XWPFParagraph lines = starting(lowered, "A line long");
        XWPFParagraph controlLines = starting(control, "A line long");

        assertThat(before(controlLines)).as("the premise: the gap takes all of its space above").isZero();
        assertThat(before(paragraph(lowered, "High"))).as("the page's 2pt above it, not lowered")
                .isEqualTo(before(paragraph(control, "High")));
        assertThat(spacing(lines).getLine()).as("its lines as far apart as with nothing set high above")
                .isEqualTo(spacing(controlLines).getLine());
    }

    @Test
    void theParagraphUnderTheLineTakesItsOwnRaiseFirst() throws Exception {
        // The paragraph under it, which Word sets low, rises into half a point above it first.
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addParagraph(p -> p.text("Low").margin(DocumentInsets.top(0.5)));
        XWPFDocument document = export(content);
        long lower = lowering(paragraph(document, "High"));
        long raise = 10 - before(paragraph(export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN))
                .addParagraph(p -> p.text("Low").margin(DocumentInsets.top(0.5)))), "Low"));

        assertThat(raise).as("the premise: the paragraph under it rises, leaving less than the lowering")
                .isPositive().isGreaterThan(10 - lower);
        assertThat(before(paragraph(document, "Low"))).as("its raise, whole, and the rest to the line").isZero();
        assertThat(before(paragraph(document, "High"))).as("what the raise leaves").isEqualTo(10 - raise);
    }

    @Test
    void aRowsLiftOutOfTheSpaceAboveItsTableComesBeforeTheLine() throws Exception {
        // The row's first lines, which Word sets low, lift it out of the half point above its
        // table first; the line above takes what is left.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(0.5)))
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right"))));
        XWPFDocument control = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN).margin(DocumentInsets.bottom(0.5)))
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right"))));
        XWPFParagraph high = paragraph(document, "High");
        long lift = 10 - after(paragraph(control, "High"));
        long given = Math.min(lowering(high), 10 - lift);

        assertThat(lift).as("the premise: the row is lifted out of the space above its table").isPositive();
        assertThat(before(high)).as("what the lift leaves").isEqualTo(given);
        assertThat(after(high)).isEqualTo(10 - lift - given);
    }

    @Test
    void aRowsLiftComesBeforeTheLineWithABlockUnderTheRowToo() throws Exception {
        // The rows are lifted at the section's end, after the block under the row is written.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(0.5)))
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right")))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFDocument control = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN).margin(DocumentInsets.bottom(0.5)))
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right")))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFParagraph high = paragraph(document, "High");
        long lift = 10 - after(paragraph(control, "High"));
        long given = Math.min(lowering(high), 10 - lift);

        assertThat(lift).as("the premise: the row is lifted out of the space above its table").isPositive();
        assertThat(before(high)).as("what the lift leaves").isEqualTo(given);
        assertThat(after(high)).isEqualTo(10 - lift - given);
        assertThat(before(paragraph(document, "Next"))).isEqualTo(before(paragraph(control, "Next")));
    }

    @Test
    void anItemUnderTheLineTakesNothingOfItsOwnLoweringToGiveIt() throws Exception {
        // The Word list's item under it, set high by half a point and more, is lowered by its own
        // space above, and taken back at the document's end, which gives none of it back: the line
        // above takes only what the tenth of a point above the item held before that.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addList(list -> list.textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).size(24).build())
                        .margin(DocumentInsets.top(0.1)).items("One")));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: lower than the space under it").isGreaterThan(2);
        assertThat(before(paragraph(document, "High"))).as("the tenth of a point above the list").isEqualTo(2);
        assertThat(before(paragraph(document, "One"))).as("its own lowering taken back, nothing more").isZero();
    }

    @Test
    void aLineSetHighAboveAnOverlaysTableIsNotLowered() throws Exception {
        // A stack laying a table over a backdrop measures it from the page.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addLayerStack(stack -> stack.margin(DocumentInsets.top(10))
                        .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(200, 40).build())
                        .layer(new com.demcha.compose.document.dsl.TableBuilder().autoColumns(1).row("Cell").build(),
                                LayerAlign.TOP_LEFT)));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "High"))).isZero();
    }

    @Test
    void aLineSetHighWithNothingUnderItIsNotLowered() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10))));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "High"))).isZero();
    }

    @Test
    void aLineSetHighAboveTheNextPagesFirstBlockIsNotLowered() throws Exception {
        // Word sets the next page's first block at its top, whatever the space above it: nothing
        // there gives the line back what it would take, and its page would run that much longer.
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 500)
                .addParagraph(p -> p.name("High").text("High").textStyle(HIGH))
                .addParagraph(p -> p.name("Next").text("Next").textStyle(EVEN).margin(DocumentInsets.top(50)));
        try (DocumentSession session = session(content)) {
            assertThat(startPage(session, "High")).as("the premise: on the first page").isZero();
            assertThat(startPage(session, "Next")).as("the premise: opening the second").isEqualTo(1);
        }
        XWPFDocument document = export(content);

        assertThat(before(paragraph(document, "High"))).as("the spacer above it past its hairline, and nothing more")
                .isEqualTo(Math.round((500 - 0.1) * 20));
    }

    @Test
    void aLineSetHighAtThePagesFootIsNotLoweredIntoTheSpaceLeftThere() throws Exception {
        // A page break under it: nothing under it on its page, and the space below it stays at
        // the page's foot.
        XWPFDocument broken = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));

        assertThat(lowering(paragraph(broken, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(broken, "High"))).isZero();
        // A table the layout moves to the next page leaves the page above the same way.
        Consumer<PageFlowBuilder> moved = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 520)
                .addParagraph(p -> p.name("High").text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addTable(table -> table.name("Moved").autoColumns(1).row("One").row("Two"));
        try (DocumentSession session = session(moved)) {
            assertThat(startPage(session, "Moved")).as("the premise: the table opens the next page").isEqualTo(1);
        }
        assertThat(before(paragraph(export(moved), "High"))).as("the spacer above it past its hairline, and nothing more")
                .isEqualTo(Math.round((520 - 0.1) * 20));
    }

    @Test
    void aLineSetHighOpeningItsPageIsNotLowered() throws Exception {
        // Word sets a page's first line at its top, whatever the space above it: grown, that space
        // would lower nothing, and what the paragraph under it gave would stand it higher.
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 545)
                .addParagraph(p -> p.name("High").text("High").textStyle(HIGH))
                .addParagraph(p -> p.name("Next").text("Next").textStyle(EVEN).margin(DocumentInsets.top(10)));
        try (DocumentSession session = session(content)) {
            assertThat(startPage(session, "High")).as("the premise: opening the second page").isEqualTo(1);
            assertThat(startPage(session, "Next")).as("the premise: under it there").isEqualTo(1);
        }
        XWPFDocument document = export(content);

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "Next"))).as("its 10pt, whole").isEqualTo(200);
    }

    @Test
    void aLineSetHighOpeningACardIsNotLowered() throws Exception {
        // A card's top border is taken inside the space above its first line (takeTheTopBorderInside):
        // grown, that space would give it back and the card grow.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                        .borders(com.demcha.compose.document.style.DocumentBorders.top(
                                com.demcha.compose.document.style.DocumentStroke.of(DocumentColor.BLACK, 2)))
                        .addParagraph(p -> p.text("High").textStyle(HIGH))
                        .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10)))));
        XWPFParagraph high = inACell(document, "High");

        assertThat(lowering(high)).as("the premise: set high").isGreaterThan(1);
        assertThat(before(inACell(document, "Next"))).as("its 10pt, whole").isEqualTo(200);
    }

    @Test
    void aLineSetHighByUnderATenthOfAPointIsNotLowered() throws Exception {
        // Word sets lines on a grid 0.12pt apart: a twip of space moves a line a step or nothing.
        DocumentTextStyle small = DocumentTextStyle.builder().fontName(FontName.UBUNTU).size(2.5).build();
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("Small").textStyle(small))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFParagraph line = paragraph(document, "Small");
        double high = UBUNTU_ASCENT * 2.5 - 0.8 * DocxTwips.of(line.getCTP().getPPr().getSpacing().getLine()) / 20.0;

        assertThat(high).as("the premise: set high, by under a tenth of a point").isBetween(0.05, 0.1);
        assertThat(before(line)).isZero();
        assertThat(before(paragraph(document, "Next"))).isEqualTo(200);
    }

    @Test
    void aLineSetHighAboveAListOpeningTheNextPageIsNotLowered() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 520)
                .addParagraph(p -> p.name("High").text("High").textStyle(HIGH))
                .addList(list -> list.name("Next").textStyle(EVEN).margin(DocumentInsets.top(30)).items("Item"));
        try (DocumentSession session = session(content)) {
            assertThat(startPage(session, "High")).as("the premise: on the first page").isZero();
            assertThat(startPage(session, "Next")).as("the premise: opening the second").isEqualTo(1);
        }
        XWPFDocument document = export(content);

        assertThat(before(paragraph(document, "High"))).as("the spacer above it past its hairline, and nothing more")
                .isEqualTo(Math.round((520 - 0.1) * 20));
    }

    @Test
    void aListsItemsSetHighAreLeftToTheirList() throws Exception {
        // Set high by under half a point, a list's items wait for nothing under them.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addList(list -> list.textStyle(HIGH).items("One", "Two"))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFDocument control = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addList(list -> list.textStyle(EVEN).items("One", "Two"))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));

        assertThat(lowering(paragraph(document, "One"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "Two"))).isEqualTo(before(paragraph(control, "Two")));
        assertThat(before(paragraph(document, "Next"))).isEqualTo(before(paragraph(control, "Next")));
    }

    @Test
    void aLineSetHighAboveARuleOverARowIsLoweredOnce() throws Exception {
        // The rule under it gives its lowering from the space above the rule; the row's cells,
        // written while it waits, do not give it again.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addLine(line -> line.horizontal(100).thickness(1).color(DocumentColor.BLACK)
                        .margin(DocumentInsets.top(10)))
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right"))));
        XWPFParagraph high = paragraph(document, "High");

        assertThat(before(high)).as("lowered, once").isEqualTo(lowering(high));
    }

    @Test
    void aLineSetHighAboveATableTakesNothingOfTheSpaceUnderTheTable() throws Exception {
        // A tenth of a point above the table, less than the line is set high: the paragraph under
        // the table is not the line's to take the rest from.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(0.1)))
                .addTable(table -> table.autoColumns(1).row("Cell"))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFParagraph high = paragraph(document, "High");

        assertThat(lowering(high)).as("the premise: lower than the space above the table").isGreaterThan(2);
        assertThat(before(high)).as("the tenth of a point above the table").isEqualTo(2);
        assertThat(before(paragraph(document, "Next"))).as("its 10pt, whole").isEqualTo(200);
    }

    @Test
    void aSpacerUnderALineSetHighEndsItsWait() throws Exception {
        // The spacer takes nothing of its own top edge: the paragraph under it is not the line's to
        // take from.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addSpacer(spacer -> spacer.width(100).height(10))
                .addParagraph(p -> p.text("Next").textStyle(EVEN)));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "High"))).isZero();
    }

    @Test
    void aLineSetHighAboveABandIsNotLoweredByTheBandsContent() throws Exception {
        // A band lays its text over a backdrop and measures it from the page: its content is not
        // the line's to take from.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addLayerStack(stack -> stack.margin(DocumentInsets.top(10))
                        .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(200, 40).build())
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Next").textStyle(EVEN)
                                .margin(DocumentInsets.top(10)).build(), LayerAlign.TOP_LEFT)));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "High"))).isZero();
    }

    @Test
    void aLineSetHighAboveAShapeHoldingTextIsNotLoweredByThatText() throws Exception {
        // The shape is drawn where the page puts it and its text is set in the room held for it:
        // the space above that text is not the line's to take, or the text would leave its shape.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .add(new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Badge").rectangle(200, 30)
                        .fillColor(DocumentColor.rgb(238, 243, 249)).margin(DocumentInsets.top(10))
                        .position(new com.demcha.compose.document.dsl.ParagraphBuilder().name("Inside").text("Inside")
                                .textStyle(EVEN).build(), 0, 0, LayerAlign.TOP_LEFT)
                        .build()));

        assertThat(lowering(paragraph(document, "High"))).as("the premise: set high").isGreaterThan(1);
        assertThat(before(paragraph(document, "High"))).isZero();
    }

    @Test
    void aLineSetHighAboveAShapeHoldingATableIsNotLoweredByWhatFollows() throws Exception {
        // The space the line owes below it is written once the table in the shape starts: the
        // shape is drawn where the page puts it, and that space is not the line's to take, nor,
        // past the shape, the space above the paragraph under it, or the table would stand lower.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .add(badgeHoldingATable())
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFDocument control = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN).margin(DocumentInsets.bottom(10)))
                .add(badgeHoldingATable())
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFParagraph high = paragraph(document, "High");

        assertThat(lowering(high)).as("the premise: set high").isGreaterThan(1);
        assertThat(before(high)).isZero();
        assertThat(after(high)).as("the space under it, whole").isEqualTo(after(paragraph(control, "High")));
        assertThat(before(paragraph(document, "Next"))).isEqualTo(before(paragraph(control, "Next")));
    }

    @Test
    void aLineSetHighAboveAStackOfOneLayerHoldingARowIsNotLoweredByWhatFollows() throws Exception {
        // The row is written in the stack's overlay: neither the space above it nor the space
        // above the paragraph under the stack is the line's to take.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH).margin(DocumentInsets.bottom(10)))
                .addLayerStack(stack -> stack.layer(stackedRow(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFDocument control = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(EVEN).margin(DocumentInsets.bottom(10)))
                .addLayerStack(stack -> stack.layer(stackedRow(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));
        XWPFParagraph high = paragraph(document, "High");

        assertThat(lowering(high)).as("the premise: set high").isGreaterThan(1);
        assertThat(before(high)).isZero();
        assertThat(after(high)).isEqualTo(after(paragraph(control, "High")));
        assertThat(before(paragraph(document, "Next"))).isEqualTo(before(paragraph(control, "Next")));
    }

    @Test
    void aBlockWithNoSpaceAboveItBetweenTheLineAndWhatFollowsLowersNothing() throws Exception {
        // A rule right under the line gives nothing, and the paragraph under the rule is not
        // the line's to take from.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addParagraph(p -> p.text("High").textStyle(HIGH))
                .addLine(line -> line.horizontal(100).thickness(1).color(DocumentColor.BLACK))
                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10))));

        assertThat(before(paragraph(document, "High"))).isZero();
        assertThat(before(paragraph(document, "Next"))).as("its 10pt, less the rule's tenth past its box")
                .isEqualTo(200 - 2);
    }

    @Test
    void aLineSetHighInAStackOfOneLayerIsLoweredAsInTheFlow() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addLayerStack(stack -> stack.layer(new com.demcha.compose.document.dsl.SectionBuilder()
                        .addParagraph(p -> p.text("High").textStyle(HIGH))
                        .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10)))
                        .build(), LayerAlign.TOP_LEFT)));
        long lower = lowering(paragraph(document, "High"));

        assertThat(before(paragraph(document, "High"))).as("lowered").isEqualTo(lower);
        assertThat(before(paragraph(document, "Next"))).isEqualTo(200 - lower);
    }

    @Test
    void aLineSetHighInAStackLayingOneLayerOverAnotherIsNotLowered() throws Exception {
        // An overlay's writer measures what follows from the page.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addLayerStack(stack -> stack
                        .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(200, 60).build())
                        .layer(new com.demcha.compose.document.dsl.SectionBuilder()
                                .addParagraph(p -> p.text("High").textStyle(HIGH))
                                .addParagraph(p -> p.text("Next").textStyle(EVEN).margin(DocumentInsets.top(10)))
                                .build(), LayerAlign.TOP_LEFT)));
        XWPFParagraph high = anywhere(document, "High");

        assertThat(lowering(high)).as("the premise: set high").isGreaterThan(1);
        assertThat(before(high)).isZero();
    }

    /** A shape holding a table, set where the page puts it. */
    private static com.demcha.compose.document.node.DocumentNode badgeHoldingATable() {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Badge").rectangle(200, 30)
                .fillColor(DocumentColor.rgb(238, 243, 249))
                .position(new com.demcha.compose.document.dsl.TableBuilder().autoColumns(1).row("Cell").build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .build();
    }

    /** A section holding a row of two cells, for a stack's one layer. */
    private static com.demcha.compose.document.node.DocumentNode stackedRow() {
        return new com.demcha.compose.document.dsl.SectionBuilder()
                .addRow(row -> row.addSection("Left", left -> left.addParagraph("Left"))
                        .addSection("Right", right -> right.addParagraph("Right")))
                .build();
    }

    /** How far Word sets a 10pt Ubuntu line above the page's baseline, in twips, from the line written. */
    private static long lowering(XWPFParagraph paragraph) {
        assertThat(paragraph.getCTP().getPPr().getSpacing().getLineRule())
                .as("the premise: an exact line")
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
        double line = DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
        long twips = Math.round((UBUNTU_ASCENT * 10 - 0.8 * line) * 20);
        assertThat(twips).as("the premise: Word sets it high, by under half a point").isBetween(2L, 9L);
        return twips;
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.pageFlow(content::accept);
        return session;
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session(content)) {
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }

    private static int startPage(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow().startPage();
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph starting(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().startsWith(text)).findFirst().orElseThrow();
    }

    /** The paragraph holding exactly {@code text}, in the body or a table's cell. */
    private static XWPFParagraph anywhere(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().equals(text)).findFirst()
                .orElseGet(() -> inACell(document, text));
    }

    /** The paragraph of a table's cell, at any depth, holding exactly {@code text}. */
    private static XWPFParagraph inACell(XWPFDocument document, String text) {
        java.util.ArrayDeque<XWPFTable> tables = new java.util.ArrayDeque<>(document.getTables());
        while (!tables.isEmpty()) {
            for (var row : tables.pop().getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            return paragraph;
                        }
                    }
                    tables.addAll(cell.getTables());
                }
            }
        }
        throw new AssertionError("no cell holds " + text);
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
    }
}
