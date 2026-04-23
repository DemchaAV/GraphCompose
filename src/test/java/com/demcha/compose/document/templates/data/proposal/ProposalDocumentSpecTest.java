package com.demcha.compose.document.templates.data.proposal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalDocumentSpecTest {

    @Test
    void builderShouldCreateDocumentLevelProposalSpec() {
        ProposalDocumentSpec spec = ProposalDocumentSpec.builder()
                .proposalNumber("PROP-2026-014")
                .projectTitle("GraphCompose rollout")
                .executiveSummary("Reusable document templates for business workflows.")
                .sender(party -> party.name("GraphCompose Studio"))
                .recipient(party -> party.name("Northwind Systems"))
                .section("Scope", "Introduce invoice and proposal templates.")
                .timelineItem("Week 1", "5 days", "Foundation and review loop")
                .pricingRow("Foundation", "Template APIs", "GBP 3,200")
                .emphasizedPricingRow("Total", "Fixed-price delivery", "GBP 3,200")
                .acceptanceTerm("Pricing is valid until the stated expiration date.")
                .build();

        assertThat(spec.proposal().proposalNumber()).isEqualTo("PROP-2026-014");
        assertThat(spec.proposal().projectTitle()).isEqualTo("GraphCompose rollout");
        assertThat(spec.proposal().sender().name()).isEqualTo("GraphCompose Studio");
        assertThat(spec.proposal().recipient().name()).isEqualTo("Northwind Systems");
        assertThat(spec.proposal().sections()).singleElement()
                .extracting(ProposalSection::title)
                .isEqualTo("Scope");
        assertThat(spec.proposal().timeline()).singleElement()
                .extracting(ProposalTimelineItem::phase)
                .isEqualTo("Week 1");
        assertThat(spec.proposal().pricingRows()).hasSize(2);
        assertThat(spec.proposal().pricingRows().get(1).emphasized()).isTrue();
        assertThat(spec.proposal().acceptanceTerms())
                .containsExactly("Pricing is valid until the stated expiration date.");
    }

    @Test
    void fromShouldWrapExistingProposalDataWithoutChangingIt() {
        ProposalData proposal = ProposalData.builder()
                .proposalNumber("PROP-1")
                .build();

        assertThat(ProposalDocumentSpec.from(proposal).proposal()).isSameAs(proposal);
    }
}
