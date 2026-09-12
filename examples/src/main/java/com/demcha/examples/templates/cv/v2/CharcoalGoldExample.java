package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.CharcoalGold;
import com.demcha.examples.support.CharcoalGoldSampleData;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Charcoal Gold preset against the
 * project-manager sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-charcoal-gold-v2.pdf}.</p>
 *
 * <p>Unlike its ported siblings this preset leaves the page to the caller,
 * so the session sets A4 with no margin: both columns run to the paper edge
 * and each carries its own padding.</p>
 */
public final class CharcoalGoldExample {

    private CharcoalGoldExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-charcoal-gold-v2.pdf");
        CvDocument doc = CharcoalGoldSampleData.sample();
        DocumentTemplate<CvDocument> template = CharcoalGold.create();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
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
