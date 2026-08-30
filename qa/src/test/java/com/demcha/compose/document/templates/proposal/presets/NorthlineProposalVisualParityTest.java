package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Pixel-diff visual parity gate for {@link NorthlineProposal} — both pages
 * of the canonical fixture against checked-in baselines.
 *
 * <p>Separate from {@code ProposalV2VisualParityTest} because that gate is
 * parameterised on the narrative {@code ProposalDocumentSpec} while this
 * preset consumes the structured spec. Budget and tolerance mirror the
 * other template parity gates (Lato/Spectral are bundled TTFs, the same
 * cross-platform drift class as the CV presets' faces).</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} and
 * commit the updated PNGs with the change. Baselines live under
 * {@code src/test/resources/visual-baselines/proposal-v2-layered/}.</p>
 */
class NorthlineProposalVisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "proposal-v2-layered");

    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @Test
    void rendersWithinPixelDiffBudget() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document().create()) {
            NorthlineProposal.create().compose(
                    document, NorthlineProposalFixtures.canonicalProposal());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline("northline_proposal", pdfBytes);
    }
}
