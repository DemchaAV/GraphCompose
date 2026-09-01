package com.demcha.compose.document.templates.data.invoice;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the compatibility promise made when the structured invoice model grew
 * the fields a second sheet needed: the constructors that predate them are
 * still there and still mean what they meant, so a caller written against
 * any of them keeps compiling and linking.
 */
class StructuredInvoiceCompatibilityTest {

    @Test
    void theBrandConstructorThatPredatesTheMonogramLeavesItBlank() {
        InvoiceBrand brand = new InvoiceBrand(null, "Luma", "Studio", "Design.");
        assertThat(brand.monogramTop()).isEmpty();
        assertThat(brand.monogramBottom()).isEmpty();
        assertThat(brand.hasMonogram()).isFalse();
        assertThat(brand.name()).isEqualTo("Luma");
    }

    @Test
    void aBrandWithEitherMonogramLineHasOne() {
        assertThat(new InvoiceBrand(null, "Luma", "", "", "L", "").hasMonogram()).isTrue();
        assertThat(new InvoiceBrand(null, "Luma", "", "", "", "&Co.").hasMonogram()).isTrue();
    }

    @Test
    void theContactConstructorThatPredatesTheSecondRegistrationLeavesItBlank() {
        InvoiceContactBlock supplier = new InvoiceContactBlock(
                "Luma Ltd", List.of("14 Gower Street"), "+44", "a@b.c", "b.c",
                "Company No.", "12578934");
        assertThat(supplier.taxRegistrationLabel()).isEmpty();
        assertThat(supplier.taxRegistrationNumber()).isEmpty();
        assertThat(supplier.registrationNumber()).isEqualTo("12578934");
    }

    @Test
    void theContactConstructorThatPredatesTheLegalFootnoteLeavesItBlank() {
        InvoiceContactBlock supplier = new InvoiceContactBlock(
                "Luma Ltd", List.of("14 Gower Street"), "+44", "a@b.c", "b.c",
                "Company No.", "12578934", "VAT No.", "GB123");
        assertThat(supplier.legalFootnote()).isEmpty();
        assertThat(supplier.taxRegistrationNumber()).isEqualTo("GB123");
    }

    @Test
    void aContactCarriesTheLegalFootnoteItIsGiven() {
        InvoiceContactBlock supplier = new InvoiceContactBlock(
                "Luma Ltd", List.of("14 Gower Street"), "+44", "a@b.c", "b.c",
                "Company No.", "12578934", "VAT No.", "GB123",
                "Luma Ltd is a subsidiary of Luma Group plc.");
        assertThat(supplier.legalFootnote())
                .isEqualTo("Luma Ltd is a subsidiary of Luma Group plc.");
    }

    @Test
    void aNullLegalFootnoteNormalizesToBlankLikeTheFieldsBesideIt() {
        InvoiceContactBlock supplier = new InvoiceContactBlock(
                "Luma Ltd", List.of("14 Gower Street"), "+44", "a@b.c", "b.c",
                "Company No.", "12578934", "VAT No.", "GB123", null);
        assertThat(supplier.legalFootnote()).isEmpty();
    }

    @Test
    void thePaymentConstructorThatPredatesTheAccountHolderLeavesItBlank() {
        InvoicePaymentBlock payment = new InvoicePaymentBlock(
                "PAYMENT", List.of(new InvoicePaymentBlock.Field("BANK", "Starling")),
                "Pay by transfer.", "Due in 30 days", "30 days");
        assertThat(payment.accountHolder()).isEmpty();
        assertThat(payment.signOff()).isEmpty();
        assertThat(payment.dueNoticeEmphasis()).isEqualTo("30 days");
    }

    @Test
    void theColumnsConstructorThatPredatesTheVatColumnLeavesItBlank() {
        InvoiceServiceLines.Columns columns = new InvoiceServiceLines.Columns(
                "#", "DESCRIPTION", "PERIOD", "QTY", "UNIT", "AMOUNT");
        assertThat(columns.vat()).isEmpty();
        assertThat(columns.amount()).isEqualTo("AMOUNT");
    }

