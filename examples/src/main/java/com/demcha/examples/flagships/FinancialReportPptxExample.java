package com.demcha.examples.flagships;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * The {@link FinancialReportExample} one-page dashboard emitted as a PowerPoint
 * deck: the same composition, built once through the shared session config and
 * written through the fixed-layout PPTX backend via
 * {@link DocumentSession#buildPptx()}. One resolved page becomes one
 * identically-sized slide, so the dashboard opens in PowerPoint as an editable
 * copy of the PDF — metric tiles, the native charts, and the data table stay
 * editable shapes and text frames; the masthead photo travels as a picture.
 *
 * @since 2.1.0
 */
public final class FinancialReportPptxExample {

    private FinancialReportPptxExample() {
    }

    /**
     * Builds the dashboard as the .pptx twin of {@link FinancialReportExample}.
     *
     * @return the generated file path
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "financial-report.pptx");
        try (DocumentSession document = FinancialReportExample.document(outputFile).create()) {
            FinancialReportExample.compose(document);
            document.buildPptx();
        }
        return outputFile;
    }

    /**
     * Generates the deck from the command line.
     *
     * @param args unused
     * @throws Exception when rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
