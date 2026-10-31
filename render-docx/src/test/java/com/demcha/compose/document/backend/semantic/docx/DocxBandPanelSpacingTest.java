package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * A card under a drawn layer holds the page's space outside its table and its padding inside its
 * cell, not the padding in both.
 *
 * <p>A stack laying an icon over a card's corner is written as a band, and a band measures the
 * space it owes above its first written block and below its lowest to that block. Where a panel
 * holds the block, the distance runs through the panel's padding — which the panel writes again
 * as its cell's margins. Measured in Word 16: such a card of 14pt padding set its text 13.99pt
 * and the paragraph under it 28.02pt below where the page sets them, one of 20pt padding 19.99
 * and 40.02pt, one with a 10pt margin of its own 13.95 and 28.06pt, and one holding a further
 * panel of 6pt 13.99 and 34.02pt. The same card with no second layer, which is no band, stood
 * where the page sets it to 0.06pt. The space is now measured to the panel's box, and every one
 * of them stands within 0.06pt of the page.</p>
 *
 * <p>What a band owes between two of its own layers is not this space and is measured to the text
 * still ({@code DocxLayerColumns.band} hands the flow no {@code painted} test of its own): in the
 * document {@link #aPanelInAnotherLayerThanTheLowestBlockIsNotTheOneMeasuredTo} builds, the text
 * of the layer under the card stands 14pt below where the page sets it, the card's padding over
 * again. That is the same defect one layer down, left as it stands here and unchanged by this
 * test, which asserts the space under the band and not the resume above that text.</p>
 */
class DocxBandPanelSpacingTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INNER = DocumentColor.rgb(224, 233, 244);
    private static final double SPACING = 8;
    private static final double PADDING = 14;
    /** A second padding, so that what the space outside holds cannot be the padding by accident. */
    private static final double THICKER = 20;
    private static final long SPACING_TWIPS = Math.round(SPACING * 20);
    /** What a line of the default text is raised into the space above it, which it owes back below. */
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;
    private static final long PADDING_TWIPS = Math.round(PADDING * 20);
    /** A 10pt circle, drawn, laid over the card's top right corner. */
    private static final DocumentNode ICON =
            new com.demcha.compose.document.dsl.EllipseBuilder().size(10, 10).fillColor(SURFACE).build();

    @Test
    void aCardUnderADrawnLayerHoldsThePagesSpaceOutsideItsTable() throws Exception {
        try (XWPFDocument document = export(card(PADDING, DocumentInsets.zero()), true)) {
            assertThat(after(document.getParagraphs().get(0)))
                    .as("the page's space above the card, its padding not again")
                    .isEqualTo(SPACING_TWIPS);
            assertThat(before(last(document)))
                    .as("the page's space under the card, its padding not again, its own line's raise aside")
                    .isEqualTo(SPACING_TWIPS - RAISE);
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0)))
                    .as("the padding, held once, in the cell's margins, the first line's raise into it aside")
                    .containsExactly(PADDING_TWIPS - RAISE, PADDING_TWIPS);
        }
    }

    @Test
    void aThickerPaddingIsHeldInsideTheCellToo() throws Exception {
        // The space outside is the page's whatever the padding is: measured to the text, a 20pt
        // card stood 19.99pt low in Word and the paragraph under it 40.02pt.
        long padding = Math.round(THICKER * 20);
        try (XWPFDocument document = export(card(THICKER, DocumentInsets.zero()), true)) {
            assertThat(after(document.getParagraphs().get(0)))
                    .as("the page's space above the card, its thicker padding not again")
                    .isEqualTo(SPACING_TWIPS);
            assertThat(before(last(document)))
                    .as("and under it, its own line's raise aside")
                    .isEqualTo(SPACING_TWIPS - RAISE);
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0)))
                    .as("the thicker padding, held once, in the cell's margins")
                    .containsExactly(padding - RAISE, padding);
        }
    }

    @Test
    void aCardsOwnMarginIsStillHeldOutsideItsTable() throws Exception {
        // Its margin stands between the card's box and the block above it on the page, and the
        // table carries no space of its own: the space outside holds it, the padding still inside.
        try (XWPFDocument document = export(card(PADDING, DocumentInsets.of(10)), true)) {
            long outside = Math.round((SPACING + 10) * 20);
            assertThat(after(document.getParagraphs().get(0))).isEqualTo(outside);
            assertThat(before(last(document))).isEqualTo(outside - RAISE);
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0)))
                    .containsExactly(PADDING_TWIPS - RAISE, PADDING_TWIPS);
        }
    }

    @Test
    void aPanelInsideTheCardAddsNothingToTheSpaceOutsideIt() throws Exception {
        // The outermost panel is the one whose box the space is measured to: a note in a panel of
        // its own inside the card writes its own 6pt inside the card's cell, and the card's 14pt
        // inside the space the band owes would be both over again.
        DocumentNode card = new SectionBuilder().softPanel(SURFACE, 0, PADDING)
                .addParagraph(p -> p.text("A card under an icon."))
                .addSection(inner -> inner.softPanel(INNER, 0, 6)
                        .addParagraph(p -> p.text("An inner note in a panel of its own.")))
                .build();
        try (XWPFDocument document = export(card, true)) {
            assertThat(after(document.getParagraphs().get(0))).isEqualTo(SPACING_TWIPS);
            assertThat(before(last(document))).isEqualTo(SPACING_TWIPS - RAISE);
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0)))
                    .containsExactly(PADDING_TWIPS - RAISE, PADDING_TWIPS);
        }
    }

    @Test
    void aCardWithNoDrawnLayerBesideItIsNoBandAndStandsWhereItDid() throws Exception {
        // A stack of one layer lays nothing over anything and is no band: the space round it was
        // right before and is the same number now.
        try (XWPFDocument document = export(card(PADDING, DocumentInsets.zero()), false)) {
            assertThat(after(document.getParagraphs().get(0))).isEqualTo(SPACING_TWIPS);
            assertThat(before(last(document))).isEqualTo(SPACING_TWIPS - RAISE);
            assertThat(margins(document.getTables().get(0).getRow(0).getCell(0)))
                    .containsExactly(PADDING_TWIPS - RAISE, PADDING_TWIPS);
        }
    }

    @Test
    void aBandWhoseFirstBlockNoPanelHoldsIsStillMeasuredToThatBlock() throws Exception {
        // No panel writes anything above the text of an unpainted section, so the band owes the
        // whole distance from the stack's top to that text, as it always has.
        // Its first block carries a top margin of its own, which the block writes itself: the band
        // owes the distance to the block's box less that margin, and a sign slipped there stands
        // everything in the band twice the margin low.
        DocumentNode plain = new SectionBuilder().padding(DocumentInsets.of(PADDING))
                .addParagraph(p -> p.name("Card text").text("A card under an icon.")
                        .margin(DocumentInsets.top(7)))
                .build();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page.spacing(SPACING)
                    .addParagraph(p -> p.text("Before the card."))
                    .addLayerStack(stack -> stack.name("Stack").layer(plain).layer(ICON, LayerAlign.TOP_RIGHT))
                    .addParagraph(p -> p.text("After the card.")));
            PlacedNode stack = placed(session, "Stack");
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getTables()).as("an unpainted section is no panel").isEmpty();
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                PlacedNode text = placed(session, "Card text");
                double above = stack.placementY() + stack.placementHeight()
                               - (text.placementY() + text.placementHeight());
                // With no table to carry it, the space stands as the text's own space above it.
                assertThat(before(paragraphs.get(1)))
                        .as("the page's space above the stack and its distance to the text in it")
                        .isCloseTo(Math.round((SPACING + above) * 20) - RAISE,
                                offset(2L));
            }
        }
    }

    @Test
    void aCardWhoseOnlyTextIsInAPanelOfItsOwnIsMeasuredToTheCardsBox() throws Exception {
        // The outermost panel holding the block is the one measured to, and this is the shape that
        // tells the two apart: with no text of its own, every leaf the card holds lies inside the
        // inner panel, so taking the innermost would measure to that panel's box and leave the
        // card's own 14pt padding in the space outside the table, above and below.
        DocumentNode card = new SectionBuilder().softPanel(SURFACE, 0, PADDING)
                .addSection(inner -> inner.softPanel(INNER, 0, 6)
                        .addParagraph(p -> p.text("The card's only text, in a panel of its own.")))
                .build();
        try (XWPFDocument document = export(card, true)) {
            assertThat(after(document.getParagraphs().get(0)))
                    .as("the page's space above the card, neither padding again")
                    .isEqualTo(SPACING_TWIPS);
            assertThat(before(last(document)))
                    .as("and under it, its own line's raise aside")
                    .isEqualTo(SPACING_TWIPS - RAISE);
        }
    }

    @Test
    void aPaintedBarThatIsItselfTheBandsBlockIsMeasuredToItsOwnBox() throws Exception {
        // A card with no children of its own is the band's block, and writes its own padding as
        // its cell's margins just the same, so the space round it is measured to its box: in a
        // band it stands where a stack of one layer, which is no band, stands it. Measured to the
        // bar instead, the band form stood 10pt short above it and 8pt long below.
        DocumentNode bar = new SectionBuilder().softPanel(SURFACE, 0, 8)
                .margin(DocumentInsets.of(10)).build();
        try (XWPFDocument band = export(bar, true); XWPFDocument alone = export(bar, false)) {
            long outside = Math.round((SPACING + 10) * 20);
            assertThat(after(band.getParagraphs().get(0)))
                    .as("the page's space above the bar and its own margin, its padding not again")
                    .isEqualTo(outside);
            assertThat(before(last(band)))
                    .as("and the same under it")
                    .isEqualTo(outside - RAISE);
            assertThat(new long[]{after(band.getParagraphs().get(0)), before(last(band))})
                    .as("a band sets it where no band does")
                    .containsExactly(after(alone.getParagraphs().get(0)), before(last(alone)));
        }
    }

    @Test
    void aPanelInAnotherLayerThanTheLowestBlockIsNotTheOneMeasuredTo() throws Exception {
        // The panel has to hold the block: a card in one layer writes nothing above or below the
        // text of a plain layer standing under it, and the space below the band is the page's
        // distance from that text, as where no panel is in the stack at all.
        DocumentNode lower = new SectionBuilder().margin(DocumentInsets.top(60))
                .addParagraph(p -> p.name("Lower text").text("Under the card."))
                .build();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page.spacing(SPACING)
                    .addParagraph(p -> p.text("Before the card."))
                    .addLayerStack(stack -> stack.name("Stack")
                            .layer(card(PADDING, DocumentInsets.zero()))
                            .layer(lower, LayerAlign.TOP_LEFT))
                    .addParagraph(p -> p.text("After the card.")));
            PlacedNode stack = placed(session, "Stack");
            PlacedNode text = placed(session, "Lower text");
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                double below = text.placementY() - stack.placementY();
                // The lower layer's own last line was raised into the space above it and owes that
                // back below it, so what stands under the band is the page's space, whole.
                assertThat(before(last(document)))
                        .as("the page's distance from the lower layer's text to the stack's foot, "
                            + "and the page's space under the stack")
                        .isCloseTo(Math.round((SPACING + below) * 20),
                                offset(2L));
            }
        }
    }

    private static DocumentNode card(double padding, DocumentInsets margin) {
        return new SectionBuilder().softPanel(SURFACE, 0, padding).margin(margin)
                .addParagraph(p -> p.text("A card under an icon."))
                .build();
    }

    /** The card between a paragraph above and one under it, in a stack with the icon over it or alone. */
    private static XWPFDocument export(DocumentNode card, boolean underTheIcon) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page.spacing(SPACING)
                    .addParagraph(p -> p.text("Before the card."))
                    .addLayerStack(stack -> {
                        stack.layer(card);
                        if (underTheIcon) {
                            stack.layer(ICON, LayerAlign.TOP_RIGHT);
                        }
                    })
                    .addParagraph(p -> p.text("After the card.")));
            docx = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    private static PlacedNode placed(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(node -> name.equals(node.semanticName()) || name.equals(node.path()))
                .findFirst().orElseThrow();
    }

    /** The paragraph under the card: the last the body holds. */
    private static XWPFParagraph last(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.size() - 1);
    }

    /** The cell's top and bottom margins, in twips. */
    private static long[] margins(XWPFTableCell cell) {
        return new long[]{width(cell.getCTTc().getTcPr().getTcMar().getTop()),
                width(cell.getCTTc().getTcPr().getTcMar().getBottom())};
    }

    private static long width(CTTblWidth margin) {
        return margin == null || margin.getW() == null ? -1 : DocxTwips.of(margin.getW());
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties == null || properties.getSpacing() == null || properties.getSpacing().getAfter() == null
                ? 0 : DocxTwips.of(properties.getSpacing().getAfter());
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties == null || properties.getSpacing() == null || properties.getSpacing().getBefore() == null
                ? 0 : DocxTwips.of(properties.getSpacing().getBefore());
    }
}
