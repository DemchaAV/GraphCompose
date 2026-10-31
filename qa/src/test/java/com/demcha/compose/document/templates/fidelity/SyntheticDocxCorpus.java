package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.function.Consumer;

/**
 * Documents of the DOCX fidelity corpus built for a shape no template preset has on its own
 * fixture, so that an editor setting one off the page fails the gate as a preset would.
 *
 * <p>Three set a way the page breaks inside a panel or a column: a page break among a panel's
 * children and inside a panel nested in it, page breaks the page sets as boxes of no height in a
 * row's column and in a stack's layer, and a card whose table the layout runs over three pages,
 * with other margins from the second. Three others set a shape container holding text in the
 * body: a box narrower than the page, a box clipped to its outline, and a card. One more lays an
 * icon over a card's top right corner, in a layer stack. Their text is set in a face the export
 * embeds, so both editors break at the page's words the lines they set at the page's width.</p>
 *
 * <p>The export writes the three boxes as panels, their text inside the outline: Word sets the
 * narrow box's note and the card's text at the page's words, and the paragraph under each box
 * where the page sets it; of the clipped box, whose clip is written as the panel's exact height,
 * both editors show the lines the page shows. The gate reads lines of text only: whether the
 * text stays inside the outline, and whether the card grows when text is added to it in an
 * editor, it does not see.</p>
 *
 * <p>The card under the icon is held where the page sets it, the icon's own place aside: see
 * {@link #iconOverCard}.</p>
 */
final class SyntheticDocxCorpus {

    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    private static final DocumentTextStyle HEADING = DocumentTextStyle.builder().fontName(FontName.LATO).size(12)
            .decoration(DocumentTextDecoration.BOLD).build();
    private static final DocumentTableStyle CELLS = DocumentTableStyle.builder().textStyle(TEXT).build();
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INNER = DocumentColor.rgb(224, 233, 244);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentInsets MARGIN = DocumentInsets.of(36);
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<circle cx='5' cy='5' r='5' fill='#1a5694'/></svg>");

    private SyntheticDocxCorpus() {
    }

    /** The documents of the {@code synthetic} family, one for each shape. */
    static List<DocxCorpusDocument> documents() {
        return List.of(
                synthetic("break_in_panel", SyntheticDocxCorpus::breakInAPanel),
                synthetic("break_in_column", SyntheticDocxCorpus::breakInAColumn),
                synthetic("card_over_pages", SyntheticDocxCorpus::cardOverPages),
                synthetic("narrow_container", SyntheticDocxCorpus::narrowContainer),
                synthetic("clipped_container", SyntheticDocxCorpus::clippedContainer),
                synthetic("rounded_card", SyntheticDocxCorpus::roundedCard),
                synthetic("icon_over_card", SyntheticDocxCorpus::iconOverCard));
    }

