package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.invoice.spec.InvoiceSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the v2 invoice pipeline through
 * {@link ModernInvoice}.
 */
class ModernInvoiceSmokeTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static InvoiceSpec sampleSpec() {
        return InvoiceSpec.builder()
                .invoiceNumber("GC-2026-001")
                .issueDate("01 May 2026")
                .dueDate("15 May 2026")
                .fromParty(new InvoiceSpec.Party("GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK"),
                        "billing@graphcompose.dev",
                        "+44 20 5555 1000",
                        "GB-99887766"))
                .billToParty(new InvoiceSpec.Party("Northwind Systems",
                        List.of("Attn: Finance", "410 Market Avenue"),
                        "ap@northwind.example", "+44 161 555 2200", ""))
                .lineItem(new InvoiceSpec.LineItem(
                        "Discovery workshop", "Stakeholder interviews",
                        "1", "GBP 1,450", "GBP 1,450"))
                .lineItem(new InvoiceSpec.LineItem(
                        "Template architecture", "Reusable document flows",
                        "2", "GBP 980", "GBP 1,960"))
                .summaryRow(InvoiceSpec.SummaryRow.of("Subtotal", "GBP 3,410"))
                .summaryRow(InvoiceSpec.SummaryRow.of("VAT (20%)", "GBP 682"))
                .summaryRow(InvoiceSpec.SummaryRow.total("Total", "GBP 4,092"))
                .note("Payment due within 14 days.")
                .build();
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<InvoiceSpec> template = ModernInvoice.create(THEME);
        assertThat(template.id()).isEqualTo(ModernInvoice.ID);
        assertThat(template.displayName()).isEqualTo(ModernInvoice.DISPLAY_NAME);
    }

    @Test
    void composeAddsRootDocumentNode() throws Exception {
        DocumentTemplate<InvoiceSpec> template = ModernInvoice.create(THEME);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            template.compose(session, sampleSpec());
            assertThat(session.roots()).isNotEmpty();
        }
    }
}
