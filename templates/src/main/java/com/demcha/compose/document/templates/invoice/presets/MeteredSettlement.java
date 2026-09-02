package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CLOSING_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CLOSING_LEFT_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CLOSING_RIGHT_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_PADDING;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_TEXT_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.DUE_VALUE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_BORDER;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_FILL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_HEAD_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_PADDING;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_ROW_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_TEXT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_TABLE_TO_CLOSING;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUMMARY_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUMMARY_INSET_L;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUMMARY_INSET_R;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUMMARY_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTALS_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTALS_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTALS_RULE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTALS_RULE_T;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTALS_VALUE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTAL_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TOTAL_VALUE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.WHITE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.iconHeading;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.labelledRow;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.layeredRow;

/**
 * The band under the table: the bank details on the left, and on the right the
 * totals over the card that carries the due date.
 */
final class MeteredSettlement {

    private MeteredSettlement() {
    }

    /**
     * The closing row.
     *
     * @param body         the page's body section
     * @param payment      the bank details and the due notice
     * @param totals       the summed rows and the grand total
     * @param currencyCode the code every figure is written behind
     */
    static void render(SectionBuilder body, InvoicePaymentBlock payment,
                       InvoiceTotalsBlock totals, String currencyCode) {
        body.addRow("ClosingRow", row -> row
                .margin(SEAM_TABLE_TO_CLOSING)
                .verticalAlign(RowVerticalAlign.BOTTOM)
                .gap(CLOSING_GUTTER)
                .columns(DocumentRowColumn.fixed(CLOSING_LEFT_W),
                        DocumentRowColumn.fixed(CLOSING_RIGHT_W))
                .addSection("PaymentColumn", left -> renderPaymentDetails(left, payment))
                .addSection("SummaryColumn", right -> {
                    right.spacing(SUMMARY_GAP);
                    renderTotals(right, totals, currencyCode);
                    renderDueBy(right, payment);
                }));
    }

    private static void renderTotals(SectionBuilder column, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        column.addSection("Totals", block -> {
            block.keepTogether();
            block.padding(new DocumentInsets(0, SUMMARY_INSET_R, 0, SUMMARY_INSET_L));
            block.spacing(TOTALS_GAP);
            List<InvoiceTotalsBlock.Row> rows = totals.rows();
            for (int i = 0; i < rows.size(); i++) {
                InvoiceTotalsBlock.Row row = rows.get(i);
                totalsRow(block, "TotalsRow_" + i, row.label(),
                        MeteredText.codedMoney(currencyCode, row.amount()),
                        TOTALS_LABEL, TOTALS_VALUE);
            }
            block.addLine(l -> l
                    .name("TotalsRule")
                    .horizontal(SUMMARY_W)
                    .thickness(TOTALS_RULE_T)
                    .color(ACCENT)
                    .margin(new DocumentInsets(TOTALS_RULE_GAP, 0, TOTALS_RULE_GAP, 0)));
            totalsRow(block, "TotalDue", totals.totalLabel(),
                    MeteredText.codedMoney(currencyCode, totals.totalAmount()),
                    TOTAL_LABEL, TOTAL_VALUE);
        });
    }

    private static void totalsRow(SectionBuilder parent, String name, String label, String value,
                                  DocumentTextStyle labelStyle, DocumentTextStyle valueStyle) {
        layeredRow(parent, name, row -> row
                .verticalAlign(RowVerticalAlign.CENTER)
                .weights(1, 1)
                .addParagraph(p -> p.text(label).textStyle(labelStyle))
                .addParagraph(p -> p.text(value).textStyle(valueStyle).align(TextAlign.RIGHT)));
    }

    private static void renderPaymentDetails(SectionBuilder column, InvoicePaymentBlock payment) {
        column.addSection("PaymentDetails", card -> {
            card.keepTogether();
            card.softPanel(WHITE, PANEL_RADIUS, PANEL_PADDING,
                    DocumentStroke.of(PANEL_BORDER, HAIRLINE));
            card.spacing(PANEL_ROW_GAP);
            iconHeading(card, "PaymentDetailsHeading", MeteredIcons.CARD,
                    PANEL_ICON, PANEL_ICON_GAP, payment.heading(), PANEL_TITLE);
            card.addSection("PaymentDetailsRows", rows -> {
                rows.margin(new DocumentInsets(PANEL_HEAD_GAP, 0, 0, 0));
                rows.spacing(PANEL_ROW_GAP);
                List<InvoicePaymentBlock.Field> fields = payment.fields();
                for (int i = 0; i < fields.size(); i++) {
                    InvoicePaymentBlock.Field field = fields.get(i);
                    labelledRow(rows, "PaymentDetail_" + i, field.label(), field.value(),
                            PANEL_LABEL_W, PANEL_TEXT, PANEL_TEXT, linkFor(field.value()));
                }
            });
        });
    }

    /**
     * A bank detail is a reference, not a destination — except the remittance
     * address, which is the one field on the panel a reader would act on.
     */
    private static DocumentLinkOptions linkFor(String value) {
        return value.contains("@") ? InvoiceUri.mailLink(value) : null;
    }

    private static void renderDueBy(SectionBuilder column, InvoicePaymentBlock payment) {
        if (payment.dueNotice().isBlank() && payment.dueNoticeEmphasis().isBlank()) {
            return;
        }
        column.addSection("PaymentDueBy", card -> {
            card.keepTogether();
            card.softPanel(PANEL_FILL, PANEL_RADIUS, DUE_PADDING);
            layeredRow(card, "PaymentDueByRow", row -> row
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(DUE_ICON + DUE_ICON_GAP),
                            DocumentRowColumn.auto())
                    .addSection("PaymentDueByIcon", cell -> cell
                            .addSvgIcon(MeteredIcons.icon(MeteredIcons.CALENDAR), DUE_ICON))
                    .addSection("PaymentDueByText", text -> text
                            .spacing(DUE_TEXT_GAP)
                            .addParagraph(p -> p.name("PaymentDueByLabel")
                                    .text(payment.dueNotice()).textStyle(DUE_LABEL))
                            .addParagraph(p -> p.name("PaymentDueByValue")
                                    .text(payment.dueNoticeEmphasis()).textStyle(DUE_VALUE))));
        });
    }
}
