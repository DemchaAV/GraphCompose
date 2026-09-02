package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.rota.RotaGroup;
import com.demcha.compose.document.templates.data.rota.RotaStaff;
import com.demcha.compose.document.templates.data.rota.StructuredRotaData;
import com.demcha.compose.document.templates.data.rota.StructuredRotaDocumentSpec;

import java.util.Objects;

import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.FOOTER_RULE_WIDTH;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.FOOTER_SIZE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.LABEL_COLUMN_WIDTH;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.MARGIN_BOTTOM;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.NAVY;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAGE;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAPER;

/**
 * Cobalt Rota — a staff rota on one landscape sheet: a navy-ruled grid of who
 * works when, read across a row.
 *
 * <p>The label column carries the venue's mark over the staff names; the day
 * columns carry a heading, whatever else is happening that day, a strip of
 * swatches saying what the colours mean, the covers each service is expecting,
 * and then the people — in bands, each band opening with a navy strip and its
 * own mark. A day's cell holds nothing, one entry, or a stack of them — two is
 * what a split shift comes to and what the row's height is measured for; more
 * than two fits, but stands taller than the row around it.</p>
 *
 * <h2>One table, not a stack of them</h2>
 *
 * <p>Everything from the masthead rule to the last person is rows of a single
 * table. That is what keeps the columns aligned down the sheet: a masthead above
 * the table, or a legend beside it, would be a second structure to keep in step
 * with the first. It is also why the four header rows are declared repeating — a
 * rota longer than a page carries its day headings onto the next one rather than
 * leaving a page of unlabelled columns.</p>
 *
 * <h2>The grid is the document's own shape</h2>
 *
 * <p>The sheet has as many day columns as the document has days, and every
 * person's entries are read by position against that list. Seven is the common
 * case and nothing here requires it. A legend documenting more statuses than
 * there are days is closed off at the sheet's width, and one documenting fewer
 * is padded out, so the table stays square either way.</p>
 *
 * <h2>The wordmark is sized to its column, not to itself</h2>
 *
 * <p>The lockup sits in the label column and is set at a fixed fraction of it,
 * which is what keeps it on one line at the size the design draws it. It does
 * not shrink to fit: a venue whose name runs long wraps instead, so a long name
 * belongs in {@code wordmarkSub} with a short form above it.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Carlito, with the wordmark in Spectral. The templates
 * artifact carries no fonts: both arrive with {@code graph-compose-fonts} on
 * the classpath, or a caller registers families of those names on the session
 * itself. With neither, the engine substitutes and every size is solved for
 * type that is not there.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredRotaDocumentSpec> template = CobaltRota.create();
 * template.compose(session, spec);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class CobaltRota {

    /**
     * Stable template identifier.
     */
    public static final String ID = "rota-cobalt";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Cobalt Rota";

    /**
     * Recommended session margin (in points). The preset sets its own page
     * geometry — landscape A4 and the design's own margins — inside
     * {@code compose}, so callers leave the session margin at zero and let the
     * preset own the page frame.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private CobaltRota() {
    }

    /**
     * Builds the preset.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<StructuredRotaDocumentSpec> create() {
        return new Template();
    }

    private record Template() implements DocumentTemplate<StructuredRotaDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, StructuredRotaDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            StructuredRotaData rota = Objects.requireNonNull(spec, "spec").rota();

            renderChrome(document, rota);

            document.pageFlow(page -> {
                // spacing(0): the sheet is one table and the flow holds nothing
                // else, so a flow spacing would only push it off its margin.
                page.name("CobaltRota").spacing(0);
                CobaltStyles.Grid grid = CobaltStyles.Grid.of(rota.days().size());
                page.addTable(table -> {
                    table.name("Schedule")
                            .columns(columns(grid))
                            .padding(DocumentInsets.zero())
                            .margin(DocumentInsets.zero());
                    CobaltHeader.render(table, rota, grid);
                    CobaltRows.renderLegend(table, rota.legend(), grid);
                    CobaltRows.renderCovers(table, rota, grid);
                    for (RotaGroup group : rota.groups()) {
                        CobaltRows.renderBand(table, group, grid);
                        int row = 0;
                        for (RotaStaff staff : group.staff()) {
                            CobaltRows.renderStaff(table, staff, grid, row++ % 2 == 1);
                        }
                    }
                });
            });
        }

        /**
         * The page and the line that closes it.
         *
         * <p>The footer is chrome rather than a last row of the table, because
         * it repeats on a continuation page and the table's rows do not. Its
         * rule is deliberately lighter than the grid's: the zone fixes the gap
         * between rule and text, so a heavier one crowds the text against
         * it.</p>
         */
        private static void renderChrome(DocumentSession document, StructuredRotaData rota) {
            document.pageSize(PAGE)
                    .margin(PAGE_MARGIN)
                    .pageBackground(PAPER);

            String name = rota.venue().footerName();
            String range = rota.week().rangeLabel();
            String signature = name.isBlank() || range.isBlank()
                    ? name + range
                    : name + "  |  " + range;

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    // The zone's depth is what places the footer: the separator
                    // draws at its top and the text just beneath.
                    .height((float) (MARGIN_BOTTOM * 0.90))
                    .leftText(rota.footer().note())
                    .rightText(signature)
                    .fontSize((float) FOOTER_SIZE)
                    .textColor(NAVY)
                    .showSeparator(true)
                    .separatorColor(NAVY)
                    .separatorThickness((float) FOOTER_RULE_WIDTH)
                    .build());
        }

        /**
         * The label column and one column per day, all fixed.
         *
         * <p>Fixed and not weighted: a weight is a share of what is left, and a
         * table's own weights would have to be restated on every one of the
         * rows, where a width is stated once for the sheet.</p>
         */
        private static DocumentTableColumn[] columns(CobaltStyles.Grid grid) {
            DocumentTableColumn[] columns = new DocumentTableColumn[grid.columnCount()];
            columns[0] = DocumentTableColumn.fixed(LABEL_COLUMN_WIDTH);
            for (int day = 1; day < grid.columnCount(); day++) {
                columns[day] = DocumentTableColumn.fixed(grid.dayWidth());
            }
            return columns;
        }
    }
}
