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
 * <p>Each is asserted the way the engine's own scenarios are. The layout snapshot pins
 * <b>node geometry</b> — each node's box and the pages it spans — so a paragraph that grows
 * a line, or content that starts breaking a page earlier, names the node that moved. It
 * records nothing below the node, so the pixel baseline is what covers everything inside the
 * box: ordering, joining, the glyphs themselves.</p>
 *
 * <p>The test composes through each example's own {@code compose} and {@code open}, so it
 * holds the document the example writes rather than a copy that can drift from it.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} for the coordinates and
 * {@code -Dgraphcompose.visual.approve=true} for the pixels, after looking at the render.</p>
 */
class RtlExampleVisualTest {

    /**
     * Scaled to the page, because a fixed budget does not guard a small one.
     *
     * <p>A4 at 72 dpi is about 500 000 pixels; one part in seventy leaves room for the
     * antialiasing drift between platforms that the per-pixel tolerance does not absorb,
     * while still failing on a line that moved or a script that stopped joining.</p>
     */
    private static PdfVisualRegression visual(int width, int height) {
        return PdfVisualRegression.standard()
                .perPixelTolerance(6)
                .mismatchedPixelBudget((long) width * height / 70);
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
