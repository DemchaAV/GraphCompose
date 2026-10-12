package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A Word list's item Word sets high is lowered by the space above it, its marker with its text.
 *
 * <p>Word sets a Word list's marker from the paragraph's mark, and LibreOffice does not move it by
 * the mark's position: lowered by its position, the text would stand off its bullet there. The
 * space above moves both, in both editors; the items under it, and what follows the list, give it
 * back from the space above them.</p>
 */
class DocxListItemLowerTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;

    @Test
    void anItemWhoseMarkerWordDrawsIsLoweredByTheSpaceAboveItAndWhatFollowsGivesItBack() throws Exception {
        // Lato's 24pt line stands 0.65pt high in Word: the space above lowers it, marker and text,
        // and the paragraph under the list takes as much out of the space above it.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph item = paragraph(exported, "Heading");
        long lower = Math.round(-(0.8 * line(item) - LATO_ASCENT * 24) * 20);

        assertThat(item.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
        assertThat(lower).as("the premise: Word's baseline is the higher, by more than half a point")
                .isGreaterThan(10);
        assertThat(before(item)).isEqualTo(200 + lower);
        assertThat(position(item)).isZero();
        assertThat(markPosition(item)).isZero();
        assertThat(before(paragraph(exported, "Body"))).as("the space above it, less what the item took")
                .isEqualTo(200 - lower);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void theItemsUnderAnItemLoweredGiveItBackFromTheSpaceAboveThem() throws Exception {
        // Each item stands as high: the second and third are lowered by what the one above them
        // owes, their space above as written; the paragraph under the list gives back the last's.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).itemSpacing(10).margin(DocumentInsets.top(10))
                        .items("One", "Two", "Three"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph one = paragraph(exported, "One");
        long lower = Math.round(-(0.8 * line(one) - LATO_ASCENT * 24) * 20);

        assertThat(before(one)).isEqualTo(200 + lower);
        assertThat(before(paragraph(exported, "Two"))).isEqualTo(200);
        assertThat(before(paragraph(exported, "Three"))).isEqualTo(200);
        assertThat(before(paragraph(exported, "Body"))).isEqualTo(200 - lower);
    }

    @Test
    void itemsWithNoSpaceAboveThemToGiveItBackTakeItBackByTheirOwnLowering() throws Exception {
        // Items of two lines 3pt apart, 3pt apart themselves: the lines take the whole gap above
        // each, and stand high. The first is lowered by its space above; the others, with none to
        // give, stand as low as the one above leaves them, which is their own lowering.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(10)).lineSpacing(3).itemSpacing(3).margin(DocumentInsets.top(10))
                        .items("One\nmore", "Two\nmore", "Three\nmore"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph one = starting(exported, "One");
        long lowered = before(one) - (10 - 3) * 20;

        assertThat(lowered).as("the 3pt the lines took above the page's, and the face's own seat")
                .isCloseTo(highTakingTheGap(one, 3, LATO_ASCENT * 10), within(1L)).isGreaterThan(10);
        assertThat(before(starting(exported, "Two"))).isZero();
        assertThat(before(starting(exported, "Three"))).isZero();
        assertThat(before(paragraph(exported, "Body"))).isEqualTo(200 - lowered);
    }

    @Test
    void anItemUnderOneLoweredGivesItBackBeforeItRises() throws Exception {
        // Spectral's 10pt line stands 1.6pt low in Word, and an item of two lines 15pt apart stands
        // high: the lines take 12pt of their gap from the space above them. The one-line item under
        // it gives back what the first owes, then rises into what is left of the gap above it.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(spectral(10)).lineSpacing(15).itemSpacing(20)
                        .margin(DocumentInsets.top(20)).items("First\nSecond", "Third"))
                .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph first = exported.document().getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().contains("First")).findFirst().orElseThrow();
        XWPFParagraph third = paragraph(exported, "Third");
        long lowered = before(first) - (20 - 15) * 20;
        long raise = Math.round((0.8 * line(third) - SPECTRAL_ASCENT * 10) * 20);

        assertThat(lowered).as("the item of two lines, lowered")
                .isCloseTo(highTakingTheGap(first, 15, SPECTRAL_ASCENT * 10), within(1L)).isGreaterThan(10);
        assertThat(raise).as("the premise: the one-line item stands low").isGreaterThan(10);
        assertThat(before(third)).as("the gap, less what the first owed and the raise")
                .isEqualTo(20 * 20 - lowered - raise);
        assertThat(after(third)).as("what it rose, owed below it").isZero();
        assertThat(before(paragraph(exported, "Body"))).as("the space above it and the raise").isEqualTo(200 + raise);
    }

    @Test
    void whatTheSpaceAboveWhatFollowsCannotGiveBackIsNamed() throws Exception {
        // Nothing stands between the list and the paragraph under it: lowered, the item stands that
        // paragraph lower too, and the report says so.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.name("Duties").textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                .addParagraph(p -> p.name("Body").text("Body").textStyle(lato(10))));
        XWPFParagraph item = paragraph(exported, "Heading");
        long lower = Math.round(-(0.8 * line(item) - LATO_ASCENT * 24) * 20);

        assertThat(before(item)).isEqualTo(200 + lower);
        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Body");
            assertThat(note.detail()).contains("does not hold the line hanging below the block above it");
        });
    }

    @Test
    void anItemLoweredAtTheEndOfACellTakesItOutOfTheSpaceTheCellOwesUnderIt() throws Exception {
        Exported exported = export(page -> page
                .addParagraph("Before")
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                        .padding(DocumentInsets.of(8)).addParagraph("Title").addList(list -> list.textStyle(lato(24))
                                .margin(new DocumentInsets(10, 0, 4, 0)).items("Inside"))));
        XWPFTableCell panel = exported.document().getTables().get(0).getRow(0).getCell(0);
        XWPFParagraph inside = panel.getParagraphs().get(1);
        long lower = Math.round(-(0.8 * line(inside) - LATO_ASCENT * 24) * 20);

        assertThat(inside.getText()).as("the premise: the card ends with the item").isEqualTo("Inside");
        assertThat(lower).as("the premise: Word's baseline is the higher").isGreaterThan(10);
        assertThat(before(inside)).as("the list's 10pt, what the title rose and owes, and the lowering")
                .isEqualTo(200 + DocxExports.DEFAULT_LINE_RAISE + lower);
        assertThat(after(inside)).as("the list's 4pt below it, less what the item took").isEqualTo(4 * 20 - lower);
        assertThat(bottomMargin(panel)).as("its padding").isEqualTo(8 * 20);
    }

    @Test
    void anItemLoweredAtTheEndOfACardTakesItOutOfTheCardsPaddingBelow() throws Exception {
        // Nothing stands under the item in the card: what it hangs below the cell the card's
        // padding takes, as it takes any line hanging there.
        Exported exported = export(page -> page
                .addParagraph("Before")
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                        .padding(DocumentInsets.of(8)).addParagraph("Title")
                        .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Inside"))));
        XWPFTableCell panel = exported.document().getTables().get(0).getRow(0).getCell(0);
        XWPFParagraph inside = panel.getParagraphs().get(1);
        long lower = Math.round(-(0.8 * line(inside) - LATO_ASCENT * 24) * 20);

        assertThat(lower).as("the premise: Word's baseline is the higher").isGreaterThan(10);
        assertThat(before(inside)).as("the list's 10pt, what the title rose and owes, and the lowering")
                .isEqualTo(200 + DocxExports.DEFAULT_LINE_RAISE + lower);
        assertThat(bottomMargin(panel)).as("its padding, less what the item took").isEqualTo(8 * 20 - lower);
    }

    @Test
    void anItemOpeningACardIsNotLowered() throws Exception {
        // A card's top border is taken inside the space above its first line, which a lowering
        // would then be shrunk back out of.
        Exported exported = export(page -> page
                .addParagraph("Before")
                .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                        .padding(DocumentInsets.of(8)).addList(list -> list.textStyle(lato(24)).items("Inside"))));
        XWPFTableCell panel = exported.document().getTables().get(0).getRow(0).getCell(0);
        XWPFParagraph inside = panel.getParagraphs().get(0);

        assertThat(inside.getText()).as("the premise: the item opens the card").isEqualTo("Inside");
        assertThat(-(0.8 * line(inside) - LATO_ASCENT * 24)).as("the premise: Word's baseline is the higher")
                .isGreaterThan(0.5);
        assertThat(before(inside)).isZero();
        assertThat(bottomMargin(panel)).isEqualTo(8 * 20);
    }

    @Test
    void anItemLoweredAtTheEndOfTheDocumentIsTakenBack() throws Exception {
        // Nothing under it gives it back, and the last page would run that much longer.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).itemSpacing(10).margin(DocumentInsets.top(10))
                        .items("One", "Two")));
        long lower = Math.round(-(0.8 * line(paragraph(exported, "One")) - LATO_ASCENT * 24) * 20);

        assertThat(before(paragraph(exported, "One"))).as("lowered, given back by the second").isEqualTo(200 + lower);
        assertThat(before(paragraph(exported, "Two"))).as("the gap, less what the first took").isEqualTo(200 - lower);
    }

    @Test
    void theLastItemOfAListEndingATablesComposedCellIsTakenBack() throws Exception {
        // The first item is lowered, and the second gives that back; the second's own lowering
        // nothing under it in the cell gives back.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(new ListBuilder()
                        .textStyle(lato(24)).itemSpacing(10).items("One", "Two").build()))))) {
            XWPFParagraph one = inACell(document, "One");
            long lower = Math.round(-(0.8 * line(one) - LATO_ASCENT * 24) * 20);

            assertThat(one.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
            assertThat(lower).as("the premise: Word's baseline is the higher").isGreaterThan(10);
            assertThat(before(one)).isEqualTo(lower);
            assertThat(before(inACell(document, "Two"))).as("the gap, less what the first took").isEqualTo(200 - lower);
        }
    }

    @Test
    void anItemLoweredAboveAParagraphTheLayoutOpensAPageWithIsTakenBack() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                    .spacer(1, 480)
                    .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                    .addParagraph(p -> p.name("Next").text("Next").textStyle(lato(10)).margin(DocumentInsets.top(30))));
            assertThat(startPage(session, "Next")).as("the premise: the paragraph under the list opens the next page")
                    .isEqualTo(1);
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                assertThat(before(paragraph(document, "Heading")))
                        .as("the spacer's height past its hairline and the list's 10pt, as written")
                        .isCloseTo(Math.round((480 - 0.1 + 10) * 20), within(1L));
            }
        }
    }

    @Test
    void anItemLoweredIntoTheSpaceAboveAParagraphStaysLoweredWhereALaterBlockOpensAPage() throws Exception {
        // The paragraph under the list gave its lowering back: a block after it that opens the
        // next page has nothing of it to take back.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                    .spacer(1, 440)
                    .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                    .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10)))
                    .addParagraph(p -> p.name("Next").text("Next").textStyle(lato(10)).margin(DocumentInsets.top(50))));
            assertThat(startPage(session, "Next")).as("the premise: the last paragraph opens the next page").isEqualTo(1);
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph item = paragraph(document, "Heading");
                long lower = Math.round(-(0.8 * line(item) - LATO_ASCENT * 24) * 20);

                assertThat(before(item)).isCloseTo(Math.round((440 - 0.1 + 10) * 20) + lower, within(1L));
                assertThat(before(paragraph(document, "Body"))).isEqualTo(200 - lower);
            }
        }
    }

    @Test
    void anItemLoweredAtTheEndOfARowsCellIsTakenBackWhereTheBlockUnderTheRowOpensAPage() throws Exception {
        // The row hangs what the item was lowered by below itself. A paragraph under it on the same
        // page gives that back; one opening the next page, where Word sets its line at the top, does not.
        Consumer<SectionBuilder> lowered = column -> column.add(new ListBuilder().textStyle(lato(24))
                .margin(DocumentInsets.top(10)).items("Heading").build());
        // Lato's line, a quarter point high, lifts the row out of nothing.
        Consumer<SectionBuilder> side = column -> column.addParagraph(p -> p.text("Side").textStyle(lato(10)));
        assertThat(headingAbove(10, page -> page.addRow(row -> row.addSection(lowered).addSection(side))))
                .as("lowered, under a paragraph on its page").isEqualTo(200 + latoLowering());
        assertThat(headingAbove(50, page -> page.addRow(row -> row.addSection(lowered).addSection(side))))
                .as("its space above, as written").isEqualTo(200);
        // A taller column beside it leaves room under its cell in the row, which takes the hang.
        Consumer<SectionBuilder> tall = column -> {
            for (String line : List.of("One", "Two", "Three", "Four")) {
                column.addParagraph(p -> p.text(line).textStyle(lato(10)));
            }
        };
        assertThat(headingAbove(50, page -> page.addRow(row -> row.addSection(lowered).addSection(tall))))
                .as("lowered, the room under its cell giving it back").isEqualTo(200 + latoLowering());
    }

    @Test
    void anItemLoweredAtTheEndOfACardWithNoPaddingIsTakenBackWhereTheBlockUnderItOpensAPage() throws Exception {
        Consumer<PageFlowBuilder> card = page -> page.addSection("Card", section -> section
                .fillColor(DocumentColor.rgb(238, 243, 249)).addParagraph("Title")
                .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading")));
        assertThat(headingAbove(10, card)).as("lowered, under a paragraph on its page").isEqualTo(200 + latoLowering());
        assertThat(headingAbove(50, card)).as("its space above, as written").isEqualTo(200);
    }

    @Test
    void theItemsOfListsSideBySideInARowAreEachTakenBack() throws Exception {
        // Each column's list hangs its own lowering below the row: the row's hang let go, both are
        // taken back, not as one run of the two.
        Consumer<PageFlowBuilder> row = page -> page.addRow(r -> r
                .addSection(column -> column.add(new ListBuilder().textStyle(lato(24))
                        .margin(DocumentInsets.top(10)).items("Heading").build()))
                .addSection(column -> column.add(new ListBuilder().textStyle(lato(24))
                        .margin(DocumentInsets.top(10)).items("Beside").build())));
        assertThat(headingAbove(10, row)).as("lowered, under a paragraph on its page").isEqualTo(200 + latoLowering());
        assertThat(headingAbove(50, row)).as("its space above, as written").isEqualTo(200);
        assertThat(paragraphAbove(50, row, "Beside")).as("the one beside it, as written").isEqualTo(200);
    }

    @Test
    void anItemLoweredAboveAListTheLayoutOpensAPageWithIsTakenBack() throws Exception {
        assertThat(flowItemAbove(page -> page.addList(list -> list.name("Next").textStyle(lato(10))
                .margin(DocumentInsets.top(10)).items("Next item")))).isEqualTo(200 + latoLowering());
        assertThat(flowItemAbove(page -> page.addList(list -> list.name("Next").textStyle(lato(10))
                .margin(DocumentInsets.top(50)).items("Next item")))).isEqualTo(200);
    }

    @Test
    void aBlockOnALaterPageTakesNothingOfTheHangTheListAboveLeft() throws Exception {
        // The list under it opens the next page: its space above is written whole, and nothing of
        // a hang taken back is named.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10))).spacer(1, 460)
                .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                .addList(list -> list.textStyle(lato(10)).margin(DocumentInsets.top(50)).items("Next item")));

        assertThat(before(paragraph(exported, "Next item"))).as("its 50pt, whole").isEqualTo(50 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void anItemLoweredAboveAParagraphOpeningAPageInAStackOfOneLayerIsTakenBack() throws Exception {
        assertThat(flowItemAbove(page -> page.addLayerStack(stack -> stack.layer(new com.demcha.compose.document.dsl
                .ParagraphBuilder().name("Next").text("Next").textStyle(lato(10)).margin(DocumentInsets.top(10))
                .build(), LayerAlign.TOP_LEFT)))).isEqualTo(200 + latoLowering());
        assertThat(flowItemAbove(page -> page.addLayerStack(stack -> stack.layer(new com.demcha.compose.document.dsl
                .ParagraphBuilder().name("Next").text("Next").textStyle(lato(10)).margin(DocumentInsets.top(50))
                .build(), LayerAlign.TOP_LEFT)))).isEqualTo(200);
    }

    @Test
    void anItemLoweredAboveABandKeepsWhatTheBandsSpaceAboveTakesAndTheRestIsTakenBack() throws Exception {
        // A band resumes the space the page measures above its first block, the hang taken out of
        // it; with no space above the band, nothing gives the lowering back.
        assertThat(aboveABand(10)).as("lowered, the band's 10pt above taking it").isEqualTo(200 + latoLowering());
        assertThat(aboveABand(0)).as("its space above, as written").isEqualTo(200);
    }

    private static long aboveABand(double gap) throws Exception {
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"))
                .addLayerStack(stack -> stack.name("Band").margin(DocumentInsets.top(gap))
                        .back(new com.demcha.compose.document.node.SpacerNode("Room", 200, 40,
                                DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().name("Next").text("Next")
                                .build(), LayerAlign.TOP_LEFT)));
        return before(paragraph(exported, "Heading"));
    }

    /** The space above the item "Heading" of a list near a page's foot, {@code next} written under it. */
    private static long flowItemAbove(Consumer<PageFlowBuilder> next) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Lead").textStyle(lato(10))).spacer(1, 460)
                        .addList(list -> list.textStyle(lato(24)).margin(DocumentInsets.top(10)).items("Heading"));
                next.accept(page);
            });
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return before(paragraph(document, "Heading")) - Math.round((460 - 0.1) * 20);
            }
        }
    }

    private static long paragraphAbove(double nextAbove, Consumer<PageFlowBuilder> block, String text) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Lead").textStyle(lato(10))).spacer(1, 460);
                block.accept(page);
                page.addParagraph(p -> p.name("Next").text("Next").textStyle(lato(10))
                        .margin(DocumentInsets.top(nextAbove)));
            });
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return before(inACell(document, text));
            }
        }
    }

    /**
     * The space above the cell paragraph "Heading" a block near a page's foot ends with, a
     * paragraph {@code nextAbove} points under it; at 50 that paragraph opens the next page.
     */
    private static long headingAbove(double nextAbove, Consumer<PageFlowBuilder> block) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Lead").textStyle(lato(10))).spacer(1, 460);
                block.accept(page);
                page.addParagraph(p -> p.name("Next").text("Next").textStyle(lato(10))
                        .margin(DocumentInsets.top(nextAbove)));
            });
            assertThat(startPage(session, "Next")).as("the premise: where the paragraph under the block starts")
                    .isEqualTo(nextAbove > 30 ? 1 : 0);
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return before(inACell(document, "Heading"));
            }
        }
    }

    /** How far Word sets a line of Lato at 24pt above the page's, in twips. */
    private static long latoLowering() {
        return Math.round(-(0.8 * 28.8 - LATO_ASCENT * 24) * 20);
    }

    @Test
    void anItemLoweredAboveATableTakesItOutOfTheSpaceAboveTheTable() throws Exception {
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(24)).margin(new DocumentInsets(10, 0, 10, 0)).items("Heading"))
                .addTable(t -> t.columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200))
                        .row("Cell")));
        XWPFParagraph item = paragraph(exported, "Heading");
        long lower = Math.round(-(0.8 * line(item) - LATO_ASCENT * 24) * 20);

        assertThat(before(item)).isEqualTo(200 + lower);
        assertThat(after(item)).as("the list's 10pt below it, less what the item took").isEqualTo(200 - lower);
    }

    @Test
    void anItemWordSetsHighByLessThanHalfAPointIsNotLowered() throws Exception {
        // Lato's 10pt line stands a quarter point high in Word: where its position would move it
        // nothing, the space above does not either.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addList(list -> list.textStyle(lato(10)).margin(DocumentInsets.top(10)).items("Item")));
        XWPFParagraph item = paragraph(exported, "Item");
        double high = -(0.8 * line(item) - LATO_ASCENT * 10);

        assertThat(high).as("the premise: high by under half a point").isBetween(0.1, 0.5);
        assertThat(before(item)).isEqualTo(200);
    }

    @Test
    void anItemTheLayoutSetsOnANewPageIsNotLoweredAndTheItemsAboveItAreTakenBack() throws Exception {
        // Word drops a paragraph's space above at the top of a page, and sets the page's first line
        // at its top: nothing on the page above gives back what the first item was lowered by.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 500)
                    .addList(list -> list.name("Duties").textStyle(lato(24)).itemSpacing(10)
                            .margin(DocumentInsets.top(10)).items("One", "Two", "Three"))
                    .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph two = paragraph(document, "Two");
                long lower = Math.round(-(0.8 * line(two) - LATO_ASCENT * 24) * 20);

                assertThat(pages).as("the premise: the second item opens the next page").containsExactly(0, 1, 1);
                assertThat(before(paragraph(document, "One")))
                        .as("the spacer's height past its hairline and the list's 10pt, as written")
                        .isCloseTo(Math.round((500 - 0.1 + 10) * 20), within(1L));
                assertThat(before(two)).as("the gap, as written").isEqualTo(200);
                assertThat(before(paragraph(document, "Three"))).as("the gap and its own lowering").isEqualTo(200 + lower);
            }
        }
    }

    @Test
    void theItemsOfAListTheLayoutSetsOverTwoPagesInACellKeepTheSpaceAboveThem() throws Exception {
        // A card the page breaks is one cell going on over the next page: what an item owed would be
        // the space above the one opening that page, inside the cell.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 460)
                    .addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                            .addList(list -> list.name("Duties").textStyle(lato(24)).itemSpacing(10)
                                    .margin(DocumentInsets.top(10)).items("One", "Two", "Three"))));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                assertThat(pages).as("the premise: the list goes on over the next page").contains(0, 1);
                assertThat(document.getTables()).as("the premise: one cell over both pages").hasSize(1);
                for (String text : List.of("One", "Two", "Three")) {
                    assertThat(before(inACell(document, text))).as("the space above %s, as written", text).isEqualTo(200);
                }
            }
        }
    }

    @Test
    void anItemInAStackOfOneLayerIsLoweredAsInTheFlow() throws Exception {
        // A template wraps an entry in a stack of one layer, which lays nothing over anything.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addLayerStack(stack -> stack.name("Entry")
                        .layer(new ListBuilder().textStyle(lato(24)).margin(DocumentInsets.top(10))
                                .items("Heading").build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph item = paragraph(exported, "Heading");
        long lower = Math.round(-(0.8 * line(item) - LATO_ASCENT * 24) * 20);

        assertThat(lower).as("the premise: Word's baseline is the higher").isGreaterThan(10);
        assertThat(before(item)).isEqualTo(200 + lower);
        assertThat(before(paragraph(exported, "Body"))).isEqualTo(200 - lower);
    }

    @Test
    void theFirstItemOfAStackOfOneLayerTheLayoutMovesToANewPageIsNotLowered() throws Exception {
        // Word drops the space above the page's first line, so lowered by it the first item would
        // stand as high as before, and the second, giving back what the first owed, as high too.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 500)
                    .addLayerStack(stack -> stack.name("Entry")
                            .layer(new ListBuilder().name("Duties").textStyle(lato(24)).itemSpacing(10)
                                    .margin(DocumentInsets.top(10)).items("One", "Two").build(), LayerAlign.TOP_LEFT))
                    .addParagraph(p -> p.text("Body").textStyle(lato(10)).margin(DocumentInsets.top(10))));
            List<Integer> pages = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("Duties"))
                    .map(PlacedFragment::pageIndex).toList();
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph two = paragraph(document, "Two");
                long lower = Math.round(-(0.8 * line(two) - LATO_ASCENT * 24) * 20);

                assertThat(pages).as("the premise: the stack's list opens the next page").containsExactly(1, 1);
                assertThat(before(paragraph(document, "One")))
                        .as("the spacer's height past its hairline and the list's 10pt, as written")
                        .isCloseTo(Math.round((500 - 0.1 + 10) * 20), within(1L));
                assertThat(before(two)).as("the gap and its own lowering, nothing owed above it").isEqualTo(200 + lower);
            }
        }
    }

    @Test
    void anItemInAStackOverADrawingKeepsTheSpaceAboveIt() throws Exception {
        // A stack laying its list over a shape measures what follows it from the page: what an item
        // owed below it would not reach what follows.
        Exported exported = export(page -> page
                .addParagraph(p -> p.text("Above").textStyle(lato(10)))
                .addLayerStack(stack -> stack.name("Stack")
                        .back(new ShapeBuilder().name("Backdrop").size(200, 80).build())
                        .layer(new ListBuilder().textStyle(lato(24)).margin(DocumentInsets.top(10))
                                .items("Heading").build(), LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.text("After").textStyle(lato(10)).margin(DocumentInsets.top(10))));
        XWPFParagraph item = anywhere(exported.document(), "Heading");

        assertThat(item.getCTP().getPPr().isSetNumPr()).as("the premise: a Word list's item").isTrue();
        assertThat(0.8 * line(item) - LATO_ASCENT * 24).as("the premise: Word's baseline is the higher")
                .isLessThan(-0.5);
        assertThat(before(item)).as("nothing grown above it").isZero();
        assertThat(position(item)).isZero();
    }

    /**
     * How far Word sets an item of two lines high, in twips, its lines taking the whole gap
     * between them from the space above: Word's lines are that gap taller than the page's, and it
     * seats their middle four fifths of the way down, where the page sets the first line its
     * ascent below the top it took the gap above.
     */
    private static long highTakingTheGap(XWPFParagraph item, double gap, double ascent) {
        double line = line(item);
        return Math.round((gap + ascent - 0.8 * line) * 20);
    }

    private static DocumentTextStyle spectral(double size) {
        return DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(size).build();
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    private record Exported(XWPFDocument document, DocxExportReport report) {
    }

    private static int startPage(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow().startPage();
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    private static XWPFParagraph paragraph(Exported exported, String text) {
        return paragraph(exported.document(), text);
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph starting(Exported exported, String text) {
        return exported.document().getParagraphs().stream()
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

    private static long bottomMargin(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return properties == null || !properties.isSetTcMar() || !properties.getTcMar().isSetBottom() ? 0
                : DocxTwips.of(properties.getTcMar().getBottom().getW());
    }

    /** The {@code w:position} of a paragraph's first run, in half points; 0 for none. */
    private static long position(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getRArray(0).getRPr();
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
