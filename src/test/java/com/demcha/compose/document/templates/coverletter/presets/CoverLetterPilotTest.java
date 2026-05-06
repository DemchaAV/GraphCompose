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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pilot test for the Templates v2 cover-letter pipeline. Renders the
 * {@link ModernProfessionalLetter} preset to
 * {@code target/visual-tests/clean/templates/coverletter/v2/} so the
 * full vertical (spec → builder → layout → header component →
 * markdown-aware paragraphs → session.add) can be inspected end-to-end.
 */
class CoverLetterPilotTest {

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

    @Test
    void modernProfessionalLetterIdentities() {
        assertThat(ModernProfessionalLetter.ID).isEqualTo("modern-professional-letter");
        assertThat(ModernProfessionalLetter.DISPLAY_NAME).isEqualTo("Modern Professional Letter");

        DocumentTemplate<CoverLetterSpec> template = ModernProfessionalLetter.create(THEME);
        assertThat(template.id()).isEqualTo(ModernProfessionalLetter.ID);
        assertThat(template.displayName()).isEqualTo(ModernProfessionalLetter.DISPLAY_NAME);
    }

    @Test
    void rendersModernProfessionalLetterToPdf() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve("modern_professional_letter_v2_render_file.pdf");
        Files.deleteIfExists(output);

        DocumentTemplate<CoverLetterSpec> template = ModernProfessionalLetter.create(THEME);
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
    void specBuilderProducesImmutableSpec() {
        CoverLetterSpec spec = CoverLetterSpec.builder()
                .header(CoverLetterHeader.builder().name("X").build())
                .greeting("Hi")
                .paragraph("Body")
                .closing("Bye")
                .build();
        assertThat(spec.greeting()).isEqualTo("Hi");
        assertThat(spec.bodyParagraphs()).containsExactly("Body");
        assertThat(spec.closing()).isEqualTo("Bye");
    }

    @Test
    void specRequiresNonNullHeaderAndBodyList() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new CoverLetterSpec(null, "Hi", List.of(), "Bye"))
                .isInstanceOf(NullPointerException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new CoverLetterSpec(
                        CoverLetterHeader.builder().name("X").build(),
                        "Hi", null, "Bye"))
                .isInstanceOf(NullPointerException.class);
    }
}
