package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.EngineeringResume;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Engineering Resume CV preset against the shared
 * grouped skills sample data — full-width navy command header with
 * UPPERCASE Barlow name, right-aligned contact stack with cyan-green
 * underlined links, dark navy skill rail (Core Stack / Learning /
 * Details) and white evidence cards for Leadership Experience and
 * Technical Evidence on the right.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-engineering-resume-v2.pdf}.</p>
 */
public final class CvEngineeringResumeExample {

    private CvEngineeringResumeExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-engineering-resume-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = EngineeringResume.create();

        float m = (float) EngineeringResume.RECOMMENDED_MARGIN;
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
