package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ConsultingInvoice;
import com.demcha.examples.support.ConsultingInvoiceSampleData;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code invoice.v2} Consulting Invoice preset against
 * the shared structured invoice sample data.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/invoice/invoice-consulting-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — size, margins, the page fills and
 * the footer band — so the session starts unconfigured; see
 * {@code ConsultingInvoice.RECOMMENDED_MARGIN}. The brand logo is supplied
 * through the data, the way a caller supplies its own.</p>
 */
public final class ConsultingInvoiceV2Example {

    private ConsultingInvoiceV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/invoice", "invoice-consulting-v2.pdf");
        StructuredInvoiceDocumentSpec spec = ConsultingInvoiceSampleData.sample();
        DocumentTemplate<StructuredInvoiceDocumentSpec> template = ConsultingInvoice.create();

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
