package com.demcha.compose.document.templates.cv;

import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.builtins.BlueBannerCvTemplate;
import com.demcha.compose.document.templates.builtins.BoxedSectionsCvTemplate;
import com.demcha.compose.document.templates.builtins.CenteredHeadlineCvTemplate;
import com.demcha.compose.document.templates.builtins.MonogramSidebarCvTemplate;
import com.demcha.compose.document.templates.builtins.SidebarPortraitCvTemplate;
import com.demcha.compose.document.templates.builtins.ClassicSerifCvTemplate;
import com.demcha.compose.document.templates.builtins.CompactMonoCvTemplate;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.builtins.EditorialBlueCvTemplate;
import com.demcha.compose.document.templates.builtins.ExecutiveSlateCvTemplate;
import com.demcha.compose.document.templates.builtins.NordicCleanCvTemplate;
import com.demcha.compose.document.templates.builtins.ProductLeaderCvTemplate;
import com.demcha.compose.document.templates.builtins.TechLeadCvTemplate;
import com.demcha.compose.document.templates.builtins.TimelineMinimalCvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.font.FontName;
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

import javax.imageio.ImageIO;
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

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15)) {
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
        TemplateTestSupport.assertPdfContainsGuideLines(outputFile);
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

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15)) {
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

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15)) {
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
        TemplateTestSupport.assertPdfContainsGuideLines(outputFile);
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

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15)) {
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

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 18, 18, 18, 18)) {
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
        TemplateTestSupport.assertPdfContainsGuideLines(outputFile);
    }

    @Test
    void shouldRenderExecutiveSlateTemplateToFile() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("executive_slate_cv_render_file", "clean", "templates", "cv");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 20, 20, 20, 20)) {
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

    @ParameterizedTest(name = "modern CV template {0}")
    @MethodSource("modernCvTemplates")
    void shouldRenderModernCvTemplateVariantsToFile(CvTemplate template, float margin, int expectedPages) throws Exception {
        String slug = template.getTemplateId().replace('-', '_');
        Path outputFile = VisualTestOutputs.preparePdf(slug + "_cv_render_file", "clean", "templates", "cv", "variants");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, margin, margin, margin, margin)) {
            template.compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, expectedPages);
        TemplateTestSupport.assertPdfPageCount(outputFile, expectedPages);
    }

    @Test
    void shouldUseFullWidthReferenceRulesInCenteredHeadlineTemplate() throws Exception {
        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 22, 22, 22, 22)) {
            new CenteredHeadlineCvTemplate().compose(document, TemplateTestSupport.canonicalCv());

            double innerWidth = document.canvas().innerWidth();
            List<PlacedFragment> rules = document.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().contains("CenteredHeadline"))
                    .filter(fragment -> fragment.path().contains("Rule"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.LineFragmentPayload)
                    .toList();

            assertThat(rules).hasSizeGreaterThanOrEqualTo(3);
            assertThat(rules)
                    .allSatisfy(fragment -> assertThat(fragment.width()).isCloseTo(innerWidth, within(0.01)));
            assertThat(document.layoutGraph().fragments())
                    .extracting(PlacedFragment::path)
                    .noneMatch(path -> path.contains("CenteredHeadlineBanner"));
        }
    }

    @Test
    void shouldShipTimelineContactIconsAsTransparentPngAssets() throws Exception {
        for (String icon : List.of("github.png", "linkedin.png", "google.png", "dribbble.png")) {
            assertTransparentPngAsset("/templates/cv/timeline-minimal/icons/", icon);
        }
    }

    @Test
    void shouldShipMonogramSidebarContactIconsAsTransparentPngAssets() throws Exception {
        for (String icon : List.of("phone.png", "email.png", "location.png", "linkedin.png")) {
            assertTransparentPngAsset("/templates/cv/monogram-sidebar/icons/", icon);
        }
    }

    @Test
    void shouldShipSidebarPortraitContactIconsAsTransparentPngAssets() throws Exception {
        for (String icon : List.of(
                "phone.png", "email.png", "location.png",
                "google.png", "linkedin.png", "github.png", "dribbble.png")) {
            assertTransparentPngAsset("/templates/cv/sidebar-portrait/icons/", icon);
        }
    }

    @Test
    void shouldKeepMonogramSidebarReferenceProportions() throws Exception {
        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 0, 0, 0, 0)) {
            new MonogramSidebarCvTemplate().compose(document, TemplateTestSupport.canonicalCv());

            double pageHeight = document.canvas().height();
            double sidebarWidth = document.canvas().innerWidth() * 0.33;
            List<PlacedFragment> fragments = document.layoutGraph().fragments();
            PlacedFragment sidebar = fragments.stream()
                    .filter(fragment -> fragment.path().endsWith("MonogramSidebarSidebar[0]"))
                    .findFirst()
                    .orElseThrow();
            PlacedFragment monogramRing = fragments.stream()
                    .filter(fragment -> fragment.path().contains("MonogramRing"))
                    .findFirst()
                    .orElseThrow();

            assertThat(sidebar.y()).isCloseTo(0.0, within(0.25));
            assertThat(monogramRing.width()).isCloseTo(122.0, within(0.01));
            assertThat(monogramRing.height()).isCloseTo(122.0, within(0.01));
            assertThat(topOf(pageHeight, monogramRing)).isCloseTo(36.0, within(0.01));

            List<PlacedFragment> sidebarRules = fragments.stream()
                    .filter(fragment -> fragment.path().contains("MonogramSidebarSidebar"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.LineFragmentPayload)
                    .toList();

            assertThat(sidebarRules).hasSize(3);
            assertThat(sidebarRules)
                    .allSatisfy(rule -> {
                        assertThat(rule.width()).isCloseTo(118.0, within(0.01));
                        assertThat(rule.x()).isCloseTo((sidebarWidth - 118.0) / 2.0, within(0.01));
                    });

            PlacedFragment profileHeader = fragments.stream()
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload)
                    .filter(fragment -> firstLine(fragment).contains("P R O F E S S I O N A L   S U M M A R Y"))
                    .findFirst()
                    .orElseThrow();

            assertThat(topOf(pageHeight, profileHeader)).isCloseTo(209.87, within(0.01));
        }
    }

    @Test
    void shouldKeepSidebarPortraitReferenceProportions() throws Exception {
        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 0, 0, 0, 0)) {
            new SidebarPortraitCvTemplate().compose(document, TemplateTestSupport.canonicalCv());

            double pageWidth = document.canvas().innerWidth();
            double pageHeight = document.canvas().height();
            List<PlacedFragment> fragments = document.layoutGraph().fragments();
            PlacedFragment sidebar = fragments.stream()
                    .filter(fragment -> fragment.path().endsWith("SidebarPortraitBodySidebar[0]"))
                    .findFirst()
                    .orElseThrow();
            double sidebarWidth = sidebar.width();
            PlacedFragment heroPanel = fragments.stream()
                    .filter(fragment -> fragment.path().contains("SidebarPortraitHero"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.ShapeFragmentPayload)
                    .findFirst()
                    .orElseThrow();
            PlacedFragment photo = fragments.stream()
                    .filter(fragment -> fragment.path().contains("SidebarPortraitPhoto"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.EllipseFragmentPayload)
                    .findFirst()
                    .orElseThrow();
            BuiltInNodeDefinitions.ParagraphFragmentPayload titlePayload = fragments.stream()
                    .filter(fragment -> fragment.path().contains("SidebarPortraitHero"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload)
                    .map(fragment -> (BuiltInNodeDefinitions.ParagraphFragmentPayload) fragment.payload())
                    .filter(payload -> payload.lines().getFirst().text().contains("Y O U R"))
                    .findFirst()
                    .orElseThrow();
            PlacedFragment profileRule = fragments.stream()
                    .filter(fragment -> fragment.path().contains("SidebarPortraitContent"))
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.LineFragmentPayload)
                    .findFirst()
                    .orElseThrow();

            assertThat(sidebar.x()).isCloseTo(0.0, within(0.01));
            assertThat(sidebar.y()).isCloseTo(0.0, within(0.25));
            assertThat(sidebar.width()).isCloseTo(sidebarWidth, within(0.01));
            assertThat(sidebar.height()).isCloseTo(pageHeight, within(0.25));
            assertThat(heroPanel.x()).isCloseTo(sidebarWidth, within(0.01));
            assertThat(heroPanel.width()).isCloseTo(pageWidth - sidebarWidth, within(0.01));
            assertThat(heroPanel.height()).isLessThan(105.0);
            assertThat(photo.y() + photo.height() / 2.0)
                    .isCloseTo(heroPanel.y() + heroPanel.height() / 2.0, within(4.0));
            assertThat(titlePayload.lines()).hasSize(1);
            assertThat(profileRule.x()).isCloseTo(sidebarWidth + 34.0, within(0.01));
        }
    }

    @Test
    void shouldRenderTimelineMinimalContactLinksAsPdfAnnotations() throws Exception {
        byte[] pdfBytes;
        try (var document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 22, 22, 22, 22)) {
            new TimelineMinimalCvTemplate().compose(document, TemplateTestSupport.canonicalCv());
            pdfBytes = document.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<String> uris = new ArrayList<>();
            for (var page : document.getPages()) {
                for (var annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionURI action) {
                        uris.add(action.getURI());
                    }
                }
            }

            assertThat(uris).contains(
                    "mailto:alex.carter@example.dev",
                    "https://www.linkedin.com/in/alex-carter-demo",
                    "https://github.com/example/alex-carter");
        }
    }

    @Test
    void shouldRenderTimelineMinimalTemplateToFileWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf(
                "timeline_minimal_cv_render_file_with_guide_lines",
                "guides",
                "templates",
                "cv",
                "variants");
        var cv = TemplateTestSupport.canonicalCv();

        try (var document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 22, 22, 22, 22, true)) {
            new TimelineMinimalCvTemplate().compose(document, cv);
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
        TemplateTestSupport.assertPdfPageCount(outputFile, 2);
        TemplateTestSupport.assertPdfContainsGuideLines(outputFile);
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

    private static Stream<Arguments> modernCvTemplates() {
        return Stream.of(
                Arguments.of(new NordicCleanCvTemplate(), 18, 2),
                Arguments.of(new CompactMonoCvTemplate(), 20, 1),
                Arguments.of(new ProductLeaderCvTemplate(), 18, 1),
                Arguments.of(new ClassicSerifCvTemplate(), 20, 2),
                Arguments.of(new TechLeadCvTemplate(), 20, 1),
                Arguments.of(new TimelineMinimalCvTemplate(), 22, 2),
                Arguments.of(new CenteredHeadlineCvTemplate(), 22, 1),
                Arguments.of(new BoxedSectionsCvTemplate(), 22, 2),
                Arguments.of(new SidebarPortraitCvTemplate(), 0, 1),
                Arguments.of(new BlueBannerCvTemplate(), 28, 1),
                Arguments.of(new MonogramSidebarCvTemplate(), 0, 1));
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

    private static void assertTransparentPngAsset(String root, String icon) throws Exception {
        try (var stream = CvTemplateRenderTest.class.getResourceAsStream(root + icon)) {
            assertThat(stream).as(icon + " resource").isNotNull();

            var image = ImageIO.read(stream);

            assertThat(image).as(icon + " image").isNotNull();
            assertThat(image.getWidth()).as(icon + " width").isEqualTo(64);
            assertThat(image.getHeight()).as(icon + " height").isEqualTo(64);
            assertThat(image.getColorModel().hasAlpha()).as(icon + " alpha channel").isTrue();
            assertThat((image.getRGB(0, 0) >>> 24) & 0xff).as(icon + " transparent corner").isZero();
        }
    }

    private static double topOf(double pageHeight, PlacedFragment fragment) {
        return pageHeight - (fragment.y() + fragment.height());
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
