package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * The Engine Deck flagship rendered as a PowerPoint deck: the exact
 * composition of {@link EngineDeckExample} — banner infographic, authoring
 * pipeline, and two benchmark pages — built once and emitted through the
 * fixed-layout PPTX backend via {@link DocumentSession#buildPptx()}. One
 * resolved page becomes one identically-sized slide with the same geometry
 * the PDF carries, so the .pptx opens in PowerPoint as a slide-per-page copy
 * of the PDF deck — editable shapes and text frames, except clipped regions,
 * which land as pixel-exact pictures.
 */
public final class EngineDeckPptxExample {

    private EngineDeckPptxExample() {
    }

    /**
     * Builds the landscape showcase deck as a .pptx.
     *
     * @return the generated file path
     * @throws Exception when rendering or icon IO fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("flagships", "engine-deck.pptx");
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4.landscape())
                .margin(16, 16, 30, 16)
                .create()) {
            EngineDeckExample.compose(document);
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
