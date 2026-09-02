package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.CONTINUATION_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PAGE_NUMBER_BAND;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.PAGE_NUMBER_SIZE;

/**
 * Payments — a lavender-and-navy invoice: a masthead crossed by a diagonal
 * band, a half-split issuer and metadata header, two addressed parties, a
 * marked line-item table, a settlement row pairing bank details against the
 * totals, a note block and a two-cell document footer.
 *
 * <h2>It flows</h2>
 *
 * <p>Unlike most presets in this package the sheet is not drawn for one page.
 * The design shows six service lines and a real month of usage brings dozens,
 * so the table is the region that grows: its header repeats on every page it
 * reaches, a continuation page reserves a deeper bottom margin than page one,
 * and every page carries its number. Pagination here is load-bearing rather
 * than incidental — a financial record that runs over has to make a missing
 * page detectable.</p>
 *
 * <h2>The lockup is the caller's</h2>
 *
 * <p>The design's mark occupies a measured 136 × 55.3 design px box beside the
 * title. What fills it comes from {@code InvoiceBrand}: a document that brings
 * a logo has it drawn to that height, so a wider or narrower mark keeps the
 * design's optical weight; a document that brings only a name has the name set
 * as a wordmark. The templates artifact carries no mark of its own.</p>
 *
 * <h2>One column, not five</h2>
 *
 * <p>The table has an outer box and a rule between every row and no interior
 * verticals, which is exactly what a single-column table draws — a cell strokes
 * all four of its own edges, and a one-column table has no interior vertical
 * edge. The five columns are a row inside each cell, on shares of the table's
 * width. Building it the other way round, with the box from a stroked section
 * wrapped round the table, is wrong on a document that paginates: a section's
 * box fills its page fragment rather than hugging its rows, so every
 * continuation page would end with an empty bordered strip.</p>
 *
 * <h2>Every horizontal pair inside a cell is a wrapped row</h2>
 *
 * <p>A row nested directly in a row cell is refused by the layout compiler, so
 * each such pair — a party's mark and its label, a line's mark and its text,
 * the card's fields, the totals grid — is a row wrapped in a single layer of a
 * stack.</p>
 *
 * <h2>What the document says, and what the preset chooses</h2>
 *
 * <p>A service line names its own mark through
 * {@code InvoiceServiceLines.Line.icon()}, from this preset's own vocabulary,
 * and an unknown token is reported as a data error naming the set. A line with
 * no token is drawn without a mark. The other four marks are the preset's:
 * which glyph opens the bill-to block, the payment card, the notes and the two
 * footer cells is a property of the design rather than of the document.</p>
 *
 * <p>Figures are written with the locale stated rather than inherited, so the
 * same data renders the same sheet on any machine. The currency is named once
 * in the amount column's header and its symbol carried on each figure.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato throughout. The templates artifact carries no
 * fonts — register the family on the session, or the engine substitutes and the
 * measured geometry no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = PaymentsInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class PaymentsInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "payments-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Payments Invoice";

    private PaymentsInvoice() {
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
            document.pageMargins(List.of(PageMarginRule.from(2, CONTINUATION_MARGIN)));
            renderPageNumber(document);

            String currency = data.currencyCode();
            document.pageFlow(page -> {
                page.name("PaymentsInvoice").padding(DocumentInsets.zero()).spacing(0);
                PaymentsMasthead.render(page, data.brand(), data.supplier(), data.masthead());
                PaymentsParties.render(page, data.billTo(), data.shipTo());
                PaymentsLines.render(page, data.serviceLines(), currency);
                PaymentsSettlement.render(page, data.payment(), data.totals(), currency);
                PaymentsClosing.render(page, data.notes(), data.payment());
            });
        }

        /**
         * The only chrome the sheet has, and an addition rather than a
         * reproduction: the design is one page and carries no number.
         *
         * <p>The height is stated because a default band would take more than
         * the bottom margin leaves and push the footer onto a second page.
         * Sized to the margin, the band sits inside it rather than taking
         * content space, and the type is sized to the band rather than the
         * other way round.</p>
         */
        private static void renderPageNumber(DocumentSession document) {
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    // The zone's own default is the standard-14 face, which
                    // would leave the page number the one line on the sheet in
                    // a family the design never uses.
                    .fontName(FACE)
                    .height((float) PAGE_NUMBER_BAND)
                    .fontSize((float) PAGE_NUMBER_SIZE)
                    .textColor(MUTED)
                    .showSeparator(false)
                    .build());
        }
    }
}
