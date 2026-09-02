package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.FOOTER_BAND_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.FOOTER_FILL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.FOOTER_TEXT_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.FOOTER_ZONE_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HEADER_GAP;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HEADER_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HEADER_RIGHT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.LOWER_GAP;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.LOWER_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PARTIES_LEFT_WEIGHT;

/**
 * Consulting Invoice — the professional-services invoice preset: a
 * corporate masthead over priced service lines, with the bank details and
 * the closing notes side by side beneath them.
 *
 * <p>The masthead pairs the sender's brand lockup and contact channels with
 * the document title and its metadata; the body bills a recipient, states
 * what the invoice covers, and prices the work line by line with a service
 * period and a unit against each line; the totals stack closes with an
 * emphasized total band. Long invoices flow: the line-items table repeats
 * its header on the next page, and the totals stack stays whole.</p>
 *
 * <p>The preset owns its session geometry — A4, the margin published as
 * {@link #RECOMMENDED_MARGIN}, the near-white page fill and the quiet
 * footer band — and draws the legal line and page numbers as footer chrome
 * rather than flow content, so they repeat on every page. The legal line is
 * composed from the supplier's registered name and registration, so those
 * are stated once. A session margin set by the caller is overwritten.</p>
 *
 * <p>The preset draws in {@code FontName.POPPINS} and consumes the
 * structured invoice model ({@link StructuredInvoiceDocumentSpec}): its
 * headings, labels and column titles are content, so the same composition
 * prints an invoice in another business's wording without a fork. The
 * brand logo arrives through the data as an image the caller owns; a
 * document without one falls back to the wordmark lockup. The contact
 * marks, the bank badge and the calendar are template chrome and ship
 * inside the artifact.</p>
 */
public final class ConsultingInvoice {

    /**
     * Stable template identifier.
     */
    public static final String ID = "invoice-consulting";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Consulting Invoice";

    /**
     * The session margin (in points) the preset sets. It is published so a
     * caller can measure against the same frame; the preset applies it
     * itself inside {@code compose}, together with the page size and the
     * page fills, so callers leave the session unconfigured.
     */
    public static final double RECOMMENDED_MARGIN = ConsultingStyles.PAGE_MARGIN;

    private ConsultingInvoice() {
    }

    /**
     * Builds the preset.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<StructuredInvoiceDocumentSpec> create() {
        return new Template();
    }

    private record Template() implements DocumentTemplate<StructuredInvoiceDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, StructuredInvoiceDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            StructuredInvoiceData data = Objects.requireNonNull(spec, "spec").invoice();

            renderChrome(document, data.supplier());

            document.pageFlow(page -> page
                    .name("ConsultingInvoice")
                    .spacing(0)
                    .addRow("Masthead", row -> row
                            .gap(HEADER_GAP)
                            .weights(HEADER_LEFT_WEIGHT, HEADER_RIGHT_WEIGHT)
                            .verticalAlign(RowVerticalAlign.TOP)
                            .addSection("SupplierHeader",
                                    section -> ConsultingMasthead.renderSupplier(section, data))
                            .addSection("InvoiceHeader",
                                    section -> ConsultingMasthead.renderInvoiceHeader(section, data)))
                    .addSpacer(spacer -> spacer.height(16))
                    .addRow("PartiesAndSummary", row -> row
                            .gap(35)
                            .weights(PARTIES_LEFT_WEIGHT, 1.0 - PARTIES_LEFT_WEIGHT)
                            .verticalAlign(RowVerticalAlign.TOP)
                            .addSection("BilledTo",
                                    section -> ConsultingBody.renderBilledTo(section, data))
                            .addSection("InvoiceSummary",
                                    section -> ConsultingBody.renderSummary(section, data)))
                    .addSpacer(spacer -> spacer.height(10))
                    .addTable(table -> ConsultingBody.renderServiceLines(table, data))
                    .addSection("Totals", section -> ConsultingBody.renderTotals(section, data))
                    .addLine(line -> line
                            .horizontal(CONTENT_WIDTH)
                            .thickness(0.8)
                            .color(HAIRLINE)
                            .margin(DocumentInsets.symmetric(5, 0)))
                    .addRow("PaymentAndNotes", row -> row
                            .gap(LOWER_GAP)
                            .weights(LOWER_LEFT_WEIGHT, 1.0 - LOWER_LEFT_WEIGHT)
                            .verticalAlign(RowVerticalAlign.TOP)
                            .addSection("PaymentInformation",
                                    section -> ConsultingClosing.renderPayment(section, data))
                            .addSection("Notes",
                                    section -> ConsultingClosing.renderNotes(section, data))));
        }

        /**
         * The page frame and the band that closes every page: the near-white
         * page fill and the quiet footer band are page-ratio background
         * fills, and the legal line and page numbers are FOOTER-zone chrome
         * drawn after layout, so both repeat on every page of a long
         * invoice.
         */
        private static void renderChrome(DocumentSession document, InvoiceContactBlock supplier) {
            document.pageSize(PAGE)
                    .margin(DocumentInsets.of(PAGE_MARGIN))
                    .pageBackgrounds(List.of(
                            PageBackgroundFill.fullPage(PAGE_BACKGROUND),
                            PageBackgroundFill.bottomBand(FOOTER_BAND_RATIO, FOOTER_FILL)))
                    .footer(DocumentHeaderFooter.builder()
                            .zone(DocumentHeaderFooterZone.FOOTER)
                            .height((float) FOOTER_ZONE_HEIGHT)
                            .centerText(footerLine(supplier))
                            .rightText("Page {page} / {pages}")
                            .fontSize((float) FOOTER_TEXT_SIZE)
                            .textColor(MUTED)
                            .showSeparator(false)
                            .build());
        }

        /**
         * The legal line: the supplier's registered name and, when it is
         * registered, its labelled registration number. A supplier without
         * a registration prints its name alone rather than a name with a
         * dangling separator.
         */
        private static String footerLine(InvoiceContactBlock supplier) {
            String registration = (supplier.registrationLabel() + " "
                    + supplier.registrationNumber()).trim();
            if (registration.isEmpty()) {
                return supplier.legalName();
            }
            if (supplier.legalName().isEmpty()) {
                return registration;
            }
            return supplier.legalName() + "   |   " + registration;
        }
    }
}
