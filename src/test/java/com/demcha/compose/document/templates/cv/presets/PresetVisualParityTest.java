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
import com.demcha.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for every Templates v2 CV preset.
 *
 * <p>Each preset renders a fixed canonical {@link CvSpec} on full A4
 * with the preset's {@code RECOMMENDED_MARGIN}, the resulting PDF is
 * rasterized via PDFBox, and the per-pixel diff against a checked-in
 * baseline PNG is asserted to stay within the budget specified in
 * {@code docs/private/templates-restructure-plan.md} sec 6.2 (2500
 * mismatched pixels at per-channel tolerance 8). Re-run with
 * {@code -Dgraphcompose.visual.approve=true} to refresh the baselines
 * after a deliberate visual change.</p>
 *
 * <p>Baselines live under
 * {@code src/test/resources/visual-baselines/cv-v2/}. On failure the
 * harness writes {@code <slug>-page-0.actual.png} and
 * {@code <slug>-page-0.diff.png} next to the baseline.</p>
 */
class PresetVisualParityTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "cv-v2");

    // Calibrated for cross-platform PDFBox font + colour rendering
    // drift between Windows-recorded baselines and Linux CI. Budget
    // sized to cover the v1.6.0 CI observation (worst preset
    // modern_professional / editorial_blue at ~15.6k mismatched
    // pixels = 3.1% of 595x841) with a comfortable margin.
    private static final long PIXEL_DIFF_BUDGET = 20000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Function<BusinessTheme, DocumentTemplate<CvSpec>> factory) throws Exception {
        DocumentTemplate<CvSpec> template = factory.apply(THEME);
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, canonicalCvSpec());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline(slug, pdfBytes);
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("modern_professional",
                        ModernProfessional.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) ModernProfessional::create),
                Arguments.of("nordic_clean",
                        NordicClean.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) NordicClean::create),
                Arguments.of("classic_serif",
                        ClassicSerif.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) ClassicSerif::create),
                Arguments.of("compact_mono",
                        CompactMono.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) CompactMono::create),
                Arguments.of("executive",
                        Executive.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) Executive::create),
                Arguments.of("engineering_resume",
                        EngineeringResume.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) EngineeringResume::create),
                Arguments.of("timeline_minimal",
                        TimelineMinimal.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) TimelineMinimal::create),
                Arguments.of("boxed_sections",
                        BoxedSections.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) BoxedSections::create),
                Arguments.of("centered_headline",
                        CenteredHeadline.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) CenteredHeadline::create),
                Arguments.of("blue_banner",
                        BlueBanner.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) BlueBanner::create),
                Arguments.of("editorial_blue",
                        EditorialBlue.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) EditorialBlue::create),
                Arguments.of("panel",
                        Panel.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) Panel::create),
                Arguments.of("sidebar_portrait",
                        SidebarPortrait.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) SidebarPortrait::create),
                Arguments.of("monogram_sidebar",
                        MonogramSidebar.RECOMMENDED_MARGIN,
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) MonogramSidebar::create));
    }

    private static CvSpec canonicalCvSpec() {
        return CvSpec.builder()
                .header(CvHeader.builder()
                        .name("Artem Demchyshyn")
                        .jobTitle("Backend Java Developer")
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
}
