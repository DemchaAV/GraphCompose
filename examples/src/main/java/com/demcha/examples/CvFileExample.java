package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.templates.data.MainPageCV;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public final class CvFileExample {

    private CvFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cv-editorial-blue.pdf");
        MainPageCV original = ExampleDataFactory.sampleCv();
        MainPageCvDTO rewritten = ExampleDataFactory.sampleCvRewrite();

        EditorialBlueCvTemplate template = new EditorialBlueCvTemplate();

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            template.compose(composer, original, rewritten);
            composer.build();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
