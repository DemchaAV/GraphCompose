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
 * A panel whose right border reaches its table cell's edge: Word cuts off what passes that edge,
 * so the export narrows the panel's table out of its right margin, and the report names how much
 * nearer its text the border is written and what of it still reaches past the room the cell
 * leaves.
 */
class DocxPanelRightBorderTest {

    private static final String SUBJECT = "right border";
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    // Word draws the whole border past the table's edge; the cell leaves room for it and half a
    // point clear of its gridline.
    private static final double BORDER = 1.5;
    private static final DocumentStroke ACCENT = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), BORDER);
    private static final Pattern HELD = Pattern.compile(
            "^its right border written up to (\\d+(?:\\.\\d+)?)pt nearer its text than the page draws it, to end "
            + "short of its table cell's edge, past which Word cuts it off on screen$");
    private static final Pattern NOT_HELD = Pattern.compile(
            "^its right border (?:written up to (\\d+(?:\\.\\d+)?)pt nearer its text than the page draws it, all "
            + "its right margin gives, and still )?reaching up to (\\d+(?:\\.\\d+)?)pt past its table cell's "
            + "edge, where Word cuts it off on screen$");

    @Test
    void aCardAsWideAsItsCellNamesHowMuchNearerItsTextItsRightBorderIsWritten() throws Exception {
        Consumer<PageFlowBuilder> content = card(10);
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(notes.get(0).path()).as("on the card").contains("Inner");
        Matcher held = HELD.matcher(notes.get(0).detail());
        assertThat(held.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        // The figure is what the table gives up: the card as wide as its cell, half its border
        // past each side, less the table as written.
        Nested nested = nested(content);
        assertThat(Double.parseDouble(held.group(1))).as("the points the table gives up")
                .isCloseTo(nested.room() + 1.5 - nested.written(), within(0.06))
                .as("its whole border and the half point clear of the gridline, each side's half border")
                .isCloseTo(1.5 + 1.5 + 0.5, within(0.06));
    }

    @Test
    void aCardWhoseRightMarginCannotTakeItsBorderInNamesWhatStillReachesPast() throws Exception {
        // A 1pt padding holds a quarter of a point past the half border it is drawn over.
        Consumer<PageFlowBuilder> content = card(1);
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        Matcher past = NOT_HELD.matcher(notes.get(0).detail());
        assertThat(past.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(past.group(1))).as("all its right margin gives").isCloseTo(0.25, within(0.01));
        Nested nested = nested(content);
        assertThat(Double.parseDouble(past.group(2))).as("what the table still runs past the room it is left")
                .isCloseTo(nested.past(), within(0.06))
                .isGreaterThan(0.5);
    }

    @Test
    void aCardWithNoRightPaddingNamesItsWholeBorderPastTheRoom() throws Exception {
        Consumer<PageFlowBuilder> content = card(0);
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        Matcher past = NOT_HELD.matcher(notes.get(0).detail());
        assertThat(past.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(past.group(1)).as("nothing written nearer its text").isNull();
        Nested nested = nested(content);
        assertThat(Double.parseDouble(past.group(2)))
                .isCloseTo(nested.past(), within(0.06))
                .isGreaterThan(0.5);
    }

    @Test
    void aShapeContainerAsWideAsItsCellIsNamed() throws Exception {
        // A box composed in a table cell is written as a panel. Its padding is set round its
        // outline, as a margin: nothing of its right margin is there to take the border in, and
        // a cell with no padding of its own holds nothing past its text.
        Consumer<PageFlowBuilder> content = shapeInACell(0);
        List<DocxExportReport.Note> notes = notes(content);

        assertThat(notes).hasSize(1);
        Matcher past = NOT_HELD.matcher(notes.get(0).detail());
        assertThat(past.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        Nested nested = nested(content);
        assertThat(Double.parseDouble(past.group(2)))
                .isCloseTo(nested.past(), within(0.06))
                .isGreaterThan(0.5);
    }

    @Test
    void aShapeContainerWhoseBorderReachesIntoItsCellsPaddingIsNotNamed() throws Exception {
        // The 2.5pt padding past the cell's text holds what the box's border reaches past it,
        // 2.25pt, the border ending a quarter of a point inside the cell's edge, within the half
        // point left clear of the gridline: it stands where the page draws it, inside the cell.
        assertThat(notes(shapeInACell(2.5))).isEmpty();
    }

    @Test
    void aPaintedRowAsWideAsItsCellIsNamedTheSameWay() throws Exception {
        List<DocxExportReport.Note> notes = notes(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addRow("Band", row -> row
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(10))
                        .addParagraph("Left")
                        .addParagraph("Right"))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Band");
        Matcher held = HELD.matcher(notes.get(0).detail());
        assertThat(held.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(held.group(1))).isCloseTo(1.5 + 1.5 + 0.5, within(0.06));
    }

    @Test
    void aCardWithRoomPastItsRightBorderIsNotNamed() throws Exception {
        // Its right margin leaves the border and the half point clear of the gridline inside the cell.
        assertThat(notes(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .stroke(ACCENT)
                        .margin(new DocumentInsets(0, 6, 0, 0))
                        .padding(DocumentInsets.of(10))
                        .addParagraph(p -> p.text("Inner")))))).isEmpty();
    }

    @Test
    void aCardWithNoRightBorderIsNotNamed() throws Exception {
        assertThat(notes(page -> page.addSection("Outer", outer -> outer
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
                .fillColor(SURFACE)
                .addSection("Inner", inner -> inner
                        .stroke(ACCENT)
                        .padding(DocumentInsets.of(padding))
                        .addParagraph(p -> p.text("Inner"))));
    }

    /** A stroked box as wide as its table cell's text, the cell padded the same on every side. */
    private static Consumer<PageFlowBuilder> shapeInACell(double padding) {
        return page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new ShapeContainerBuilder()
                                .name("Shape").rectangle(200 - 2 * padding, 30)
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

    /**
     * The holding cell's text width, its right margin and the nested table's written width, in
     * points.
     */
    private record Nested(double room, double right, double written) {

        /** What the table and the whole border Word draws past it run past the cell's edge. */
        double past() {
            return written + BORDER - room - right;
        }
    }

    private static Nested nested(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, content)) {
            XWPFTableCell host = document.getTables().get(0).getRow(0).getCell(0);
            var properties = host.getCTTc().getTcPr();
            // A panel's cell states its width; a table's cell takes its grid column's.
            Object cellWidth = properties.isSetTcW() ? properties.getTcW().getW()
                    : host.getTableRow().getTable().getCTTbl().getTblGrid().getGridColArray(0).getW();
            double room = (DocxTwips.of(cellWidth)
                           - DocxTwips.of(properties.getTcMar().getLeft().getW())
                           - DocxTwips.of(properties.getTcMar().getRight().getW())) / 20.0;
            double written = DocxTwips.of(host.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()) / 20.0;
            return new Nested(room, DocxTwips.of(properties.getTcMar().getRight().getW()) / 20.0, written);
        }
    }
}
