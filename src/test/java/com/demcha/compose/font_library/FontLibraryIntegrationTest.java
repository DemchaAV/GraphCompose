package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.word_systems.WordFont;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FontLibraryIntegrationTest {

    @Test
    void shouldExposeBundledGoogleFontsInPdfComposer() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf().create()) {
            FontLibrary fonts = composer.entityManager().getFonts();

            assertThat(fonts.availableFonts())
                    .contains(FontName.HELVETICA, FontName.LATO, FontName.IBM_PLEX_SERIF, FontName.ZILLA_SLAB,
                            FontName.KANIT, FontName.VOLKHOV, FontName.ANDIKA);
            assertThat(fonts.getFont(FontName.LATO, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.LATO, WordFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.KANIT, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.KANIT, WordFont.class)).isPresent();
        }
    }

    @Test
    void shouldRegisterCustomFontFamilyFromFilePaths() throws Exception {
        FontName customFamily = FontName.of("Brand Sans");
        Path fontsRoot = Path.of("src", "main", "resources", "fonts", "google", "lato");

        try (PdfComposer composer = GraphCompose.pdf()
                .registerFontFamily(
                        customFamily,
                        fontsRoot.resolve("Lato-Regular.ttf"),
                        fontsRoot.resolve("Lato-Bold.ttf"),
                        fontsRoot.resolve("Lato-Italic.ttf"),
                        fontsRoot.resolve("Lato-BoldItalic.ttf"))
                .create()) {

            FontLibrary fonts = composer.entityManager().getFonts();
            assertThat(fonts.availableFonts()).contains(customFamily);
            assertThat(fonts.getFont(customFamily, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(customFamily, WordFont.class)).isPresent();
        }
    }
}
