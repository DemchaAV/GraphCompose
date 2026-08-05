package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.TimelineMinimal;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Timeline Minimal CV preset against the shared
 * grouped skills sample data — spaced uppercase Barlow Condensed
 * name, right-aligned contact stack with PNG icons, and the central
 * vertical timeline axis (4 segments / 3 circles) separating the
 * sidebar from the main column.
 *
 * <p>Module headings come from the sample's own section titles, so the
 * sidebar reads Education &amp; Certifications / Technical Skills /
 * Projects and the main column Professional Summary / Professional
 * Experience. The sample carries more than one page of content: the
 * remainder continues on page two, where the axis shortens to match.</p>
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
