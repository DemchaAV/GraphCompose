package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered proposal presets.
 *
 * <p>Each preset renders the canonical {@link ProposalDocumentSpec}
 * from {@link ProposalPresetFixtures} on A4 at the preset's
 * {@code RECOMMENDED_MARGIN}; the PDF is rasterised page-by-page and
 * compared per-pixel against a checked-in baseline PNG.
 * {@code ModernProposal} renders the cinematic "modern business" proposal
 * look on a {@code BrandTheme}, so this gate locks that look against drift.</p>
 *
 * <p>{@code ProposalPresetLayoutSnapshotTest} gates the same presets on
 * the same proposal structurally.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} to
 * overwrite the baselines, and commit the updated PNGs in the same change.
 * Baselines live under
 * {@code src/test/resources/visual-baselines/proposal-v2-layered/}.</p>
 */
class ProposalV2VisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "proposal-v2-layered");

    // Mirrors CvV2VisualParityTest / InvoiceV2VisualParityTest: Helvetica is
    // the PDFBox built-in font with the widest cross-platform glyph/colour
    // drift, so the budget is sized generously for Windows vs Linux CI.
    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<ProposalDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<ProposalDocumentSpec> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, ProposalPresetFixtures.canonicalProposal());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline(slug, pdfBytes);
    }

    private static Stream<Arguments> presets() {
        return ProposalPresetFixtures.presets();
    }
}
