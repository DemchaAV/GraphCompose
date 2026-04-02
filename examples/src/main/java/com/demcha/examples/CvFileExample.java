package com.demcha.examples;

import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.templates.data.MainPageCV;

import java.nio.file.Path;

public final class CvFileExample {

    private CvFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cv-editorial-blue.pdf");
        MainPageCV original = ExampleDataFactory.sampleCv();
        MainPageCvDTO rewritten = ExampleDataFactory.sampleCvRewrite();

        new EditorialBlueCvTemplate().render(original, rewritten, outputFile);
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
