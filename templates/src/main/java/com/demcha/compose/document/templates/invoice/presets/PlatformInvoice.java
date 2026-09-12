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

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.FACE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.FOOTER_BAND_H;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.FOOTER_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.PAGE_MARGIN;

/**
 * Platform — an invoice for platform usage billed by region: a lockup against
 * the title, the supplier split from the invoice's metadata by a full-height
 * hairline, two addressed parties, a six-column usage table, an outlined bank
 * panel beside the totals and the due-date card, and an identity band closing
 * the sheet.
 *
 * <h2>Where, beside what</h2>
 *
 * <p>The table is six columns wide because this design bills the same service in
 * more than one place and prints where beside what:
 * {@code DESCRIPTION | SERVICE | REGION | USAGE | UNIT PRICE | AMOUNT}. A line
 * names its region through {@code InvoiceServiceLines.Line.region()}, and a
 * document with one location per invoice leaves it blank.</p>
 *
 * <h2>Two scales, on purpose</h2>
 *
 * <p>The design is 1.50 aspect where A4 is 1.414, so one conversion constant
 * cannot serve both axes. Widths, x-offsets and type sizes go through the
 * horizontal scale because that is the axis where advance widths have to fit;
 * heights, pitches and gaps go through the vertical one, which is 6.3% tighter;
 * marks and discs go through their mean, and are therefore about 3% out in each
 * axis rather than 6% out in one.</p>
 *
 * <h2>Rules are rows</h2>
 *
 * <p>A table's rules come from each cell's own style and cover all four of that
 * cell's edges, so any stroke that buys a horizontal separator also buys five
 * verticals — which this design has nowhere inside its table. Every cell is
 * stroked at zero width and the separators are rows of their own: one cell
 * spanning every column, zero padding, and a full-width line as its content.</p>
 *
 * <h2>It flows</h2>
 *
 * <p>The design shows five service lines and real usage brings dozens, so the
 * table is the region that grows: its header repeats on every page it reaches,
 * a continuation page keeps the first page's geometry, and every page carries
 * its number. The bottom margin is the enumeration's band exactly — reserving
 * less lets a continuation page's last row run into it, a defect page one is
 * structurally unable to show.</p>
 *
 * <h2>A rate is not an amount, and usage is not a tally</h2>
 *
 * <p>A unit price is written at the precision it is quoted at, between two
 * places and four: a rate of 0.0670 rounded to an amount's two places is a
 * different price. Usage that names a unit carries two places even on a whole
 * number, because the meter reads to that precision; usage that names none is a
 * count of whole things and is written bare. The currency is stated once per
 * money column and the figures under it are bare. Figures state their locale
 * rather than inheriting it.</p>
 *
 * <h2>The lockup is the caller's</h2>
 *
 * <p>The design opens and closes with the same mark. Both come from
 * {@code InvoiceBrand}: the masthead draws the logo at the design's measured
 * width and the identity band at its smaller one, or the brand's name when there
 * is no logo. The templates artifact carries no mark of its own.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Barlow throughout, and the preset's pitches are solved
 * from Barlow's own cap height and ascender. The templates artifact carries no
 * fonts — register the family on the session, or the engine substitutes and that
 * arithmetic no longer describes the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = PlatformInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class PlatformInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "platform-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Platform Invoice";

    private PlatformInvoice() {
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
            // A continuation page has the same geometry as the first: it loses
            // the masthead only because that is body content already drawn.
            // Stating the rule keeps the page model decided rather than
            // inherited by accident.
            document.pageMargins(List.of(PageMarginRule.from(2, PAGE_MARGIN)));
            renderPageNumber(document);

            document.pageFlow(page -> {
                page.name("PlatformInvoice").padding(DocumentInsets.zero()).spacing(0);
                PlatformMasthead.renderBrandHeader(page, data.brand(), data.masthead());
                PlatformMasthead.renderIdentityRow(page, data.supplier(), data.masthead());
                PlatformMasthead.renderIdentityRule(page);
                PlatformParties.render(page, data.billTo(), data.shipTo());
                PlatformLines.render(page, data.serviceLines(), data.currencyCode());
                PlatformSettlement.render(page, data.payment(), data.totals(),
                        data.currencyCode());
                PlatformClosing.renderNoteRule(page);
                PlatformClosing.renderNote(page, data.notes(), data.supplier());
                PlatformClosing.renderIdentityRule(page);
                PlatformClosing.renderIdentity(page, data.brand(), data.supplier());
            });
        }

        /**
         * The only chrome the sheet has. The design's own identity band cannot be
         * chrome — a header/footer zone carries left, centre and right strings
         * and nothing else, and the band holds a lockup — so page identity is
         * carried by the enumeration alone.
         *
         * <p>The height is stated because it is also what positions the number
         * inside the band: the engine seats the line box at the band's top and
         * the band's foot is the paper's, so the number rises point for point
         * with it. The face is stated because the zone's own default is the
         * standard-14 one, which would leave the number the single line on the
         * sheet in a family the design never uses.</p>
         */
        private static void renderPageNumber(DocumentSession document) {
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) FOOTER_BAND_H)
                    .fontName(FACE)
                    .fontSize((float) FOOTER_SIZE)
                    .textColor(MUTED)
                    .centerText("Page {page} of {pages}")
                    .showSeparator(false)
                    .build());
        }
    }
}
