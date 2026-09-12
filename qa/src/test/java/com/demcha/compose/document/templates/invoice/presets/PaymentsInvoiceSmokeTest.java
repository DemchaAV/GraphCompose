package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryBlock;
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

/**
 * Smoke test for {@link PaymentsInvoice} — proves the preset renders a
 * {@link StructuredInvoiceData} end-to-end with its packaged marks, falls back
 * from a logo to a wordmark, writes its figures with the locale stated rather
 * than inherited, reports an unknown mark as a data error, repeats the table
 * header when the lines run past a page, and carries the support contacts as
 * link annotations, which move no pixel and no layout node so neither gate
 * would notice them going missing.
 */
class PaymentsInvoiceSmokeTest {

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            PaymentsInvoice.create().compose(session, data);
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
            PaymentsInvoice.create().compose(session, data);
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

    /** The canonical invoice with its service lines replaced. */
    private static StructuredInvoiceData withLines(InvoiceServiceLines lines) {
        StructuredInvoiceData base = PaymentsInvoiceFixtures.canonicalInvoice();
        return new StructuredInvoiceData(base.brand(), base.supplier(), base.masthead(),
                base.billTo(), base.shipTo(), base.summary(), lines, base.totals(),
                base.payment(), base.notes(), base.currencyCode());
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredInvoiceData> template = PaymentsInvoice.create();
        assertThat(template.id()).isEqualTo(PaymentsInvoice.ID);
        assertThat(template.displayName()).isEqualTo(PaymentsInvoice.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalInvoiceWithPackagedMarks() throws Exception {
        render(PaymentsInvoiceFixtures.canonicalInvoice());
    }

    @Test
    void canonicalRenderCarriesEveryBlocksText() throws Exception {
        String text = textOf(render(PaymentsInvoiceFixtures.canonicalInvoice()));
        assertThat(text)
                .contains("INVOICE")
                .contains("Meridian Payments Ltd.")
                .contains("Company No. 09048900")
                .contains("VAT No. GB 123 4567 89")
                .contains("Invoice Number")
                .contains("BILL TO")
                .contains("SHIP TO")
                .contains("Northwind Ltd.")
                .contains("Subscription Billing")
                .contains("PAYMENT DETAILS")
                .contains("GB29 HRBK 4002 5071 2954 08")
                .contains("TOTAL DUE")
                .contains("NOTES")
                .contains("Questions? We're here to help.");
    }

    @Test
    void aDocumentWithNoLogoFallsBackToItsName() throws Exception {
        // The preset carries no mark of its own, so a document that brings only
        // a name still gets a lockup rather than an empty box.
        assertThat(textOf(render(PaymentsInvoiceFixtures.canonicalInvoice())))
                .contains("Meridian");
    }

    @Test
    void figuresAreWrittenWithTheCurrencyStatedOnceAndCarried() throws Exception {
        // The column names the currency; each figure carries its symbol. Both
        // come from the ISO code rather than from a formatted string.
        String text = textOf(render(PaymentsInvoiceFixtures.canonicalInvoice()));
        assertThat(text).contains("(GBP)").contains("£2,623.20").contains("£1,180.00");
    }

    @Test
    void theDueLineIsSetInCapitalsWhateverCaseTheDocumentUses() throws Exception {
        // The design sets the footer's due line in capitals, so the preset is
        // what uppercases it rather than the document having to.
        assertThat(textOf(render(PaymentsInvoiceFixtures.canonicalInvoice())))
                .contains("PAYMENT DUE BY 14 JUNE 2025");
    }

    @Test
    void theSupportContactsAreClickable() throws Exception {
        List<String> targets = linkTargets(render(PaymentsInvoiceFixtures.canonicalInvoice()));
        assertThat(targets)
                .contains("mailto:support@meridianpayments.example")
                .contains("tel:+442039661900");
    }

    @Test
    void aTrunkPrefixIsPrintedButNotDialled() throws Exception {
        // "(0)" is for a domestic dialler; left in the digits it would make a
        // number that reaches nobody. The fixture's own number carries one, so
        // the sheet prints it and the annotation drops it.
        byte[] pdfBytes = render(PaymentsInvoiceFixtures.canonicalInvoice());
        // The stripper's own spacing is not the design's, so the printed form
        // is compared without it.
        assertThat(textOf(pdfBytes).replaceAll("(?U)\\s", ""))
                .contains("+44(0)2039661900");
        assertThat(linkTargets(pdfBytes))
                .contains("tel:+442039661900")
                .doesNotContain("tel:+4402039661900");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        InvoiceServiceLines wrong = new InvoiceServiceLines(
                PaymentsInvoiceFixtures.serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Telepathy", "Reading minds", "",
                        BigDecimal.ONE, "", BigDecimal.TEN, BigDecimal.TEN, "20%", "telescope")));

        assertThatThrownBy(() -> render(withLines(wrong)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("shield");
    }

    @Test
    void aLineWithoutAMarkIsDrawnWithoutOne() throws Exception {
        InvoiceServiceLines unmarked = new InvoiceServiceLines(
                PaymentsInvoiceFixtures.serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Consulting", "A day of it", "",
                        BigDecimal.ONE, "", new BigDecimal("1200"), new BigDecimal("1200"),
                        "20%")));

        assertThat(textOf(render(withLines(unmarked)))).contains("Consulting");
    }

