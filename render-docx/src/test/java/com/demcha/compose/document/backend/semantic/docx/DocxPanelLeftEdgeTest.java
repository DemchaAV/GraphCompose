package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel in a table cell that the page sets left of the cell's text: its left border centred on
 * the text's edge, or a margin below zero. LibreOffice starts a nested table at its cell's text,
 * and Word may, so the report names how far left the page sets it.
 */
class DocxPanelLeftEdgeTest {

    private static final String SUBJECT = "left edge";
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor OUTER = DocumentColor.rgb(250, 245, 230);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);
    private static final String HUNG = "its left edge, which the page sets 1.5pt left of its table cell's text, "
                                       + "comes out up to about that much right of the page's: LibreOffice starts a "
                                       + "nested table at its cell's text, and Word may";

    @Test
    void anAccentHangingOutOfARowsCellIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = notes(beside(card()));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Card");
        assertThat(notes.get(0).detail()).isEqualTo(HUNG);
    }

    @Test
    void anAccentHangingOutOfACellInAFilledPanelIsNamedTheSameWay() throws Exception {
        // Word sets it where it is written there, measured 0.04 to 0.17pt off the page's;
        // LibreOffice still starts it at the cell's text.
        List<DocxExportReport.Note> notes = notes(page -> page.addSection("Outer", outer -> outer.fillColor(OUTER)
                .addRow(row -> row.spacing(12).weights(1, 1)
                        .addSection("Other", other -> other.addParagraph(p -> p.text("Other column")))
                        .addSection("Card", card()))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).detail()).isEqualTo(HUNG);
    }

    @Test
    void anAccentAloneIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = notes(beside(quote -> quote.accentLeft(ACCENT, 3)
                .padding(DocumentInsets.of(8)).addParagraph(p -> p.text("A line set off by a rule"))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).detail()).isEqualTo(HUNG);
    }

    @Test
    void aRowsColumnHoldsItsCardsLeftMarginOnce() throws Exception {
        // The row's cell starts past the card's 6pt margin: its accent still hangs half out.
        Consumer<PageFlowBuilder> content = beside(card -> card().accept(card.margin(new DocumentInsets(0, 0, 0, 6))));
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).detail()).isEqualTo(HUNG);
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 30, content)) {
            assertThat(document.getDocument().xmlText()).as("half the accent left of the cell's text, not 4.5pt right")
                    .containsPattern("<w:tblInd[^>]*w:w=\"-30\"");
        }
    }

    @Test
    void aPaintedRowInAFilledPanelIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = notes(page -> page.addSection("Outer", outer -> outer.fillColor(OUTER)
                .addRow(row -> row.name("Band").fillColor(SURFACE).padding(DocumentInsets.of(8))
                        .borders(DocumentBorders.left(DocumentStroke.of(ACCENT, 3)))
                        .addParagraph(p -> p.text("Engineer")).addParagraph(p -> p.text("2019")))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Band");
        assertThat(notes.get(0).detail()).isEqualTo(HUNG);
    }

    @Test
    void aMarginBelowZeroInAPanelsCellIsNamedForThePanel() throws Exception {
        // Its whole table moves, its text with it.
        List<DocxExportReport.Note> notes = notes(inABorderedPanel(card -> card
                .borders(DocumentBorders.right(DocumentStroke.of(ACCENT, 1))).padding(DocumentInsets.of(8))
                .margin(new DocumentInsets(0, 0, 0, -4)).addParagraph(p -> p.text("Bled card"))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Card");
        assertThat(notes.get(0).detail()).isEqualTo("the panel, which the page sets 4pt left of its table cell's "
                                                     + "text, comes out up to about that much right of the page's, "
                                                     + "its text with it: LibreOffice starts a nested table at its "
                                                     + "cell's text, and Word may");
    }

    @Test
    void aPanelThePageSetsInsideItsCellIsNotNamed() throws Exception {
        // In from the panel's cell's text by more than half its accent.
        assertThat(notes(inABorderedPanel(card -> card().accept(card.margin(new DocumentInsets(0, 0, 0, 2))))))
                .isEmpty();
    }

    @Test
    void aBoxInATableCellIsNamed() throws Exception {
        // Composed in a table cell, written as a panel: its 1.1pt border hangs half out.
        List<DocxExportReport.Note> notes = notes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(160))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(new ShapeContainerBuilder()
                        .name("Shape").rectangle(150, 30).fillColor(SURFACE)
                        .stroke(DocumentStroke.of(ACCENT, 1.1))
                        .position(new ParagraphBuilder().name("Label").text("Label").build(), 10, 0,
                                LayerAlign.CENTER_LEFT)
                        .build()))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).detail()).startsWith("its left edge, which the page sets 0.55pt left of its "
                                                     + "table cell's text");
    }

    @Test
    void aHairlineHangingLessThanATenthOfAPointIsNotNamed() throws Exception {
        assertThat(notes(beside(card -> card.fillColor(SURFACE).padding(DocumentInsets.of(8))
                .borders(DocumentBorders.left(DocumentStroke.of(ACCENT, 0.15)))
                .addParagraph(p -> p.text("Hairline card"))))).isEmpty();
    }

    @Test
    void anAccentInTheBodyIsNotNamed() throws Exception {
        // In the body the table starts at the panel's edge, its left border centred there.
        assertThat(notes(page -> page.addSection("Card", card()))).isEmpty();
    }

    @Test
    void aPanelBledLeftInTheBodyIsNotNamed() throws Exception {
        // A body table is placed by its indent, into the page's margin as far as it is bled.
        assertThat(notes(page -> page.addSection("Card", card -> card().accept(
                card.margin(new DocumentInsets(0, 0, 0, -12)))))).isEmpty();
    }

    /** A filled card with a 3pt accent on its left, padded 8pt. */
    private static Consumer<SectionBuilder> card() {
        return card -> card.fillColor(SURFACE).accentLeft(ACCENT, 3).padding(DocumentInsets.of(8))
                .addParagraph(p -> p.text("Accented card"));
    }

    /** The card as the second column of a row, beside a column of text. */
    private static Consumer<PageFlowBuilder> beside(Consumer<SectionBuilder> card) {
        return page -> page.addRow(row -> row.spacing(12).weights(1, 1)
                .addSection("Other", other -> other.addParagraph(p -> p.text("Other column")))
                .addSection("Card", card));
    }

    /** The card inside a panel with a border and no fill, padded 10pt. */
    private static Consumer<PageFlowBuilder> inABorderedPanel(Consumer<SectionBuilder> card) {
        return page -> page.addSection("Outer", outer -> outer.stroke(DocumentStroke.of(ACCENT, 0.5))
                .padding(DocumentInsets.of(10)).addSection("Card", card));
    }

    private static List<DocxExportReport.Note> notes(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(400, 400, 30, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report.bySubject().getOrDefault(SUBJECT, List.of());
    }
}
