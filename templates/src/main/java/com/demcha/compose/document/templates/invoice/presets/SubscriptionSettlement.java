package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CYCLE_AMBER;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_TABLE_TO_TOTALS;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NOTES_BODY_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NOTES_BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NOTES_LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NOTES_TEXT_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NOTES_TOP_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.NO_RULE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_DUE_PAD_V;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_LABEL_SHARE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_LEFT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_PAD_H;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_PAD_V;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_ROW_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTALS_RULE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTAL_DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TOTAL_DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.leading;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionWidgets.headingPlaque;

/**
 * The band under the table: the notes on the left, the totals on the right.
 */
final class SubscriptionSettlement {

    private SubscriptionSettlement() {
    }

    /**
     * The notes-and-totals row.
     *
     * <p>The two are not top-flush: the design starts the notes heading a
     * heading-height below the first totals line beside it, which is what makes
     * the block read as an aside to the sum rather than as a second column of
     * it.</p>
     *
     * @param page         the page flow
     * @param notes        the note beside the totals
     * @param totals       the summed rows and the grand total
     * @param currencyCode the code every money figure carries as a symbol
     */
    static void render(PageFlowBuilder page, InvoiceNotesBlock notes,
                       InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("NotesAndTotals", row -> {
            row.padding(CONTENT_PAD);
            row.margin(new DocumentInsets(GAP_TABLE_TO_TOTALS, 0, 0, 0));
            row.spacing(0);
            row.weights(TOTALS_LEFT_RATIO, 1 - TOTALS_LEFT_RATIO);
            row.addSection("Notes", cell -> renderNotes(cell, notes));
            row.addSection("Totals", cell -> renderTotals(cell, totals, currencyCode));
        });
    }

    private static void renderNotes(SectionBuilder cell, InvoiceNotesBlock notes) {
        cell.spacing(0);
        if (notes.heading().isBlank() && notes.paragraphs().isEmpty()) {
            return;
        }
        cell.margin(new DocumentInsets(NOTES_TOP_OFFSET, 0, 0, 0));
        // The paragraph wraps at its container, and the design wraps it well
        // before the cell ends, so the width belongs to the cell.
        cell.padding(new DocumentInsets(
                0, CONTENT_W * TOTALS_LEFT_RATIO - NOTES_TEXT_W, 0, 0));
        if (!notes.heading().isBlank()) {
            headingPlaque(cell, "NotesHeading", notes.heading(), HEADING_SIZE, CYCLE_AMBER);
        }
        List<String> paragraphs = notes.paragraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            String prose = paragraphs.get(i);
            int index = i;
            cell.addParagraph(p -> p
                    .name("NotesBody_" + index)
                    .text(prose)
                    .textStyle(plain(NOTES_BODY_SIZE, INK))
                    .lineSpacing(leading(NOTES_LINE_PITCH, NOTES_BODY_SIZE))
                    .margin(new DocumentInsets(NOTES_BODY_GAP, 0, 0, 0)));
        }
    }

    private static void renderTotals(SectionBuilder cell, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        cell.spacing(0);
        cell.keepTogether();
        double width = CONTENT_W * (1 - TOTALS_LEFT_RATIO);
        cell.addTable(table -> {
            table.name("TotalsTable");
            table.defaultCellStyle(DocumentTableStyle.builder().stroke(NO_RULE).build());
            table.columns(DocumentTableColumn.fixed(width * TOTALS_LABEL_SHARE),
                    DocumentTableColumn.fixed(width * (1 - TOTALS_LABEL_SHARE)));
            for (InvoiceTotalsBlock.Row row : totals.rows()) {
                table.rowCells(
                        totalsCell(row.label(), TOTALS_ROW_SIZE,
                                DocumentTableTextAnchor.CENTER_LEFT, TOTALS_PAD_V),
                        totalsCell(SubscriptionText.money(currencyCode, row.amount()),
                                TOTALS_ROW_SIZE, DocumentTableTextAnchor.CENTER_RIGHT,
                                TOTALS_PAD_V));
            }
            table.rowCells(DocumentTableCell
                    .node(new SpacerBuilder()
                            .name("TotalsRule")
                            .size(px(1), TOTALS_RULE)
                            .build())
                    .withStyle(DocumentTableStyle.builder()
                            .stroke(NO_RULE)
                            .padding(DocumentInsets.zero())
                            .fillColor(ACCENT)
                            .build())
                    .colSpan(2));
            table.rowCells(
                    totalsCell(totals.totalLabel(), TOTAL_DUE_LABEL_SIZE,
                            DocumentTableTextAnchor.CENTER_LEFT, TOTALS_DUE_PAD_V),
                    totalsCell(SubscriptionText.money(currencyCode, totals.totalAmount()),
                            TOTAL_DUE_VALUE_SIZE, DocumentTableTextAnchor.CENTER_RIGHT,
                            TOTALS_DUE_PAD_V));
        });
    }

    private static DocumentTableCell totalsCell(String value, double size,
                                                DocumentTableTextAnchor anchor, double padV) {
        return DocumentTableCell.text(value).withStyle(DocumentTableStyle.builder()
                .stroke(NO_RULE)
                .padding(new DocumentInsets(padV, TOTALS_PAD_H, padV, TOTALS_PAD_H))
                .textStyle(bold(size, INK))
                .textAnchor(anchor)
                .build());
    }
}
