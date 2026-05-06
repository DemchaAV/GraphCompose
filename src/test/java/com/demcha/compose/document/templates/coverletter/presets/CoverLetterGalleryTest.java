package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterHeader;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders every Templates v2 cover-letter preset against the same
 * shared sample spec and writes the results to
 * {@code target/visual-tests/clean/templates/coverletter/v2/} for
 * human glance comparison. The 14 letter renders should read as a
 * matched set with the sibling 14 CV preset renders in
 * {@code .../templates/cv/v2/}.
 */
class CoverLetterGalleryTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static final Path OUTPUT_DIR = Path.of(
            "target", "visual-tests", "clean", "templates", "coverletter", "v2");

    private static CoverLetterSpec sampleSpec() {
        return CoverLetterSpec.builder()
                .header(CoverLetterHeader.builder()
                        .name("Artem Demchyshyn")
                        .address("London, UK")
                        .phone("+44 20 5555 1000")
                        .email("artem@demo.dev")
                        .link("LinkedIn", "https://linkedin.com/in/graphcompose")
                        .link("GitHub", "https://github.com/DemchaAV")
                        .build())
                .greeting("Dear Hiring Team at **Northwind Systems**,")
                .paragraph("I am excited to share my interest in the Senior "
                        + "Platform Engineer role. My recent work has focused "
                        + "on building **reusable document-generation systems** "
                        + "that balance public API design, render quality, and "
                        + "maintainability.")
                .paragraph("I enjoy translating fuzzy workflow requirements into "
                        + "clear template abstractions, reliable test coverage, "
                        + "and examples that make adoption easier for the rest "
                        + "of the team.")
                .paragraph("I would welcome the opportunity to bring that same "
                        + "mix of engineering rigor and product thinking to your "
                        + "platform group.")
                .closing("Sincerely, *Artem Demchyshyn*")
                .build();
    }

    private void render(String slug, Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>> factory) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve(slug + "_v2_render_file.pdf");
        Files.deleteIfExists(output);

        DocumentTemplate<CoverLetterSpec> template = factory.apply(THEME);
        try (DocumentSession session = GraphCompose.document(output)
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            template.compose(session, sampleSpec());
            session.buildPdf();
        }
        assertThat(output).exists();
        assertThat(Files.size(output)).isPositive();
    }

    @Test
    void rendersNordicCleanLetter() throws Exception {
        render("nordic_clean_letter", NordicCleanLetter::create);
    }

    @Test
    void rendersClassicSerifLetter() throws Exception {
        render("classic_serif_letter", ClassicSerifLetter::create);
    }

    @Test
    void rendersCompactMonoLetter() throws Exception {
        render("compact_mono_letter", CompactMonoLetter::create);
    }

    @Test
    void rendersExecutiveLetter() throws Exception {
        render("executive_letter", ExecutiveLetter::create);
    }

    @Test
    void rendersEngineeringResumeLetter() throws Exception {
        render("engineering_resume_letter", EngineeringResumeLetter::create);
    }

    @Test
    void rendersTimelineMinimalLetter() throws Exception {
        render("timeline_minimal_letter", TimelineMinimalLetter::create);
    }

    @Test
    void rendersBoxedSectionsLetter() throws Exception {
        render("boxed_sections_letter", BoxedSectionsLetter::create);
    }

    @Test
    void rendersCenteredHeadlineLetter() throws Exception {
        render("centered_headline_letter", CenteredHeadlineLetter::create);
    }

    @Test
    void rendersBlueBannerLetter() throws Exception {
        render("blue_banner_letter", BlueBannerLetter::create);
    }

    @Test
    void rendersEditorialBlueLetter() throws Exception {
        render("editorial_blue_letter", EditorialBlueLetter::create);
    }

    @Test
    void rendersPanelLetter() throws Exception {
        render("panel_letter", PanelLetter::create);
    }

    @Test
    void rendersSidebarPortraitLetter() throws Exception {
        render("sidebar_portrait_letter", SidebarPortraitLetter::create);
    }

    @Test
    void rendersMonogramSidebarLetter() throws Exception {
        render("monogram_sidebar_letter", MonogramSidebarLetter::create);
    }

    @Test
    void exposesStableIdentities() {
        assertThat(NordicCleanLetter.ID).isEqualTo("nordic-clean-letter");
        assertThat(ClassicSerifLetter.ID).isEqualTo("classic-serif-letter");
        assertThat(CompactMonoLetter.ID).isEqualTo("compact-mono-letter");
        assertThat(ExecutiveLetter.ID).isEqualTo("executive-letter");
        assertThat(EngineeringResumeLetter.ID).isEqualTo("engineering-resume-letter");
        assertThat(TimelineMinimalLetter.ID).isEqualTo("timeline-minimal-letter");
        assertThat(BoxedSectionsLetter.ID).isEqualTo("boxed-sections-letter");
        assertThat(CenteredHeadlineLetter.ID).isEqualTo("centered-headline-letter");
        assertThat(BlueBannerLetter.ID).isEqualTo("blue-banner-letter");
        assertThat(EditorialBlueLetter.ID).isEqualTo("editorial-blue-letter");
        assertThat(PanelLetter.ID).isEqualTo("panel-letter");
        assertThat(SidebarPortraitLetter.ID).isEqualTo("sidebar-portrait-letter");
        assertThat(MonogramSidebarLetter.ID).isEqualTo("monogram-sidebar-letter");
    }
}
