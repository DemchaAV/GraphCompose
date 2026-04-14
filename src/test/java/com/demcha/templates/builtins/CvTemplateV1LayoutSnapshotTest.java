package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.EmailYaml;
import com.demcha.templates.data.Header;
import com.demcha.templates.data.LinkYml;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.data.ModuleSummary;
import com.demcha.templates.data.ModuleYml;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CvTemplateV1LayoutSnapshotTest {

    private final CvTemplateV1 template = new CvTemplateV1();
    private final MainPageCV original = new MainPageCVMock().getMainPageCV();

    @Test
    void shouldMatchStandardCvLayoutSnapshot() throws Exception {
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(composer, original, rewritten);
            LayoutSnapshotAssertions.assertMatches(composer, "template_cv_1_standard", "templates", "cv");
        }
    }

    @Test
    void shouldMatchRichCvLayoutSnapshotAndRenderPdf() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_layout_snapshot_rich", "clean", "templates", "cv");
        MainPageCV expanded = createExpandedCvForOneAndHalfPages(original);
        MainPageCvDTO rewritten = MainPageCvDTO.from(expanded);

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(composer, expanded, rewritten);
            LayoutSnapshotAssertions.assertMatches(composer, "template_cv_1_rich_one_and_half_pages", "templates", "cv");
            composer.build();
        }

        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
        }
    }

    @ParameterizedTest(name = "font theme {0}")
    @MethodSource("nonDefaultFontThemes")
    void shouldMatchFontThemeLayoutSnapshot(FontName fontName) throws Exception {
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            new CvTemplateV1(themeWith(fontName)).compose(composer, original, rewritten);
            LayoutSnapshotAssertions.assertMatches(
                    composer,
                    "template_cv_1_" + snapshotSlug(fontName),
                    "templates",
                    "cv",
                    "font-themes");
        }
    }

    private static Stream<FontName> nonDefaultFontThemes() {
        return Stream.of(
                FontName.TIMES_ROMAN,
                FontName.LATO,
                FontName.PT_SERIF,
                FontName.POPPINS,
                FontName.IBM_PLEX_SERIF,
                FontName.SPECTRAL,
                FontName.KANIT,
                FontName.VOLKHOV,
                FontName.ANDIKA);
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

    private static String snapshotSlug(FontName fontName) {
        return fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
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
                        "GraphCompose Template Extensions | Built reusable CV, proposal, invoice, and cover-letter template layers with markdown-aware block text, theme support, and render validation.",
                        "Document Preview Tooling | Added local preview workflows, scale resolution helpers, and visual fixtures that shorten iteration time for layout-heavy documents.",
                        "Developer Experience Improvements | Reduced friction around sample data, test outputs, and template exploration so contributors can validate changes quickly.")));

        expanded.setProfessionalExperience(copyModuleWithExtraPoints(
                base.getProfessionalExperience(),
                List.of(
                        "Senior Backend Engineer | Led a multi-quarter modernization effort for internal services, introduced clearer service boundaries, and documented rollout plans for critical migrations.",
                        "Platform Engineer | Worked closely with product and operations to stabilize releases, improve observability, and simplify deployment pipelines for cross-team tools.",
                        "Technical Lead | Mentored engineers through design reviews, introduced stronger testing expectations, and maintained delivery momentum during periods of changing requirements.",
                        "Consulting Engineer | Delivered bespoke automation and reporting features, often combining backend APIs, file generation, and presentation-friendly exports for client teams.")));

        expanded.setAdditional(copyModuleWithExtraPoints(
                base.getAdditional(),
                List.of(
                        "Languages: Ukrainian (native), English (professional working proficiency).",
                        "Open-source interests: document tooling, rendering systems, developer productivity, and maintainable test suites.",
                        "Working style: calm under ambiguity, highly collaborative, and strongly biased toward clear interfaces and observable behaviour.")));

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
        link.setLinkUrl(source.getLinkUrl());
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
