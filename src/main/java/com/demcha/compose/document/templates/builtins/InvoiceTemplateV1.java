package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.support.business.BusinessDocumentSceneStyles;
import com.demcha.compose.document.templates.support.business.InvoiceTemplateComposer;
import com.demcha.compose.document.templates.support.common.SessionTemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;

/**
 * Canonical V2 implementation of the invoice template.
 */
public final class InvoiceTemplateV1 implements InvoiceTemplate {
    private final InvoiceTemplateComposer composer = new InvoiceTemplateComposer(new BusinessDocumentSceneStyles());

    /**
     * Creates the canonical invoice template.
     */
    public InvoiceTemplateV1() {
    }

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
    public void compose(DocumentSession document, InvoiceDocumentSpec spec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), spec);
        try {
            composer.compose(new SessionTemplateComposeTarget(document), spec);
            TemplateLifecycleLog.success(getTemplateId(), spec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), spec, startNanos, ex);
            throw ex;
        }
    }
}
