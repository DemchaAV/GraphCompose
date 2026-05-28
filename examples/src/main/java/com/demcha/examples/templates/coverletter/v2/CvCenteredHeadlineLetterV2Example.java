package com.demcha.examples.templates.coverletter.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.coverletter.v2.presets.CenteredHeadlineLetter;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Centered Headline cover-letter preset — centred
 * spaced-caps Poppins name, spaced-caps subheadline, framing rules, and
 * centred contact, then a single-column letter body.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/coverletter/cover-letter-centered-headline-v2.pdf}.</p>
 */
public final class CvCenteredHeadlineLetterV2Example {

    private CvCenteredHeadlineLetterV2Example() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/coverletter", "cover-letter-centered-headline-v2.pdf");
        CoverLetterDocument doc = ExampleDataFactory.sampleCoverLetterDocumentV2();
        DocumentTemplate<CoverLetterDocument> template = CenteredHeadlineLetter.create();

        float m = (float) CenteredHeadlineLetter.RECOMMENDED_MARGIN;
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
