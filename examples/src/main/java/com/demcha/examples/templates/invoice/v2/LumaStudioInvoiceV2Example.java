package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.LumaStudioInvoice;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.LumaStudioInvoiceSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code invoice.v2} Luma Studio Invoice preset against
 * the shared structured invoice sample data.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/invoice/invoice-luma-studio-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — size, margins, the paper tint, the
 * cream sidebar column and the sign-off band — so the session starts
 * unconfigured. The lockup is drawn from the brand's monogram and wordmark,
 * and the amounts take their mark from the data's currency code.</p>
 */
public final class LumaStudioInvoiceV2Example {

    private LumaStudioInvoiceV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/invoice", "invoice-luma-studio-v2.pdf");
        StructuredInvoiceDocumentSpec spec = LumaStudioInvoiceSampleData.sample();
        DocumentTemplate<StructuredInvoiceDocumentSpec> template = LumaStudioInvoice.create();

        try (DocumentSession document = GraphCompose.document(outputFile).create()) {
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
