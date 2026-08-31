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
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared fixture data for the {@link LumaStudioInvoice} gates — the SAME
 * specs feed the pixel parity test and the layout snapshot test, so a
 * geometry shift the pixel budget absorbs still trips the exact snapshot,
 * and vice versa.
 */
final class LumaStudioInvoiceFixtures {

    /** The break the sheet stacks its multi-line notes on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private LumaStudioInvoiceFixtures() {
    }

    /**
     * Canonical studio invoice — five priced service lines on one page,
     * exercising the sidebar lockup, both party blocks, the VAT column, the
     * total-due band, the notes and payment pair, and the sign-off band.
     */
    static StructuredInvoiceDocumentSpec canonicalInvoice() {
        return StructuredInvoiceDocumentSpec.from(base().build());
    }

    /**
     * Overflow studio invoice — the same sheet billed phase by phase, so the
     * table paginates over three pages and the repeated header, the per-page
     * chrome, a middle page carrying nothing but table, and the whole-block
     * closing all become part of the frozen contract. No manual page breaks.
     */
    static StructuredInvoiceDocumentSpec overflowInvoice() {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        for (int phase = 1; phase <= 4; phase++) {
            for (InvoiceServiceLines.Line line : serviceLines().lines()) {
                lines.add(new InvoiceServiceLines.Line(lines.size() + 1, line.title(),
                        line.description() + " Phase " + phase + ".", line.servicePeriod(),
                        line.quantity(), line.unit(), line.unitPrice(), line.amount(),
                        line.vatRate()));
            }
        }
        return StructuredInvoiceDocumentSpec.from(base()
                .serviceLines(new InvoiceServiceLines(serviceLines().columns(), lines))
                .build());
    }

    private static StructuredInvoiceData.Builder base() {
        return StructuredInvoiceData.builder()
                .brand(new InvoiceBrand(null, "LUMA & CO. STUDIO", null,
                        "Branding. Design. Digital.", "L", "&Co."))
                .supplier(new InvoiceContactBlock("LUMA & CO. STUDIO",
                        List.of("Studio 3.02, The Loom", "14 Gower Street",
                                "London WC1E 6BT", "United Kingdom"),
                        "+44 (0)20 7946 0832", "hello@lumaandco.studio",
                        "www.lumaandco.studio",
                        "Company No.", "12578934", "VAT No.", "369 4567 89"))
                .masthead(new InvoiceMasthead("INVOICE", List.of(
                        new InvoiceMasthead.Entry("INVOICE NO.", "INV-2024-0587", false),
                        new InvoiceMasthead.Entry("ISSUE DATE", "20 May 2024", false),
                        new InvoiceMasthead.Entry("DUE DATE", "19 June 2024", false),
                        new InvoiceMasthead.Entry("PAYMENT TERMS", "30 Days", false),
                        new InvoiceMasthead.Entry("CURRENCY", "GBP", false))))
                .billTo(new InvoiceRecipient("BILL TO", "Northfield Consulting Ltd",
                        "Attn: Sarah Mitchell",
                        List.of("21 Jubilee Way", "London SE1 3SS", "United Kingdom"), "", ""))
                .shipTo(new InvoiceRecipient("SHIP TO", "Northfield Consulting Ltd", "",
                        List.of("The Foundry, 2nd Floor", "17-19 Great Suffolk Street",
                                "London SE1 0NS", "United Kingdom"), "", ""))
                .serviceLines(serviceLines())
                .totals(new InvoiceTotalsBlock(List.of(
                        new InvoiceTotalsBlock.Row("SUBTOTAL", new BigDecimal("8500.00")),
                        new InvoiceTotalsBlock.Row("VAT (20%)", new BigDecimal("1700.00"))),
                        "TOTAL DUE", new BigDecimal("10200.00")))
                .notes(new InvoiceNotesBlock("NOTES", List.of(
                        "Thank you for your business." + NEWLINE
                                + "If you have any questions regarding" + NEWLINE
                                + "this invoice, please get in touch.",
                        "All work remains the intellectual property" + NEWLINE
                                + "of LUMA & CO. STUDIO until payment" + NEWLINE
                                + "has been received in full."), "", ""))
                .payment(new InvoicePaymentBlock("PAYMENT DETAILS", List.of(
                        new InvoicePaymentBlock.Field("BANK", "Starling Bank"),
                        new InvoicePaymentBlock.Field("SORT CODE", "60-83-71"),
                        new InvoicePaymentBlock.Field("ACCOUNT NO.", "98765432"),
                        new InvoicePaymentBlock.Field("IBAN", "GB36 SRLG 6083 7198 7654 32"),
                        new InvoicePaymentBlock.Field("BIC", "SRLGGB2L")),
                        "Please make payment by bank transfer to:",
                        "Payment is due by 19 June 2024.", "",
                        "LUMA & CO. STUDIO LTD",
                        "Thank you for choosing LUMA & CO. STUDIO."))
                .currencyCode("GBP");
    }

    private static InvoiceServiceLines serviceLines() {
        return new InvoiceServiceLines(
                new InvoiceServiceLines.Columns("", "DESCRIPTION", "", "QTY", "UNIT PRICE",
                        "AMOUNT", "VAT"),
                List.of(
                        line(1, "Brand Strategy Workshop",
                                "Discovery session, research & brand positioning.", "1200.00"),
                        line(2, "Visual Identity Design",
                                "Logo suite, colour palette, typography & guidelines.", "2650.00"),
                        line(3, "Website Design (Up to 8 pages)",
                                "UX/UI design for desktop and mobile.", "3200.00"),
                        line(4, "Copywriting", "Website copy & key messaging.", "850.00"),
                        line(5, "Project Management",
                                "Planning, coordination & client liaison.", "600.00")));
    }

    private static InvoiceServiceLines.Line line(int number, String title, String description,
                                                 String price) {
        return new InvoiceServiceLines.Line(number, title, description, "", BigDecimal.ONE, "",
                new BigDecimal(price), new BigDecimal(price), "20%");
    }
}
