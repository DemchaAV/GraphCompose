package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code invoice.v2} Modern Invoice preset against
 * the shared {@code InvoiceDocumentSpec} sample data using the default
 * {@code BrandTheme.invoiceModern()} theme.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/invoice/invoice-modern-v2.pdf}.</p>
 *
 * <p>This is the "hello world" for the v2 invoice pipeline: fetch sample
 * data, ask the preset for a template, render it — the same shape as the
 * v2 CV examples, now on the invoice family.</p>
 */
public final class ModernInvoiceV2Example {

    private ModernInvoiceV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/invoice", "invoice-modern-v2.pdf");
        InvoiceDocumentSpec spec = ExampleDataFactory.sampleInvoice();
        BrandTheme theme = BrandTheme.invoiceModern();
        DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create(theme);

        float m = (float) ModernInvoice.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.palette().mainFill())
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
