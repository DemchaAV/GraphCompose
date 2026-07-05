package com.demcha.compose.font;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.word.WordFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FontLibraryIntegrationTest {

    @Test
    void shouldExposeBundledGoogleFontsInEngineComposerHarness() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThat(composer.availableFonts())
                    .contains(FontName.HELVETICA, FontName.LATO, FontName.IBM_PLEX_SERIF, FontName.ZILLA_SLAB,
                            FontName.KANIT, FontName.VOLKHOV, FontName.ANDIKA, FontName.JETBRAINS_MONO);
        }

        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = PdfFontLibraryFactory.library(document);
            assertThat(fonts.getFont(FontName.LATO, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.LATO, WordFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.KANIT, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.KANIT, WordFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.JETBRAINS_MONO, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(FontName.JETBRAINS_MONO, WordFont.class)).isPresent();
        }
    }

    @Test
    void shouldRegisterCustomFontFamilyFromFilePaths() throws Exception {
        FontName customFamily = FontName.of("Brand Sans");
        // The bundled fonts moved to the graph-compose-fonts module in v1.8.0.
        // This test exercises file-based registration, so it reads the real TTFs
        // from that module's resources (engine test cwd is the repo root).
        Path fontsRoot = Path.of("fonts", "src", "main", "resources", "fonts", "google", "lato");

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
            FontLibrary fonts = PdfFontLibraryFactory.library(document, List.of(customDefinition));
            assertThat(fonts.getFont(customFamily, PdfFont.class)).isPresent();
            assertThat(fonts.getFont(customFamily, WordFont.class)).isPresent();
        }
    }

    @Test
    void shouldMaterializeStandard14FontsWithoutBundledFontsArtifact() {
        // The standard-14 families embed nothing and need no PDF document and no
        // graph-compose-fonts jar — this is the baseline an engine-only consumer
        // (no bundled fonts on the classpath) still gets.
        FontLibrary standard = PdfFontLibraryFactory.standardLibrary();

        assertThat(standard.getFont(FontName.HELVETICA, PdfFont.class)).isPresent();
        assertThat(standard.getFont(FontName.TIMES_ROMAN, PdfFont.class)).isPresent();
        assertThat(standard.getFont(FontName.COURIER, PdfFont.class)).isPresent();
    }

    @Test
    void shouldGiveActionableErrorWhenBundledFontResourceMissing() {
        // Simulates the bundled fonts being absent (graph-compose-fonts not on
        // the classpath): a fonts/google/... resource that cannot be found must
        // point the caller at the companion artifact, not just print a raw path.
        FontFamilyDefinition missing = FontFamilyDefinition
                .classpath(FontName.of("Missing Family"), "fonts/google/__missing__/Missing-Regular.ttf")
                .build();
        FontFamilyDefinition.FontBinarySource regular = missing.fontSourceSet().orElseThrow().regular();

        assertThatThrownBy(regular::openStream)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("graph-compose-fonts")
                .hasMessageContaining("fonts/google/__missing__/Missing-Regular.ttf");
    }
}
