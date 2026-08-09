package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatus;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatusTone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the layered receipt pipeline through {@link ModernReceipt}
 * — proves the preset renders a {@link ReceiptDocumentSpec} end-to-end on a
 * {@link BrandTheme}, through every factory, under a foreign theme, for each
 * status tone, and on a receipt carrying nothing but an amount.
 */
class ModernReceiptSmokeTest {

    private static ReceiptDocumentSpec sampleSpec() {
        return ReceiptDocumentSpec.of(receipt -> receipt
                .documentTitle("Transfer confirmation")
                .issuerName("Northwind Pay")
                .generatedOn("09 August 2026")
                .reference("NWP-4821-0067")
                .amount("Amount collected", "£66.62")
                .amountCaption("Direct Debit collected by Harbour Finance Ltd")
                .status(ReceiptStatus.settled("Completed"))
                .summaryField("Value date", "07 Jul 2026")
                .summaryField("Scheme", "Bacs Direct Debit")
                .payer("Paid from", party -> party
                        .name("Alex Sample")
                        .addressLines("12 Example Way", "Brentford TW0 0AA")
                        .field("Account", "•••• 4396"))
                .beneficiary("Paid to", party -> party
                        .name("Harbour Finance Ltd")
                        .field("Account", "•••• 5604"))
                .detailGroup("Transfer details", group -> group
                        .field("Mandate reference", "MND-0110-8054-5652")
                        .emphasized("Total debited", "£66.62"))
                .event("Instructed", "07 Jul 2026, 08:12 BST", "Submitted to the scheme.")
                .event("Settled", "09 Jul 2026, 06:30 BST", "Confirmed by the receiving bank.")
                .note("Keep this confirmation for your records.")
                .verification("https://example.com/verify/NWP-4821-0067", "Scan to check it.")
                .supportLine("help@northwind-pay.example")
                .legalNote("Every name and account on this page is invented."));
    }

    /** Nothing but an amount: exercises every skipped-block path at once. */
    private static ReceiptDocumentSpec minimalSpec() {
        return ReceiptDocumentSpec.of(receipt -> receipt.amount("Amount", "£1.00"));
    }

    /** One side only: the pair widget's single-column path. */
    private static ReceiptDocumentSpec payerOnlySpec() {
        return ReceiptDocumentSpec.of(receipt -> receipt
                .amount("Amount", "£1.00")
                .payer("Paid from", party -> party.name("Alex Sample")));
    }

    private static void render(DocumentTemplate<ReceiptDocumentSpec> template,
                               ReceiptDocumentSpec spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(ModernReceipt.RECOMMENDED_MARGIN))
                .create()) {
            template.compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            // Renders all the way to bytes: the QR code and the inline glyphs only
            // reach a backend here, so a compose-only assertion would miss them.
            assertThat(session.toPdfBytes()).isNotEmpty();
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<ReceiptDocumentSpec> template = ModernReceipt.create();
        assertThat(template.id()).isEqualTo(ModernReceipt.ID);
        assertThat(template.displayName()).isEqualTo(ModernReceipt.DISPLAY_NAME);
    }

    @Test
    void defaultFactoryRendersWithReceiptTheme() throws Exception {
        render(ModernReceipt.create(), sampleSpec());
    }

    @Test
    void rendersWithExplicitTheme() throws Exception {
        render(ModernReceipt.create(BrandTheme.receiptModern()), sampleSpec());
    }

    @Test
    void rendersWithIssuerBranding() throws Exception {
        // No mark: the branded path that matters here is the accent, which reaches
        // the hero strip, the direction arrow, and the reached timeline step.
        ModernReceipt.Options options = ModernReceipt.Options.defaults()
                .withTimelineTitle("Where it got to");
        render(ModernReceipt.create(BrandTheme.receiptModern(), options), sampleSpec());
    }

    @Test
    void readsAnyTheme() throws Exception {
        // A theme built for a CV drives every surface the receipt reads, so the
        // preset must not depend on a token only its own theme sets.
        render(ModernReceipt.create(BrandTheme.boxedClassic()), sampleSpec());
    }

    @Test
    void nullOptionsFallBackToDefaults() throws Exception {
        render(ModernReceipt.create(BrandTheme.receiptModern(), null), sampleSpec());
    }

    @ParameterizedTest
    @EnumSource(ReceiptStatusTone.class)
    void rendersEveryStatusTone(ReceiptStatusTone tone) throws Exception {
        ReceiptDocumentSpec spec = ReceiptDocumentSpec.of(receipt -> receipt
                .amount("Amount", "£1.00")
                .status("Status", tone));
        render(ModernReceipt.create(BrandTheme.receiptModern(),
                ModernReceipt.Options.defaults()), spec);
    }

    @Test
    void rendersReceiptCarryingOnlyAnAmount() throws Exception {
        render(ModernReceipt.create(), minimalSpec());
    }

    @Test
    void rendersOneSidedTransfer() throws Exception {
        render(ModernReceipt.create(), payerOnlySpec());
    }

    @Test
    void accentIsOptional() throws Exception {
        // A null accent must render an unbranded but complete receipt rather than
        // throwing on the first colour the hero asks for.
        ModernReceipt.Options options = new ModernReceipt.Options(null, 0, null, null, "  ");
        assertThat(options.logoWidth()).isEqualTo(ModernReceipt.Options.DEFAULT_LOGO_WIDTH);
        assertThat(options.timelineTitle()).isEqualTo(ModernReceipt.Options.DEFAULT_TIMELINE_TITLE);
        render(ModernReceipt.create(BrandTheme.receiptModern(), options), sampleSpec());
    }

    @Test
    void brandedOptionsCarryTheAccent() {
        DocumentColor accent = DocumentColor.rgb(23, 92, 211);
        ModernReceipt.Options options = ModernReceipt.Options.branded(null, accent);
        assertThat(options.accent()).isEqualTo(accent);
        assertThat(options.withLogoWidth(40).logoWidth()).isEqualTo(40);
        assertThat(options.withLogoColor(accent).logoColor()).isEqualTo(accent);
    }
}
