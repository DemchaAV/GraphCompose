package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
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
 * <p>Each preset renders the canonical {@link CoverLetterDocument}
 * from {@link CoverLetterPresetFixtures} on full A4 with the preset's
 * {@code RECOMMENDED_MARGIN}; the resulting PDF is rasterised
 * page-by-page and compared per-pixel against a checked-in baseline
 * PNG. Failures write the actual render + diff image next to the
 * baseline.</p>
 *
 * <p>{@code CoverLetterPresetLayoutSnapshotTest} gates the same presets
 * on the same letter structurally — see {@code CvPresetFixtures} for
 * why both gates exist.</p>
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
            template.compose(document, CoverLetterPresetFixtures.canonicalLetter());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline(slug, pdfBytes);
    }

    private static Stream<Arguments> presets() {
        return CoverLetterPresetFixtures.presets();
    }
}
