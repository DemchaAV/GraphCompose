package com.demcha.compose.document.templates.proposal.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pixel-diff visual parity gate for the v2 layered proposal presets.
 *
 * <p>Each preset renders the same canonical {@link ProposalDocumentSpec}
 * on A4 at the preset's {@code RECOMMENDED_MARGIN}; the PDF is rasterised
 * page-by-page and compared per-pixel against a checked-in baseline PNG.
 * {@code ModernProposal} renders the cinematic "modern business" proposal
 * look on a {@code BrandTheme}, so this gate locks that look against drift.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate visual
 * change, re-run with {@code -Dgraphcompose.visual.approve=true} to
 * overwrite the baselines, and commit the updated PNGs in the same change.
 * Baselines live under
 * {@code src/test/resources/visual-baselines/proposal-v2-layered/}.</p>
 */
class ProposalV2VisualParityTest {

    private static final Path BASELINE_ROOT = Path.of(
            "src", "test", "resources", "visual-baselines", "proposal-v2-layered");

    // Mirrors CvV2VisualParityTest / InvoiceV2VisualParityTest: Helvetica is
    // the PDFBox built-in font with the widest cross-platform glyph/colour
    // drift, so the budget is sized generously for Windows vs Linux CI.
    private static final long PIXEL_DIFF_BUDGET = 50_000L;
    private static final int PER_PIXEL_TOLERANCE = 8;

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void rendersWithinPixelDiffBudget(String slug,
                                      double margin,
                                      Supplier<DocumentTemplate<ProposalDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<ProposalDocumentSpec> template = factory.get();
        float m = (float) margin;
        byte[] pdfBytes;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, canonicalProposal());
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
                Arguments.of("modern_proposal",
                        ModernProposal.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<ProposalDocumentSpec>>) ModernProposal::create));
    }

    /**
     * Canonical sample proposal — exercises the hero, executive summary,
     * both parties, body sections, the timeline + pricing tables (with an
     * emphasized total), the acceptance terms, and the footer. Kept inline
     * so the test depends only on main + main-test code.
     */
    private static ProposalDocumentSpec canonicalProposal() {
        return ProposalDocumentSpec.from(ProposalData.builder()
                .title("Proposal")
                .proposalNumber("GC-P-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("30 Apr 2026")
                .projectTitle("Document platform consolidation")
                .executiveSummary("A phased engagement to retire per-team PDF scripts in "
                        + "favour of one canonical, snapshot-tested document engine that "
                        + "serves billing, hiring, and reporting flows.")
                .sender(from -> from
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("hello@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .website("graphcompose.dev"))
                .recipient(to -> to
                        .name("Northwind Systems")
                        .addressLines("Attn: Procurement Team", "410 Market Avenue", "Manchester, UK")
                        .email("procurement@northwind.example")
                        .phone("+44 161 555 2200"))
                .section("Scope of work",
                        "Discovery of the current document estate, a reference architecture, "
                                + "and a production rollout of the canonical engine.",
                        "Migration of the three highest-volume templates with visual parity gates.")
                .section("Approach",
                        "Iterative delivery in two-week increments with weekly checkpoints "
                                + "and a shared visual-regression dashboard.")
                .timelineItem("Discovery", "2 weeks", "Stakeholder interviews + estate audit")
                .timelineItem("Build", "6 weeks", "Engine integration + template migration")
                .timelineItem("Rollout", "2 weeks", "Cutover, training, and handover")
                .pricingRow("Discovery", "Workshops + audit", "GBP 6,000")
                .pricingRow("Build", "Engine + 3 migrations", "GBP 24,000")
                .pricingRow("Rollout", "Cutover + enablement", "GBP 6,000")
                .emphasizedPricingRow("Total", "Fixed-fee engagement", "GBP 36,000")
                .acceptanceTerm("50% on signature, 50% on final delivery.")
                .acceptanceTerm("Fixed-fee; scope changes handled via a written change note.")
                .acceptanceTerm("Valid for 30 days from the prepared date.")
                .footerNote("Thank you for considering GraphCompose for your document platform.")
                .build());
    }
}
