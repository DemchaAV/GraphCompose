package com.demcha.templates.cv_templates;

import com.demcha.templates.CvTheme;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.CvTemplateV1;
import com.demcha.templates.data.EmailYaml;
import com.demcha.templates.data.Header;
import com.demcha.templates.data.LinkYml;
import com.demcha.templates.data.ModuleSummary;
import com.demcha.templates.data.ModuleYml;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.mock.MainPageCVMock;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.testing.fixtures.CvTestFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateCV1RenderTest {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    private final MainPageCV original = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewritten = MainPageCvDTO.from(original);

    @Test
    void shouldRenderTemplateCvAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_document", "clean", "templates", "cv");

        CvTemplateV1 template = new CvTemplateV1();

        try (PDDocument document = template.render(original, rewritten)) {
            document.save(outputFile.toFile());
        }

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file", "clean", "templates", "cv");

        CvTemplateV1 template = new CvTemplateV1();
        template.render(original, rewritten, outputFile);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

        assertPdfLooksValid(outputFile);
    }
    @Test
    void shouldRenderTemplateCvDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_with_guide_lines", "guides", "templates", "cv");

        CvTemplateV1 template = new CvTemplateV1();
        template.render(original, rewritten, outputFile,true);
        System.out.printf("Document saves %s",outputFile.toAbsolutePath());

        assertPdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderTemplateCvWithMoreInformationAcrossAboutOneAndHalfPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_rich_one_and_half_pages", "clean", "templates", "cv");

        MainPageCV expanded = CvTestFixtures.createExpandedCvForOneAndHalfPages(original);
        MainPageCvDTO expandedRewritten = MainPageCvDTO.from(expanded);

        CvTemplateV1 template = new CvTemplateV1();
        template.render(expanded, expandedRewritten, outputFile);

        assertPdfLooksValid(outputFile);
        assertPdfPageCount(outputFile, 2);
    }

    @Test
    void shouldRenderTemplateCvWithMoreInformationAcrossAboutOneAndHalfPagesWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf(
                "template_cv_1_render_file_rich_one_and_half_pages_with_guides",
                "guides",
                "templates",
                "cv");

        MainPageCV expanded = CvTestFixtures.createExpandedCvForOneAndHalfPages(original);
        MainPageCvDTO expandedRewritten = MainPageCvDTO.from(expanded);

        CvTemplateV1 template = new CvTemplateV1();
        template.render(expanded, expandedRewritten, outputFile, true);

        assertPdfLooksValid(outputFile);
        assertPdfPageCount(outputFile, 2);
    }

    @ParameterizedTest(name = "theme font {0}")
    @MethodSource("fontThemes")
    void shouldRenderTemplateCvWithDifferentFonts(FontName fontName, String expectedPdfFontNameFragment) throws Exception {
        String slug = fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_" + slug, "clean", "templates", "cv", "font-themes");

        CvTemplateV1 template = new CvTemplateV1(themeWith(fontName));

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

    private void assertPdfPageCount(Path outputFile, int expectedPages) throws Exception {
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isEqualTo(expectedPages);
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
