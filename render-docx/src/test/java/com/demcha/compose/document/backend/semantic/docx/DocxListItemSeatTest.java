package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A list's item stands on the page's baseline in Word's exact lines, as a paragraph does.
 *
 * <p>Word stands an exact line's baseline four fifths of the way down it, whatever the face;
 * the page sets it the face's ascent below the line's top. An item set low rises into the space
 * above it, the gap between the items, and owes as much below; one set high, or low by more than
 * that space gives, is moved by its position.</p>
 */
class DocxListItemSeatTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    /** Spectral's own line, in ems: its ascent and 463 units of descent. */
    private static final double SPECTRAL_LINE = 1.522;
    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;

    @Test
    void anItemWordSetsLowRisesIntoTheSpaceAboveItAndTheNextIntoWhatItOwes() throws Exception {
        // Spectral's 30pt line stands nearly 5pt low in Word. The first item moves up into the
        // list's 20pt above it; the second into the gap and what the first owes, the gap left as
        // it was; the paragraph under the list stands where it did, the last item's raise above it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(spectral(30)).itemSpacing(10).margin(DocumentInsets.top(20))
                        .items("One", "Two"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10))))) {
            XWPFParagraph one = paragraph(document, "One");
            XWPFParagraph two = paragraph(document, "Two");
            double raise = 0.8 * line(one) - SPECTRAL_ASCENT * 30;

            assertThat(raise).as("the premise: Word's baseline is the lower, by less than the gap").isBetween(1.0, 10.0);
            assertThat(before(one)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
            assertThat(before(two)).as("the gap, what the first owed taken back").isCloseTo(200L, within(1L));
            assertThat(position(one)).isZero();
            assertThat(position(two)).isZero();
            assertThat(before(paragraph(document, "Body"))).as("what the last item owes below it")
                    .isCloseTo(Math.round(raise * 20), within(1L));
        }
    }

    @Test
    void anItemWhoseMarkerIsWrittenAsTextIsLoweredWithItByItsPosition() throws Exception {
        // Lato's 24pt line stands 0.65pt high in Word: moved down, the space above as written, and
        // the marker, a run of the item's own, with it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).marker("•").hangingIndent(true).markerGap(6)
                        .margin(DocumentInsets.top(10)).addItem(text -> text.style("Heading", lato(24)))))) {
            XWPFParagraph item = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Heading")).findFirst().orElseThrow();
            double raise = 0.8 * line(item) - LATO_ASCENT * 24;

            assertThat(item.getText()).as("the premise: its marker written as its text").startsWith("•");
            assertThat(item.getCTP().getPPr().getSpacing().getLineRule()).as("the premise: an exact line")
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(raise).as("the premise: Word's baseline is the higher, by more than half a point")
                    .isLessThan(-0.5);
            assertThat(before(item)).isEqualTo(200);
            for (var run : item.getCTP().getRList()) {
                assertThat(positionOf(run)).as("every run, the marker's too").isEqualTo(Math.round(raise * 2));
            }
        }
    }

    @Test
    void anItemWhoseMarkerWordDrawsIsNotMovedByItsPosition() throws Exception {
        // Word sets a Word list's marker from the paragraph's mark, and LibreOffice does not move
        // it by the mark's position: lowered by its position, the text would stand off its bullet.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).items("Heading")))) {
            XWPFParagraph item = paragraph(document, "Heading");
            double raise = 0.8 * line(item) - LATO_ASCENT * 24;

            assertThat(item.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
            assertThat(raise).as("the premise: Word's baseline is the higher, by more than half a point")
                    .isLessThan(-0.5);
            assertThat(position(item)).isZero();
            assertThat(markPosition(item)).isZero();
        }
    }

    @Test
    void anItemWhoseMarkerWordDrawsRisesAsFarAsTheSpaceAboveAndNoFurther() throws Exception {
        // Spectral's 30pt line stands nearly 5pt low in Word, under 2pt of space: the line and its
        // marker rise those 2pt, and what is left stays, rather than the text alone moving on.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(spectral(30)).margin(DocumentInsets.top(2)).items("Title"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10))))) {
            XWPFParagraph item = paragraph(document, "Title");
            double raise = 0.8 * line(item) - SPECTRAL_ASCENT * 30;

            assertThat(item.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
            assertThat(raise).as("the premise: lower than the space above holds").isGreaterThan(2.0);
            assertThat(before(item)).as("all of the 2pt").isZero();
            assertThat(position(item)).isZero();
            assertThat(markPosition(item)).isZero();
            assertThat(before(paragraph(document, "Body"))).as("the 2pt the line took").isEqualTo(40);
        }
    }

    @Test
    void anItemInALayerStackKeepsTheSpaceAboveIt() throws Exception {
        // A stack measures what follows it from the page: a raise owed below an item inside it
        // would not reach what follows.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above").textStyle(lato(10)))
                .addLayerStack(stack -> stack.name("Stack")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 80).build())
                        .layer(new ListBuilder().noMarker().textStyle(spectral(30)).margin(DocumentInsets.top(20))
                                .items("Title").build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph item = anywhere(document, "Title");
            double raise = 0.8 * line(item) - SPECTRAL_ASCENT * 30;

            assertThat(raise).as("the premise: Word's baseline is the lower").isGreaterThan(1);
            assertThat(after(item)).as("nothing owed below it").isZero();
            assertThat(position(item)).as("raised by its position").isCloseTo(Math.round(raise * 2), within(1L));
        }
    }

    @Test
    void anItemTheLayoutsLinesAreNotMatchedToIsLeftAsWordSetsIt() throws Exception {
        // A list in a table's composed cell lays out no lines of its own to seat it by.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new ListBuilder().noMarker().textStyle(spectral(30))
                        .items("Inside").build()))))) {
            XWPFParagraph inside = inACell(document, "Inside");

            assertThat(position(inside)).isZero();
            assertThat(before(inside)).isZero();
        }
    }

    @Test
    void anItemsWrappedLinesAreSeatedAtTheirMiddleOne() throws Exception {
        // Two lines 10pt apart and no space above to give: Word's lines are each 5pt taller than
        // the face, stepping 5pt closer than the page's, and the error is shared by the two.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.noMarker().textStyle(spectral(20)).lineSpacing(10).items("First\nSecond")))) {
            XWPFParagraph item = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("First")).findFirst().orElseThrow();
            double face = SPECTRAL_LINE * 20;
            double line = line(item);
            double middle = 0.5;

            assertThat(line).as("the premise: the one gap shared by two lines").isCloseTo(face + 5, within(0.05));
            assertThat(position(item)).isCloseTo(Math.round(
                    (middle * line + 0.8 * line - middle * (face + 10) - SPECTRAL_ASCENT * 20) * 2), within(1L));
        }
    }

    @Test
    void anItemTheLayoutSetsOnANewPageKeepsTheSpaceAboveIt() throws Exception {
        // Word drops a paragraph's space above at the top of a page: raised into it, the second
        // item would stand as low as before and what it owed would stand the third lower.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 480)
                    .addList(list -> list.name("Duties").noMarker().textStyle(spectral(30)).itemSpacing(10)
                            .margin(DocumentInsets.top(10)).items("One", "Two", "Three")));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph one = paragraph(document, "One");
                XWPFParagraph two = paragraph(document, "Two");
                XWPFParagraph three = paragraph(document, "Three");
                double raise = 0.8 * line(one) - SPECTRAL_ASCENT * 30;

                assertThat(pages).as("the premise: the second item opens the next page").containsExactly(0, 1, 1);
                assertThat(before(one)).as("the spacer's height past its hairline and the list's 10pt, less the raise")
                        .isCloseTo(Math.round((480 - 0.1 + 10 - raise) * 20), within(1L));
                assertThat(before(two)).as("the gap and what the first owed, whole")
                        .isCloseTo(Math.round((10 + raise) * 20), within(1L));
                assertThat(position(two)).as("raised by its position").isCloseTo(Math.round(raise * 2), within(1L));
                assertThat(before(three)).as("the gap less its own raise, nothing owed above it")
                        .isCloseTo(Math.round((10 - raise) * 20), within(1L));
            }
        }
    }

    @Test
    void theFirstItemOfAListTheLayoutStartsOnANewPageKeepsTheSpaceAboveIt() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 520)
                    .addList(list -> list.name("Duties").noMarker().textStyle(spectral(30)).margin(DocumentInsets.top(10))
                            .items("One"))
                    .addParagraph(p -> p.text("Body").textStyle(lato(10))));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph one = paragraph(document, "One");
                double raise = 0.8 * line(one) - SPECTRAL_ASCENT * 30;

                assertThat(pages).as("the premise: the list opens the next page").containsExactly(1);
                assertThat(position(one)).as("raised by its position").isCloseTo(Math.round(raise * 2), within(1L));
                assertThat(before(paragraph(document, "Body"))).as("nothing owed below it").isZero();
            }
        }
    }

    @Test
    void anItemOpeningAPanelRisesIntoItsPadding() throws Exception {
        // The default text stands RAISE twips low in Word, under half a point: the item opening a
        // card has no space above it, and takes the raise from the card's padding.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("Before")
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                        .padding(DocumentInsets.of(8)).addList(list -> list.items("Inside"))))) {
            XWPFTableCell panel = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph inside = panel.getParagraphs().get(0);

            assertThat(inside.getText()).as("the premise: the card opens with the item").isEqualTo("Inside");
            assertThat(topMargin(panel)).as("its padding, less what the item rose into").isEqualTo(8 * 20 - RAISE);
            assertThat(after(inside)).as("given back below the item").isEqualTo(RAISE);
            assertThat(position(inside)).isZero();
        }
    }

    @Test
    void anItemOpeningARowsColumnRisesOutOfTheSpaceAboveItsTable() throws Exception {
        // A row of columns has its space above written above its table: the row is lifted out of
        // it, and each item gives what it rose back below itself.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.addSection(column -> column.add(new ListBuilder().items("Left").build()))
                        .addSection(column -> column.add(new ListBuilder().items("Right").build())))
                .addParagraph("After"))) {
            XWPFTable table = document.getTables().get(0);
            List<IBodyElement> body = document.getBodyElements();
            XWPFParagraph above = (XWPFParagraph) body.get(body.indexOf(table) - 1);

            assertThat(after(above)).as("the gap, less the lift").isEqualTo(10 * 20 - RAISE);
            for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
                XWPFParagraph first = cell.getParagraphs().get(0);
                assertThat(first.getCTP().getPPr().isSetNumPr()).as("the premise: the column opens with an item").isTrue();
                assertThat(before(first)).isZero();
                assertThat(after(first)).as("given back below the item").isEqualTo(RAISE);
            }
        }
    }

    @Test
    void anItemOfAListInACellTheLayoutBreaksOverAPageKeepsTheSpaceAboveIt() throws Exception {
        // A panel the page breaks is one cell going on over the next page: what the first item
        // owed would be the space above the one opening that page, inside the cell.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 460)
                    .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                            .addList(list -> list.name("Duties").noMarker().textStyle(spectral(30)).itemSpacing(10)
                                    .margin(DocumentInsets.top(10)).items("One", "Two", "Three"))));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph one = inACell(document, "One");
                XWPFParagraph two = inACell(document, "Two");
                XWPFParagraph three = inACell(document, "Three");
                double raise = 0.8 * line(three) - SPECTRAL_ASCENT * 30;

                assertThat(pages).as("the premise: the second item opens the next page").containsExactly(0, 1, 1);
                assertThat(document.getTables()).as("the premise: one cell over both pages").hasSize(1);
                for (XWPFParagraph item : List.of(one, two, three)) {
                    assertThat(before(item)).as("the space above %s, as written", item.getText()).isEqualTo(200);
                    assertThat(position(item)).as("raised by its position").isCloseTo(Math.round(raise * 2), within(1L));
                }
            }
        }
    }

    @Test
    void anItemOfRunsIsSeatedAsAPlainOneIs() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(spectral(30)).marker("•").hangingIndent(true).markerGap(6)
                        .margin(DocumentInsets.top(20))
                        .addItem(text -> text.style("Rich ", spectral(30)).style("item", spectral(30)))))) {
            XWPFParagraph item = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("item")).findFirst().orElseThrow();
            double raise = 0.8 * line(item) - SPECTRAL_ASCENT * 30;

            assertThat(item.getCTP().getPPr().isSetNumPr()).as("the premise: its marker written as text").isFalse();
            assertThat(before(item)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
            assertThat(position(item)).isZero();
        }
    }

    private static DocumentTextStyle spectral(double size) {
        return DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(size).build();
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
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

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
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

    private static long topMargin(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return properties == null || !properties.isSetTcMar() || !properties.getTcMar().isSetTop() ? 0
                : DocxTwips.of(properties.getTcMar().getTop().getW());
    }

    /** The {@code w:position} of a paragraph's first run, in half points; 0 for none. */
    private static long position(XWPFParagraph paragraph) {
        return positionOf(paragraph.getCTP().getRArray(0));
    }

    private static long positionOf(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
        var properties = run.getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).longValue();
    }

    /** The {@code w:position} of a paragraph's mark, in half points; 0 for none. */
    private static long markPosition(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetRPr() || properties.getRPr().sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getRPr().getPositionArray(0).getVal()).longValue();
    }
}
