package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A block whose text a writer of its own writes — a list's items, two halves sharing one line —
 * carries the drawings of the blocks holding it.
 *
 * <p>A paragraph was offered to the shapes beside it only when a paragraph node was being
 * written, so a card whose only text was a list's items, and a CV entry's head band written as one
 * line, had no paragraph to offer: their shapes went to the text nearest them, in the body the
 * paragraph above the card, and stayed where that paragraph's top was while the card moved.
 * Measured in Word 16.0.20527 and LibreOffice 24.2: a paragraph above lengthened by a line moved
 * the card's text 9.72pt in Word and 19.40pt in LibreOffice and left the shape where it was; with
 * the item carrying it, the shape moves with the card (9.70 and 19.35pt) and stands on its corner,
 * 0.09pt from it in Word against 13.76pt before.</p>
 */
class DocxTextWriterAnchorTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 30, 40);
    private static final DocumentColor SURFACE = DocumentColor.rgb(230, 240, 250);
    private static final double DOT = 12;
    private static final double CARD_PADDING = 10;
    /** Where a band's dot is laid across it, from the band's left edge, in points. */
    private static final double DOT_ACROSS = 150;

    @Test
    void aCardWhoseTextIsAListsItemsCarriesTheDrawingLaidOverIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the card"));
            page.addLayerStack(stack -> stack
                    .layer(new SectionBuilder().softPanel(SURFACE, 0, 10)
                            .addList(list -> list.bullet().items("First item", "Second item")).build())
                    .layer(dot(), LayerAlign.TOP_RIGHT));
        })) {
            XWPFParagraph item = firstItem(document);

            assertThat(placedFrom(item)).as("in the first item's paragraph, not on the page")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(placedFrom(document.getParagraphs().get(0))).as("not in the paragraph above")
                    .isEmpty();
            assertThat(onThePage(document)).isZero();
            // The card is a table cell, so the dot is placed from the cell's text column, which
            // the item states by where its own text starts: the page lays the dot at the card's
            // right edge, so it stands the cell's text width less its own width into that column.
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            assertThat(offsetAcross(item)).as("from the cell's text column")
                    .isCloseTo(widthOf(card) - marginOf(card) - DOT, within(1.0));
        }
    }

    @Test
    void theFirstItemCarriesOneStandingBesideTheLast() throws Exception {
        // The holder's first paragraph on the page takes it, however far from its text it stands:
        // where it stands gives only its offset.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the card"));
            page.addLayerStack(stack -> stack
                    .layer(new SectionBuilder().softPanel(SURFACE, 0, 10)
                            .addList(list -> list.bullet().items("First item", "Second item")).build())
                    .layer(dot(), LayerAlign.BOTTOM_RIGHT));
        })) {
            XWPFParagraph item = firstItem(document);

            assertThat(placedFrom(item)).containsExactly(STRelFromV.PARAGRAPH);
            // The card holds two items of one line and 10pt of padding either side: the dot at its
            // foot stands below the first item's top by about the second item's line and the
            // padding under it, and no further than the card is tall.
            assertThat(offsetDown(item)).as("below the first item's top, where it stands")
                    .isBetween(9.0, 60.0);
        }
    }

    @Test
    void theItemWhoseTextItStandsBesideCarriesOneNoBlockHolds() throws Exception {
        // Each item is offered with its own text's box: a dot nothing holds, standing level with
        // the third item and clear of the items' text across the page, is anchored in that item.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addList(list -> list.bullet().items("First item", "Second item", "Third item",
                    "Fourth item"));
            page.addEllipse(dot -> dot.name("Dot").circle(10).fillColor(INK)
                    .margin(new DocumentInsets(-22, 0, 0, 250)));
        })) {
            List<XWPFParagraph> items = document.getParagraphs().stream()
                    .filter(para -> para.getCTP().getPPr() != null && para.getCTP().getPPr().isSetNumPr())
                    .toList();

            assertThat(items).hasSize(4);
            assertThat(placedFrom(items.get(2))).as("the third item, whose line it stands on")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(placedFrom(items.get(0))).as("not the first").isEmpty();
            assertThat(onThePage(document)).isZero();
        }
    }

    @Test
    void theLineTwoHalvesShareCarriesTheDrawingsOfTheirBand() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the band"));
            page.addContainer(band -> band.name("EntryHead").rectangle(360, 16)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .position(new ParagraphBuilder().name("Title").text("Delivery engineer")
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0, LayerAlign.CENTER_LEFT)
                    .position(new ParagraphBuilder().name("Dates").text("2019 - 2024").align(TextAlign.RIGHT)
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0, LayerAlign.CENTER_RIGHT)
                    .position(dot(), 160, 0, LayerAlign.CENTER_LEFT));
        })) {
            XWPFParagraph shared = shared(document);

            assertThat(shared.getText()).isEqualTo("Delivery engineer\t2019 - 2024");
            assertThat(placedFrom(shared)).as("in the line the halves share")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(onThePage(document)).isZero();
        }
    }

    @Test
    void theSharedLineCarriesOneNoBlockHoldsThatStandsBesideIt() throws Exception {
        // A dot no block carries — nothing holding it writes text — goes to the paragraph whose
        // text it stands nearest, counted from the text's edges. This one stands level with the
        // band at its right end, beside the dates, with a paragraph of body text just above it:
        // the line reaches across to the dates, so it is the nearer text. Offered as the box of
        // the half that starts the line — a title 60pt of a line 360pt wide — the line would be
        // reckoned 250pt away and the paragraph above would take the dot.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the band"));
            page.addContainer(band -> band.name("EntryHead").rectangle(360, 16)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .position(new ParagraphBuilder().name("Title").text("Engineer")
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0, LayerAlign.CENTER_LEFT)
                    .position(new ParagraphBuilder().name("Dates").text("2019 - 2024").align(TextAlign.RIGHT)
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0, LayerAlign.CENTER_RIGHT));
            page.addEllipse(dot -> dot.name("Dot").circle(12).fillColor(INK)
                    .margin(new DocumentInsets(-14, 0, 0, 290)));
        })) {
            XWPFParagraph shared = shared(document);

            assertThat(placedFrom(shared)).as("the dot, beside the dates")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(placedFrom(document.getParagraphs().get(0))).as("not the paragraph above")
                    .isEmpty();
            assertThat(onThePage(document)).isZero();
        }
    }

    @Test
    void anItemWhoseMarkerIsDrawnCarriesThemToo() throws Exception {
        // A list whose marker is drawn has no Word list definition to be given, so its items go
        // through the other item writer (writeRichListLine), which offers their place by the same
        // call and tabs their text past the marker.
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> card = page -> {
            page.addParagraph(p -> p.text("Above the card"));
            page.addLayerStack(stack -> stack
                    .layer(new SectionBuilder().softPanel(SURFACE, 0, CARD_PADDING)
                            .addList(list -> list.name("Skills").marker(m -> m.dot(4, INK))
                                    .markerGap(6).hangingIndent(true).normalizeMarkers(false)
                                    .addItem("First item").addItem("Second item")).build())
                    .layer(dot(), LayerAlign.TOP_RIGHT));
        };
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, card)) {
            XWPFTableCell panel = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph item = panel.getParagraphs().stream()
                    .filter(para -> para.getText().contains("First item")).findFirst().orElseThrow();

            assertThat(DocxExports.report(400, 600, 20, card).bySubject())
                    .as("laid out — without a layout nothing is drawn and the rest proves nothing")
                    .doesNotContainKey("measured geometry");
            assertThat(placedFrom(item)).as("in the first item, marker and all")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(offsetAcross(item)).as("from the cell's text column")
                    .isCloseTo(widthOf(panel) - marginOf(panel) - DOT, within(1.0));
            assertThat(onThePage(document)).isZero();
        }
    }

    @Test
    void aSharedLineStatesItsTextsOwnStartNotTheBoxItIsSetIn() throws Exception {
        // The line's indent is written from the inked start of the half that starts it, and a
        // shape anchored in a cell is placed from the cell's text column, which the place states
        // by its left edge less that indent. Here the band's title is centred in a box as wide as
        // the band, so its box starts a long way left of its text: stated as the box, the column
        // would come out short by that much and the cell would be given no shape to carry.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addSection(
                card -> card.softPanel(SURFACE, 0, 10).addContainer(band -> band.name("EntryHead")
                        .rectangle(320, 16).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Title").text("Engineer")
                                .align(TextAlign.CENTER)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Dates").text("2019 - 2024")
                                .align(TextAlign.RIGHT)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                LayerAlign.CENTER_RIGHT)
                        .position(dot(), DOT_ACROSS, 0, LayerAlign.CENTER_LEFT))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph shared = card.getParagraphs().stream()
                    .filter(para -> para.getText().contains("\t")).findFirst().orElseThrow();

            assertThat(placedFrom(shared)).as("the cell carries it")
                    .containsExactly(STRelFromV.PARAGRAPH);
            assertThat(offsetAcross(shared)).as("from the cell's text column, where the page draws it")
                    .isCloseTo(DOT_ACROSS, within(1.0));
        }
    }

    @Test
    void aSharedLineInACellPlacesItFromTheCellsTextColumn() throws Exception {
        // A shape anchored in a cell's paragraph is placed from the cell's text column, which the
        // seat states by where its own text starts. A band inside a card is such a line, and the
        // dot laid on it stands where the band places it, measured from that column.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addSection(
                card -> card.softPanel(SURFACE, 0, 10).addContainer(band -> band.name("EntryHead")
                        .rectangle(320, 16).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Title").text("Engineer")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Dates").text("2019 - 2024").align(TextAlign.RIGHT)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                LayerAlign.CENTER_RIGHT)
                        .position(dot(), 140, 0, LayerAlign.CENTER_LEFT))))) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph shared = card.getParagraphs().stream()
                    .filter(para -> para.getText().contains("\t")).findFirst().orElseThrow();

            assertThat(placedFrom(shared)).containsExactly(STRelFromV.PARAGRAPH);
            assertThat(offsetAcross(shared)).as("from the cell's text column, where the line starts")
                    .isCloseTo(140, within(1.0));
        }
    }

    @Test
    void aSharedLineHungPastItsCellsPaddingOffersNone() throws Exception {
        // Word draws no text past a cell's left edge, so a line pulled out past its card's
        // padding is written with no indent at all and its text does not start where the layout
        // puts it. The cell's text column cannot be read back from such a place, so the line
        // offers none and the drawing keeps to the text nearest it, as before this rule.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the card"));
            page.addSection(card -> card.softPanel(SURFACE, 0, CARD_PADDING)
                    .addContainer(band -> band.name("EntryHead").rectangle(320, 16)
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .margin(new DocumentInsets(0, 0, 0, -(CARD_PADDING + 2)))
                            .position(new ParagraphBuilder().name("Title").text("Engineer")
                                    .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                    LayerAlign.CENTER_LEFT)
                            .position(new ParagraphBuilder().name("Dates").text("2019 - 2024")
                                    .align(TextAlign.RIGHT)
                                    .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(), 0, 0,
                                    LayerAlign.CENTER_RIGHT)
                            .position(dot(), DOT_ACROSS, 0, LayerAlign.CENTER_LEFT)));
        })) {
            XWPFTableCell card = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph shared = card.getParagraphs().stream()
                    .filter(para -> para.getText().contains("\t")).findFirst().orElseThrow();

            assertThat(indentOf(shared)).as("written with no indent, hung out as it is").isZero();
            assertThat(placedFrom(shared)).as("the line takes none").isEmpty();
        }
    }

    @Test
    void aCardWhoseTextATablesCellsHoldLeavesItWhereItWas() throws Exception {
        // A table's own cells state no width of their own — a table's grid holds it — and such a
        // cell is set to the middle, whose text LibreOffice 24.2 moves for a drawing it carries
        // (measured for #977). So the cell is offered no drawing to carry, and the shape keeps to
        // the text nearest it, as before.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> {
            page.addParagraph(p -> p.text("Above the card"));
            page.addLayerStack(stack -> stack
                    .layer(new SectionBuilder().softPanel(SURFACE, 0, 10)
                            .addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                                    .rowCells(DocumentTableCell.text("A cell"))).build())
                    .layer(dot(), LayerAlign.TOP_RIGHT));
        })) {
            XWPFTableCell cellOfTheTable = document.getTables().get(0).getRow(0).getCell(0)
                    .getTables().get(0).getRow(0).getCell(0);

            assertThat(placedFrom(cellOfTheTable.getParagraphs().get(0))).as("the cell takes none")
                    .isEmpty();
            assertThat(placedFrom(document.getParagraphs().get(0)))
                    .as("the paragraph above the card keeps it, as before")
                    .containsExactly(STRelFromV.PARAGRAPH);
        }
    }

    private static DocumentNode dot() {
        return new EllipseNode("dot", DOT, DOT, INK, null, null, null, null, null);
    }

    /** The one Word paragraph two halves of a band share, found by the tab between them. */
    private static XWPFParagraph shared(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(para -> para.getText().contains("\t")).findFirst().orElseThrow();
    }

    private static XWPFParagraph firstItem(XWPFDocument document) {
        return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                .filter(para -> para.getCTP().getPPr() != null && para.getCTP().getPPr().isSetNumPr())
                .findFirst().orElseThrow();
    }

    private static List<STRelFromV.Enum> placedFrom(XWPFParagraph paragraph) {
        List<STRelFromV.Enum> from = new ArrayList<>();
        for (CTAnchor anchor : anchorsIn(paragraph.getCTP())) {
            from.add(anchor.getPositionV().getRelativeFrom());
        }
        return from;
    }

    private static double offsetDown(XWPFParagraph paragraph) {
        return anchorsIn(paragraph.getCTP()).get(0).getPositionV().getPosOffset() / 12700.0;
    }

    private static double offsetAcross(XWPFParagraph paragraph) {
        return anchorsIn(paragraph.getCTP()).get(0).getPositionH().getPosOffset() / 12700.0;
    }

    /** The left indent written on a paragraph, in points; 0 where none is written. */
    private static double indentOf(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetInd() || !properties.getInd().isSetLeft()) {
            return 0;
        }
        return ((Number) properties.getInd().getLeft()).doubleValue() / 20;
    }

    /** A cell's width as written, in points. */
    private static double widthOf(XWPFTableCell cell) {
        return ((Number) cell.getCTTc().getTcPr().getTcW().getW()).doubleValue() / 20;
    }

    /** The room a cell leaves its text at the left, as written, in points. */
    private static double marginOf(XWPFTableCell cell) {
        return ((Number) cell.getCTTc().getTcPr().getTcMar().getLeft().getW()).doubleValue() / 20;
    }

    private static long onThePage(XWPFDocument document) {
        return anchorsIn(document.getDocument()).stream()
                .filter(anchor -> anchor.getPositionV().getRelativeFrom() == STRelFromV.PAGE)
                .count();
    }

    private static List<CTAnchor> anchorsIn(XmlObject root) {
        List<CTAnchor> anchors = new ArrayList<>();
        try (org.apache.xmlbeans.XmlCursor cursor = root.newCursor()) {
            cursor.selectPath("declare namespace wp='http://schemas.openxmlformats.org/drawingml/2006/"
                              + "wordprocessingDrawing' .//wp:anchor");
            while (cursor.toNextSelection()) {
                if (cursor.getObject() instanceof CTAnchor anchor) {
                    anchors.add(anchor);
                }
            }
        }
        return anchors;
    }
}
