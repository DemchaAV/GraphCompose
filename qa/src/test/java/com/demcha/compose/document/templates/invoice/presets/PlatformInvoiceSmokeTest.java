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

class PlatformInvoiceSmokeTest {

    // -- what the sheet says -------------------------------------------

    @Test
    void theSheetCarriesTheIssuerTheCustomerAndEveryLine() throws Exception {
        String text = textOf(render(PlatformInvoiceFixtures.invoice()));
        assertThat(text)
                .contains("Aurora Cloud Inc.")
                .contains("Bright Future Ltd.")
                .contains("Compute Instances")
                .contains("Managed Database")
                .contains("Support Plan")
                .contains("TOTAL DUE");
    }

    @Test
    void aLineStatesWhereItWasDelivered() throws Exception {
        String text = textOf(render(PlatformInvoiceFixtures.invoice()));
        assertThat(text).contains("REGION").contains("ca-central1").contains("Global");
    }

    @Test
    void aRateKeepsThePrecisionItIsQuotedAtAndAnAmountDoesNot() throws Exception {
        String text = textOf(render(PlatformInvoiceFixtures.invoice()));
        // 0.0685 rounded to an amount's two places is 0.07 — a different price
        // that multiplies out to a different bill.
        assertThat(text).contains("0.0685").contains("0.0210").contains("0.1385");
        // Amounts stay at two.
        assertThat(text).contains("103.04").contains("22.36");
    }

    @Test
    void usageWithAUnitIsAMeasurementAndUsageWithoutOneIsACount() throws Exception {
        String text = textOf(render(PlatformInvoiceFixtures.invoice()));
        // The meter reads to two places, so a whole number keeps them.
        assertThat(text).contains("744.00").contains("480.00").contains("260.00");
        // The support line counts one plan and names no unit, so it stays bare.
        assertThat(text).doesNotContain("1.00");
    }

    @Test
    void theCurrencyIsStatedOnceInEachMoneyColumnAndOnceOnTheTotal() throws Exception {
        String text = textOf(render(PlatformInvoiceFixtures.invoice()));
        assertThat(text).contains("UNIT PRICE (CAD)").contains("AMOUNT (CAD)");
        // The grand total stands under no caption, so it names its own currency.
        assertThat(text).contains("CAD 195.76");
        // The summed rows sit under a column that already stated it.
        assertThat(text).contains("186.44").doesNotContain("CAD 186.44");
    }

    // -- what a reader can act on --------------------------------------

    @Test
    void everyAddressTheSheetPrintsIsReachable() throws Exception {
        List<String> targets = linkTargets(render(PlatformInvoiceFixtures.invoice()));
        assertThat(targets)
                .contains("mailto:billing@aurora.example")
                // A site printed without a scheme still needs one to open.
                .contains("https://aurora.example")
                .contains("tel:+16045550180");
    }

    // -- what the document chooses, and what it may not ----------------

    @Test
    void aLineThatNamesNoMarkStillRenders() throws Exception {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                PlatformInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Compute Instances", "N2 Standard VM",
                        "Compute", new BigDecimal("744"), "Hours",
                        new BigDecimal("0.0685"), new BigDecimal("50.96"), "", "",
                        "ca-central1"))));
        assertThat(textOf(render(data))).contains("Compute Instances");
    }

    @Test
    void anUnknownMarkIsReportedAsADataErrorNamingTheSet() {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                PlatformInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Compute Instances", "N2 Standard VM",
                        "Compute", new BigDecimal("744"), "Hours",
                        new BigDecimal("0.0685"), new BigDecimal("50.96"), "", "rocket",
                        "ca-central1"))));
        assertThatThrownBy(() -> render(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rocket")
                .hasMessageContaining("compute");
    }

    @Test
    void anInvoiceThatShipsNowhereLeavesTheColumnEmptyRatherThanFailing() throws Exception {
        StructuredInvoiceData base = PlatformInvoiceFixtures.invoice();
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
        assertThat(pagesOf(PlatformInvoiceFixtures.invoice())).isEqualTo(1);
    }

    @Test
    void enoughLinesPaginateAndTheHeaderComesBackWithThem() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            many.add(new InvoiceServiceLines.Line(i, "Compute Instances " + i, "N2 Standard VM",
                    "Compute", new BigDecimal("744"), "Hours", new BigDecimal("0.0685"),
                    new BigDecimal("50.96"), "", "compute", "ca-central1"));
        }
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                PlatformInvoiceFixtures.invoice().serviceLines().columns(), many));
        assertThat(pagesOf(data)).isGreaterThan(1);

        String text = textOf(render(data));
        // The caps header is repeated, so it appears once per page the table reaches.
        assertThat(text.split("REGION", -1).length - 1).isGreaterThan(1);
    }

    // -- helpers ---------------------------------------------------------

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            PlatformInvoice.create().compose(session, data);
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
            PlatformInvoice.create().compose(session, data);
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
        StructuredInvoiceData base = PlatformInvoiceFixtures.invoice();
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
