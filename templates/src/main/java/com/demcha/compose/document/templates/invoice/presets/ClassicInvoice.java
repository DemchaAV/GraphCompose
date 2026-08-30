package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;

import java.util.List;
import java.util.Objects;

/**
 * Classic Invoice — the letterhead-style layered invoice preset.
 *
 * <p>An invoice with a document header band (company name + first
 * address line on the left, a 28pt "INVOICE" title with number and
 * issue date on the right), a "TOTAL DUE" hero strip, a
 * BILL&nbsp;TO / FROM two-column party row, the line-items table, a
 * dedicated Summary table composed <em>after</em> the line items
 * (subtotal / tax / TOTAL, the last row emphasized), and a notes /
 * payment-terms footer. Long invoices paginate naturally: the
 * line-items table flows onto the next page with its header repeated,
 * and the summary + footer follow the table. The preset carries its
 * spacing on the page flow itself (16pt flow spacing, 24pt padding on
 * every edge) and expects a zero session margin — see
 * {@link #RECOMMENDED_MARGIN}.</p>
 *
 * <p>Layout traits that distinguish it from {@link ModernInvoice} and are
 * part of this preset's visual contract: the header band (Modern has
 * none), the party order (BILL&nbsp;TO left, FROM right — Modern renders
 * FROM first), the separate Summary table (Modern folds the summary rows
 * into the line-items table; this one repeats the line-items column
 * pattern and shrink-wraps to its own rows), the fully-rounded hero
 * panel (Modern rounds the right edge only), and the dark table-header
 * ink on the deep-teal fill (Modern uses the light surface colour). The
 * party blocks join address lines, email, and phone only — no tax id
 * line.</p>
 *
 * <p>Ported-render fidelity: the preset reproduces the rendered output
 * of the published standalone {@code invoice-classic} template. Three
 * traits follow from that and are deliberate: the header always reads
 * "INVOICE" — {@link InvoiceData#title()} is not rendered; the
 * "TOTAL DUE" strip carries the invoice metadata (number, dates,
 * status), not an amount; and the table-header ink stays dark on the
 * teal fill. Changing any of them is a redesign of the ported look,
 * not a port fix.</p>
 *
 * <p>The preset reads every shared surface from the
 * {@link BrandTheme#invoiceModern()} tokens (palette, Helvetica type
 * scale, hero panel radius / padding / accent width). The two ink
 * colours below and the geometry constants that have no theme slot
 * stay preset-local.</p>
 */
public final class ClassicInvoice {

    /**
     * Stable template identifier.
     */
    public static final String ID = "invoice-classic";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Classic Invoice";

    /**
     * Recommended session margin (in points). The preset pads the page
     * flow itself (24pt on every edge), so the session margin stays zero;
     * a non-zero margin would double the page frame.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final double TABLE_PADDING = 7.0;

    /**
     * Flow spacing between the header, hero, parties, table, summary,
     * and footer blocks. Intentionally wider than the theme's
     * {@code pageFlowSpacing} — the letterhead look breathes more.
     */
    private static final double PAGE_SPACING = 16.0;

    /**
     * Page-flow padding on every edge — the preset's page frame
     * (replaces the session margin, see {@link #RECOMMENDED_MARGIN}).
     */
    private static final double PAGE_PADDING = 24.0;

    /**
     * Company-name size in the header band — the h2 step of the modern
     * business type ladder (between the theme's 11pt banner and 28pt
     * headline slots, which is why it has no {@code Typography} slot).
     */
    private static final double COMPANY_NAME_SIZE = 17.0;

    /**
     * Deep teal used for the INVOICE title, the company name, and the
     * line-items table header fill (the modern business primary).
     * Same value as {@link ModernInvoice}'s preset-local primary — kept
     * preset-local because adding a {@code Palette} slot would change the
     * record's public constructor; fold both into a shared slot when the
     * palette can grow compatibly.
     */
    private static final DocumentColor PRIMARY = DocumentColor.rgb(20, 60, 75);

