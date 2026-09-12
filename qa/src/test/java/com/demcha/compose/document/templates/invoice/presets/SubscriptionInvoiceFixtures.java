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
 * The one invoice the Subscription preset's tests render.
 *
 * <p>Both gates read it: the exact layout snapshot and the pixel baseline. One
 * fixture behind both is what makes the two comparable — a shift the snapshot
 * catches is a shift in the same document the baseline was recorded from.</p>
 *
 * <p>It bills seats with a tax rate on every line, because that is what this
 * design's table carries and the others' do not.</p>
 */
final class SubscriptionInvoiceFixtures {

    private SubscriptionInvoiceFixtures() {
    }

    /** A billing period of software subscriptions, billed to one customer. */
    static StructuredInvoiceData invoice() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "Halstead", "", "", "", ""))
                .supplier(new InvoiceContactBlock(
                        "Halstead Software Ltd.",
                        List.of("18 Chandler Row", "Leeds LS1 4QT", "United Kingdom"),
                        "",
                        "billing@halstead.example",
                        "www.halstead.example",
                        "", "",
                        "VAT No", "GB 418 2276 03"))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice No", "HS-INV-2026-0417", false),
                        new InvoiceMasthead.Entry("Issue Date", "27 August 2026", false),
                        new InvoiceMasthead.Entry("Due Date", "10 September 2026", false),
                        new InvoiceMasthead.Entry("Payment Terms", "14 days", false),
                        new InvoiceMasthead.Entry("Currency", "GBP", false))))
                .billTo(new InvoiceRecipient("BILL TO", "Northline Consulting Ltd",
                        "Attn: Sarah Mitchell",
                        List.of("21 Jubilee Way", "London SE1 3SS", "United Kingdom"),
                        "", "", "", ""))
                .shipTo(new InvoiceRecipient("SHIP TO", "Northline Consulting Ltd",
                        "The Foundry, 2nd Floor",
                        List.of("17-19 Great Suffolk Street", "London SE1 0NS",
                                "United Kingdom"),
                        "", "", "", ""))
                .serviceLines(new InvoiceServiceLines(
                        new InvoiceServiceLines.Columns("#", "DESCRIPTION", "",
                                "QTY", "UNIT PRICE", "AMOUNT", "VAT"),
                        lines()))
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("Subtotal", new BigDecimal("947.00")),
                        new InvoiceTotalsBlock.Row("VAT (20%)", new BigDecimal("189.40"))),
                        "TOTAL DUE", new BigDecimal("1136.40")))
                .payment(new InvoicePaymentBlock("PAYMENT DETAILS", List.of(
                        new InvoicePaymentBlock.Field("Beneficiary", "Halstead Software Ltd"),
                        new InvoicePaymentBlock.Field("Bank", "Northgate Bank"),
                        new InvoicePaymentBlock.Field("Sort Code", "40-05-30"),
                        new InvoicePaymentBlock.Field("Account Number", "12345678"),
                        new InvoicePaymentBlock.Field("Reference", "HS-INV-2026-0417")),
                        "Please include the invoice number as the payment reference"
                                + " to ensure timely allocation.",
                        "Payment is due by 10 September 2026. If payment is not received"
                                + " by the due date, late fees may apply.\n"
                                + "For any queries, please contact billing@halstead.example.",
                        "", "", "Thank you for your business."))
                .notes(new InvoiceNotesBlock("NOTES",
                        List.of("This invoice covers software subscriptions and hosted"
                                + " services for the current billing period."),
                        "", ""))
                .currencyCode("GBP")
                .build();
    }

    private static List<InvoiceServiceLines.Line> lines() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Halstead Workspace Premium (10 seats)", "10", "18.00", "180.00"));
        lines.add(line(2, "Hosted Platform Services", "1", "420.00", "420.00"));
        lines.add(line(3, "Analytics Pro Licences (5 seats)", "5", "8.40", "42.00"));
        lines.add(line(4, "Priority Support Plan", "1", "275.00", "275.00"));
        lines.add(line(5, "Voice Add-on (5 seats)", "5", "6.00", "30.00"));
        return lines;
    }

    private static InvoiceServiceLines.Line line(int number, String title, String quantity,
                                                 String unitPrice, String amount) {
        return new InvoiceServiceLines.Line(number, title, "", "",
                new BigDecimal(quantity), "", new BigDecimal(unitPrice),
                new BigDecimal(amount), "20%", "", "");
    }
}
