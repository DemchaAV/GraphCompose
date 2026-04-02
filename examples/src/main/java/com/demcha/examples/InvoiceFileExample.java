package com.demcha.examples;

import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.InvoiceTemplateV1;

import java.nio.file.Path;

public final class InvoiceFileExample {

    private InvoiceFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("invoice.pdf");
        new InvoiceTemplateV1().render(ExampleDataFactory.sampleInvoice(), outputFile);
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
