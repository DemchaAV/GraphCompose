package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Layout snapshot regression test for every Templates v2 CV preset.
 *
 * <p>Each preset composes a fixed sample {@link CvSpec} into an
 * in-memory session and the resulting layout-graph snapshot is
 * compared against a checked-in baseline JSON. A snapshot mismatch
 * means the preset's rendered tree drifted — either intentionally
 * (re-run with {@code -Dgraphcompose.updateSnapshots=true} to refresh
 * the baseline) or as a regression.</p>
 */
class PresetLayoutSnapshotTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static CvSpec canonicalCvSpec() {
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
                /* Note: this snapshot test intentionally exercises the
                 * legacy MultiParagraphBlock pipe-separated string path
                 * to pin the parser's em-dash / en-dash / ASCII-hyphen
                 * separator handling. The structurally equivalent
                 * preferred path uses WorkHistoryBlock — see
                 * PresetVisualGalleryTest#sampleSpec for the canonical
                 * example. Both paths converge on renderWorkEntry and
                 * produce the same LayoutGraph for equivalent data. */
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry("Location",
                                        "Based in London and available for hybrid or remote collaboration"),
                                new KeyValueBlock.Entry("Interests",
                                        "Platform architecture, DX, and document-quality automation")))))
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void shouldMatchLayoutSnapshot(String slug,
                                   Function<BusinessTheme, DocumentTemplate<CvSpec>> factory) throws Exception {
        DocumentTemplate<CvSpec> template = factory.apply(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, canonicalCvSpec());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug, "cv-v2");
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("template_v2_cv_modern_professional",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) ModernProfessional::create),
                Arguments.of("template_v2_cv_nordic_clean",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) NordicClean::create),
                Arguments.of("template_v2_cv_classic_serif",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) ClassicSerif::create),
                Arguments.of("template_v2_cv_compact_mono",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) CompactMono::create),
                Arguments.of("template_v2_cv_executive",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) Executive::create),
                Arguments.of("template_v2_cv_engineering_resume",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) EngineeringResume::create),
                Arguments.of("template_v2_cv_timeline_minimal",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) TimelineMinimal::create),
                Arguments.of("template_v2_cv_boxed_sections",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) BoxedSections::create),
                Arguments.of("template_v2_cv_centered_headline",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) CenteredHeadline::create),
                Arguments.of("template_v2_cv_blue_banner",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) BlueBanner::create),
                Arguments.of("template_v2_cv_editorial_blue",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) EditorialBlue::create),
                Arguments.of("template_v2_cv_panel",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) Panel::create),
                Arguments.of("template_v2_cv_sidebar_portrait",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) SidebarPortrait::create),
                Arguments.of("template_v2_cv_monogram_sidebar",
                        (Function<BusinessTheme, DocumentTemplate<CvSpec>>) MonogramSidebar::create));
    }
}
