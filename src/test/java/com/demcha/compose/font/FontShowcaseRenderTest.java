package com.demcha.compose.font;

import com.demcha.compose.GraphCompose;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FontShowcaseRenderTest {

    @Test
    void shouldRenderAvailableFontsPreviewToVisualTestsDirectory() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("available-fonts-preview", "clean", "fonts");

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
