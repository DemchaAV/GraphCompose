package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.InvoiceData;

/**
 * Canonical compose contract for reusable invoice templates.
 */
public interface InvoiceTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    /**
     * Composes an invoice into a live document session.
     *
     * @param document active mutable document session receiving template nodes
     * @param data invoice document data
     */
    void compose(DocumentSession document, InvoiceData data);
}
