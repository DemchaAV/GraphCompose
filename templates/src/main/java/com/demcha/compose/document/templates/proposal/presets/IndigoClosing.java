package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;
import com.demcha.compose.document.templates.data.proposal.ProposalFooter;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.rule;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.CONTENT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_ADDRESS_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_BODY_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_CONTACT_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_MARK_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_MARK_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_NAME_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_NAME_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_RULE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FOOTER_TEXT_COL_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.bold;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.boxBottomPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.plain;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.toPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.topBearing;

/**
 * What closes the sheet: a rule the width of the ink, then the issuer's mark,
 * legal name, registered address and the channels a reader can follow.
 *
 * <h2>What this port leaves out</h2>
 *
 * <p>The design closes with its own logotype. That is a brand asset and the
 * templates artifact carries none, so the mark's place is kept and set with the
 * monogram the document names, at the size the design's glyph reads at beside
 * the name. A document naming no monogram leaves the place empty, and the name
 * beside it does not move.</p>
 */
final class IndigoClosing {

    /**
     * The separator between two channels, spaced away from both. It is a run of
     * its own so that only the channel either side of it is clickable, and so
     * that it keeps the lighter colour the design gives it.
     */
    private static final String SEPARATOR = "   |   ";

    private IndigoClosing() {
    }

    /** The full-width rule the foot opens with. */
    static void renderFooterRule(PageFlowBuilder page, IndigoFlow flow) {
        double top = flow.boxAt(FOOTER_RULE_AT, 1.5);
        page.addLine(line -> rule(line, "FooterRule", CONTENT_W, RULE_STRONG)
                .margin(new DocumentInsets(top, 0, 0, 0)));
    }

    /**
     * The foot itself.
     *
     * @param page   the page flow
     * @param brand  the issuer's mark
     * @param footer the issuer's identity as the foot states it
     * @param flow   the sheet's cursor
     */
    static void renderFooter(PageFlowBuilder page, ProposalBrand brand, ProposalFooter footer,
                             IndigoFlow flow) {
        if (!footer.isPresent() && brand.monogram().isBlank()) {
            return;
        }
        // The row opens on the name's own box, not on the mark's: the name's
        // box starts above its cap and above the mark, and a row opened on the
        // mark would owe the name a negative margin.
        double rowTopPx = FOOTER_NAME_CAP - toPx(topBearing(FOOTER_NAME_SIZE, true));
        double top = flow.boxAt(rowTopPx,
                boxBottomPx(FOOTER_CONTACT_CAP, FOOTER_BODY_SIZE, false) - rowTopPx);
        page.addRow("Footer", row -> {
            row.spacing(0)
                    .margin(new DocumentInsets(top, 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(FOOTER_TEXT_COL_W),
                            DocumentRowColumn.weight(1));
            row.addSection("FooterMark", mark -> {
                mark.spacing(0);
                if (brand.monogram().isBlank()) {
                    return;
                }
                mark.addParagraph(p -> p
                        .name("FooterMonogram")
                        .text(brand.monogram())
                        .textStyle(bold(FOOTER_MARK_SIZE, INK))
                        .margin(new DocumentInsets(new IndigoFlow(rowTopPx)
                                .boxAt(FOOTER_MARK_AT, toPx(FOOTER_MARK_SIZE)), 0, 0, 0)));
            });
            row.addSection("FooterIdentity", identity -> {
                identity.spacing(0);
                IndigoFlow cell = new IndigoFlow(rowTopPx);
                renderName(identity, footer, cell);
                renderAddress(identity, footer, cell);
                renderChannels(identity, footer, cell);
            });
        });
    }

    private static void renderName(SectionBuilder identity, ProposalFooter footer,
                                   IndigoFlow cell) {
        if (footer.name().isBlank()) {
            return;
        }
        identity.addParagraph(p -> p
                .name("FooterName")
                .text(footer.name())
                .textStyle(bold(FOOTER_NAME_SIZE, INK))
                .margin(new DocumentInsets(
                        cell.capAt(FOOTER_NAME_CAP, FOOTER_NAME_SIZE, true), 0, 0, 0)));
    }

    /** The registered address, which the design runs as one line however it is authored. */
    private static void renderAddress(SectionBuilder identity, ProposalFooter footer,
                                      IndigoFlow cell) {
        if (footer.addressLines().isEmpty()) {
            return;
        }
        identity.addParagraph(p -> p
                .name("FooterAddress")
                .text(String.join(", ", footer.addressLines()))
                .textStyle(plain(FOOTER_BODY_SIZE, BODY))
                .margin(new DocumentInsets(
                        cell.capAt(FOOTER_ADDRESS_CAP, FOOTER_BODY_SIZE, false), 0, 0, 0)));
    }

    /**
     * The channels, each linked, divided by the design's own separator.
     *
     * <p>A confidentiality line closes the same line rather than opening one of
     * its own: the design's last line already ends against the bottom margin, so
     * a fourth line in the foot would carry the whole block onto a second page.
     * It is set as prose and not as a channel — a notice is a sentence, and a
     * sentence is not somewhere a reader can be sent.</p>
     */
    private static void renderChannels(SectionBuilder identity, ProposalFooter footer,
                                       IndigoFlow cell) {
        List<String> contacts = footer.contacts();
        String notice = footer.confidentiality();
        if (contacts.isEmpty() && notice.isBlank()) {
            return;
        }
        double top = cell.capAt(FOOTER_CONTACT_CAP, FOOTER_BODY_SIZE, false);
        identity.addParagraph(p -> {
            p.name("FooterChannels").margin(new DocumentInsets(top, 0, 0, 0));
            DocumentTextStyle body = plain(FOOTER_BODY_SIZE, BODY);
            DocumentTextStyle divider = plain(FOOTER_BODY_SIZE, RULE_STRONG);
            for (int i = 0; i < contacts.size(); i++) {
                if (i > 0) {
                    p.inlineText(SEPARATOR, divider);
                }
                appendChannel(p, contacts.get(i), body);
            }
            if (!notice.isBlank()) {
                if (!contacts.isEmpty()) {
                    p.inlineText(SEPARATOR, divider);
                }
                p.inlineText(notice, body);
            }
        });
    }

    /** A channel, made followable when its printed shape says what it is. */
    private static void appendChannel(ParagraphBuilder p, String contact,
                                      DocumentTextStyle body) {
        DocumentLinkOptions link = ContactUri.channelLink(contact);
        if (link == null) {
            p.inlineText(contact, body);
        } else {
            p.inlineText(contact, body, link);
        }
    }
}
