package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceDocumentSpec;

import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_HEIGHT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CLOSING_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CLOSING_TO_BANNER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.FOLIO_ZONE_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.INK_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MASTHEAD_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MASTHEAD_RIGHT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MICRO_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ON_DARK;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_RIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_TOP;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAPER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PARTIES_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PARTIES_TO_TABLE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.RULE_TO_CLOSING;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.RULE_TO_PARTIES;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SIDEBAR_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SIDEBAR_WIDTH_RATIO;

/**
 * Luma Studio Invoice — a studio invoice built around a cream sidebar: the
 * brand lockup sits on a terracotta block at the top of that column, an
 * ornament runs down the rest of it, and the billing sheet is set in the
 * page beside them.
 *
 * <p>The sheet reads top to bottom: the sender over the document title and
 * its metadata, the two parties across a rule, the priced service lines, the
 * totals stack closing on a filled total-due band, and the notes and payment
 * details side by side above the sign-off band at the foot.</p>
 *
 * <p>Long invoices flow. The line-items table repeats its dark header on the
 * next page, the totals stack and each closing block stay whole, and the paper
 * tint, the cream sidebar column and the dark foot band are page backgrounds,
 * so every page carries the same frame and the folio always has a dark ground.
 * Every page reserves the band's height as bottom margin, so no row runs under
 * it.</p>
 *
 * <p>The sign-off carries its own strip rather than relying on that foot band.
 * On a one-page invoice the two land together; on a longer one the flow ends
 * where the closing blocks end, and a sign-off that relied on the background
 * would set white words on pale paper. It is flow content only because footer
 * chrome carries text today and this band needs two faces and a disc — once a
 * footer zone can hold a node, the sign-off belongs in one.</p>
 *
 * <p>The preset owns its session geometry — A4, the margins, the page fills
 * and the folio zone — so callers leave the session unconfigured; a margin
 * set by the caller is overwritten. It consumes the structured invoice model
 * ({@link StructuredInvoiceDocumentSpec}): every heading, label and column
 * title is content, so the same composition prints an invoice in another
 * studio's wording without a fork. Amounts are prefixed with the mark of the
 * data's own currency code. The preset draws in Carlito, Spectral and Tinos;
 * its marks — the contact icons, the two closing-block badges, the heart on
 * the band and the sidebar sprig — are template chrome and ship inside the
 * artifact.</p>
 *
 * @since 2.4.0
 */
public final class LumaStudioInvoice {

    /**
     * Stable template identifier.
     */
    public static final String ID = "invoice-luma-studio";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Luma Studio Invoice";

    private LumaStudioInvoice() {
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
            String currency = currencyMark(data.currencyCode());

            renderChrome(document);

            document.pageFlow(page -> {
                page.name("LumaStudioInvoice").spacing(0).padding(DocumentInsets.zero());

                page.add(LumaStudioSidebar.render(data.brand()));

                page.addRow("Masthead", row -> row
                        .spacing(0)
                        .gap(0)
                        .weights(MASTHEAD_LEFT_WEIGHT, MASTHEAD_RIGHT_WEIGHT)
                        .addSection("SupplierHeader",
                                section -> LumaStudioMasthead.renderSupplier(section, data.supplier()))
                        .addSection("InvoiceHeader",
                                section -> LumaStudioMasthead.renderTitle(section, data.masthead())));

                page.addLine(LumaStudioMasthead::renderRule);

                // The gap down to the table is this row's bottom margin rather
                // than a spacer: a block of its own would take the parties'
                // place at a page break and leave the gap stranded.
                page.addRow("Parties", row -> row
                        .spacing(0)
                        .gap(0)
                        .weights(PARTIES_LEFT_WEIGHT, 1.0 - PARTIES_LEFT_WEIGHT)
                        .margin(new DocumentInsets(RULE_TO_PARTIES, 0, PARTIES_TO_TABLE, 0))
                        .addSection("BillTo",
                                section -> LumaStudioMasthead.renderParty(section, data.billTo(), false))
                        .addSection("ShipTo",
                                section -> LumaStudioMasthead.renderParty(section, data.shipTo(), true)));

                LumaStudioLines.render(page, data.serviceLines(), currency);

                page.addSection("Totals",
                        section -> LumaStudioClosing.renderTotals(section, data.totals(), currency));

                page.addLine(LumaStudioClosing::renderFooterRule);

                page.addRow("Closing", row -> row
                        .spacing(0)
                        .gap(0)
                        .weights(CLOSING_LEFT_WEIGHT, 1.0 - CLOSING_LEFT_WEIGHT)
                        .margin(new DocumentInsets(RULE_TO_CLOSING, 0, 0, 0))
                        .addSection("Notes",
                                section -> LumaStudioClosing.renderNotes(section, data.notes()))
                        .addSection("PaymentDetails",
                                section -> LumaStudioClosing.renderPayment(section, data.payment())));

                page.addSection("ClosingBannerBlock", block -> {
                    block.spacing(0).margin(new DocumentInsets(CLOSING_TO_BANNER, 0, 0, 0));
                    LumaStudioClosing.renderBanner(block, data.payment());
                });
            });
        }

        /**
         * The page frame: the paper tint, the cream sidebar column and the
         * dark foot band are page-ratio background fills, and the folio is
         * FOOTER-zone chrome, so all four repeat on every page.
         *
         * <p>The bottom margin is the band's height, so the last row on a page
         * stops above it rather than running underneath, and the sign-off block
         * reaches down into that reserved space by its own negative bottom
         * margin rather than pushing the flow onto another page.</p>
         */
        private static void renderChrome(DocumentSession document) {
            document.pageSize(PAGE)
                    .margin(new DocumentInsets(
                            PAGE_MARGIN_TOP, PAGE_MARGIN_RIGHT, BANNER_HEIGHT, PAGE_MARGIN_LEFT))
                    .pageBackgrounds(List.of(
                            PageBackgroundFill.fullPage(PAPER),
                            PageBackgroundFill.leftColumn(SIDEBAR_WIDTH_RATIO, SIDEBAR_SURFACE),
                            PageBackgroundFill.bottomBand(BANNER_HEIGHT_RATIO, INK_SURFACE)))
                    .footer(DocumentHeaderFooter.builder()
                            .zone(DocumentHeaderFooterZone.FOOTER)
                            .height((float) FOLIO_ZONE_HEIGHT)
                            .rightText("Page {page} of {pages}")
                            .fontSize((float) MICRO_SIZE)
                            .textColor(ON_DARK)
                            .showSeparator(false)
                            .build());
        }

        /**
         * The mark amounts are written with, for the currency the data states.
         *
         * <p>The model carries the ISO code, and this sheet prints a symbol
         * against every figure, so the code is the authority and the symbol is
         * derived from it — two stated fields could disagree, and then the
         * document would contradict itself. A code the platform does not know
         * prints as itself rather than as nothing, so an amount is never left
         * bare.</p>
         */
        private static String currencyMark(String currencyCode) {
            if (currencyCode.isBlank()) {
                return "";
            }
            try {
                return Currency.getInstance(currencyCode).getSymbol(Locale.ENGLISH);
            } catch (IllegalArgumentException unknownCode) {
                return currencyCode;
            }
        }
    }
}
