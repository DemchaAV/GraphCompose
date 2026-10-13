package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A line in a band is raised into the space above it, as in the flow, and the band pays what it
 * owes below.
 *
 * <p>A band measures the space down to each later layer, and below itself, from the page. A line
 * raised into the space above it owes that much below it, and the band dropped it. So no line in a
 * band was raised that way, and Word set {@code ConsultingInvoice}'s contact lines half a point low
 * in Word and both LibreOffice builds.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBandRaiseTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    private static final DocumentTextStyle TITLE = DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(30).build();

    @Test
    void aLaterLayerResumesBelowTheRaisedLineAboveIt() throws Exception {
        // The second layer's gap is the page's, down from a line set as high as it was raised.
        try (Export export = export(page -> page
                .addLayerStack(stack -> stack.name("Band")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 160).build())
                        .layer(title("First", 10), LayerAlign.TOP_LEFT)
                        .layer(title("Second", 0), LayerAlign.BOTTOM_LEFT)))) {
            XWPFParagraph first = export.paragraph("First");
            XWPFParagraph second = export.paragraph("Second");
            double raise = raiseOf(first);
            double gap = export.placed("First").placementY()
                         - (export.placed("Second").placementY() + export.placed("Second").placementHeight());

            assertThat(raise).as("the premise: Word's baseline is the lower").isGreaterThan(1);
            assertThat(before(first)).as("the first line raised into its 10pt").isCloseTo(Math.round((10 - raise) * 20), within(1L));
            assertThat(after(first) + before(second))
                    .as("the page's gap down to the second, which rises as far: the first's raise owed back")
                    .isCloseTo(Math.round(gap * 20), within(2L));
        }
    }

    @Test
    void aRaiseOwedInACellAmongTheLayersIsPaidInItsCell() throws Exception {
        // A row among the layers holds its cell's raise in the cell, which pays it as it ends: the
        // band owes nothing more for it.
        try (Export export = export(page -> page
                .addLayerStack(stack -> stack.name("Band")
                        .layer(new ShapeBuilder().name("Backdrop").size(200, 120).build(), LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder().addRow(row -> row.name("Row")
                                        .addParagraph(p -> p.text("Left"))
                                        .addParagraph(p -> p.name("Right").text("Right").textStyle(TITLE)
                                                .margin(DocumentInsets.top(20))))
                                .build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.name("Below").text("Below").textStyle(TITLE)))) {
            XWPFTableCell cell = export.document().getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph right = cell.getParagraphs().get(cell.getParagraphs().size() - 1);
            XWPFParagraph below = export.paragraph("Below");
            double raise = raiseOf(right);
            double gap = export.placed("Row").placementY()
                         - (export.placed("Below").placementY() + export.placed("Below").placementHeight());

            assertThat(right.getText()).as("the premise: the last cell's last paragraph, the last line written in the"
                                           + " band").isEqualTo("Right");
            assertThat(after(right)).as("its raise owed in its cell").isCloseTo(Math.round(raise * 20), within(1L));
            assertThat(before(below)).as("from the row's foot, less the line's own raise: nothing of the cell's")
                    .isCloseTo(Math.round((gap - raiseOf(below)) * 20), within(2L));
        }
    }

    @Test
    void aBandThatWritesNothingOwesTheRaiseAboveItOnce() throws Exception {
        // The raise the line before the band owes is in the space the band opens with, which a band
        // writing nothing hands on whole: it is not owed a second time.
        try (Export export = export(page -> page
                .addParagraph(p -> p.name("Lead").text("Lead").textStyle(TITLE).margin(DocumentInsets.top(10)))
                .addLayerStack(stack -> stack.name("Band")
                        .layer(new ShapeBuilder().name("Backdrop").size(200, 40).build(), LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder().name("Empty").build(), LayerAlign.CENTER))
                .addParagraph(p -> p.name("Next").text("Next").textStyle(TITLE)))) {
            XWPFParagraph lead = export.paragraph("Lead");
            XWPFParagraph next = export.paragraph("Next");
            double gap = export.placed("Lead").placementY()
                         - (export.placed("Next").placementY() + export.placed("Next").placementHeight());

            assertThat(export.document().getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("the premise: the band writes no paragraph").containsExactly("Lead", "Next");
            assertThat(before(lead)).as("the lead raised into its 10pt").isLessThan(200);
            assertThat(after(lead) + before(next)).as("the page's gap, the lead's raise owed back once")
                    .isCloseTo(Math.round(gap * 20), within(2L));
        }
    }

    @Test
    void aBandOpeningItsPageLeavesItsFirstLineWhereWordSetsItAndRaisesTheRest() throws Exception {
        // Word drops the space above the first paragraph on a page: raised into it, the line would
        // stand where Word sets it, and what it owed would stand all below it lower.
        try (Export export = export(page -> page
                .spacer(0, 500)
                .addLayerStack(stack -> stack.name("Band")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 140).build())
                        .layer(new SectionBuilder()
                                .add(title("One", 10))
                                .add(title("Two", 10))
                                .build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph one = export.paragraph("One");
            XWPFParagraph two = export.paragraph("Two");

            assertThat(export.placed("Band").startPage()).as("the premise: the band opens the second page").isOne();
            assertThat(position(one)).as("raised by its position, not into the space Word drops")
                    .isCloseTo((int) Math.round(raiseOf(one) * 2), within(1));
            assertThat(position(two)).as("the next line moves").isZero();
            assertThat(before(two)).as("raised into its 10pt").isCloseTo(Math.round((10 - raiseOf(two)) * 20), within(1L));
        }
    }

    @Test
    void aBandOpeningItsPageWithATableRaisesItsCellsLinesAndTheLineAfterIt() throws Exception {
        // The table opens the page: neither a paragraph in its cell nor the one under it is the
        // page's first, and Word keeps the space above them.
        try (Export export = export(page -> page
                .spacer(0, 500)
                .addLayerStack(stack -> stack.name("Band")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 160).build())
                        .layer(new SectionBuilder()
                                .addRow(row -> row
                                        .addParagraph(p -> p.name("Left").text("Left").textStyle(TITLE)
                                                .margin(DocumentInsets.top(20)))
                                        .addParagraph("Right"))
                                .add(title("Two", 10))
                                .build(), LayerAlign.TOP_LEFT)))) {
            XWPFTableCell cell = export.document().getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph left = cell.getParagraphs().get(cell.getParagraphs().size() - 1);
            XWPFParagraph two = export.paragraph("Two");

            assertThat(export.placed("Band").startPage()).as("the premise: the band opens the second page").isOne();
            assertThat(left.getText()).as("the premise: the first cell's paragraph").isEqualTo("Left");
            assertThat(position(left)).as("the cell's line moves").isZero();
            assertThat(position(two)).as("the line under the table moves").isZero();
            assertThat(before(two)).as("raised into its 10pt").isCloseTo(Math.round((10 - raiseOf(two)) * 20), within(1L));
        }
    }

    @Test
    void aListOpeningABandsPageLeavesItsFirstItemWhereWordSetsItAndRaisesTheNext() throws Exception {
        // A list's item is raised as a paragraph is: its first, opening the page, not.
        try (Export export = export(page -> page
                .spacer(0, 500)
                .addLayerStack(stack -> stack.name("Band").padding(10)
                        .back(new ShapeBuilder().name("Backdrop").size(200, 140).build())
                        .layer(new com.demcha.compose.document.dsl.ListBuilder().noMarker().textStyle(TITLE)
                                .itemSpacing(10).items("One", "Two").build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph one = export.paragraph("One");
            XWPFParagraph two = export.paragraph("Two");

            assertThat(export.placed("Band").startPage()).as("the premise: the band opens the second page").isOne();
            assertThat(before(one)).as("the premise: space above it to raise it into").isPositive();
            assertThat(position(one)).as("raised by its position, not into the space Word drops")
                    .isCloseTo((int) Math.round(raiseOf(one) * 2), within(1));
            assertThat(position(two)).as("the next item moves").isZero();
        }
    }

    @Test
    void aRaiseIsOwedPastTheRoomHeldForADrawingAfterIt() throws Exception {
        // A canvas that only draws holds its room as space owed, and writes no paragraph: the
        // raise the line above it owes is still owed below the band.
        try (Export export = export(page -> page
                .addLayerStack(stack -> stack.name("Band")
                        .layer(new ShapeBuilder().name("Backdrop").size(200, 160).build(), LayerAlign.TOP_LEFT)
                        .layer(new SectionBuilder()
                                .add(title("Title", 10))
                                .add(new com.demcha.compose.document.dsl.CanvasLayerBuilder(60, 24)
                                        .name("Logo").position(new ShapeBuilder().size(20, 20).build(), 0, 0).build())
                                .build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.name("Next").text("Next").textStyle(TITLE)))) {
            XWPFParagraph title = export.paragraph("Title");
            XWPFParagraph next = export.paragraph("Next");
            double gap = export.placed("Title").placementY()
                         - (export.placed("Next").placementY() + export.placed("Next").placementHeight());

            assertThat(before(title)).as("the title raised into its 10pt").isCloseTo(Math.round((10 - raiseOf(title)) * 20), within(1L));
            assertThat(after(title) + before(next)).as("the page's gap, the canvas's room in it, the raise owed back")
                    .isCloseTo(Math.round(gap * 20), within(2L));
        }
    }

    private static com.demcha.compose.document.node.DocumentNode title(String name, double above) {
        return new ParagraphBuilder().name(name).text(name).textStyle(TITLE).margin(DocumentInsets.top(above)).build();
    }

    /** How far Word sets a Spectral 30pt line's baseline below the page's, in points. */
    private static double raiseOf(XWPFParagraph paragraph) {
        BigInteger line = (BigInteger) paragraph.getCTP().getPPr().getSpacing().getLine();
        return 0.8 * line.doubleValue() / 20 - SPECTRAL_ASCENT * 30;
    }

    /** The position the paragraph's first run is raised by, in half points. */
    private static int position(XWPFParagraph paragraph) {
        var properties = paragraph.getRuns().get(0).getCTR().getRPr();
        return properties != null && properties.sizeOfPositionArray() > 0
                ? ((BigInteger) properties.getPositionArray(0).getVal()).intValue() : 0;
    }

    private static long before(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? ((BigInteger) spacing.getBefore()).longValue() : 0;
    }

    private static long after(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetAfter() ? ((BigInteger) spacing.getAfter()).longValue() : 0;
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.pageFlow(content);
        byte[] bytes = session.export(new DocxSemanticBackend());
        return new Export(session, new XWPFDocument(new ByteArrayInputStream(bytes)));
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        XWPFParagraph paragraph(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().equals(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph reads " + text));
        }

        PlacedNode placed(String name) {
            List<PlacedNode> nodes = session.layoutGraph().nodes();
            return nodes.stream().filter(node -> name.equals(node.semanticName())).findFirst()
                    .orElseThrow(() -> new AssertionError("nothing placed is named " + name));
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
