package com.demcha.compose.document.templates.invoice.presets;

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
 * Shared fixture data for the {@link WorkspaceInvoice} gates — the SAME
 * document feeds the pixel parity test and the layout snapshot test, so a
 * geometry shift the pixel budget absorbs still trips the exact snapshot, and
 * vice versa.
 */
final class WorkspaceInvoiceFixtures {

    private WorkspaceInvoiceFixtures() {
    }

    /**
     * The canonical one-page invoice — six metadata rows, two addressed parties
     * each printing a registration, four marked service lines, eight payment
     * fields, two summed rows and a closing line with an address inside it.
     *
     * @return the document
     */
    static StructuredInvoiceData canonicalInvoice() {
        return new StructuredInvoiceData(
                brand(), supplier(), masthead(), billTo(), shipTo(),
                new InvoiceSummaryBlock("", "", ""),
                serviceLines(), totals(), payment(), notes(), "USD");
    }

    /** No logo: the wordmark is what a document without one falls back to. */
    static InvoiceBrand brand() {
        return new InvoiceBrand(null, "kestrel", "", "", "", "");
    }

    static InvoiceContactBlock supplier() {
        return new InvoiceContactBlock(
                "Kestrel Collaboration, Inc.",
                List.of("500 Howard Street, 6th Floor", "San Francisco, CA 94105",
                        "United States"),
                "", "", "kestrel.example",
                "", "",
                "VAT ID:", "US 77-0560185");
    }

    static InvoiceMasthead masthead() {
        List<InvoiceMasthead.Entry> entries = new ArrayList<>();
        entries.add(new InvoiceMasthead.Entry("Invoice Number:", "INV-2024-0003521", true));
        entries.add(new InvoiceMasthead.Entry("Invoice Date:", "27 May 2024", false));
        entries.add(new InvoiceMasthead.Entry("Billing Period:", "1 - 31 May 2024", false));
        entries.add(new InvoiceMasthead.Entry("Workspace ID:", "T04FQ9L2M", false));
        entries.add(new InvoiceMasthead.Entry("Payment Terms:", "Net 30", false));
        entries.add(new InvoiceMasthead.Entry("Due Date:", "26 June 2024", false));
        return new InvoiceMasthead("INVOICE", entries);
    }

    /** The registration is the labelled pair printed under the address. */
    static InvoiceRecipient billTo() {
        return new InvoiceRecipient("BILL TO", "Bright Future Ltd.", "",
                List.of("45 King Street", "Manchester M2 7AZ", "United Kingdom"),
                "", "", "VAT ID:", "GB 987 6543 21");
    }

    static InvoiceRecipient shipTo() {
        return new InvoiceRecipient("SHIP TO", "Bright Future Ltd.", "",
                List.of("Unit 4, Riverside Park", "Leeds LS1 4AP", "United Kingdom"),
                "", "", "", "");
    }

    static InvoiceServiceLines serviceLines() {
        // The currency is not in the labels: the preset names it once per money
        // column, so a document states the code and nothing else.
        InvoiceServiceLines.Columns columns = new InvoiceServiceLines.Columns(
                "", "DESCRIPTION", "PLAN / SERVICE", "QTY", "UNIT PRICE", "AMOUNT", "");
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        lines.add(line(1, "Kestrel Business+", "Workspace subscription",
                "Business+ Annual", "50", "Users", "8.75", "437.50", "grid"));
        lines.add(line(2, "Enterprise Key Management", "Customer-managed encryption keys",
                "Add-on", "1", "", "100.00", "100.00", "search"));
        lines.add(line(3, "Priority Support", "Named engineer and response target",
                "Standard", "1", "", "50.00", "50.00", "shield"));
        lines.add(line(4, "Workflow Automation", "Templates and scheduled runs",
                "Included", "1", "", "0.00", "0.00", "grid"));
        return new InvoiceServiceLines(columns, lines);
    }

    private static InvoiceServiceLines.Line line(int number, String title, String description,
                                                 String plan, String quantity, String unit,
                                                 String unitPrice, String amount, String icon) {
        return new InvoiceServiceLines.Line(number, title, description, plan,
                new BigDecimal(quantity), unit, decimal(unitPrice), decimal(amount), "", icon);
    }

    static InvoiceTotalsBlock totals() {
        List<InvoiceTotalsBlock.Row> rows = new ArrayList<>();
        rows.add(new InvoiceTotalsBlock.Row("Subtotal", decimal("587.50")));
        rows.add(new InvoiceTotalsBlock.Row("Tax (0%)", decimal("0.00")));
        return new InvoiceTotalsBlock(rows, "TOTAL DUE", decimal("587.50"));
    }

    static InvoicePaymentBlock payment() {
        List<InvoicePaymentBlock.Field> fields = new ArrayList<>();
        fields.add(new InvoicePaymentBlock.Field("Bank Name:", "Bay Union Bank, N.A."));
        fields.add(new InvoicePaymentBlock.Field("Account Name:", "Kestrel Collaboration, Inc."));
        fields.add(new InvoicePaymentBlock.Field("Account Number:", "000000123456789"));
        fields.add(new InvoicePaymentBlock.Field("Routing (ABA):", "021000021"));
        fields.add(new InvoicePaymentBlock.Field("SWIFT / BIC:", "BAYUUS33"));
        fields.add(new InvoicePaymentBlock.Field("Currency:", "USD"));
        fields.add(new InvoicePaymentBlock.Field("Reference:", "INV-2024-0003521"));
        fields.add(new InvoicePaymentBlock.Field("Bank Address:", "1 Market Plaza, San Francisco"));
        return new InvoicePaymentBlock("PAYMENT DETAILS", fields,
                "Please include the invoice number in your payment reference.",
                "PAYMENT DUE BY",
                "26 June 2024",
                "", "");
    }

    static InvoiceNotesBlock notes() {
        return new InvoiceNotesBlock("Thank you for using Kestrel.",
                List.of("Questions about this invoice? Reach us at billing@kestrel.example "
                        + "and we will get back to you within one business day."),
                "billing@kestrel.example", "");
    }

    private static BigDecimal decimal(String printed) {
        return new BigDecimal(printed.replace(",", ""));
    }
}
