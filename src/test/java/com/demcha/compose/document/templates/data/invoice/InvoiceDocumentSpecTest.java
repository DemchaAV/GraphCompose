package com.demcha.compose.document.templates.data.invoice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDocumentSpecTest {

    @Test
    void builderShouldCreateDocumentLevelInvoiceSpec() {
        InvoiceDocumentSpec spec = InvoiceDocumentSpec.builder()
                .invoiceNumber("GC-2026-041")
                .fromParty(party -> party.name("GraphCompose Studio"))
                .billToParty(party -> party.name("Northwind Systems"))
                .lineItem("Template architecture", "Reusable invoice flow", "1", "GBP 980", "GBP 980")
                .summaryRow("Subtotal", "GBP 980")
                .totalRow("Total", "GBP 980")
                .note("Please include the invoice number on the remittance.")
                .paymentTerm("Payment due within 14 days.")
                .build();

        assertThat(spec.invoice().invoiceNumber()).isEqualTo("GC-2026-041");
        assertThat(spec.invoice().fromParty().name()).isEqualTo("GraphCompose Studio");
        assertThat(spec.invoice().billToParty().name()).isEqualTo("Northwind Systems");
        assertThat(spec.invoice().lineItems()).singleElement()
                .extracting(InvoiceLineItem::description)
                .isEqualTo("Template architecture");
        assertThat(spec.invoice().summaryRows()).hasSize(2);
        assertThat(spec.invoice().summaryRows().get(1).emphasized()).isTrue();
        assertThat(spec.invoice().notes()).containsExactly("Please include the invoice number on the remittance.");
        assertThat(spec.invoice().paymentTerms()).containsExactly("Payment due within 14 days.");
    }

    @Test
    void fromShouldWrapExistingInvoiceDataWithoutChangingIt() {
        InvoiceData invoice = InvoiceData.builder()
                .invoiceNumber("INV-1")
                .build();

        assertThat(InvoiceDocumentSpec.from(invoice).invoice()).isSameAs(invoice);
    }
}
