package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.OrangeOps;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.support.OrangeOpsSampleData;

import java.nio.file.Path;

/**
 * Renders the layered {@code cv.v2} Orange Ops preset against the design
 * sample.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-orange-ops-v2.pdf}.</p>
 *
 * <p>The preset owns its page geometry — every length is a share of the
 * design's own grid — so the session starts unconfigured. What it does need is
 * the display family: Orange Ops sets its name and headings in Oswald, which
 * neither the templates artifact nor {@code graph-compose-fonts} carries, so
 * registering it is the caller's job and this example is the caller. The two
 * faces are the example module's own resources.</p>
 */
public final class OrangeOpsExample {

    private OrangeOpsExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/cv", "cv-orange-ops-v2.pdf");
        CvDocument doc = OrangeOpsSampleData.sample();
        DocumentTemplate<CvDocument> template = OrangeOps.create();

        try (DocumentSession document = GraphCompose.document(outputFile).create()) {
            document.registerFontFamily(
                    FontFamilyDefinition.classpath(OrangeOps.DISPLAY_FONT,
                                    "/fonts/oswald/Oswald-Regular.ttf")
                            .wordFamily("Oswald")
                            .boldResource("/fonts/oswald/Oswald-SemiBold.ttf")
                            .build());
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
