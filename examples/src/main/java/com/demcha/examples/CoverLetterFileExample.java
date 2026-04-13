package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.CoverLetterTemplateV1;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public final class CoverLetterFileExample {

    private CoverLetterFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cover-letter.pdf");
        CoverLetterTemplateV1 template = new CoverLetterTemplateV1();

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(
                    composer,
                    ExampleDataFactory.sampleHeader(),
                    ExampleDataFactory.sampleCoverLetter(),
                    ExampleDataFactory.sampleJobDetails());
            composer.build();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
