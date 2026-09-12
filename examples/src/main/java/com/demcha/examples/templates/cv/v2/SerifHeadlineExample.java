package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.SerifHeadline;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.SerifHeadlineSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Serif Headline preset against the
 * software-engineer sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-serif-headline-v2.pdf}.</p>
 *
 * <p>The preset owns its page — A4, with the margin the design's own grid
 * defines — so the session starts unconfigured.</p>
 */
public final class SerifHeadlineExample {

    private SerifHeadlineExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-serif-headline-v2.pdf");
        CvDocument doc = SerifHeadlineSampleData.sample();
        DocumentTemplate<CvDocument> template = SerifHeadline.create();

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
