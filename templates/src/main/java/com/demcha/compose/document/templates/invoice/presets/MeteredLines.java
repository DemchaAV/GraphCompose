package com.demcha.compose.document.templates.invoice.presets;

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

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.BODY_CELL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CELL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.COL_AMOUNT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.COL_QUANTITY;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.COL_SERVICE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.COL_UNIT_PRICE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DESC_TEXT_COL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DESC_TILE_COL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.HEADER_CELL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.LINE_SUB;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.LINE_TEXT_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.LINE_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_PARTIES_TO_TABLE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TABLE_HEAD;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TABLE_PAD_L;

/**
 * The metered line-item table: a dark caps header that repeats on every page it
 * reaches, and one row per service line with its mark on a bordered tile.
 */
final class MeteredLines {

    private MeteredLines() {
    }

    /**
     * The table.
     *
     * @param body         the page's body section
     * @param serviceLines the lines and their column captions
     * @param currencyCode the code every figure is written behind
     */
    static void render(SectionBuilder body, InvoiceServiceLines serviceLines, String currencyCode) {
        body.addTable(table -> {
            table.name("LineItems");
            table.margin(SEAM_PARTIES_TO_TABLE);
            table.width(CONTENT_W);
            table.columns(DocumentTableColumn.fixed(CONTENT_W));
            table.defaultCellStyle(BODY_CELL);
            renderHeader(table, serviceLines.columns());
            List<InvoiceServiceLines.Line> lines = serviceLines.lines();
            for (int i = 0; i < lines.size(); i++) {
                table.rowCells(DocumentTableCell
                        .node(lineRow(lines.get(i), i, currencyCode))
                        .withStyle(BODY_CELL));
            }
        });
    }

    /**
     * The dark band: its fill, its inverse caps, and the instruction that brings
     * it back at the top of every page the table reaches.
     */
    private static void renderHeader(TableBuilder table, InvoiceServiceLines.Columns columns) {
        table.headerCells(DocumentTableCell.node(headerRow(columns)).withStyle(HEADER_CELL));
        table.repeatHeader();
    }

    private static DocumentNode headerRow(InvoiceServiceLines.Columns columns) {
        RowBuilder row = new RowBuilder();
        row.name("LineItemsHeaderRow")
                .verticalAlign(RowVerticalAlign.CENTER)
                .columns(DocumentRowColumn.fixed(DESC_TILE_COL),
                        DocumentRowColumn.fixed(DESC_TEXT_COL),
                        DocumentRowColumn.fixed(COL_SERVICE),
                        DocumentRowColumn.fixed(COL_QUANTITY),
                        DocumentRowColumn.fixed(COL_UNIT_PRICE),
                        DocumentRowColumn.fixed(COL_AMOUNT))
                .addSpacer(0)
                .addParagraph(p -> p.name("HeadDescription")
                        .text(columns.description()).textStyle(TABLE_HEAD))
                .addParagraph(p -> p.name("HeadService")
                        .text(columns.servicePeriod()).textStyle(TABLE_HEAD).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("HeadQuantity")
                        .text(columns.quantity()).textStyle(TABLE_HEAD).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("HeadUnitPrice")
                        .text(columns.unitPrice()).textStyle(TABLE_HEAD).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("HeadAmount")
                        .text(columns.amount()).textStyle(TABLE_HEAD).align(TextAlign.CENTER));
        return row.build();
    }

    /**
     * One service line.
     *
     * <p>The mark is the document's: a line names one from this preset's own
     * vocabulary through {@code icon()}, and a line that names none keeps the
     * tile's space so the text lanes stay on their axis down the column.</p>
     */
    private static DocumentNode lineRow(InvoiceServiceLines.Line line, int index,
                                        String currencyCode) {
        String token = line.icon();
        DocumentNode mark = token == null || token.isBlank()
                ? MeteredWidgets.tileSpace(index)
                : MeteredWidgets.tile(token, index);
        RowBuilder row = new RowBuilder();
        row.name("LineItemRow_" + index)
                .verticalAlign(RowVerticalAlign.CENTER)
                .columns(DocumentRowColumn.fixed(DESC_TILE_COL),
                        DocumentRowColumn.fixed(DESC_TEXT_COL),
                        DocumentRowColumn.fixed(COL_SERVICE),
                        DocumentRowColumn.fixed(COL_QUANTITY),
                        DocumentRowColumn.fixed(COL_UNIT_PRICE),
                        DocumentRowColumn.fixed(COL_AMOUNT))
                .addSection("LineTileCell_" + index, cell -> cell
                        .padding(new DocumentInsets(0, 0, 0, TABLE_PAD_L))
                        .add(mark))
                .addSection("LineText_" + index, text -> text
                        .spacing(LINE_TEXT_GAP)
                        .addParagraph(p -> p.name("LineTitle_" + index)
                                .text(line.title()).textStyle(LINE_TITLE))
                        .addParagraph(p -> p.name("LineSubtitle_" + index)
                                .text(line.description()).textStyle(LINE_SUB)))
                .addParagraph(p -> p.name("LineService_" + index)
                        .text(line.servicePeriod()).textStyle(CELL).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("LineQuantity_" + index)
                        .text(MeteredText.quantityWithUnit(line.quantity(), line.unit()))
                        .textStyle(CELL).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("LineUnitPrice_" + index)
                        .text(MeteredText.codedRate(currencyCode, line.unitPrice()))
                        .textStyle(CELL).align(TextAlign.CENTER))
                .addParagraph(p -> p.name("LineAmount_" + index)
                        .text(MeteredText.codedMoney(currencyCode, line.amount()))
                        .textStyle(CELL).align(TextAlign.CENTER));
        return row.build();
    }
}
