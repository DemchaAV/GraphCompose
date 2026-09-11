package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Shared roster and canonical sample invoice for the layered invoice
 * presets.
 *
 * <p>Both preset gates read from here so they always describe the same
 * render: {@code InvoiceV2VisualParityTest} compares the rasterised
 * pages per-pixel, {@code InvoicePresetLayoutSnapshotTest} compares the
 * post-layout node tree. Adding a preset to {@link #presets()} enrols
 * it in both.</p>
 */
final class InvoicePresetFixtures {

    private InvoicePresetFixtures() {
    }

    /**
     * Every layered invoice preset, as {@code (slug,
     * recommendedMargin, factory)} triples.
     */
    static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("modern_invoice",
                        ModernInvoice.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<InvoiceDocumentSpec>>) ModernInvoice::create));
    }

    /**
     * Canonical sample invoice — exercises the hero, both parties, a
     * multi-row line-items table, subtotal / tax / total summary, and the
     * notes / payment-terms footer. Kept inline so the tests depend only
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
}
