package com.demcha.compose.document.templates.data.receipt;

import java.util.function.Consumer;

/**
 * Public compose-first payment receipt input.
 *
 * <p><b>Authoring role:</b> gives callers one document-level object to pass
 * to receipt templates, the same way {@code InvoiceDocumentSpec} does for
 * invoices.</p>
 *
 * <p>Unlike the invoice spec this one does not re-declare the payload's
 * builder methods. A receipt spec carries exactly one payload and
 * {@link ReceiptData#builder()} is already its fluent surface, so mirroring
 * twenty setters here would add a second place to keep in step for no
 * additional reach — {@link #of(Consumer)} gives the same one-expression
 * authoring.</p>
 *
 * @param receipt normalized receipt content rendered by receipt templates
 */
public record ReceiptDocumentSpec(ReceiptData receipt) {

    /**
     * Creates a normalized receipt document spec.
     */
    public ReceiptDocumentSpec {
        receipt = receipt == null ? ReceiptData.builder().build() : receipt;
    }

    /**
     * Wraps existing receipt data in the document-level spec expected by
     * receipt templates.
     *
     * @param receipt receipt data
     * @return document spec
     */
    public static ReceiptDocumentSpec from(ReceiptData receipt) {
        return new ReceiptDocumentSpec(receipt);
    }

    /**
     * Builds receipt data inline and wraps it.
     *
     * @param spec receipt data builder callback
     * @return document spec
     */
    public static ReceiptDocumentSpec of(Consumer<ReceiptData.Builder> spec) {
        ReceiptData.Builder builder = ReceiptData.builder();
        if (spec != null) {
            spec.accept(builder);
        }
        return new ReceiptDocumentSpec(builder.build());
    }
}
