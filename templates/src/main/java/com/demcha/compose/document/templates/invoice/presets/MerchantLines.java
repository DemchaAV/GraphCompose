package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CELL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.COLUMN_CENTRED;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.COLUMN_SHARES;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ON_FILL;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_BLOCK_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_ROW;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_HEADER_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_ICON_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_PAD_CENTRED;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_PAD_EDGE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_PAD_HEAD;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_PAD_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TABLE_ROW_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.WHITE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.style;

/**
 * The line-item table: a dark caps header that repeats on every page it reaches,
 * and one bordered row per service line.
 *
 * <h2>Each row is one cell</h2>
 *
 * <p>A cell's stroke draws all four of its edges, which is exactly what this
 * design's rows are — a soft box round each one, with no interior verticals. So
 * a row is a single cell spanning every column, and the five columns are a
 * composed row inside it. Stroking five real cells would draw four verticals the
 * design does not have.</p>
 */
final class MerchantLines {

    private MerchantLines() {
    }

    /**
     * The table.
     *
     * @param page         the page flow
     * @param serviceLines the lines and their column captions
     * @param currencyCode the code the two money captions carry
     */
    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencyCode) {
        List<InvoiceServiceLines.Line> lines = serviceLines.lines();
        page.addTable(table -> {
            table.name("LineItems").width(CONTENT_W);
            table.margin(new DocumentInsets(py(667 - 451) - PARTY_BLOCK_H, 0, 0, 0));
            List<DocumentTableColumn> columns = new ArrayList<>();
            for (double share : COLUMN_SHARES) {
                columns.add(DocumentTableColumn.fixed(CONTENT_W * share));
            }
            table.columns(columns.toArray(new DocumentTableColumn[0]));
            renderHeader(table, serviceLines.columns(), currencyCode);
            for (int i = 0; i < lines.size(); i++) {
                table.rowCells(List.of(rowCell(lines.get(i), i, currencyCode)));
            }
        });
    }

    /**
     * The captions, with the currency stated once in each money column.
     *
     * <p>The design puts the code in the caption and writes the figures under it
     * bare, which is why nothing in the table carries a currency of its own.</p>
     */
    private static List<String> captions(InvoiceServiceLines.Columns columns,
                                         String currencyCode) {
        return List.of(
                columns.description(),
                columns.servicePeriod(),
                columns.quantity(),
                MerchantText.captionWithCurrency(columns.unitPrice(), currencyCode),
                MerchantText.captionWithCurrency(columns.amount(), currencyCode));
    }

    private static void renderHeader(TableBuilder table, InvoiceServiceLines.Columns columns,
                                     String currencyCode) {
        List<String> captions = captions(columns, currencyCode);
        double pad = (TABLE_HEADER_H - LINE_BOX * TABLE_HEAD_SIZE) / 2.0;
        List<DocumentTableCell> cells = new ArrayList<>();
        for (int i = 0; i < captions.size(); i++) {
            DocumentInsets insets = COLUMN_CENTRED[i]
                    ? new DocumentInsets(pad, TABLE_PAD_CENTRED, pad, TABLE_PAD_CENTRED)
                    : new DocumentInsets(pad, TABLE_PAD_EDGE, pad, TABLE_PAD_HEAD);
            DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                    .padding(insets)
                    .fillColor(INK)
                    .stroke(DocumentStroke.of(INK, 0))
                    .textStyle(bold(TABLE_HEAD_SIZE, ON_FILL))
                    .textAnchor(COLUMN_CENTRED[i]
                            ? DocumentTableTextAnchor.CENTER
                            : DocumentTableTextAnchor.CENTER_LEFT)
                    .build();
            cells.add(new DocumentTableCell(List.of(captions.get(i)), cellStyle));
        }
        table.headerCells(cells);
        table.repeatHeader();
    }

    /** One service line: a bordered cell across every column, holding a row of five. */
    private static DocumentTableCell rowCell(InvoiceServiceLines.Line line, int index,
                                             String currencyCode) {
        double pad = (TABLE_ROW_H - descriptionHeight()) / 2.0;
        DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .fillColor(WHITE)
                .stroke(DocumentStroke.of(RULE_SOFT, RULE_ROW))
                .textStyle(style(CELL_SIZE, INK))
                .build();

        SectionBuilder holder = new SectionBuilder();
        holder.name("LineRow_" + index + "Holder");
        holder.addRow("LineRow_" + index, row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .padding(new DocumentInsets(pad, 0, pad, 0));
            List<DocumentRowColumn> widths = new ArrayList<>();
            for (double share : COLUMN_SHARES) {
                widths.add(DocumentRowColumn.fixed(CONTENT_W * share));
            }
            row.columns(widths.toArray(new DocumentRowColumn[0]));
            row.add(descriptionCell(line, index));
            row.addParagraph(p -> p.name("LineService_" + index)
                    .text(line.servicePeriod()).textStyle(style(CELL_SIZE, INK))
                    .align(TextAlign.CENTER));
            row.addParagraph(p -> p.name("LineQuantity_" + index)
                    .text(MerchantText.quantity(line.quantity()))
                    .textStyle(style(CELL_SIZE, INK)).align(TextAlign.CENTER));
            row.addParagraph(p -> p.name("LineUnitPrice_" + index)
                    .text(MerchantText.money(line.unitPrice()))
                    .textStyle(style(CELL_SIZE, INK)).align(TextAlign.CENTER));
            row.addParagraph(p -> p.name("LineAmount_" + index)
                    .text(MerchantText.money(line.amount()))
                    .textStyle(style(CELL_SIZE, INK)).align(TextAlign.CENTER));
        });
        return new DocumentTableCell(List.of(), cellStyle, COLUMN_SHARES.length, 1,
                holder.build());
    }

    /**
     * The mark and the two-line description beside it.
     *
     * <p>The mark hangs in a fixed gutter and the text takes the rest, so a
     * longer product name wraps under itself rather than under the mark. A line
     * that names no mark keeps the gutter, so the text lanes stay on their axis
     * down the column.</p>
     */
    private static DocumentNode descriptionCell(InvoiceServiceLines.Line line, int index) {
        String token = line.icon();
        // The block is a row and it sits inside one, which the layout compiler
        // refuses — so it goes through a LayerStack layer, the same way every
        // other horizontal pair on this sheet does.
        SectionBuilder inner = new SectionBuilder();
        inner.name("LineDescription_" + index + "Holder");
        inner.addRow("LineDescription_" + index, cell -> {
            cell.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .padding(new DocumentInsets(0, 0, 0, TABLE_PAD_ICON))
                    .columns(DocumentRowColumn.fixed(TABLE_ICON_GUTTER),
                            DocumentRowColumn.weight(1));
            cell.addSection("LineIcon_" + index, gutter -> {
                gutter.spacing(0);
                if (!token.isBlank()) {
                    gutter.addSvgIcon(MerchantIcons.icon(token), TABLE_ICON);
                }
            });
            cell.addSection("LineText_" + index, text -> {
                text.spacing(capGap(24, ITEM_TITLE_SIZE, true, CELL_SIZE, false));
                text.addParagraph(p -> p
                        .name("LineTitle_" + index)
                        .text(line.title())
                        .textStyle(bold(ITEM_TITLE_SIZE, INK)));
                text.addParagraph(p -> p
                        .name("LineSubtitle_" + index)
                        .text(line.description())
                        .textStyle(style(CELL_SIZE, INK)));
            });
        });
        DocumentNode row = inner.build();
        SectionBuilder layered = new SectionBuilder();
        layered.name("LineDescription_" + index + "Layer");
        layered.addLayerStack(stack -> stack
                .name("LineDescription_" + index + "Stack")
                .layer(row, LayerAlign.TOP_LEFT, 0));
        return layered.build();
    }

    /** The tallest cell in a row, and therefore what its padding is solved against. */
    private static double descriptionHeight() {
        return LINE_BOX * ITEM_TITLE_SIZE
                + capGap(24, ITEM_TITLE_SIZE, true, CELL_SIZE, false)
                + LINE_BOX * CELL_SIZE;
    }
}
