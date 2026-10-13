package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.LayerAlign;
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
import static org.assertj.core.api.Assertions.within;

/**
 * A line in a layer stack of one layer is seated as a line in the flow is.
 *
 * <p>Such a stack lays nothing over anything, and its contents are written in the flow: a
 * template wraps a row in one to set it in a row's cell. A line Word sets low rises into the space
 * above it and owes as much below, where the block under it, inside the stack or past it, is given
 * it, and a row's first lines rise out of the space above its table; a stack laying one layer over
 * another measures what follows it from the page, and its lines keep the space above them
 * ({@code DocxBaselineSeatTest}). The layout moves the stack to a new page whole,
 * and its first line then opens that page, where Word drops the space above it: it is neither
 * moved up into that space nor lowered by growing it there.</p>
 */
class DocxStackOfOneLayerSeatTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;
    /** Spectral at 30pt, which Word sets nearly 5pt low. */
    private static final DocumentTextStyle LOW = DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(30).build();
    /** Lato at 10pt, which Word sets high by under half a point. */
    private static final DocumentTextStyle HIGH = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    /** Gothic A1, which Word seats where the page does to a few hundredths: neither raised nor lowered. */
    private static final DocumentTextStyle EVEN = DocumentTextStyle.builder().fontName(FontName.GOTHIC_A1).size(10).build();

    @Test
    void aLineSetLowIsRaisedIntoTheSpaceAboveItAndTheBlockPastTheStackIsGivenWhatItOwes() throws Exception {
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                        .addParagraph(p -> p.text("Title").textStyle(LOW).margin(DocumentInsets.top(20)))
                        .build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Body").textStyle(EVEN)));
        XWPFParagraph title = paragraph(document, "Title");
        double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

        assertThat(raise).as("the premise: Word's baseline is the lower, by less than the space").isBetween(1.0, 20.0);
        assertThat(before(title)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
        assertThat(position(title)).as("nothing left to its position").isZero();
        assertThat(before(paragraph(document, "Body"))).as("what the title owes below it")
                .isCloseTo(Math.round(raise * 20), within(1L));
    }

    @Test
    void anItemSetLowRisesIntoTheSpaceAboveItAndTheNextIntoWhatItOwes() throws Exception {
        // As in the flow: the first item moves up into the list's 20pt above it, the second into the
        // gap and what the first owes, and the paragraph past the stack is given what the last owes.
        XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                        .addList(list -> list.textStyle(LOW).itemSpacing(10).margin(DocumentInsets.top(20))
                                .items("One", "Two"))
                        .build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Body").textStyle(EVEN)));
        XWPFParagraph one = paragraph(document, "One");
        double raise = 0.8 * line(one) - SPECTRAL_ASCENT * 30;

        assertThat(raise).as("the premise: Word's baseline is the lower, by less than the gap").isBetween(1.0, 10.0);
        assertThat(before(one)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
        assertThat(before(paragraph(document, "Two"))).as("the gap, what the first owed taken back")
                .isCloseTo(200L, within(1L));
        assertThat(before(paragraph(document, "Body"))).as("what the last item owes below it")
                .isCloseTo(Math.round(raise * 20), within(1L));
    }

    @Test
    void aLineOpeningItsPageIsNotRaisedIntoTheSpaceAboveIt() throws Exception {
        // Word drops the space above the page's first line: taken from it, the raise would move
        // the line nothing, and what it owed would stand everything under it lower.
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 500)
                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                        .addParagraph(p -> p.name("Title").text("Title").textStyle(LOW).margin(DocumentInsets.top(10)))
                        .build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Body").textStyle(EVEN));
        try (DocumentSession session = session(content)) {
            assertThat(startPage(session, "Title")).as("the premise: the stack opens the second page").isEqualTo(1);
        }
        XWPFDocument document = export(content);
        XWPFParagraph title = paragraph(document, "Title");
        double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

        assertThat(position(title)).as("raised by its position").isCloseTo((int) Math.round(raise * 2), within(1));
        assertThat(before(paragraph(document, "Body"))).as("nothing owed below it").isZero();
    }

    @Test
    void aLineSetHighOpeningItsPageIsNotLowered() throws Exception {
        // Grown, the space above the page's first line would lower nothing, and what the paragraph
        // under it gave would stand it higher.
        Consumer<PageFlowBuilder> content = page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN)).spacer(1, 545)
                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                        .addParagraph(p -> p.name("High").text("High").textStyle(HIGH))
                        .addParagraph(p -> p.name("Next").text("Next").textStyle(EVEN).margin(DocumentInsets.top(10)))
                        .build(), LayerAlign.TOP_LEFT));
        try (DocumentSession session = session(content)) {
            assertThat(startPage(session, "High")).as("the premise: the stack opens the second page").isEqualTo(1);
            assertThat(startPage(session, "Next")).as("the premise: under it there").isEqualTo(1);
        }
        XWPFDocument document = export(content);
        XWPFParagraph high = paragraph(document, "High");
        long lower = Math.round((LATO_ASCENT * 10 - 0.8 * line(high)) * 20);

        assertThat(lower).as("the premise: Word sets it high, by under half a point").isBetween(2L, 9L);
        assertThat(before(paragraph(document, "Next"))).as("its 10pt, whole").isEqualTo(200);
    }

    @Test
    void aRowsFirstLinesRiseOutOfTheSpaceAboveItsTable() throws Exception {
        // A template wraps a row in a stack of one layer to set it in a row's cell.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                        .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                        .build(), LayerAlign.TOP_LEFT))
                .addParagraph("After"))) {
            XWPFTable table = document.getTables().get(0);

            assertThat(after(paragraph(document, "Above"))).as("the gap, less the lift")
                    .isEqualTo(10 * 20 - DocxExports.DEFAULT_LINE_RAISE);
            for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
                assertThat(before(cell.getParagraphs().get(0))).isZero();
                assertThat(after(cell.getParagraphs().get(0))).as("given back below the line")
                        .isEqualTo(DocxExports.DEFAULT_LINE_RAISE);
            }
        }
    }

    @Test
    void aRowNestedThroughAStackInARowsCellRisesOutOfTheSpaceAboveItsTable() throws Exception {
        // A row cannot sit in a row's cell, and a template sets it in one through a stack of one layer.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Above")
                .addRow(outer -> outer
                        .addSection("Cell", cell -> cell.spacing(10).addParagraph("Head")
                                .addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                                        .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                                        .build(), LayerAlign.TOP_LEFT)))
                        .addParagraph("Other"))
                .addParagraph("After"))) {
            XWPFTableCell left = inACell(document, "Left");

            assertThat(after(inACell(document, "Head").getParagraphs().get(0))).as("the cell's gap, less the lift")
                    .isEqualTo(10 * 20 - DocxExports.DEFAULT_LINE_RAISE);
            for (XWPFTableCell cell : left.getTableRow().getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).as("given back below the line")
                        .isEqualTo(DocxExports.DEFAULT_LINE_RAISE);
            }
        }
    }

    @Test
    void aRowInAStackTheLayoutMovesToANewPageIsNotLifted() throws Exception {
        // Seven lines 10pt apart fill the 160pt of the first page, and the stack starts the next:
        // the space above its row stays on the page above, and lifted out of it the row would stand lower.
        Consumer<PageFlowBuilder> content = page -> {
            page.spacing(10);
            for (int i = 0; i < 7; i++) {
                page.addParagraph("Line " + i);
            }
            page.addLayerStack(stack -> stack.name("Entry").layer(new SectionBuilder()
                    .addRow(row -> row.name("Row").addParagraph("Left").addParagraph("Right"))
                    .build(), LayerAlign.TOP_LEFT));
            page.addParagraph("After");
        };
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            assertThat(startPage(session, "Row")).as("the premise: the row starts the second page").isOne();
        }
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, content)) {
            for (XWPFTableCell cell : inACell(document, "Left").getTableRow().getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).isZero();
            }
        }
    }

    @Test
    void aLineInAStackOfOneLayerInsideABandIsRaisedIntoTheSpaceAboveIt() throws Exception {
        // The outer stack, laying its layer over a shape, is a band: it pays what its last line owes
        // below for its raise (DocxBaselineSeatTest), so the line moves, as in the flow.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(EVEN))
                .addLayerStack(outer -> outer.name("Card")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 80).build())
                        .layer(new LayerStackBuilder().name("Entry").layer(new SectionBuilder()
                                .addParagraph(p -> p.text("Title").textStyle(LOW).margin(DocumentInsets.top(20)))
                                .build(), LayerAlign.TOP_LEFT).build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph title = paragraph(document, "Title");
            double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(raise).as("the premise: Word's baseline is the lower").isGreaterThan(1);
            assertThat(before(title)).as("its 20pt less the raise").isCloseTo(Math.round((20 - raise) * 20), within(1L));
            assertThat(position(title)).as("no position: the line moves").isZero();
        }
    }

    @Test
    void aRowOfAStackLayingItOverAShapeIsNotLifted() throws Exception {
        // The stack measures what follows it from the page: the space under the row's lines would
        // not reach what follows.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addLayerStack(stack -> stack.name("Entry")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 40).build())
                        .layer(new SectionBuilder().addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                                .build(), LayerAlign.TOP_LEFT))
                .addParagraph("After"))) {
            XWPFTableCell left = inACell(document, "Left");

            assertThat(after(left.getParagraphs().get(0))).as("nothing given back below the line").isZero();
        }
    }

    @Test
    void aRowInAStackOfOneLayerInsideAStackLayingItOverAShapeIsNotLifted() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addLayerStack(outer -> outer.name("Card")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 40).build())
                        .layer(new LayerStackBuilder().name("Entry").layer(new SectionBuilder()
                                .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                                .build(), LayerAlign.TOP_LEFT).build(), LayerAlign.TOP_LEFT))
                .addParagraph("After"))) {
            XWPFTableCell left = inACell(document, "Left");

            assertThat(after(left.getParagraphs().get(0))).as("nothing given back below the line").isZero();
        }
    }

    private static XWPFTableCell inACell(XWPFDocument document, String text) {
        XWPFTableCell found = inACell(document.getTables(), text);
        if (found == null) {
            throw new AssertionError("no cell opening with " + text);
        }
        return found;
    }

    /** The cell opening with the text, in these tables or the tables their cells hold. */
    private static XWPFTableCell inACell(java.util.List<XWPFTable> tables, String text) {
        for (XWPFTable table : tables) {
            for (var row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (!cell.getParagraphs().isEmpty() && cell.getParagraphs().get(0).getText().equals(text)) {
                        return cell;
                    }
                    XWPFTableCell nested = inACell(cell.getTables(), text);
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
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

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(spacing(paragraph).getLine()) / 20.0;
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    private static int position(XWPFParagraph paragraph) {
        var properties = paragraph.getRuns().get(0).getCTR().getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).intValue();
    }
}
