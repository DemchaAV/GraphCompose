package com.demcha.Templatese.cv_templates;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.template.MainPageCvDTO;
import com.demcha.Templatese.templates.Template_CV1;
import com.demcha.compose.font_library.FontName;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateCV1RenderTest {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");
    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);

    @Test
    void shouldRenderTemplateCvAsDocument() throws Exception {
        Path outputFile = prepareOutputFile("template_cv_1_render_document");

        Template_CV1 template = new Template_CV1();

        try (PDDocument document = template.render(original, rewritten)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFile() throws Exception {
        Path outputFile = prepareOutputFile("template_cv_1_render_file");

        Template_CV1 template = new Template_CV1();
        template.render(original, rewritten, outputFile);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

        assertPdfLooksValid(outputFile);
    }
    @Test
    void shouldRenderTemplateCvDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = prepareOutputFile("template_cv_1_render_file_with_guide_lines");

        Template_CV1 template = new Template_CV1();
        template.render(original, rewritten, outputFile,true);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

        assertPdfLooksValid(outputFile);
    }

    @ParameterizedTest(name = "theme font {0}")
    @MethodSource("fontThemes")
    void shouldRenderTemplateCvWithDifferentFonts(FontName fontName, String expectedPdfFontNameFragment) throws Exception {
        String slug = fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        Path outputFile = prepareOutputFile("template_cv_1_render_" + slug);

        Template_CV1 template = new Template_CV1(themeWith(fontName));

        try (PDDocument document = template.render(original, rewritten)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
        assertPdfUsesFont(outputFile, expectedPdfFontNameFragment);
    }

    private void assertPdfLooksValid(Path outputFile) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    private void assertPdfUsesFont(Path outputFile, String expectedPdfFontNameFragment) throws Exception {
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            boolean containsExpectedFont = false;
            String normalizedExpectedName = normalizeFontName(expectedPdfFontNameFragment);

            for (var page : saved.getPages()) {
                for (var resourceFontName : page.getResources().getFontNames()) {
                    PDFont font = page.getResources().getFont(resourceFontName);
                    if (font != null && normalizeFontName(font.getName()).contains(normalizedExpectedName)) {
                        containsExpectedFont = true;
                        break;
                    }
                }
                if (containsExpectedFont) {
                    break;
                }
            }

            assertThat(containsExpectedFont)
                    .as("PDF should use font containing '%s'", expectedPdfFontNameFragment)
                    .isTrue();
        }
    }

    private static String normalizeFontName(String value) {
        return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static Path prepareOutputFile(String baseName) throws Exception {
        Files.createDirectories(VISUAL_DIR);
        Path outputFile = VISUAL_DIR.resolve(baseName + ".pdf");
        try {
            Files.deleteIfExists(outputFile);
            return outputFile;
        } catch (FileSystemException ignored) {
            return VISUAL_DIR.resolve(baseName + "_" + UUID.randomUUID() + ".pdf");
        }
    }

    private static CvTheme themeWith(FontName fontName) {
        CvTheme base = CvTheme.defaultTheme();
        return new CvTheme(
                base.primaryColor(),
                base.secondaryColor(),
                base.bodyColor(),
                base.accentColor(),
                fontName,
                fontName,
                base.nameFontSize(),
                base.headerFontSize(),
                base.bodyFontSize(),
                base.spacing(),
                base.moduleMargin(),
                base.spacingModuleName());
    }

    private static Stream<Arguments> fontThemes() {
        return Stream.of(
                Arguments.of(FontName.HELVETICA, "Helvetica"),
                Arguments.of(FontName.TIMES_ROMAN, "Times"),
                Arguments.of(FontName.LATO, "Lato"),
                Arguments.of(FontName.PT_SERIF, "PT Serif"),
                Arguments.of(FontName.POPPINS, "Poppins"),
                Arguments.of(FontName.IBM_PLEX_SERIF, "IBMPlexSerif"),
                Arguments.of(FontName.SPECTRAL, "Spectral"),
                Arguments.of(FontName.KANIT, "Kanit"),
                Arguments.of(FontName.VOLKHOV, "Volkhov"),
                Arguments.of(FontName.ANDIKA, "Andika"));
    }
}
