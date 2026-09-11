package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Shared roster and canonical sample proposal for the layered proposal
 * presets.
 *
 * <p>Both preset gates read from here so they always describe the same
 * render: {@code ProposalV2VisualParityTest} compares the rasterised
 * pages per-pixel, {@code ProposalPresetLayoutSnapshotTest} compares
 * the post-layout node tree. Adding a preset to {@link #presets()}
 * enrols it in both.</p>
 */
final class ProposalPresetFixtures {

    private ProposalPresetFixtures() {
    }

    /**
     * Every layered proposal preset, as {@code (slug,
     * recommendedMargin, factory)} triples.
     */
    static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("modern_proposal",
                        ModernProposal.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<ProposalDocumentSpec>>) ModernProposal::create));
    }

    /**
     * Canonical sample proposal — exercises the hero, executive summary,
     * both parties, body sections, the timeline + pricing tables (with an
     * emphasized total), the acceptance terms, and the footer. Kept inline
     * so the tests depend only on main + main-test code.
     */
    static ProposalDocumentSpec canonicalProposal() {
        return ProposalDocumentSpec.from(ProposalData.builder()
                .title("Proposal")
                .proposalNumber("GC-P-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("30 Apr 2026")
                .projectTitle("Document platform consolidation")
                .executiveSummary("A phased engagement to retire per-team PDF scripts in "
                        + "favour of one canonical, snapshot-tested document engine that "
                        + "serves billing, hiring, and reporting flows.")
                .sender(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("hello@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .website("graphcompose.dev"))
                .recipient(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Procurement Team", "410 Market Avenue", "Manchester, UK")
                        .email("procurement@northwind.example")
                        .phone("+44 161 555 2200"))
                .section("Scope of work",
                        "Discovery of the current document estate, a reference architecture, "
                                + "and a production rollout of the canonical engine.",
                        "Migration of the three highest-volume templates with visual parity gates.")
                .section("Approach",
                        "Iterative delivery in two-week increments with weekly checkpoints "
                                + "and a shared visual-regression dashboard.")
                .timelineItem("Discovery", "2 weeks", "Stakeholder interviews + estate audit")
                .timelineItem("Build", "6 weeks", "Engine integration + template migration")
                .timelineItem("Rollout", "2 weeks", "Cutover, training, and handover")
                .pricingRow("Discovery", "Workshops + audit", "GBP 6,000")
                .pricingRow("Build", "Engine + 3 migrations", "GBP 24,000")
                .pricingRow("Rollout", "Cutover + enablement", "GBP 6,000")
                .emphasizedPricingRow("Total", "Fixed-fee engagement", "GBP 36,000")
                .acceptanceTerm("50% on signature, 50% on final delivery.")
                .acceptanceTerm("Fixed-fee; scope changes handled via a written change note.")
                .acceptanceTerm("Valid for 30 days from the prepared date.")
                .footerNote("Thank you for considering GraphCompose for your document platform.")
                .build());
    }
}
