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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
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

        MainPageCV expanded = createExpandedCvForOneAndHalfPages(original);
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

        MainPageCV expanded = createExpandedCvForOneAndHalfPages(original);
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

    private static MainPageCV createExpandedCvForOneAndHalfPages(MainPageCV base) {
        MainPageCV expanded = new MainPageCV();
        expanded.setHeader(copyHeader(base.getHeader()));

        ModuleSummary summary = new ModuleSummary();
        summary.setModuleName(base.getModuleSummary().getModuleName());
        summary.setBlockSummary(String.join(" ",
                base.getModuleSummary().getBlockSummary(),
                "Focused on platform engineering, document generation, backend integration, and resilient delivery for teams that need both speed and maintainability.",
                "Comfortable translating ambiguous business needs into structured implementation plans, reusable libraries, and production-ready developer workflows.",
                "Strong at decomposing large initiatives into reviewable milestones, mentoring contributors, and improving code health while continuing to ship value."));
        expanded.setModuleSummary(summary);

        expanded.setTechnicalSkills(copyModuleWithExtraPoints(
                base.getTechnicalSkills(),
                List.of(
                        "Java 21, Spring Boot, PDFBox, layout engines, rendering pipelines, and template abstractions for document generation at scale.",
                        "Testing strategy across unit, integration, render, and visual regression checks with strong focus on layout stability and pagination behaviour.",
                        "Architecture work covering API design, modularization, refactoring plans, performance tuning, and reusable design tokens for templates.")));

        expanded.setEducationCertifications(copyModuleWithExtraPoints(
                base.getEducationCertifications(),
                List.of(
                        "Advanced coursework in distributed systems, document automation, developer tooling, and software architecture communication.",
                        "Continuous self-study in typography for programmatic documents, PDF internals, content measurement, and layout debugging techniques.")));

        expanded.setProjects(copyModuleWithExtraPoints(
                base.getProjects(),
                List.of(
                        "**GraphCompose Template Extensions** | Built reusable CV, proposal, invoice, and cover-letter template layers with markdown-aware block text, theme support, and render validation.",
                        "**Document Preview Tooling** | Added local preview workflows, scale resolution helpers, and visual fixtures that shorten iteration time for layout-heavy documents.",
                        "**Developer Experience Improvements** | Reduced friction around sample data, test outputs, and template exploration so contributors can validate changes quickly.")));

        expanded.setProfessionalExperience(copyModuleWithExtraPoints(
                base.getProfessionalExperience(),
                List.of(
                        "**Senior Backend Engineer** | Led a multi-quarter modernization effort for internal services, introduced clearer service boundaries, and documented rollout plans for critical migrations.",
                        "**Platform Engineer** | Worked closely with product and operations to stabilize releases, improve observability, and simplify deployment pipelines for cross-team tools.",
                        "**Technical Lead **| Mentored engineers through design reviews, introduced stronger testing expectations, and maintained delivery momentum during periods of changing requirements.",
                        "**Consulting Engineer** | Delivered bespoke automation and reporting features, often combining backend APIs, file generation, and presentation-friendly exports for client teams.")));

        expanded.setAdditional(copyModuleWithExtraPoints(
                base.getAdditional(),
                List.of(
                        "**Languages:** Ukrainian (native), English (professional working proficiency).",
                        "**Open-source interests:** document tooling, rendering systems, developer productivity, and maintainable test suites.",
                        "**Working style:** calm under ambiguity, highly collaborative, and strongly biased toward clear interfaces and observable behaviour.")));

        return expanded;
    }

    private static Header copyHeader(Header source) {
        Header header = new Header();
        header.setName(source.getName());
        header.setAddress(source.getAddress());
        header.setPhoneNumber(source.getPhoneNumber());
        header.setEmail(copyEmail(source.getEmail()));
        header.setGitHub(copyLink(source.getGitHub()));
        header.setLinkedIn(copyLink(source.getLinkedIn()));
        return header;
    }

    private static EmailYaml copyEmail(EmailYaml source) {
        EmailYaml email = new EmailYaml();
        email.setTo(source.getTo());
        email.setSubject(source.getSubject());
        email.setBody(source.getBody());
        email.setDisplayText(source.getDisplayText());
        return email;
    }

    private static LinkYml copyLink(LinkYml source) {
        LinkYml link = new LinkYml();
        link.setDisplayText(source.getDisplayText());
        if (source.getLinkUrl() != null) {
            link.setLinkUrl(new LinkUrl(source.getLinkUrl().getUrl()));
        }
        return link;
    }

    private static ModuleYml copyModuleWithExtraPoints(ModuleYml source, List<String> extraPoints) {
        ModuleYml module = new ModuleYml();
        module.setName(source.getName());
        List<String> points = new ArrayList<>(source.getModulePoints());
        points.addAll(extraPoints);
        module.setModulePoints(points);
        return module;
    }
}
