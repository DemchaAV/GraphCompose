package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ClassicInvoice;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code invoice.v2} Classic Invoice preset against
 * the shared {@code InvoiceDocumentSpec} sample data using the default
 * {@code BrandTheme.invoiceModern()} theme.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/invoice/invoice-classic-v2.pdf}.</p>
 *
 * <p>The preset pads the page flow itself, so the session margin stays at
 * {@code ClassicInvoice.RECOMMENDED_MARGIN} (zero) and the page keeps its
 * plain white background — the letterhead look of the preset.</p>
 */
public final class ClassicInvoiceV2Example {

    private ClassicInvoiceV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/invoice", "invoice-classic-v2.pdf");
        InvoiceDocumentSpec spec = ExampleDataFactory.sampleInvoice();
        DocumentTemplate<InvoiceDocumentSpec> template =
                ClassicInvoice.create(BrandTheme.invoiceModern());

        float m = (float) ClassicInvoice.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
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
