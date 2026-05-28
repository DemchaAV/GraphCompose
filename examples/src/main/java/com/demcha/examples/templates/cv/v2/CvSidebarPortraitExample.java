package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.SidebarPortrait;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Sidebar Portrait CV preset against the shared
 * grouped skills sample data — pale-beige left sidebar with a
 * circular portrait photo, contact stack with inline icons, and
 * education / skills / languages summary, plus the hero name strip
 * and main career narrative on the right.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-sidebar-portrait-v2.pdf}.</p>
 */
public final class CvSidebarPortraitExample {

    private CvSidebarPortraitExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-sidebar-portrait-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = SidebarPortrait.create();

        float m = (float) SidebarPortrait.RECOMMENDED_MARGIN;
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
