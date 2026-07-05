package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered invoice presets.
 *
 * <p>Each preset renders the same canonical {@link InvoiceDocumentSpec}
 * on A4 at the preset's {@code RECOMMENDED_MARGIN}; the PDF is rasterised
 * page-by-page and compared per-pixel against a checked-in baseline PNG.
 * {@code ModernInvoice} renders the cinematic "modern business" invoice
 * look on a {@code BrandTheme}, so this gate locks that look against drift.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} to
 * overwrite the baselines, and commit the updated PNGs in the same change.
 * Baselines live under
 * {@code src/test/resources/visual-baselines/invoice-v2-layered/}.</p>
 */
class InvoiceV2VisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "invoice-v2-layered");

    // Mirrors CvV2VisualParityTest: Helvetica is the PDFBox built-in font
    // with the widest cross-platform glyph/colour drift, so the budget is
    // sized generously for Windows-recorded vs Linux-CI rendering.
    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<InvoiceDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<InvoiceDocumentSpec> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, canonicalInvoice());
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
                Arguments.of("modern_invoice",
                        ModernInvoice.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<InvoiceDocumentSpec>>) ModernInvoice::create));
    }

    /**
     * Canonical sample invoice — exercises the hero, both parties, a
     * multi-row line-items table, subtotal / tax / total summary, and the
     * notes / payment-terms footer. Kept inline so the test depends only
     * on main + main-test code.
     */
    private static InvoiceDocumentSpec canonicalInvoice() {
        return InvoiceDocumentSpec.from(InvoiceData.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .status("Pending")
                .fromParty(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Finance Team", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200")
                        .taxId("NW-2026-01"))
                .lineItem("Discovery workshop", "Stakeholder interviews",
                        "1", "GBP 1,450", "GBP 1,450")
                .lineItem("Template architecture", "Reusable document flows",
                        "2", "GBP 980", "GBP 1,960")
                .lineItem("Render QA", "Cross-platform pixel diffing",
                        "3", "GBP 320", "GBP 960")
                .lineItem("Developer enablement", "Authoring docs + examples",
                        "1", "GBP 780", "GBP 780")
                .summaryRow("Subtotal", "GBP 5,150")
                .summaryRow("VAT (20%)", "GBP 1,030")
                .totalRow("Total", "GBP 6,180")
                .note("Please include the invoice number on your remittance advice.")
                .note("All work was delivered as agreed during the April implementation window.")
                .paymentTerm("Payment due within 14 calendar days.")
                .paymentTerm("Bank transfer preferred; contact billing@graphcompose.dev for remittance details.")
                .paymentTerm("Late payments may delay additional template customization work.")
                .footerNote("Thank you for choosing GraphCompose for production document rendering.")
                .build());
    }
}
