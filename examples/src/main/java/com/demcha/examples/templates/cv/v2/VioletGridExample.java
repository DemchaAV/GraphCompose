package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.VioletGrid;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.VioletGridSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Violet Grid preset against the design
 * sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-violet-grid-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — every length is a share of the
 * design's own grid — so the session starts unconfigured.</p>
 */
public final class VioletGridExample {

    private VioletGridExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/cv", "cv-violet-grid-v2.pdf");
        CvDocument doc = VioletGridSampleData.sample();
        DocumentTemplate<CvDocument> template = VioletGrid.create();

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
