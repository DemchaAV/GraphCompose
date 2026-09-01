package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.BILL_TO_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.DIVIDER_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LEFT_COLUMN_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_DISC_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PARTY_TAX_GAP_PX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.SHIP_TO_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.spaces;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.style;
import static com.demcha.compose.document.templates.invoice.presets.MerchantMasthead.layeredRow;

/**
 * The two addressed parties, each under a heading opening with a filled disc.
 */
final class MerchantParties {

    private MerchantParties() {
    }

    /**
     * The parties row.
     *
     * @param page   the page flow
     * @param billTo the billed party
     * @param shipTo the shipped-to party, or {@code null} when the invoice ships
     *               nowhere and the column stays empty
     */
    static void render(PageFlowBuilder page, InvoiceRecipient billTo, InvoiceRecipient shipTo) {
        page.addRow("PartiesRow", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(LEFT_COLUMN_W),
                            DocumentRowColumn.fixed(DIVIDER_W),
                            DocumentRowColumn.weight(1))
                    // The row's top is the discs'; the rule above it occupies a
                    // whole point whatever it paints.
                    .margin(new DocumentInsets(py(451 - 423) - RULE_BOX, 0, 0, 0));
            row.addSection("BillToColumn", column ->
                    renderParty(column, "BillTo", billTo, MerchantIcons.BILL_TO, BILL_TO_PAD_L));
            row.addSection("PartyColumnDivider", column -> {
                column.spacing(0);
                // The divider outlasts both columns' text, which is why its own
                // length is measured rather than taken from either of them.
                column.addLine(line -> line
                        .name("PartyColumnDivider")
                        .vertical(py(640 - 452))
                        .thickness(DIVIDER_W)
                        .color(RULE_SOFT)
                        .margin(new DocumentInsets(py(452 - 451), 0, 0, 0)));
            });
            row.addSection("ShipToColumn", column -> {
                if (shipTo != null) {
                    renderParty(column, "ShipTo", shipTo, MerchantIcons.SHIP_TO, SHIP_TO_PAD_L);
                } else {
                    column.spacing(0);
                }
            });
        });
    }

    private static void renderParty(SectionBuilder column, String id, InvoiceRecipient party,
                                    String token, double padLeft) {
        column.spacing(0).padding(new DocumentInsets(0, 0, 0, padLeft));

        layeredRow(column, id + "Label", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(PARTY_GUTTER), DocumentRowColumn.weight(1));
            row.add(new ShapeContainerBuilder()
                    .name(id + "Disc")
                    .circle(PARTY_DISC_D)
                    .fillColor(ACCENT)
                    .center(MerchantIcons.icon(token).node(PARTY_DISC_GLYPH))
                    .build());
            row.addParagraph(p -> p
                    .name(id + "LabelText")
                    .text(party.heading())
                    .textStyle(bold(PARTY_LABEL_SIZE, ACCENT)));
        });

        column.addSection(id + "Body", block -> {
            block.spacing(0).padding(new DocumentInsets(0, 0, 0, PARTY_GUTTER));
            // The label row is as tall as the disc, so the party name is measured
            // from the disc's foot rather than from the label's.
            block.addParagraph(p -> p
                    .name(id + "Name")
                    .text(party.name())
                    .textStyle(bold(PARTY_NAME_SIZE, INK))
                    .margin(capTop(498 - 495, PARTY_NAME_SIZE, true)));

            block.addSection(id + "Address", lines -> {
                lines.spacing(capGap(24, BODY_SIZE, false, BODY_SIZE, false))
                        .margin(new DocumentInsets(
                                capGap(527 - 498, PARTY_NAME_SIZE, true, BODY_SIZE, false),
                                0, 0, 0));
                List<String> address = party.addressLines();
                for (int i = 0; i < address.size(); i++) {
                    String line = address.get(i);
                    int index = i;
                    lines.addParagraph(p -> p
                            .name(id + "Address_" + index)
                            .text(line)
                            .textStyle(style(BODY_SIZE, INK)));
                }
            });

            if (!party.registrationNumber().isBlank()) {
                block.addParagraph(p -> {
                    p.name(id + "Registration");
                    p.inlineText(party.registrationLabel(), style(BODY_SIZE, INK));
                    p.inlineText(spaces(PARTY_TAX_GAP_PX, BODY_SIZE), style(BODY_SIZE, INK));
                    p.inlineText(party.registrationNumber(), style(BODY_SIZE, INK));
                    p.margin(new DocumentInsets(
                            capGap(621 - 575, BODY_SIZE, false, BODY_SIZE, false), 0, 0, 0));
                });
            }
        });
    }
}