    @Test
    void theLineConstructorThatPredatesTheVatRateLeavesItAndTheMarkBlank() {
        InvoiceServiceLines.Line line = new InvoiceServiceLines.Line(
                1, "Workshop", "A day of it", "May", BigDecimal.ONE, "day",
                new BigDecimal("1200"), new BigDecimal("1200"));
        assertThat(line.vatRate()).isEmpty();
        assertThat(line.icon()).isEmpty();
        assertThat(line.amount()).isEqualByComparingTo("1200");
    }

    @Test
    void theLineConstructorThatPredatesTheMarkLeavesItBlank() {
        // The tax rate arrived before the mark did, so a caller written against
        // that version keeps compiling and keeps drawing no mark.
        InvoiceServiceLines.Line line = new InvoiceServiceLines.Line(
                1, "Workshop", "A day of it", "May", BigDecimal.ONE, "day",
                new BigDecimal("1200"), new BigDecimal("1200"), "20%");
        assertThat(line.vatRate()).isEqualTo("20%");
        assertThat(line.icon()).isEmpty();
    }

    @Test
    void aLineCarriesTheMarkItIsGiven() {
        InvoiceServiceLines.Line line = new InvoiceServiceLines.Line(
                1, "Workshop", "A day of it", "May", BigDecimal.ONE, "day",
                new BigDecimal("1200"), new BigDecimal("1200"), "20%", "card");
        assertThat(line.icon()).isEqualTo("card");
    }

    @Test
    void aNullMarkNormalizesToBlankLikeTheFieldsBesideIt() {
        InvoiceServiceLines.Line line = new InvoiceServiceLines.Line(
                1, "Workshop", "A day of it", "May", BigDecimal.ONE, "day",
                new BigDecimal("1200"), new BigDecimal("1200"), "20%", null);
        assertThat(line.icon()).isEmpty();
    }

    @Test
    void theRecipientConstructorThatPredatesTheRegistrationLeavesItBlank() {
        InvoiceRecipient recipient = new InvoiceRecipient("BILL TO", "Northwind Ltd.", "",
                List.of("42 Bridgewater Street"), "Email", "ap@example.test");
        assertThat(recipient.registrationLabel()).isEmpty();
        assertThat(recipient.registrationNumber()).isEmpty();
        assertThat(recipient.hasRegistration()).isFalse();
    }

    @Test
    void aRecipientCarriesTheRegistrationItIsGiven() {
        InvoiceRecipient recipient = new InvoiceRecipient("BILL TO", "Northwind Ltd.", "",
                List.of("42 Bridgewater Street"), "", "", "VAT Number", "GB 987 6543 21");
        assertThat(recipient.registrationLabel()).isEqualTo("VAT Number");
        assertThat(recipient.registrationNumber()).isEqualTo("GB 987 6543 21");
        assertThat(recipient.hasRegistration()).isTrue();
    }

    @Test
    void aRecipientWithALabelAndNoNumberPrintsNothing() {
        // The number is what makes the row worth drawing; a label on its own is
        // a heading over an absence.
        InvoiceRecipient recipient = new InvoiceRecipient("BILL TO", "Northwind Ltd.", "",
                List.of(), "", "", "VAT Number", null);
        assertThat(recipient.hasRegistration()).isFalse();
    }

    @Test
    void theDataConstructorThatPredatesShipToLeavesItEmptyRatherThanNull() {
        // The record normalizes an absent block to its empty form, so preset
        // code reads it without a null check — the promise the whole model
        // makes.
        StructuredInvoiceData data = new StructuredInvoiceData(
                null, null, null,
                new InvoiceRecipient("BILL TO", "Northfield", "", List.of("21 Jubilee Way"),
                        "", ""),
                null, null, null, null, null, "GBP");
        assertThat(data.shipTo()).isNotNull();
        assertThat(data.shipTo().name()).isEmpty();
        assertThat(data.billTo().name()).isEqualTo("Northfield");
    }

    @Test
    void theBuilderCarriesShipToThrough() {
        StructuredInvoiceData data = StructuredInvoiceData.builder()
                .billTo(new InvoiceRecipient("BILL TO", "Northfield", "", List.of(), "", ""))
                .shipTo(new InvoiceRecipient("SHIP TO", "The Foundry", "", List.of(), "", ""))
                .build();
        assertThat(data.shipTo().name()).isEqualTo("The Foundry");
        assertThat(data.shipTo().heading()).isEqualTo("SHIP TO");
    }
}
