package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.BoxedSections;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the v2 Boxed Sections CV preset against the structured
 * {@code CvDocument} sample data using the default
 * {@code CvTheme.boxedClassic()} theme.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-boxed-sections-v2.pdf}.</p>
 *
 * <p>This is the canonical "hello world" for the v2 CV pipeline:
 * fetch sample data, ask the preset for a template, render it. No
 * custom theme, no manual section ordering, no parsing.</p>
 */
public final class CvBoxedV2Example {

    private CvBoxedV2Example() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-boxed-sections-v2.pdf");
        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        DocumentTemplate<CvDocument> template = BoxedSections.create();

        float m = (float) BoxedSections.RECOMMENDED_MARGIN;
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
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
