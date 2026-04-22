package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.word_systems.WordFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FontLibraryIntegrationTest {

    @Test
    void shouldExposeBundledGoogleFontsInEngineComposerHarness() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThat(composer.availableFonts())
                    .contains(FontName.HELVETICA, FontName.LATO, FontName.IBM_PLEX_SERIF, FontName.ZILLA_SLAB,
                            FontName.KANIT, FontName.VOLKHOV, FontName.ANDIKA);
        }

        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = DefaultFonts.library(document);
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

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .registerFontFamily(
                        customFamily,
                        fontsRoot.resolve("Lato-Regular.ttf"),
                        fontsRoot.resolve("Lato-Bold.ttf"),
                        fontsRoot.resolve("Lato-Italic.ttf"),
                        fontsRoot.resolve("Lato-BoldItalic.ttf"))
                .create()) {

            assertThat(composer.availableFonts()).contains(customFamily);
        }

        FontFamilyDefinition customDefinition = FontFamilyDefinition.files(customFamily, fontsRoot.resolve("Lato-Regular.ttf"))
                .boldPath(fontsRoot.resolve("Lato-Bold.ttf"))
                .italicPath(fontsRoot.resolve("Lato-Italic.ttf"))
                .boldItalicPath(fontsRoot.resolve("Lato-BoldItalic.ttf"))
                .build();

        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = DefaultFonts.library(document, List.of(customDefinition));
            assertThat(fonts.getFont(customFamily, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(customFamily, WordFont.class)).isPresent();
        }
    }
}
