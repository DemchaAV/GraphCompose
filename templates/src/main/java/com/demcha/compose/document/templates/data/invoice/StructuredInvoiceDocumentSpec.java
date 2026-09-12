package com.demcha.compose.document.templates.data.invoice;

/**
 * Public compose-first input for structured invoice templates.
 *
 * <p><b>Authoring role:</b> the document-level object structured invoice
 * presets are parameterised on, the way the display-oriented presets are
 * parameterised on {@link InvoiceDocumentSpec}. Presets that render the
 * structured shape — brand lockup, labelled masthead metadata, priced
 * service lines with a numeric totals stack, bank payment fields —
 * consume this spec; presets that render pre-formatted display strings
 * stay on the other one.</p>
 *
 * @param invoice normalized structured invoice content
 */
public record StructuredInvoiceDocumentSpec(StructuredInvoiceData invoice) {

    /**
     * Creates a normalized structured invoice document spec.
     */
    public StructuredInvoiceDocumentSpec {
        invoice = invoice == null ? StructuredInvoiceData.builder().build() : invoice;
    }

    /**
     * Wraps existing structured invoice data in the document-level spec
     * expected by structured invoice templates.
     *
     * @param invoice structured invoice data
     * @return document spec
     */
    public static StructuredInvoiceDocumentSpec from(StructuredInvoiceData invoice) {
        return new StructuredInvoiceDocumentSpec(invoice);
    }
}
