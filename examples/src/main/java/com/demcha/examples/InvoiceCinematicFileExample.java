package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Phase E.1 — runnable showcase for {@code InvoiceTemplateV2}, the
 * cinematic theme-driven invoice template. Renders the same
 * {@link com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec}
 * as the v1 template through the modern business theme so reviewers can
 * compare the two outputs side by side.
 *
 * @author Artem Demchyshyn
 */
public final class InvoiceCinematicFileExample {

    private InvoiceCinematicFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("invoice-cinematic.pdf");
        BusinessTheme theme = BusinessTheme.modern();
        InvoiceTemplateV2 template = new InvoiceTemplateV2(theme);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
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
