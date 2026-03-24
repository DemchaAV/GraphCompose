package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FontShowcaseRenderTest {

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");

    @Test
    void shouldRenderAvailableFontsPreviewToVisualTestsDirectory() throws Exception {
        Path outputFile = VISUAL_DIR.resolve("available-fonts-preview.pdf");
        Files.createDirectories(VISUAL_DIR);
        Files.deleteIfExists(outputFile);

        GraphCompose.renderAvailableFontsPreview(outputFile);

        assertThat(GraphCompose.availableFonts())
                .contains(FontName.HELVETICA, FontName.LATO, FontName.POPPINS, FontName.IBM_PLEX_SERIF)
                .hasSizeGreaterThan(10);
        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }
}
