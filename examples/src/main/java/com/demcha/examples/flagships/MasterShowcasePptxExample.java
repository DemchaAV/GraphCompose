package com.demcha.examples.flagships;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * The {@link MasterShowcaseExample} kitchen-sink report emitted as a PowerPoint
 * deck: the same composition, built once through the shared session config and
 * written through the fixed-layout PPTX backend via
 * {@link DocumentSession#buildPptx(java.nio.file.Path)}. Each resolved page becomes one
 * identically-sized slide, so the multi-page report opens in PowerPoint as an
 * editable copy of the PDF — rich text, the advanced table, header/footer
 * chrome, and barcodes arrive as native shapes and text frames; the rotated,
 * clip-masked seal is the one region that lands as a pixel-exact picture.
 *
 * @since 2.1.0
 */
public final class MasterShowcasePptxExample {

    private MasterShowcasePptxExample() {
    }

    /**
     * Builds the report as the .pptx twin of {@link MasterShowcaseExample}.
     *
     * @return the generated file path
     * @throws Exception when composition or rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "master-showcase.pptx");
        try (DocumentSession document = MasterShowcaseExample.document(outputFile).create()) {
            MasterShowcaseExample.compose(document);
            document.buildPptx(outputFile);
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
