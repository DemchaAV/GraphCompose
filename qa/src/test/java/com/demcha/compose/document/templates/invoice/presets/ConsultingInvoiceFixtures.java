package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.image.DocumentImageData;
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
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared fixture data for the Consulting Invoice gates — the SAME spec
 * feeds the smoke, layout-snapshot and pixel gates, so a geometry shift the
 * pixel budget absorbs still trips the exact snapshot, and vice versa.
 *
 * <p>The canonical fixture is the preset's reference content: a
 * professional-services invoice with a brand logo, five masthead metadata
 * rows (one emphasized), five priced service lines, a subtotal / tax
 * stack with the total band, five bank fields, and notes naming both query
 * channels. The overflow fixture repeats the service lines until the table
 * paginates, which is what freezes the repeating table header.</p>
 *
 * <p>Kept in lockstep with the examples module's
 * {@code ConsultingInvoiceSampleData} — the two modules cannot share a
 * source file, so a content change here belongs there too.</p>
 */
final class ConsultingInvoiceFixtures {

    private ConsultingInvoiceFixtures() {
    }

    /** The single-page reference invoice. */
    static StructuredInvoiceDocumentSpec canonicalInvoice() {
        return StructuredInvoiceDocumentSpec.from(baseBuilder(serviceLines(5)).build());
    }

    /** Enough service lines to push the table onto a second page. */
    static StructuredInvoiceDocumentSpec overflowInvoice() {
        return StructuredInvoiceDocumentSpec.from(baseBuilder(serviceLines(26)).build());
    }

    /** The sample logo, read from the qa test resources. */
    static DocumentImageData sampleLogo() {
        String path = "/sample-data/consulting-invoice-logo.png";
        try (InputStream input = ConsultingInvoiceFixtures.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing sample logo: " + path);
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read sample logo: " + path, e);
        }
    }

    private static StructuredInvoiceData.Builder baseBuilder(InvoiceServiceLines lines) {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(sampleLogo(), "NORTHPOINT", "CONSULTING",
                        "Strategy. Solutions. Results."))
                .supplier(new InvoiceContactBlock("Northpoint Consulting Pty Ltd",
                        List.of("Level 8, 1 Collins Street", "Melbourne VIC 3000 Australia"),
                        "+61 3 9876 5432", "hello@northpoint.com.au", "northpoint.com.au",
                        "ABN", "12 345 678 901"))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("Invoice Number:", "INV-2025-0478", false),
                        new InvoiceMasthead.Entry("Issue Date:", "26 May 2025", false),
                        new InvoiceMasthead.Entry("Due Date:", "25 June 2025", true),
                        new InvoiceMasthead.Entry("Project:", "Digital Strategy Engagement", false),
                        new InvoiceMasthead.Entry("PO Number:", "PO-7892", false))))
                .billTo(new InvoiceRecipient("BILLED TO", "Greenfield Industries Ltd.",
                        "Accounts Payable Department",
                        List.of("12 Innovation Drive", "Melbourne VIC 3000", "Australia"),
                        "Email:", "ap@greenfield.com.au"))
                .summary(new InvoiceSummaryBlock("INVOICE SUMMARY",
                        "Professional services rendered in accordance with the statement of "
                                + "work for the period",
                        "1 May 2025 – 25 May 2025."))
                .serviceLines(lines)
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("SUBTOTAL", new BigDecimal("14000.00")),
                        new InvoiceTotalsBlock.Row("GST (10%)", new BigDecimal("1400.00"))),
                        "TOTAL DUE", new BigDecimal("15400.00")))
                .payment(new InvoicePaymentBlock("PAYMENT INFORMATION", List.of(
                        new InvoicePaymentBlock.Field("Bank Name:", "Example Bank"),
                        new InvoicePaymentBlock.Field("Account Name:",
                                "Northpoint Consulting Pty Ltd"),
                        new InvoicePaymentBlock.Field("BSB:", "123-456"),
                        new InvoicePaymentBlock.Field("Account Number:", "12345678"),
                        new InvoicePaymentBlock.Field("Reference:", "INV-2025-0478")),
                        "Please ensure the invoice number is included in the payment reference.",
                        "Payment is due within 30 days from the issue date.", "30 days"))
                .notes(new InvoiceNotesBlock("NOTES", List.of(
                        "Thank you for your business.",
                        "If you have any questions regarding this invoice, please contact us "
                                + "at accounts@northpoint.com.au or +61 3 9876 5432."),
                        "accounts@northpoint.com.au", "+61 3 9876 5432"))
                .currencyCode("AUD");
    }

    private static InvoiceServiceLines serviceLines(int count) {
        String[][] source = {
                {"Strategic Consultation", "Leadership alignment workshops", "1–10 May 2025",
                        "10.00", "hrs", "250.00", "2500.00"},
                {"Market & Competitive Analysis", "Research and analysis report", "1–15 May 2025",
                        "1.00", "ea", "3500.00", "3500.00"},
                {"Digital Roadmap Development", "Strategy and roadmap creation", "5–20 May 2025",
                        "1.00", "ea", "4800.00", "4800.00"},
                {"Stakeholder Review Sessions", "Facilitation and documentation", "15–22 May 2025",
                        "6.00", "hrs", "220.00", "1320.00"},
                {"Presentation & Final Report", "Executive presentation and delivery",
                        "20–25 May 2025", "1.00", "ea", "1880.00", "1880.00"}};
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String[] row = source[index % source.length];
            lines.add(new InvoiceServiceLines.Line(index + 1, row[0], row[1], row[2],
                    new BigDecimal(row[3]), row[4], new BigDecimal(row[5]),
                    new BigDecimal(row[6])));
        }
        return new InvoiceServiceLines(
                new InvoiceServiceLines.Columns("#", "DESCRIPTION", "SERVICE PERIOD", "QTY",
                        "UNIT PRICE", "AMOUNT"),
                lines);
    }
}
