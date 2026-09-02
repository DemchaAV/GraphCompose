package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the structured invoice pipeline through
 * {@link ConsultingInvoice} — proves the preset renders a
 * {@link StructuredInvoiceDocumentSpec} end-to-end with the packaged icon
 * set and a caller-supplied logo, falls back to the wordmark lockup when
 * no logo is supplied, renders an empty document through its guards, and
 * puts the priced figures on the text layer, where the layout snapshot
 * cannot see inside the table.
 */
class ConsultingInvoiceSmokeTest {

    private static byte[] render(StructuredInvoiceDocumentSpec spec) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            ConsultingInvoice.create().compose(session, spec);
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

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredInvoiceDocumentSpec> template = ConsultingInvoice.create();
        assertThat(template.id()).isEqualTo(ConsultingInvoice.ID);
        assertThat(template.displayName()).isEqualTo(ConsultingInvoice.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalInvoiceWithLogoAndPackagedIcons() throws Exception {
        render(ConsultingInvoiceFixtures.canonicalInvoice());
    }

    @Test
    void rendersWordmarkLockupWhenNoLogoIsSupplied() throws Exception {
        StructuredInvoiceData canonical =
                ConsultingInvoiceFixtures.canonicalInvoice().invoice();
        InvoiceBrand brand = canonical.brand();
        StructuredInvoiceData withoutLogo = rebuild(canonical)
                .brand(new InvoiceBrand(null, brand.name(), brand.qualifier(), brand.tagline()))
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(withoutLogo)));
        // The wordmark lockup draws the brand name as text; the logo path does not.
        assertThat(text).contains("NORTHPOINT");
    }

    @Test
    void rendersEmptyInvoice() throws Exception {
        // Exercises the empty-collection paths through full layout and render:
        // no metadata rows, no service lines, no totals rows, no bank fields,
        // no notes — and the blank brand that has neither logo nor name.
        render(StructuredInvoiceDocumentSpec.from(StructuredInvoiceData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesTheTableAndTotalsText() throws Exception {
        // The line-items table and the totals bands are leaf nodes in the
        // layout snapshot, so their content is asserted on the text layer.
        String text = textOf(render(ConsultingInvoiceFixtures.canonicalInvoice()));
        assertThat(text)
                .contains("Strategic Consultation")
                .contains("Leadership alignment workshops")
                .contains("1–10 May 2025")
                .contains("2,500.00")
                .contains("GST (10%)")
                .contains("TOTAL DUE (AUD)")
                .contains("15,400.00")
                .contains("INV-2025-0478");
    }

    @Test
    void aDocumentWithoutACurrencyPrintsNoEmptyParentheses() throws Exception {
        StructuredInvoiceData canonical = ConsultingInvoiceFixtures.canonicalInvoice().invoice();
        StructuredInvoiceData withoutCurrency = rebuild(canonical).currencyCode("").build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(withoutCurrency)));
        assertThat(text).doesNotContain("()").contains("TOTAL DUE");
    }

    @Test
    void aSupplierWithoutARegistrationPrintsNoDanglingSeparator() throws Exception {
        StructuredInvoiceData canonical = ConsultingInvoiceFixtures.canonicalInvoice().invoice();
        InvoiceContactBlock supplier = canonical.supplier();
        StructuredInvoiceData withoutRegistration = rebuild(canonical)
                .supplier(new InvoiceContactBlock(supplier.legalName(), supplier.addressLines(),
                        supplier.phone(), supplier.email(), supplier.website(), "", ""))
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(withoutRegistration)));
        assertThat(text)
                .contains("Northpoint Consulting Pty Ltd")
                .doesNotContain("Pty Ltd   |");
    }

    @Test
    void theBankBandGrowsWithTheNumberOfPaymentFields() throws Exception {
        StructuredInvoiceData canonical = ConsultingInvoiceFixtures.canonicalInvoice().invoice();
        InvoicePaymentBlock payment = canonical.payment();
        List<InvoicePaymentBlock.Field> more = new ArrayList<>(payment.fields());
        more.add(new InvoicePaymentBlock.Field("SWIFT:", "EXAMPLEAU"));
        more.add(new InvoicePaymentBlock.Field("IBAN:", "AU00 1234 5678 9012"));
        StructuredInvoiceData withMoreFields = rebuild(canonical)
                .payment(new InvoicePaymentBlock(payment.heading(), more, payment.instruction(),
                        payment.dueNotice(), payment.dueNoticeEmphasis()))
                .build();

        // The band is sized from the field count, so the last field is drawn
        // inside it rather than over the instruction beneath.
        String text = textOf(render(StructuredInvoiceDocumentSpec.from(withMoreFields)));
        assertThat(text).contains("SWIFT:").contains("IBAN:")
                .contains("Please ensure the invoice number");
    }

    /** Copies every component of a spec so one of them can be replaced. */
    private static StructuredInvoiceData.Builder rebuild(StructuredInvoiceData data) {
        return StructuredInvoiceData.builder()
                .brand(data.brand())
                .supplier(data.supplier())
                .masthead(data.masthead())
                .billTo(data.billTo())
                .summary(data.summary())
                .serviceLines(data.serviceLines())
                .totals(data.totals())
                .payment(data.payment())
                .notes(data.notes())
                .currencyCode(data.currencyCode());
    }

    @Test
    void overflowInvoiceRepeatsTheTableHeaderOnTheSecondPage() throws Exception {
        byte[] pdfBytes = render(ConsultingInvoiceFixtures.overflowInvoice());
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(document)).contains("SERVICE PERIOD");
        }
    }
}
