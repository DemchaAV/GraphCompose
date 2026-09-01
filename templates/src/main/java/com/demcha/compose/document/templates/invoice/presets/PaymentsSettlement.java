package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_DISC_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_DISC_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_FIELD_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_HEAD_BOTTOM_PX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_HEAD_GAP;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_PAD;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CARD_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DIVIDER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DUE_PAD;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DUE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DUE_SUB_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_MED;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.SETTLEMENT_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.SETTLEMENT_RIGHT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_TOP_BEARING;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_TEXT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTALS_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTAL_DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTAL_DUE_SUB_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TOTAL_DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.capOffset;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

/**
 * The settlement row: the payment card against the totals and the panel that
 * closes them.
 *
 * <p>The split is not the even one the two rows above it use — the card is
 * narrower than the totals cell, and the divider sits <em>inside</em> the
 * gutter rather than on either cell's edge. That asymmetry is in the design.</p>
 */
final class PaymentsSettlement {

    private PaymentsSettlement() {
    }

    /**
     * Draws the row.
     *
     * @param page         the page flow
     * @param payment      the bank details and the due notice
     * @param totals       the summed rows and the amount due
     * @param currencyCode the ISO code the figures are stated in
     */
    static void render(PageFlowBuilder page, InvoicePaymentBlock payment,
                       InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("SettlementRow", row -> {
            row.spacing(SETTLEMENT_GUTTER)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(CARD_W), DocumentRowColumn.weight(1));
            row.addSection("PaymentDetails", cell -> renderCard(cell, payment));
            row.addSection("Settlement", cell -> {
                // The divider sits inside the gutter rather than on either cell's
                // edge, so it is this cell's left border and the cell then pads
                // itself back out to where the panel's edge actually is.
                cell.spacing(0)
                        .borders(DocumentBorders.left(DocumentStroke.of(DIVIDER, RULE_THIN)))
                        .padding(new DocumentInsets(0, 0, 0, SETTLEMENT_RIGHT_INSET));
                renderTotals(cell, totals, currencyCode);
                renderDuePanel(cell, payment, totals, currencyCode);
            });
        });
    }

    /**
     * A tinted rounded card with no border — the fill alone separates it from
     * the page. Built from a fill, a radius and a padding rather than a panel
     * helper, which takes one padding value where this card's insets are
     * asymmetric.
     */
    private static void renderCard(SectionBuilder cell, InvoicePaymentBlock payment) {
        cell.spacing(0)
                .fillColor(CARD_SURFACE)
                .cornerRadius(CARD_RADIUS)
                .padding(CARD_PAD)
                .keepTogether();

        PaymentsWidgets.layeredRow(cell, "CardHead", row -> {
            row.spacing(CARD_HEAD_GAP)
                    .verticalAlign(RowVerticalAlign.TOP)
                    // The disc's own inset lives inside this column, so the column
                    // has to be wide enough for both — otherwise the heading
                    // starts on the disc's left edge plus the gap rather than on
                    // its right edge plus the gap.
                    .columns(DocumentRowColumn.fixed(CARD_DISC_INSET + CARD_DISC_D),
                            DocumentRowColumn.weight(1));
            row.addSection("CardDisc", disc -> disc
                    .spacing(0)
                    .padding(new DocumentInsets(0, 0, 0, CARD_DISC_INSET))
                    .add(PaymentsWidgets.disc(PaymentsIcons.BANK, CARD_DISC_SURFACE,
                            CARD_DISC_D, CARD_GLYPH)));
            row.addParagraph(p -> p
                    .name("CardLabel")
                    .text(payment.heading())
                    .textStyle(style(CARD_HEAD_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                    .margin(new DocumentInsets(
                            capOffset(1078 - 1065, CARD_HEAD_SIZE), 0, 0, 0)));
        });

        // Two stacks, as in the metadata grid: the rows line up on a shared
        // pitch rather than on a two-column row each.
        PaymentsWidgets.layeredRow(cell, "CardFields", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    // Measured from the head ROW's bottom edge, which the disc
                    // sets — not from the heading's own line box, which is
                    // shorter than the disc and therefore not what the fields
                    // follow.
                    .margin(new DocumentInsets(px(1110 - CARD_HEAD_BOTTOM_PX)
                            - TEXT_TOP_BEARING * CARD_LABEL_SIZE, 0, 0, CARD_TEXT_INDENT))
                    .columns(DocumentRowColumn.fixed(CARD_LABEL_W), DocumentRowColumn.weight(1));
            row.addSection("CardFieldLabels", labels -> {
                labels.spacing(gap(CARD_FIELD_PITCH, CARD_LABEL_SIZE));
                int index = 0;
                for (InvoicePaymentBlock.Field field : payment.fields()) {
                    String name = "CardFieldLabel" + (++index);
                    labels.addParagraph(p -> p
                            .name(name)
                            .text(field.label())
                            .textStyle(style(CARD_LABEL_SIZE, INK, DocumentTextDecoration.BOLD)));
                }
            });
            row.addSection("CardFieldValues", values -> {
                values.spacing(gap(CARD_FIELD_PITCH, CARD_VALUE_SIZE));
                int index = 0;
                for (InvoicePaymentBlock.Field field : payment.fields()) {
                    String name = "CardFieldValue" + (++index);
                    values.addParagraph(p -> p
                            .name(name)
                            .text(field.value())
                            .textStyle(style(CARD_VALUE_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT)));
                }
            });
        });
    }

    private static void renderTotals(SectionBuilder cell, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        cell.addSection("Totals", block -> {
            block.spacing(0)
                    .padding(new DocumentInsets(0, TOTALS_PAD_R, 0, TOTALS_TEXT_INSET))
                    .margin(new DocumentInsets(
                            capOffset(1082 - 1060, TOTALS_LABEL_SIZE), 0, 0, 0))
                    .keepTogether();
            PaymentsWidgets.layeredRow(block, "TotalsGrid", row -> {
                row.spacing(0).verticalAlign(RowVerticalAlign.TOP).evenWeights();
                row.addSection("TotalsLabels", labels -> {
                    labels.spacing(gap(TOTALS_PITCH, TOTALS_LABEL_SIZE));
                    int index = 0;
                    for (InvoiceTotalsBlock.Row entry : totals.rows()) {
                        String name = "TotalsLabel" + (++index);
                        labels.addParagraph(p -> p
                                .name(name)
                                .text(entry.label())
                                .textStyle(style(TOTALS_LABEL_SIZE, INK,
                                        DocumentTextDecoration.BOLD)));
                    }
                });
                row.addSection("TotalsValues", values -> {
                    values.spacing(gap(TOTALS_PITCH, TOTALS_VALUE_SIZE));
                    int index = 0;
                    for (InvoiceTotalsBlock.Row entry : totals.rows()) {
                        String name = "TotalsValue" + (++index);
                        values.addParagraph(p -> p
                                .name(name)
                                .text(PaymentsText.amount(currencyCode, entry.amount()))
                                .textStyle(style(TOTALS_VALUE_SIZE, BODY,
                                        DocumentTextDecoration.DEFAULT))
                                .align(TextAlign.RIGHT));
                    }
                });
            });
        });

        cell.addLine(line -> line
                .name("TotalsRule")
                .horizontal(TOTALS_RULE_W)
                .thickness(RULE_MED)
                .color(DIVIDER)
                // Measured cap-to-rule, less what the last label's box hangs
                // below its own cap: the line box minus the top bearing, not the
                // whole line box.
                .margin(new DocumentInsets(
                        px(1154 - 1120) - (LINE_BOX - TEXT_TOP_BEARING) * TOTALS_LABEL_SIZE,
                        0,
                        px(1173 - 1156),
                        0)));
    }

    /**
     * Navy fill owning white type. CENTER, not TOP: the amount is centred
     * against the label block rather than aligned to its first line.
     *
     * <p>The line under the label is the payment block's due notice — the sheet
     * says when the money is wanted twice, once here beside the amount and once
     * in the footer, and both come from the same field.</p>
     */
    private static void renderDuePanel(SectionBuilder cell, InvoicePaymentBlock payment,
                                       InvoiceTotalsBlock totals, String currencyCode) {
        cell.addSection("TotalDuePanel", panel -> {
            panel.spacing(0)
                    .fillColor(INK)
                    .cornerRadius(DUE_RADIUS)
                    .padding(DUE_PAD)
                    .keepTogether();
            PaymentsWidgets.layeredRow(panel, "TotalDueRow", row -> {
                row.spacing(0).verticalAlign(RowVerticalAlign.CENTER).evenWeights();
                row.addSection("TotalDueText", text -> {
                    text.spacing(0);
                    text.addParagraph(p -> p
                            .name("TotalDueLabel")
                            .text(totals.totalLabel())
                            .textStyle(style(TOTAL_DUE_LABEL_SIZE, SURFACE,
                                    DocumentTextDecoration.BOLD)));
                    if (!payment.dueNotice().isBlank()) {
                        text.addParagraph(p -> p
                                .name("TotalDueSubLabel")
                                .text(payment.dueNotice())
                                .textStyle(style(TOTAL_DUE_SUB_SIZE, SURFACE,
                                        DocumentTextDecoration.DEFAULT))
                                .margin(new DocumentInsets(
                                        px(DUE_SUB_PITCH) - LINE_BOX * TOTAL_DUE_LABEL_SIZE
                                                + TEXT_TOP_BEARING
                                                * (TOTAL_DUE_LABEL_SIZE - TOTAL_DUE_SUB_SIZE),
                                        0, 0, 0)));
                    }
                });
                row.addParagraph(p -> p
                        .name("TotalDueValue")
                        .text(PaymentsText.amount(currencyCode, totals.totalAmount()))
                        .textStyle(style(TOTAL_DUE_VALUE_SIZE, SURFACE,
                                DocumentTextDecoration.BOLD))
                        .align(TextAlign.RIGHT));
            });
        });
    }
}
