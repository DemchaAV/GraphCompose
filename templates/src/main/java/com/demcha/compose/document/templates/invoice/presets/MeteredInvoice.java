package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.BAND_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_BAND_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_DARK;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_LINE_ONE_H;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_LINE_TWO_H;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_PAGE_H;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.FOOTER_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.INVERSE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PAGE_MARGIN;

/**
 * Metered — an invoice for usage that is billed by the meter: an orange-accented
 * masthead, the supplier against the invoice's own metadata, two addressed
 * parties, a line-item table whose marks sit on bordered tiles, a closing row
 * pairing the bank details against the totals and the due date, and a
 * full-bleed dark band along the foot of every page.
 *
 * <h2>It flows</h2>
 *
 * <p>The design shows five service lines and real usage brings dozens, so the
 * table is the region that grows: its header repeats on every page it reaches,
 * the bottom margin reserves the dark band on every page so no row runs into
 * the chrome, and every page carries its number.</p>
 *
 * <h2>One column, not five</h2>
 *
 * <p>The table has an outer box and a rule between every row and no interior
 * verticals, which is exactly what a single-column table draws: a cell strokes
 * all four of its own edges and a one-column table has no interior vertical
 * edge. The five columns are a row inside each cell. Built the other way round
 * — the box from a stroked section wrapped round the table — every continuation
 * page would end in an empty bordered strip.</p>
 *
 * <h2>A rate is not an amount</h2>
 *
 * <p>Metered rates are fractions of a currency unit: an hour of compute at
 * 0.0680 rounds to 0.07 at an amount's two places, which is a different price
 * and multiplies out to a different bill. A unit price is written at its own
 * scale, between two and four places; every other figure on the sheet is
 * written at two. Figures state their locale rather than inheriting it, so the
 * same data renders the same sheet on any machine.</p>
 *
 * <h2>The lockup is the caller's</h2>
 *
 * <p>The masthead draws the logo from {@code InvoiceBrand} at the design's
 * measured width, or the brand's name when there is no logo. The templates
 * artifact carries no mark of its own.</p>
 *
 * <h2>What the document says, and what the preset chooses</h2>
 *
 * <p>A service line names its own mark through
 * {@code InvoiceServiceLines.Line.icon()}, from this preset's own vocabulary,
 * and an unknown token is reported as a data error naming the set. A line that
 * names no mark keeps its tile, so the text lanes stay on their axis down the
 * column. The party marks, the bank mark, the calendar and the note's mark are
 * the preset's: which glyph opens which block is a property of the design
 * rather than of the document.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato throughout. The templates artifact carries no
 * fonts — register the family on the session, or the engine substitutes and the
 * measured geometry no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = MeteredInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class MeteredInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "metered-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Metered Invoice";

    private MeteredInvoice() {
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

            // Every length on the sheet is a share of the design's own grid, so
            // the preset sets the page and its margins rather than following
            // what the caller configured.
            document.pageSize(PAGE);
            document.margin(PAGE_MARGIN);
            renderFooterBand(document);
            renderPageFooter(document, data.supplier());

            String currency = data.currencyCode();
            document.pageFlow(page -> page
                    .name("MeteredInvoice")
                    .padding(DocumentInsets.zero())
                    .spacing(0)
                    .addSection("Body", body -> {
                        body.spacing(BAND_GAP);
                        MeteredMasthead.renderBrandHeader(body, data.brand(), data.masthead());
                        MeteredMasthead.renderIdentityRow(body, data.supplier(), data.masthead());
                        MeteredMasthead.renderParties(body, data.billTo(), data.shipTo());
                        MeteredLines.render(body, data.serviceLines(), currency);
                        MeteredSettlement.render(body, data.payment(), data.totals(), currency);
                        MeteredClosing.render(body, data.notes(), data.supplier());
                    }));
        }

        /**
         * The band is a page background, not a section fill and not a bleed: it
         * has to reach all three bottom edges and repeat on every page whatever
         * the content height is.
         */
        private static void renderFooterBand(DocumentSession document) {
            document.pageBackgrounds(
                    List.of(PageBackgroundFill.bottomBand(FOOTER_BAND_RATIO, FOOTER_DARK)));
        }

        /**
         * Repeating chrome. A footer entry carries left, centre and right
         * strings and nothing else, so each line of the band is its own entry
         * positioned by its distance from the paper's bottom edge: the
         * supplier's legal line, its address, and the enumeration on the band's
         * own centre line. The first is the disclosure the jurisdiction
         * requires when the supplier states one, and its name when it does
         * not — either way the band identifies who issued the sheet.
         *
         * <p>Each entry names the sheet's face. The zone's own default is the
         * standard-14 face, which would leave the band the one place on the
         * sheet in a family the design never uses.</p>
         */
        private static void renderPageFooter(DocumentSession document,
                                             InvoiceContactBlock supplier) {
            legalLine(document, FOOTER_LINE_ONE_H, supplier.legalFootnote().isBlank()
                    ? supplier.legalName()
                    : supplier.legalFootnote());
            legalLine(document, FOOTER_LINE_TWO_H, String.join(", ", supplier.addressLines()));
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) FOOTER_PAGE_H)
                    .fontName(FACE)
                    .fontSize((float) FOOTER_SIZE)
                    .textColor(INVERSE)
                    .rightText("Page {page} of {pages}")
                    .numbering(DocumentPageNumbering.builder()
                            .style(DocumentPageNumberStyle.DECIMAL)
                            .showOnFirstPage(true)
                            .build())
                    .build());
        }

        private static void legalLine(DocumentSession document, double height, String text) {
            if (text.isBlank()) {
                return;
            }
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) height)
                    .fontName(FACE)
                    .fontSize((float) FOOTER_SIZE)
                    .textColor(INVERSE)
                    .leftText(text)
                    .build());
        }
    }
}
