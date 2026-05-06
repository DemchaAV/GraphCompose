package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the canonical Modern Professional CV using the Templates v2
 * preset {@link ModernProfessional}. This replaces the legacy
 * {@code CvTemplateV1} example wiring; the visual signature and
 * sample data both come from the v2 surface.
 */
public final class CvFileExample {

    private CvFileExample() {
    }

    /**
     * Renders the example PDF to {@code generated-pdfs/cv-modern-professional.pdf}.
     *
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cv-modern-professional.pdf");
        CvSpec spec = ExampleDataFactory.sampleCvSpecV2();
        DocumentTemplate<CvSpec> template = ModernProfessional.create(BusinessTheme.modern());

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .create()) {
            template.compose(document, spec);
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
