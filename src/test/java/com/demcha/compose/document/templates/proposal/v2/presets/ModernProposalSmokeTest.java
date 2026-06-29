package com.demcha.compose.document.templates.proposal.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the layered {@code proposal.v2} pipeline through
 * {@link ModernProposal} — proves the preset renders a
 * {@link ProposalDocumentSpec} end-to-end on a {@link BrandTheme}, via
 * both factory variants, with any theme, and on an empty proposal.
 */
class ModernProposalSmokeTest {

    private static ProposalDocumentSpec sampleSpec() {
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

    /** A proposal with no sections, timeline, pricing, terms, or footer — the empty paths. */
    private static ProposalDocumentSpec minimalSpec() {
        return ProposalDocumentSpec.from(ProposalData.builder()
                .proposalNumber("GC-P-2026-015")
                .sender(from -> from.name("GraphCompose Studio"))
                .recipient(to -> to.name("Northwind Systems"))
                .build());
    }

    private static void render(DocumentTemplate<ProposalDocumentSpec> template,
                               ProposalDocumentSpec spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            template.compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<ProposalDocumentSpec> template = ModernProposal.create();
        assertThat(template.id()).isEqualTo(ModernProposal.ID);
        assertThat(template.displayName()).isEqualTo(ModernProposal.DISPLAY_NAME);
    }

    @Test
    void defaultFactoryRendersWithProposalTheme() throws Exception {
        // create() wires BrandTheme.proposalModern() — the variant the example uses.
        render(ModernProposal.create(), sampleSpec());
    }

    @Test
    void rendersWithExplicitTheme() throws Exception {
        render(ModernProposal.create(BrandTheme.proposalModern()), sampleSpec());
    }

    @Test
    void readsAnyTheme() throws Exception {
        // Renders under a non-proposal theme without crashing: the hero, headings,
        // labels, and footer follow the theme; the table body cells inherit the DSL
        // default (as in the cinematic builtin).
        render(ModernProposal.create(BrandTheme.boxedClassic()), sampleSpec());
    }

    @Test
    void rendersEmptyProposal() throws Exception {
        // Exercises the empty-collection + skipped-table/footer paths.
        render(ModernProposal.create(), minimalSpec());
    }
}
