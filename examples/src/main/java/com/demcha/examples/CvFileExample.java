package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.document.templates.data.cv.MainPageCV;
import com.demcha.compose.document.templates.data.cv.MainPageCvDTO;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
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

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create()) {
            template.compose(document, original, rewritten);
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
