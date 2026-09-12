package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_STROKE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CELL_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CELL_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CELL_STYLE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.COLUMN_SHARES;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.FIGURE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HAIRLINE_STRONG;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ITEM_SUB_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TABLE_INSET;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TABLE_W;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.topBearing;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.rule;

/**
 * The line-item table, inside its own rounded card.
 *
 * <h2>Six columns inside one</h2>
 *
 * <p>A cell's stroke draws all four of its edges, so a six-column table would
 * draw five verticals this design does not have. The table is one column wide
 * and each cell carries a composed row of six, with the rule under a row drawn
 * inside that row's own content — which is also what lets the last row leave the
 * rule out and take the card's lower padding instead.</p>
 */
final class ObsidianLines {

    private ObsidianLines() {
    }

    /**
     * The table.
     *
     * @param page         the page flow
     * @param serviceLines the lines and their column captions
     * @param currencyCode the code every figure carries as a mark
     */
    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencyCode) {
        List<InvoiceServiceLines.Line> lines = serviceLines.lines();
        page.addSection("LineItemsCard", card -> {
            card.spacing(0)
                    .fillColor(SURFACE)
                    .stroke(CARD_STROKE)
                    .cornerRadius(CARD_RADIUS)
                    .padding(new DocumentInsets(0, TABLE_INSET, 0, TABLE_INSET))
                    .margin(new DocumentInsets(px(28), 0, 0, 0));
            card.addTable(table -> {
                table.name("LineItems")
                        .width(TABLE_W)
                        .columns(DocumentTableColumn.fixed(TABLE_W))
                        .defaultCellStyle(CELL_STYLE);
                renderHeader(table, serviceLines.columns());
                for (int i = 0; i < lines.size(); i++) {
                    table.rowCells(DocumentTableCell
                            .node(lineCell(lines.get(i), i, i == lines.size() - 1, currencyCode))
                            .withStyle(CELL_STYLE));
                }
            });
        });
    }

    private static void renderHeader(TableBuilder table, InvoiceServiceLines.Columns columns) {
        SectionBuilder cell = new SectionBuilder();
        cell.name("LineItemsHeaderCell").spacing(0);
        cell.addRow("LineItemsHeader", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(capTop(596 - 573, TABLE_HEAD_SIZE, false))
                    .columns(columnWidths());
            headCell(row, "HeadIndex", columns.index(), TextAlign.LEFT);
            headCell(row, "HeadDescription", columns.description(), TextAlign.LEFT);
            headCell(row, "HeadQuantity", columns.quantity(), TextAlign.CENTER);
            headCell(row, "HeadUnitPrice", columns.unitPrice(), TextAlign.RIGHT);
            headCell(row, "HeadTax", columns.vat(), TextAlign.RIGHT);
            headCell(row, "HeadAmount", columns.amount(), TextAlign.RIGHT);
        });
        cell.addLine(line -> rule(line, "LineItemsHeaderRule", TABLE_W, HAIRLINE_STRONG)
                .margin(new DocumentInsets(
                        px(626 - 596) - LINE_BOX * TABLE_HEAD_SIZE
                                + topBearing(TABLE_HEAD_SIZE, false),
                        0, 0, 0)));
        table.headerCells(DocumentTableCell.node(cell.build()).withStyle(CELL_STYLE))
                .repeatHeader();
    }

    private static void headCell(RowBuilder row, String name, String label, TextAlign align) {
        row.addParagraph(p -> p
                .name(name)
                .text(label)
                .textStyle(plain(TABLE_HEAD_SIZE, MUTED))
                .align(align)
                .margin(new DocumentInsets(0,
                        align == TextAlign.RIGHT ? CELL_PAD_R : 0, 0,
                        align == TextAlign.LEFT ? CELL_PAD_L : 0)));
    }

    private static DocumentNode lineCell(InvoiceServiceLines.Line line, int index, boolean last,
                                         String currencyCode) {
        int number = line.lineNumber() > 0 ? line.lineNumber() : index + 1;
        SectionBuilder cell = new SectionBuilder();
        cell.name("LineCell_" + index).spacing(0);
        cell.addRow("Line_" + index, row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    // A row's title sits a measured distance below the rule above
                    // it every time, and the rule's own thickness is part of that.
                    .margin(new DocumentInsets(
                            px(18) - RULE_BOX - topBearing(ITEM_TITLE_SIZE, false), 0, 0, 0))
                    .columns(columnWidths());
            row.addParagraph(p -> p
                    .name("LineIndex_" + index)
                    .text(Integer.toString(number))
                    .textStyle(plain(ITEM_TITLE_SIZE, MUTED))
                    .margin(new DocumentInsets(0, 0, 0, CELL_PAD_L)));
            row.addSection("LineDescription_" + index, text -> {
                text.spacing(0).padding(new DocumentInsets(0, 0, 0, CELL_PAD_L));
                text.addParagraph(p -> p
                        .name("LineTitle_" + index)
                        .text(line.title())
                        .textStyle(plain(ITEM_TITLE_SIZE, INK)));
                text.addParagraph(p -> p
                        .name("LineSubtitle_" + index)
                        .text(line.description())
                        .textStyle(plain(ITEM_SUB_SIZE, MUTED))
                        .margin(new DocumentInsets(Math.max(0, capGap(
                                669 - 644, ITEM_TITLE_SIZE, false, ITEM_SUB_SIZE, false)),
                                0, 0, 0)));
            });
            valueCell(row, "LineQuantity_" + index,
                    ObsidianText.quantity(line.quantity()), TextAlign.CENTER);
            valueCell(row, "LineUnitPrice_" + index,
                    ObsidianText.money(currencyCode, line.unitPrice()), TextAlign.RIGHT);
            valueCell(row, "LineTax_" + index,
                    ObsidianText.lineTax(currencyCode, line.quantity(), line.unitPrice(),
                            line.amount()),
                    TextAlign.RIGHT);
            valueCell(row, "LineAmount_" + index,
                    ObsidianText.money(currencyCode, line.amount()), TextAlign.RIGHT);
        });
        if (last) {
            cell.addSpacer(spacer -> spacer.height(px(983 - 967)));
        } else {
            cell.addLine(line2 -> rule(line2, "LineRule_" + index, TABLE_W, HAIRLINE)
                    .margin(new DocumentInsets(
                            Math.max(0, px(697.5 - 669) - LINE_BOX * ITEM_SUB_SIZE
                                    + topBearing(ITEM_SUB_SIZE, false)),
                            0, 0, 0)));
        }
        return cell.build();
    }

    private static void valueCell(RowBuilder row, String name, String value, TextAlign align) {
        row.addParagraph(p -> p
                .name(name)
                .text(value)
                .textStyle(plain(FIGURE_SIZE, INK))
                .align(align)
                .margin(new DocumentInsets(0, align == TextAlign.RIGHT ? CELL_PAD_R : 0, 0, 0)));
    }

    /** The six widths, with the last taking the remainder so they sum exactly. */
    private static DocumentRowColumn[] columnWidths() {
        DocumentRowColumn[] columns = new DocumentRowColumn[COLUMN_SHARES.length];
        double assigned = 0;
        for (int i = 0; i < COLUMN_SHARES.length - 1; i++) {
            double width = TABLE_W * COLUMN_SHARES[i];
            assigned += width;
            columns[i] = DocumentRowColumn.fixed(width);
        }
        columns[COLUMN_SHARES.length - 1] = DocumentRowColumn.fixed(TABLE_W - assigned);
        return columns;
    }
}
