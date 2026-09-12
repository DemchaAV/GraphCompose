package com.demcha.compose.document.templates.data.invoice;

import com.demcha.compose.document.image.DocumentImageData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Normalization and immutability contract of the structured invoice model:
 * absent components become their empty forms, leaf strings never surface as
 * {@code null}, money and quantities default to zero rather than null, the
 * brand logo stays optional, and every collection is frozen at construction.
 */
class StructuredInvoiceDataTest {

    @Test
    void emptyBuilderYieldsEmptyFormsForEveryComponent() {
        StructuredInvoiceData data = StructuredInvoiceData.builder().build();

        assertThat(data.brand().name()).isEmpty();
        assertThat(data.brand().hasLogo()).isFalse();
        assertThat(data.supplier().addressLines()).isEmpty();
        assertThat(data.masthead().entries()).isEmpty();
        assertThat(data.billTo().addressLines()).isEmpty();
        assertThat(data.summary().intro()).isEmpty();
        assertThat(data.serviceLines().lines()).isEmpty();
        assertThat(data.serviceLines().columns().amount()).isEmpty();
        assertThat(data.totals().rows()).isEmpty();
        assertThat(data.totals().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(data.payment().fields()).isEmpty();
        assertThat(data.notes().paragraphs()).isEmpty();
        assertThat(data.currencyCode()).isEmpty();
    }

    @Test
    void leafRecordsNormalizeNullStringsToEmpty() {
        InvoiceBrand brand = new InvoiceBrand(null, null, null, null);
        assertThat(brand.name()).isEmpty();
        assertThat(brand.qualifier()).isEmpty();
        assertThat(brand.tagline()).isEmpty();

        InvoiceContactBlock supplier =
                new InvoiceContactBlock(null, null, null, null, null, null, null);
        assertThat(supplier.legalName()).isEmpty();
        assertThat(supplier.website()).isEmpty();
        assertThat(supplier.registrationLabel()).isEmpty();

        InvoiceMasthead.Entry entry = new InvoiceMasthead.Entry(null, null, false);
        assertThat(entry.label()).isEmpty();
        assertThat(entry.value()).isEmpty();

        InvoiceRecipient recipient = new InvoiceRecipient(null, null, null, null, null, null);
        assertThat(recipient.heading()).isEmpty();
        assertThat(recipient.emailLabel()).isEmpty();
        assertThat(recipient.subline()).isEmpty();

        InvoicePaymentBlock.Field field = new InvoicePaymentBlock.Field(null, null);
        assertThat(field.label()).isEmpty();
        assertThat(field.value()).isEmpty();
    }

    @Test
    void moneyAndQuantitiesDefaultToZeroRatherThanNull() {
        InvoiceServiceLines.Line line = new InvoiceServiceLines.Line(
                1, null, null, null, null, null, null, null);
        assertThat(line.quantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line.unitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line.amount()).isEqualByComparingTo(BigDecimal.ZERO);

        InvoiceTotalsBlock.Row row = new InvoiceTotalsBlock.Row("Subtotal", null);
        assertThat(row.amount()).isEqualByComparingTo(BigDecimal.ZERO);

        InvoiceTotalsBlock totals = new InvoiceTotalsBlock(null, null, null);
        assertThat(totals.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void brandLogoIsOptionalAndReportedByHasLogo() {
        DocumentImageData logo = DocumentImageData.fromBytes(new byte[] {1, 2, 3});
        InvoiceBrand withLogo = new InvoiceBrand(logo, "Northpoint", "Consulting", "Tagline");
        assertThat(withLogo.hasLogo()).isTrue();
        assertThat(withLogo.logo()).isSameAs(logo);

        assertThat(new InvoiceBrand(null, "Northpoint", "", "").hasLogo()).isFalse();
    }

    @Test
    void everyCollectionIsFrozenAtConstruction() {
        List<String> source = new ArrayList<>(List.of("seed"));
        InvoiceContactBlock supplier = new InvoiceContactBlock(
                "Studio", source, "", "", "", "ABN", "12 345");
        source.add("added after construction");
        assertThat(supplier.addressLines()).containsExactly("seed");

        List<List<?>> frozen = List.of(
                supplier.addressLines(),
                new InvoiceMasthead("INVOICE", new ArrayList<>()).entries(),
                new InvoiceRecipient("BILLED TO", "N", "", new ArrayList<>(), "", "").addressLines(),
                new InvoiceServiceLines(null, new ArrayList<>()).lines(),
                new InvoiceTotalsBlock(new ArrayList<>(), "TOTAL", BigDecimal.ONE).rows(),
                new InvoicePaymentBlock("PAY", new ArrayList<>(), "", "", "").fields(),
                new InvoiceNotesBlock("NOTES", new ArrayList<>(), "", "").paragraphs());
        for (List<?> list : frozen) {
            assertThatThrownBy(() -> list.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void builderPassesEveryComponentThroughUnchanged() {
        InvoiceBrand brand = new InvoiceBrand(null, "Northpoint", "Consulting", "Strategy.");
        InvoiceContactBlock supplier = new InvoiceContactBlock("Northpoint Pty Ltd",
                List.of("Level 8"), "+61", "hello@example.com", "example.com", "ABN", "12 345");
        InvoiceMasthead masthead = new InvoiceMasthead("INVOICE",
                List.of(new InvoiceMasthead.Entry("Due Date:", "25 June 2025", true)));
        InvoiceRecipient billTo = new InvoiceRecipient("BILLED TO", "Greenfield",
                "Accounts Payable", List.of("12 Innovation Drive"), "Email:", "ap@example.com");
        InvoiceSummaryBlock summary =
                new InvoiceSummaryBlock("INVOICE SUMMARY", "Services rendered.", "1–25 May");
        InvoiceServiceLines serviceLines = new InvoiceServiceLines(
                new InvoiceServiceLines.Columns("#", "DESCRIPTION", "SERVICE PERIOD",
                        "QTY", "UNIT PRICE", "AMOUNT"),
                List.of(new InvoiceServiceLines.Line(1, "Consultation", "Workshops",
                        "1–10 May", new BigDecimal("10"), "hrs",
                        new BigDecimal("250"), new BigDecimal("2500"))));
        InvoiceTotalsBlock totals = new InvoiceTotalsBlock(
                List.of(new InvoiceTotalsBlock.Row("SUBTOTAL", new BigDecimal("14000"))),
                "TOTAL DUE", new BigDecimal("15400"));
        InvoicePaymentBlock payment = new InvoicePaymentBlock("PAYMENT INFORMATION",
                List.of(new InvoicePaymentBlock.Field("BSB:", "123-456")),
                "Include the invoice number.", "Payment is due within 30 days.", "30 days");
        InvoiceNotesBlock notes = new InvoiceNotesBlock("NOTES",
                List.of("Thank you."), "accounts@example.com", "+61");
        StructuredInvoiceData data = StructuredInvoiceData.builder()
                .brand(brand)
                .supplier(supplier)
                .masthead(masthead)
                .billTo(billTo)
                .summary(summary)
                .serviceLines(serviceLines)
                .totals(totals)
                .payment(payment)
                .notes(notes)
                .currencyCode("AUD")
                .build();

        assertThat(data.brand()).isSameAs(brand);
        assertThat(data.supplier()).isSameAs(supplier);
        assertThat(data.masthead()).isSameAs(masthead);
        assertThat(data.billTo()).isSameAs(billTo);
        assertThat(data.summary()).isSameAs(summary);
        assertThat(data.serviceLines()).isSameAs(serviceLines);
        assertThat(data.totals()).isSameAs(totals);
        assertThat(data.payment()).isSameAs(payment);
        assertThat(data.notes()).isSameAs(notes);
        assertThat(data.currencyCode()).isEqualTo("AUD");
        assertThat(data.masthead().entries().get(0).emphasized()).isTrue();
    }

    @Test
    void documentSpecNormalizesNullInvoiceToEmptyData() {
        assertThat(StructuredInvoiceDocumentSpec.from(null).invoice()).isNotNull();
        assertThat(new StructuredInvoiceDocumentSpec(null).invoice().payment().fields()).isEmpty();

        StructuredInvoiceData data = StructuredInvoiceData.builder().build();
        assertThat(StructuredInvoiceDocumentSpec.from(data).invoice()).isSameAs(data);
    }
}
