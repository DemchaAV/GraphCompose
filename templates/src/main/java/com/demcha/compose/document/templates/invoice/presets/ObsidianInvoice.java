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

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CONTINUATION_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.META_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAGE_BG;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAGE_NUMBER_BAND;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAGE_NUMBER_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.blockGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.rule;

/**
 * Obsidian — a dark invoice built from rounded cards: a wordmark over the
 * document's name against a ruled metadata stack, the issuer and the billed
 * party on discs in cards of their own, a line-item table in a third, the totals
 * in a fourth, the notes and payment details side by side in a fifth and sixth,
 * and a closing band under a hairline.
 *
 * <h2>The sheet is dark, and it is the page that is dark</h2>
 *
 * <p>The fill is set on the page rather than on the flow. A container's fill is
 * bounded by its content height, so a continuation page carrying three line
 * items would show two thirds of a white sheet under them; the page background
 * covers the paper whatever the content does.</p>
 *
 * <h2>Sizes are solved from ink, not from cap height</h2>
 *
 * <p>The design's own face is not bundled, and matching a cap height alone would
 * still set every string to the wrong width. Each size here is solved from the
 * measured ink width of the string it sets, with the substitute's width ratio
 * applied once — and every size is named, in the styles, by the string it came
 * from. Vertical positions are cap tops rather than box edges, because ink is
 * what the design can be measured on.</p>
 *
 * <h2>Six columns inside one</h2>
 *
 * <p>A cell's stroke draws all four of its edges, so a six-column table would
 * draw five verticals this design does not have. The table is one column wide
 * and each cell carries a composed row of six, with the rule under a row drawn
 * inside that row's own content — which is what lets the last row leave the rule
 * out and take the card's lower padding instead.</p>
 *
 * <h2>The tax column is worked out, not asked for</h2>
 *
 * <p>This design gives the tax its own money column where the model carries a
 * rate. The figure is not a second thing to state: a line already gives the
 * quantity, the unit price and the total it comes to, so the tax is the
 * difference between what was charged and what the goods cost. Reading it off
 * the line is also what keeps the column and the total consistent when either
 * moves.</p>
 *
 * <h2>The marks are the caller's, and there are two of them</h2>
 *
 * <p>The issuer's disc — in its party card and again in the closing band —
 * carries the logo from {@code InvoiceBrand} when there is one, the brand's own
 * monogram when it states one, and initials taken from the name otherwise. The
 * billed party's disc always shows initials taken from its name. The templates
 * artifact carries no mark of its own; the masthead sets the brand's name as
 * type, which is what this design does with it.</p>
 *
 * <h2>The closing band is the design's tightest place</h2>
 *
 * <p>Its right column is the design's own measured width, which holds an
 * address of about twenty characters. A longer one wraps there rather than
 * overflowing — correct, but it makes the band a line taller, and on a sheet
 * whose content already reaches the foot that is enough to carry the band to a
 * second page. The design leaves very little under it.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Gothic A1, which is the family every size above was
 * solved against. The templates artifact carries no fonts — register it on the
 * session, or the engine substitutes and every size is solved for type that is
 * not there.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = ObsidianInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class ObsidianInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "obsidian-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Obsidian Invoice";

    private ObsidianInvoice() {
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
            document.margin(PAGE_MARGIN);
            document.pageMargins(List.of(PageMarginRule.from(2, CONTINUATION_MARGIN)));
            // Not a fill on the flow: a container's fill is bounded by content
            // height, and a continuation page holding three line items would show
            // two thirds of a white sheet under them.
            document.pageBackground(PAGE_BG);
            renderPageNumber(document);

            String currency = data.currencyCode();
            document.pageFlow(page -> {
                page.name("ObsidianInvoice").padding(DocumentInsets.zero()).spacing(0);
                ObsidianMasthead.render(page, data.brand(), data.masthead());
                page.addLine(line -> rule(line, "HeaderDivider", CONTENT_W, HAIRLINE)
                        .margin(new DocumentInsets(
                                Math.max(0, blockGap(28.5, META_VALUE_SIZE, 0)), 0, px(27), 0)));
                ObsidianParties.render(page, data.brand(), data.supplier(), data.billTo());
                ObsidianLines.render(page, data.serviceLines(), currency);
                ObsidianSettlement.render(page, data.totals(), currency);
                ObsidianCards.render(page, data.notes(), data.payment());
                page.addLine(line -> rule(line, "FooterDivider", CONTENT_W, HAIRLINE)
                        .margin(new DocumentInsets(px(23), 0,
                                Math.max(0, px(1417 - 1398.5) - RULE_BOX), 0)));
                ObsidianClosing.render(page, data.brand(), data.supplier(), data.payment());
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
                    .centerText("Page {page} of {pages}")
                    .height((float) PAGE_NUMBER_BAND)
                    .fontName(FACE)
                    .fontSize((float) PAGE_NUMBER_SIZE)
                    .textColor(MUTED)
                    .showSeparator(false)
                    .build());
        }
    }
}
