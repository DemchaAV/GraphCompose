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

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.IDENTITY_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.MASTHEAD_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PAGE_MARGIN_FIRST;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PAGE_MARGIN_LATER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PAGE_NUMBER_BAND;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PAGE_NUMBER_SIZE;

/**
 * Merchant — a commerce invoice with a green accent: a lockup against the title
 * over a short accent rule, the supplier's details beside the invoice's
 * metadata, two addressed parties on filled discs, a line-item table whose rows
 * are bordered boxes, a bank panel beside the totals and the due-by card, a
 * closing note, and an identity band closing the sheet on a marked tile.
 *
 * <h2>Each table row is one cell</h2>
 *
 * <p>A cell's stroke draws all four of its edges, which is exactly what this
 * design's rows are: a soft box round each one and no interior verticals. So a
 * row is a single cell spanning every column with the five columns composed
 * inside it — stroking five real cells would draw four verticals the design does
 * not have.</p>
 *
 * <h2>An inline mark sets the line box</h2>
 *
 * <p>The supplier's contact rows are the one block whose line box is not its
 * type: a mark taller than the text sets the box, so a pitch measured from the
 * text and solved against the text lands short by the difference. Those pitches
 * are solved against the mark instead, which is measured rather than assumed —
 * the first render asked for 28.5 design pixels and laid out 31.</p>
 *
 * <h2>Two blocks disagree about where the page starts</h2>
 *
 * <p>The lockup's ink and the title's box sit at different heights, so the flow
 * opens at the earlier of the two and each carries the difference as its own
 * margin. That is also why the rules under the masthead and the identity band
 * are placed against those blocks' computed heights rather than against a
 * guessed one.</p>
 *
 * <h2>It flows</h2>
 *
 * <p>The design shows four service lines and real billing brings dozens, so the
 * table is the region that grows: its header repeats on every page it reaches,
 * and a continuation page reserves the whole enumeration band where page one
 * does not — page one ends in a footer band the design already leaves room
 * under, while a continuation page ends in table rows, and a table row has no
 * hole in its middle for a number to sit in.</p>
 *
 * <h2>What this port leaves out</h2>
 *
 * <p>The design closes with a row of three discs carrying social-platform marks.
 * Those marks belong to their platforms, and the templates artifact does not
 * redistribute other companies' trademarks, so neither they nor the rule that
 * divided them off are drawn. The band keeps the tile and the issuer's identity,
 * which is what identifies the sheet.</p>
 *
 * <h2>Money and marks</h2>
 *
 * <p>The currency is stated once in each money column's caption and the figures
 * under it are written bare, with the code carried only on the grand total,
 * which stands under no caption. A quantity of nothing — a fee charged as it
 * falls rather than per unit — prints the design's own dash, because a zero in a
 * quantity column reads as none delivered rather than as not counted. A service
 * line names its own mark from this preset's vocabulary, and an unknown token is
 * reported as a data error naming the set.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Gothic A1, which is the family every size was solved
 * against. The templates artifact carries no fonts — register it on the session,
 * or the engine substitutes and every size is solved for type that is not
 * there.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = MerchantInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class MerchantInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "merchant-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Merchant Invoice";

    private MerchantInvoice() {
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
            document.margin(PAGE_MARGIN_FIRST);
            document.pageMargins(List.of(PageMarginRule.from(2, PAGE_MARGIN_LATER)));
            renderPageNumber(document);

            String currency = data.currencyCode();
            document.pageFlow(page -> {
                page.name("MerchantInvoice").padding(DocumentInsets.zero()).spacing(0);
                MerchantMasthead.renderBrandHeader(page, data.brand(), data.masthead());
                MerchantMasthead.renderTitleRule(page, MASTHEAD_H);
                MerchantMasthead.renderIdentityRow(page, data.supplier(), data.masthead());
                MerchantMasthead.renderIdentityRule(page, IDENTITY_H);
                MerchantParties.render(page, data.billTo(), data.shipTo());
                MerchantLines.render(page, data.serviceLines(), currency);
                MerchantSettlement.render(page, data.payment(), data.totals(), currency);
                MerchantClosing.renderNoteRule(page);
                MerchantClosing.renderNote(page, data.notes(), data.supplier());
                MerchantClosing.renderIdentityRule(page);
                MerchantClosing.renderIdentity(page, data.brand(), data.supplier());
            });
        }

        /**
         * The enumeration. The design has none — it is one sheet — but the
         * document flows, and a reader holding page 1 of 2 has to be able to tell
         * that page 2 is missing.
         *
         * <p>The face is stated because the zone's own default is the standard-14
         * one, which would leave the number the single line on the sheet in a
         * family the design never uses.</p>
         */
        private static void renderPageNumber(DocumentSession document) {
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) PAGE_NUMBER_BAND)
                    .fontName(FACE)
                    .fontSize((float) PAGE_NUMBER_SIZE)
                    .textColor(INK)
                    .centerText("Page {page} of {pages}")
                    .showSeparator(false)
                    .build());
        }
    }
}
