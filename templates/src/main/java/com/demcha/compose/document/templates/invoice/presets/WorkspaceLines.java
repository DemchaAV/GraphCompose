package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CELL_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CELL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.COLUMN_SHARES;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HEADER_CELL_STYLE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HEAD_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_CELL_STYLE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_SUB_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_TEXT_GAP;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_TILE_D;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.blockGap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.style;

/**
 * The service lines.
 *
 * <h2>One column, not five</h2>
 *
 * <p>The design's table has an outer box and a rule between every row and
 * <b>no interior verticals</b> — which is exactly the rule topology a
 * single-column table draws, because a cell strokes all four of its own edges
 * and a one-column table has no interior vertical edge to draw. The five
 * columns are a row inside each cell, laid out on shares of the table's width,
 * so they stay aligned down the page without the engine drawing a line between
 * them.</p>
 *
 * <p>Reaching the same topology the other way — unstroked cells, separators as
 * full-width rule rows, and the outer box from a stroked section wrapped round
 * the table — is wrong on a document that paginates: a section's box fills its
 * page fragment rather than hugging its rows, so every continuation page would
 * end with an empty bordered strip below its last line, and on page one that
 * strip's bottom edge lands on the page number.</p>
 */
final class WorkspaceLines {

    private WorkspaceLines() {
    }

    /**
     * Draws the table.
     *
     * @param page         the page flow
     * @param serviceLines the columns and the lines
     * @param currencyCode the ISO code the figures are stated in
     */
    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencyCode) {
        page.addSection("LineItemsBox", box -> {
            box.spacing(0)
                    .padding(DocumentInsets.zero())
                    .margin(new DocumentInsets(blockGap(45, BODY_SIZE, 0), 0, px(27), 0));
            box.addTable(table -> {
                table.name("LineItems")
                        .width(CONTENT_W)
                        .columns(DocumentTableColumn.fixed(CONTENT_W));
                renderHeader(table, serviceLines.columns(), currencyCode);
                List<InvoiceServiceLines.Line> lines = serviceLines.lines();
                for (int i = 0; i < lines.size(); i++) {
                    table.rowCells(DocumentTableCell
                            .node(renderLine(lines.get(i), i))
                            .withStyle(ITEM_CELL_STYLE));
                }
            });
        });
    }

    /**
     * The header repeats on every page its table reaches. It is one cell like
     * every other row, so its own stroke draws the table's top edge and the rule
     * under the band — in the accent's border step, which is what the design
     * tints the band's edges with.
     */
    private static void renderHeader(TableBuilder table, InvoiceServiceLines.Columns columns,
                                     String currencyCode) {
        RowBuilder row = new RowBuilder();
        row.name("LineItemsHeader").spacing(0).columns(columnWidths());
        row.addParagraph(p -> p
                .name("HeadDescription")
                .text(columns.description())
                .textStyle(style(TABLE_HEAD_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                .margin(new DocumentInsets(0, 0, 0, HEAD_PAD_L)));
        headCell(row, "HeadPlan", columns.servicePeriod());
        headCell(row, "HeadQuantity", columns.quantity());
        // The currency is named once per money column rather than on every
        // figure under it, which is where this design states it.
        headCell(row, "HeadUnitPrice",
                WorkspaceText.labelWithCurrency(columns.unitPrice(), currencyCode));
        headCell(row, "HeadAmount",
                WorkspaceText.labelWithCurrency(columns.amount(), currencyCode));
        table.headerCells(DocumentTableCell.node(row.build()).withStyle(HEADER_CELL_STYLE))
                .repeatHeader();
    }

    private static void headCell(RowBuilder row, String name, String label) {
        row.addParagraph(p -> p
                .name(name)
                .text(label)
                .textStyle(style(TABLE_HEAD_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                .align(TextAlign.CENTER));
    }

    /** The five columns, as a row inside the row's single cell. */
    private static DocumentNode renderLine(InvoiceServiceLines.Line line, int index) {
        int n = index + 1;
        RowBuilder row = new RowBuilder();
        row.name("Item" + n)
                .spacing(0)
                .verticalAlign(RowVerticalAlign.CENTER)
                .columns(columnWidths());
        row.addSection("ItemDescription" + n, cell -> {
            cell.spacing(0).padding(new DocumentInsets(0, 0, 0, CELL_PAD_L));
            // Mark and text are a horizontal pair inside a row cell, which the
            // layout compiler refuses as a bare row.
            WorkspaceWidgets.layeredRow(cell, "ItemBody" + n, body -> {
                body.spacing(ITEM_TEXT_GAP)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .columns(DocumentRowColumn.fixed(ITEM_TILE_D),
                                DocumentRowColumn.weight(1));
                body.addSection("ItemGlyph" + n, glyph -> {
                    glyph.spacing(0);
                    if (!line.icon().isBlank()) {
                        glyph.add(WorkspaceWidgets.tile(line.icon()));
                    }
                });
                body.addSection("ItemText" + n, text -> {
                    text.spacing(gap(25, ITEM_TITLE_SIZE));
                    text.addParagraph(p -> p
                            .name("ItemTitle" + n)
                            .text(line.title())
                            .textStyle(style(ITEM_TITLE_SIZE, INK,
                                    DocumentTextDecoration.BOLD)));
                    if (!line.description().isBlank()) {
                        text.addParagraph(p -> p
                                .name("ItemSubtitle" + n)
                                .text(line.description())
                                .textStyle(style(ITEM_SUB_SIZE, MUTED,
                                        DocumentTextDecoration.DEFAULT)));
                    }
                });
            });
        });
        valueCell(row, "ItemPlan" + n, line.servicePeriod());
        valueCell(row, "ItemQuantity" + n,
                WorkspaceText.quantityWithUnit(line.quantity(), line.unit()));
        valueCell(row, "ItemUnitPrice" + n, WorkspaceText.money(line.unitPrice()));
        valueCell(row, "ItemAmount" + n, WorkspaceText.money(line.amount()));
        return row.build();
    }

    private static void valueCell(RowBuilder row, String name, String value) {
        row.addParagraph(p -> p
                .name(name)
                .text(value)
                .textStyle(style(CELL_SIZE, MUTED, DocumentTextDecoration.DEFAULT))
                .align(TextAlign.CENTER));
    }

    /**
     * The five column widths as shares of the table's own width. The last takes
     * whatever the others leave: five independently rounded shares sum to the
     * width plus a half-thousandth of a point, and the engine refuses a row
     * narrower than its columns.
     */
    private static DocumentRowColumn[] columnWidths() {
        DocumentRowColumn[] columns = new DocumentRowColumn[COLUMN_SHARES.length];
        double assigned = 0;
        for (int i = 0; i < COLUMN_SHARES.length - 1; i++) {
            double width = CONTENT_W * COLUMN_SHARES[i];
            assigned += width;
            columns[i] = DocumentRowColumn.fixed(width);
        }
        columns[COLUMN_SHARES.length - 1] = DocumentRowColumn.fixed(CONTENT_W - assigned);
        return columns;
    }
}
