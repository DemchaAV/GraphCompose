package com.demcha.examples;

import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.CoverLetterTemplateV1;

import java.nio.file.Path;

public final class CoverLetterFileExample {

    private CoverLetterFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cover-letter.pdf");

        new CoverLetterTemplateV1().render(
                ExampleDataFactory.sampleHeader(),
                ExampleDataFactory.sampleCoverLetter(),
                ExampleDataFactory.sampleJobDetails(),
                outputFile);

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
