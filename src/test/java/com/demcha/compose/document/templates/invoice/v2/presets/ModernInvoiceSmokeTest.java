package com.demcha.compose.document.templates.invoice.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the layered {@code invoice.v2} pipeline through
 * {@link ModernInvoice} — proves the preset renders an
 * {@link InvoiceDocumentSpec} end-to-end on a {@link BrandTheme}, via
 * both factory variants, with any theme, and on an empty invoice.
 */
class ModernInvoiceSmokeTest {

    private static InvoiceDocumentSpec sampleSpec() {
        return InvoiceDocumentSpec.from(InvoiceData.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-001")
                .issueDate("01 May 2026")
                .dueDate("15 May 2026")
                .status("Sent")
                .fromParty(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance", "410 Market Avenue")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200"))
                .lineItem("Discovery workshop", "Stakeholder interviews",
                        "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable document flows",
                        "2", "GBP 980", "GBP 1,960")
                .summaryRow("Subtotal", "GBP 3,410")
                .summaryRow("VAT (20%)", "GBP 682")
                .totalRow("Total", "GBP 4,092")
                .note("Payment due within 14 days.")
                .paymentTerm("Bank transfer, NET 14")
                .footerNote("Thank you for your business.")
                .build());
    }

    /** An invoice with no line items, summaries, notes, or footer — the empty paths. */
    private static InvoiceDocumentSpec minimalSpec() {
        return InvoiceDocumentSpec.from(InvoiceData.builder()
                .invoiceNumber("GC-2026-002")
                .fromParty(from -> from.name("GraphCompose Studio"))
                .billToParty(to -> to.name("Northwind Systems"))
                .build());
    }

    private static void render(DocumentTemplate<InvoiceDocumentSpec> template,
                               InvoiceDocumentSpec spec) throws Exception {
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
        DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();
        assertThat(template.id()).isEqualTo(ModernInvoice.ID);
        assertThat(template.displayName()).isEqualTo(ModernInvoice.DISPLAY_NAME);
    }

    @Test
    void defaultFactoryRendersWithInvoiceTheme() throws Exception {
        // create() wires BrandTheme.invoiceModern() — the variant the example uses.
        render(ModernInvoice.create(), sampleSpec());
    }

    @Test
    void rendersWithExplicitTheme() throws Exception {
        render(ModernInvoice.create(BrandTheme.invoiceModern()), sampleSpec());
    }

    @Test
    void readsAnyTheme() throws Exception {
        // Proves the preset reads the theme rather than assuming invoiceModern() slots.
        render(ModernInvoice.create(BrandTheme.boxedClassic()), sampleSpec());
    }

    @Test
    void rendersEmptyInvoice() throws Exception {
        // Exercises the empty-collection + header-only-table + skipped-footer paths.
        render(ModernInvoice.create(), minimalSpec());
    }
}
