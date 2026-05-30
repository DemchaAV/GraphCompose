package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.MinimalUnderlined;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Minimal Underlined preset against the same Jordan
 * Rivera sample data used by {@link CvBoxedV2Example}. Demonstrates
 * that switching from one preset to another is a one-line change at
 * the call site — same data, different visual composition.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-minimal-underlined.pdf}.</p>
 */
public final class CvMinimalUnderlinedExample {

    private CvMinimalUnderlinedExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-minimal-underlined-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = MinimalUnderlined.create();

        float m = (float) MinimalUnderlined.RECOMMENDED_MARGIN;
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
