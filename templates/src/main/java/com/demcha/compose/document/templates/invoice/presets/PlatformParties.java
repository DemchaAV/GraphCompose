package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BILL_TO_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BILL_TO_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CAP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.DIVIDER_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LEFT_COLUMN_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PARTY_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PIN_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SHIP_TO_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SHIP_TO_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.spaces;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;
import static com.demcha.compose.document.templates.invoice.presets.PlatformWidgets.layeredRow;
import static com.demcha.compose.document.templates.invoice.presets.PlatformWidgets.partyDisc;

/**
 * The two addressed parties, split by the same hairline the identity row uses.
 *
 * <p>The two headings are not drawn alike, and that is the design's own choice
 * rather than an inconsistency: the billed party's mark sits knocked out of a
 * filled disc, the shipped-to party's is a bare pin. They also sit on different
 * gutters and different left insets, both measured.</p>
 */
final class PlatformParties {

    private PlatformParties() {
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
                    .margin(new DocumentInsets(py(432 - 409), 0, 0, 0));
            row.addSection("BillToColumn", column ->
                    renderParty(column, "BillTo", billTo, PlatformIcons.BILL_TO,
                            BILL_TO_PAD_L, BILL_TO_GUTTER, true));
            row.addSection("PartyColumnDivider", column -> {
                column.spacing(0);
                column.addLine(line -> line
                        .name("PartyColumnDivider")
                        .vertical(py(622 - 437))
                        .thickness(DIVIDER_W)
                        .color(RULE)
                        .margin(new DocumentInsets(py(437 - 432), 0, 0, 0)));
            });
            row.addSection("ShipToColumn", column -> {
                if (shipTo != null) {
                    renderParty(column, "ShipTo", shipTo, PlatformIcons.SHIP_TO,
                            SHIP_TO_PAD_L, SHIP_TO_GUTTER, false);
                } else {
                    column.spacing(0);
                }
            });
        });
    }

    private static void renderParty(SectionBuilder column, String id, InvoiceRecipient party,
                                    String token, double padLeft, double gutter,
                                    boolean drawsDisc) {
        column.spacing(0).padding(new DocumentInsets(0, 0, 0, padLeft));

        layeredRow(column, id + "Label", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(gutter), DocumentRowColumn.weight(1));
            row.add(drawsDisc
                    ? partyDisc(id + "Disc", token)
                    : PlatformIcons.icon(token).node(PIN_W));
            row.addParagraph(p -> p
                    .name(id + "LabelText")
                    .text(party.heading())
                    .textStyle(bold(LABEL_SIZE, ACCENT)));
        });

        column.addSection(id + "Body", block -> {
            block.spacing(0).padding(new DocumentInsets(0, 0, 0, gutter));

            block.addParagraph(p -> p
                    .name(id + "Name")
                    .text(party.name())
                    .textStyle(bold(PARTY_NAME_SIZE, INK))
                    .margin(new DocumentInsets(
                            py(474 - 466) - CAP_INSET * PARTY_NAME_SIZE, 0, 0, 0)));

            block.addSection(id + "Address", lines -> {
                lines.spacing(capPitch(25.3, BODY_SIZE))
                        .margin(new DocumentInsets(
                                capPitch(30, PARTY_NAME_SIZE, BODY_SIZE), 0, 0, 0));
                List<String> address = party.addressLines();
                for (int i = 0; i < address.size(); i++) {
                    String line = address.get(i);
                    int index = i;
                    lines.addParagraph(p -> p
                            .name(id + "Address_" + index)
                            .text(line)
                            .textStyle(style(BODY_SIZE, BODY)));
                }
            });

            if (!party.registrationNumber().isBlank()) {
                block.addParagraph(p -> {
                    p.name(id + "Registration");
                    p.inlineText(party.registrationLabel(), style(BODY_SIZE, BODY));
                    p.inlineText(spaces(26, BODY_SIZE), style(BODY_SIZE, BODY));
                    p.inlineText(party.registrationNumber(), style(BODY_SIZE, BODY));
                    p.margin(new DocumentInsets(capPitch(41, BODY_SIZE), 0, 0, 0));
                });
            }
        });
    }
}
