package com.demcha.examples.templates.coverletter.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.coverletter.v2.presets.ExecutiveLetter;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Executive cover-letter preset against the shared
 * Jordan Rivera identity — the same masthead as the Executive CV
 * (uppercase Poppins slate name, Lato meta + bronze link row,
 * full-width muted rule) followed by a single-column letter body.
 *
 * <p>Pair with {@code CvExecutiveExample} to view the CV and letter as
 * a matched set.</p>
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/coverletter/cover-letter-executive-v2.pdf}.</p>
 */
public final class CvExecutiveLetterV2Example {

    private CvExecutiveLetterV2Example() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/coverletter", "cover-letter-executive-v2.pdf");
        CoverLetterDocument doc = ExampleDataFactory.sampleCoverLetterDocumentV2();
        DocumentTemplate<CoverLetterDocument> template = ExecutiveLetter.create();

        float m = (float) ExecutiveLetter.RECOMMENDED_MARGIN;
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