    /**
     * Gold accent for the hero strip, the status read-out, and the footer
     * column rules (the modern business accent). Preset-local, same
     * rationale as {@link #PRIMARY}.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);

    private ClassicInvoice() {
    }

    /**
     * Builds the preset with the modern business theme
     * ({@link BrandTheme#invoiceModern()}).
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<InvoiceDocumentSpec> create() {
        return create(BrandTheme.invoiceModern());
    }

    /**
     * Builds the preset with a caller-supplied theme, so callers can
     * vary the invoice flavour (typography scale, palette) without
     * forking this class.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<InvoiceDocumentSpec> create(BrandTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(BrandTheme theme) implements DocumentTemplate<InvoiceDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, InvoiceDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            InvoiceData data = Objects.requireNonNull(spec, "spec").invoice();

            DocumentColor panelFill = theme.palette().banner();   // soft tan
            DocumentColor surface = theme.palette().mainFill();   // cream
            DocumentColor rule = theme.palette().rule();

            DocumentTextStyle titleStyle = DocumentTextStyle.builder()
                    .fontName(theme.typography().headlineFont())
                    .size(theme.typography().sizeHeadline())
                    .decoration(DocumentTextDecoration.BOLD)
                    .color(PRIMARY)
                    .build();
            DocumentTextStyle companyStyle = DocumentTextStyle.builder()
                    .fontName(theme.typography().headlineFont())
                    .size(COMPANY_NAME_SIZE)
                    .decoration(DocumentTextDecoration.BOLD)
                    .color(PRIMARY)
                    .build();
            DocumentTextStyle labelStyle = theme.bodyBoldStyle();
            DocumentTextStyle bodyStyle = theme.bodyStyle();
            DocumentTextStyle captionStyle = DocumentTextStyle.builder()
                    .fontName(theme.typography().bodyFont())
                    .size(theme.typography().sizeEntrySubtitle())
                    .color(theme.palette().muted())
                    .build();

            // Line-item cells intentionally carry NO textStyle — they inherit
            // the DSL default table-cell text (same guard as ModernInvoice).
            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING))
                    .build();
            // The header keeps the dark body-bold ink on the deep-teal fill —
            // the ported template's approved render (Modern uses the light
            // surface ink instead). A higher-contrast header is a deliberate
            // redesign of the ported look, not a port fix.
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(PRIMARY)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(labelStyle)
                    .build();
            DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                    .fillColor(panelFill)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(labelStyle)
                    .build();

            PageFlowBuilder flow = document.dsl().pageFlow()
                    .name("Invoice")
                    .spacing(PAGE_SPACING)
                    .padding(DocumentInsets.of(PAGE_PADDING))
                    .addRow("Header", row -> renderHeader(row, data,
                            companyStyle, titleStyle, captionStyle))
                    .addSection("Hero", section -> renderHero(section, data,
                            panelFill, labelStyle))
                    .addRow("Parties", row -> renderParties(row, data,
                            labelStyle, bodyStyle))
                    .addTable(table -> renderLineItems(table, data,
                            bordered, headerStyle, panelFill, surface));
            // The engine rejects a zero-row table, so an invoice without
            // summary rows renders without the Summary section.
            if (!data.summaryRows().isEmpty()) {
                flow.addSection("Summary", section -> renderSummary(section, data,
                        bordered, totalStyle));
            }
            flow.addSection("Footer", section -> renderFooter(section, data,
                            labelStyle, captionStyle))
                    .build();
        }

        private void renderHeader(RowBuilder row,
                                  InvoiceData data,
                                  DocumentTextStyle companyStyle,
                                  DocumentTextStyle titleStyle,
                                  DocumentTextStyle captionStyle) {
            row.spacing(18);
            row.weights(1, 1);
            row.addSection("HeaderLeft", section -> section
                    .spacing(2)
                    .addParagraph(p -> p
                            .text(data.fromParty().name())
                            .textStyle(companyStyle)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text(firstAddressLine(data.fromParty()))
                            .textStyle(captionStyle)
                            .margin(DocumentInsets.zero())));
            row.addSection("HeaderRight", section -> section
                    .spacing(2)
                    .addParagraph(p -> p
                            .text("INVOICE")
                            .textStyle(titleStyle)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text(safe(data.invoiceNumber()))
                            .textStyle(captionStyle)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text("Issued " + safe(data.issueDate()))
                            .textStyle(captionStyle)
                            .margin(DocumentInsets.zero())));
        }

        private void renderHero(SectionBuilder section,
                                InvoiceData data,
                                DocumentColor panelFill,
                                DocumentTextStyle labelStyle) {
            // The strip is a metadata read-out — the ported template renders
            // no amount after the TOTAL DUE label.
            section.softPanel(panelFill,
                            theme.spacing().bannerCornerRadius(),
                            theme.spacing().bannerInnerPadding())
                    .accentLeft(ACCENT, theme.spacing().accentRuleWidth())
                    .spacing(6)
                    .addParagraph(p -> p
                            .text("TOTAL DUE")
                            .textStyle(labelStyle)
                            .margin(DocumentInsets.zero()))
                    .addRich(rich -> rich
                            .plain("Invoice ").bold(safe(data.invoiceNumber()))
                            .plain("    Issued ").bold(safe(data.issueDate()))
                            .plain("    Due ").bold(safe(data.dueDate()))
                            .plain("    Status ").accent(safeStatus(data.status()), ACCENT));
        }

        private void renderParties(RowBuilder row,
                                   InvoiceData data,
                                   DocumentTextStyle labelStyle,
                                   DocumentTextStyle bodyStyle) {
            row.spacing(18);
            row.weights(1, 1);
            row.addSection("BillTo", section -> renderContactBlock(
                    section, data.billToParty(), "BILL TO", labelStyle, bodyStyle));
            row.addSection("From", section -> renderContactBlock(
                    section, data.fromParty(), "FROM", labelStyle, bodyStyle));
        }

        private static void renderLineItems(TableBuilder table,
                                            InvoiceData data,
                                            DocumentTableStyle bordered,
                                            DocumentTableStyle headerStyle,
                                            DocumentColor zebraOdd,
                                            DocumentColor zebraEven) {
            table.name("LineItems")
                    .columns(
                            DocumentTableColumn.auto(),
                            DocumentTableColumn.fixed(54),
                            DocumentTableColumn.fixed(96),
                            DocumentTableColumn.fixed(96))
                    .defaultCellStyle(bordered)
                    .headerRow("Description", "Qty", "Unit", "Amount")
                    .headerStyle(headerStyle)
                    .repeatHeader()
                    .zebra(zebraOdd, zebraEven);
            // Only item.description() renders in the cell — the optional
            // details string would drive the auto-sized description column
            // past the inner page width (same guard as ModernInvoice).
            for (InvoiceLineItem item : data.lineItems()) {
                table.row(item.description(), item.quantity(),
                        item.unitPrice(), item.amount());
            }
        }

        private static void renderSummary(SectionBuilder section,
                                          InvoiceData data,
                                          DocumentTableStyle bordered,
                                          DocumentTableStyle totalStyle) {
            // The summary lives in its own table that repeats the LineItems
            // column pattern (auto / 54 / 96 / 96) — the ported template's
            // shape. The table shrink-wraps to its own rows (the empty auto
            // column collapses), so it renders narrower than the line items
            // above. The LAST row is the grand total and renders via
            // totalRow for the emphasized style.
            section.addTable(summary -> {
                summary.name("SummaryTable")
                        .columns(
                                DocumentTableColumn.auto(),
                                DocumentTableColumn.fixed(54),
                                DocumentTableColumn.fixed(96),
                                DocumentTableColumn.fixed(96))
                        .defaultCellStyle(bordered);
                List<InvoiceSummaryRow> summaries = data.summaryRows();
                for (int i = 0; i < summaries.size(); i++) {
                    InvoiceSummaryRow row = summaries.get(i);
                    if (i == summaries.size() - 1) {
                        summary.totalRow(totalStyle, "", "", row.label(), row.value());
                    } else {
                        summary.row("", "", row.label(), row.value());
                    }
                }
            });
        }

        private void renderFooter(SectionBuilder section,
                                  InvoiceData data,
                                  DocumentTextStyle labelStyle,
                                  DocumentTextStyle captionStyle) {
            section.spacing(8)
                    .addRow("FooterRow", row -> row
                            .spacing(18)
                            .weights(1, 1)
                            .addSection("InvoiceNotes", col -> col
                                    .accentLeft(ACCENT, 3)
                                    .padding(0, 0, 0, 8)
                                    .spacing(3)
                                    .addParagraph(p -> p
                                            .text("Notes")
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addList(list -> list.items(data.notes())))
                            .addSection("InvoicePaymentTerms", col -> col
                                    .accentLeft(ACCENT, 3)
                                    .padding(0, 0, 0, 8)
                                    .spacing(3)
                                    .addParagraph(p -> p
                                            .text("Payment terms")
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addList(list -> list.items(data.paymentTerms()))));
            if (!data.footerNote().isBlank()) {
                section.addParagraph(p -> p
                        .text(data.footerNote())
                        .textStyle(captionStyle)
                        .margin(new DocumentInsets(14, 0, 0, 0)));
            }
        }

        private static void renderContactBlock(SectionBuilder section,
                                               InvoiceParty party,
                                               String label,
                                               DocumentTextStyle labelStyle,
                                               DocumentTextStyle bodyStyle) {
            section.spacing(2)
                    .addParagraph(p -> p
                            .text(label)
                            .textStyle(labelStyle)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text(party.name())
                            .textStyle(labelStyle)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text(joinAddress(party))
                            .textStyle(bodyStyle)
                            .lineSpacing(1.3)
                            .margin(DocumentInsets.zero()));
        }
    }

    private static String firstAddressLine(InvoiceParty party) {
        for (String line : party.addressLines()) {
            if (line != null && !line.isBlank()) {
                return line;
            }
        }
        return "";
    }

    private static String joinAddress(InvoiceParty party) {
        StringBuilder builder = new StringBuilder();
        for (String line : party.addressLines()) {
            if (line == null || line.isBlank()) {
                continue;
            }
            append(builder, line);
        }
        if (!party.email().isBlank()) {
            append(builder, party.email());
        }
        if (!party.phone().isBlank()) {
            append(builder, party.phone());
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private static String safeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        return status;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
