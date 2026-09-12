package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@link LumaStudioInvoice} — proves the preset renders a
 * {@link StructuredInvoiceDocumentSpec} end-to-end with its packaged icon
 * set, renders an empty document through its guards, writes the currency
 * mark its data's code names, puts the priced figures on the text layer
 * where the layout snapshot cannot see inside the table, and carries the
 * contact channels as link annotations — which move no pixel and no layout
 * node, so neither gate would notice them going missing.
 */
class LumaStudioInvoiceSmokeTest {

    private static byte[] render(StructuredInvoiceDocumentSpec spec) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            LumaStudioInvoice.create().compose(session, spec);
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

    /**
     * The text with its spaces removed. The sheet's headings and labels are
     * letter-spaced runs, so the extractor can report a gap between each pair
     * of letters; what is being asserted is the wording, not the tracking.
     */
    private static String compact(String text) {
        return text.replace(" ", "");
    }

    /** The text as one run, so an assertion need not know where a line broke. */
    private static String unwrapped(String text) {
        return text.replace(String.valueOf((char) 13), "")
                .replace(String.valueOf((char) 10), " ");
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

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredInvoiceDocumentSpec> template = LumaStudioInvoice.create();
        assertThat(template.id()).isEqualTo(LumaStudioInvoice.ID);
        assertThat(template.displayName()).isEqualTo(LumaStudioInvoice.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalInvoiceWithPackagedIcons() throws Exception {
        render(LumaStudioInvoiceFixtures.canonicalInvoice());
    }

    @Test
    void rendersEmptyInvoice() throws Exception {
        // Exercises the empty-collection paths through full layout and render:
        // no metadata rows, no service lines, no totals rows, no bank fields,
        // no notes, and a brand with neither monogram nor wordmark.
        render(StructuredInvoiceDocumentSpec.from(StructuredInvoiceData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesTheTableTotalsAndBankText() throws Exception {
        // The line-items table, the totals bands and the bank rows are leaf
        // nodes in the layout snapshot, so their content is asserted here.
        String text = textOf(render(LumaStudioInvoiceFixtures.canonicalInvoice()));
        assertThat(text)
                .contains("Brand Strategy Workshop")
                .contains("1,200.00")
                .contains("10,200.00")
                .contains("GB36 SRLG 6083 7198 7654 32")
                .contains("Starling Bank");
        // The description wraps inside its cell, so it is asserted unwrapped.
        assertThat(unwrapped(text))
                .contains("Discovery session, research & brand positioning.");
        assertThat(compact(text))
                .contains("TOTALDUE")
                .contains("INV-2024-0587")
                .contains("PAYMENTDETAILS");
    }

    @Test
    void amountsCarryTheMarkOfTheStatedCurrency() throws Exception {
        String text = textOf(render(LumaStudioInvoiceFixtures.canonicalInvoice()));
        assertThat(text).contains("£10,200.00");
    }

    @Test
    void anUnknownCurrencyCodePrintsItselfRatherThanNothing() throws Exception {
        StructuredInvoiceData data = rebuild(
                LumaStudioInvoiceFixtures.canonicalInvoice().invoice())
                .currencyCode("ZZZ")
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(data)));
        assertThat(text).contains("ZZZ10,200.00");
    }

    @Test
    void aDocumentWithoutACurrencyLeavesTheFiguresBare() throws Exception {
        StructuredInvoiceData data = rebuild(
                LumaStudioInvoiceFixtures.canonicalInvoice().invoice())
                .currencyCode("")
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(data)));
        assertThat(text).contains("10,200.00").doesNotContain("£10,200.00");
    }

    @Test
    void contactChannelsAreClickable() throws Exception {
        List<String> targets = linkTargets(render(LumaStudioInvoiceFixtures.canonicalInvoice()));
        assertThat(targets)
                .contains("tel:+442079460832")
                .contains("mailto:hello@lumaandco.studio")
                .contains("https://www.lumaandco.studio");
    }

    @Test
    void aSupplierWithoutRegistrationsPrintsNoDanglingSeparator() throws Exception {
        StructuredInvoiceData canonical =
                LumaStudioInvoiceFixtures.canonicalInvoice().invoice();
        InvoiceContactBlock supplier = canonical.supplier();
        StructuredInvoiceData data = rebuild(canonical)
                .supplier(new InvoiceContactBlock(supplier.legalName(), supplier.addressLines(),
                        supplier.phone(), supplier.email(), supplier.website(),
                        "", "", "", ""))
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(data)));
        assertThat(text).contains("Studio 3.02, The Loom").doesNotContain("|");
    }

    @Test
    void aSupplierWithOneRegistrationPrintsNoSeparator() throws Exception {
        StructuredInvoiceData canonical =
                LumaStudioInvoiceFixtures.canonicalInvoice().invoice();
        InvoiceContactBlock supplier = canonical.supplier();
        StructuredInvoiceData data = rebuild(canonical)
                .supplier(new InvoiceContactBlock(supplier.legalName(), supplier.addressLines(),
                        supplier.phone(), supplier.email(), supplier.website(),
                        supplier.registrationLabel(), supplier.registrationNumber(), "", ""))
                .build();

        String text = textOf(render(StructuredInvoiceDocumentSpec.from(data)));
        assertThat(text).contains("Company No.  12578934").doesNotContain("|");
    }

    @Test
    void overflowInvoiceRepeatsTheTableHeaderAndTheFolioOnTheSecondPage() throws Exception {
        byte[] pdfBytes = render(LumaStudioInvoiceFixtures.overflowInvoice());
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String secondPage = compact(stripper.getText(document));
            assertThat(secondPage).contains("DESCRIPTION").contains("UNITPRICE");
            assertThat(secondPage).contains("Page2of");
        }
    }

    @Test
    void overflowInvoicePricesEveryLine() throws Exception {
        String text = textOf(render(LumaStudioInvoiceFixtures.overflowInvoice()));
        int occurrences = text.split("Project Management", -1).length - 1;
        assertThat(occurrences).isEqualTo(4);
    }

    @Test
    void theSignOffLandsOnItsOwnDarkGroundOnTheLastPage() throws Exception {
        // The words are white. The dark foot band is a page background pinned
        // to the paper's edge, and on a multi-page invoice the flow ends well
        // above it — so the sign-off carries its own strip, and this is what
        // says the strip is still under the words rather than the page's.
        try (DocumentSession session = GraphCompose.document().create()) {
            LumaStudioInvoice.create().compose(
                    session, LumaStudioInvoiceFixtures.overflowInvoice());
            LayoutNodeSnapshot band = node(session, "BannerBand");
            LayoutNodeSnapshot signOff = node(session, "SignOff");

            assertThat(signOff.startPage()).isEqualTo(band.startPage());
            assertThat(signOff.placementY()).isGreaterThanOrEqualTo(band.placementY());
            assertThat(signOff.placementY() + signOff.placementHeight())
                    .isLessThanOrEqualTo(band.placementY() + band.placementHeight());
        }
    }

    private static LayoutNodeSnapshot node(DocumentSession session, String entityName) {
        return session.layoutSnapshot().nodes().stream()
                .filter(candidate -> entityName.equals(candidate.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No node named " + entityName));
    }

    /** Copies every component of a spec so one of them can be replaced. */
    private static StructuredInvoiceData.Builder rebuild(StructuredInvoiceData data) {
        return StructuredInvoiceData.builder()
                .brand(data.brand())
                .supplier(data.supplier())
                .masthead(data.masthead())
                .billTo(data.billTo())
                .shipTo(data.shipTo())
                .summary(data.summary())
                .serviceLines(data.serviceLines())
                .totals(data.totals())
                .payment(data.payment())
                .notes(data.notes())
                .currencyCode(data.currencyCode());
    }
}
