package com.demcha.templates.builtins;

import com.demcha.compose.document.templates.support.InvoiceTemplateComposer;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.SessionTemplateComposeTarget;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.api.InvoiceTemplate;
import com.demcha.templates.data.InvoiceData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

/**
 * Deprecated bridge to the canonical invoice template.
 */
@Deprecated(forRemoval = false)
public class InvoiceTemplateV1 extends PdfTemplateAdapterSupport implements InvoiceTemplate {
    private final InvoiceTemplateComposer composer = new InvoiceTemplateComposer(
            new com.demcha.compose.document.templates.support.BusinessDocumentSceneStyles());
    private final InvoiceSceneBuilder legacySceneBuilder = new InvoiceSceneBuilder(new BusinessDocumentSceneStyles());

    @Override
    public void compose(DocumentComposer composer, InvoiceData data) {
        legacySceneBuilder.compose(composer, data);
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

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(InvoiceData data) {
        return render(data, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(InvoiceData data, boolean guideLines) {
        return renderToDocumentSession(
                guideLines,
                "Failed to generate invoice",
                PDRectangle.A4,
                22,
                22,
                22,
                22,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(InvoiceData data, Path path) {
        render(data, path, false);
    }

    @Deprecated(forRemoval = false)
    @Override
    public void render(InvoiceData data, Path path, boolean guideLines) {
        renderToFileSession(
                path,
                guideLines,
                "Failed to generate invoice",
                "Invoice saved to {}",
                PDRectangle.A4,
                22,
                22,
                22,
                22,
                session -> composer.compose(
                        new SessionTemplateComposeTarget(session),
                        LegacyTemplateMappers.toCanonical(data)));
    }
}
