package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Pixel-diff visual parity gate for {@link CobaltRota} — the canonical
 * fixture's page against a checked-in baseline.
 *
 * <p><strong>What this gate does and does not hold.</strong> The sheet is
 * composed table cells almost end to end and the layout snapshot sees two nodes
 * for the whole of it, so this baseline is the only thing that sees the sheet at
 * all. It is not, on its own, enough: the page is about half a million pixels
 * and one chip is under two thousand of them, so a budget wide enough to absorb
 * a renderer's antialiasing is wide enough to hide several chips. The budget
 * below is therefore a fraction of the sibling presets', and what pins the chips
 * themselves is
 * {@code CobaltRotaSmokeTest.everyStatusIsDrawnInItsOwnColourAndCoversTheAreaItShould},
 * which counts each status's ink directly.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual change,
 * re-run with {@code -Dgraphcompose.visual.approve=true} and commit the updated
 * PNGs with the change. Baselines live under
 * {@code src/test/resources/visual-baselines/rota/}.</p>
 */
class CobaltRotaVisualParityTest {

    private static final Path BASELINE_ROOT =
            Path.of("src", "test", "resources", "visual-baselines", "rota");

    /**
     * Tight, because the sheet is one deterministic table of solid fills and
     * type: what varies between platforms is the antialiasing of the glyph edges
     * and nothing else. Two thousand pixels is about one chip.
     */
    private static final long PIXEL_DIFF_BUDGET = 2_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @Test
    void rendersWithinPixelDiffBudget() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document().create()) {
            CobaltRota.create().compose(document, CobaltRotaFixtures.canonicalRota());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline("cobalt_rota", pdfBytes);
    }
}
