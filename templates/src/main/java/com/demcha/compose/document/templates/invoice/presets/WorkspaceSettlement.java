package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT_BORDER;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_ICON;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_INNER_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_PAD;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_TEXT_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CARD_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_PAD;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_TILE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_TILE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_TILE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.NOTE_ICON_COL;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.RULE_MED;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SETTLEMENT_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SETTLEMENT_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SETTLEMENT_RIGHT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SETTLEMENT_RIGHT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTALS_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTALS_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTALS_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTALS_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTAL_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TOTAL_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.capOffset;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.style;

/**
 * The settlement row: the payment card against the totals and the panel that
 * closes them.
 *
 * <p>The split is not the half the two rows above it use — the left cell is
 * narrower than the right, with a wide gutter between. That asymmetry is in the
 * design.</p>
 */
final class WorkspaceSettlement {

    private WorkspaceSettlement() {
    }

    /**
     * Draws the row.
     *
     * @param page         the page flow
     * @param payment      the bank details, the note and the due notice
     * @param totals       the summed rows and the amount due
     * @param currencyCode the ISO code the figures are stated in
     */
    static void render(PageFlowBuilder page, InvoicePaymentBlock payment,
                       InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("SettlementRow", row -> {
            row.spacing(SETTLEMENT_GUTTER).weights(SETTLEMENT_LEFT, SETTLEMENT_RIGHT);
            row.addSection("PaymentDetails", cell -> renderCard(cell, payment));
            row.addSection("Settlement", cell -> {
                cell.spacing(0).padding(new DocumentInsets(0, SETTLEMENT_RIGHT_INSET, 0, 0));
                renderTotals(cell, totals, currencyCode);
                renderDuePanel(cell, payment);
            });
        });
    }

