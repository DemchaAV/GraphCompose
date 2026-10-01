package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.List;

/** The proposal presets of the DOCX fidelity corpus. */
public final class ProposalDocxCorpus {

    private ProposalDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        return List.of(
                new DocxCorpusDocument("proposal", "indigo", -1,
                        s -> IndigoProposal.create().compose(s, IndigoProposalFixtures.canonicalProposal())),
                new DocxCorpusDocument("proposal", "editorial", -1,
                        s -> EditorialProposal.create().compose(s, EditorialProposalFixtures.canonicalProposal())),
                new DocxCorpusDocument("proposal", "northline", -1,
                        s -> NorthlineProposal.create().compose(s, NorthlineProposalFixtures.canonicalProposal())),
                new DocxCorpusDocument("proposal", "modern", 28,
                        s -> ModernProposal.create().compose(s, modern())));
    }

    // The spec ModernProposalSmokeTest renders.
    private static ProposalDocumentSpec modern() {
        return ProposalDocumentSpec.from(ProposalData.builder()
                .title("Proposal")
                .proposalNumber("GC-P-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("30 Apr 2026")
                .projectTitle("Document platform consolidation")
                .executiveSummary("A phased engagement to retire per-team PDF scripts "
                        + "in favour of one canonical document engine.")
                .sender(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK")
                        .email("hello@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .website("graphcompose.dev"))
                .recipient(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Procurement", "410 Market Avenue")
                        .email("procurement@northwind.example"))
                .section("Scope", "Discovery, architecture, and a reference rollout.")
                .section("Approach", "Iterative delivery with weekly checkpoints.")
                .timelineItem("Discovery", "2 weeks", "Stakeholder interviews + audit")
                .timelineItem("Build", "6 weeks", "Engine + template migration")
                .pricingRow("Discovery", "Workshops + audit", "GBP 6,000")
                .pricingRow("Build", "Engine + templates", "GBP 24,000")
                .emphasizedPricingRow("Total", "", "GBP 30,000")
                .acceptanceTerm("50% on signature, 50% on delivery.")
                .acceptanceTerm("Valid for 30 days from the prepared date.")
                .footerNote("Thank you for considering GraphCompose.")
                .build());
    }
}
