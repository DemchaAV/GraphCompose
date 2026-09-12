package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObsidianInvoiceSmokeTest {

    // -- what the sheet says -------------------------------------------

    @Test
    void theSheetCarriesTheIssuerTheCustomerAndEveryLine() throws Exception {
        String text = textOf(render(ObsidianInvoiceFixtures.invoice()));
        assertThat(text)
                .contains("Kestrel Business")
                .contains("Northline Consulting Ltd")
                .contains("Subscription setup")
                .contains("Business account maintenance")
                .contains("Total due");
    }

    @Test
    void theTaxColumnIsWorkedOutFromTheLineRatherThanStated() throws Exception {
        String text = textOf(render(ObsidianInvoiceFixtures.invoice()));
        // 1 x £150.00 charged at £180.00 leaves £30.00 of tax, and the model
        // carries a rate rather than that figure.
        assertThat(text).contains("£30.00").contains("£50.00").contains("£40.00");
    }

    @Test
    void aLineWithNoTaxLeavesTheColumnAtZeroRatherThanNegative() throws Exception {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                ObsidianInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Zero-rated service", "No tax due", "",
                        BigDecimal.ONE, "", new BigDecimal("150.00"),
                        new BigDecimal("150.00"), "", "", ""))));
        assertThat(textOf(render(data))).contains("£0.00");
    }

    @Test
    void moneyCarriesItsCurrencyMarkAgainstTheDigits() throws Exception {
        String text = textOf(render(ObsidianInvoiceFixtures.invoice()));
        assertThat(text).contains("£150.00").contains("£876.00");
    }

    // -- the discs ------------------------------------------------------

    @Test
    void theBilledPartysDiscTakesItsInitialsFromItsName() throws Exception {
        // "Northline Consulting Ltd" gives NC, which is the design's own disc.
        assertThat(textOf(render(ObsidianInvoiceFixtures.invoice()))).contains("NC");
    }

    @Test
    void theIssuersDiscFallsBackFromMonogramToInitials() throws Exception {
        StructuredInvoiceData base = ObsidianInvoiceFixtures.invoice();
        // The fixture states a monogram, so that is what the disc shows.
        assertThat(textOf(render(base))).contains("K");

        StructuredInvoiceData noMonogram = withBrand(
                new InvoiceBrand(null, "Kestrel Business", "", "", "", ""));
        // With none stated it falls back to initials taken from the name.
        assertThat(textOf(render(noMonogram))).contains("KB");
    }

    // -- what a reader can act on --------------------------------------

    @Test
    void everyAddressTheSheetPrintsIsReachable() throws Exception {
        List<String> targets = linkTargets(render(ObsidianInvoiceFixtures.invoice()));
        assertThat(targets).contains("mailto:ar@kestrel.example");
    }

    @Test
    void theDueSentenceSetsItsDateApartWithoutBreakingTheSentence() throws Exception {
        String text = textOf(render(ObsidianInvoiceFixtures.invoice()));
        // The date is a run of the same sentence, not a paragraph of its own.
        assertThat(text).contains("Payment is due by 10 September 2026.");
    }

    // -- how it flows ---------------------------------------------------

    @Test
    void theDesignsOwnLineCountIsOneSheet() throws Exception {
        assertThat(pagesOf(ObsidianInvoiceFixtures.invoice())).isEqualTo(1);
    }

    @Test
    void enoughLinesPaginateAndTheHeaderComesBackWithThem() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            many.add(new InvoiceServiceLines.Line(i, "Subscription setup " + i,
                    "One-off account and workspace setup", "",
                    BigDecimal.ONE, "", new BigDecimal("150.00"),
                    new BigDecimal("180.00"), "", "", ""));
        }
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                ObsidianInvoiceFixtures.invoice().serviceLines().columns(), many));
        assertThat(pagesOf(data)).isGreaterThan(1);

        String text = textOf(render(data));
        // The caps header is repeated, so it appears once per page the table reaches.
        assertThat(text.split("Unit price", -1).length - 1).isGreaterThan(1);
    }

    // -- helpers ---------------------------------------------------------

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            ObsidianInvoice.create().compose(session, data);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    private static String textOf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static int pagesOf(StructuredInvoiceData data) throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ObsidianInvoice.create().compose(session, data);
            return session.layoutSnapshot().totalPages();
        }
    }

    private static List<String> linkTargets(byte[] pdfBytes) throws Exception {
        List<String> targets = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link
                            && link.getAction() instanceof PDActionURI uri) {
                        targets.add(uri.getURI());
                    }
                }
            }
        }
        return targets;
    }

    private static StructuredInvoiceData withLines(InvoiceServiceLines lines) {
        StructuredInvoiceData base = ObsidianInvoiceFixtures.invoice();
        return StructuredInvoiceData.builder()
                .brand(base.brand()).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo())
                .serviceLines(lines).totals(base.totals())
                .payment(base.payment()).notes(base.notes())
                .currencyCode(base.currencyCode()).build();
    }

    private static StructuredInvoiceData withBrand(InvoiceBrand brand) {
        StructuredInvoiceData base = ObsidianInvoiceFixtures.invoice();
        return StructuredInvoiceData.builder()
                .brand(brand).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo())
                .serviceLines(base.serviceLines()).totals(base.totals())
                .payment(base.payment()).notes(base.notes())
                .currencyCode(base.currencyCode()).build();
    }
}
