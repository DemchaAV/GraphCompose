package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.rota.RotaDay;
import com.demcha.compose.document.templates.data.rota.RotaVenue;
import com.demcha.compose.document.templates.data.rota.StructuredRotaData;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.BODY_TOP_RULE_TEXT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NAME;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NAME_PAD_BOTTOM;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NAME_PAD_TOP;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NOTE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NOTE_PAD_BOTTOM;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_NOTE_PAD_TOP;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.DAY_ORDINAL_SUFFIX;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.GRID_LINE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.GRID_RULE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.HEADER_ROW_COUNT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LOCKUP_LINE_SPACING;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.MASTHEAD_RULE_TEXT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.NAVY;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.NAVY_RULE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAPER;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.WORDMARK;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.WORDMARK_SUB;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.labelHeaderStyle;

/**
 * The four rows that open the sheet: a navy rule, the lockup beside the day
 * names, whatever is happening on each day, and the rule that closes the header
 * and opens the body.
 *
 * <p>Four real table rows and not one row with nested tables in it: a nested
 * table in a header cell draws its own rules at its own heights, and the day
 * columns, holding notes of one, two and three lines, would then close with a
 * rule drawn at three different heights.</p>
 */
final class CobaltHeader {

    private CobaltHeader() {
    }

    /**
     * The header, declared as repeating.
     *
     * <p>The count is not optional. {@code repeatHeader()} with no argument
     * repeats one row, which on a four-row header leaves a continuation page
     * carrying unlabelled day columns under a stray rule.</p>
     *
     * @param table the sheet's one table
     * @param rota  the document
     * @param grid  the sheet's geometry
     */
    static void render(TableBuilder table, StructuredRotaData rota, CobaltStyles.Grid grid) {
        table.headerCells(ruleCells(NAVY, NAVY_RULE, MASTHEAD_RULE_TEXT, grid));
        table.headerCells(dayNameCells(rota, grid));
        table.headerCells(dayNoteCells(rota, grid));
        table.headerCells(ruleCells(GRID_LINE, GRID_RULE, BODY_TOP_RULE_TEXT, grid));
        table.repeatHeader(HEADER_ROW_COUNT);
    }

    /**
     * A row that is only a rule.
     *
     * <p>An empty cell is as tall as its own font, so the rule's thickness is
     * carried by the text style rather than by a height nobody can set. Every
     * column is filled, including the label column: the rule runs the width of
     * the sheet.</p>
     */
    private static List<DocumentTableCell> ruleCells(DocumentColor fill, DocumentStroke stroke,
                                                     DocumentTextStyle thickness,
                                                     CobaltStyles.Grid grid) {
        DocumentTableStyle rule = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .fillColor(fill)
                .stroke(stroke)
                .textStyle(thickness)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
        List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
        for (int column = 0; column < grid.columnCount(); column++) {
            cells.add(DocumentTableCell.text("").withStyle(rule));
        }
        return cells;
    }

    /** The lockup in the label column, then one heading per day. */
    private static List<DocumentTableCell> dayNameCells(StructuredRotaData rota,
                                                        CobaltStyles.Grid grid) {
        DocumentTableStyle style = DocumentTableStyle.builder()
                .padding(new DocumentInsets(
                        DAY_NAME_PAD_TOP, grid.cellPadX(), DAY_NAME_PAD_BOTTOM, grid.cellPadX()))
                .fillColor(PAPER)
                .stroke(GRID_RULE)
                .textStyle(DAY_NAME)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
        List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
        cells.add(DocumentTableCell.node(lockupNode(rota.venue())).withStyle(labelHeaderStyle()));
        for (RotaDay day : rota.days()) {
            cells.add(DocumentTableCell.node(dayNameNode(day)).withStyle(style));
        }
        return cells;
    }

    /**
     * The venue's mark, which sits in the label column's header rather than in a
     * masthead of its own — the sheet is one table, and a masthead above it
     * would be a second structure to keep aligned with the first.
     */
    private static DocumentNode lockupNode(RotaVenue venue) {
        SectionBuilder lockup = new SectionBuilder();
        lockup.name("BrandLockup").spacing(0);
        boolean named = !venue.wordmark().isBlank();
        if (named) {
            lockup.addParagraph(p -> p
                    .name("Wordmark")
                    .text(venue.wordmark())
                    .textStyle(WORDMARK)
                    .align(TextAlign.LEFT)
                    .lineSpacing(0));
        }
        if (!venue.wordmarkSub().isBlank()) {
            // The negative margin closes up the gap under the wordmark's own
            // line box. With no wordmark there is no gap to close, and applying
            // it anyway would pull the subtitle up through the cell's padding
            // and into the rule above it.
            double closeUp = named ? LOCKUP_LINE_SPACING : 0;
            lockup.addParagraph(p -> p
                    .name("WordmarkSub")
                    .text(venue.wordmarkSub())
                    .textStyle(WORDMARK_SUB)
                    .align(TextAlign.LEFT)
                    .lineSpacing(0)
                    .margin(new DocumentInsets(closeUp, 0, 0, 0)));
        }
        return lockup.build();
    }

    /**
     * One day's heading: the weekday and the date, with the ordinal's tail set
     * smaller.
     *
     * <p>Two runs of one paragraph rather than two paragraphs, because the tail
     * belongs on the same line as what it follows.</p>
     */
    private static DocumentNode dayNameNode(RotaDay day) {
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .name("DayName")
                .textStyle(DAY_NAME)
                .align(TextAlign.CENTER)
                .lineSpacing(0);
        paragraph.rich(rich -> rich
                .style(day.name() + " " + day.ordinal(), DAY_NAME)
                .style(day.ordinalSuffix(), DAY_ORDINAL_SUFFIX));
        return paragraph.build();
    }

    /** What else is happening that day, under its heading. */
    private static List<DocumentTableCell> dayNoteCells(StructuredRotaData rota,
                                                        CobaltStyles.Grid grid) {
        DocumentTableStyle style = DocumentTableStyle.builder()
                .padding(new DocumentInsets(
                        DAY_NOTE_PAD_TOP, grid.cellPadX(), DAY_NOTE_PAD_BOTTOM, grid.cellPadX()))
                .fillColor(PAPER)
                .stroke(GRID_RULE)
                .textStyle(DAY_NOTE)
                .textAnchor(DocumentTableTextAnchor.TOP_LEFT)
                .lineSpacing(1.5)
                .build();
        List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
        cells.add(DocumentTableCell.text("").withStyle(labelHeaderStyle()));
        for (RotaDay day : rota.days()) {
            cells.add(DocumentTableCell.node(dayNoteNode(day)).withStyle(style));
        }
        return cells;
    }

    /**
     * A note as a paragraph node rather than as cell text: a text cell asks its
     * fixed column for its natural width, and a note long enough to need two
     * lines then overflows instead of wrapping.
     */
    private static DocumentNode dayNoteNode(RotaDay day) {
        return new ParagraphBuilder()
                .name("DayNote")
                .text(day.note())
                .textStyle(DAY_NOTE)
                .align(TextAlign.CENTER)
                .lineSpacing(1.5)
                .margin(DocumentInsets.zero())
                .build();
    }
}
