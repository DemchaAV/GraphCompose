package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_IDENTITY_TO_PARTIES;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PARTY_BODY_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PARTY_LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PARTY_SPLIT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.cycle;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionWidgets.headingPlaque;

/**
 * The addressed parties, each under a heading underlined in the next colour of
 * the cycle.
 */
final class SubscriptionParties {

    private SubscriptionParties() {
    }

    /**
     * The parties row.
     *
     * <p>The split is the design's measured one when both parties are set. With
     * one party there is nothing to split against, so the block takes the row.</p>
     *
     * @param page   the page flow
     * @param billTo the billed party
     * @param shipTo the shipped-to party, or {@code null} when the invoice ships
     *               nowhere
     */
    static void render(PageFlowBuilder page, InvoiceRecipient billTo, InvoiceRecipient shipTo) {
        List<InvoiceRecipient> parties = new ArrayList<>();
        parties.add(billTo);
        if (shipTo != null) {
            parties.add(shipTo);
        }
        page.addRow("Parties", row -> {
            row.padding(CONTENT_PAD);
            row.margin(new DocumentInsets(GAP_IDENTITY_TO_PARTIES, 0, 0, 0));
            row.spacing(0);
            if (parties.size() == 2) {
                row.weights(PARTY_SPLIT, 1 - PARTY_SPLIT);
            } else {
                row.evenWeights();
            }
            for (int i = 0; i < parties.size(); i++) {
                InvoiceRecipient party = parties.get(i);
                DocumentColor accent = cycle(i);
                int index = i;
                row.addSection("Party_" + index, cell -> renderParty(cell, index, party, accent));
            }
        });
    }

    private static void renderParty(SectionBuilder cell, int index, InvoiceRecipient party,
                                    DocumentColor accent) {
        cell.spacing(0);
        headingPlaque(cell, "PartyHeading_" + index, party.heading(), HEADING_SIZE, accent);
        cell.addSection("PartyAddress_" + index, block -> {
            block.margin(new DocumentInsets(PARTY_BODY_GAP, 0, 0, 0));
            block.spacing(PARTY_LINE_PITCH - 1.2 * BODY_SIZE);
            // The attention line sits above the address, which is what the
            // contract says it is and what this design draws.
            if (!party.name().isBlank()) {
                block.addParagraph(p -> p
                        .name("PartyName_" + index)
                        .text(party.name())
                        .textStyle(plain(BODY_SIZE, INK)));
            }
            if (!party.subline().isBlank()) {
                block.addParagraph(p -> p
                        .name("PartySubline_" + index)
                        .text(party.subline())
                        .textStyle(plain(BODY_SIZE, INK)));
            }
            List<String> address = party.addressLines();
            for (int i = 0; i < address.size(); i++) {
                String line = address.get(i);
                int lineIndex = i;
                block.addParagraph(p -> p
                        .name("PartyAddress_" + index + "_" + lineIndex)
                        .text(line)
                        .textStyle(plain(BODY_SIZE, INK)));
            }
            if (!party.registrationNumber().isBlank()) {
                block.addParagraph(p -> p
                        .name("PartyRegistration_" + index)
                        .text(party.registrationLabel() + " " + party.registrationNumber())
                        .textStyle(plain(BODY_SIZE, INK)));
            }
        });
    }
}
