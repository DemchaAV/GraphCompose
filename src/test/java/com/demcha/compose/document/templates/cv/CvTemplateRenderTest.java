package com.demcha.compose.document.templates.cv;

import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.font_library.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldRenderEachTechnicalSkillAsItsOwnBullet() throws Exception {
        var original = TemplateTestSupport.canonicalCv();
        var rewritten = TemplateTestSupport.rewrite(original);
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, original, rewritten);
            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(pdf);
            String technicalSkills = section(text, "Technical Skills", "Education & Certifications");

            assertThat(countOccurrences(technicalSkills, "\u2022")).isEqualTo(7);
            assertThat(technicalSkills).contains("\u2022 Languages:");
            assertThat(technicalSkills).contains("\u2022 Backend (Spring):");
            assertThat(technicalSkills).contains("\u2022 Tools & Delivery:");
        }
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

    private static String section(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, Math.max(0, startIndex));
        if (startIndex < 0 || endIndex < 0) {
            return text;
        }
        return text.substring(startIndex, endIndex);
    }

    private static int countOccurrences(String text, String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }
}
