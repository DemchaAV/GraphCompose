package com.demcha.examples.templates.cv;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Canonical single-file CV authoring demo using the layered
 * {@code cv.v2} surface — the simplest "author one CV" path: build a
 * {@link CvDocument}, pick a preset's {@code create()} factory, compose.
 * The full 14-preset gallery lives in
 * {@link CvTemplateGalleryFileExample}.
 */
public final class CvFileExample {

    private CvFileExample() {
    }

    /**
     * Renders the example PDF to {@code generated-pdfs/templates/cv/cv-modern-professional.pdf}.
     *
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/cv", "cv-modern-professional.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = ModernProfessional.create();

        float m = (float) ModernProfessional.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, doc);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * Renders the example PDF and prints the path.
     *
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
