package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV2;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the same {@link ProposalDocumentSpec} via
 * {@code ProposalTemplateV2} with each of the three built-in
 * {@link BusinessTheme} themes. The output lives under
 * {@code target/visual-tests/proposal-template-v2/} so a reviewer can
 * flip through the three PDFs side-by-side and see the theme switch
 * visually.
 *
 * @author Artem Demchyshyn
 */
class ProposalTemplateV2DemoTest {

    @Test
    void modernThemeRendersToValidPdf() throws Exception {
        renderTheme("proposal-modern", BusinessTheme.modern());
    }

    @Test
    void classicThemeRendersToValidPdf() throws Exception {
        renderTheme("proposal-classic", BusinessTheme.classic());
    }

    @Test
    void executiveThemeRendersToValidPdf() throws Exception {
        renderTheme("proposal-executive", BusinessTheme.executive());
    }

    private static void renderTheme(String stem, BusinessTheme theme) throws Exception {
        Path output = VisualTestOutputs.preparePdf(stem, "proposal-template-v2");
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            new ProposalTemplateV2(theme).compose(document, sampleProposal());
            Files.write(output, document.toPdfBytes());
        }
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }

    private static ProposalDocumentSpec sampleProposal() {
        return ProposalDocumentSpec.builder()
                .title("Proposal")
                .proposalNumber("PROP-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("16 Apr 2026")
                .projectTitle("GraphCompose rollout for internal document operations")
                .executiveSummary("This proposal describes a practical adoption path for reusable GraphCompose templates, render tests, and runnable examples across billing, hiring, and client-facing delivery workflows.")
                .sender(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("hello@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .website("graphcompose.dev"))
                .recipient(party -> party
                        .name("Northwind Systems")
                        .addressLines("Product Engineering", "410 Market Avenue", "Manchester, UK")
                        .email("platform@northwind.example")
                        .phone("+44 161 555 2200")
                        .website("northwind.example"))
                .section("Scope",
                        "Introduce built-in invoice and proposal templates with a consistent business presentation layer.",
                        "Keep the production artifact clean by moving development-only preview code out of the published runtime scope.")
                .section("Deliverables",
                        "Public DTOs and template interfaces for invoice and proposal rendering.",
                        "Render tests and a standalone examples module that generates PDF files on demand.")
                .timelineItem("Week 1", "5 days", "Invoice API and first template delivery.")
                .timelineItem("Week 2", "5 days", "Proposal layout, review loop, and render tests.")
                .timelineItem("Week 3", "3 days", "Examples module and README handoff.")
                .pricingRow("Foundation", "Template APIs and DTO modeling", "GBP 3,200")
                .pricingRow("Document delivery", "Invoice and proposal templates with tests", "GBP 4,450")
                .emphasizedPricingRow("Total investment", "Fixed-price project delivery", "GBP 9,500")
                .acceptanceTerm("Proposal pricing is valid until the stated expiration date.")
                .acceptanceTerm("Additional template families can be scoped in a separate phase.")
                .footerNote("Prepared to demonstrate the business-document side of GraphCompose.")
                .build();
    }
}
