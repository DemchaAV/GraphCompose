package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.support.business.BusinessDocumentSceneStyles;
import com.demcha.compose.document.templates.support.business.InvoiceTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;

/**
 * Canonical V2 implementation of the invoice template.
 */
public final class InvoiceTemplateV1 implements InvoiceTemplate {
    private final InvoiceTemplateComposer composer = new InvoiceTemplateComposer(new BusinessDocumentSceneStyles());

    @Override
    public String getTemplateId() {
        return "invoice-v1";
    }

    @Override
    public String getTemplateName() {
        return "Invoice V1";
    }

    @Override
    public String getDescription() {
        return "A light business invoice template with metadata, billing parties, line items, and totals.";
    }

    @Override
    public void compose(DocumentSession document, InvoiceData data) {
        composer.compose(new SessionTemplateComposeTarget(document), data);
    }
}
