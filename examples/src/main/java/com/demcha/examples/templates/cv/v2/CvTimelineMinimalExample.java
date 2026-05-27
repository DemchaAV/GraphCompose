package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.TimelineMinimal;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Timeline Minimal CV preset against the shared
 * grouped skills sample data — spaced uppercase Barlow Condensed
 * name, right-aligned contact stack with PNG icons, and the central
 * vertical timeline axis (4 segments / 3 circles) separating the
 * sidebar (Education / Skills / Expertise / Languages) from the main
 * column (Professional Profile / Work Experience).
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-timeline-minimal-v2.pdf}.</p>
 */
public final class CvTimelineMinimalExample {

    private CvTimelineMinimalExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-timeline-minimal-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = TimelineMinimal.create();

        float m = (float) TimelineMinimal.RECOMMENDED_MARGIN;
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
