package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.api.InvoiceTemplate;
import com.demcha.templates.data.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

@Slf4j
public class InvoiceTemplateV1 implements InvoiceTemplate {
    private static final float PAGE_MARGIN = 22f;

    private final InvoiceSceneBuilder sceneBuilder;

    public InvoiceTemplateV1() {
        this.sceneBuilder = new InvoiceSceneBuilder(new BusinessDocumentSceneStyles());
    }

    /**
     * Preferred backend-neutral template contract.
     */
    @Override
    public void compose(DocumentComposer composer, InvoiceData data) {
        sceneBuilder.compose(composer, data);
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

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(InvoiceData data) {
        return render(data, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDFBox document output.
     */
    @Deprecated(forRemoval = false)
    @Override
    public PDDocument render(InvoiceData data, boolean guideLines) {
        try {
            PdfComposer composer = createComposer(null, guideLines);
            compose(composer, data);
            return composer.toPDDocument();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate invoice", ex);
        }
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(InvoiceData data, Path path) {
        render(data, path, false);
    }

    /**
     * Deprecated compatibility adapter for direct PDF file writing.
     */
    @Deprecated(forRemoval = false)
    @Override
    public void render(InvoiceData data, Path path, boolean guideLines) {
        try (PdfComposer composer = createComposer(path, guideLines)) {
            compose(composer, data);
            composer.build();
            log.info("Invoice saved to {}", path.toAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate invoice", ex);
        }
    }

    private PdfComposer createComposer(Path path, boolean guideLines) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(PDRectangle.A4)
                .margin(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }
}
