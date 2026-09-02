package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The one invoice the Merchant preset's tests render.
 *
 * <p>Both gates read it: the exact layout snapshot and the pixel baseline. One
 * fixture behind both is what makes the two comparable — a shift the snapshot
 * catches is a shift in the same document the baseline was recorded from.</p>
 *
 * <p>One of its lines counts nothing, because a fee charged as it falls rather
 * than per unit is what the design's dash is for and a fixture without one would
 * not exercise it.</p>
 */
final class MerchantInvoiceFixtures {

    private MerchantInvoiceFixtures() {
    }

    /** A month of commerce-platform subscription, billed to one merchant. */
    static StructuredInvoiceData invoice() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "Coastline", "", "", "C", ""))
                .supplier(new InvoiceContactBlock(
                        "Coastline Commerce Inc.",
                        List.of("150 Elgin Street, 8th Floor", "Ottawa, ON K2P 1L4", "Canada"),
                        "+1 613-555-0142",
                        "billing@coastline.example",
                        "www.coastline.example",
                        "", "",
                        "VAT ID:", "CA 813 9020 285 RT0001"))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice Number:", "INV-2026-000789", true),
                        new InvoiceMasthead.Entry("Invoice Date:", "27 May 2026", false),
                        new InvoiceMasthead.Entry("Due Date:", "26 June 2026", false),
                        new InvoiceMasthead.Entry("Billing Period:", "01 May 2026 – 31 May 2026", false),
                        new InvoiceMasthead.Entry("Payment Terms:", "Net 30 Days", false),
                        new InvoiceMasthead.Entry("Currency:", "USD", false))))
                .billTo(new InvoiceRecipient("BILL TO", "Bright Future Ltd.", "",
                        List.of("45 King Street", "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "VAT ID:", "GB 987 6543 21"))
                .shipTo(new InvoiceRecipient("SHIP TO", "Bright Future Ltd.", "",
                        List.of("Warehouse 7", "North Industrial Estate",
                                "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "", ""))
                .serviceLines(new InvoiceServiceLines(
                        new InvoiceServiceLines.Columns("", "DESCRIPTION", "PLAN / SERVICE",
                                "QTY", "UNIT PRICE", "AMOUNT"),
                        lines()))
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("Subtotal", new BigDecimal("444.00")),
                        new InvoiceTotalsBlock.Row("Tax (0%)", new BigDecimal("0.00"))),
                        "TOTAL DUE", new BigDecimal("444.00")))
                .payment(new InvoicePaymentBlock("PAYMENT DETAILS", List.of(
                        new InvoicePaymentBlock.Field("Bank Name:", "Harbour Bank of Canada"),
                        new InvoicePaymentBlock.Field("Account Name:", "Coastline Commerce Inc."),
                        new InvoicePaymentBlock.Field("Account Number:", "1001-4267-153"),
                        new InvoicePaymentBlock.Field("SWIFT Code:", "HRBRCAT2"),
                        new InvoicePaymentBlock.Field("Institution Number:", "003"),
                        new InvoicePaymentBlock.Field("Transit Number:", "00016"),
                        new InvoicePaymentBlock.Field("Account Type:", "CAD Account"),
                        new InvoicePaymentBlock.Field("Remittance Email:",
                                "billing@coastline.example"),
                        new InvoicePaymentBlock.Field("Reference:", "INV-2026-000789")),
                        "Please include the invoice number\nin your payment reference.",
                        "PAYMENT DUE BY", "26 June 2026", "", ""))
                .notes(new InvoiceNotesBlock("Thank you for choosing Coastline.",
                        List.of("If you have any questions about this invoice,"
                                + " please contact us at billing@coastline.example."),
                        "billing@coastline.example", ""))
                .currencyCode("USD")
                .build();
    }

    private static List<InvoiceServiceLines.Line> lines() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Coastline Advanced", "Monthly subscription", "Advanced Plan",
                "1", "399.00", "399.00", "bag"));
        // Charged as it falls rather than per unit, which is what the dash is for.
        lines.add(line(2, "Payment Processing", "Transaction fee", "As per usage",
                "0", "0.00", "0.00", "tag"));
        lines.add(line(3, "Additional Staff Accounts", "Staff account", "Additional",
                "3", "15.00", "45.00", "globe"));
        lines.add(line(4, "Priority Support", "24/7 support", "Included",
                "1", "0.00", "0.00", "support"));
        return lines;
    }

    private static InvoiceServiceLines.Line line(int number, String title, String subtitle,
                                                 String plan, String quantity,
                                                 String unitPrice, String amount, String icon) {
        return new InvoiceServiceLines.Line(number, title, subtitle, plan,
                new BigDecimal(quantity), "", new BigDecimal(unitPrice),
                new BigDecimal(amount), "", icon, "");
    }
}
