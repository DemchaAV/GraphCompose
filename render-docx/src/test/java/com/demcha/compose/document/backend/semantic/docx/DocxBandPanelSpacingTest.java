package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
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
import java.util.function.Consumer;

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
 * <p>What a band owes between two of its own layers — a later layer's resume — is measured to a
 * panel's box by the same rule: below the panel the layers before it end in, and above the panel
 * it opens with. Measured to the text, it held the padding over again one layer down: in Word 16
 * a caption laid under a panel of 6pt padding stood 6.0pt low, under one of 12pt 12.0pt, under one
 * padded 10pt below alone 10.0pt, and a panel written as a later layer stood its 6pt top padding
 * low, in a band and in a column's cell alike. Measured to the box, each stands within 0.1pt of
 * the page.</p>
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
                // The gap the lower layer resumes with is measured to the card's box, not through
                // its padding, which the card writes itself: measured to the card's text, the
                // lower layer's text stood the card's padding low.
                PlacedNode card = placed(session, "Card");
                double gap = card.placementY() - (text.placementY() + text.placementHeight());
                assertThat(before(middle(document)))
                        .as("the page's gap from the card's box to the lower layer's text, the card's "
                            + "%spt padding not again", PADDING)
                        .isCloseTo(Math.round(gap * 20) - RAISE, offset(2L));
            }
        }
    }

    @Test
    void aCaptionUnderAPanelResumesBelowThePanelsBox() throws Exception {
        // A panel padded 10pt below its text and not above it, so that what the gap leaves out
        // can only be the padding below. Measured to the text, the caption stood 10.0pt low in
        // Word 16; padded above alone, it stood where the page sets it.
        DocumentNode panel = new SectionBuilder().name("Panel").fillColor(SURFACE)
                .padding(new DocumentInsets(0, 0, BOTTOM_ONLY, 0))
                .addParagraph(p -> p.text("A panel padded below its text."))
                .build();
        try (DocumentSession session = layered(panel, captionAt(40))) {
            PlacedNode box = placed(session, "Panel");
            PlacedNode caption = placed(session, "Caption");
            double gap = box.placementY() - (caption.placementY() + caption.placementHeight());
            try (XWPFDocument document = docx(session)) {
                assertThat(before(paragraph(document, "Caption")))
                        .as("the page's gap from the panel's box to the caption's, its 40pt margin "
                            + "included, the panel's %spt padding below not again", BOTTOM_ONLY)
                        .isCloseTo(Math.round(gap * 20) - RAISE, offset(2L));
            }
        }
    }

    @Test
    void aPanelOpeningALaterLayerResumesAboveItsBox() throws Exception {
        // The panel writes its 6pt padding above its text, and its text a 5pt margin of its own
        // inside it: the gap the layer resumes with ends at the panel's box, and neither is in it.
        // Measured to the text, the panel stood its padding low in Word 16, 6.0pt.
        DocumentNode panel = new SectionBuilder().name("Panel").fillColor(SURFACE)
                .padding(DocumentInsets.of(6)).margin(DocumentInsets.top(20))
                .addParagraph(p -> p.text("A panel under the head.").margin(DocumentInsets.top(5)))
                .build();
        DocumentNode head = new ParagraphBuilder().name("Head").text("The head.").build();
        try (DocumentSession session = layered(head, panel)) {
            double gap = gapAbove(session, "Head", "Panel");
            try (XWPFDocument document = docx(session)) {
                assertThat(after(paragraph(document, "The head.")))
                        .as("the page's gap from the head to the panel's box, its padding and its "
                            + "text's margin inside it not in it")
                        .isCloseTo(Math.round(gap * 20) + RAISE, offset(2L));
            }
        }
    }

    @Test
    void aPanelOpeningALaterLayerInAColumnsCellResumesAboveItsBox() throws Exception {
        // The same rule in a stack written as columns: the later layer of the right column opens
        // with a panel, which stood its 6pt top padding low in Word 16 measured to its text.
        DocumentNode panel = new SectionBuilder().name("Panel").fillColor(SURFACE)
                .padding(DocumentInsets.of(6))
                .addParagraph(p -> p.text("A panel in the column."))
                .build();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 500)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page.spacing(SPACING)
                    .addParagraph(p -> p.text("Before the stack."))
                    .addLayerStack(stack -> stack.name("Stack")
                            .layer(column(0, 200, 0, new ParagraphBuilder().text("Side").build()))
                            .layer(column(160, 0, 0, new ParagraphBuilder().name("Head").text("The head.").build()))
                            .layer(column(160, 0, 40, panel)))
                    .addParagraph(p -> p.text("After the stack.")));
            double gap = gapAbove(session, "Head", "Panel");
            try (XWPFDocument document = docx(session)) {
                assertThat(document.getTables()).as("written as columns").isNotEmpty();
                assertThat(after(paragraph(document, "The head.")))
                        .as("the page's gap from the head to the panel's box, its padding not in it")
                        .isCloseTo(Math.round(gap * 20), offset(2L));
            }
        }
    }

    @Test
    void aCaptionUnderAnUnpaintedSectionResumesBelowItsText() throws Exception {
        // No panel writes the padding of an unpainted section: what stands between its text and
        // the caption is the gap, the padding below the text included, as it always was.
        DocumentNode section = new SectionBuilder().padding(new DocumentInsets(0, 0, BOTTOM_ONLY, 0))
                .addParagraph(p -> p.name("Text").text("An unpainted section."))
                .build();
        try (DocumentSession session = layered(section, captionAt(40))) {
            double gap = gapAbove(session, "Text", "Caption");
            try (XWPFDocument document = docx(session)) {
                assertThat(document.getTables()).as("an unpainted section is no panel").isEmpty();
                assertThat(before(paragraph(document, "Caption")))
                        .as("the page's gap from the section's text, its padding below included")
                        .isCloseTo(Math.round(gap * 20), offset(2L));
            }
        }
    }

    @Test
    void aCaptionUnderAFilledRowResumesBelowItsText() throws Exception {
        // A row is not a panel: its fill is not written in a band, nor its padding below, so the
        // gap runs from its text.
        DocumentNode rowed = new SectionBuilder()
                .addRow(row -> row.fillColor(SURFACE).padding(new DocumentInsets(0, 0, BOTTOM_ONLY, 0))
                        .addParagraph(p -> p.name("Text").text("A filled row.")))
                .build();
        try (DocumentSession session = layered(rowed, captionAt(40))) {
            double gap = gapAbove(session, "Text", "Caption");
            try (XWPFDocument document = docx(session)) {
                assertThat(before(paragraph(document, "Caption")))
                        .as("the page's gap from the row's text")
                        .isCloseTo(Math.round(gap * 20) - RAISE, offset(2L));
            }
        }
    }

    @Test
    void aPanelInARowOpeningALaterLayerKeepsItsMarginInTheRowsCell() throws Exception {
        // The row's table takes the resume, and the panel in its cell writes its own 8pt margin
        // there: measured to the panel's box with that margin left in, the panel stood 8.1pt low
        // in Word 16.
        DocumentNode rowed = new SectionBuilder().margin(DocumentInsets.top(20))
                .addRow(row -> row.add(new SectionBuilder().name("Panel").fillColor(SURFACE)
                        .margin(DocumentInsets.top(8)).padding(DocumentInsets.of(6))
                        .addParagraph(p -> p.text("A panel in a row."))
                        .build()))
                .build();
        DocumentNode head = new ParagraphBuilder().name("Head").text("The head.").build();
        try (DocumentSession session = layered(head, rowed)) {
            double gap = gapAbove(session, "Head", "Panel");
            try (XWPFDocument document = docx(session)) {
                assertThat(after(paragraph(document, "The head.")))
                        .as("the page's gap from the head to the panel's box, less the 8pt margin the "
                            + "panel writes in the row's cell")
                        .isCloseTo(Math.round((gap - 8) * 20) + RAISE, offset(2L));
            }
        }
    }

    @Test
    void aPanelThePageStartsOverTheLayersBeforeItIsNamedFromItsBox() throws Exception {
        // The page starts the panel's box over the head, its text below the head's end: Word
        // writes the panel under the head, and it stood 13.0pt low in Word 16 with no note while
        // the overlap was read from its text.
        DocumentNode head = new ParagraphBuilder().name("Head").text("The head.")
                .margin(DocumentInsets.bottom(30)).build();
        DocumentNode panel = new SectionBuilder().name("Panel").fillColor(SURFACE)
                .padding(DocumentInsets.of(20))
                .addParagraph(p -> p.text("A panel over the head."))
                .build();
        Consumer<PageFlowBuilder> content = page -> page.spacing(SPACING)
                .addParagraph(p -> p.text("Before the stack."))
                .addLayerStack(stack -> stack.name("Stack").layer(head, LayerAlign.TOP_LEFT)
                        .layer(panel, LayerAlign.BOTTOM_RIGHT))
                .addParagraph(p -> p.text("After the stack."));
        assertThat(DocxExports.report(400, 500, 36, content).bySubject().get("overlapping layer"))
                .as("the panel, written under the head the page starts its box over")
                .isNotNull()
                .anySatisfy(note -> assertThat(note.path()).contains("Panel"));
    }

    @Test
    void aPanelWhoseBoxStartsBelowTheLayersBeforeItIsNotNamed() throws Exception {
        // The same panel under a head whose end is above the panel's box: nothing overlaps, and
        // reading the overlap from the box names nothing.
        DocumentNode head = new ParagraphBuilder().name("Head").text("The head.").build();
        DocumentNode panel = new SectionBuilder().name("Panel").fillColor(SURFACE)
                .padding(DocumentInsets.of(20)).margin(DocumentInsets.top(30))
                .addParagraph(p -> p.text("A panel under the head."))
                .build();
        Consumer<PageFlowBuilder> content = page -> page.spacing(SPACING)
                .addParagraph(p -> p.text("Before the stack."))
                .addLayerStack(stack -> stack.name("Stack").layer(head, LayerAlign.TOP_LEFT)
                        .layer(panel, LayerAlign.BOTTOM_RIGHT))
                .addParagraph(p -> p.text("After the stack."));
        assertThat(DocxExports.report(400, 500, 36, content).bySubject().get("overlapping layer"))
                .as("nothing laid over anything").isNull();
    }

    /** Padding below a panel's text alone, unlike {@link #PADDING} and {@link #THICKER}. */
    private static final double BOTTOM_ONLY = 10;

    private static DocumentNode captionAt(double margin) {
        return new ParagraphBuilder().name("Caption").text("Caption").margin(DocumentInsets.top(margin)).build();
    }

    /** A layer of a stack written as columns: a section padded in to its column, and down to {@code top}. */
    private static DocumentNode column(double left, double right, double top, DocumentNode content) {
        return new SectionBuilder().spacing(0).padding(new DocumentInsets(top, right, 0, left)).add(content).build();
    }

    /** {@code first} and {@code later} as two layers of a band, at its top left and bottom right. */
    private static DocumentSession layered(DocumentNode first, DocumentNode later) {
        DocumentSession session = GraphCompose.document().pageSize(400, 500)
                .margin(DocumentInsets.of(36)).create();
        session.pageFlow(page -> page.spacing(SPACING)
                .addParagraph(p -> p.text("Before the stack."))
                .addLayerStack(stack -> stack.name("Stack").layer(first, LayerAlign.TOP_LEFT)
                        .layer(later, LayerAlign.BOTTOM_RIGHT))
                .addParagraph(p -> p.text("After the stack.")));
        return session;
    }

    /** The page's gap from the foot of {@code upper}'s box to the top of {@code lower}'s. */
    private static double gapAbove(DocumentSession session, String upper, String lower) {
        PlacedNode above = placed(session, upper);
        PlacedNode below = placed(session, lower);
        return above.placementY() - (below.placementY() + below.placementHeight());
    }

    private static XWPFDocument docx(DocumentSession session) throws Exception {
        return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
    }

    /** The paragraph whose text is {@code text}, in the body or in a table's cell. */
    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        List<XWPFParagraph> all = new java.util.ArrayList<>(document.getParagraphs());
        document.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells()
                .forEach(cell -> all.addAll(cell.getParagraphs()))));
        return all.stream().filter(paragraph -> text.equals(paragraph.getText())).findFirst().orElseThrow();
    }

    private static DocumentNode card(double padding, DocumentInsets margin) {
        return new SectionBuilder().name("Card").softPanel(SURFACE, 0, padding).margin(margin)
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

    /** The paragraph between the first and the last the body holds. */
    private static XWPFParagraph middle(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.size() - 2);
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
