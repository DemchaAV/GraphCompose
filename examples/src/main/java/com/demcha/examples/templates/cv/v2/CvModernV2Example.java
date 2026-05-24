package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Modern Professional CV preset against the shared
 * sample data — single-page, right-aligned slate-blue name, flat
 * bright-blue section titles.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-modern-professional-v2.pdf}.</p>
 */
public final class CvModernV2Example {

    private CvModernV2Example() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-modern-professional-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = ModernProfessional.create();

        float m = (float) ModernProfessional.RECOMMENDED_MARGIN;
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
