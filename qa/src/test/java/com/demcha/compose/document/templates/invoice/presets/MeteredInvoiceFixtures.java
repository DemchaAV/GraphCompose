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
 * The one invoice the Metered preset's tests render.
 *
 * <p>Both gates read it: the exact layout snapshot and the pixel baseline. One
 * fixture behind both is what makes the two comparable — a shift the snapshot
 * catches is a shift in the same document the baseline was recorded from.</p>
 */
final class MeteredInvoiceFixtures {

    private MeteredInvoiceFixtures() {
    }

    /** A month of metered platform usage, billed to one customer. */
    static StructuredInvoiceData invoice() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "Northwind Cloud", "", "", "", ""))
                .supplier(new InvoiceContactBlock(
                        "Northwind Cloud Services Ltd.",
                        List.of("120 Riverside Way", "Bristol, BS1 6QT", "United Kingdom"),
                        "+44 (0)117 496 2280",
                        "billing@northwind.example",
                        "https://northwind.example",
                        "", "",
                        "VAT ID:", "GB 412 8830 27",
                        "Northwind Cloud Services Ltd. is a subsidiary of Northwind Group plc."))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice Number:", "INV-2026-00041882", true),
                        new InvoiceMasthead.Entry("Invoice Date:", "31 July 2026", false),
                        new InvoiceMasthead.Entry("Due Date:", "30 August 2026", false),
                        new InvoiceMasthead.Entry("Bill Period:", "01 Jul 2026 – 31 Jul 2026", false),
                        new InvoiceMasthead.Entry("Payment Terms:", "Net 30 Days", false),
                        new InvoiceMasthead.Entry("Currency:", "GBP", false))))
                .billTo(new InvoiceRecipient("BILL TO", "Bright Future Ltd.", "",
                        List.of("45 King Street", "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "VAT ID:", "GB 987 6543 21"))
                .shipTo(new InvoiceRecipient("SHIP TO", "Bright Future Ltd.", "",
                        List.of("Warehouse 7", "North Industrial Estate",
                                "Manchester, M2 4WU", "United Kingdom"),
                        "", "", "", ""))
                .serviceLines(new InvoiceServiceLines(
                        new InvoiceServiceLines.Columns("", "DESCRIPTION", "SERVICE",
                                "QTY", "UNIT PRICE", "AMOUNT"),
                        lines()))
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("Subtotal", new BigDecimal("214.71")),
                        new InvoiceTotalsBlock.Row("VAT (20%)", new BigDecimal("42.94"))),
                        "TOTAL DUE", new BigDecimal("257.65")))
                .payment(new InvoicePaymentBlock("PAYMENT DETAILS", List.of(
                        new InvoicePaymentBlock.Field("Account Name:",
                                "Northwind Cloud Services Ltd."),
                        new InvoicePaymentBlock.Field("Bank Name:", "Riverbank plc"),
                        new InvoicePaymentBlock.Field("Account Number:", "60415523"),
                        new InvoicePaymentBlock.Field("Sort Code:", "20-41-08"),
                        new InvoicePaymentBlock.Field("IBAN:", "GB29 RIVB 2041 0860 4155 23"),
                        new InvoicePaymentBlock.Field("Account Type:", "Business Current"),
                        new InvoicePaymentBlock.Field("Remittance Email:",
                                "billing@northwind.example")),
                        "", "PAYMENT DUE BY", "30 August 2026", "", ""))
                .notes(new InvoiceNotesBlock(
                        "Thank you for running on Northwind Cloud.",
                        List.of("For billing and account support, visit"
                                + " https://northwind.example or write to"
                                + " billing@northwind.example."),
                        "billing@northwind.example", ""))
                .currencyCode("GBP")
                .build();
    }

    private static List<InvoiceServiceLines.Line> lines() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Compute Instances", "On-demand usage", "London (eu-west)",
                "744", "Hrs", "0.0710", "52.82", "compute"));
        lines.add(line(2, "Object Storage", "Stored volume", "London (eu-west)",
                "480", "GB", "0.0215", "10.32", "storage"));
        lines.add(line(3, "Managed Database", "Primary and replica", "London (eu-west)",
                "744", "Hrs", "0.1650", "122.76", "database"));
        lines.add(line(4, "Egress Traffic", "Data transfer out", "Global",
                "310", "GB", "0.0900", "27.90", "transfer"));
        lines.add(line(5, "Support Plan", "Standard response", "Global",
                "1", "", "0.91", "0.91", "support"));
        return lines;
    }

    private static InvoiceServiceLines.Line line(int number, String title, String description,
                                                 String service, String quantity, String unit,
                                                 String unitPrice, String amount, String icon) {
        return new InvoiceServiceLines.Line(number, title, description, service,
                new BigDecimal(quantity), unit, new BigDecimal(unitPrice),
                new BigDecimal(amount), "", icon);
    }
}
