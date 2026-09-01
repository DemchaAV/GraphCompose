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
 * The one invoice the Obsidian preset's tests render.
 *
 * <p>Both gates read it: the exact layout snapshot and the pixel baseline. One
 * fixture behind both is what makes the two comparable — a shift the snapshot
 * catches is a shift in the same document the baseline was recorded from.</p>
 *
 * <p>Its lines are priced so the tax the preset works out is a round figure per
 * line, because that column is derived rather than stated and a fixture that
 * hid the arithmetic would not exercise it.</p>
 */
final class ObsidianInvoiceFixtures {

    private ObsidianInvoiceFixtures() {
    }

    /** A month of business-account services, billed to one customer. */
    static StructuredInvoiceData invoice() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "Kestrel", "", "", "K", ""))
                .supplier(new InvoiceContactBlock(
                        "Kestrel Business",
                        List.of("7 Harbourside Walk", "Bristol BS1 5TY", "United Kingdom"),
                        "", "ar@kestrel.example", "", "", "", "", ""))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice number", "INV-2026-1048", false),
                        new InvoiceMasthead.Entry("Issue date", "27 August 2026", false),
                        new InvoiceMasthead.Entry("Due date", "10 September 2026", true),
                        new InvoiceMasthead.Entry("Payment terms", "14 days", false),
                        new InvoiceMasthead.Entry("Currency", "GBP", false))))
                .billTo(new InvoiceRecipient("Bill to", "Northline Consulting Ltd", "",
                        List.of("21 Jubilee Way", "London SE1 3SS", "United Kingdom"),
                        "", "", "", ""))
                .serviceLines(new InvoiceServiceLines(
                        new InvoiceServiceLines.Columns("#", "Description", "",
                                "Qty", "Unit price", "Amount", "VAT (20%)"),
                        lines()))
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("Subtotal", new BigDecimal("730.00")),
                        new InvoiceTotalsBlock.Row("VAT (20%)", new BigDecimal("146.00"))),
                        "Total due", new BigDecimal("876.00")))
                .payment(new InvoicePaymentBlock("Payment details", List.of(
                        new InvoicePaymentBlock.Field("Payee", "Kestrel Business"),
                        new InvoicePaymentBlock.Field("Sort code", "04-00-75"),
                        new InvoicePaymentBlock.Field("Account number", "12345678"),
                        new InvoicePaymentBlock.Field("Reference", "INV-2026-1048")),
                        "Please pay by bank transfer\nto the account below.",
                        "Payment is due by 10 September 2026.",
                        "10 September 2026", "", "Thank you for your business."))
                .notes(new InvoiceNotesBlock("Notes", List.of(
                        "Thank you for choosing Kestrel Business.\n"
                                + "This invoice covers services and features provided\n"
                                + "to your business as of the date of issue.",
                        "If you have any questions about this invoice,\n"
                                + "please contact us at ar@kestrel.example."),
                        "", ""))
                .currencyCode("GBP")
                .build();
    }

    private static List<InvoiceServiceLines.Line> lines() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Subscription setup", "One-off account and workspace setup",
                "150.00", "180.00"));
        lines.add(line(2, "International payment support",
                "Monthly support for cross-border payments", "250.00", "300.00"));
        lines.add(line(3, "Expense management integration", "API access and integration",
                "200.00", "240.00"));
        lines.add(line(4, "Virtual cards", "50 virtual cards", "100.00", "120.00"));
        lines.add(line(5, "Business account maintenance", "Monthly account maintenance fee",
                "30.00", "36.00"));
        return lines;
    }

    private static InvoiceServiceLines.Line line(int number, String title, String subtitle,
                                                 String unitPrice, String amount) {
        return new InvoiceServiceLines.Line(number, title, subtitle, "",
                BigDecimal.ONE, "", new BigDecimal(unitPrice),
                new BigDecimal(amount), "", "", "");
    }
}
