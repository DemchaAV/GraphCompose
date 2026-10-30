package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.function.Consumer;

/**
 * Small documents whose every drawing is painted for a block that writes text in the flow: one for
 * each writer of a paragraph that carries a drawing today — a paragraph of the body, a panel's, a
 * column of a row, an item of a list, the one line two halves of a band share, and a table cell a
 * drawing is all of.
 *
 * <p>The template corpus proves what the export does today over whole presets, where a drawing's
 * owner is buried in a preset's tree. These documents hold the rule itself: a drawing whose owner
 * writes text must be carried by a paragraph, never nailed to the sheet, and each document is
 * small enough that a failure names the writer that stopped offering its paragraph
 * ({@link DocxDrawingAnchorGuardTest}).</p>
 *
 * <p>Each is laid out on a small page, its drawing standing beside its owner's text so that there
 * is a paragraph to carry it: a drawing further from text than the export reaches is another case,
 * and not this one.</p>
 */
final class FlowOwnerDrawingDocuments {

    private static final DocumentInsets MARGIN = DocumentInsets.of(24);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();

    /** The cells' text, in the same face: the export embeds it, so the editors set the same lines. */
    private static final DocumentTableStyle CELLS = DocumentTableStyle.builder().textStyle(TEXT).build();

    /** A disc the export writes as a drawing, small enough to stand beside a line of text. */
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<circle cx='5' cy='5' r='5' fill='#1a5694'/></svg>");

    private FlowOwnerDrawingDocuments() {
    }

    /**
     * A document and the writer of text whose paragraph is to carry its drawings.
     *
     * @param name     its name, which a failure is reported under
     * @param writer   the writer of text the drawings' owners hold, named for the report
     * @param drawings how many drawings a node paints in it
     * @param compose  how it is composed
     */
    record FlowOwnerDocument(String name, String writer, int drawings, Consumer<DocumentSession> compose) {
    }

    /** One document for each writer of a paragraph that may carry a drawing. */
    static List<FlowOwnerDocument> documents() {
        return List.of(
                new FlowOwnerDocument("disc_beside_body_text", "a paragraph of the body", 1,
                        FlowOwnerDrawingDocuments::discBesideBodyText),
                new FlowOwnerDocument("icon_over_panel_text", "a panel's paragraph", 1,
                        FlowOwnerDrawingDocuments::iconOverPanelText),
                new FlowOwnerDocument("icon_in_row_column", "a column of a row", 1,
                        FlowOwnerDrawingDocuments::iconInRowColumn),
                new FlowOwnerDocument("icon_over_list_card", "an item of a list", 1,
                        FlowOwnerDrawingDocuments::iconOverListCard),
                new FlowOwnerDocument("disc_alone_in_a_cell", "a table cell a drawing is all of", 1,
                        FlowOwnerDrawingDocuments::discAloneInACell),
                new FlowOwnerDocument("disc_beside_a_bands_line", "the one line two halves of a band share", 1,
                        FlowOwnerDrawingDocuments::discBesideABandsLine));
    }

    /**
     * A disc between two paragraphs of the body: the page flow holds both the disc and the text,
     * so a paragraph of the flow carries it.
     */
    private static void discBesideBodyText(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the disc: this paragraph grows a line when it is edited, "
                                           + "and the disc under it has to move down with it."))
                .addEllipse(18, 18, INK)
                .addParagraph(p -> text(p, "Under the disc, on the same page.")));
    }

    /**
     * An icon laid over the top right corner of a panel whose own paragraph is the only text
     * holding it: that paragraph carries the icon.
     */
    private static void iconOverPanelText(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the panel."))
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().softPanel(SURFACE, 0, 12)
                                .addParagraph(p -> text(p, "A panel under an icon laid over its corner, its "
                                                           + "text held in by its padding."))
                                .build())
                        .layer(ICON.node(16), LayerAlign.TOP_RIGHT))
                .addParagraph(p -> text(p, "Under the panel, on the same page.")));
    }

    /** An icon in one column of a row and text in the other: the row's columns write both. */
    private static void iconInRowColumn(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the row."))
                .addRow(row -> row.gap(8).weights(1, 4)
                        .add(ICON.node(16))
                        .addParagraph(p -> text(p, "Beside the icon, in the other column of the row: the icon "
                                                   + "stands against this text and moves with it.")))
                .addParagraph(p -> text(p, "Under the row, on the same page.")));
    }

    /**
     * An icon laid over a card whose only text is a list's items: an item of the list carries it,
     * no paragraph of the card's own being written.
     */
    private static void iconOverListCard(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the card."))
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().softPanel(SURFACE, 0, 12)
                                .addList(list -> list.bullet().textStyle(TEXT)
                                        .items("The first item of the card's list.",
                                                "The second, which the icon stands beside.",
                                                "The third and last item."))
                                .build())
                        .layer(ICON.node(16), LayerAlign.TOP_RIGHT))
                .addParagraph(p -> text(p, "Under the card, on the same page.")));
    }

    /**
     * A table a cell of which holds nothing but a disc, its neighbour holding text. A drawing that
     * is all its cell holds is the export's own exception: it is anchored in that cell's paragraph
     * directly, without the walk from the cell whose box holds it
     * ({@code DocxDrawingAnchors#anchorInCell}), so this document holds that path and not the walk.
     *
     * <p>The walk itself has no document here. A composed cell holding text as well as a drawing
     * leaves the drawing on the page today, as 25 of the corpus's do, so there is nothing for a
     * document of the rule to assert: the corpus ledger carries that case until the export does.</p>
     */
    private static void discAloneInACell(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the table."))
                .addTable(table -> table
                        .columns(DocumentTableColumn.fixed(80), DocumentTableColumn.fixed(170))
                        .defaultCellStyle(CELLS)
                        .rowCells(DocumentTableCell.node(new EllipseBuilder().circle(18).fillColor(INK)
                                        .stroke(DocumentStroke.of(INK, 1)).build()),
                                DocumentTableCell.text("Beside the disc, in the cell next to it."))
                        .rowCells(DocumentTableCell.text("Second row"), DocumentTableCell.text("Under the disc")))
                .addParagraph(p -> text(p, "Under the table, on the same page.")));
    }

    /**
     * A band whose title stands at the left of a line and whose dates stand at the right of the
     * same line, with a disc laid over it: the export writes the two halves as one Word line, and
     * that line is the only text the band writes, so it carries the disc.
     */
    private static void discBesideABandsLine(DocumentSession session) {
        session.pageSize(300, 320).margin(MARGIN);
        session.pageFlow(page -> page.spacing(6)
                .addParagraph(p -> text(p, "Above the band."))
                .addLayerStack(stack -> stack
                        .layer(paragraph("Senior engineer"), LayerAlign.TOP_LEFT)
                        .layer(paragraph("2019-2024"), LayerAlign.TOP_RIGHT)
                        .layer(new EllipseBuilder().circle(8).fillColor(INK).build(), LayerAlign.CENTER_LEFT))
                .addParagraph(p -> text(p, "Under the band, on the same page.")));
    }

    private static ParagraphBuilder text(ParagraphBuilder paragraph, String text) {
        return paragraph.text(text).textStyle(TEXT);
    }

    private static ParagraphNode paragraph(String text) {
        return text(new ParagraphBuilder(), text).build();
    }
}