    /**
     * A layer stack laying an icon over the top right corner of a card, between a paragraph above
     * it and one under it. The paragraph above is the document's first of body text and its
     * longest, so an edit that lengthens either moves the card, and the icon has to move with it.
     *
     * <p>Its baseline holds the card where the page sets it, within 0.06pt. It held the card
     * 13.99pt low in Word and 13.98pt in LibreOffice, and the paragraph under it 28.02 and
     * 27.98pt: the stack is written as a band, and the band measured the space round it through
     * the card's padding to the text in it, which the card writes again as its cell's margins.
     * The space is now measured to the card's box ({@code DocxLayerColumns#edgeIn}). The icon
     * is carried by the card's text, so it stands on the card's corner as each editor sets the
     * card, and moves with it when the text above is edited. The gate reads lines of text only:
     * where the icon stands it does not see, and {@link DocxDrawingAnchorGuardTest} holds the
     * anchor it is carried by.</p>
     */
    private static void iconOverCard(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card: this paragraph is the longest in the document, so "
                                           + "an edit that lengthens the longest paragraph lengthens it and moves "
                                           + "the card and the icon over its corner down the page together."))
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().softPanel(SURFACE, 0, 14)
                                .addParagraph(p -> text(p, "A card under an icon laid over its top right "
                                                           + "corner, its text held in by its padding."))
                                .build())
                        .layer(ICON.node(18), LayerAlign.TOP_RIGHT))
                .addParagraph(p -> text(p, "After the card: this paragraph stands under it on the page.")));
    }

    /**
     * A rounded box narrower than the page between a paragraph above it and one under it, its
     * note set left in a block centred in the box and wrapped over three lines inside it.
     */
    private static void narrowContainer(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the box, on a line of its own."))
                .addContainer(box -> box.roundedRect(220, 90, 10).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .center(paragraph("A note centred in a box narrower than the page: the page wraps "
                                          + "it over three lines inside the box, each line held within "
                                          + "its outline.")))
                .addParagraph(p -> text(p, "After the box: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the box.")));
    }

    /**
     * A rounded box clipped to its outline, holding more text than it has room for: the page
     * centres the text across the box, sets it from the box's top, as it is taller than the box,
     * and cuts the lines that run past its foot. A paragraph stands above the box and one under
     * it.
     */
    private static void clippedContainer(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the clipped box, on a line of its own."))
                .addContainer(box -> box.roundedRect(200, 60, 12).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(paragraph("Clipped text in a rounded box too short to hold it. The page "
                                          + "sets it from the top of the box and cuts it at the outline, so "
                                          + "the lines that run past the foot of the box are not seen. Only "
                                          + "the lines the box holds are read, and the box stays as tall as "
                                          + "it was made, whatever it is given to hold.")))
                .addParagraph(p -> text(p, "After the clipped box: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the box.")));
    }

    /**
     * A rounded card as wide as the page between its margins, its heading and text set from its
     * top left corner and filling it, under its default clip, between a paragraph above it and
     * one under it. Text added to the card in an editor needs it taller.
     */
    private static void roundedCard(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card, on a line of its own."))
                .addContainer(card -> card.roundedRect(328, 80, 12).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .topLeft(new SectionBuilder().padding(DocumentInsets.of(12)).spacing(4)
                                .addParagraph(p -> p.text("A rounded card").textStyle(HEADING))
                                .addParagraph(p -> text(p, "Its text runs over three lines from the card's top "
                                                           + "left corner, and the card is just tall enough to hold "
                                                           + "them with its padding round them: a line more, and "
                                                           + "the card needs to be taller."))
                                .build()))
                .addParagraph(p -> text(p, "After the card: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the card.")));
    }

    /**
     * A card with a page break among its children, and a card nested in it with a break of its
     * own: the card runs over three pages, each part where the page sets it.
     */
    private static void breakInAPanel(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card, on the first page."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(8)).spacing(6)
                        .addParagraph(p -> text(p, "The card opens on the first page."))
                        .addParagraph(p -> text(p, "Its second line stands under the first."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "After the card's own break, on the second page."))
                        .addSection(inner -> inner.fillColor(INNER).padding(DocumentInsets.of(6)).spacing(4)
                                .addParagraph(p -> text(p, "The inner card opens under it."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "After the inner card's break, on the third page.")))
                        .addParagraph(p -> text(p, "The card closes on the third page.")))
                .addParagraph(p -> text(p, "After the card, on the third page.")));
    }

    /**
     * Page breaks the page sets as boxes of no height: in a row's column and in a stack's one
     * layer. The page goes on with what follows on the same page, and so does the editor.
     */
    private static void breakInAColumn(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "A row and a stack, each with a break in it."))
                .addRow(row -> row.spacing(12)
                        .addSection(column -> column.spacing(4)
                                .addParagraph(p -> text(p, "Part A of the column."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "Part B of the column.")))
                        .addParagraph(p -> text(p, "The column beside it.")))
                .addLayerStack(stack -> stack.topLeft(new SectionBuilder().spacing(4)
                        .addParagraph(p -> text(p, "The layer before its break."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "The layer after its break."))
                        .build()))
                .addParagraph(p -> text(p, "After the row and the stack, on the same page.")));
    }

    /**
     * A bordered card whose table of sixty rows the layout runs over three pages, its header
     * repeated at the top of each, the pages after the first set against a larger top margin.
     *
     * <p>Spaced so that page 1's rows leave room under them for the paragraph that ends a Word
     * section at the cut, as gaps of 2 to 6pt and 12 to 16pt do: at 8 or 10pt the export names
     * page 2's margins instead, and both editors set page 2 with page 1's. A change to a table's
     * metrics can move page 1's last row into that case: the gate then fails this document with
     * every line of page 2 about 53pt high. Where the export's report names page 2's margins as
     * leaving page 1's rows too near its foot, the fixture is to be re-spaced; where it does not,
     * the section at the cut was lost, and the change is at fault.</p>
     */
    private static void cardOverPages(DocumentSession session) {
        session.pageSize(400, 600).margin(MARGIN);
        session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(90, 36, 36, 36))));
        session.pageFlow(page -> page.spacing(4)
                .addParagraph(p -> text(p, "The card's table runs over three pages."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(6))
                        .addTable(table -> {
                            table.columns(DocumentTableColumn.fixed(220), DocumentTableColumn.fixed(90))
                                    .defaultCellStyle(CELLS).headerStyle(CELLS)
                                    .header("Item", "Amount").repeatHeader();
                            for (int row = 1; row <= 60; row++) {
                                table.row("Line item " + row, String.valueOf(row * 7));
                            }
                        }))
                .addParagraph(p -> text(p, "After the card, on its last page.")));
    }

    private static ParagraphBuilder text(ParagraphBuilder paragraph, String text) {
        return paragraph.text(text).textStyle(TEXT);
    }

    private static ParagraphNode paragraph(String text) {
        return text(new ParagraphBuilder(), text).build();
    }

    private static DocxCorpusDocument synthetic(String name, Consumer<DocumentSession> compose) {
        return new DocxCorpusDocument("synthetic", name, -1, compose);
    }
}
