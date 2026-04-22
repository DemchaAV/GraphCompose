package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;

/**
 * Canonical compose contract for reusable invoice templates.
 *
 * <p><b>Responsibility:</b> define one invoice scene that translates business
 * data into semantic sections, rows, and totals inside a live
 * {@link DocumentSession}.</p>
 *
 * <pre>{@code
 * InvoiceTemplate template = new InvoiceTemplateV1();
 *
 * try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf")).create()) {
 *     template.compose(document, invoiceData);
 *     document.buildPdf();
 * }
 * }</pre>
 */
public interface InvoiceTemplate {

    /**
     * Stable public template identifier.
     *
     * @return unique template id used by registries and integrations
     */
    String getTemplateId();

    /**
     * Human-readable display name.
     *
     * @return template display name
     */
    String getTemplateName();

    /**
     * Optional human-readable description.
     *
     * @return template description, or an empty string when omitted
     */
    default String getDescription() {
        return "";
    }

    /**
     * Composes an invoice into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param data invoice document data
     * @throws NullPointerException if an implementation requires non-null inputs
     */
    void compose(DocumentSession document, InvoiceData data);
}
