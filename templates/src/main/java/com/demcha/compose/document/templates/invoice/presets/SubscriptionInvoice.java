package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CHROME_GREY;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CHROME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.FOOTER_H;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.FOOTER_RESERVE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.MARGIN_T;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAGE;

/**
 * Subscription — an invoice for seats and licences billed with tax per line: a
 * lockup against the title, the supplier's details beside a metadata panel whose
 * rows each open with a coloured bar, two addressed parties under coloured
 * underlines, a six-column table carrying a tax rate on every line, the notes
 * beside the totals, a marked payment band, a four-segment strip, and a closing
 * band that bleeds to three paper edges.
 *
 * <h2>Tax on the line, not only in the sum</h2>
 *
 * <p>This is the first preset in the family to draw
 * {@code InvoiceServiceLines.Line.vatRate()} and its caption, because it bills
 * subscriptions in a jurisdiction that prints the rate per line rather than only
 * as a total. It is also the one that numbers its lines: a line states its own
 * number through {@code lineNumber()}, and one that states none is numbered by
 * its position.</p>
 *
 * <h2>Colour by position, never by meaning</h2>
 *
 * <p>The metadata bars, the party underlines and the closing strip all draw from
 * one four-colour cycle indexed by position — the fourth metadata row is amber
 * because it is fourth. Nothing in the document ever names a colour.</p>
 *
 * <h2>The page has no side margins</h2>
 *
 * <p>The strip and the closing band have to reach the paper edges, and they do
 * it by being the only blocks that carry no horizontal padding — every other
 * block carries the content inset itself. The bottom margin reserves the
 * enumeration's strip, and the band reaches the bottom edge by bleeding rather
 * than by being the last thing on the page, so reserving it costs the design
 * nothing but a few points of the band's lower padding.</p>
 *
 * <h2>Every cell is unstroked</h2>
 *
 * <p>A cell's stroke draws all four of its edges and there is no per-edge
 * control, so any stroke that buys the table's horizontal rule also buys five
 * verticals it does not have. The rule under the header is an accent on the
 * header cell, and the rule under each data row is a row of its own.</p>
 *
 * <h2>Money carries its symbol</h2>
 *
 * <p>This design writes money with the currency's symbol against the digits
 * rather than with a code in front or a caption above, so every figure carries
 * its own currency and none of the columns state one. Figures state their locale
 * rather than inheriting it.</p>
 *
 * <p>That is also this preset's one boundary. The columns are measured for a
 * one-character mark, so a currency the runtime knows only by its three-letter
 * code — several ordinary European ones among them — does not fit the unit-price
 * column, and the render is refused there rather than letting the figure run
 * under its neighbour. A document billed in such a currency wants a preset that
 * states the currency in the column caption instead.</p>
 *
 * <h2>The lockup is the caller's</h2>
 *
 * <p>The masthead draws the logo from {@code InvoiceBrand} at the design's
 * measured width, and the brand's name beside it as the wordmark. The templates
 * artifact carries no mark of its own.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Fira Sans Condensed, chosen by measurement: the usual
 * humanist substitute matches this design's mixed-case widths but sets uppercase
 * about 20% wider per unit of cap height, which shows immediately as headings
 * and their coloured rules overshooting. The templates artifact carries no fonts
 * — register the family on the session, or the engine substitutes and the
 * measured geometry no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = SubscriptionInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class SubscriptionInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "subscription-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Subscription Invoice";

    private SubscriptionInvoice() {
    }

    /**
     * Creates the template.
     *
     * @return a template composing a {@link StructuredInvoiceData}
     */
    public static DocumentTemplate<StructuredInvoiceData> create() {
        return new Template();
    }

    private record Template() implements DocumentTemplate<StructuredInvoiceData> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, StructuredInvoiceData data) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(data, "data");

            document.pageSize(PAGE);
            // No side margins and no real bottom margin: the strip and the
            // closing band have to reach the paper edges, and they do it by
            // being the only blocks that carry no horizontal padding.
            document.margin(new DocumentInsets(MARGIN_T, 0, FOOTER_RESERVE, 0));
            renderPageNumber(document);

            String currency = data.currencyCode();
            document.pageFlow(page -> {
                page.name("SubscriptionInvoice");
                page.padding(DocumentInsets.zero());
                page.spacing(0);
                SubscriptionMasthead.renderBrandHeader(page, data.brand(), data.masthead());
                SubscriptionMasthead.renderIdentityBand(page, data.supplier(), data.masthead());
                SubscriptionParties.render(page, data.billTo(), data.shipTo());
                SubscriptionLines.render(page, data.serviceLines(), currency);
                SubscriptionSettlement.render(page, data.notes(), data.totals(), currency);
                SubscriptionClosing.renderPaymentBand(page, data.payment());
                SubscriptionClosing.renderPaymentNote(page, data.payment());
                SubscriptionClosing.renderStrip(page);
                SubscriptionClosing.renderBand(page, data.payment(), data.supplier());
            });
        }

        /**
         * The enumeration. The design has none — it is one sheet — but the
         * document flows, and a reader holding page 1 of 2 has to be able to tell
         * that page 2 is missing.
         *
         * <p>It sits in the foot, over the closing band's fill. The face is
         * stated because the zone's own default is the standard-14 one, which
         * would leave the number the single line on the sheet in a family the
         * design never uses.</p>
         */
        private static void renderPageNumber(DocumentSession document) {
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) FOOTER_H)
                    .fontName(FACE)
                    .fontSize((float) CHROME_SIZE)
                    .textColor(CHROME_GREY)
                    .centerText("Page {page} of {pages}")
                    .showSeparator(false)
                    .numbering(DocumentPageNumbering.DEFAULT)
                    .build());
        }
    }
}
