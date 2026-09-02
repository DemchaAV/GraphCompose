package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DIVIDER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_ADDRESS_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_CELL_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_DISC_GAP;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_DISC_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PARTY_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_TOP_BEARING;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.capOffset;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

/**
 * The addressed parties: who is billed, and where the work went.
 *
 * <p>One method draws both — they are the same component with different data,
 * down to the registration line only one of them usually has. The divider
 * belongs to the split, so it is the second cell's own left edge rather than a
 * rule drawn between the two.</p>
 */
final class PaymentsParties {

    private PaymentsParties() {
    }

    /**
     * Draws the row.
     *
     * @param page   the page flow
     * @param billTo who is billed
     * @param shipTo where the work went, or a party with no name when absent
     */
    static void render(PageFlowBuilder page, InvoiceRecipient billTo, InvoiceRecipient shipTo) {
        List<Party> parties = new ArrayList<>();
        parties.add(new Party(PaymentsIcons.BILL_TO, billTo));
        if (!shipTo.name().isBlank()) {
            parties.add(new Party(PaymentsIcons.SHIP_TO, shipTo));
        }

        page.addRow("PartiesRow", row -> {
            row.spacing(0).verticalAlign(RowVerticalAlign.TOP).evenWeights();
            for (int i = 0; i < parties.size(); i++) {
                Party party = parties.get(i);
                boolean first = i == 0;
                row.addSection("Party" + (i + 1), cell -> {
                    cell.spacing(0);
                    if (first) {
                        cell.padding(new DocumentInsets(0, 0, 0, PARTY_DISC_INSET));
                    } else {
                        cell.borders(DocumentBorders.left(DocumentStroke.of(DIVIDER, RULE_THIN)))
                                .padding(new DocumentInsets(0, 0, 0, PARTY_CELL_INSET));
                    }
                    renderParty(cell, party);
                });
            }
        });
    }

    private static void renderParty(SectionBuilder cell, Party party) {
        InvoiceRecipient who = party.recipient();
        PaymentsWidgets.layeredRow(cell, "PartyHead", row -> {
            row.spacing(PARTY_DISC_GAP)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(DISC_D), DocumentRowColumn.weight(1));
            row.addSection("PartyDisc", disc -> disc
                    .spacing(0)
                    .add(PaymentsWidgets.disc(party.token(), ACCENT_SURFACE)));
            row.addParagraph(p -> p
                    .name("PartyLabel")
                    .text(who.heading())
                    .textStyle(style(PARTY_LABEL_SIZE, INK, DocumentTextDecoration.BOLD))
                    .margin(new DocumentInsets(capOffset(391 - 378, PARTY_LABEL_SIZE), 0, 0, 0)));
        });
        cell.addSection("PartyBody", block -> {
            block.spacing(0).padding(new DocumentInsets(0, 0, 0, PARTY_TEXT_INDENT));
            block.addParagraph(p -> p
                    .name("PartyName")
                    .text(who.name())
                    .textStyle(style(PARTY_NAME_SIZE, INK, DocumentTextDecoration.BOLD)));
            block.addSection("PartyAddress", lines -> {
                lines.spacing(gap(PARTY_ADDRESS_PITCH, BODY_SIZE))
                        .margin(new DocumentInsets(px(451 - 423) - LINE_BOX * PARTY_NAME_SIZE
                                + TEXT_TOP_BEARING * (PARTY_NAME_SIZE - BODY_SIZE), 0, 0, 0));
                PaymentsWidgets.textLines(lines, "PartyAddressLine", who.addressLines(),
                        BODY_SIZE, BODY);
            });
            // The subline is drawn UNDER the address, set off by the design's own
            // gap: this sheet uses it for the registration a billed party prints
            // below where it is, not for a second line of its name.
            if (!who.subline().isBlank()) {
                block.addParagraph(p -> p
                        .name("PartySubline")
                        .text(who.subline())
                        .textStyle(style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT))
                        .margin(new DocumentInsets(gap(552 - 513, BODY_SIZE), 0, 0, 0)));
            }
        });
    }

    /** A party and the mark the preset gives it. */
    private record Party(String token, InvoiceRecipient recipient) {
    }
}
