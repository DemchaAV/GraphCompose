package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.rota.RotaDay;
import com.demcha.compose.document.templates.data.rota.RotaGroup;
import com.demcha.compose.document.templates.data.rota.RotaLegend;
import com.demcha.compose.document.templates.data.rota.RotaShift;
import com.demcha.compose.document.templates.data.rota.RotaStaff;
import com.demcha.compose.document.templates.data.rota.ShiftEmphasis;
import com.demcha.compose.document.templates.data.rota.ShiftStatus;
import com.demcha.compose.document.templates.data.rota.StructuredRotaData;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.BAND_ICON_SIZE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.BAND_PAD_X;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.BAND_PAD_Y;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CELL_PAD_Y;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_CORNER_RADIUS;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_HEIGHT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_STACK_GAP;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_STACK_HEIGHT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.COVERS_LABEL;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.COVERS_PAD_Y;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.COVERS_TAG;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.COVERS_VALUE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.GRID_RULE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.GROUP_LABEL;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LABEL_PAD_X;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LEGEND_CHIP_HEIGHT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LEGEND_LABEL;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LEGEND_PAD_Y;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.NAVY;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.NAVY_RULE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAPER;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.SINGLE_SLOT_PAD_Y;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.STAFF_NAME;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.ZEBRA_TINT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.chipCellStyle;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.colorFor;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.labelOnFillFor;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.slotStyle;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.textCellStyle;

/**
 * Every row of the sheet below the header: what the colours mean, how busy each
 * day is, and then the bands of staff and their days.
 */
final class CobaltRows {

    private CobaltRows() {
    }

    /**
     * The strip of swatches that says what the colours mean.
     *
     * <p>As many strips as it takes. A site documents as many statuses as it
     * uses and the sheet has as many columns as it has days; where the first
     * outruns the second the legend runs onto another row rather than losing
     * its tail — a status a reader cannot look up is worse than a second
     * strip.</p>
     */
    static void renderLegend(TableBuilder table, RotaLegend legend, CobaltStyles.Grid grid) {
        if (!legend.isPresent()) {
            return;
        }
        List<RotaLegend.Entry> entries = legend.entries();
        int perRow = Math.max(1, grid.days());
        int strips = Math.max(1, (entries.size() + perRow - 1) / perRow);
        for (int strip = 0; strip < strips; strip++) {
            List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
            // Only the first strip is labelled; the ones under it continue it.
            cells.add(DocumentTableCell.text(strip == 0 ? legend.label() : "")
                    .withStyle(textCellStyle(LEGEND_LABEL, DocumentTableTextAnchor.CENTER,
                            LEGEND_PAD_Y, LABEL_PAD_X, GRID_RULE, PAPER)));
            int from = strip * perRow;
            int to = Math.min(entries.size(), from + perRow);
            for (int i = from; i < to; i++) {
                cells.add(DocumentTableCell.node(swatchNode(entries.get(i), grid))
                        .withStyle(chipCellStyle(grid.legendInset(), LEGEND_PAD_Y,
                                GRID_RULE, PAPER)));
            }
            squareOff(cells, grid, GRID_RULE, PAPER, LEGEND_PAD_Y);
            table.rowCells(cells);
        }
    }

    private static DocumentNode swatchNode(RotaLegend.Entry entry, CobaltStyles.Grid grid) {
        return new ShapeContainerBuilder()
                .name("LegendSwatch")
                .roundedRect(grid.legendWidth(), LEGEND_CHIP_HEIGHT, CHIP_CORNER_RADIUS)
                .fillColor(colorFor(entry.status()))
                .center(CobaltChips.label(entry.label(),
                        labelOnFillFor(entry.status(), false), grid.legendWidth()))
                .build();
    }

