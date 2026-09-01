package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_STROKE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_CARD_BOTTOM;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_CARD_TOP;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_DUE_AT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_PAD_R;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_ROW_AT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_RULE_AT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_RULE_OVERHANG;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTALS_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTAL_DUE_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TOTAL_DUE_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.topBearing;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.layeredRow;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.rule;

/**
 * The totals card, on the right half of the sheet under the table.
 */
final class ObsidianSettlement {

    private ObsidianSettlement() {
    }

    /**
     * The totals row.
     *
     * @param page         the page flow
     * @param totals       the summed rows and the grand total
     * @param currencyCode the code every figure carries as a mark
     */
    static void render(PageFlowBuilder page, InvoiceTotalsBlock totals, String currencyCode) {
        page.addRow("TotalsRow", row -> {
            row.spacing(CARD_GUTTER).verticalAlign(RowVerticalAlign.TOP).weights(HALF, HALF);
            row.addSection("TotalsSpacer", cell -> cell.spacing(0));
            row.addSection("TotalsCard", cell -> renderCard(cell, totals, currencyCode));
        });
    }

    private static void renderCard(SectionBuilder cell, InvoiceTotalsBlock totals,
                                   String currencyCode) {
        cell.spacing(0)
                .fillColor(SURFACE)
                .stroke(CARD_STROKE)
                .cornerRadius(CARD_RADIUS)
                .padding(new DocumentInsets(0, TOTALS_PAD_R,
                        Math.max(0, capGap(TOTALS_CARD_BOTTOM - TOTALS_DUE_AT,
                                TOTAL_DUE_VALUE_SIZE, true, 0, false)),
                        TOTALS_PAD_L))
                .margin(new DocumentInsets(px(13), 0, 0, 0))
                .keepTogether();

        List<InvoiceTotalsBlock.Row> rows = totals.rows();
        for (int i = 0; i < rows.size(); i++) {
            InvoiceTotalsBlock.Row entry = rows.get(i);
            int index = i;
            String name = "TotalsRow_" + index;
            double above = index == 0
                    ? px(TOTALS_ROW_AT[0] - TOTALS_CARD_TOP)
                            - topBearing(TOTALS_LABEL_SIZE, false)
                    : capGap(TOTALS_ROW_AT[1] - TOTALS_ROW_AT[0],
                            TOTALS_VALUE_SIZE, false, TOTALS_VALUE_SIZE, false);
            double gapAbove = Math.max(0, above);
            layeredRow(cell, name, row -> {
                row.spacing(0)
                        .margin(new DocumentInsets(gapAbove, 0, 0, 0))
                        .weights(HALF, HALF);
                row.addParagraph(p -> p
                        .name(name + "Label")
                        .text(entry.label())
                        .textStyle(plain(TOTALS_LABEL_SIZE, MUTED)));
                row.addParagraph(p -> p
                        .name(name + "Value")
                        .text(ObsidianText.money(currencyCode, entry.amount()))
                        .textStyle(plain(TOTALS_VALUE_SIZE, INK))
                        .align(TextAlign.RIGHT));
            });
        }

        // Wider than the text it separates, which is the design's own
        // arrangement: the card is padded to the text, and the rule reaches back
        // out to the card's inner edge on both sides.
        cell.addLine(line -> rule(line, "TotalsRule",
                innerWidth() + 2 * TOTALS_RULE_OVERHANG, HAIRLINE)
                .margin(new DocumentInsets(
                        Math.max(0, capGap(TOTALS_RULE_AT - TOTALS_ROW_AT[1],
                                TOTALS_VALUE_SIZE, false, 0, false)),
                        -TOTALS_RULE_OVERHANG, 0, -TOTALS_RULE_OVERHANG)));

        layeredRow(cell, "TotalDue", row -> {
            row.spacing(0)
                    // Centred, not baseline-aligned: the label's ink band and the
                    // larger value's share a centre line, measured.
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .margin(new DocumentInsets(
                            Math.max(0, px(TOTALS_DUE_AT - TOTALS_RULE_AT) - RULE_BOX
                                    - topBearing(TOTAL_DUE_VALUE_SIZE, true)),
                            0, 0, 0))
                    .weights(HALF, HALF);
            row.addParagraph(p -> p
                    .name("TotalDueLabel")
                    .text(totals.totalLabel())
                    .textStyle(bold(TOTAL_DUE_LABEL_SIZE, INK)));
            row.addParagraph(p -> p
                    .name("TotalDueValue")
                    .text(ObsidianText.money(currencyCode, totals.totalAmount()))
                    .textStyle(bold(TOTAL_DUE_VALUE_SIZE, ACCENT))
                    .align(TextAlign.RIGHT));
        });
    }

    private static double innerWidth() {
        return (CONTENT_W - CARD_GUTTER) * HALF - TOTALS_PAD_L - TOTALS_PAD_R;
    }
}
