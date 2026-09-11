package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatus;
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
            template.compose(document, canonicalReceipt());
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

    /**
     * Canonical sample receipt — exercises the masthead, the hero with its
     * status chip and summary fields, both party sides, two detail groups,
     * the status trail, the notes block, and the footer with its QR code.
     * Kept inline so the test depends only on main code.
     */
    private static ReceiptDocumentSpec canonicalReceipt() {
        return ReceiptDocumentSpec.of(receipt -> receipt
                .documentTitle("Transfer confirmation")
                .issuerName("Northwind Pay")
                .generatedOn("09 August 2026")
                .reference("NWP-4821-0067")
                .amount("Amount collected", "£66.62")
                .amountCaption("Direct Debit collected by Harbour Finance Ltd")
                .status(ReceiptStatus.settled("Completed"))
                .summaryField("Value date", "07 Jul 2026")
                .summaryField("Operation date", "07 Jul 2026")
                .summaryField("Scheme", "Bacs Direct Debit")
                .payer("Paid from", party -> party
                        .name("Alex Sample")
                        .addressLines("12 Example Way", "Brentford TW0 0AA", "United Kingdom")
                        .field("Account", "•••• 4396")
                        .field("Sort code", "00-00-00"))
                .beneficiary("Paid to", party -> party
                        .name("Harbour Finance Ltd")
                        .addressLines("1 Sample Quay", "Manchester M0 0AA")
                        .field("Account", "•••• 5604")
                        .field("Sort code", "00-00-11"))
                .detailGroup("Transfer details", group -> group
                        .field("Mandate reference", "MND-0110-8054-5652")
                        .field("Transaction ID", "b71f0c2e-9a44-4f18-bd30-51c7a2e9d840")
                        .field("Payment scheme", "Bacs Direct Debit")
                        .field("Statement description", "HARBOUR FIN 4821"))
                .detailGroup("Amount breakdown", group -> group
                        .field("Amount", "£66.62")
                        .field("Transfer fee", "£0.00")
                        .field("Exchange rate", "1.00 GBP / GBP")
                        .emphasized("Total debited", "£66.62"))
                .event("Instructed", "07 Jul 2026, 08:12 BST",
                        "Collection request submitted to the scheme.")
                .event("In clearing", "08 Jul 2026, 09:00 BST",
                        "Three-working-day Bacs cycle.")
                .event("Settled", "09 Jul 2026, 06:30 BST",
                        "Funds debited and confirmed by the receiving bank.")
                .note("Keep this confirmation for your records. It is not a tax invoice.")
                .verification("https://example.com/verify/NWP-4821-0067",
                        "Scan to check this confirmation against the issuing records.")
                .supportLine("Support  +44 20 0000 0000")
                .supportLine("help@northwind-pay.example")
                .legalNote("Northwind Pay is a fictional institution used in GraphCompose "
                        + "examples; every name, account, and reference on this page is "
                        + "invented."));
    }
}
