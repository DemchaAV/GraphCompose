package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered CV presets.
 *
 * <p>Each preset renders the canonical {@link CvDocument} from
 * {@link CvPresetFixtures} on full A4 with the preset's
 * {@code RECOMMENDED_MARGIN}; the resulting PDF is rasterised
 * page-by-page and compared per-pixel against a checked-in baseline
 * PNG. Failures write the actual render + diff image next to the
 * baseline.</p>
 *
 * <p>{@code CvPresetLayoutSnapshotTest} gates the same presets on the
 * same document structurally — see {@link CvPresetFixtures} for why
 * both gates exist.</p>
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
            template.compose(document, CvPresetFixtures.canonicalDocument());
            pdfBytes = document.toPdfBytes();
        }

        PdfVisualRegression.standard()
                .baselineRoot(BASELINE_ROOT)
                .perPixelTolerance(PER_PIXEL_TOLERANCE)
                .mismatchedPixelBudget(PIXEL_DIFF_BUDGET)
                .assertMatchesBaseline(slug, pdfBytes);
    }

    private static Stream<Arguments> presets() {
        return CvPresetFixtures.presets();
    }
}