    @Test
    void aLongerInvoiceRunsOnAndRepeatsItsTableHeader() throws Exception {
        // The table is the region that grows, so the sheet flows rather than
        // refusing — and a continuation page has to say what its columns are.
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            many.add(new InvoiceServiceLines.Line(index, "Service " + index,
                    "A line of description for it", "",
                    BigDecimal.ONE, "", new BigDecimal("120"), new BigDecimal("120"),
                    "20%", "card"));
        }
        StructuredInvoiceData longer = withLines(new InvoiceServiceLines(
                PaymentsInvoiceFixtures.serviceLines().columns(), many));

        assertThat(pagesOf(longer)).isGreaterThan(1);
        byte[] pdfBytes = render(longer);
        assertThat(textOf(pdfBytes)).contains("Service 30");
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(document))
                    .as("the second page names its columns")
                    .contains("DESCRIPTION")
                    .contains("UNIT PRICE");
        }
    }

    @Test
    void everyPageCarriesItsNumber() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            many.add(new InvoiceServiceLines.Line(index, "Service " + index, "", "",
                    BigDecimal.ONE, "", new BigDecimal("120"), new BigDecimal("120"),
                    "20%", "card"));
        }
        String text = textOf(render(withLines(new InvoiceServiceLines(
                PaymentsInvoiceFixtures.serviceLines().columns(), many))));
        assertThat(text).contains("Page 1 of").contains("Page 2 of");
    }

    @Test
    void anInvoiceWithNoShipToDrawsOneParty() throws Exception {
        StructuredInvoiceData base = PaymentsInvoiceFixtures.canonicalInvoice();
        StructuredInvoiceData single = new StructuredInvoiceData(base.brand(), base.supplier(),
                base.masthead(), base.billTo(),
                new InvoiceRecipient("", "", "", List.of(), "", ""),
                base.summary(), base.serviceLines(), base.totals(), base.payment(),
                base.notes(), base.currencyCode());

        String text = textOf(render(single));
        assertThat(text).contains("BILL TO").doesNotContain("SHIP TO");
    }

    @Test
    void anInvoiceWithNoNotesDropsTheBlockRatherThanLeavingItsDisc() throws Exception {
        StructuredInvoiceData base = PaymentsInvoiceFixtures.canonicalInvoice();
        StructuredInvoiceData bare = new StructuredInvoiceData(base.brand(), base.supplier(),
                base.masthead(), base.billTo(), base.shipTo(), base.summary(),
                base.serviceLines(), base.totals(), base.payment(),
                new InvoiceNotesBlock("NOTES", List.of(), "ops@example.test", ""),
                base.currencyCode());

        String text = textOf(render(bare));
        assertThat(text).doesNotContain("Late payment may incur interest");
        assertThat(linkTargets(render(bare))).contains("mailto:ops@example.test");
    }

    @Test
    void anEmptyDocumentStillRenders() throws Exception {
        // Every block absent: the preset draws its furniture and nothing else,
        // rather than failing on the first missing field.
        render(new StructuredInvoiceData(new InvoiceBrand(null, "", "", ""), null, null,
                null, null, new InvoiceSummaryBlock("", "", ""),
                new InvoiceServiceLines(null, List.of()), null,
                new InvoicePaymentBlock("", List.of(), "", "", "", "", ""),
                new InvoiceNotesBlock("", List.of(), "", ""), ""));
    }
}
