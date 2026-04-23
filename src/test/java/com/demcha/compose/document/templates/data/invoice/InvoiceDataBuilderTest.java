package com.demcha.compose.document.templates.data.invoice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDataBuilderTest {

    @Test
    void builderShouldCreateInvoiceWithPartiesItemsTotalsNotesAndTerms() {
        InvoiceData invoice = InvoiceData.builder()
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Platform Refresh Sprint")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example"))
                .lineItem("Discovery workshop", "Stakeholder interviews", "1", "GBP 1,450", "GBP 1,450")
                .lineItem(item -> item
                        .description("Render QA")
                        .details("Visual validation and guide passes")
                        .quantity("2")
                        .unitPrice("GBP 320")
                        .amount("GBP 640"))
                .summaryRow("Subtotal", "GBP 2,090")
                .totalRow("Total", "GBP 2,090")
                .note("Please include the invoice number on your remittance advice.")
                .paymentTerm("Payment due within 14 calendar days.")
                .footerNote("Thank you for choosing GraphCompose.")
                .build();

        assertThat(invoice.title()).isEqualTo("Invoice");
        assertThat(invoice.invoiceNumber()).isEqualTo("GC-2026-041");
        assertThat(invoice.fromParty().name()).isEqualTo("GraphCompose Studio");
        assertThat(invoice.billToParty().name()).isEqualTo("Northwind Systems");
        assertThat(invoice.lineItems()).hasSize(2);
        assertThat(invoice.summaryRows()).hasSize(2);
        assertThat(invoice.summaryRows().get(1).emphasized()).isTrue();
        assertThat(invoice.notes()).containsExactly("Please include the invoice number on your remittance advice.");
        assertThat(invoice.paymentTerms()).containsExactly("Payment due within 14 calendar days.");
        assertThat(invoice.footerNote()).isEqualTo("Thank you for choosing GraphCompose.");
    }
}
