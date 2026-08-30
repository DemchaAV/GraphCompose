package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;

/**
 * Shared fixture data for the invoice preset gates — the SAME specs feed
 * the pixel parity test and the layout snapshot test, so a geometry shift
 * the pixel budget absorbs still trips the exact snapshot, and vice versa.
 */
final class InvoicePresetFixtures {

    private InvoicePresetFixtures() {
    }

    /**
     * Canonical sample invoice — exercises the hero, both parties, a
     * multi-row line-items table, subtotal / tax / total summary, and the
     * notes / payment-terms footer. Kept in qa so the gates depend only
     * on main + main-test code.
     */
    static InvoiceDocumentSpec canonicalInvoice() {
        return InvoiceDocumentSpec.from(InvoiceData.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .status("Pending")
                .fromParty(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200")
                        .taxId("NW-2026-01"))
                .lineItem("Discovery workshop", "Stakeholder interviews",
                        "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable document flows",
                        "2", "GBP 980", "GBP 1,960")
                .lineItem("Render QA", "Cross-platform pixel diffing",
                        "3", "GBP 320", "GBP 960")
                .lineItem("Developer enablement", "Authoring docs + examples",
                        "1", "GBP 780", "GBP 780")
                .summaryRow("Subtotal", "GBP 5,150")
                .summaryRow("VAT (20%)", "GBP 1,030")
                .totalRow("Total", "GBP 6,180")
                .note("Please include the invoice number on your remittance advice.")
                .note("All work was delivered as agreed during the April implementation window.")
                .paymentTerm("Payment due within 14 calendar days.")
                .paymentTerm("Bank transfer preferred; contact billing@graphcompose.dev for remittance details.")
                .paymentTerm("Late payments may delay additional template customization work.")
                .footerNote("Thank you for choosing GraphCompose for production document rendering.")
                .build());
    }

    /**
     * Overflow invoice — forty line items so the table paginates naturally
     * and the repeated header + cross-page flow become part of the frozen
     * contract. No manual page breaks.
     */
    static InvoiceDocumentSpec stressInvoice() {
        String[] services = {
                "Discovery workshop", "Design system audit", "Template architecture",
                "Layout engine tuning", "Render QA pass", "Accessibility review",
                "Font pipeline setup", "Chart integration", "Data mapping",
                "Pagination hardening", "Visual regression wiring", "Docs authoring",
                "Stakeholder demo", "Performance profiling", "Release engineering",
                "Support retainer"};
        InvoiceData.Builder builder = InvoiceData.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-042")
                .issueDate("04 May 2026")
                .dueDate("18 May 2026")
                .status("Pending")
                .fromParty(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000"))
                .billToParty(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200"));
        for (int i = 0; i < 40; i++) {
            String service = services[i % services.length];
            int quantity = 1 + i % 4;
            int unit = 180 + (i * 37) % 900;
            builder.lineItem(
                    service + " — sprint " + (1 + i / services.length),
                    "Delivered under the framework agreement",
                    String.valueOf(quantity),
                    "GBP " + unit,
                    "GBP " + (quantity * unit));
        }
        return InvoiceDocumentSpec.from(builder
                .summaryRow("Subtotal", "GBP 44,890")
                .summaryRow("VAT (20%)", "GBP 8,978")
                .totalRow("Total", "GBP 53,868")
                .note("Sprints 1-3 were delivered under the framework agreement.")
                .note("Hardware and third-party licences are billed separately.")
                .paymentTerm("Payment due within 14 calendar days.")
                .paymentTerm("Bank transfer preferred; contact billing@graphcompose.dev for remittance details.")
                .footerNote("Thank you for choosing GraphCompose for production document rendering.")
                .build());
    }
}
