package com.demcha.examples.support;

import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared sample data for the Payments invoice example.
 *
 * <p>Kept in lockstep with the qa module's {@code PaymentsInvoiceFixtures} — the
 * two modules cannot share a source file, so a content change here belongs
 * there too.</p>
 */
public final class PaymentsInvoiceSampleData {

    private PaymentsInvoiceSampleData() {
    }

    /**
     * The sample invoice — seven metadata rows, two addressed parties, six
     * marked service lines, seven payment fields, two summed rows and a
     * two-line note.
     *
     * @return the document the example renders
     */
    public static StructuredInvoiceData sample() {
        return new StructuredInvoiceData(
                brand(), supplier(), masthead(), billTo(), shipTo(),
                new InvoiceSummaryBlock("", "", ""),
                serviceLines(), totals(), payment(), notes(), "GBP");
    }

    /** No logo: the wordmark is what a document without one falls back to. */
    private static InvoiceBrand brand() {
        return new InvoiceBrand(null, "Meridian", "", "", "", "");
    }

    private static InvoiceContactBlock supplier() {
        return new InvoiceContactBlock(
                "Meridian Payments Ltd.",
                List.of("1 Harbour Exchange", "Canary Wharf", "London E14 9GE",
                        "United Kingdom"),
                "", "", "",
                "", "Company No. 09048900",
                "", "VAT No. GB 123 4567 89");
    }

    private static InvoiceMasthead masthead() {
        List<InvoiceMasthead.Entry> entries = new ArrayList<>();
        entries.add(new InvoiceMasthead.Entry("Invoice Number", "INV-2025-05-10472", false));
        entries.add(new InvoiceMasthead.Entry("Invoice Date", "10 May 2025", false));
        entries.add(new InvoiceMasthead.Entry("Billing Period", "1 - 30 April 2025", false));
        entries.add(new InvoiceMasthead.Entry("Account ID", "acct_4Q8xTb2mKp", false));
        entries.add(new InvoiceMasthead.Entry("Payment Terms", "Net 30", false));
        entries.add(new InvoiceMasthead.Entry("Due Date", "14 June 2025", false));
        entries.add(new InvoiceMasthead.Entry("Status", "UNPAID", true));
        return new InvoiceMasthead("INVOICE", entries);
    }

    /** The subline is where a billed party's registration goes on this sheet. */
    private static InvoiceRecipient billTo() {
        return new InvoiceRecipient("BILL TO", "Northwind Ltd.",
                "VAT No. GB 987 6543 21",
                List.of("Attention: Finance Team", "42 Bridgewater Street",
                        "Manchester M15 4QT", "United Kingdom"),
                "", "");
    }

    private static InvoiceRecipient shipTo() {
        return new InvoiceRecipient("SHIP TO", "Northwind Ltd.", "",
                List.of("Operations Centre", "7 Kingsway Park",
                        "Leeds LS12 6BD", "United Kingdom"),
                "", "");
    }

    private static InvoiceServiceLines serviceLines() {
        InvoiceServiceLines.Columns columns = new InvoiceServiceLines.Columns(
                "", "DESCRIPTION", "", "QUANTITY", "UNIT PRICE", "AMOUNT", "VAT");
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Subscription Billing",
                "Subscription management and recurring billing",
                "1", "250.00", "250.00", "20%", "card"));
        lines.add(line(2, "Payment Processing",
                "Online card payments processing (volume tier)",
                "1", "1,180.00", "1,180.00", "20%", "card-settings"));
        lines.add(line(3, "Fraud Screening",
                "Advanced fraud screening and rules",
                "1", "320.00", "320.00", "20%", "shield"));
        lines.add(line(4, "Company Formation",
                "Incorporation and compliance filings",
                "1", "180.00", "180.00", "20%", "globe"));
        lines.add(line(5, "In-Person Terminals",
                "Terminal rental and firmware updates",
                "4", "39.00", "156.00", "20%", "mobile"));
        lines.add(line(6, "Priority Support",
                "Named engineer, one-hour response",
                "1", "100.00", "100.00", "20%", "headset"));
        return new InvoiceServiceLines(columns, lines);
    }

    private static InvoiceServiceLines.Line line(int number, String title, String description,
                                                 String quantity, String unitPrice,
                                                 String amount, String vat, String icon) {
        return new InvoiceServiceLines.Line(number, title, description, "",
                decimal(quantity), "", decimal(unitPrice), decimal(amount), vat, icon);
    }

    private static InvoiceTotalsBlock totals() {
        List<InvoiceTotalsBlock.Row> rows = new ArrayList<>();
        rows.add(new InvoiceTotalsBlock.Row("Subtotal (excl. VAT)", decimal("2,186.00")));
        rows.add(new InvoiceTotalsBlock.Row("VAT at 20%", decimal("437.20")));
        return new InvoiceTotalsBlock(rows, "TOTAL DUE", decimal("2,623.20"));
    }

    private static InvoicePaymentBlock payment() {
        List<InvoicePaymentBlock.Field> fields = new ArrayList<>();
        fields.add(new InvoicePaymentBlock.Field("Bank Name", "Harbour Bank plc"));
        fields.add(new InvoicePaymentBlock.Field("Account Name", "Meridian Payments Ltd."));
        fields.add(new InvoicePaymentBlock.Field("Sort Code", "40-02-50"));
        fields.add(new InvoicePaymentBlock.Field("Account Number", "71295408"));
        fields.add(new InvoicePaymentBlock.Field("IBAN", "GB29 HRBK 4002 5071 2954 08"));
        fields.add(new InvoicePaymentBlock.Field("SWIFT / BIC", "HRBKGB2L"));
        fields.add(new InvoicePaymentBlock.Field("Reference", "INV-2025-05-10472"));
        return new InvoicePaymentBlock("PAYMENT DETAILS", fields,
                "Please include the invoice number as payment reference.",
                "Due by 14 June 2025",
                "Payment due by 14 June 2025",
                "",
                "Questions? We're here to help.");
    }

    private static InvoiceNotesBlock notes() {
        return new InvoiceNotesBlock("NOTES",
                List.of("Thank you for your business. This invoice covers services and "
                                + "fees for the period stated above.",
                        "Late payment may incur interest at the statutory rate."),
                "support@meridianpayments.example",
                "+44 (0) 20 3966 1900");
    }

    /** A printed figure back to a number: the grouping comes off. */
    private static BigDecimal decimal(String printed) {
        return new BigDecimal(printed.replace(",", ""));
    }
}
