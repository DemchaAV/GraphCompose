package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders every Templates v2 CV preset against the same shared sample
 * spec and writes the results to {@code target/visual-tests/clean/templates/cv/v2/}
 * for human glance comparison with the legacy preset PDFs in the
 * sibling {@code variants/} directory.
 *
 * <p>The intent is parity-by-eye, not bit-exact reproduction — Phase
 * E.1 ships the new architecture with reasonable visual fidelity, and
 * Phase E.3's snapshot parity gate locks the per-preset look before
 * the legacy classes are deleted in the cleanup commit.</p>
 */
class PresetVisualGalleryTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static final Path OUTPUT_DIR = Path.of(
            "target", "visual-tests", "clean", "templates", "cv", "v2");

    private static CvSpec sampleSpec() {
        return CvSpec.builder()
                .header(CvHeader.builder()
                        .name("Artem Demchyshyn")
                        .address("London, UK")
                        .phone("+44 20 5555 1000")
                        .email("artem@demo.dev")
                        .link("LinkedIn", "https://linkedin.com/in/graphcompose")
                        .link("GitHub", "https://github.com/DemchaAV")
                        .build())
                .module(CvModule.of("Professional Summary",
                        new ParagraphBlock(
                                "Platform engineer building resilient PDF and "
                                        + "document-generation workflows for reliable "
                                        + "business output.")))
                .module(CvModule.of("Technical Skills",
                        new BulletListBlock(List.of(
                                "Java 21, PDFBox, Maven, REST APIs",
                                "Template design systems, pagination, semantic layout composition",
                                "Testing strategy, CI pipelines, developer enablement"))))
                .module(CvModule.of("Education & Certifications",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("MSc Computer Science",
                                        "University of Manchester | 2021"),
                                new IndentedBlock.Item("Oracle Java Certification",
                                        "Professional track | 2023")))))
                .module(CvModule.of("Projects",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("GraphCompose",
                                        "Declarative PDF layout engine for reusable document generation"),
                                new IndentedBlock.Item("Template Studio",
                                        "Internal tool for evaluating CV, proposal, and invoice output")))))
                .module(CvModule.of("Professional Experience",
                        new MultiParagraphBlock(List.of(
                                "**Senior Platform Engineer**, Northwind Systems | "
                                        + "*2024-Present* — Led reusable document flows.",
                                "**Software Engineer**, BrightLeaf Labs | *2021-2024* "
                                        + "— Built backend services and rendering pipelines."))))
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry("Location",
                                        "Based in London and available for hybrid or remote collaboration"),
                                new KeyValueBlock.Entry("Interests",
                                        "Platform architecture, DX, and document-quality automation")))))
                .build();
    }

    private void render(String fileSlug, Function<BusinessTheme, DocumentTemplate<CvSpec>> factory) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve(fileSlug + "_render_file.pdf");
        Files.deleteIfExists(output);

        DocumentTemplate<CvSpec> template = factory.apply(THEME);

        try (DocumentSession session = GraphCompose.document(output)
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .create()) {

            template.compose(session, sampleSpec());
            session.buildPdf();
        }

        assertThat(output).exists();
        assertThat(Files.size(output)).isPositive();
    }

    @Test
    void rendersNordicClean() throws Exception {
        render("nordic_clean_v2", NordicClean::create);
    }

    @Test
    void rendersClassicSerif() throws Exception {
        render("classic_serif_v2", ClassicSerif::create);
    }

    @Test
    void rendersCompactMono() throws Exception {
        render("compact_mono_v2", CompactMono::create);
    }

    @Test
    void rendersExecutive() throws Exception {
        render("executive_v2", Executive::create);
    }

    @Test
    void rendersEngineeringResume() throws Exception {
        render("engineering_resume_v2", EngineeringResume::create);
    }

    @Test
    void rendersTimelineMinimal() throws Exception {
        render("timeline_minimal_v2", TimelineMinimal::create);
    }

    @Test
    void rendersBoxedSections() throws Exception {
        render("boxed_sections_v2", BoxedSections::create);
    }

    @Test
    void rendersCenteredHeadline() throws Exception {
        render("centered_headline_v2", CenteredHeadline::create);
    }

    @Test
    void rendersBlueBanner() throws Exception {
        render("blue_banner_v2", BlueBanner::create);
    }

    @Test
    void rendersEditorialBlue() throws Exception {
        render("editorial_blue_v2", EditorialBlue::create);
    }

    @Test
    void rendersPanel() throws Exception {
        render("panel_v2", Panel::create);
    }

    @Test
    void rendersSidebarPortrait() throws Exception {
        render("sidebar_portrait_v2", SidebarPortrait::create);
    }

    @Test
    void rendersMonogramSidebar() throws Exception {
        render("monogram_sidebar_v2", MonogramSidebar::create);
    }

    @Test
    void exposesStableIdentities() {
        assertThat(NordicClean.ID).isEqualTo("nordic-clean");
        assertThat(NordicClean.DISPLAY_NAME).isEqualTo("Nordic Clean");
        assertThat(ClassicSerif.ID).isEqualTo("classic-serif");
        assertThat(ClassicSerif.DISPLAY_NAME).isEqualTo("Classic Serif");
        assertThat(CompactMono.ID).isEqualTo("compact-mono");
        assertThat(CompactMono.DISPLAY_NAME).isEqualTo("Compact Mono");
        assertThat(Executive.ID).isEqualTo("executive");
        assertThat(Executive.DISPLAY_NAME).isEqualTo("Executive");
        assertThat(EngineeringResume.ID).isEqualTo("engineering-resume");
        assertThat(EngineeringResume.DISPLAY_NAME).isEqualTo("Engineering Resume");
        assertThat(TimelineMinimal.ID).isEqualTo("timeline-minimal");
        assertThat(TimelineMinimal.DISPLAY_NAME).isEqualTo("Timeline Minimal");
        assertThat(BoxedSections.ID).isEqualTo("boxed-sections");
        assertThat(BoxedSections.DISPLAY_NAME).isEqualTo("Boxed Sections");
        assertThat(CenteredHeadline.ID).isEqualTo("centered-headline");
        assertThat(CenteredHeadline.DISPLAY_NAME).isEqualTo("Centered Headline");
        assertThat(BlueBanner.ID).isEqualTo("blue-banner");
        assertThat(BlueBanner.DISPLAY_NAME).isEqualTo("Blue Banner");
        assertThat(EditorialBlue.ID).isEqualTo("editorial-blue");
        assertThat(EditorialBlue.DISPLAY_NAME).isEqualTo("Editorial Blue");
        assertThat(Panel.ID).isEqualTo("panel");
        assertThat(Panel.DISPLAY_NAME).isEqualTo("Panel");
        assertThat(SidebarPortrait.ID).isEqualTo("sidebar-portrait");
        assertThat(SidebarPortrait.DISPLAY_NAME).isEqualTo("Sidebar Portrait");
        assertThat(MonogramSidebar.ID).isEqualTo("monogram-sidebar");
        assertThat(MonogramSidebar.DISPLAY_NAME).isEqualTo("Monogram Sidebar");
    }

    @Test
    void presetsAreInvocableWithoutSampleSpec() {
        // Smoke test that every preset class loads and returns a
        // non-null DocumentTemplate without touching the spec layer.
        assertThat(NordicClean.create(THEME)).isNotNull();
        assertThat(ClassicSerif.create(THEME)).isNotNull();
        assertThat(CompactMono.create(THEME)).isNotNull();
        assertThat(Executive.create(THEME)).isNotNull();
        assertThat(EngineeringResume.create(THEME)).isNotNull();
        assertThat(TimelineMinimal.create(THEME)).isNotNull();
        assertThat(BoxedSections.create(THEME)).isNotNull();
        assertThat(CenteredHeadline.create(THEME)).isNotNull();
        assertThat(BlueBanner.create(THEME)).isNotNull();
        assertThat(EditorialBlue.create(THEME)).isNotNull();
        assertThat(Panel.create(THEME)).isNotNull();
        assertThat(SidebarPortrait.create(THEME)).isNotNull();
        assertThat(MonogramSidebar.create(THEME)).isNotNull();
    }
}
