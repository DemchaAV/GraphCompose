package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.proposal.spec.ProposalSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 proposal pipeline through
 * {@link ModernProposal}.
 */
class ModernProposalSmokeTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static ProposalSpec sampleSpec() {
        return ProposalSpec.builder()
                .title("Platform Refresh Sprint")
                .fromParty(new ProposalSpec.Party("GraphCompose Studio",
                        "billing@graphcompose.dev", "+44 20 5555 1000"))
                .toParty(new ProposalSpec.Party("Northwind Systems",
                        "platform@northwind.example", "+44 161 555 2200"))
                .section(new ProposalSpec.Section("Overview",
                        "We propose a focused refresh of your **internal "
                                + "platform** documents — invoice, proposal, and CV — "
                                + "using the GraphCompose v1.6 architecture."))
                .section(new ProposalSpec.Section("Scope",
                        "Discovery, template architecture, render QA, and "
                                + "developer enablement, delivered across a "
                                + "4-week sprint."))
                .pricingRow(ProposalSpec.PricingRow.of("Discovery workshop", "GBP 1,450"))
                .pricingRow(ProposalSpec.PricingRow.of("Template architecture", "GBP 1,960"))
                .pricingRow(ProposalSpec.PricingRow.of("Render QA", "GBP 960"))
                .pricingRow(ProposalSpec.PricingRow.headline("Total", "GBP 4,370"))
                .footerNote("*Thank you for considering GraphCompose Studio.*")
                .build();
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<ProposalSpec> template = ModernProposal.create(THEME);
        assertThat(template.id()).isEqualTo(ModernProposal.ID);
        assertThat(template.displayName()).isEqualTo(ModernProposal.DISPLAY_NAME);
    }

    @Test
    void composeAddsRootDocumentNode() throws Exception {
        DocumentTemplate<ProposalSpec> template = ModernProposal.create(THEME);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            template.compose(session, sampleSpec());
            assertThat(session.roots()).isNotEmpty();
        }
    }
}