    /**
     * An outlined rounded card. Built from a fill, a stroke, a radius and a
     * padding rather than a panel helper, which takes one padding value where
     * this card's insets are asymmetric.
     */
    private static void renderCard(SectionBuilder cell, InvoicePaymentBlock payment) {
        cell.spacing(0)
                .fillColor(SURFACE)
                .stroke(DocumentStroke.of(ACCENT_BORDER, RULE_MED))
                .cornerRadius(CARD_RADIUS)
                .padding(CARD_PAD)
                .keepTogether();

        WorkspaceWidgets.layeredRow(cell, "CardHead", row -> {
            row.spacing(CARD_ICON_GAP)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(CARD_ICON), DocumentRowColumn.weight(1));
            row.addSection("CardIcon", glyph -> glyph
                    .spacing(0)
                    .add(WorkspaceWidgets.glyph(WorkspaceIcons.BANK, CARD_ICON)));
            row.addParagraph(p -> p
                    .name("CardLabel")
                    .text(payment.heading())
                    .textStyle(style(CARD_TITLE_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                    .margin(new DocumentInsets(capOffset(10, CARD_TITLE_SIZE), 0, 0, 0)));
        });

        cell.addSection("CardFields", fields -> {
            // The head row is as tall as its glyph, and the first field sits a
            // measured distance below the head's top — not below the heading's
            // own line box, which is shorter than the glyph.
            fields.spacing(gap(22.4, CARD_TEXT_SIZE))
                    .margin(new DocumentInsets(px(41 - 27 - 3), 0, 0, 0));
            int index = 0;
            for (InvoicePaymentBlock.Field field : payment.fields()) {
                String name = "CardField" + (++index);
                WorkspaceWidgets.layeredRow(fields, name, row -> {
                    row.spacing(0)
                            .columns(DocumentRowColumn.fixed(CARD_LABEL_W),
                                    DocumentRowColumn.weight(1));
                    row.addParagraph(p -> p
                            .name(name + "Label")
                            .text(field.label())
                            .textStyle(style(CARD_TEXT_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT)));
                    row.addParagraph(p -> p
                            .name(name + "Value")
                            .text(field.value())
                            .textStyle(style(CARD_TEXT_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT)));
                });
            }
        });

        if (payment.instruction().isBlank()) {
            return;
        }
        cell.addLine(line -> line
                .name("CardRule")
                .horizontal(CARD_INNER_W)
                .thickness(RULE_THIN)
                .color(HAIRLINE)
                .margin(new DocumentInsets(gap(23, CARD_TEXT_SIZE), 0, px(16), 0)));

        // Two cells rather than an inline glyph: the design's wrapped second
        // line returns to the TEXT axis, not to the glyph's.
        WorkspaceWidgets.layeredRow(cell, "CardNote", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(NOTE_ICON_COL), DocumentRowColumn.weight(1));
            row.addSection("CardNoteIcon", glyph -> glyph
                    .spacing(0)
                    .add(WorkspaceWidgets.glyph(WorkspaceIcons.INFO, NOTE_ICON,
                            HorizontalAlign.LEFT)));
            row.addParagraph(p -> p
                    .name("CardNoteText")
                    .text(payment.instruction())
                    .textStyle(style(CARD_TEXT_SIZE, MUTED, DocumentTextDecoration.DEFAULT))
                    .lineSpacing(gap(22, CARD_TEXT_SIZE)));
        });
    }

    private static void renderTotals(SectionBuilder cell, InvoiceTotalsBlock totals,
                                     String currencyCode) {
        cell.addSection("Totals", block -> {
            block.spacing(gap(35, TOTALS_SIZE))
                    .padding(new DocumentInsets(0, TOTALS_PAD_R, 0, TOTALS_PAD_L))
                    .margin(new DocumentInsets(px(10.3), 0, 0, 0))
                    .keepTogether();
            int index = 0;
            for (InvoiceTotalsBlock.Row entry : totals.rows()) {
                String name = "Total" + (++index);
                WorkspaceWidgets.layeredRow(block, name, row -> {
                    row.spacing(0).weights(HALF, HALF);
                    row.addParagraph(p -> p
                            .name(name + "Label")
                            .text(entry.label())
                            .textStyle(style(TOTALS_SIZE, INK, DocumentTextDecoration.DEFAULT)));
                    row.addParagraph(p -> p
                            .name(name + "Value")
                            .text(WorkspaceText.money(entry.amount()))
                            .textStyle(style(TOTALS_SIZE, INK, DocumentTextDecoration.DEFAULT))
                            .align(TextAlign.RIGHT));
                });
            }
        });

        // Accent-coloured, not a grey hairline: a colour scan across it returns
        // a clear violet hue.
        cell.addLine(line -> line
                .name("TotalsRule")
                .horizontal(TOTALS_RULE_W)
                .thickness(RULE_MED)
                .color(ACCENT)
                .margin(new DocumentInsets(gap(35, TOTALS_SIZE), 0, px(24.3), TOTALS_PAD_L)));

        cell.addSection("TotalDue", block -> {
            block.spacing(0)
                    .padding(new DocumentInsets(0, TOTALS_PAD_R, 0, TOTALS_PAD_L))
                    .keepTogether();
            WorkspaceWidgets.layeredRow(block, "TotalDueRow", row -> {
                row.spacing(0).verticalAlign(RowVerticalAlign.CENTER).weights(HALF, HALF);
                row.addParagraph(p -> p
                        .name("TotalDueLabel")
                        .text(totals.totalLabel())
                        .textStyle(style(TOTAL_LABEL_SIZE, INK, DocumentTextDecoration.BOLD)));
                row.addParagraph(p -> p
                        .name("TotalDueValue")
                        .text(WorkspaceText.coded(currencyCode, totals.totalAmount()))
                        .textStyle(style(TOTAL_VALUE_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                        .align(TextAlign.RIGHT));
            });
        });
    }

    /**
     * The calendar tile has to be a shape: a bare glyph on the panel would lose
     * the white square the design draws behind it, which is the only thing
     * separating the glyph from the panel's tint.
     */
    private static void renderDuePanel(SectionBuilder cell, InvoicePaymentBlock payment) {
        if (payment.dueNotice().isBlank() && payment.dueNoticeEmphasis().isBlank()) {
            return;
        }
        cell.addSection("PaymentDuePanel", panel -> {
            panel.spacing(0)
                    .fillColor(ACCENT_SURFACE)
                    .stroke(DocumentStroke.of(ACCENT_BORDER, RULE_MED))
                    .cornerRadius(DUE_RADIUS)
                    .padding(DUE_PAD)
                    .margin(new DocumentInsets(px(42.7), 0, 0, 0))
                    .keepTogether();
            WorkspaceWidgets.layeredRow(panel, "PaymentDueRow", row -> {
                row.spacing(DUE_TILE_GAP)
                        .verticalAlign(RowVerticalAlign.TOP)
                        .columns(DocumentRowColumn.fixed(DUE_TILE), DocumentRowColumn.weight(1));
                row.addSection("DueTile", tile -> tile
                        .spacing(0)
                        .add(new ShapeContainerBuilder()
                                .name("DueTileShape")
                                .roundedRect(DUE_TILE, DUE_TILE, DUE_TILE_RADIUS)
                                .fillColor(SURFACE)
                                .center(WorkspaceWidgets.glyph(WorkspaceIcons.CALENDAR, DUE_GLYPH))
                                .build()));
                row.addSection("DueText", text -> {
                    text.spacing(gap(23.6, DUE_LABEL_SIZE))
                            .margin(new DocumentInsets(capOffset(5, DUE_LABEL_SIZE), 0, 0, 0));
                    text.addParagraph(p -> p
                            .name("DueLabel")
                            .text(payment.dueNotice())
                            .textStyle(style(DUE_LABEL_SIZE, INK,
                                    DocumentTextDecoration.DEFAULT)));
                    text.addParagraph(p -> p
                            .name("DueValue")
                            .text(payment.dueNoticeEmphasis())
                            .textStyle(style(DUE_VALUE_SIZE, INK, DocumentTextDecoration.BOLD)));
                });
            });
        });
    }
}
