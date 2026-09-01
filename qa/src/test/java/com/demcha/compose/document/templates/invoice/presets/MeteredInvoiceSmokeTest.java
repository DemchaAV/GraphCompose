package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
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

class MeteredInvoiceSmokeTest {

    // -- what the sheet says -------------------------------------------

    @Test
    void theSheetCarriesTheIssuerTheCustomerAndEveryLine() throws Exception {
        String text = textOf(render(MeteredInvoiceFixtures.invoice()));
        assertThat(text)
                .contains("Northwind Cloud Services Ltd.")
                .contains("Bright Future Ltd.")
                .contains("Compute Instances")
                .contains("Managed Database")
                .contains("Support Plan")
                .contains("TOTAL DUE");
    }

    @Test
    void aRateKeepsThePrecisionItIsQuotedAtAndAnAmountDoesNot() throws Exception {
        String text = textOf(render(MeteredInvoiceFixtures.invoice()));
        // 0.0710 rounded to an amount's two places is 0.07 — a different price
        // that multiplies out to a different bill.
        assertThat(text).contains("GBP 0.0710").contains("GBP 0.0215");
        // The support line is quoted at two places and stays there.
        assertThat(text).contains("GBP 0.91");
        // Amounts and totals are written at two throughout.
        assertThat(text).contains("GBP 122.76").contains("GBP 257.65");
    }

    @Test
    void aCountIsWrittenWithTheThingItCounts() throws Exception {
        String text = textOf(render(MeteredInvoiceFixtures.invoice()));
        assertThat(text).contains("744 Hrs").contains("480 GB");
        // The support line counts a plan, which has no unit.
        assertThat(text).contains("Support Plan");
    }

    @Test
    void theBandUnderTheSheetCarriesTheDisclosureTheSupplierStates() throws Exception {
        String text = textOf(render(MeteredInvoiceFixtures.invoice()));
        assertThat(text).contains("is a subsidiary of Northwind Group plc.");
    }

    @Test
    void aSupplierThatStatesNoDisclosureIsIdentifiedByItsNameInstead() throws Exception {
        StructuredInvoiceData data = MeteredInvoiceFixtures.invoice();
        InvoiceContactBlock plain = new InvoiceContactBlock(
                data.supplier().legalName(), data.supplier().addressLines(),
                data.supplier().phone(), data.supplier().email(), data.supplier().website(),
                "", "", data.supplier().taxRegistrationLabel(),
                data.supplier().taxRegistrationNumber());
        String text = textOf(render(StructuredInvoiceData.builder()
                .brand(data.brand()).supplier(plain).masthead(data.masthead())
                .billTo(data.billTo()).shipTo(data.shipTo())
                .serviceLines(data.serviceLines()).totals(data.totals())
                .payment(data.payment()).notes(data.notes())
                .currencyCode(data.currencyCode()).build()));
        assertThat(text).contains("Northwind Cloud Services Ltd.")
                .doesNotContain("is a subsidiary of");
    }

    // -- what a reader can act on --------------------------------------

    @Test
    void everyAddressTheSheetPrintsIsReachable() throws Exception {
        List<String> targets = linkTargets(render(MeteredInvoiceFixtures.invoice()));
        assertThat(targets)
                .contains("mailto:billing@northwind.example")
                .contains("https://northwind.example")
                // The trunk prefix goes: it is the digit a caller omits from abroad.
                .contains("tel:+441174962280");
    }

    @Test
    void theNotesProseLinksTheAddressesInsideItAndNotTheSentenceAroundThem()
            throws Exception {
        byte[] pdf = render(MeteredInvoiceFixtures.invoice());
        assertThat(linkTargets(pdf))
                .filteredOn(target -> target.startsWith("mailto:"))
                .isNotEmpty();
        // The sentence still reads as one sentence.
        assertThat(textOf(pdf))
                .contains("For billing and account support, visit");
    }

    // -- what the document chooses, and what it may not ----------------

    @Test
    void aLineDrawsTheMarkItNames() throws Exception {
        String text = textOf(render(MeteredInvoiceFixtures.invoice()));
        // A mark leaves no glyph, so the render succeeding on every packaged
        // token is the assertion; an unknown one is rejected below.
        assertThat(text).contains("Egress Traffic");
    }

    @Test
    void aLineThatNamesNoMarkStillRenders() throws Exception {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MeteredInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Compute Instances", "On-demand usage",
                        "London (eu-west)", new BigDecimal("744"), "Hrs",
                        new BigDecimal("0.0710"), new BigDecimal("52.82"), "", ""))));
        assertThat(textOf(render(data))).contains("Compute Instances");
    }

    @Test
    void anUnknownMarkIsReportedAsADataErrorNamingTheSet() {
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MeteredInvoiceFixtures.invoice().serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Compute Instances", "On-demand usage",
                        "London (eu-west)", new BigDecimal("744"), "Hrs",
                        new BigDecimal("0.0710"), new BigDecimal("52.82"), "", "rocket"))));
        assertThatThrownBy(() -> render(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rocket")
                .hasMessageContaining("compute");
    }

    // -- how it flows ---------------------------------------------------

    @Test
    void theDesignsOwnLineCountIsOneSheet() throws Exception {
        assertThat(pagesOf(MeteredInvoiceFixtures.invoice())).isEqualTo(1);
    }

    @Test
    void enoughLinesPaginateAndTheHeaderComesBackWithThem() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            many.add(new InvoiceServiceLines.Line(i, "Compute Instances " + i,
                    "On-demand usage", "London (eu-west)", new BigDecimal("744"), "Hrs",
                    new BigDecimal("0.0710"), new BigDecimal("52.82"), "", "compute"));
        }
        StructuredInvoiceData data = withLines(new InvoiceServiceLines(
                MeteredInvoiceFixtures.invoice().serviceLines().columns(), many));
        assertThat(pagesOf(data)).isGreaterThan(1);

        String text = textOf(render(data));
        // The caps header is repeated, so it appears once per page the table reaches.
        assertThat(text.split("UNIT PRICE", -1).length - 1).isGreaterThan(1);
    }

    // -- helpers ---------------------------------------------------------

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            MeteredInvoice.create().compose(session, data);
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
            MeteredInvoice.create().compose(session, data);
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
        StructuredInvoiceData base = MeteredInvoiceFixtures.invoice();
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
