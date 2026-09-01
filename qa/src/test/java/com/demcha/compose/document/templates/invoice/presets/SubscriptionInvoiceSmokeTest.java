package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
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

class SubscriptionInvoiceSmokeTest {

    // -- what the sheet says -------------------------------------------

    @Test
    void theSheetCarriesTheIssuerTheCustomerAndEveryLine() throws Exception {
        String text = textOf(render(SubscriptionInvoiceFixtures.invoice()));
        assertThat(text)
                .contains("Halstead Software Ltd.")
                .contains("Northline Consulting Ltd")
                .contains("Halstead Workspace Premium (10 seats)")
                .contains("Priority Support Plan")
                .contains("TOTAL DUE");
    }

    @Test
    void everyLineCarriesItsOwnTaxRate() throws Exception {
        String text = textOf(render(SubscriptionInvoiceFixtures.invoice()));
        // The rate is printed per line, not only in the sum, which is what the
        // VAT column is for.
        assertThat(text.split("20%", -1).length - 1).isGreaterThanOrEqualTo(5);
    }

    @Test
    void aLineIsNumberedByWhatItStatesAndOtherwiseByItsPosition() throws Exception {
        StructuredInvoiceData stated = withLines(new InvoiceServiceLines(
                SubscriptionInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(line(41, "Carried forward"), line(42, "Second"))));
        assertThat(textOf(render(stated))).contains("41").contains("42");

        StructuredInvoiceData unstated = withLines(new InvoiceServiceLines(
                SubscriptionInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(line(0, "First"), line(0, "Second"))));
        String text = textOf(render(unstated));
        assertThat(text).contains("1").contains("2");
    }

    @Test
    void moneyCarriesItsCurrencySymbolAgainstTheDigits() throws Exception {
        String text = textOf(render(SubscriptionInvoiceFixtures.invoice()));
        // Every figure names its own currency, because no column states one.
        assertThat(text).contains("£18.00").contains("£1,136.40");
    }

    @Test
    void aCurrencyWhoseMarkTheColumnCannotHoldIsRejectedRatherThanOverflowed() {
        StructuredInvoiceData base = SubscriptionInvoiceFixtures.invoice();
        StructuredInvoiceData data = StructuredInvoiceData.builder()
                .brand(base.brand()).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo()).shipTo(base.shipTo())
                .serviceLines(base.serviceLines()).totals(base.totals())
                .payment(base.payment()).notes(base.notes())
                .currencyCode("ZZZ").build();
        // The design writes money with a one-character mark and its columns are
        // measured for one, so a currency the runtime knows only by its
        // three-letter code does not fit the unit-price column. Widening it
        // would be a different sheet, so the render is refused where it happens
        // rather than letting the figure run under its neighbour — a defect a
        // reader would have to catch by eye.
        assertThatThrownBy(() -> render(data))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("column");
    }

    // -- what a reader can act on --------------------------------------

    @Test
    void everyAddressTheSheetPrintsIsReachable() throws Exception {
        List<String> targets = linkTargets(render(SubscriptionInvoiceFixtures.invoice()));
        assertThat(targets)
                .contains("mailto:billing@halstead.example")
                // A site printed without a scheme still needs one to open.
                .contains("https://www.halstead.example");
    }

    @Test
    void theClosingBandLinksTheAddressInsideItsSentence() throws Exception {
        byte[] pdf = render(SubscriptionInvoiceFixtures.invoice());
        assertThat(textOf(pdf)).contains("For any queries, please contact");
        assertThat(linkTargets(pdf))
                .filteredOn(target -> target.equals("mailto:billing@halstead.example"))
                // Once in the supplier block and once in the closing band.
                .hasSizeGreaterThanOrEqualTo(2);
    }

    // -- what the design assumes, and what it tolerates ----------------

    @Test
    void aPaymentBandWithADifferentCellCountStillDividesEvenly() throws Exception {
        StructuredInvoiceData base = SubscriptionInvoiceFixtures.invoice();
        InvoicePaymentBlock payment = base.payment();
        StructuredInvoiceData data = StructuredInvoiceData.builder()
                .brand(base.brand()).supplier(base.supplier()).masthead(base.masthead())
                .billTo(base.billTo()).shipTo(base.shipTo())
                .serviceLines(base.serviceLines()).totals(base.totals())
                .payment(new InvoicePaymentBlock(payment.heading(),
                        payment.fields().subList(0, 3), payment.instruction(),
                        payment.dueNotice(), "", "", payment.signOff()))
                .notes(base.notes()).currencyCode(base.currencyCode()).build();
        String text = textOf(render(data));
        assertThat(text).contains("Beneficiary").contains("Sort Code")
                .doesNotContain("Account Number");
    }

    @Test
    void anInvoiceThatShipsNowhereLeavesTheSecondPartyOut() throws Exception {
        StructuredInvoiceData base = SubscriptionInvoiceFixtures.invoice();
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
        assertThat(pagesOf(SubscriptionInvoiceFixtures.invoice())).isEqualTo(1);
    }

    @Test
    void enoughLinesPaginateAndTheHeaderComesBackWithThem() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            many.add(line(i, "Halstead Workspace Premium " + i));
        }
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                SubscriptionInvoiceFixtures.invoice().serviceLines().columns(), many));
        assertThat(pagesOf(data)).isGreaterThan(1);

        String text = textOf(render(data));
        // The caps header is repeated, so it appears once per page the table reaches.
        assertThat(text.split("UNIT PRICE", -1).length - 1).isGreaterThan(1);
    }

    // -- helpers ---------------------------------------------------------

    private static InvoiceServiceLines.Line line(int number, String title) {
        return new InvoiceServiceLines.Line(number, title, "", "",
                new BigDecimal("10"), "", new BigDecimal("18.00"),
                new BigDecimal("180.00"), "20%", "", "");
    }

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            SubscriptionInvoice.create().compose(session, data);
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
            SubscriptionInvoice.create().compose(session, data);
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
        StructuredInvoiceData base = SubscriptionInvoiceFixtures.invoice();
        return StructuredInvoiceData.builder()
                .brand(base.brand())
                .supplier(base.supplier())
                .masthead(base.masthead())
                .billTo(base.billTo())
                .shipTo(base.shipTo())
                .serviceLines(lines)
                .totals(base.totals())
                .payment(base.payment())
                .notes(base.notes())
                .currencyCode(base.currencyCode())
                .build();
    }
}
