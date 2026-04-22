package com.demcha.mock;

import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalParty;
import com.demcha.compose.document.templates.data.proposal.ProposalPricingRow;
import com.demcha.compose.document.templates.data.proposal.ProposalSection;
import com.demcha.compose.document.templates.data.proposal.ProposalTimelineItem;

import java.util.List;

public final class ProposalDataFixtures {

    private ProposalDataFixtures() {
    }

    public static ProposalData longProposal() {
        return new ProposalData(
                "Proposal",
                "PROP-2026-014",
                "02 Apr 2026",
                "16 Apr 2026",
                "GraphCompose document platform rollout",
                repeatSentence("This proposal outlines a phased delivery for introducing reusable GraphCompose templates, live examples, and visual validation flows across the client reporting stack.", 5),
                new ProposalParty(
                        "GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK", "EC1A 4GC"),
                        "hello@graphcompose.dev",
                        "+44 20 5555 1000",
                        "graphcompose.dev"),
                new ProposalParty(
                        "Northwind Systems",
                        List.of("Product Engineering", "410 Market Avenue", "Manchester, UK"),
                        "platform@northwind.example",
                        "+44 161 555 2200",
                        "northwind.example"),
                List.of(
                        new ProposalSection("Scope", List.of(
                                repeatSentence("We will refine the public document API, add invoice and proposal templates, and keep the runtime artifact clean by moving developer-only tooling out of production scope.", 4),
                                repeatSentence("The implementation will prioritize readable composition code, stable visual defaults, and examples that can be shared directly with downstream application teams.", 4))),
                        new ProposalSection("Deliverables", List.of(
                                repeatSentence("The first delivery package includes reusable built-in templates, render tests for clean and guide-line variants, and a standalone examples module that generates PDFs to disk.", 4),
                                repeatSentence("A second output is a set of onboarding-friendly example entry points that demonstrate file rendering for CV, cover letter, invoice, and proposal scenarios.", 4))),
                        new ProposalSection("Implementation approach", List.of(
                                repeatSentence("We will use GraphCompose containers, semantic sections, and table primitives rather than coordinate-driven drawing so that the documents stay resilient to longer content and easier to evolve.", 4),
                                repeatSentence("Testing will emphasize real PDF generation, PDFBox validation, and long-form proposal content that intentionally exercises page flow beyond a single page.", 4))),
                        new ProposalSection("Collaboration model", List.of(
                                repeatSentence("Weekly checkpoints will focus on visual deltas, example ergonomics, and whether the generated outputs are understandable for both library consumers and internal maintainers.", 4),
                                repeatSentence("Each milestone includes a review of template polish, code readability, and whether the generated examples are strong enough to support README and demo usage.", 4)))),
                List.of(
                        new ProposalTimelineItem("Week 1", "5 days", "API alignment, DTO modeling, invoice template implementation, and review-ready render tests."),
                        new ProposalTimelineItem("Week 2", "5 days", "Proposal template implementation, long-content pagination verification, and examples module setup."),
                        new ProposalTimelineItem("Week 3", "3 days", "README refresh, final PDF review, and delivery handoff package.")),
                List.of(
                        new ProposalPricingRow("Foundation", "Public template APIs, DTOs, and cleanup of devtool production scope.", "GBP 3,200", false),
                        new ProposalPricingRow("Template delivery", "Invoice and proposal layouts with render tests and polished defaults.", "GBP 4,450", false),
                        new ProposalPricingRow("Examples package", "Runnable examples module, sample data, and usage documentation.", "GBP 1,850", false),
                        new ProposalPricingRow("Total investment", "Fixed-price delivery for the agreed scope.", "GBP 9,500", true)),
                List.of(
                        "Proposal pricing is valid until the stated expiration date.",
                        "Any additional template families beyond the agreed four examples are scoped separately.",
                        "Client feedback rounds are consolidated into the scheduled milestone reviews.",
                        "Final source changes and generated examples are delivered through the repository workflow."),
                "Prepared for platform engineering leadership to evaluate production adoption of GraphCompose."
        );
    }

    private static String repeatSentence(String sentence, int count) {
        return String.join(" ", java.util.Collections.nCopies(count, sentence));
    }
}
