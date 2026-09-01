package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CAP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD_DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD_DISC_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_BORDER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_BOTTOM_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_HEAD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_ICON_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_LAST_ROW_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_NOTE_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_NOTE_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_NOTE_LAST_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_PAD_ROWS_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_PAD_X;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_ROW1_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_ROW_PITCH_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_RULE_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_TOP_Y;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PANEL_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RIGHT_COLUMN_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SUMMARY_GAP_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TOTALS_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TOTALS_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TOTALS_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TOTAL_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TOTAL_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.WHITE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;
import static com.demcha.compose.document.templates.invoice.presets.PlatformWidgets.disc;
import static com.demcha.compose.document.templates.invoice.presets.PlatformWidgets.layeredRow;

/**
 * The band under the table: the outlined bank panel on the left, and on the
 * right the totals over the filled card carrying the due date.
 */
final class PlatformSettlement {

    private PlatformSettlement() {
    }

    /**
     * The settlement row.
     *
     * <p>The card stops short of the right margin, so the row carries the
     * remainder as a cell of its own rather than letting the right column stretch
     * into it.</p>
     */
    static void render(PageFlowBuilder page, InvoicePaymentBlock payment,
                       InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("Settlement", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(PANEL_W),
                            DocumentRowColumn.fixed(SUMMARY_GAP_W),
                            DocumentRowColumn.fixed(RIGHT_COLUMN_W),
                            DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(py(1046 - 1034), 0, 0, 0));
            row.addSection("PaymentPanel", column -> renderPaymentPanel(column, payment));
            row.addSection("SettlementGap", column -> column.spacing(0));
            row.addSection("SettlementRight", column -> {
                column.spacing(0);
                renderTotals(column, totals, currencyCode);
                renderDueCard(column, payment);
            });
            row.addSection("SettlementTail", column -> column.spacing(0));
        });
    }

    private static void renderPaymentPanel(SectionBuilder column, InvoicePaymentBlock payment) {
        column.spacing(0)
                .keepTogether()
                .fillColor(WHITE)
                .stroke(DocumentStroke.of(PANEL_BORDER, RULE_THICK))
                .cornerRadius(PANEL_RADIUS);

        layeredRow(column, "PaymentHeading", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(PANEL_HEAD_GUTTER),
                            DocumentRowColumn.weight(1))
                    .padding(new DocumentInsets(py(PANEL_ICON_Y - PANEL_TOP_Y), 0, 0, PANEL_PAD_X));
            row.add(PlatformIcons.icon(PlatformIcons.BANK).node(PANEL_ICON));
            row.addParagraph(p -> p
                    .name("PaymentHeadingText")
                    .text(payment.heading())
                    .textStyle(bold(LABEL_SIZE, ACCENT)));
        });

        column.addSection("PaymentRows", rows -> {
            rows.spacing(capPitch(PANEL_ROW_PITCH_Y, SMALL_SIZE))
                    .padding(new DocumentInsets(0, PANEL_PAD_X, 0, PANEL_PAD_ROWS_L))
                    // Measured from the mark, because the heading layer's height
                    // is the mark's and not the label's line box.
                    .margin(new DocumentInsets(
                            py(PANEL_ROW1_CAP_Y - PANEL_ICON_Y)
                                    - CAP_INSET * SMALL_SIZE - PANEL_ICON,
                            0, 0, 0));
            List<InvoicePaymentBlock.Field> fields = payment.fields();
            for (int i = 0; i < fields.size(); i++) {
                InvoicePaymentBlock.Field field = fields.get(i);
                layeredRow(rows, "PaymentRow_" + i, row -> {
                    row.spacing(0)
                            .columns(DocumentRowColumn.fixed(PANEL_LABEL_W),
                                    DocumentRowColumn.weight(1));
                    row.addParagraph(p -> p.text(field.label()).textStyle(style(SMALL_SIZE, BODY)));
                    row.addParagraph(p -> {
                        p.text(field.value()).textStyle(style(SMALL_SIZE, INK));
                        // A bank detail is a reference, not a destination —
                        // except the remittance address, the one field on the
                        // panel a reader would act on.
                        if (field.value().contains("@")) {
                            p.link(InvoiceUri.mailLink(field.value()));
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
                .horizontal(PANEL_W)
                .thickness(RULE_THICK)
                .color(HAIRLINE)
                .margin(new DocumentInsets(
                        py(PANEL_RULE_Y - PANEL_LAST_ROW_CAP_Y)
                                - (LINE_BOX - CAP_INSET) * SMALL_SIZE,
                        0, 0, 0)));
        layeredRow(column, "PaymentNote", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(PANEL_NOTE_GUTTER),
                            DocumentRowColumn.weight(1))
                    .padding(new DocumentInsets(
                            py(PANEL_NOTE_CAP_Y - PANEL_RULE_Y) - CAP_INSET * SMALL_SIZE,
                            0,
                            py(PANEL_BOTTOM_Y - PANEL_NOTE_LAST_CAP_Y)
                                    - (LINE_BOX - CAP_INSET) * SMALL_SIZE,
                            PANEL_PAD_X));
            row.add(PlatformIcons.icon(PlatformIcons.INFO).node(PANEL_NOTE_ICON));
            row.addSection("PaymentNoteText", text -> {
                text.spacing(capPitch(22, SMALL_SIZE));
                String[] lines = payment.instruction().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String prose = lines[i];
                    int index = i;
                    text.addParagraph(p -> p
                            .name("PaymentNoteLine_" + index)
                            .text(prose)
                            .textStyle(style(SMALL_SIZE, BODY)));
                }
            });
        });
    }

    /**
     * The summed rows and the grand total.
     *
     * <p>The summed rows are written bare, under a column that already states
     * the currency; the grand total carries the code, because it stands alone
     * under no caption and a bare figure there names no currency at all.</p>
     */
    private static void renderTotals(SectionBuilder column, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        column.addSection("Totals", block -> {
            block.spacing(0)
                    .keepTogether()
                    .padding(new DocumentInsets(0, 0, 0, TOTALS_PAD_L))
                    .margin(new DocumentInsets(py(1065 - 1046) - CAP_INSET * BODY_SIZE, 0, 0, 0));
            List<InvoiceTotalsBlock.Row> rows = totals.rows();
            for (int i = 0; i < rows.size(); i++) {
                InvoiceTotalsBlock.Row entry = rows.get(i);
                boolean first = i == 0;
                layeredRow(block, "TotalsRow_" + i, row -> {
                    row.spacing(0)
                            .padding(new DocumentInsets(0, TOTALS_PAD_R, 0, 0))
                            .margin(new DocumentInsets(
                                    first ? 0 : capPitch(31, BODY_SIZE), 0, 0, 0))
                            .evenWeights();
                    row.addParagraph(p -> p.text(entry.label()).textStyle(style(BODY_SIZE, BODY)));
                    row.addParagraph(p -> p
                            .text(PlatformText.money(entry.amount()))
                            .textStyle(style(BODY_SIZE, INK))
                            .align(TextAlign.RIGHT));
                });
            }
            block.addLine(line -> line
                    .name("TotalsRule")
                    .horizontal(TOTALS_RULE_W)
                    .thickness(RULE_THICK)
                    .color(HAIRLINE)
                    .margin(new DocumentInsets(
                            py(1127) - (py(1096) + (LINE_BOX - CAP_INSET) * BODY_SIZE), 0, 0, 0)));
            layeredRow(block, "TotalDue", row -> {
                row.spacing(0)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .padding(new DocumentInsets(
                                py(1155 - 1129) - CAP_INSET * TOTAL_VALUE_SIZE,
                                TOTALS_PAD_R, 0, 0))
                        .evenWeights();
                row.addParagraph(p -> p
                        .name("TotalDueLabel")
                        .text(totals.totalLabel())
                        .textStyle(bold(TOTAL_LABEL_SIZE, INK)));
                row.addParagraph(p -> p
                        .name("TotalDueValue")
                        .text(PlatformText.coded(currencyCode, totals.totalAmount()))
                        .textStyle(bold(TOTAL_VALUE_SIZE, ACCENT))
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
                    .fillColor(CARD)
                    .cornerRadius(CARD_RADIUS)
                    .margin(new DocumentInsets(py(1215 - 1182), 0, 0, 0));
            layeredRow(panel, "DueDateRow", row -> {
                row.spacing(0)
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .columns(DocumentRowColumn.fixed(CARD_GUTTER), DocumentRowColumn.weight(1))
                        .padding(new DocumentInsets(
                                py(1235 - 1215), 0, py(1306 - 1285), CARD_PAD_L));
                row.add(disc("DueDateDisc", PlatformIcons.CALENDAR, CARD_DISC_D, CARD_DISC_ICON));
                row.addSection("DueDateText", text -> {
                    text.spacing(capPitch(28, LABEL_SIZE, DUE_VALUE_SIZE));
                    text.addParagraph(p -> p
                            .name("DueDateLabel")
                            .text(payment.dueNotice())
                            .textStyle(bold(LABEL_SIZE, ACCENT)));
                    text.addParagraph(p -> p
                            .name("DueDateValue")
                            .text(payment.dueNoticeEmphasis())
                            .textStyle(bold(DUE_VALUE_SIZE, INK)));
                });
            });
        });
    }
}
