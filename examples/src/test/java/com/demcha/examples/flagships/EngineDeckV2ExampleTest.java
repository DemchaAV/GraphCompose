package com.demcha.examples.flagships;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke coverage for the parallel module-first release deck.
 */
class EngineDeckV2ExampleTest {

    @Test
    void rendersTheFivePageWidescreenDeck() throws Exception {
        Path pdf = EngineDeckV2Example.generate();

        assertThat(pdf).exists();
        assertThat(Files.size(pdf)).isGreaterThan(20_000L);
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(5);
            assertThat(document.getPage(0).getMediaBox().getWidth())
                    .isEqualTo((float) EngineDeckV2Example.PAGE_WIDTH);
            assertThat(document.getPage(0).getMediaBox().getHeight())
                    .isEqualTo((float) EngineDeckV2Example.PAGE_HEIGHT);
            assertThat(pageText(document, 1)).doesNotContain("1 / 4");
            assertThat(pageText(document, 2)).contains("1 / 4");
            assertThat(pageText(document, 5)).contains("4 / 4");
        }
    }

    private static String pageText(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }
}
