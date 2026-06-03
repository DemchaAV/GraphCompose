package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered cover-letter
 * presets — the letter sibling of {@code CvV2VisualParityTest}.
 *
 * <p>Each preset renders the same canonical {@link CoverLetterDocument}
 * on full A4 with the preset's {@code RECOMMENDED_MARGIN}; the
 * resulting PDF is rasterised page-by-page and compared per-pixel
 * against a checked-in baseline PNG. Failures write the actual render +
 * diff image next to the baseline.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate
 * visual change, re-run with
 * {@code -Dgraphcompose.visual.approve=true} (or environment variable
 * {@code GRAPHCOMPOSE_VISUAL_APPROVE=true}) to overwrite the baselines
 * with the current rendering. Commit the updated PNGs as part of the
 * same change.</p>
 *
 * <p>Baselines live under
 * {@code src/test/resources/visual-baselines/coverletter-v2-layered/}.
 * Budget mirrors {@code CvV2VisualParityTest} (50 000 mismatched
 * pixels at per-channel tolerance 8) — sized for the worst-case
 * Helvetica cross-platform drift between Windows-recorded baselines
 * and Linux CI.</p>
 */
class CoverLetterV2VisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "coverletter-v2-layered");

    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<CoverLetterDocument>> factory)
            throws Exception {
        DocumentTemplate<CoverLetterDocument> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, canonicalLetter());
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
                Arguments.of("executive",
                        ExecutiveLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ExecutiveLetter::create),
                Arguments.of("modern_professional",
                        ModernProfessionalLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ModernProfessionalLetter::create),
                Arguments.of("boxed_sections",
                        BoxedSectionsLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) BoxedSectionsLetter::create),
                Arguments.of("classic_serif",
                        ClassicSerifLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ClassicSerifLetter::create),
                Arguments.of("editorial_blue",
                        EditorialBlueLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) EditorialBlueLetter::create),
                Arguments.of("centered_headline",
                        CenteredHeadlineLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) CenteredHeadlineLetter::create),
                Arguments.of("blue_banner",
                        BlueBannerLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) BlueBannerLetter::create),
                Arguments.of("engineering_resume",
                        EngineeringResumeLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) EngineeringResumeLetter::create),
                Arguments.of("panel",
                        PanelLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) PanelLetter::create),
                Arguments.of("compact_mono",
                        CompactMonoLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) CompactMonoLetter::create),
                Arguments.of("nordic_clean",
                        NordicCleanLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) NordicCleanLetter::create),
                Arguments.of("sidebar_portrait",
                        SidebarPortraitLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) SidebarPortraitLetter::create),
                Arguments.of("monogram_sidebar",
                        MonogramSidebarLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) MonogramSidebarLetter::create),
                Arguments.of("timeline_minimal",
                        TimelineMinimalLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) TimelineMinimalLetter::create),
                Arguments.of("mint-editorial-letter",
                        MintEditorialLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) MintEditorialLetter::create));
    }

    /**
     * Canonical sample letter — the same Jordan Rivera identity as
     * {@code CvV2VisualParityTest} so the letter masthead is verified
     * against the same content the CV gate uses, plus a greeting, three
     * body paragraphs with inline markdown, and a closing.
     *
     * <p>Kept inline (not pulled from the examples module) so the test
     * depends only on main + main-test code.</p>
     */
    private static CoverLetterDocument canonicalLetter() {
        return CoverLetterDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000",
                                "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
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
                .closing("Sincerely,")
                .build();
    }
}
