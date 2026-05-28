package com.demcha.examples.templates.coverletter.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.coverletter.v2.presets.EngineeringResumeLetter;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Engineering Resume cover-letter preset — full-width
 * navy command header (UPPERCASE name + role subtitle, right-aligned
 * contact with cyan-green links, green accent strip) then a
 * single-column letter body. Pair with {@code CvEngineeringResumeExample}.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/coverletter/cover-letter-engineering-resume-v2.pdf}.</p>
 */
public final class CvEngineeringResumeLetterV2Example {

    private CvEngineeringResumeLetterV2Example() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/coverletter", "cover-letter-engineering-resume-v2.pdf");
        CoverLetterDocument doc = ExampleDataFactory.sampleCoverLetterDocumentV2();
        DocumentTemplate<CoverLetterDocument> template = EngineeringResumeLetter.create();

        float m = (float) EngineeringResumeLetter.RECOMMENDED_MARGIN;
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
