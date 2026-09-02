package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.templates.data.proposal.ProposalAttention;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;
import com.demcha.compose.document.templates.data.proposal.ProposalFooter;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;
import com.demcha.compose.document.templates.data.proposal.ProposalInvestment;
import com.demcha.compose.document.templates.data.proposal.ProposalMetaLine;
import com.demcha.compose.document.templates.data.proposal.ProposalRecipient;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalTermsBlock;
import com.demcha.compose.document.templates.data.proposal.ProposalTitleLines;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;

import java.util.List;

/**
 * Shared fixture data for the Indigo proposal gates — the SAME spec feeds the
 * smoke, layout-snapshot and pixel gates, so a geometry shift the pixel budget
 * absorbs still trips the exact snapshot, and vice versa.
 *
 * <p>The canonical fixture is the preset's reference content: an addressed head
 * with a person to reply to, a four-line headline over a three-line standfirst,
 * four header tiles, the tinted band with a four-line paragraph and four feature
 * tiles, five numbered steps beside four priced rows and a total, three notes,
 * and a foot with an address and three channels.</p>
 *
 * <p><strong>The line counts are part of the fixture.</strong> The preset places
 * every block at a stated position, and the blocks that wrap — the standfirst,
 * the band's paragraph, the plan's opening and the notes list — declare how many
 * lines they set in, because the engine cannot be asked at compose time. Editing
 * one of them to a different line count moves everything below it, which both
 * gates will report. Change the text and the map together, or keep the
 * length.</p>
 */
final class IndigoProposalFixtures {

    private IndigoProposalFixtures() {
    }

    /** One page: a financial-infrastructure partnership offered to one customer. */
    static StructuredProposalDocumentSpec canonicalProposal() {
        return StructuredProposalDocumentSpec.from(StructuredProposalData.builder()
                .brand(new ProposalBrand("K", "Kestrel", "", "PROPOSAL",
                        "kestrel.example", "Kestrel Payments Ltd"))
                .recipient(new ProposalRecipient("PREPARED FOR", "Bright Future Ltd.",
                        List.of("45 King Street", "Manchester, M2 4WU", "United Kingdom")))
                .attention(new ProposalAttention("ATTN", "Priya Raman", "Founder",
                        "priya@brightfuture.example", "+44 7700 900123"))
                .title(ProposalTitleLines.of("", List.of(
                        "Proposal for",
                        "Financial Infrastructure",
                        "& Embedded Finance",
                        "Partnership"),
                        List.of("Empowering Bright Future Ltd. with modern financial"
                                + " infrastructure, global capabilities, and a seamless"
                                + " experience for your customers.")))
                .meta(new ProposalMetaLine(List.of(
                        new ProposalMetaLine.Entry("date", "DATE", "27 May 2026"),
                        new ProposalMetaLine.Entry("identifier", "PROPOSAL ID",
                                "KES-2026-0527-01"),
                        new ProposalMetaLine.Entry("valid", "VALID UNTIL", "26 June 2026"),
                        new ProposalMetaLine.Entry("prepared-by", "PREPARED BY",
                                "Kestrel Business Team"))))
                .glance(new ProposalGlance("ABOUT KESTREL BUSINESS", List.of(
                        new ProposalGlance.Fact("global", "Global", "Scale", ""),
                        new ProposalGlance.Fact("infrastructure", "Modern",
                                "Infrastructure", ""),
                        new ProposalGlance.Fact("secure", "Secure &", "Compliant", ""),
                        new ProposalGlance.Fact("apis", "Developer", "First APIs", "")),
                        "Kestrel Business provides modern financial infrastructure for"
                                + " forward-thinking companies. From multi-currency accounts"
                                + " and payments to cards, FX, and embedded finance APIs —"
                                + " all in one platform."))
                .scope(new ProposalScope("PROJECT OVERVIEW", "", List.of(
                        new ProposalScope.Item("01", "Multi-currency business accounts",
                                "Hold, send, receive in 30+ currencies"),
                        new ProposalScope.Item("02", "Corporate cards & expenses",
                                "Issue physical & virtual cards with smart controls"),
                        new ProposalScope.Item("03", "Payments & collections",
                                "Local & global payments, direct debits, card acquiring"),
                        new ProposalScope.Item("04", "Embedded finance APIs",
                                "Build financial products directly into your platform"),
                        new ProposalScope.Item("05", "Compliance & risk",
                                "KYC, AML, fraud monitoring and regulatory coverage")),
                        "This proposal outlines a tailored financial infrastructure solution"
                                + " to support Bright Future Ltd. in enabling fast, secure and"
                                + " scalable financial services for your business and your"
                                + " customers."))
                .investment(new ProposalInvestment("INVESTMENT SUMMARY", "", "", "", List.of(
                        new ProposalInvestment.Row("Implementation & Onboarding",
                                "£2,500", null),
                        new ProposalInvestment.Row("Platform Access & APIs",
                                "£3,000", null),
                        new ProposalInvestment.Row("Transaction Fees (est.)", "Variable", null),
                        new ProposalInvestment.Row("Support & Account Management",
                                "Included", null)),
                        "Estimated First Year Investment", "£5,500"))
                .terms(new ProposalTermsBlock("NOTES", "", List.of(
                        "Pricing excludes applicable taxes.",
                        "Transaction fees are based on usage and volume.",
                        "This proposal is valid for 30 days from the date above.")))
                .footer(new ProposalFooter("Kestrel Payments Ltd",
                        List.of("7 Harbour Circus, Canary Wharf, London E14 4HD,"
                                + " United Kingdom"),
                        List.of("kestrel.example/business", "business@kestrel.example",
                                "+44 20 3322 8352"),
                        ""))
                .build());
    }
}
