package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.MintEditorial;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the Mint Editorial CV against the rich "Rose Harris" showcase
 * dataset with a single <strong>custom colour</strong> via
 * {@link MintEditorial.Options} — a soft kraft-paper header band that fills
 * the whole masthead zone from the top page edge down to the mint rule. Only
 * {@code headerBandColor} is set; everything else stays default, so the dark
 * name, mint tagline, and mint full-width rule are unchanged and read cleanly
 * on the light tan band. This demonstrates the colour-customisation API; the
 * default-coloured render lives in {@code CvMintEditorialExample} and is left
 * untouched.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-mint-editorial-v2-custom.pdf}.</p>
 */
public final class CvMintEditorialCustomExample {

    private CvMintEditorialCustomExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-mint-editorial-v2-custom.pdf");
        CvDocument doc = ExampleDataFactory.mintEditorialShowcaseCv();

        // Set ONLY the header band colour — a soft warm kraft-paper tan. Every
        // other knob stays default: dark ink name, mint tagline, mint
        // full-width rule. The dark name reads cleanly on the light tan band.
        MintEditorial.Options options = MintEditorial.Options.builder()
                .headerBandColor(DocumentColor.rgb(228, 217, 198))  // kraft-paper tan band
                .build();
        DocumentTemplate<CvDocument> template = MintEditorial.create(options);

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
