package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The point of slack a panel is given past the page's width, for an editor setting its text in its
 * own face, and where its right border may reach in a table cell.
 *
 * <p>The slack comes out of the panel's right padding, so its fill and borders end where the page
 * ends them and its text keeps the slack. Where the padding cannot hold it, or a line Word sets
 * wider than the page runs further into the padding than the rest holds, the panel is written that
 * much wider, and the report names it. In a table cell, the cell's own right margin holds what of
 * a right border passes the cell's text.</p>
 */
class DocxPanelSlackTest {

    private static final String RIGHT_END = "right end";
    private static final String RIGHT_BORDER = "right border";
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentStroke ACCENT = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 1);
    private static final String LONG = "Built backend services and document rendering pipelines processing two million "
                                       + "documents a month, and drove the migration to a layout engine of its own, "
                                       + "cutting render latency to a third.";
    // A line about 105pt wide at 7.25pt.
    private static final String ONE_LINE = "Rendering pipelines for documents";
    // A line a little narrower at 5.25pt.
    private static final String LATER_LINE = "Rendering pipelines for documents, and layouts";
    // Words a letter wide, so a panel's lines fill it and it is as wide as its column.
    private static final String FILL = "i ".repeat(240);
    // In from its cell's text past its left border, which does not then hang out of the cell.
    private static final DocumentInsets STANDS_IN = new DocumentInsets(0, 0, 0, 2);
    // A column of a two-column row with a 12pt gap on a 340pt page: (340 - 12) / 2.
    private static final double COLUMN = 164;
    private static final double SLACK = DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS;

    @Test
    void theSlackComesOutOfTheRightPaddingAndThePanelEndsWhereThePageEndsIt() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(10))
                .addParagraph(p -> p.text("Short")));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable table = document.getTables().get(0);

            assertThat(twips(table.getCTTbl().getTblPr().getTblW().getW())).as("as wide as the page draws it")
                    .isEqualTo(Math.round(placed * 20));
            assertThat(twips(rightMargin(table))).as("the padding less the slack")
                    .isEqualTo(Math.round((10 - SLACK) * 20));
            assertThat(textWidth(table)).as("its text given the slack past the page's")
                    .isCloseTo(placed - 20 + SLACK, within(0.05));
        }
        assertThat(notes(content, RIGHT_END)).isEmpty();
    }

    @Test
    void aPanelWhosePaddingCannotHoldTheSlackIsWrittenWiderAndNamed() throws Exception {
        // A heading underlined by its bottom border, with no padding to hold the slack: the rule is
        // a point longer than the page's.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Heading", heading -> heading
                .borders(DocumentBorders.bottom(ACCENT))
                .addParagraph(p -> p.text("Experience")));
        double placed = placedWidth(content, "Heading");
        try (XWPFDocument document = export(content)) {
            assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()))
                    .isEqualTo(Math.round((placed + SLACK) * 20));
        }
        List<DocxExportReport.Note> notes = notes(content, RIGHT_END);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Heading");
        assertThat(notes.get(0).detail()).isEqualTo("its table written 1pt wider than the page draws it, the room an "
                + "editor setting its text in its own face may need, which its right margin cannot hold: what it "
                + "paints at its right end reaches that much further right");
    }

    @Test
    void aThinPaddingWithNoLinePastItsTextHoldsTheSlack() throws Exception {
        // 1.5pt holds the point: no line runs past the text into what is left of it.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(1.5))
                .addParagraph(p -> p.text("Short")));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable table = document.getTables().get(0);

            assertThat(twips(table.getCTTbl().getTblPr().getTblW().getW())).isEqualTo(Math.round(placed * 20));
            assertThat(twips(rightMargin(table))).isEqualTo(Math.round((1.5 - SLACK) * 20));
        }
        assertThat(notes(content, RIGHT_END)).isEmpty();
    }

    @Test
    void aCentredLineGivenRoomPastItsTextGetsTheSlackBack() throws Exception {
        // A centred line of its own, tracked, is set at Word's size, its letters 3.4% wider and
        // its tracking not, near 293pt in a 293pt measure, and given 3% more room at its right
        // alone in a cell. Its widest as Word sets it stays inside the padding beside the slack,
        // but a line centred in the wider measure may run to it.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fixedWidth(300)
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addParagraph(p -> p.text("Rendering pipelines for every document").align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(7.25)
                                .withLetterSpacing(DocumentLetterSpacing.points(4.2)))));
        double placed = placedWidth(content, "Card");
        assertThat(lineWidths(content)).as("one line, as the page sets it").singleElement()
                .satisfies(width -> assertThat(width).isBetween(285.0, 291.0));
        try (XWPFDocument document = export(content)) {
            assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()))
                    .isEqualTo(Math.round((placed + SLACK) * 20));
        }
    }

    @Test
    void aPanelPaintingNothingAtItsRightEndIsNotNamed() throws Exception {
        // A left border alone stands where the page draws it, however wide the table.
        assertThat(notes(page -> page.addSection("Quote", quote -> quote
                .borders(DocumentBorders.left(ACCENT))
                .addParagraph(p -> p.text("A line set off by a rule"))), RIGHT_END)).isEmpty();
    }

    @Test
    void aPanelBrokenAcrossPagesIsNamedOnce() throws Exception {
        // A page break among its children closes its table and opens another on the next page:
        // each is as wide, and the panel one loss.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Broken", section -> section
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Before the break"))
                .addPageBreak(pageBreak -> pageBreak.name("Break"))
                .addParagraph(p -> p.text("After the break")));
        try (XWPFDocument document = export(content)) {
            assertThat(document.getTables()).as("a table a page").hasSize(2);
        }
        assertThat(notes(content, RIGHT_END)).hasSize(1);
    }

    @Test
    void aLineWordSetsWiderRunningPastWhatThePaddingStillHoldsGetsTheSlackBack() throws Exception {
        // Set at 7.25pt, written at 7.5: each line Word sets is 3.4% wider than the page's, near ten
        // points on a 290pt line of words a letter wide, past the 3pt the padding holds beside the
        // slack. Word cuts off what passes the panel's edge.
        Consumer<PageFlowBuilder> content = card(7.25);
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable table = document.getTables().get(0);

            assertThat(twips(table.getCTTbl().getTblPr().getTblW().getW())).as("given the slack back")
                    .isEqualTo(Math.round((placed + SLACK) * 20));
            assertThat(twips(rightMargin(table))).as("all the padding").isEqualTo(4 * 20L);
        }
        List<DocxExportReport.Note> notes = notes(content, RIGHT_END);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).detail()).contains("1pt wider than the page draws it")
                .contains("cannot hold beside the lines it sets past its text");
    }

    @Test
    void aListItemsLineWordSetsWiderGetsTheSlackBackWhereItIsGivenRoomPastItsText() throws Exception {
        // An item written as its marker's letters is given room past its text, as a paragraph is;
        // one past a hanging marker's column is held to its text, where Word wraps it.
        for (boolean hanging : new boolean[]{false, true}) {
            Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                    .fixedWidth(300)
                    .fillColor(SURFACE)
                    .padding(DocumentInsets.of(4))
                    .addList(list -> list.bullet().hangingIndent(hanging)
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(7.25)).items(FILL)));
            double placed = placedWidth(content, "Card");
            try (XWPFDocument document = export(content)) {
                assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()))
                        .as("hanging %s", hanging)
                        .isEqualTo(Math.round((placed + (hanging ? 0 : SLACK)) * 20));
            }
        }
    }

    @Test
    void aPanelBrokenAcrossPagesIsNamedAtItsWidestTable() throws Exception {
        // Each table takes back what its own lines need: the second's, set at 5.25pt and written
        // at 5.5, grows 4.8% where the first's grows 3.4%.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Broken", section -> section
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addParagraph(p -> p.text(ONE_LINE).textStyle(DocumentTextStyle.DEFAULT.withSize(7.25)))
                .addPageBreak(pageBreak -> pageBreak.name("Break"))
                .addParagraph(p -> p.text(LATER_LINE).textStyle(DocumentTextStyle.DEFAULT.withSize(5.25))));
        List<Double> lines = lineWidths(content);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(1)).as("the later line, against the first").isBetween(lines.get(0) - 4, lines.get(0));
        double placed = placedWidth(content, "Broken");
        double widest;
        try (XWPFDocument document = export(content)) {
            double first = twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()) / 20.0;
            double second = twips(document.getTables().get(1).getCTTbl().getTblPr().getTblW().getW()) / 20.0;
            assertThat(second).as("the later table the wider").isGreaterThan(first + 0.05);
            assertThat(first).as("the first given some back").isGreaterThan(placed + 0.1);
            widest = second - placed;
        }
        List<DocxExportReport.Note> notes = notes(content, RIGHT_END);
        assertThat(notes).hasSize(1);
        java.util.regex.Matcher named = java.util.regex.Pattern.compile("^its table written ([0-9.]+)pt wider")
                .matcher(notes.get(0).detail());
        assertThat(named.find()).as(notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(named.group(1))).as("the wider table's").isCloseTo(widest, within(0.03));
    }

    @Test
    void aLineWithinAPointOfWhatThePaddingHoldsGetsWhatItLacksBack() throws Exception {
        // One line about 105pt wide at 7.25pt, written at 7.5, runs 3.6pt further than the page's,
        // 2.6pt past the text the slack widens: within the 3pt the 4pt padding holds beside the
        // slack, though not by the point an editor's face may set it wider still.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addParagraph(p -> p.text(ONE_LINE).textStyle(DocumentTextStyle.DEFAULT.withSize(7.25))));
        double placed = placedWidth(content, "Card");
        assertThat(placed - 8).as("the line, as the page sets it").isBetween(95.0, 115.0);
        try (XWPFDocument document = export(content)) {
            assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()) / 20.0)
                    .as("given back what the face may need past the padding")
                    .isGreaterThan(placed + 0.2).isLessThan(placed + SLACK);
        }
    }

    @Test
    void aLineHeldInByItsOwnPaddingLeavesTheSlackInThePanelsPadding() throws Exception {
        // Its lines run past their own 10pt padding, not past the panel's text.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fixedWidth(300)
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addParagraph(p -> p.text(FILL).padding(new DocumentInsets(0, 10, 0, 0))
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(7.25))));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()))
                    .isEqualTo(Math.round(placed * 20));
        }
    }

    @Test
    void aLineInATableInsideThePanelLeavesTheSlackInThePanelsPadding() throws Exception {
        // A row's lines run in its own cells, which cut them off at their own edges.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Card", card -> card
                .fixedWidth(300)
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addRow(row -> row.spacing(8).weights(1, 1)
                        .addParagraph(p -> p.text(FILL).textStyle(DocumentTextStyle.DEFAULT.withSize(7.25)))
                        .addParagraph(p -> p.text(FILL).textStyle(DocumentTextStyle.DEFAULT.withSize(7.25)))));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            assertThat(twips(document.getTables().get(0).getCTTbl().getTblPr().getTblW().getW()))
                    .isEqualTo(Math.round(placed * 20));
        }
    }

    @Test
    void aLineWordSetsAsThePageDoesLeavesTheSlackInThePadding() throws Exception {
        Consumer<PageFlowBuilder> content = card(7.5);
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable table = document.getTables().get(0);

            assertThat(twips(table.getCTTbl().getTblPr().getTblW().getW())).isEqualTo(Math.round(placed * 20));
            assertThat(twips(rightMargin(table))).isEqualTo(Math.round((4 - SLACK) * 20));
        }
        assertThat(notes(card(7.5), RIGHT_END)).isEmpty();
    }

    @Test
    void aRailHungOutOfTheColumnItFillsEndsWhereThePageEndsIt() throws Exception {
        // 161pt wide in a 163pt column, its 3pt left border hanging out of the cell: Word and
        // LibreOffice start it half that border in, and the table gives up the whole border, so
        // it ends where the page ends it, clear of the cell's edge. Its slack then comes out of
        // its right margin, as any panel's does.
        Consumer<PageFlowBuilder> content = page -> page.addRow(row -> row
                .spacing(14)
                .weights(1, 1)
                .addSection("Rail", rail -> rail
                        .fixedWidth(161)
                        .fillColor(SURFACE)
                        .accentLeft(ACCENT.color(), 3)
                        .padding(DocumentInsets.of(4))
                        .addParagraph(p -> p.text(LONG)))
                .addSection("Main", main -> main.addParagraph(p -> p.text(LONG))));
        try (XWPFDocument document = export(content)) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFTable rail = cell.getTables().get(0);
            double indent = twips(rail.getCTTbl().getTblPr().getTblInd().getW()) / 20.0;
            double width = twips(rail.getCTTbl().getTblPr().getTblW().getW()) / 20.0;

            assertThat(room(cell)).as("the column").isCloseTo(163, within(0.05));
            assertThat(indent).isEqualTo(-1.5);
            assertThat(1.5 + width).as("from half its border in, it ends where the page ends it, 161pt in")
                    .isCloseTo(161, within(0.05));
            assertThat(twips(rightMargin(rail))).as("its padding less the slack")
                    .isEqualTo(Math.round((4 - SLACK) * 20));
        }
    }

    @Test
    void aHungCardsLineSetPastItsTextTakesBackWhatTheHangGaveUp() throws Exception {
        // A 2pt accent hanging out of its column: the table gives up the whole border, 2pt, to end
        // where the page ends the card. At 7.25pt, written at 7.5, its lines run past its text
        // further than its 1pt margin holds, and take back the slack and those 2pt; at 7.5pt they
        // do not, and the card ends where the page ends it.
        java.util.function.DoubleFunction<Consumer<PageFlowBuilder>> hung = size -> page -> page.addRow(row -> row
                .spacing(12)
                .weights(1, 1)
                .addSection("Card", card -> card.fixedWidth(160).fillColor(SURFACE)
                        .accentLeft(ACCENT.color(), 2).padding(DocumentInsets.of(1))
                        .addParagraph(p -> p.text(FILL).textStyle(DocumentTextStyle.DEFAULT.withSize(size))))
                .addSection("Other", other -> other.addParagraph(p -> p.text("Beside"))));
        double placed = placedWidth(hung.apply(7.25), "Card");
        try (XWPFDocument document = export(hung.apply(7.25))) {
            XWPFTable card = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);
            assertThat(twips(card.getCTTbl().getTblPr().getTblW().getW()))
                    .as("its half border, the slack and the 2pt the hang gave up, back")
                    .isEqualTo(Math.round((placed + 1 + SLACK) * 20));
            assertThat(twips(rightMargin(card))).as("all its padding, the hang's taken back off the table alone")
                    .isEqualTo(20L);
        }
        assertThat(notes(hung.apply(7.25), RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("3pt wider than the page draws it")
                        .contains("beside the lines it sets past its text"));
        try (XWPFDocument document = export(hung.apply(7.5))) {
            XWPFTable card = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);
            assertThat(twips(card.getCTTbl().getTblPr().getTblW().getW()))
                    .as("its half border in, the whole border given up: ending where the page ends it")
                    .isEqualTo(Math.round((placed + 1 - 2) * 20));
            assertThat(twips(rightMargin(card))).as("its padding less the slack")
                    .isEqualTo(Math.round((1 - SLACK) * 20));
        }
        assertThat(notes(hung.apply(7.5), RIGHT_END)).isEmpty();
    }

    @Test
    void aCardBledLeftOfItsCellGivesUpItsBorderAndKeepsItsBleed() throws Exception {
        // Bled 4pt left of its filled panel's text by a margin below zero, a 2pt accent on its edge:
        // the editors move it, text and all, to the cell's text, and the table gives up the border
        // alone, 2pt. Giving up the bleed too would narrow the text the editors move with it.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE).padding(DocumentInsets.of(10))
                .addSection("Card", card -> card.fixedWidth(140).fillColor(DocumentColor.WHITE)
                        .accentLeft(ACCENT.color(), 2).padding(DocumentInsets.of(4))
                        .margin(new DocumentInsets(0, 0, 0, -4)).addParagraph(p -> p.text("Bled"))));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable card = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);
            assertThat(twips(card.getCTTbl().getTblPr().getTblInd().getW())).as("its bleed and half its border out")
                    .isEqualTo(-100L);
            assertThat(twips(card.getCTTbl().getTblPr().getTblW().getW())).as("its half border in, its border given up")
                    .isEqualTo(Math.round((placed + 1 - 2) * 20));
        }
    }

    @Test
    void aCardSetInLessThanHalfItsBorderGivesUpOnlyWhatTheEditorsMoveIt() throws Exception {
        // Half a point in from its filled panel's text, a 2pt accent on its edge: written half a
        // point left of the text, it is moved a point and a half, half its border in from the
        // text, and gives up that much, not the whole border.
        Consumer<PageFlowBuilder> content = page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE).padding(DocumentInsets.of(10))
                .addSection("Card", card -> card.fixedWidth(140).fillColor(DocumentColor.WHITE)
                        .accentLeft(ACCENT.color(), 2).padding(DocumentInsets.of(4))
                        .margin(new DocumentInsets(0, 0, 0, 0.5)).addParagraph(p -> p.text("In"))));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable card = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);
            assertThat(twips(card.getCTTbl().getTblPr().getTblInd().getW())).isEqualTo(-10L);
            assertThat(twips(card.getCTTbl().getTblPr().getTblW().getW())).as("its half border in, 1.5pt given up")
                    .isEqualTo(Math.round((placed + 1 - 1.5) * 20));
        }
    }

    @Test
    void aHungCardFillingItsColumnKeepsWhatItsCellTakesWhateverItsLinesNeed() throws Exception {
        // As wide as its column, the 2pt accent hanging out of it: started a point in, the table
        // would run past the cell, which Word widens to hold it. Its lines at 7.25pt run past its
        // text, but do not take back the 2pt the cell takes: it ends at the cell's text. As wide as
        // its column, it is given no slack.
        Consumer<PageFlowBuilder> content = page -> page.addRow(row -> row
                .spacing(12)
                .weights(1, 1)
                .addSection("Card", card -> card.fixedWidth(COLUMN).fillColor(SURFACE)
                        .accentLeft(ACCENT.color(), 2).padding(DocumentInsets.of(1))
                        .addParagraph(p -> p.text(FILL).textStyle(DocumentTextStyle.DEFAULT.withSize(7.25))))
                .addSection("Other", other -> other.addParagraph(p -> p.text("Beside"))));
        double placed = placedWidth(content, "Card");
        try (XWPFDocument document = export(content)) {
            XWPFTable card = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);
            assertThat(twips(card.getCTTbl().getTblPr().getTblW().getW()))
                    .as("its half border in, the 2pt the cell takes: from a point in, at the cell's text")
                    .isEqualTo(Math.round((placed + 1 - 2) * 20));
        }
    }

    @Test
    void aBorderTheCellsRightMarginHoldsIsNotHeldIn() throws Exception {
        // Two cards filling a row's columns: the gap between them is the left cell's right margin,
        // which holds the left card's border past the cell's text; the right cell has none. Held
        // in at the text, a card's border beside a gap stood 0.6 to 0.9pt left of the page's in
        // Word; held at the cell's edge, 0.3 to 0.6pt right of it.
        Consumer<PageFlowBuilder> content = page -> page.addRow(row -> row
                .spacing(12)
                .weights(1, 1)
                .addSection("Left", left -> left.stroke(ACCENT).padding(DocumentInsets.of(10)).margin(STANDS_IN)
                        .fixedWidth(COLUMN - 2).addParagraph(p -> p.text(LONG)))
                .addSection("Right", right -> right.stroke(ACCENT).padding(DocumentInsets.of(10)).margin(STANDS_IN)
                        .fixedWidth(COLUMN - 2).addParagraph(p -> p.text(LONG))));
        double placed = placedWidth(content, "Left");
        try (XWPFDocument document = export(content)) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            assertThat(twips(cell.getCTTc().getTcPr().getTcMar().getRight().getW())).as("the gap")
                    .isEqualTo(12 * 20L);
            XWPFTable left = cell.getTables().get(0);
            // Its half borders in, less the whole of its left one: the editors start it half that
            // border in from the cell's text (whatTheHangGivesUp).
            assertThat(twips(left.getCTTbl().getTblPr().getTblW().getW()))
                    .as("as wide as the page draws it, its right end where the page's is")
                    .isEqualTo(Math.round(placed * 20));
            // Held in at the cell's text, its border would take half a point of its margin, and
            // its hang give up the rest of its point: held at the gap's edge, the hang gives up
            // the whole point, and that half point comes off its margin.
            assertThat(textWidth(left))
                    .as("the page's text and its two borders, less the point the hang gives up but for that half point")
                    .isCloseTo(placed - 18 - 1 + 0.5, within(0.05));
        }
        List<DocxExportReport.Note> notes = notes(content, RIGHT_BORDER);
        assertThat(notes).as("only the right card's border is held in").hasSize(1);
        assertThat(notes.get(0).path()).contains("Right");
    }

    @Test
    void aCardHungLeftOfItsCellBesideAGapKeepsItsTextsWidth() throws Exception {
        // A card filling its column, its border on the cell's text: Word starts it half that
        // border in, and the table gives up what then runs past the text. Held at the text, its
        // right border took 2.5pt of its margin first and its left side gave up nothing; held at
        // the gap's edge, it takes none, and the left side gives up a point, which comes off the
        // margin, so the text keeps its width.
        Consumer<PageFlowBuilder> content = page -> page.addRow(row -> row
                .spacing(12)
                .weights(1, 1)
                .addSection("Left", left -> left.stroke(ACCENT).padding(DocumentInsets.of(10))
                        .fixedWidth(COLUMN).addParagraph(p -> p.text(LONG)))
                .addSection("Right", right -> right.addParagraph(p -> p.text(LONG))));
        try (XWPFDocument document = export(content)) {
            XWPFTable left = document.getTables().get(0).getRow(0).getCell(0).getTables().get(0);

            assertThat(twips(left.getCTTbl().getTblPr().getTblW().getW())).as("ending at the cell's text as Word starts it")
                    .isEqualTo(Math.round(COLUMN * 20));
            assertThat(textWidth(left)).as("the page's text, and its two borders").isCloseTo(COLUMN - 18, within(0.05));
        }
        assertThat(notes(content, RIGHT_BORDER)).isEmpty();
    }

    @Test
    void aTableItsBorderHoldsNarrowerThanThePageIsNotNamedWider() throws Exception {
        // A point narrower than the right column, given that point of slack. Its 1.75pt padding
        // holds 1.25pt past half its border: holding its border clear of the column's edge takes
        // all of it, the slack included, and the table is still 0.25pt narrower than the page
        // draws the card. Nothing of it reaches further right.
        Consumer<PageFlowBuilder> content = page -> page.addRow(row -> row
                .spacing(12)
                .weights(1, 1)
                .addSection("Left", left -> left.addParagraph(p -> p.text(LONG)))
                .addSection("Right", right -> right.stroke(ACCENT).padding(DocumentInsets.of(1.75))
                        .fixedWidth(COLUMN - 1).addParagraph(p -> p.text(LONG))));
        assertThat(notes(content, RIGHT_END)).isEmpty();
        List<DocxExportReport.Note> held = notes(content, RIGHT_BORDER);
        assertThat(held).hasSize(1);
        assertThat(held.get(0).detail()).startsWith("its table written 0.25pt narrower than the page draws it");
    }

    /** A filled card narrower than its page, padded 4pt, its lines filled at the size given. */
    private static Consumer<PageFlowBuilder> card(double size) {
        return page -> page.addSection("Card", card -> card
                .fixedWidth(300)
                .fillColor(SURFACE)
                .padding(DocumentInsets.of(4))
                .addParagraph(p -> p.text(FILL).textStyle(DocumentTextStyle.DEFAULT.withSize(size))));
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 30, content);
    }

    private static List<DocxExportReport.Note> notes(Consumer<PageFlowBuilder> content, String subject)
            throws Exception {
        DocxExportReport report = DocxExports.report(400, 400, 30, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report.bySubject().getOrDefault(subject, List.of());
    }

    /** How wide the layout placed the section of that name, in points. */
    private static double placedWidth(Consumer<PageFlowBuilder> content, String name) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().nodes().stream()
                    .filter(node -> node.nodeKind().equals("SectionNode") && node.path().endsWith(name + "[0]")
                                    || node.nodeKind().equals("SectionNode") && node.path().contains("/" + name + "["))
                    .findFirst()
                    .orElseThrow()
                    .placementWidth();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** How wide the page sets each line of the document's paragraphs, in points. */
    private static List<Double> lineWidths(Consumer<PageFlowBuilder> content) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().fragments().stream()
                    .map(com.demcha.compose.document.layout.PlacedFragment::payload)
                    .filter(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::isInstance)
                    .map(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream())
                    .map(line -> line.spans().stream().mapToDouble(span -> span.width()).sum())
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The right margin of a panel's cell, as written. */
    private static Object rightMargin(XWPFTable table) {
        return table.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar().getRight().getW();
    }

    /** How wide a panel's table leaves its text: its width less its cell's margins, in points. */
    private static double textWidth(XWPFTable panel) {
        var properties = panel.getRow(0).getCell(0).getCTTc().getTcPr();
        return (twips(panel.getCTTbl().getTblPr().getTblW().getW()) - twips(properties.getTcMar().getLeft().getW())
                - twips(properties.getTcMar().getRight().getW())) / 20.0;
    }

    /** What a cell leaves its content across, as Word measures it: its width less its margins. */
    private static double room(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return (twips(properties.getTcW().getW()) - twips(properties.getTcMar().getLeft().getW())
                - twips(properties.getTcMar().getRight().getW())) / 20.0;
    }

    private static long twips(Object measure) {
        return DocxTwips.of(measure);
    }
}
