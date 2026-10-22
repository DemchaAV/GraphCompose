package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A panel whose right border reaches its table cell's edge. Word draws a nested table's right
 * border outside the table and cuts off what passes the cell's edge, so the export writes the
 * panel's table narrower out of its right margin; the report names how much narrower than the
 * page draws it, and about how far what the margin cannot take in still passes the cell's edge.
 *
 * <p>On a 523pt column, a card's 1.5pt border: the page draws the card as wide as the column, its
 * border centred on its edges. Word starts the table at the cell's text and draws the border
 * whole outside its right edge, half a point clear of the cell's gridline.
 */
class DocxPanelRightBorderTest {

    private static final String SUBJECT = "right border";
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final double BORDER = 1.5;
    // The page's column, which a panel as wide as its cell fills with no slack past it.
    private static final double COLUMN = 523;
    private static final DocumentStroke ACCENT = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), BORDER);
    private static final Pattern HELD = Pattern.compile(
            "^its table written (\\d+(?:\\.\\d+)?)pt narrower than the page draws it, out of its right margin, so "
            + "its right border, which Word draws outside the table, ends short of its table cell's edge, past "
            + "which Word cuts it off on screen$");
    private static final Pattern NOT_HELD = Pattern.compile(
            "^(?:its table written (\\d+(?:\\.\\d+)?)pt narrower than the page draws it, all its right margin "
            + "gives, and its right border, which Word draws outside the table, still|its right border, which "
            + "Word draws outside its table,) reaching about (\\d+(?:\\.\\d+)?)pt past its table cell's edge, "
            + "where Word cuts it off on screen$");

    @Test
    void aCardAsWideAsItsCellNamesHowMuchNarrowerItsTableIsWritten() throws Exception {
        Consumer<PageFlowBuilder> content = card(10);
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(notes.get(0).path()).as("on the card").contains("Inner");
        double narrower = held(notes.get(0));
        // The table starts half a border right of the card's left edge and its border ends a
        // whole border and half a point past its right one: 0.75 + 0.75 + 1.5 + 0.5 = 3.5.
        assertThat(narrower).as("by half of each side's border, the whole border and the half point")
                .isCloseTo(3.5, within(0.01));
        Nested nested = nested(content);
        assertThat(nested.room()).as("a cell no wider than the card").isCloseTo(COLUMN, within(0.06));
        assertThat(narrower).as("as the file writes it: the card and its half borders, less its table")
                .isCloseTo(COLUMN + BORDER - nested.written(), within(0.06));
    }

    @Test
    void aCardTheEditorsSlackWidenedNamesOnlyWhatItsTableIsNarrowerThanThePage() throws Exception {
        // 0.6pt narrower than its cell, the card is given that much slack, as a panel sized round
        // its content is; its table gives that back first, which is not narrower than the page.
        double width = COLUMN - 0.6;
        Consumer<PageFlowBuilder> content = page -> page.addSection("Outer", outer -> outer
                .fixedWidth(COLUMN)
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .fixedWidth(width)
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Inner"))));
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        double narrower = held(notes.get(0));
        assertThat(narrower).isCloseTo(3.5 - 0.6, within(0.01));
        assertThat(narrower).as("as the file writes it: the card and its half borders, less its table")
                .isCloseTo(width + BORDER - nested(content).written(), within(0.06));
    }

    @Test
    void aCardWhoseRightMarginCannotTakeItsBorderInNamesWhatStillPassesTheEdge() throws Exception {
        // A 1pt padding gives a quarter of a point past the half border drawn over it. Word starts
        // the table at the cell's text and its left side gives up what runs past the cell, the
        // half border and what its margin gives of the other half: 1.5pt. The table ends a quarter
        // of a point short of the cell's text, and its border 1.25pt past it.
        List<DocxExportReport.Note> notes = notes(card(1));

        assertThat(notes).hasSize(1);
        double[] past = notHeld(notes.get(0));
        assertThat(past[0]).as("all its right margin gives").isCloseTo(0.25, within(0.01));
        assertThat(past[1]).as("about how far its border passes the cell's edge").isCloseTo(1.25, within(0.01));
    }

    @Test
    void aCardWithNoRightPaddingNamesItsBorderPastTheEdge() throws Exception {
        // Its table ends half a border past the cell's text, its border 2.25pt past it.
        List<DocxExportReport.Note> notes = notes(card(0));

        assertThat(notes).hasSize(1);
        double[] past = notHeld(notes.get(0));
        assertThat(past[0]).as("nothing narrower").isNaN();
        assertThat(past[1]).isCloseTo(2.25, within(0.01));
    }

    @Test
    void aShapeContainerAsWideAsItsCellIsNamed() throws Exception {
        // A box composed in a table cell is written as a panel. Its padding is set round its
        // outline, as a margin: nothing of its right margin is there to take the border in, and
        // a cell with no padding of its own holds nothing past its text.
        List<DocxExportReport.Note> notes = notes(shapeInACell(200, 0));

        assertThat(notes).hasSize(1);
        double[] past = notHeld(notes.get(0));
        assertThat(past[0]).isNaN();
        assertThat(past[1]).isCloseTo(2.25, within(0.01));
    }

    @Test
    void aShapeContainerInACellTooNarrowToTakeAnyOfItsBorderInIsNamed() throws Exception {
        // A 2pt column leaves no room for a 1.5pt border and the half point: nothing is taken in,
        // and the border passes the edge all the same.
        List<DocxExportReport.Note> notes = notes(shapeInACell(2, 0));

        assertThat(notes).hasSize(1);
        double[] past = notHeld(notes.get(0));
        assertThat(past[0]).isNaN();
        assertThat(past[1]).isCloseTo(2.25, within(0.01));
    }

    @Test
    void aShapeContainerWhoseBorderReachesPastItsCellsPaddingIsNamedForWhatPassesIt() throws Exception {
        // A 1pt padding past the cell's text holds a point of the 2.25pt the box's border reaches
        // past it: the rest passes the cell's edge.
        List<DocxExportReport.Note> notes = notes(shapeInACell(200, 1));

        assertThat(notes).hasSize(1);
        double[] past = notHeld(notes.get(0));
        assertThat(past[0]).isNaN();
        assertThat(past[1]).isCloseTo(1.25, within(0.01));
    }

    @Test
    void aShapeContainerInAnUnruledCellIsNamedForWhatPassesItsEdge() throws Exception {
        // With no rule at its edge, the cell's 1pt padding holds a point of the 2.25pt the box's
        // border reaches past its text, as a ruled one does: the rest passes the cell's edge.
        List<DocxExportReport.Note> notes = notes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new ShapeContainerBuilder()
                                .name("Shape").rectangle(198, 30)
                                .stroke(ACCENT)
                                .position(new ParagraphBuilder().name("Label").text("Label").build(), 10, 0,
                                        LayerAlign.CENTER_LEFT)
                                .build())
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(1))
                                .stroke(DocumentStroke.of(SURFACE, 0)).build()))));

        assertThat(notes).hasSize(1);
        assertThat(notHeld(notes.get(0))[1]).isCloseTo(1.25, within(0.01));
    }

    @Test
    void aCardInARuledCellIsHeldInAtTheCellsText() throws Exception {
        // A card as wide as its composed cell's text, its cell padded 2.5pt and ruled 2pt: the
        // rule may cover the padding, which is left to the text, and the card's border ends the
        // half point short of the text, as it does with no padding: 0.75 + 0.75 + 1.5 + 0.5.
        List<DocxExportReport.Note> notes = notes(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                                .name("Inner").stroke(ACCENT).padding(DocumentInsets.of(10))
                                .addParagraph(p -> p.text("Inner")).build())
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(2.5))
                                .stroke(DocumentStroke.of(SURFACE, 2)).build()))));

        assertThat(notes).hasSize(1);
        assertThat(held(notes.get(0))).isCloseTo(3.5, within(0.01));
    }

    @Test
    void aShapeContainerWhoseBorderReachesIntoItsCellsPaddingIsNotNamed() throws Exception {
        // The 2.5pt padding past the cell's text holds the 2.25pt the box's border reaches past
        // it: the border ends a quarter of a point inside the cell's edge, within the half point
        // left clear of the gridline, and stands where the page draws it.
        assertThat(notes(shapeInACell(200, 2.5))).isEmpty();
    }

    @Test
    void aPaintedRowAsWideAsItsCellIsNamedTheSameWay() throws Exception {
        List<DocxExportReport.Note> notes = notes(page -> page.addSection("Outer", outer -> outer
                .fixedWidth(COLUMN)
                .fillColor(SURFACE)
                .addRow("Band", row -> row
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(10))
                        .addParagraph("Left")
                        .addParagraph("Right"))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Band");
        assertThat(held(notes.get(0))).isCloseTo(3.5, within(0.01));
    }

    @Test
    void aCardWithRoomPastItsRightBorderIsNotNamed() throws Exception {
        // 6pt narrower than its cell, it leaves the border and the half point clear of the gridline
        // inside the cell, the editor's slack given too.
        assertThat(notes(page -> page.addSection("Outer", outer -> outer
                .fixedWidth(COLUMN)
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .fixedWidth(COLUMN - 6)
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Inner")))))).isEmpty();
    }

    @Test
    void aCardWithNoRightBorderIsNotNamed() throws Exception {
        assertThat(notes(page -> page.addSection("Outer", outer -> outer
                .fixedWidth(COLUMN)
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .borders(DocumentBorders.left(ACCENT))
                        .padding(DocumentInsets.of(0))
                        .addParagraph(p -> p.text("Inner")))))).isEmpty();
    }

    @Test
    void aCardInTheBodyIsNotNamed() throws Exception {
        // In the body no cell's edge cuts the border off: the table keeps its whole width.
        assertThat(notes(page -> page.addSection("Card", card -> card
                .stroke(ACCENT)
                .padding(DocumentInsets.of(0))
                .addParagraph(p -> p.text("Card"))))).isEmpty();
    }

    /** A stroked card as wide as the filled panel holding it, padded the same on every side. */
    private static Consumer<PageFlowBuilder> card(double padding) {
        return page -> page.addSection("Outer", outer -> outer
                .fixedWidth(COLUMN)
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .fixedWidth(COLUMN)
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(padding))
                        .addParagraph(p -> p.text("Inner"))));
    }

    /** A stroked box as wide as its table cell's text, the cell padded the same on every side. */
    private static Consumer<PageFlowBuilder> shapeInACell(double column, double padding) {
        return page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(column))
                .rowCells(DocumentTableCell.node(new ShapeContainerBuilder()
                                .name("Shape").rectangle(column - 2 * padding, 30)
                                .stroke(ACCENT)
                                .position(new ParagraphBuilder().name("Label").text("Label").build(), 10, 0,
                                        LayerAlign.CENTER_LEFT)
                                .build())
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(padding)).build())));
    }

    /** The right-border notes of an export the layout stands behind. */
    private static List<DocxExportReport.Note> notes(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(595, 842, 36, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report.bySubject().getOrDefault(SUBJECT, List.of());
    }

    /** How much narrower a note of a border held inside its cell names its table. */
    private static double held(DocxExportReport.Note note) {
        Matcher held = HELD.matcher(note.detail());
        assertThat(held.matches()).as("the note: %s", note.detail()).isTrue();
        return Double.parseDouble(held.group(1));
    }

    /** How much narrower a note of a border past the edge names its table, NaN for none, and how far past. */
    private static double[] notHeld(DocxExportReport.Note note) {
        Matcher past = NOT_HELD.matcher(note.detail());
        assertThat(past.matches()).as("the note: %s", note.detail()).isTrue();
        return new double[]{past.group(1) == null ? Double.NaN : Double.parseDouble(past.group(1)),
                Double.parseDouble(past.group(2))};
    }

    /** The holding cell's text width and the nested table's written width, in points. */
    private record Nested(double room, double written) {
    }

    private static Nested nested(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, content)) {
            XWPFTableCell host = document.getTables().get(0).getRow(0).getCell(0);
            var properties = host.getCTTc().getTcPr();
            double room = (DocxTwips.of(properties.getTcW().getW())
                           - DocxTwips.of(properties.getTcMar().getLeft().getW())
                           - DocxTwips.of(properties.getTcMar().getRight().getW())) / 20.0;
            double written = DocxTwips.of(host.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()) / 20.0;
            return new Nested(room, written);
        }
    }
}
