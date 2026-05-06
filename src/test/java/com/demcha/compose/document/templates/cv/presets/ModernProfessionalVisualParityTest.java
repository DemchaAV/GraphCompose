package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visual parity check for the Templates v2 pilot preset
 * {@link ModernProfessional}.
 *
 * <p>Renders the pilot preset against the same sample CV data shape
 * used by the legacy {@code CvTemplateV1} visual test and writes the
 * resulting PDF to {@code target/visual-tests/clean/templates/cv/v2/}.
 * The intent is glance-by-glance comparison with the matching legacy
 * PDF, not bit-exact reproduction — pilot landing is about validating
 * the architecture end-to-end, not about pixel parity (which is
 * Phase E.1's job once visual budgets are calibrated).</p>
 */
class ModernProfessionalVisualParityTest {

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
                                new IndentedBlock.Item(
                                        "MSc Computer Science",
                                        "University of Manchester | 2021"),
                                new IndentedBlock.Item(
                                        "Oracle Java Certification",
                                        "Professional track | 2023")))))
                .module(CvModule.of("Projects",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item(
                                        "GraphCompose",
                                        "Declarative PDF layout engine for reusable document generation"),
                                new IndentedBlock.Item(
                                        "Template Studio",
                                        "Internal tool for evaluating CV, proposal, and invoice output")))))
                .module(CvModule.of("Professional Experience",
                        new MultiParagraphBlock(List.of(
                                "**Senior Platform Engineer**, Northwind Systems | "
                                        + "*2024-Present* — Led reusable document flows for "
                                        + "reporting, billing, and hiring operations.",
                                "**Software Engineer**, BrightLeaf Labs | *2021-2024* — Built "
                                        + "backend services and production document rendering pipelines."))))
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry(
                                        "Location",
                                        "Based in London and available for hybrid or remote collaboration"),
                                new KeyValueBlock.Entry(
                                        "Interests",
                                        "Platform architecture, DX, and document-quality automation")))))
                .build();
    }

    @Test
    void renderPilotPdfForGlanceComparison() throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve("modern_professional_v2_render_file.pdf");
        Files.deleteIfExists(output);

        DocumentTemplate<CvSpec> template = ModernProfessional.create(THEME);

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
}
