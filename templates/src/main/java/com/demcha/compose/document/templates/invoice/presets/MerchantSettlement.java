package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.BORDER_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_BOTTOM_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_ICON_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_LABEL_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_TOP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CARD_VALUE_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_BOTTOM_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_HEAD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ICON_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ICON_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_LAST_ROW_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_NOTE_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_NOTE_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_NOTE_PITCH_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_NOTE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ROW1_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ROW_PITCH_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_ROW_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_RULE_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_RULE_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_TOP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_MEDIUM;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_TOTALS;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.SUMMARY_GAP_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.SURFACE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOP_BEARING_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOP_BEARING_REGULAR;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_DUE_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_ROW1_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_ROW_PITCH_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_RULE_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_VALUE_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTALS_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTAL_DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOTAL_DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.style;
import static com.demcha.compose.document.templates.invoice.presets.MerchantMasthead.layeredRow;

/**
 * The band under the table: the bank panel on the left, and on the right the
 * totals over the card carrying the due date.
 */
final class MerchantSettlement {

    private MerchantSettlement() {
    }

    /**
     * The settlement row.
     *
     * @param page         the page flow
     * @param payment      the bank details and the due notice
     * @param totals       the summed rows and the grand total
     * @param currencyCode the code the grand total carries
     */
    static void render(PageFlowBuilder page, InvoicePaymentBlock payment,
                       InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("Settlement", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(PANEL_W),
                            DocumentRowColumn.fixed(SUMMARY_GAP_W),
                            DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(py(PANEL_TOP_Y - 974), 0, 0, 0));
            row.addSection("PaymentPanel", column -> renderPanel(column, payment));
            row.addSection("SettlementGap", column -> column.spacing(0));
            row.addSection("SettlementRight", column -> {
                column.spacing(0);
                renderTotals(column, totals, currencyCode);
                renderDueCard(column, payment);
            });
        });
    }

    private static void renderPanel(SectionBuilder column, InvoicePaymentBlock payment) {
        column.spacing(0)
                .keepTogether()
                .fillColor(SURFACE_SOFT)
                .stroke(DocumentStroke.of(BORDER_SOFT, RULE_THIN))
                .cornerRadius(PANEL_RADIUS);

        layeredRow(column, "PaymentHeading", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(PANEL_HEAD_GUTTER),
                            DocumentRowColumn.weight(1))
                    .padding(new DocumentInsets(
                            py(PANEL_ICON_Y - PANEL_TOP_Y), 0, 0, PANEL_ICON_PAD_L));
            row.add(MerchantIcons.icon(MerchantIcons.BANK).node(PANEL_ICON));
            row.addParagraph(p -> p
                    .name("PaymentHeadingText")
                    .text(payment.heading())
                    .textStyle(bold(PANEL_HEAD_SIZE, ACCENT)));
        });

        column.addSection("PaymentRows", rows -> {
            rows.spacing(capGap(PANEL_ROW_PITCH_Y, PANEL_ROW_SIZE, false, PANEL_ROW_SIZE, false))
                    .padding(new DocumentInsets(0, PANEL_PAD_R, 0, PANEL_PAD_L))
                    // Measured from the mark, because the heading layer's height
                    // is the mark's and not the label's line box.
                    .margin(new DocumentInsets(
                            py(PANEL_ROW1_CAP_Y - PANEL_ICON_Y)
                                    - TOP_BEARING_REGULAR * PANEL_ROW_SIZE - PANEL_ICON,
                            0, 0, 0));
            List<InvoicePaymentBlock.Field> fields = payment.fields();
            for (int i = 0; i < fields.size(); i++) {
                InvoicePaymentBlock.Field field = fields.get(i);
                int index = i;
                layeredRow(rows, "PaymentRow_" + index, row -> {
                    row.spacing(0)
                            .columns(DocumentRowColumn.fixed(PANEL_LABEL_W),
                                    DocumentRowColumn.weight(1));
                    row.addParagraph(p -> p.text(field.label())
                            .textStyle(style(PANEL_ROW_SIZE, INK)));
                    row.addParagraph(p -> {
                        p.text(field.value()).textStyle(style(PANEL_ROW_SIZE, INK));
                        // A bank detail is a reference, not a destination — except
                        // an address, the one field a reader would act on.
                        if (field.value().contains("@")) {
                            p.link(ContactUri.mailLink(field.value()));
                        }
                    });
                });
            }
        });

        if (payment.instruction().isBlank()) {
            return;
        }
        column.addLine(line -> line
                .name("PaymentPanelRule")
                .horizontal(PANEL_RULE_W)
                .thickness(RULE_THIN)
                .color(RULE_SOFT)
                .margin(new DocumentInsets(
                        py(PANEL_RULE_Y - PANEL_LAST_ROW_CAP_Y)
                                + TOP_BEARING_REGULAR * PANEL_ROW_SIZE
                                - LINE_BOX * PANEL_ROW_SIZE,
                        0, 0, PANEL_RULE_PAD_L)));
        layeredRow(column, "PaymentNote", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(PANEL_NOTE_GUTTER),
                            DocumentRowColumn.weight(1))
                    .padding(new DocumentInsets(
                            py(PANEL_NOTE_CAP_Y - PANEL_RULE_Y) - RULE_BOX
                                    - TOP_BEARING_REGULAR * PANEL_NOTE_SIZE,
                            0,
                            py(PANEL_BOTTOM_Y - PANEL_NOTE_CAP_Y - PANEL_NOTE_PITCH_Y)
                                    + TOP_BEARING_REGULAR * PANEL_NOTE_SIZE
                                    - LINE_BOX * PANEL_NOTE_SIZE,
                            PANEL_PAD_L));
            row.add(MerchantIcons.icon(MerchantIcons.INFO).node(PANEL_NOTE_ICON));
            row.addSection("PaymentNoteText", text -> {
                text.spacing(capGap(PANEL_NOTE_PITCH_Y, PANEL_NOTE_SIZE, false,
                        PANEL_NOTE_SIZE, false));
                String[] lines = payment.instruction().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String prose = lines[i];
                    int index = i;
                    text.addParagraph(p -> p
                            .name("PaymentNoteLine_" + index)
                            .text(prose)
                            .textStyle(style(PANEL_NOTE_SIZE, INK)));
                }
            });
        });
    }

    private static void renderTotals(SectionBuilder column, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        column.addSection("Totals", block -> {
            block.spacing(0)
                    .keepTogether()
                    .margin(capTop(TOTALS_ROW1_CAP_Y - PANEL_TOP_Y, TOTALS_LABEL_SIZE, false));
            List<InvoiceTotalsBlock.Row> rows = totals.rows();
            for (int i = 0; i < rows.size(); i++) {
                InvoiceTotalsBlock.Row entry = rows.get(i);
                boolean first = i == 0;
                int index = i;
                layeredRow(block, "TotalsRow_" + index, row -> {
                    row.spacing(0)
                            .padding(new DocumentInsets(0, TOTALS_VALUE_PAD_R, 0, 0))
                            // The row's box is the value's, which is the taller of
                            // the two, so the pitch is solved against that and not
                            // against the label the pitch was measured on.
                            .margin(new DocumentInsets(first ? 0
                                    : py(TOTALS_ROW_PITCH_Y)
                                            + TOP_BEARING_REGULAR * TOTALS_LABEL_SIZE
                                            - LINE_BOX * TOTALS_VALUE_SIZE
                                            - TOP_BEARING_REGULAR * TOTALS_LABEL_SIZE,
                                    0, 0, 0))
                            .evenWeights();
                    row.addParagraph(p -> p.text(entry.label())
                            .textStyle(style(TOTALS_LABEL_SIZE, INK)));
                    row.addParagraph(p -> p
                            .text(MerchantText.money(entry.amount()))
                            .textStyle(style(TOTALS_VALUE_SIZE, INK))
                            .align(TextAlign.RIGHT));
                });
            }
            block.addLine(line -> line
                    .name("TotalsRule")
                    .horizontal(TOTALS_RULE_W)
                    .thickness(RULE_MEDIUM)
                    .color(RULE_TOTALS)
                    // Same correction: the box above this rule is the value's.
                    .margin(new DocumentInsets(
                            py(TOTALS_RULE_Y - TOTALS_ROW1_CAP_Y - TOTALS_ROW_PITCH_Y)
                                    + TOP_BEARING_REGULAR * TOTALS_LABEL_SIZE
                                    - LINE_BOX * TOTALS_VALUE_SIZE,
                            0, 0, 0)));
            layeredRow(block, "TotalDue", row -> {
                row.spacing(0)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .padding(new DocumentInsets(
                                py(TOTALS_DUE_CAP_Y - TOTALS_RULE_Y) - RULE_BOX
                                        - TOP_BEARING_BOLD * TOTAL_DUE_VALUE_SIZE,
                                TOTALS_VALUE_PAD_R, 0, 0))
                        .evenWeights();
                row.addParagraph(p -> p
                        .name("TotalDueLabel")
                        .text(totals.totalLabel())
                        .textStyle(bold(TOTAL_DUE_LABEL_SIZE, INK)));
                row.addParagraph(p -> p
                        .name("TotalDueValue")
                        .text(MerchantText.coded(currencyCode, totals.totalAmount()))
                        .textStyle(bold(TOTAL_DUE_VALUE_SIZE, ACCENT))
                        .align(TextAlign.RIGHT));
            });
        });
    }

    private static void renderDueCard(SectionBuilder column, InvoicePaymentBlock payment) {
        if (payment.dueNotice().isBlank() && payment.dueNoticeEmphasis().isBlank()) {
            return;
        }
        column.addSection("DueDateCard", panel -> {
            panel.spacing(0)
                    .keepTogether()
                    .fillColor(SURFACE_SOFT)
                    .cornerRadius(PANEL_RADIUS)
                    // Measured from the total's cap, which is what sits above it
                    // in this column.
                    .margin(new DocumentInsets(
                            py(CARD_TOP_Y - TOTALS_DUE_CAP_Y)
                                    + TOP_BEARING_BOLD * TOTAL_DUE_VALUE_SIZE
                                    - LINE_BOX * TOTAL_DUE_VALUE_SIZE,
                            0, 0, 0));
            layeredRow(panel, "DueDateRow", row -> {
                row.spacing(0)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .columns(DocumentRowColumn.fixed(CARD_GUTTER),
                                DocumentRowColumn.weight(1))
                        .padding(new DocumentInsets(
                                py(CARD_ICON_Y - CARD_TOP_Y), 0,
                                py(CARD_BOTTOM_Y - CARD_ICON_Y) - CARD_ICON, CARD_PAD_L));
                row.add(MerchantIcons.icon(MerchantIcons.CALENDAR).node(CARD_ICON));
                row.addSection("DueDateText", text -> {
                    text.spacing(capGap(CARD_VALUE_CAP_Y - CARD_LABEL_CAP_Y,
                            DUE_LABEL_SIZE, true, DUE_VALUE_SIZE, true));
                    text.addParagraph(p -> p
                            .name("DueDateLabel")
                            .text(payment.dueNotice())
                            .textStyle(bold(DUE_LABEL_SIZE, INK)));
                    text.addParagraph(p -> p
                            .name("DueDateValue")
                            .text(payment.dueNoticeEmphasis())
                            .textStyle(bold(DUE_VALUE_SIZE, INK)));
                });
            });
        });
    }
}
