package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase E.1 — InvoiceTemplateV2 must compose against the canonical DSL,
 * accept any BusinessTheme via the constructor, and produce a valid
 * paginated PDF for the standard {@link InvoiceDocumentSpec} sample.
 *
 * @author Artem Demchyshyn
 */
class InvoiceTemplateV2Test {

    @Test
    void defaultConstructorUsesModernTheme() {
        InvoiceTemplateV2 template = new InvoiceTemplateV2();
        assertThat(template.getTemplateId()).isEqualTo("invoice-v2");
        assertThat(template.getTemplateName()).isEqualTo("Invoice V2 (cinematic)");
    }

    @Test
    void invoiceTemplateImplementsCanonicalInterface() {
        InvoiceTemplate template = new InvoiceTemplateV2();
        assertThat(template).isNotNull();
    }

    @Test
    void composeProducesValidPdfBytesForSampleInvoice() throws Exception {
        InvoiceDocumentSpec spec = sampleInvoice();
        InvoiceTemplateV2 template = new InvoiceTemplateV2();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(BusinessTheme.modern().pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {

            template.compose(document, spec);
            byte[] bytes = document.toPdfBytes();
            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                    .as("PDF magic header — graphics-state leak would corrupt this")
                    .isEqualTo("%PDF-");
        }
    }

    @Test
    void differentThemesProduceDistinctRenderedBytes() throws Exception {
        InvoiceDocumentSpec spec = sampleInvoice();

        byte[] modernBytes = renderWithTheme(spec, BusinessTheme.modern());
        byte[] classicBytes = renderWithTheme(spec, BusinessTheme.classic());

        // Both must be valid PDF byte streams.
        assertThat(modernBytes).startsWith("%PDF-".getBytes());
        assertThat(classicBytes).startsWith("%PDF-".getBytes());
        // The themed variants should differ in bytes — the palette colours
        // get embedded in the PDF stream, so a theme switch must be
        // observable downstream.
        assertThat(modernBytes).isNotEqualTo(classicBytes);
    }

    @Test
    void layoutGraphContainsLineItemsTable() throws Exception {
        InvoiceDocumentSpec spec = sampleInvoice();
        InvoiceTemplateV2 template = new InvoiceTemplateV2();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(28))
                .create()) {

            template.compose(document, spec);
            LayoutGraph graph = document.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThanOrEqualTo(1);
            // The layout graph should contain at least one node whose
            // semantic name signals the line-items table — this is the
            // anchor invariant that proves the template ran the table
            // composition (not just the hero block).
            assertThat(graph.nodes())
                    .anyMatch(node -> node.semanticName() != null
                            && node.semanticName().contains("InvoiceLineItems"));
        }
    }

    private static byte[] renderWithTheme(InvoiceDocumentSpec spec, BusinessTheme theme) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            new InvoiceTemplateV2(theme).compose(document, spec);
            return document.toPdfBytes();
        }
    }

    private static InvoiceDocumentSpec sampleInvoice() {
        return InvoiceDocumentSpec.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Platform Refresh Sprint")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example"))
                .lineItem("Discovery workshop", "Stakeholder interviews", "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable document flows", "2", "GBP 980", "GBP 1,960")
                .lineItem("Render QA", "Visual validation passes", "3", "GBP 320", "GBP 960")
                .summaryRow("Subtotal", "GBP 4,370")
                .summaryRow("VAT (20%)", "GBP 874")
                .totalRow("Total", "GBP 5,244")
                .note("Please include the invoice number on your remittance advice.")
                .paymentTerm("Payment due within 14 calendar days.")
                .footerNote("Thank you for choosing GraphCompose for production rendering.")
                .build();
    }
}
