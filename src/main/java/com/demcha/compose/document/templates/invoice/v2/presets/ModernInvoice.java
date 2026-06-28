package com.demcha.compose.document.templates.invoice.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
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
 * Modern Invoice — the first layered invoice preset.
 *
 * <p>It composes an invoice on a
 * {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 * plus the canonical DSL, mirroring the {@code cv.v2} preset shape: a
 * {@code create(BrandTheme)} factory returns a thin
 * {@link DocumentTemplate} whose {@code compose} sequences a hero panel,
 * the FROM / BILL&nbsp;TO parties, the line-items table, and a notes /
 * payment-terms footer. The visual intent is ported from the cinematic
 * {@code builtins.InvoiceTemplateV2}; this preset reads every colour,
 * font, size, and spacing value from the theme instead of a
 * {@code BusinessTheme}.</p>
 *
 * <p><strong>Why the parties render inline rather than through
 * {@code core.identity.PartyIdentity}:</strong> an invoice carries two
 * parties (sender + bill-to) shown as side-by-side blocks, and an
 * {@link InvoiceParty}'s email / phone are optional, whereas the shared
 * {@code Contact} / {@code Masthead} widgets model a single mandatory
 * contact triple for a one-person CV masthead. The two-party inline
 * layout is the invoice idiom, so the preset composes the party blocks
 * directly.</p>
 */
public final class ModernInvoice {

    /**
     * Stable template identifier.
     */
    public static final String ID = "invoice-modern";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Invoice";

    /**
     * Recommended page margin (in points).
     */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final double TABLE_PADDING = 7.0;

    /**
     * Strong accent blue for the hero accent strip and the status read
     * out. Preset-local because no other v2 preset shares it today; if a
     * second invoice preset reaches for it, promote it to a
     * {@link com.demcha.compose.document.templates.core.theme.Palette}
     * slot.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(41, 128, 185);

    private ModernInvoice() {
    }

    /**
     * Builds the preset with the Modern Invoice theme
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

            DocumentColor panelFill = theme.palette().banner();
            DocumentColor rule = theme.palette().rule();
            DocumentColor surface = theme.palette().mainFill();

            DocumentTextStyle titleStyle = theme.headlineStyle();
            DocumentTextStyle labelStyle = theme.bannerStyle();
            DocumentTextStyle bodyStyle = theme.bodyStyle();

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING))
                    .textStyle(bodyStyle)
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(theme.palette().ink())
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(theme.typography().bodyFont())
                            .decoration(DocumentTextDecoration.BOLD)
                            .size(theme.typography().sizeBanner())
                            .color(surface)
                            .build())
                    .build();
            DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                    .fillColor(panelFill)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(theme.bodyBoldStyle())
                    .build();

            document.dsl().pageFlow()
                    .name("InvoiceV2ModernRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("InvoiceHero", section -> section
                            .softPanel(panelFill,
                                    DocumentCornerRadius.right(theme.spacing().bannerCornerRadius()),
                                    theme.spacing().bannerInnerPadding())
                            .accentLeft(ACCENT, theme.spacing().accentRuleWidth())
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text(data.title().isBlank() ? "Invoice" : data.title())
                                    .textStyle(titleStyle)
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Invoice ").bold(data.invoiceNumber())
                                    .plain("    Issued ").bold(data.issueDate())
                                    .plain("    Due ").bold(data.dueDate())
                                    .plain("    Status ").accent(safeStatus(data.status()), ACCENT)))
                    .addRow("InvoiceParties", row -> row
                            .spacing(18)
                            .weights(1, 1)
                            .addSection("InvoiceFromParty", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("FROM")
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.fromParty().name())
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.fromParty()))
                                            .textStyle(bodyStyle)
                                            .lineSpacing(1.3)
                                            .margin(DocumentInsets.zero())))
                            .addSection("InvoiceBillToParty", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("BILL TO")
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.billToParty().name())
                                            .textStyle(labelStyle)
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.billToParty()))
                                            .textStyle(bodyStyle)
                                            .lineSpacing(1.3)
                                            .margin(DocumentInsets.zero()))))
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("InvoiceLineItems")
                                .columns(
                                        DocumentTableColumn.auto(),
                                        DocumentTableColumn.fixed(54),
                                        DocumentTableColumn.fixed(96),
                                        DocumentTableColumn.fixed(96))
                                .defaultCellStyle(bordered)
                                .headerRow("Description", "Qty", "Unit", "Amount")
                                .headerStyle(headerStyle)
                                .repeatHeader()
                                .zebra(panelFill, surface);
                        // Render only item.description() in the cell. The optional
                        // details string can be a long sentence; including it would
                        // force the auto-sized description column to measure against
                        // that width and overflow the inner page on A4 — the cinematic
                        // builtin guards the same way. Do not add item.details() here.
                        for (InvoiceLineItem item : data.lineItems()) {
                            configured.row(item.description(), item.quantity(),
                                    item.unitPrice(), item.amount());
                        }
                        // Convention (shared with the cinematic builtin): the LAST
                        // summary row is the grand total — it renders via totalRow
                        // so it picks up the bold total style; earlier rows render
                        // as plain body rows.
                        List<InvoiceSummaryRow> summaries = data.summaryRows();
                        for (int i = 0; i < summaries.size(); i++) {
                            InvoiceSummaryRow summary = summaries.get(i);
                            if (i == summaries.size() - 1) {
                                configured.totalRow(totalStyle, "", "", summary.label(), summary.value());
                            } else {
                                configured.row("", "", summary.label(), summary.value());
                            }
                        }
                    })
                    .addRow("InvoiceFooterRow", row -> row
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
                                    .addList(list -> list.items(data.paymentTerms()))))
                    .build();

            if (!data.footerNote().isBlank()) {
                document.dsl().pageFlow()
                        .name("InvoiceV2ModernFooter")
                        .addParagraph(p -> p
                                .text(data.footerNote())
                                .textStyle(theme.entrySubtitleStyle())
                                .margin(new DocumentInsets(14, 0, 0, 0)))
                        .build();
            }
        }
    }

    private static String safeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        return status;
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
        if (!party.taxId().isBlank()) {
            append(builder, "Tax ID " + party.taxId());
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
