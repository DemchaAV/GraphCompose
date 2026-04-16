package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.data.InvoiceData;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.LegacyTemplateSessionRenderer;

/**
 * Canonical V2 implementation of the invoice template.
 */
public final class InvoiceTemplateV1 implements InvoiceTemplate {
    private final com.demcha.templates.builtins.InvoiceTemplateV1 legacyBridge = new com.demcha.templates.builtins.InvoiceTemplateV1();

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
        LegacyTemplateSessionRenderer.renderInto(document, composer -> legacyBridge.compose(
                composer,
                LegacyTemplateMappers.toLegacy(data)));
    }
}
