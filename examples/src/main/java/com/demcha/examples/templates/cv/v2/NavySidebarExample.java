package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.NavySidebar;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.NavySidebarSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Navy Sidebar preset against the
 * marketing-manager sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-navy-sidebar-v2.pdf}.</p>
 *
 * <p>The preset owns its page — A4 with no margin, the navy plate painted as
 * a page background — so the session starts unconfigured.</p>
 */
public final class NavySidebarExample {

    private NavySidebarExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-navy-sidebar-v2.pdf");
        CvDocument doc = NavySidebarSampleData.sample();
        DocumentTemplate<CvDocument> template = NavySidebar.create();

        try (DocumentSession document = GraphCompose.document(outputFile).create()) {
            template.compose(document, doc);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
