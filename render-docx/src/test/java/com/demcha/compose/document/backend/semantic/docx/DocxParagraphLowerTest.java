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
 * its face's ascent below the line's top: Lato's ascent is 0.82 of its line, and at 10pt Word
 * sets it about a quarter point high. Under half a point its position moves nothing, and a raise
 * only moves a line up. What nothing under the line gives back, it is not lowered by, so nothing
 * under it ever stands lower.</p>
 */
class DocxParagraphLowerTest {

    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;
    /** A line Word sets high, by under half a point. */
    private static final DocumentTextStyle HIGH = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
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

        assertThat(after(high)).as("the premise: the cell's space below left out").isZero();
        assertThat(before(high)).as("taken back").isZero();
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

    /** How far Word sets a 10pt Lato line above the page's baseline, in twips, from the line written. */
    private static long lowering(XWPFParagraph paragraph) {
        double line = DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
        long twips = Math.round((LATO_ASCENT * 10 - 0.8 * line) * 20);
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
