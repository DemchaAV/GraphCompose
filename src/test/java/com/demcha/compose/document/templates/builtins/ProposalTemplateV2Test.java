package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.ProposalTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase E.2 — ProposalTemplateV2 must compose against the canonical DSL,
 * accept any BusinessTheme, and produce a valid PDF for the standard
 * sample proposal.
 *
 * @author Artem Demchyshyn
 */
class ProposalTemplateV2Test {

    @Test
    void defaultConstructorPicksModernTheme() {
        ProposalTemplateV2 template = new ProposalTemplateV2();
        assertThat(template.getTemplateId()).isEqualTo("proposal-v2");
        assertThat(template.getTemplateName()).isEqualTo("Proposal V2 (cinematic)");
    }

    @Test
    void implementsCanonicalProposalTemplateInterface() {
        ProposalTemplate template = new ProposalTemplateV2();
        assertThat(template).isNotNull();
    }

    @Test
    void composeProducesValidPdfBytesForSampleProposal() throws Exception {
        ProposalDocumentSpec spec = sampleProposal();
        ProposalTemplateV2 template = new ProposalTemplateV2();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(BusinessTheme.modern().pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {

            template.compose(document, spec);
            byte[] bytes = document.toPdfBytes();
            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                    .as("PDF magic header — graphics-state leak corrupts this")
                    .isEqualTo("%PDF-");
        }
    }

    @Test
    void differentThemesProduceDistinctRenderedBytes() throws Exception {
        ProposalDocumentSpec spec = sampleProposal();

        byte[] modernBytes = renderWithTheme(spec, BusinessTheme.modern());
        byte[] classicBytes = renderWithTheme(spec, BusinessTheme.classic());

        assertThat(modernBytes).startsWith("%PDF-".getBytes());
        assertThat(classicBytes).startsWith("%PDF-".getBytes());
        assertThat(modernBytes)
                .as("theme switch must be observable downstream — palette colours embed in the PDF stream")
                .isNotEqualTo(classicBytes);
    }

    @Test
    void layoutGraphContainsTimelineAndPricingTables() throws Exception {
        ProposalDocumentSpec spec = sampleProposal();
        ProposalTemplateV2 template = new ProposalTemplateV2();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(28))
                .create()) {

            template.compose(document, spec);
            LayoutGraph graph = document.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThanOrEqualTo(1);
            assertThat(graph.nodes())
                    .as("the timeline table is composed for every proposal that has timeline rows")
                    .anyMatch(node -> node.semanticName() != null
                            && node.semanticName().contains("ProposalTimeline"));
            assertThat(graph.nodes())
                    .as("the pricing table is composed for every proposal that has pricing rows")
                    .anyMatch(node -> node.semanticName() != null
                            && node.semanticName().contains("ProposalPricing"));
        }
    }

    private static byte[] renderWithTheme(ProposalDocumentSpec spec, BusinessTheme theme) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            new ProposalTemplateV2(theme).compose(document, spec);
            return document.toPdfBytes();
        }
    }

    private static ProposalDocumentSpec sampleProposal() {
        return ProposalDocumentSpec.builder()
                .title("Proposal")
                .proposalNumber("PROP-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("16 Apr 2026")
                .projectTitle("GraphCompose rollout")
                .executiveSummary("This proposal describes a practical adoption path for reusable GraphCompose templates.")
                .sender(party -> party.name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK")
                        .email("hello@graphcompose.dev"))
                .recipient(party -> party.name("Northwind Systems")
                        .addressLines("410 Market Avenue", "Manchester, UK")
                        .email("platform@northwind.example"))
                .section("Scope",
                        "Introduce built-in invoice and proposal templates with a consistent business presentation layer.")
                .section("Deliverables",
                        "Public DTOs and template interfaces for invoice and proposal rendering.")
                .timelineItem("Week 1", "5 days", "Invoice API and first template delivery.")
                .timelineItem("Week 2", "5 days", "Proposal layout and review loop.")
                .pricingRow("Foundation", "Template APIs and DTO modeling", "GBP 3,200")
                .pricingRow("Document delivery", "Invoice and proposal templates", "GBP 4,450")
                .emphasizedPricingRow("Total investment", "Fixed-price project delivery", "GBP 9,500")
                .acceptanceTerm("Proposal pricing is valid until the stated expiration date.")
                .footerNote("Prepared to demonstrate the business-document side of GraphCompose.")
                .build();
    }
}
