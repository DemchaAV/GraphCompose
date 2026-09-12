package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BODY_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.INK_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ITEM_DESC;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ITEM_INDEX;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ITEM_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_AMOUNT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_DESCRIPTION;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_HEADER_CENTRE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_HEADER_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_HEADER_RIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_INDEX;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAD_NUMERIC;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TABLE_HEADER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TABLE_RATIOS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TABLE_RULE_THICKNESS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_TABLE_HEADER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.cellStyle;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.headerStyle;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.leading;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.money;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.sans;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.tracked;

/**
 * The line-items table: a dark header band, one row per service line, and a
 * hairline row between neighbours.
 */
final class LumaStudioLines {

    private LumaStudioLines() {
    }

    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencySymbol) {
        page.addTable(table -> {
            compose(table, serviceLines, currencySymbol);
            // The dark header band is the region a continuation page needs:
            // without this a second page carries five unlabelled money columns.
            table.repeatHeader();
        });
    }

    private static void compose(TableBuilder table, InvoiceServiceLines serviceLines,
                                String currencySymbol) {
        InvoiceServiceLines.Columns columns = serviceLines.columns();
        DocumentTableStyle centred = headerStyle(
                DocumentTableTextAnchor.CENTER, PAD_HEADER_CENTRE);
        table.name("LineItems")
                .columns(column(0), column(1), column(2), column(3), column(4), column(5))
                .headerCells(
                        headerCell(columns.description(), TextAlign.LEFT)
                                .withStyle(headerStyle(
                                        DocumentTableTextAnchor.CENTER_LEFT, PAD_HEADER_LEFT))
                                .colSpan(2),
                        headerCell(columns.quantity(), TextAlign.CENTER),
                        headerCell(columns.unitPrice(), TextAlign.CENTER),
                        headerCell(columns.vat(), TextAlign.CENTER),
                        headerCell(columns.amount(), TextAlign.RIGHT)
                                .withStyle(headerStyle(
                                        DocumentTableTextAnchor.CENTER_RIGHT, PAD_HEADER_RIGHT)))
                .headerStyle(centred)
                .padding(DocumentInsets.zero())
                .margin(DocumentInsets.zero());

        for (InvoiceServiceLines.Line line : serviceLines.lines()) {
            table.rowCells(
                    DocumentTableCell.text(String.format("%02d", line.lineNumber()))
                            .withStyle(cellStyle(ITEM_INDEX,
                                    DocumentTableTextAnchor.CENTER_LEFT, PAD_INDEX)),
                    DocumentTableCell.node(description(line))
                            .withStyle(cellStyle(BODY,
                                    DocumentTableTextAnchor.CENTER_LEFT, PAD_DESCRIPTION)),
                    DocumentTableCell.text(quantity(line))
                            .withStyle(cellStyle(BODY,
                                    DocumentTableTextAnchor.CENTER, PAD_NUMERIC)),
                    DocumentTableCell.text(money(currencySymbol, line.unitPrice()))
                            .withStyle(cellStyle(BODY,
                                    DocumentTableTextAnchor.CENTER, PAD_NUMERIC)),
                    DocumentTableCell.text(line.vatRate())
                            .withStyle(cellStyle(BODY,
                                    DocumentTableTextAnchor.CENTER, PAD_NUMERIC)),
                    DocumentTableCell.text(money(currencySymbol, line.amount()))
                            .withStyle(cellStyle(BODY_BOLD,
                                    DocumentTableTextAnchor.CENTER_RIGHT, PAD_AMOUNT)));
            itemRule(table);
        }
    }

    private static DocumentTableColumn column(int index) {
        return DocumentTableColumn.fixed(CONTENT_WIDTH * TABLE_RATIOS[index]);
    }

    /**
     * A tracked header cell, written run by run like every other tracked line
     * on the sheet — its gaps painted in the band's own dark, not the paper's.
     */
    private static DocumentTableCell headerCell(String text, TextAlign align) {
        return DocumentTableCell.node(tracked("HeaderCell", text, TABLE_HEADER,
                TRACK_TABLE_HEADER, INK_SURFACE, align, TextVerticalAlign.DEFAULT));
    }

    /**
     * A line's title over its description, as one node so the index cell has
     * a single thing to centre against.
     *
     * <p>They are two paragraphs in a block rather than two runs of one
     * paragraph: a single paragraph would need an invisible spacer sized to
     * the remaining width to break after the title, and there is no
     * compose-time text measurement to size one with. The block puts the
     * same leading between them and lands in the same place.</p>
     */
    private static DocumentNode description(InvoiceServiceLines.Line line) {
        double gap = leading(LINE_PITCH * 0.83, ITEM_DESC);
        SectionBuilder block = new SectionBuilder();
        block.name("ItemDescription").spacing(0)
                .padding(DocumentInsets.zero())
                .margin(DocumentInsets.zero());
        block.addParagraph(p -> p
                .name("ItemTitle")
                .text(line.title())
                .textStyle(ITEM_TITLE)
                .align(TextAlign.LEFT)
                .lineSpacing(gap)
                .margin(DocumentInsets.zero()));
        block.addParagraph(p -> p
                .name("ItemBody")
                .text(line.description())
                .textStyle(ITEM_DESC)
                .align(TextAlign.LEFT)
                .lineSpacing(gap)
                .margin(new DocumentInsets(gap, 0, 0, 0)));
        return block.build();
    }

    /**
     * How much was delivered, written plainly: a whole number loses its
     * decimals, because a sheet that says "1" should not say "1.00".
     */
    private static String quantity(InvoiceServiceLines.Line line) {
        return line.quantity().stripTrailingZeros().toPlainString();
    }

    /** The hairline under a row, drawn as a row of its own. */
    private static void itemRule(TableBuilder table) {
        DocumentTableStyle rule = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .fillColor(HAIRLINE)
                .stroke(DocumentStroke.of(HAIRLINE, 0))
                .textStyle(sans(TABLE_RULE_THICKNESS, DocumentTextDecoration.DEFAULT, HAIRLINE))
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
        table.rowCells(
                DocumentTableCell.text("").withStyle(rule),
                DocumentTableCell.text("").withStyle(rule),
                DocumentTableCell.text("").withStyle(rule),
                DocumentTableCell.text("").withStyle(rule),
                DocumentTableCell.text("").withStyle(rule),
                DocumentTableCell.text("").withStyle(rule));
    }
}
