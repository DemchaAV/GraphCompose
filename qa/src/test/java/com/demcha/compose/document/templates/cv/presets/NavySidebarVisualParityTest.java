package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Pixel-diff visual parity gate for {@link NavySidebar} - the canonical
 * one-page CV against a checked-in baseline.
 *
 * <p><strong>Re-blessing baselines</strong> - after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} and commit
 * the updated PNG with the change. Baselines live under
 * {@code src/test/resources/visual-baselines/cv-v2-layered/}.</p>
 */
class NavySidebarVisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "cv-v2-layered");

    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @Test
    void rendersWithinPixelDiffBudget() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document().create()) {
            NavySidebar.create().compose(document, NavySidebarFixtures.canonicalCv());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline("navy_sidebar", pdfBytes);
    }
}
