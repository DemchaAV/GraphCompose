package com.demcha.examples;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import com.demcha.examples.features.text.ArabicArticleExample;
import com.demcha.examples.features.text.HebrewInvoiceExample;
import com.demcha.examples.features.text.WorldScriptsExample;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 * Holds the published right-to-left examples themselves, against coordinates and pixels.
 *
 * <p>These documents already have a guard: their previews are committed, and
 * {@code CommittedAssetDriftTest} compares the bytes. That catches everything and explains
 * nothing — a failure says the file differs, not which line moved or what changed on the
 * page. These are the documents a reader actually sees, so they deserve the diagnostic the
 * mechanics get.</p>
 *
 * <p>Each is asserted the way the engine's own scenarios are, with one honest difference in
 * what the pixel half can promise. The layout snapshot pins <b>node geometry</b> — each
 * node's box and the pages it spans — so a paragraph that grows a line names the node that
 * moved. The pixel baseline catches gross visual breakage and, when a change is deliberate,
 * shows what it looked like; <b>exactness is the byte guard's job</b>. These pages are A4,
 * and different platforms rasterise their glyphs differently enough (7,886 differing pixels
 * on the script catalogue between this baseline and CI's JDK, measured) that a budget tight
 * enough to catch a reordering regression on the quietest page (7,722, measured with the
 * reversal disabled) would fail an honest render on another machine. The engine-level
 * ordering guard lives in {@code RtlScenariosVisualTest} on pages small enough for the two
 * numbers to separate; here, any content change at all — reordering included — fails
 * {@code CommittedAssetDriftTest} on the bytes, which no rasteriser can blur.</p>
 *
 * <p>The test composes through each example's own {@code compose} and {@code open}, so it
 * holds the document the example writes rather than a copy that can drift from it.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} for the coordinates and
 * {@code -Dgraphcompose.visual.approve=true} for the pixels, after looking at the render.</p>
 */
class RtlExampleVisualTest {

    /**
     * Scaled to the page, and calibrated to the drift a different rasteriser produces.
     *
     * <p>One part in forty-five of an A4 page is about 11,100 pixels: 1.4× the largest
     * honest cross-platform drift measured on these documents, and 1.5× under the smallest
     * failure a disabled reversal produces on the pages whose pixel guard means anything.
     * The class comment carries the numbers and what this budget deliberately does not
     * promise.</p>
     */
    private static PdfVisualRegression visual(int width, int height) {
        return PdfVisualRegression.standard()
                .perPixelTolerance(6)
                .mismatchedPixelBudget((long) width * height / 45);
    }

    /** A4 at the 72 dpi the regression harness rasterises at. */
    private static final int A4_WIDTH = 595;
    private static final int A4_HEIGHT = 842;

    @Test
    void theArabicArticleIsUnchanged() throws Exception {
        assertExample("arabic-article", ArabicArticleExample::compose, ArabicArticleExample::open);
    }

    @Test
    void theHebrewInvoiceIsUnchanged() throws Exception {
        assertExample("hebrew-invoice", HebrewInvoiceExample::compose, HebrewInvoiceExample::open);
    }

    @Test
    void theScriptCatalogueIsUnchanged() throws Exception {
        assertExample("world-scripts", WorldScriptsExample::compose, WorldScriptsExample::open);
    }

    private static void assertExample(String name,
                                      Consumer<DocumentSession> compose,
                                      java.util.function.Function<java.nio.file.Path, DocumentSession> open)
            throws Exception {

        try (DocumentSession document = open.apply(null)) {
            compose.accept(document);

            LayoutSnapshotAssertions.assertMatches(document, "examples/rtl/" + name);
            visual(A4_WIDTH, A4_HEIGHT).assertMatchesBaseline("example-" + name, document);
        }
    }
}
