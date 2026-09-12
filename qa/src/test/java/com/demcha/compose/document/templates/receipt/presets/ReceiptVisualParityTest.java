package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the layered receipt presets.
 *
 * <p>The preset renders the canonical {@link ReceiptDocumentSpec} on A4 at
 * {@code RECOMMENDED_MARGIN}; the PDF is rasterised page-by-page and compared
 * per-pixel against a checked-in baseline PNG. The receipt look is carried by
 * hairlines, a status chip, and one very large amount — all of which move
 * silently under a theme or widget refactor, which is what this locks.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} and commit
 * the updated PNGs in the same change. Baselines live under
 * {@code src/test/resources/visual-baselines/receipt-layered/}.</p>
 */
class ReceiptVisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "receipt-layered");

    // Mirrors the invoice gate: Helvetica is the PDFBox built-in font with the
    // widest cross-platform glyph/colour drift, so the budget is sized for
    // Windows-recorded baselines against Linux CI.
    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<ReceiptDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<ReceiptDocumentSpec> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, ReceiptFixtures.canonicalReceipt());
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
                Arguments.of("modern_receipt",
                        ModernReceipt.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<ReceiptDocumentSpec>>)
                                ReceiptVisualParityTest::brandedModernReceipt));
    }

    /**
     * The branded factory, so the gate covers the accent surfaces — hero
     * strip, direction arrow, reached timeline step — and not only the
     * unbranded fallback. No mark: an SVG asset would put a second file in
     * front of the baseline.
     */
    private static DocumentTemplate<ReceiptDocumentSpec> brandedModernReceipt() {
        return ModernReceipt.create(BrandTheme.receiptModern(),
                ModernReceipt.Options.branded(null, DocumentColor.rgb(23, 92, 211)));
    }

}