    /**
     * How many each day is expecting, split across the two services.
     *
     * <p>Drawn only when the rota says. A navy bar with a blank label over seven
     * empty boxes is two rows of a twelve-person sheet spent on nothing, and the
     * sheet is short of rows as it is.</p>
     */
    static void renderCovers(TableBuilder table, StructuredRotaData rota,
                             CobaltStyles.Grid grid) {
        RotaLegend legend = rota.legend();
        boolean stated = !legend.coversLabel().isBlank()
                || rota.days().stream().anyMatch(day -> day.covers().isPresent());
        if (!stated) {
            return;
        }
        DocumentTableStyle value = DocumentTableStyle.builder()
                .padding(new DocumentInsets(
                        COVERS_PAD_Y, grid.cellPadX(), COVERS_PAD_Y, grid.cellPadX()))
                .fillColor(PAPER)
                .stroke(NAVY_RULE)
                .textStyle(COVERS_VALUE)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
        List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
        cells.add(DocumentTableCell.text(legend.coversLabel())
                .withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(
                                COVERS_PAD_Y, LABEL_PAD_X, COVERS_PAD_Y, LABEL_PAD_X))
                        .fillColor(NAVY)
                        .stroke(NAVY_RULE)
                        .textStyle(COVERS_LABEL)
                        .textAnchor(DocumentTableTextAnchor.CENTER)
                        .lineSpacing(0)
                        .build()));
        for (RotaDay day : rota.days()) {
            cells.add(DocumentTableCell.node(coversNode(day, legend)).withStyle(value));
        }
        squareOff(cells, grid, NAVY_RULE, PAPER, COVERS_PAD_Y);
        table.rowCells(cells);
    }

    /**
     * The two counts on one line, each behind its own mark.
     *
     * <p>Five runs and not one string: the marks are set smaller and paler than
     * the figures, which is what stops a reader having to know from elsewhere
     * which count is which.</p>
     */
    private static DocumentNode coversNode(RotaDay day, RotaLegend legend) {
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .name("CoversValue")
                .textStyle(COVERS_VALUE)
                .align(TextAlign.CENTER)
                .lineSpacing(0)
                .margin(DocumentInsets.zero());
        paragraph.rich(rich -> rich
                .style(legend.coversLunchLabel() + " ", COVERS_TAG)
                .style(day.covers().lunch(), COVERS_VALUE)
                .style("  /  ", COVERS_TAG)
                .style(legend.coversDinnerLabel() + " ", COVERS_TAG)
                .style(day.covers().dinner(), COVERS_VALUE));
        return paragraph.build();
    }

    /** The navy strip that opens a band of staff. */
    static void renderBand(TableBuilder table, RotaGroup group, CobaltStyles.Grid grid) {
        RowBuilder band = new RowBuilder()
                .name("GroupBand")
                .verticalAlign(RowVerticalAlign.CENTER);
        if (CobaltIcons.has(group.icon())) {
            band.weights(BAND_ICON_SIZE, CONTENT_WIDTH - BAND_ICON_SIZE)
                    .add(CobaltIcons.icon(group.icon()).node(BAND_ICON_SIZE));
        }
        band.addParagraph(p -> p
                .name("GroupLabel")
                .text(group.label())
                .textStyle(GROUP_LABEL)
                .align(TextAlign.LEFT)
                .lineSpacing(0));
        table.rowCells(DocumentTableCell.node(band.build())
                .withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(BAND_PAD_Y, BAND_PAD_X, BAND_PAD_Y, BAND_PAD_X))
                        .fillColor(NAVY)
                        .stroke(NAVY_RULE)
                        .textStyle(GROUP_LABEL)
                        .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                        .lineSpacing(0)
                        .build())
                .colSpan(grid.columnCount()));
    }

    /**
     * One person and their days.
     *
     * <p>The row's fill is threaded by hand into every cell and on into the
     * plain chips inside them, rather than left to the table's own striping: a
     * stripe set on the table would also stripe the header, the legend, the
     * covers and the bands, and each of those otherwise paints its own paper
     * over it.</p>
     *
     * @param table  the sheet's one table
     * @param staff  the person
     * @param grid   the sheet's geometry, which says how many days to walk
     * @param stripe whether this row takes the tint
     */
    static void renderStaff(TableBuilder table, RotaStaff staff, CobaltStyles.Grid grid,
                            boolean stripe) {
        DocumentColor rowFill = stripe ? ZEBRA_TINT : PAPER;
        List<DocumentTableCell> cells = new ArrayList<>(grid.columnCount());
        cells.add(DocumentTableCell.text(staff.name())
                .withStyle(textCellStyle(STAFF_NAME, DocumentTableTextAnchor.CENTER,
                        CELL_PAD_Y, LABEL_PAD_X, GRID_RULE, rowFill)));
        for (int day = 0; day < grid.days(); day++) {
            cells.add(shiftCell(staff.day(day), grid, rowFill));
        }
        table.rowCells(cells);
    }

    /**
     * One day of one person: a block of the sheet's own height, holding none,
     * one, or a stacked pair of chips.
     *
     * <p>The block is a nested one-column table because a cell places a node
     * child at its top: without a block of a stated height a short chip in a row
     * made tall by a neighbour would hang with all the slack beneath it.</p>
     */
    private static DocumentTableCell shiftCell(List<RotaShift> shifts, CobaltStyles.Grid grid,
                                               DocumentColor rowFill) {
        return DocumentTableCell.node(shiftBlock(shifts, grid, rowFill))
                .withStyle(chipCellStyle(grid.chipInsetX(), CELL_PAD_Y * 0.6, GRID_RULE, rowFill));
    }

    private static DocumentNode shiftBlock(List<RotaShift> shifts, CobaltStyles.Grid grid,
                                           DocumentColor rowFill) {
        double width = grid.chipWidth();
        TableBuilder block = new TableBuilder()
                .name("ShiftBlock")
                .columns(DocumentTableColumn.fixed(width))
                .padding(DocumentInsets.zero())
                .margin(DocumentInsets.zero());
        if (shifts.isEmpty()) {
            // An empty day is still a block of the same height, so the row it
            // sits in does not collapse around it.
            block.rowCells(DocumentTableCell.node(CobaltChips.chip(
                            "", ShiftStatus.NONE, ShiftEmphasis.PLAIN, width, CHIP_HEIGHT,
                            false, rowFill))
                    .withStyle(slotStyle(SINGLE_SLOT_PAD_Y, rowFill)));
            return block.build();
        }
        if (shifts.size() == 1) {
            RotaShift only = shifts.get(0);
            block.rowCells(DocumentTableCell.node(CobaltChips.chip(
                            only.text(), only.status(), only.emphasis(), width, CHIP_HEIGHT,
                            true, rowFill))
                    .withStyle(slotStyle(SINGLE_SLOT_PAD_Y, rowFill)));
            return block.build();
        }
        for (RotaShift shift : shifts) {
            block.rowCells(DocumentTableCell.node(CobaltChips.chip(
                            shift.text(), shift.status(), shift.emphasis(), width,
                            CHIP_STACK_HEIGHT, false, rowFill))
                    .withStyle(slotStyle(CHIP_STACK_GAP / 2, rowFill)));
        }
        return block.build();
    }

    /**
     * Makes a row exactly as wide as the sheet.
     *
     * <p>The legend documents as many statuses as a site uses and the sheet has
     * as many columns as it has days; the two are not the same number. A short
     * row is closed with empty cells carrying its own rule, and a long one is
     * cut — either way the table stays square, which a ragged row would
     * not.</p>
     */
    private static void squareOff(List<DocumentTableCell> cells, CobaltStyles.Grid grid,
                                  DocumentStroke rule, DocumentColor fill, double padY) {
        while (cells.size() < grid.columnCount()) {
            cells.add(DocumentTableCell.text("")
                    .withStyle(textCellStyle(LEGEND_LABEL, DocumentTableTextAnchor.CENTER,
                            padY, grid.cellPadX(), rule, fill)));
        }
        while (cells.size() > grid.columnCount()) {
            cells.remove(cells.size() - 1);
        }
    }
}
