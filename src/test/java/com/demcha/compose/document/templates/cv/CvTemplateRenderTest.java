package com.demcha.compose.document.templates.cv;

import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.document.templates.builtins.ExecutiveSlateCvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.font_library.FontName;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.testing.fixtures.CvTestFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CvTemplateRenderTest {

    @Test
    void shouldRenderTemplateCvAsDocument() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_document", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, cv);
            pdfBytes = document.toPdfBytes();
        }

        TemplateTestSupport.writePdf(outputFile, pdfBytes);
        TemplateTestSupport.assertPdfBytesLookValid(pdfBytes, 1);
        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldRenderTemplateCvDirectlyToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_with_guide_lines", "guides", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, true)) {
            new CvTemplateV1().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
    }

    @Test
    void shouldMakeHeaderContactLinksClickable() throws Exception {
        var cv = TemplateTestSupport.canonicalCv();
        var header = cv.header();
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, cv);
            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            List<String> uris = linkAnnotationUris(pdf);
            String extractedText = new PDFTextStripper().getText(pdf);

            assertThat(uris).anySatisfy(uri -> assertThat(uri).startsWith("mailto:" + header.getEmail().getTo()));
            assertThat(uris).contains(header.getLinkedIn().getLinkUrl().getUrl());
            assertThat(uris).contains(header.getGitHub().getLinkUrl().getUrl());
            assertThat(extractedText).contains(
                    header.getEmail().getDisplayText()
                            + " | "
                            + header.getLinkedIn().getDisplayText()
                            + " | "
                            + header.getGitHub().getDisplayText());
        }
    }

    @Test
    void shouldRenderEachTechnicalSkillAsItsOwnBullet() throws Exception {
        var cv = TemplateTestSupport.canonicalCv();
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, cv);
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
    void shouldKeepTechnicalSkillBulletSpacingConsistentWithWrappedLines() throws Exception {
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, cv);

            List<PlacedFragment> skillFragments = paragraphFragments(document.layoutGraph().fragments(), "TechnicalSkillsBody");

            assertThat(skillFragments).hasSize(7);
            for (int index = 0; index < skillFragments.size() - 1; index++) {
                PlacedFragment current = skillFragments.get(index);
                PlacedFragment next = skillFragments.get(index + 1);
                BuiltInNodeDefinitions.ParagraphFragmentPayload payload =
                        (BuiltInNodeDefinitions.ParagraphFragmentPayload) current.payload();
                double gapBetweenItems = current.y() - (next.y() + next.height());

                assertThat(gapBetweenItems).isCloseTo(payload.lineGap(), within(0.01));
            }
        }
    }

    @Test
    void shouldKeepMarkerlessCvRowsAlignedWithIndentedContinuations() throws Exception {
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, cv);

            List<PlacedFragment> projectFragments = paragraphFragments(document.layoutGraph().fragments(), "ProjectsBody");
            List<PlacedFragment> additionalFragments = paragraphFragments(document.layoutGraph().fragments(), "AdditionalBody");

            assertThat(projectFragments).hasSize(CvTestFixtures.lines(cv, "Projects").size());
            assertThat(additionalFragments).hasSize(CvTestFixtures.lines(cv, "Additional Information").size());
            assertThat(projectFragments)
                    .allSatisfy(fragment -> assertThat(firstLine(fragment)).doesNotStartWith(" "));
            assertThat(additionalFragments)
                    .allSatisfy(fragment -> assertThat(firstLine(fragment)).doesNotStartWith(" "));
            assertThat(projectFragments)
                    .anySatisfy(fragment -> assertThat(continuationLines(fragment)).anySatisfy(line ->
                            assertThat(line).startsWith("  ")));
        }
    }

    @Test
    void shouldRenderComposeFirstCvDocumentSpecWithSameListSemantics() throws Exception {
        CvDocumentSpec spec = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1().compose(document, spec);

            List<PlacedFragment> technicalSkillFragments = paragraphFragments(document.layoutGraph().fragments(), "TechnicalSkillsBody");
            List<PlacedFragment> projectFragments = paragraphFragments(document.layoutGraph().fragments(), "ProjectsBody");

            assertThat(technicalSkillFragments).hasSize(7);
            assertThat(projectFragments).hasSize(CvTestFixtures.lines(spec, "Projects").size());
            assertThat(projectFragments)
                    .anySatisfy(fragment -> assertThat(continuationLines(fragment)).anySatisfy(line ->
                            assertThat(line).startsWith("  ")));
        }
    }

    @Test
    void shouldRenderTemplateCvWithMarkdownFonts() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_markdown_fonts", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfUsesFont(outputFile, "Helvetica-Bold");
        TemplateTestSupport.assertPdfUsesFont(outputFile, "Helvetica-Oblique");
    }

    @Test
    void shouldRenderTemplateCvWithMoreInformationAcrossAboutOneAndHalfPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_render_file_rich_one_and_half_pages", "clean", "templates", "cv");
        var cv = TemplateTestSupport.expandedCanonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1().compose(document, cv);
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
        var cv = TemplateTestSupport.expandedCanonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, true)) {
            new CvTemplateV1().compose(document, cv);
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
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            new CvTemplateV1(TemplateTestSupport.cvThemeWith(fontName)).compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfUsesFont(outputFile, expectedPdfFontNameFragment);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 18, 18, 18, 18, false)) {
            new EditorialBlueCvTemplate().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfPageCount(outputFile, 1);
    }

    @Test
    void shouldRenderEditorialBlueTemplateToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("editorial_blue_cv_render_file_with_guide_lines", "guides", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 18, 18, 18, 18, true)) {
            new EditorialBlueCvTemplate().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfPageCount(outputFile, 1);
    }

    @Test
    void shouldRenderExecutiveSlateTemplateToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("executive_slate_cv_render_file", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 20, 20, 20, 20, false)) {
            new ExecutiveSlateCvTemplate().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 1);
        TemplateTestSupport.assertPdfPageCount(outputFile, 1);
    }

    @Test
    void shouldRenderExecutiveSlateTemplateWithClickableHeaderLinks() throws Exception {
        var cv = TemplateTestSupport.canonicalCv();
        var header = cv.header();
        byte[] pdfBytes;

        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 20, 20, 20, 20)) {
            new ExecutiveSlateCvTemplate().compose(document, cv);
            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            List<String> uris = linkAnnotationUris(pdf);
            String extractedText = new PDFTextStripper().getText(pdf);

            assertThat(uris).anySatisfy(uri -> assertThat(uri).startsWith("mailto:" + header.getEmail().getTo()));
            assertThat(uris).contains(header.getLinkedIn().getLinkUrl().getUrl());
            assertThat(uris).contains(header.getGitHub().getLinkUrl().getUrl());
            assertThat(extractedText).contains("alex.carter@example.dev | LinkedIn | GitHub");
        }
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

    private static List<PlacedFragment> paragraphFragments(List<PlacedFragment> fragments, String pathPart) {
        return fragments.stream()
                .filter(fragment -> fragment.path().contains(pathPart))
                .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload)
                .toList();
    }

    private static String firstLine(PlacedFragment fragment) {
        BuiltInNodeDefinitions.ParagraphFragmentPayload payload =
                (BuiltInNodeDefinitions.ParagraphFragmentPayload) fragment.payload();
        return payload.lines().getFirst().text();
    }

    private static List<String> continuationLines(PlacedFragment fragment) {
        BuiltInNodeDefinitions.ParagraphFragmentPayload payload =
                (BuiltInNodeDefinitions.ParagraphFragmentPayload) fragment.payload();
        return payload.lines().stream()
                .skip(1)
                .map(BuiltInNodeDefinitions.ParagraphLine::text)
                .toList();
    }

    private static List<String> linkAnnotationUris(PDDocument document) throws Exception {
        List<String> uris = new ArrayList<>();
        for (var page : document.getPages()) {
            for (var annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link
                        && link.getAction() instanceof PDActionURI action) {
                    uris.add(action.getURI());
                }
            }
        }
        return List.copyOf(uris);
    }
}
