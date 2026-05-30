package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.exceptions.DocumentRenderingException;
import com.demcha.compose.document.templates.cv.v2.presets.MintEditorial;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Renders the v2 Mint Editorial CV preset against the rich "Rose Harris"
 * showcase dataset ({@link ExampleDataFactory#mintEditorialShowcaseCv()})
 * so the output matches the Mint Editorial visual reference 1:1 — a
 * two-page editorial resume with a full-width mint masthead rule, a
 * contact / interests / education left sidebar beside profile + experience
 * on page 1, and an expertise / skill-bars / social sidebar beside
 * continued experience, awards and references on page 2.
 *
 * <p>The showcase dataset is example-local on purpose: the shared
 * {@link ExampleDataFactory#sampleCvDocumentV2()} stays untouched so no
 * other preset's visual baseline moves.</p>
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-mint-editorial-v2.pdf}
 * (falls back to {@code …-rev2.pdf} when the primary file is locked by an
 * open viewer).</p>
 */
public final class CvMintEditorialExample {

    private CvMintEditorialExample() {
    }

    public static Path generate() throws Exception {
        CvDocument doc = ExampleDataFactory.mintEditorialShowcaseCv();
        Path primary = ExampleOutputPaths.prepare(
                "templates/cv", "cv-mint-editorial-v2.pdf");
        try {
            return render(doc, primary);
        } catch (DocumentRenderingException rendering) {
            if (!causedByIo(rendering)) {
                throw rendering;
            }
            // The user may still have the previous PDF open in a viewer,
            // which holds a Windows file lock (surfaces as an IOException
            // wrapped in DocumentRenderingException). Fall back to a -rev2
            // path so the render still succeeds and is reported.
            Path fallback = ExampleOutputPaths.prepare(
                    "templates/cv", "cv-mint-editorial-v2-rev2.pdf");
            System.out.println("Primary output locked (" + rendering.getMessage()
                    + "); writing fallback: " + fallback);
            return render(doc, fallback);
        }
    }

    private static boolean causedByIo(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static Path render(CvDocument doc, Path outputFile) throws Exception {
        DocumentTemplate<CvDocument> template = MintEditorial.create();
        float m = (float) MintEditorial.RECOMMENDED_MARGIN;
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
