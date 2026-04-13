package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.InvoiceTemplateV1;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public final class InvoiceFileExample {

    private InvoiceFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("invoice.pdf");
        InvoiceTemplateV1 template = new InvoiceTemplateV1();

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            template.compose(composer, ExampleDataFactory.sampleInvoice());
            composer.build();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
