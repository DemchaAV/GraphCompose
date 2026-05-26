package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.Executive;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Executive CV preset against the shared grouped
 * skills sample data — uppercase Poppins slate masthead, Lato meta +
 * link row, full-width muted rule, and bronze Poppins module
 * headings over a single-column body.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-executive-v2.pdf}.</p>
 */
public final class CvExecutiveExample {

    private CvExecutiveExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-executive-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = Executive.create();

        float m = (float) Executive.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, doc);
            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
