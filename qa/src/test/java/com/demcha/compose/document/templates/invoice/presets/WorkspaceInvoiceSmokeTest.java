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
 * Smoke test for {@link WorkspaceInvoice} — proves the preset renders a
 * {@link StructuredInvoiceData} end-to-end with its packaged marks, names the
 * currency once per money column rather than on every figure, writes a quantity
 * with what it counts, prints a billed party's registration under its address,
 * reports an unknown mark as a data error, repeats the table header when the
 * lines run past a page, and carries the closing address as a link annotation,
 * which moves no pixel and no layout node so neither gate would notice it going
 * missing.
 */
class WorkspaceInvoiceSmokeTest {

    private static byte[] render(StructuredInvoiceData data) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            WorkspaceInvoice.create().compose(session, data);
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
            WorkspaceInvoice.create().compose(session, data);
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
        StructuredInvoiceData base = WorkspaceInvoiceFixtures.canonicalInvoice();
        return new StructuredInvoiceData(base.brand(), base.supplier(), base.masthead(),
                base.billTo(), base.shipTo(), base.summary(), lines, base.totals(),
                base.payment(), base.notes(), base.currencyCode());
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredInvoiceData> template = WorkspaceInvoice.create();
        assertThat(template.id()).isEqualTo(WorkspaceInvoice.ID);
        assertThat(template.displayName()).isEqualTo(WorkspaceInvoice.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalInvoiceWithPackagedMarks() throws Exception {
        render(WorkspaceInvoiceFixtures.canonicalInvoice());
    }

    @Test
    void canonicalRenderCarriesEveryBlocksText() throws Exception {
        String text = textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice()));
        assertThat(text)
                .contains("INVOICE")
                .contains("Kestrel Collaboration, Inc.")
                .contains("Invoice Number:")
                .contains("BILL TO")
                .contains("SHIP TO")
                .contains("Bright Future Ltd.")
                .contains("Kestrel Business+")
                .contains("PAYMENT DETAILS")
                .contains("BAYUUS33")
                .contains("TOTAL DUE")
                .contains("PAYMENT DUE BY")
                .contains("Thank you for using Kestrel.");
    }

    @Test
    void bothPartiesPrintTheirOwnRegistration() throws Exception {
        // The supplier's sits under its address in the header, the billed
        // party's under its address in the split — both labelled pairs.
        String text = textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice()));
        assertThat(text).contains("US 77-0560185").contains("GB 987 6543 21");
    }

    @Test
    void aPartyWithNoRegistrationPrintsNoLabel() throws Exception {
        // The ship-to has none, and a label over an absence would be worse than
        // no row at all — so there is exactly one of each in the split.
        String text = textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice()));
        assertThat(text.split("VAT ID:", -1)).hasSize(3);
    }

    @Test
    void theCurrencyIsNamedOncePerColumnAndNotOnEveryFigure() throws Exception {
        // This design states the currency in the column head; the figures under
        // it are bare, and only the total carries the code.
        String text = textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice()));
        assertThat(text)
                .contains("UNIT PRICE (USD)")
                .contains("AMOUNT (USD)")
                .contains("USD 587.50")
                .doesNotContain("$437.50");
        assertThat(text).contains("437.50");
    }

    @Test
    void aQuantityIsWrittenWithWhatItCounts() throws Exception {
        String text = textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice()));
        assertThat(text).contains("50 Users");
    }

    @Test
    void aDocumentWithNoLogoFallsBackToItsName() throws Exception {
        // The preset carries no mark of its own, so a document that brings only
        // a name still gets a lockup — and the closing band always sets it.
        assertThat(textOf(render(WorkspaceInvoiceFixtures.canonicalInvoice())))
                .contains("kestrel");
    }

    @Test
    void theClosingAddressIsClickable() throws Exception {
        assertThat(linkTargets(render(WorkspaceInvoiceFixtures.canonicalInvoice())))
                .contains("mailto:billing@kestrel.example")
                .contains("https://kestrel.example");
    }

    @Test
    void anUnknownMarkIsReportedAsADataError() {
        InvoiceServiceLines wrong = new InvoiceServiceLines(
                WorkspaceInvoiceFixtures.serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Telepathy", "Reading minds", "Add-on",
                        BigDecimal.ONE, "", BigDecimal.TEN, BigDecimal.TEN, "", "telescope")));

        assertThatThrownBy(() -> render(withLines(wrong)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telescope")
                .hasMessageContaining("shield");
    }

    @Test
    void aLineWithoutAMarkIsDrawnWithoutOne() throws Exception {
        InvoiceServiceLines unmarked = new InvoiceServiceLines(
                WorkspaceInvoiceFixtures.serviceLines().columns(),
                List.of(new InvoiceServiceLines.Line(1, "Consulting", "A day of it", "Standard",
                        BigDecimal.ONE, "", new BigDecimal("1200"), new BigDecimal("1200"), "")));

        assertThat(textOf(render(withLines(unmarked)))).contains("Consulting");
    }

    @Test
    void aLongerInvoiceRunsOnAndRepeatsItsTableHeader() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            many.add(new InvoiceServiceLines.Line(index, "Service " + index,
                    "A line of description for it", "Standard",
                    BigDecimal.ONE, "", new BigDecimal("120"), new BigDecimal("120"),
                    "", "grid"));
        }
        StructuredInvoiceData longer = withLines(new InvoiceServiceLines(
                WorkspaceInvoiceFixtures.serviceLines().columns(), many));

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
                    .contains("PLAN / SERVICE");
        }
    }

    @Test
    void everyPageCarriesItsNumber() throws Exception {
        List<InvoiceServiceLines.Line> many = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            many.add(new InvoiceServiceLines.Line(index, "Service " + index, "", "Standard",
                    BigDecimal.ONE, "", new BigDecimal("120"), new BigDecimal("120"),
                    "", "grid"));
        }
        String text = textOf(render(withLines(new InvoiceServiceLines(
                WorkspaceInvoiceFixtures.serviceLines().columns(), many))));
        assertThat(text).contains("Page 1 of").contains("Page 2 of");
    }

    @Test
    void anInvoiceWithNoShipToDrawsOneParty() throws Exception {
        StructuredInvoiceData base = WorkspaceInvoiceFixtures.canonicalInvoice();
        StructuredInvoiceData single = new StructuredInvoiceData(base.brand(), base.supplier(),
                base.masthead(), base.billTo(),
                new InvoiceRecipient("", "", "", List.of(), "", ""),
                base.summary(), base.serviceLines(), base.totals(), base.payment(),
                base.notes(), base.currencyCode());

        String text = textOf(render(single));
        assertThat(text).contains("BILL TO").doesNotContain("SHIP TO");
    }

    @Test
    void anInvoiceWithNoNoteDropsTheCardsRuleWithIt() throws Exception {
        // The rule exists to separate the fields from the note; with no note it
        // would close the card on nothing.
        StructuredInvoiceData base = WorkspaceInvoiceFixtures.canonicalInvoice();
        InvoicePaymentBlock quiet = new InvoicePaymentBlock(
                base.payment().heading(), base.payment().fields(), "",
                base.payment().dueNotice(), base.payment().dueNoticeEmphasis(), "", "");
        StructuredInvoiceData bare = new StructuredInvoiceData(base.brand(), base.supplier(),
                base.masthead(), base.billTo(), base.shipTo(), base.summary(),
                base.serviceLines(), base.totals(), quiet, base.notes(), base.currencyCode());

        assertThat(textOf(render(bare)))
                .doesNotContain("Please include the invoice number");
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
