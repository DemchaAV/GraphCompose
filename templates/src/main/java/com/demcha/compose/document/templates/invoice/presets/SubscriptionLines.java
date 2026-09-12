package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CELL_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CELL_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CELL_PAD_V;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.COLUMN_PX;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.COLUMN_TOTAL_PX;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_PARTIES_TO_TABLE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEAD_PAD_H;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEAD_PAD_V;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NO_RULE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TABLE_BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TABLE_RULE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TABLE_TOP_RULE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.px;

/**
 * The line-item table: six columns under an accent rule, a caps header that
 * repeats on every page it reaches, and a hairline under every row.
 *
 * <h2>Why every cell is unstroked</h2>
 *
 * <p>A cell's stroke draws all four of its edges and there is no per-edge
 * control, so any stroke that buys the design's horizontal rule also buys five
 * verticals it does not have. Every cell here therefore carries none: the rule
 * under the header is an accent on the header cell, and the rule under each data
 * row is a row of its own — a filled cell spanning every column, one rule
 * thick.</p>
 */
final class SubscriptionLines {

    private SubscriptionLines() {
    }

    /**
     * The table.
     *
     * @param page         the page flow
     * @param serviceLines the lines and their column captions
     * @param currencyCode the code every money figure carries as a symbol
     */
    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencyCode) {
        page.addSection("LineItems", section -> {
            section.padding(CONTENT_PAD);
            section.margin(new DocumentInsets(GAP_PARTIES_TO_TABLE, 0, 0, 0));
            section.spacing(0);
            // The accent rule belongs to a node whose box is the content box.
            // The section above carries the page's horizontal padding, so its own
            // box is the full paper width and an accent on it would run edge to
            // edge.
            section.addSection("LineItemsRuled", ruled -> {
                ruled.spacing(0);
                ruled.accentTop(ACCENT, TABLE_TOP_RULE);
                renderTable(ruled, serviceLines, currencyCode);
            });
        });
    }

    private static void renderTable(SectionBuilder ruled, InvoiceServiceLines serviceLines,
                                    String currencyCode) {
        InvoiceServiceLines.Columns columns = serviceLines.columns();
        ruled.addTable(table -> {
            table.name("LineItemTable");
            table.defaultCellStyle(DocumentTableStyle.builder().stroke(NO_RULE).build());
            table.columns(column(0), column(1), column(2), column(3), column(4), column(5));
            table.headerCells(
                    headerCell(columns.index(), TextAlign.LEFT),
                    headerCell(columns.description(), TextAlign.LEFT),
                    headerCell(columns.quantity(), TextAlign.CENTER),
                    headerCell(columns.unitPrice(), TextAlign.CENTER),
                    headerCell(columns.vat(), TextAlign.CENTER),
                    headerCell(columns.amount(), TextAlign.CENTER));
            table.repeatHeader();

            List<InvoiceServiceLines.Line> lines = serviceLines.lines();
            for (int i = 0; i < lines.size(); i++) {
                InvoiceServiceLines.Line line = lines.get(i);
                table.rowCells(
                        dataCell(number(line, i), DocumentTableTextAnchor.CENTER_LEFT),
                        dataCell(line.title(), DocumentTableTextAnchor.CENTER_LEFT),
                        dataCell(SubscriptionText.quantity(line.quantity()),
                                DocumentTableTextAnchor.CENTER),
                        dataCell(SubscriptionText.money(currencyCode, line.unitPrice()),
                                DocumentTableTextAnchor.CENTER_RIGHT),
                        dataCell(line.vatRate(), DocumentTableTextAnchor.CENTER),
                        dataCell(SubscriptionText.money(currencyCode, line.amount()),
                                DocumentTableTextAnchor.CENTER_RIGHT));
                table.rowCells(ruleRow());
            }
        });
    }

    /**
     * The number the sheet prints for a line: the one the document states, or its
     * position when it states none. A document continuing a numbering from
     * somewhere else can say so; one that does not gets 1, 2, 3.
     */
    private static String number(InvoiceServiceLines.Line line, int index) {
        return String.valueOf(line.lineNumber() > 0 ? line.lineNumber() : index + 1);
    }

    private static DocumentTableColumn column(int index) {
        return DocumentTableColumn.fixed(CONTENT_W * COLUMN_PX[index] / COLUMN_TOTAL_PX);
    }

    /**
     * A caption cell. A centred caption needs room, not alignment, so it takes
     * the narrower symmetric padding; a left-aligned one is positioned by its
     * padding and keeps the data cells' measured inset.
     */
    private static DocumentTableCell headerCell(String caption, TextAlign align) {
        SectionBuilder cell = new SectionBuilder();
        cell.name("LineItemHeaderCell");
        cell.spacing(0);
        double padL = align == TextAlign.LEFT ? CELL_PAD_L : HEAD_PAD_H;
        double padR = align == TextAlign.LEFT ? CELL_PAD_R : HEAD_PAD_H;
        cell.padding(new DocumentInsets(HEAD_PAD_V, padR, HEAD_PAD_V, padL));
        cell.accentBottom(HAIRLINE, TABLE_RULE);
        cell.addParagraph(p -> p
                .name("LineItemHeading")
                .text(caption)
                .textStyle(bold(TABLE_HEAD_SIZE, INK))
                .align(align));
        return DocumentTableCell.node(cell.build()).withStyle(DocumentTableStyle.builder()
                .stroke(NO_RULE)
                .padding(DocumentInsets.zero())
                .build());
    }

    private static DocumentTableCell dataCell(String value, DocumentTableTextAnchor anchor) {
        return DocumentTableCell.text(value).withStyle(DocumentTableStyle.builder()
                .stroke(NO_RULE)
                .padding(new DocumentInsets(CELL_PAD_V, CELL_PAD_R, CELL_PAD_V, CELL_PAD_L))
                .textStyle(plain(TABLE_BODY_SIZE, INK))
                .textAnchor(anchor)
                .build());
    }

    /** The hairline under a row: a filled cell across every column, one rule thick. */
    private static DocumentTableCell ruleRow() {
        return DocumentTableCell
                .node(new SpacerBuilder()
                        .name("LineItemRule")
                        .size(px(1), TABLE_RULE)
                        .build())
                .withStyle(DocumentTableStyle.builder()
                        .stroke(NO_RULE)
                        .padding(DocumentInsets.zero())
                        .fillColor(HAIRLINE)
                        .build())
                .colSpan(COLUMN_PX.length);
    }
}
