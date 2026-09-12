package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.TealPulse;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.TealPulseSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Teal Pulse preset against the nursing
 * sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-teal-pulse-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — the design is drawn on a raster
 * whose proportion is not A4's — so the session starts unconfigured.</p>
 */
public final class TealPulseExample {

    private TealPulseExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/cv", "cv-teal-pulse-v2.pdf");
        CvDocument doc = TealPulseSampleData.sample();
        DocumentTemplate<CvDocument> template = TealPulse.create();

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
