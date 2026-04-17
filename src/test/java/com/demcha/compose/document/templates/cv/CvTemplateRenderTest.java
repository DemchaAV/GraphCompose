package com.demcha.compose.document.templates.cv;

import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.font_library.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

class CvTemplateRenderTest {

    @Test
    void shouldRenderTemplateCvAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_document", "clean", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, original, rewritten);
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 1);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file", "clean", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_with_guide_lines", "guides", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, true)) {
            new CvTemplateV1().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderTemplateCvWithMarkdownFonts() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_markdown_fonts", "clean", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfUsesFont(outputFile, "Helvetica-Bold");
        TemplateTestSupport.assertPdfUsesFont(outputFile, "Helvetica-Oblique");
    }

    @Test
    void shouldRenderTemplateCvWithMoreInformationAcrossAboutOneAndHalfPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_rich_one_and_half_pages", "clean", "templates", "cv");
        var original = TemplateTestSupport.expandedCanonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
        TemplateTestSupport.assertPdfPageCount(outputFile, 2);
    }

    @Test
    void shouldRenderTemplateCvWithMoreInformationAcrossAboutOneAndHalfPagesWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf(
                "template_cv_1_render_file_rich_one_and_half_pages_with_guides",
                "guides",
                "templates",
                "cv");
        var original = TemplateTestSupport.expandedCanonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, true)) {
            new CvTemplateV1().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
        TemplateTestSupport.assertPdfPageCount(outputFile, 2);
    }

    @ParameterizedTest(name = "theme font {0}")
    @MethodSource("fontThemes")
    void shouldRenderTemplateCvWithDifferentFonts(FontName fontName, String expectedPdfFontNameFragment) throws Exception {
        String slug = fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_" + slug, "clean", "templates", "cv", "font-themes");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1(TemplateTestSupport.cvThemeWith(fontName)).compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfUsesFont(outputFile, expectedPdfFontNameFragment);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file", "clean", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 18, 18, 18, 18, false)) {
            new EditorialBlueCvTemplate().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfPageCount(outputFile, 1);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file_with_guide_lines", "guides", "templates", "cv");
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 18, 18, 18, 18, true)) {
            new EditorialBlueCvTemplate().compose(document, original, rewritten);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfPageCount(outputFile, 1);
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
