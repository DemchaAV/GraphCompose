package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterHeader;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for every Templates v2 cover-letter
 * preset. Mirrors {@link com.demcha.compose.document.templates.cv.presets.PresetVisualParityTest}
 * but for letters.
 *
 * <p>Each preset renders a fixed canonical {@link CoverLetterSpec} on
 * full A4 with the gallery-standard 48pt margin, the resulting PDF is
 * rasterized via PDFBox, and the per-pixel diff against a checked-in
 * baseline PNG is asserted to stay within the budget specified in
 * {@code docs/private/templates-restructure-plan.md} sec 6.2 (2500
 * mismatched pixels at per-channel tolerance 8). Re-run with
 * {@code -Dgraphcompose.visual.approve=true} to refresh the baselines
 * after a deliberate visual change.</p>
 *
 * <p>Baselines live under
 * {@code src/test/resources/visual-baselines/coverletter-v2/}.</p>
 */
class PresetVisualParityTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "coverletter-v2");

    private static final long PIXEL_DIFF_BUDGET = 2500L;
    private static final int PER_PIXEL_TOLERANCE = 8;
    private static final float MARGIN = 48f;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>> factory) throws Exception {
        DocumentTemplate<CoverLetterSpec> template = factory.apply(THEME);
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(MARGIN, MARGIN, MARGIN, MARGIN)
                .create()) {
            template.compose(document, canonicalLetterSpec());
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
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ModernProfessionalLetter::create),
                Arguments.of("nordic_clean",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) NordicCleanLetter::create),
                Arguments.of("classic_serif",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ClassicSerifLetter::create),
                Arguments.of("compact_mono",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) CompactMonoLetter::create),
                Arguments.of("executive",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ExecutiveLetter::create),
                Arguments.of("engineering_resume",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) EngineeringResumeLetter::create),
                Arguments.of("timeline_minimal",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) TimelineMinimalLetter::create),
                Arguments.of("boxed_sections",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) BoxedSectionsLetter::create),
                Arguments.of("centered_headline",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) CenteredHeadlineLetter::create),
                Arguments.of("blue_banner",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) BlueBannerLetter::create),
                Arguments.of("editorial_blue",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) EditorialBlueLetter::create),
                Arguments.of("panel",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) PanelLetter::create),
                Arguments.of("sidebar_portrait",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) SidebarPortraitLetter::create),
                Arguments.of("monogram_sidebar",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) MonogramSidebarLetter::create));
    }

    private static CoverLetterSpec canonicalLetterSpec() {
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
                        + "on building **reusable document-generation systems**.")
                .paragraph("I enjoy translating fuzzy workflow requirements into "
                        + "clear template abstractions and reliable test coverage.")
                .paragraph("I would welcome the opportunity to bring that mix "
                        + "of engineering rigor and product thinking to your team.")
                .closing("Sincerely, *Artem Demchyshyn*")
                .build();
    }
}
