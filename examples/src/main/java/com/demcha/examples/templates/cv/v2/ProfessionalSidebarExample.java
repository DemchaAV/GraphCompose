package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.ProfessionalSidebar;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.ProfessionalSidebarSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Professional Sidebar preset against the
 * backend-engineer sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-professional-sidebar-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — a 491.6 x 737.28pt sheet with no
 * margin — so the session starts unconfigured.</p>
 */
public final class ProfessionalSidebarExample {

    private ProfessionalSidebarExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-professional-sidebar-v2.pdf");
        CvDocument doc = ProfessionalSidebarSampleData.sample();
        DocumentTemplate<CvDocument> template = ProfessionalSidebar.create();

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
