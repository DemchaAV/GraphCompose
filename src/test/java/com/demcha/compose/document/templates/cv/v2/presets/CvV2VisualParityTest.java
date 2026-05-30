package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvSkill;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered CV presets.
 *
 * <p>Each preset renders the same canonical {@link CvDocument} on
 * full A4 with the preset's {@code RECOMMENDED_MARGIN}; the resulting
 * PDF is rasterised page-by-page and compared per-pixel against a
 * checked-in baseline PNG. Failures write the actual render +
 * diff image next to the baseline.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate
 * visual change, re-run with
 * {@code -Dgraphcompose.visual.approve=true} (or environment variable
 * {@code GRAPHCOMPOSE_VISUAL_APPROVE=true}) to overwrite the
 * baselines with the current rendering. Commit the updated PNGs as
 * part of the same change.</p>
 *
 * <p>Baselines live under
 * {@code src/test/resources/visual-baselines/cv-v2-layered/}. Budget
 * mirrors the v1 {@code PresetVisualParityTest} (20 000 mismatched
 * pixels at per-channel tolerance 8) — calibrated for cross-platform
 * PDFBox font + colour rendering drift between Windows-recorded
 * baselines and Linux CI.</p>
 */
class CvV2VisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "cv-v2-layered");

    // Calibrated against the worst observed cross-platform drift:
    // ModernProfessional renders ~40k mismatched pixels on Linux CI
    // vs Windows-recorded baseline because Helvetica is the only
    // PDFBox built-in font where text glyph outlines + base colours
    // differ noticeably between platforms (the PT-Serif presets hit
    // ~5-10k). Budget sized to cover the MP case with margin —
    // tighter per-preset overrides can be introduced later if drift
    // patterns diverge.
    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {
        DocumentTemplate<CvDocument> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, canonicalDocument());
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
                Arguments.of("boxed_sections",
                        BoxedSections.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) BoxedSections::create),
                Arguments.of("minimal_underlined",
                        MinimalUnderlined.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) MinimalUnderlined::create),
                Arguments.of("modern_professional",
                        ModernProfessional.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) ModernProfessional::create),
                Arguments.of("nordic_clean",
                        NordicClean.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) NordicClean::create),
                Arguments.of("centered_headline",
                        CenteredHeadline.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) CenteredHeadline::create),
                Arguments.of("blue_banner",
                        BlueBanner.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) BlueBanner::create),
                Arguments.of("editorial_blue",
                        EditorialBlue.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) EditorialBlue::create),
                Arguments.of("classic_serif",
                        ClassicSerif.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) ClassicSerif::create),
                Arguments.of("compact_mono",
                        CompactMono.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) CompactMono::create),
                Arguments.of("executive",
                        Executive.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) Executive::create),
                Arguments.of("panel",
                        Panel.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) Panel::create),
                Arguments.of("timeline_minimal",
                        TimelineMinimal.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) TimelineMinimal::create),
                Arguments.of("engineering_resume",
                        EngineeringResume.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) EngineeringResume::create),
                Arguments.of("monogram_sidebar",
                        MonogramSidebar.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) MonogramSidebar::create),
                Arguments.of("sidebar_portrait",
                        SidebarPortrait.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) SidebarPortrait::create),
                Arguments.of("mint_editorial",
                        MintEditorial.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) MintEditorial::create));
    }

    /**
     * Canonical sample document — Jordan Rivera with every v2 section
     * subtype exercised so the gate covers paragraph, grouped
     * skills, row styles, and timeline entries.
     *
     * <p>Kept inline (not pulled from the examples module) so the
     * test depends only on main + main-test code.</p>
     */
    private static CvDocument canonicalDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000",
                                "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .section(new ParagraphSection("Professional Summary",
                        "Platform engineer with **10+ years** building resilient "
                                + "document-generation pipelines, layout engines, and "
                                + "developer-facing template systems. Specialised in "
                                + "high-throughput PDF rendering, semantic authoring "
                                + "DSLs, and turning brittle production-ops scripts "
                                + "into typed, snapshot-tested libraries that scale."))
                // Skill names match the name-only form exactly; only
                // optional proficiency levels are added so data-driven
                // presets (Mint Editorial) can render meters. Name-only
                // presets read SkillGroup.skills() and render identically,
                // so the existing baselines are unaffected.
                .section(SkillsSection.builder("Technical Skills")
                        .leveledGroup("Languages", List.of(
                                CvSkill.of("Java 21", 0.95),
                                CvSkill.of("Kotlin", 0.85),
                                CvSkill.of("Groovy", 0.7),
                                CvSkill.of("Python", 0.75),
                                CvSkill.of("SQL", 0.8)))
                        .leveledGroup("Document & Print", List.of(
                                CvSkill.of("PDFBox", 0.9),
                                CvSkill.of("Apache POI (DOCX/XLSX)", 0.7),
                                CvSkill.of("iText", 0.65),
                                CvSkill.of("PostScript", 0.6),
                                CvSkill.of("ICC colour profiles", 0.55),
                                CvSkill.of("font metrics", 0.7)))
                        .leveledGroup("Layout engines", List.of(
                                CvSkill.of("Custom DSL design", 0.9),
                                CvSkill.of("semantic layout trees", 0.85),
                                CvSkill.of("pagination", 0.85),
                                CvSkill.of("snapshot testing", 0.8),
                                CvSkill.of("visual regression", 0.8)))
                        .leveledGroup("Build & infrastructure", List.of(
                                CvSkill.of("Maven", 0.9),
                                CvSkill.of("Gradle", 0.75),
                                CvSkill.of("GitHub Actions", 0.85),
                                CvSkill.of("JitPack", 0.8),
                                CvSkill.of("Docker", 0.7),
                                CvSkill.of("JMH benchmarking", 0.65)))
                        .leveledGroup("Testing", List.of(
                                CvSkill.of("JUnit 5", 0.9),
                                CvSkill.of("AssertJ", 0.85),
                                CvSkill.of("PDFBox-based PNG diff", 0.8),
                                CvSkill.of("layout-graph snapshots", 0.8),
                                CvSkill.of("mutation testing (Pitest)", 0.6)))
                        .leveledGroup("Distribution", List.of(
                                CvSkill.of("Maven Central", 0.8),
                                CvSkill.of("Sonatype OSSRH", 0.75),
                                CvSkill.of("GPG signing", 0.7),
                                CvSkill.of("JitPack", 0.8),
                                CvSkill.of("semantic versioning discipline", 0.85)))
                        .build())
                .section(EntriesSection.builder("Education & Certifications")
                        .entry("MSc Computer Science",
                                "University of Manchester",
                                "2019-2021",
                                "Distinction. Thesis: *Composable layout primitives "
                                        + "for deterministic document rendering*.")
                        .entry("BSc Software Engineering",
                                "Imperial College London",
                                "2015-2019",
                                "First-class honours. Specialisation in compilers and "
                                        + "static analysis.")
                        .entry("Oracle Java Certification",
                                "Professional track",
                                "2023-2024",
                                "Java 17 platform deep-dive: records, sealed types, "
                                        + "pattern matching, virtual threads.")
                        .build())
                .section(RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                        .row("GraphCompose (Java 21, PDFBox, Maven, JMH)",
                                "Declarative Java PDF layout engine. Semantic DSL, "
                                        + "slot-based templates, snapshot testing. Powers "
                                        + "production CV / invoice / proposal pipelines for "
                                        + "hiring tools and billing systems. *(Open source)*")
                        .row("Template Studio (Kotlin, Compose Desktop, PDFBox PNG diff)",
                                "Internal tool for evaluating CV, proposal, and "
                                        + "invoice output across 14 design presets. PNG "
                                        + "diffing, side-by-side layout, baseline freezing.")
                        .row("LayoutLint (Java 21, JavaParser, Spoon)",
                                "Static analyser that flags fragile authoring patterns "
                                        + "(deeply nested rows, untyped offsets, implicit "
                                        + "page breaks) before they ship to production.")
                        .row("ChromeForge (Java, GraphCompose, Pandoc bridge)",
                                "Editorial-magazine document toolkit built on "
                                        + "GraphCompose: cinematic covers, pull quotes, "
                                        + "multi-column flow, sidebar callouts.")
                        .build())
                .section(EntriesSection.builder("Professional Experience")
                        .entry("Senior Platform Engineer",
                                "Northwind Systems",
                                "2024-Present",
                                "Led the reusable document-generation platform serving "
                                        + "billing, hiring, and reporting flows across "
                                        + "**8 product teams**. Reduced template maintenance "
                                        + "time by **70%** by retiring per-team PDF scripts "
                                        + "in favour of one canonical engine.")
                        .entry("Software Engineer",
                                "BrightLeaf Labs",
                                "2021-2024",
                                "Built backend services and production document rendering "
                                        + "pipelines processing **2M+ documents per month**. "
                                        + "Drove the migration from iText to a custom layout "
                                        + "engine, eliminating licensing risk and cutting "
                                        + "p99 render latency from 1.4s to 380ms.")
                        .entry("Backend Engineer",
                                "Helix Print Co",
                                "2019-2021",
                                "Maintained a high-volume invoice-printing service "
                                        + "(15M PDFs/year) and authored the compliance test "
                                        + "harness that gated every template change.")
                        .build())
                .section(RowsSection.builder("Additional Information", RowStyle.PLAIN)
                        .row("Languages",
                                "English (Fluent), German (Intermediate), Spanish (Basic)")
                        .row("Work Eligibility",
                                "Eligible to work in the UK and the EU")
                        .row("Open Source",
                                "Maintainer of GraphCompose. Regular contributor to "
                                        + "PDFBox issue triage.")
                        .row("Speaking",
                                "JVM Summit 2024, Devoxx UK 2025 — both on declarative "
                                        + "document layout.")
                        .build())
                .build();
    }
}
