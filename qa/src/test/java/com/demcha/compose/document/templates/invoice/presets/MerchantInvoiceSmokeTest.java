package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantInvoiceSmokeTest {

    // -- what the sheet says -------------------------------------------

    @Test
    void theSheetCarriesTheIssuerTheCustomerAndEveryLine() throws Exception {
        String text = textOf(render(MerchantInvoiceFixtures.invoice()));
        assertThat(text)
                .contains("Coastline Commerce Inc.")
                .contains("Bright Future Ltd.")
                .contains("Coastline Advanced")
                .contains("Priority Support")
                .contains("TOTAL DUE");
    }

    @Test
    void theCurrencyIsStatedInEachMoneyCaptionAndOnceMoreOnTheTotal() throws Exception {
        String text = textOf(render(MerchantInvoiceFixtures.invoice()));
        assertThat(text).contains("UNIT PRICE (USD)").contains("AMOUNT (USD)");
        // The grand total stands under no caption, so it names its own currency.
        assertThat(text).contains("USD 444.00");
        // The figures under the captions are bare.
        assertThat(text).contains("399.00").doesNotContain("USD 399.00");
    }

    @Test
    void aLineThatCountsNothingPrintsADashRatherThanAZero() throws Exception {
        String text = textOf(render(MerchantInvoiceFixtures.invoice()));
        // A zero in a quantity column reads as none delivered rather than as not
        // counted, which is a different thing to tell a reader.
        assertThat(text).contains("—");
    }

    // -- what a reader can act on --------------------------------------

    @Test
    void everyAddressTheSheetPrintsIsReachable() throws Exception {
        List<String> targets = linkTargets(render(MerchantInvoiceFixtures.invoice()));
        assertThat(targets)
                .contains("mailto:billing@coastline.example")
                // A site printed without a scheme still needs one to open.
                .contains("https://www.coastline.example")
                .contains("tel:+16135550142");
    }

    @Test
    void theClosingNoteLinksTheAddressInsideItsSentence() throws Exception {
        byte[] pdf = render(MerchantInvoiceFixtures.invoice());
        assertThat(textOf(pdf)).contains("If you have any questions about this invoice");
        assertThat(linkTargets(pdf))
                .filteredOn(t -> t.equals("mailto:billing@coastline.example"))
                // Once in the supplier block, once in the payment panel, once in
                // the note.
                .hasSizeGreaterThanOrEqualTo(3);
    }

    // -- what the document chooses, and what it may not ----------------

    @Test
    void aLineThatNamesNoMarkStillRenders() throws Exception {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MerchantInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(line("Coastline Advanced", ""))));
        assertThat(textOf(render(data))).contains("Coastline Advanced");
    }

    @Test
    void anUnknownMarkIsReportedAsADataErrorNamingTheSet() {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MerchantInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(line("Coastline Advanced", "rocket"))));
        assertThatThrownBy(() -> render(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rocket")
                .hasMessageContaining("bag");
    }

    @Test
    void anInvoiceThatShipsNowhereLeavesTheColumnEmptyRatherThanFailing() throws Exception {
        StructuredInvoiceData base = MerchantInvoiceFixtures.invoice();
        StructuredInvoiceData data = StructuredInvoiceData.builder()
                .brand(base.brand()).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo())
                .serviceLines(base.serviceLines()).totals(base.totals())
                .payment(base.payment()).notes(base.notes())
                .currencyCode(base.currencyCode()).build();
        String text = textOf(render(data));
        assertThat(text).contains("BILL TO").doesNotContain("SHIP TO");
    }

    // -- how it flows ---------------------------------------------------

    @Test
    void theDesignsOwnLineCountIsOneSheet() throws Exception {
        assertThat(pagesOf(MerchantInvoiceFixtures.invoice())).isEqualTo(1);
    }

    @Test
    void enoughLinesPaginateAndTheHeaderComesBackWithThem() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            many.add(line("Coastline Advanced " + i, "bag"));
        }
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MerchantInvoiceFixtures.invoice().serviceLines().columns(), many));
        assertThat(pagesOf(data)).isGreaterThan(1);

        String text = textOf(render(data));
        // The caps header is repeated, so it appears once per page the table reaches.
        assertThat(text.split("PLAN / SERVICE", -1).length - 1).isGreaterThan(1);
    }

    // -- helpers ---------------------------------------------------------

    private static InvoiceServiceLines.Line line(String title, String icon) {
        return new InvoiceServiceLines.Line(1, title, "Monthly subscription", "Advanced Plan",
                BigDecimal.ONE, "", new BigDecimal("399.00"),
                new BigDecimal("399.00"), "", icon, "");
    }

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            MerchantInvoice.create().compose(session, data);
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
            MerchantInvoice.create().compose(session, data);
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
        StructuredInvoiceData base = MerchantInvoiceFixtures.invoice();
        return StructuredInvoiceData.builder()
                .brand(base.brand()).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo()).shipTo(base.shipTo())
                .serviceLines(lines).totals(base.totals())
                .payment(base.payment()).notes(base.notes())
                .currencyCode(base.currencyCode()).build();
    }
}
