package com.demcha.examples.templates.invoice;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.v2.presets.ModernInvoice;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the cinematic invoice look on the layered
 * {@code invoice.v2} surface — {@link ModernInvoice} on
 * {@link BrandTheme#invoiceModern()}, rendering the shared
 * {@link InvoiceDocumentSpec} sample on the cream page background.
 *
 * @author Artem Demchyshyn
 */
public final class InvoiceCinematicFileExample {

    private InvoiceCinematicFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/invoice", "invoice-cinematic.pdf");
        BrandTheme theme = BrandTheme.invoiceModern();
        DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create(theme);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.palette().mainFill())
                .margin(28, 28, 28, 28)
                .create()) {
            template.compose(document, ExampleDataFactory.sampleInvoice());
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
