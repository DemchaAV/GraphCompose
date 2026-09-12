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
 * The one invoice the Platform preset's tests render.
 *
 * <p>Both gates read it: the exact layout snapshot and the pixel baseline. One
 * fixture behind both is what makes the two comparable — a shift the snapshot
 * catches is a shift in the same document the baseline was recorded from.</p>
 *
 * <p>It bills across two regions on purpose, because that is the column this
 * design has and the others do not.</p>
 */
final class PlatformInvoiceFixtures {

    private PlatformInvoiceFixtures() {
    }

    /** A month of platform usage, billed across two regions to one customer. */
    static StructuredInvoiceData invoice() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "Aurora Cloud", "", "", "", ""))
                .supplier(new InvoiceContactBlock(
                        "Aurora Cloud Inc.",
                        List.of("500 Harbour Street", "Vancouver, BC V6C 2W2", "Canada"),
                        "+1 604-555-0180",
                        "billing@aurora.example",
                        "aurora.example",
                        "", "",
                        "VAT ID:", "CA BN 84213 7755"))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice Number:", "INV-2026-07-31-9F4K2M", true),
                        new InvoiceMasthead.Entry("Invoice Date:", "31 July 2026", false),
                        new InvoiceMasthead.Entry("Due Date:", "30 August 2026", false),
                        new InvoiceMasthead.Entry("Billing Account ID:", "7C2D9E-B1F4A8-3K6M0P", true),
                        new InvoiceMasthead.Entry("Billing Period:", "01 Jul 2026 – 31 Jul 2026", false),
                        new InvoiceMasthead.Entry("Payment Terms:", "Net 30 Days", false),
                        new InvoiceMasthead.Entry("Currency:", "CAD", false))))
                .billTo(new InvoiceRecipient("BILL TO", "Bright Future Ltd.", "",
                        List.of("45 King Street", "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "VAT ID:", "GB 987 6543 21"))
                .shipTo(new InvoiceRecipient("SHIP TO", "Bright Future Ltd.", "",
                        List.of("Warehouse 7", "North Industrial Estate",
                                "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "", ""))
                .serviceLines(new InvoiceServiceLines(
                        new InvoiceServiceLines.Columns("", "DESCRIPTION", "SERVICE",
                                "USAGE", "UNIT PRICE", "AMOUNT", "", "REGION"),
                        lines()))
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("Subtotal", new BigDecimal("186.44")),
                        new InvoiceTotalsBlock.Row("Tax (5%)", new BigDecimal("9.32"))),
                        "TOTAL DUE", new BigDecimal("195.76")))
                .payment(new InvoicePaymentBlock("PAYMENT INFORMATION", List.of(
                        new InvoicePaymentBlock.Field("Bank Name:", "Harbourbank of Canada"),
                        new InvoicePaymentBlock.Field("Account Name:", "Aurora Cloud Inc."),
                        new InvoicePaymentBlock.Field("Account Number:", "417 802 336"),
                        new InvoicePaymentBlock.Field("SWIFT Code:", "HBCACAT1"),
                        new InvoicePaymentBlock.Field("Transit Number:", "00812"),
                        new InvoicePaymentBlock.Field("Institution Number:", "049"),
                        new InvoicePaymentBlock.Field("Account Type:", "Business Chequing"),
                        new InvoicePaymentBlock.Field("Remittance Email:",
                                "billing@aurora.example")),
                        "Include your Billing Account ID\non your payment remittance.",
                        "DUE DATE", "30 August 2026", "", ""))
                .notes(new InvoiceNotesBlock(
                        "Thank you for running on Aurora Cloud.",
                        List.of("Manage your account at aurora.example"
                                + " or write to billing@aurora.example."),
                        "billing@aurora.example", ""))
                .currencyCode("CAD")
                .build();
    }

    private static List<InvoiceServiceLines.Line> lines() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Compute Instances", "N2 Standard VM", "Compute", "ca-central1",
                "744", "Hours", "0.0685", "50.96", "compute"));
        lines.add(line(2, "Object Storage", "Standard class", "Storage", "ca-central1",
                "480", "GB", "0.0210", "10.08", "storage"));
        lines.add(line(3, "Managed Database", "PostgreSQL instance", "Database", "ca-central1",
                "744", "Hours", "0.1385", "103.04", "database"));
        lines.add(line(4, "Egress Traffic", "Data transfer out", "Network", "Global",
                "260", "GB", "0.0860", "22.36", "network"));
        lines.add(line(5, "Support Plan", "Standard response", "Support", "Global",
                "1", "", "0.00", "0.00", "support"));
        return lines;
    }

    private static InvoiceServiceLines.Line line(int number, String title, String description,
                                                 String service, String region, String quantity,
                                                 String unit, String unitPrice, String amount,
                                                 String icon) {
        return new InvoiceServiceLines.Line(number, title, description, service,
                new BigDecimal(quantity), unit, new BigDecimal(unitPrice),
                new BigDecimal(amount), "", icon, region);
    }
}
