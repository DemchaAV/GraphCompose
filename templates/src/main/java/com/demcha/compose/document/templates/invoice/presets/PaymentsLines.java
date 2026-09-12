package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.AMOUNT_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CELL_AMOUNT_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CELL_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CELL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.COLUMN_SHARES;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DESC_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.HEADER_CELL_STYLE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_CELL_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_CELL_STYLE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_DISC_GAP;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_SUB_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_SUB_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_TEXT_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TABLE_HEAD_SMALL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_TOP_BEARING;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

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
 * <p>The alternative — unstroked cells, separators as line rows, and the outer
 * box from a stroked section wrapped round the table — is wrong on a document
 * that paginates: a section's box fills its page fragment rather than hugging
 * its rows, so every continuation page would end with an empty bordered strip
 * below its last line. Cell borders have no such problem, because the engine
 * redraws each page's cell edges from the same styles and every fragment closes
 * on its own last row.</p>
 */
final class PaymentsLines {

    private PaymentsLines() {
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
                    .margin(new DocumentInsets(px(596 - 567), 0, px(1060 - 1042), 0));
            box.addTable(table -> {
                table.name("LineItems")
                        .width(CONTENT_W)
                        .columns(DocumentTableColumn.fixed(CONTENT_W));
                renderHeader(table, serviceLines.columns(), currencyCode);
                List<InvoiceServiceLines.Line> lines = serviceLines.lines();
                for (int i = 0; i < lines.size(); i++) {
                    table.rowCells(DocumentTableCell
                            .node(renderLine(lines.get(i), i, currencyCode))
                            .withStyle(ITEM_CELL_STYLE));
                }
            });
        });
    }

    /**
     * The header repeats on every page its table reaches. It is one cell like
     * every other row, so its own stroke draws the table's top edge and the rule
     * under the band, and its fill is the tint.
     */
    private static void renderHeader(TableBuilder table, InvoiceServiceLines.Columns columns,
                                     String currencyCode) {
        RowBuilder row = new RowBuilder();
        row.name("LineItemsHeader").spacing(0).columns(columnWidths());
        row.addParagraph(p -> p
                .name("HeadDescription")
                .text(columns.description())
                .textStyle(style(TABLE_HEAD_SIZE, INK, DocumentTextDecoration.BOLD))
                .margin(new DocumentInsets(0, 0, 0, DESC_PAD_L)));
        headCell(row, "HeadQuantity", columns.quantity(), TextAlign.CENTER, 0);
        headCell(row, "HeadUnitPrice", columns.unitPrice(), TextAlign.RIGHT, CELL_PAD_R);
        headCell(row, "HeadVat", columns.vat(), TextAlign.CENTER, 0);
        // Two runs: the currency qualifier is set smaller than the label it
        // follows, which one paragraph of one style cannot express. The design
        // names the currency once, here, rather than on every amount.
        row.addParagraph(p -> {
            p.name("HeadAmount");
            p.inlineText(columns.amount(), style(TABLE_HEAD_SIZE, INK, DocumentTextDecoration.BOLD));
            if (!currencyCode.isBlank()) {
                p.inlineText(" (" + currencyCode + ")",
                        style(TABLE_HEAD_SMALL_SIZE, INK, DocumentTextDecoration.BOLD));
            }
            p.align(TextAlign.RIGHT);
            p.padding(new DocumentInsets(0, AMOUNT_PAD_R, 0, 0));
        });
        table.headerCells(DocumentTableCell.node(row.build()).withStyle(HEADER_CELL_STYLE))
                .repeatHeader();
    }

    private static void headCell(RowBuilder row, String name, String label, TextAlign align,
                                 double padRight) {
        row.addParagraph(p -> p
                .name(name)
                .text(label)
                .textStyle(style(TABLE_HEAD_SIZE, INK, DocumentTextDecoration.BOLD))
                .align(align)
                .padding(new DocumentInsets(0, padRight, 0, 0)));
    }

    /**
     * The five columns as a row inside the row's single cell. Alignment differs
     * per column and is part of the design rather than an accident: description
     * left, quantity centred, unit price right, tax centred, amount right.
     */
    private static DocumentNode renderLine(InvoiceServiceLines.Line line, int index,
                                           String currencyCode) {
        int n = index + 1;
        RowBuilder row = new RowBuilder();
        row.name("Item" + n)
                .spacing(0)
                .verticalAlign(RowVerticalAlign.CENTER)
                .columns(columnWidths());
        row.addSection("ItemDescription" + n, cell -> {
            cell.spacing(0).padding(new DocumentInsets(0, 0, 0, ITEM_CELL_INSET));
            // Mark and text are a horizontal pair inside a row cell, which the
            // layout compiler refuses as a bare row.
            PaymentsWidgets.layeredRow(cell, "ItemBody" + n, body -> {
                body.spacing(ITEM_DISC_GAP)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .columns(DocumentRowColumn.fixed(DISC_D),
                                DocumentRowColumn.fixed(ITEM_TEXT_W));
                body.addSection("ItemDisc" + n, disc -> {
                    disc.spacing(0);
                    if (!line.icon().isBlank()) {
                        disc.add(PaymentsWidgets.disc(line.icon(), ACCENT_SURFACE));
                    }
                });
                body.addSection("ItemText" + n, text -> {
                    text.spacing(0);
                    text.addParagraph(p -> p
                            .name("ItemTitle" + n)
                            .text(line.title())
                            .textStyle(style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD)));
                    if (!line.description().isBlank()) {
                        text.addParagraph(p -> p
                                .name("ItemSubtitle" + n)
                                .text(line.description())
                                .textStyle(style(ITEM_SUB_SIZE, MUTED,
                                        DocumentTextDecoration.DEFAULT))
                                .lineSpacing(gap(ITEM_SUB_PITCH, ITEM_SUB_SIZE))
                                .margin(new DocumentInsets(
                                        px(668 - 648) - LINE_BOX * ITEM_TITLE_SIZE
                                                + TEXT_TOP_BEARING
                                                * (ITEM_TITLE_SIZE - ITEM_SUB_SIZE),
                                        0, 0, 0)));
                    }
                });
            });
        });
        valueCell(row, "ItemQuantity" + n, PaymentsText.quantity(line.quantity()),
                CELL_SIZE, BODY, DocumentTextDecoration.DEFAULT, TextAlign.CENTER, 0);
        valueCell(row, "ItemUnitPrice" + n, PaymentsText.amount(currencyCode, line.unitPrice()),
                CELL_SIZE, BODY, DocumentTextDecoration.DEFAULT, TextAlign.RIGHT, CELL_PAD_R);
        valueCell(row, "ItemVat" + n, line.vatRate(),
                CELL_SIZE, BODY, DocumentTextDecoration.DEFAULT, TextAlign.CENTER, 0);
        valueCell(row, "ItemAmount" + n, PaymentsText.amount(currencyCode, line.amount()),
                CELL_AMOUNT_SIZE, INK, DocumentTextDecoration.BOLD, TextAlign.RIGHT, AMOUNT_PAD_R);
        return row.build();
    }

    private static void valueCell(RowBuilder row, String name, String value, double size,
                                  DocumentColor color, DocumentTextDecoration decoration,
                                  TextAlign align, double padRight) {
        row.addParagraph(p -> p
                .name(name)
                .text(value)
                .textStyle(style(size, color, decoration))
                .align(align)
                // padding, not margin: a paragraph's right margin inside a row
                // column is taken twice — once off the box's width and again as
                // an offset — so a 22 px inset would land the text 44 px in.
                .padding(new DocumentInsets(0, padRight, 0, 0)));
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
