package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.invoice.presets.WorkspaceInvoice;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.WorkspaceInvoiceSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code invoice.v2} Workspace preset against the design
 * sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/invoice/invoice-workspace-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry and its pagination — the table's header
 * repeats and a continuation page reserves a deeper bottom margin — so the
 * session starts unconfigured. The sample brings no logo, so the lockup box
 * beside the title is filled with the brand's name as a wordmark.</p>
 */
public final class WorkspaceInvoiceV2Example {

    private WorkspaceInvoiceV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/invoice",
                "invoice-workspace-v2.pdf");
        StructuredInvoiceData data = WorkspaceInvoiceSampleData.sample();
        DocumentTemplate<StructuredInvoiceData> template = WorkspaceInvoice.create();

        try (DocumentSession document = GraphCompose.document(outputFile).create()) {
            template.compose(document, data);
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
