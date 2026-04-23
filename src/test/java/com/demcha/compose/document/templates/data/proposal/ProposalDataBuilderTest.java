package com.demcha.compose.document.templates.data.proposal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalDataBuilderTest {

    @Test
    void builderShouldCreateProposalWithOrderedSectionsTimelinePricingAndAcceptanceTerms() {
        ProposalData proposal = ProposalData.builder()
                .proposalNumber("PROP-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("16 Apr 2026")
                .projectTitle("GraphCompose rollout for internal document operations")
                .executiveSummary("A practical adoption path for reusable GraphCompose templates.")
                .sender(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK")
                        .email("hello@graphcompose.dev")
                        .website("graphcompose.dev"))
                .recipient(party -> party
                        .name("Northwind Systems")
                        .addressLines("Product Engineering", "410 Market Avenue", "Manchester, UK")
                        .email("platform@northwind.example"))
                .section("Scope",
                        "Introduce built-in invoice and proposal templates.",
                        "Keep the production artifact clean.")
                .section(section -> section
                        .title("Deliverables")
                        .addParagraph("Public DTOs and template interfaces.")
                        .addParagraph("Render tests and runnable examples."))
                .timelineItem("Week 1", "5 days", "Foundation and review loop")
                .timelineItem("Week 2", "5 days", "Template delivery and render QA")
                .pricingRow("Foundation", "Template APIs and DTO modeling", "GBP 3,200")
                .emphasizedPricingRow("Total investment", "Fixed-price project delivery", "GBP 9,500")
                .acceptanceTerm("Proposal pricing is valid until the stated expiration date.")
                .footerNote("Prepared to demonstrate the business-document side of GraphCompose.")
                .build();

        assertThat(proposal.title()).isEqualTo("Proposal");
        assertThat(proposal.sender().name()).isEqualTo("GraphCompose Studio");
        assertThat(proposal.recipient().name()).isEqualTo("Northwind Systems");
        assertThat(proposal.sections()).hasSize(2);
        assertThat(proposal.sections().get(0).title()).isEqualTo("Scope");
        assertThat(proposal.timeline()).hasSize(2);
        assertThat(proposal.pricingRows()).hasSize(2);
        assertThat(proposal.pricingRows().get(1).emphasized()).isTrue();
        assertThat(proposal.acceptanceTerms()).containsExactly("Proposal pricing is valid until the stated expiration date.");
        assertThat(proposal.footerNote()).isEqualTo("Prepared to demonstrate the business-document side of GraphCompose.");
    }
}
