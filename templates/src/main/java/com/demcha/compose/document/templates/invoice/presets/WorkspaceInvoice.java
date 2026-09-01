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

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CONTINUATION_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PAGE_NUMBER_BAND;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PAGE_NUMBER_SIZE;

/**
 * Workspace — a violet-accented SaaS invoice: a brand masthead over an accent
 * bar, a half-split issuer and metadata header, two addressed parties on
 * discs, a line-item table whose marks sit on coloured tiles, a settlement row
 * pairing the bank details against the totals, and a closing band carrying the
 * wordmark.
 *
 * <h2>It flows</h2>
 *
 * <p>The design shows four service lines and real data brings dozens, so the
 * table is the region that grows: its header repeats on every page it reaches,
 * a continuation page reserves a deeper bottom margin than page one, and every
 * page carries its number.</p>
 *
 * <p>Numbering every page including the first is a departure from the design,
 * which is a single sheet and has none. Suppressing page one would match it and
 * still make a missing page detectable — a document that opens on "Page 2 of 3"
 * is obviously incomplete — but a page without a number cannot be told from a
 * page that was lost, so every page carries one.</p>
 *
 * <h2>One column, not five</h2>
 *
 * <p>The table has an outer box and a rule between every row and no interior
 * verticals, which is exactly what a single-column table draws: a cell strokes
 * all four of its own edges and a one-column table has no interior vertical
 * edge. The five columns are a row inside each cell. Built the other way round
 * — the box from a stroked section wrapped round the table — every continuation
 * page would end in an empty bordered strip, and on page one that strip's
 * bottom edge lands on the page number.</p>
 *
 * <h2>The lockup and the wordmark are the caller's</h2>
 *
 * <p>The design opens with a mark and closes with a wordmark. Both come from
 * {@code InvoiceBrand}: the masthead draws the logo at the design's measured
 * width, or the brand's name when there is no logo, and the closing band always
 * sets the name. The templates artifact carries no mark of its own.</p>
 *
 * <h2>What the document says, and what the preset chooses</h2>
 *
 * <p>A service line names its own mark through
 * {@code InvoiceServiceLines.Line.icon()}, from this preset's own vocabulary,
 * and an unknown token is reported as a data error naming the set. A mark the
 * design gives a colour is knocked out of a tile in it; one it does not is drawn
 * bare. A line with no token is drawn without a mark. The party discs, the bank
 * mark, the information mark and the calendar are the preset's: which glyph
 * opens which block is a property of the design rather than of the document.</p>
 *
 * <p>Figures are written with the locale stated rather than inherited, so the
 * same data renders the same sheet on any machine.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato throughout, and the face was chosen by
 * measurement: its natural widths for eight of the design's strings land within
 * 2–5% of the measured ink, where other grotesques are 8–20% out at the same cap
 * height. The templates artifact carries no fonts — register the family on the
 * session, or the engine substitutes and the measured geometry no longer
 * matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredInvoiceData> template = WorkspaceInvoice.create();
 * template.compose(session, invoice);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class WorkspaceInvoice {

    /** Stable identifier of this preset. */
    public static final String ID = "workspace-invoice";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Workspace Invoice";

    private WorkspaceInvoice() {
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
                page.name("WorkspaceInvoice").padding(DocumentInsets.zero()).spacing(0);
                WorkspaceMasthead.renderHeader(page, data.brand(), data.supplier(),
                        data.masthead());
                WorkspaceMasthead.renderParties(page, data.billTo(), data.shipTo());
                WorkspaceLines.render(page, data.serviceLines(), currency);
                WorkspaceSettlement.render(page, data.payment(), data.totals(), currency);
                WorkspaceClosing.render(page, data.notes(), data.brand(), data.supplier());
            });
        }

        /**
         * The only chrome the sheet has. The design's own closing band cannot be
         * chrome — a header/footer zone carries left, centre and right strings
         * and nothing else, and the band holds a wordmark and a link — so page
         * identity is carried by the page number alone.
         *
         * <p>The height is stated because a default band is taller than the
         * design leaves under its closing block, and a default-height band takes
         * that much off the content area and pushes the band onto a second
         * page. Sized to the bottom margin it sits inside the margin instead of
         * taking content space, and the type is sized to the band rather than
         * the other way round.</p>
         */
        private static void renderPageNumber(DocumentSession document) {
            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    .height((float) PAGE_NUMBER_BAND)
                    .fontSize((float) PAGE_NUMBER_SIZE)
                    .textColor(MUTED)
                    .showSeparator(false)
                    .build());
        }
    }
}
