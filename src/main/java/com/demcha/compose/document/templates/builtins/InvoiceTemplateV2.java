package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.api.InvoiceTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.List;
import java.util.Objects;

/**
 * Phase E.1 — cinematic invoice template that composes against the
 * canonical DSL using a {@link BusinessTheme} for every visual choice.
 *
 * <p>The template demonstrates the v1.4 / v1.5 cinematic primitives
 * stacked on top of one another:</p>
 *
 * <ul>
 *   <li>{@code softPanel} hero block carrying invoice number, dates,
 *       and the status read out as inline rich text.</li>
 *   <li>Themed table with header style on the first row, zebra
 *       alternation on the body, and a totals row anchored at the
 *       bottom.</li>
 *   <li>{@code repeatHeader()} so the totals header re-emits on every
 *       continuation page when the invoice paginates.</li>
 *   <li>{@code accentLeft} accent strip on the notes / payment-terms
 *       columns.</li>
 * </ul>
 *
 * <p>Every visual style is derived from the {@link BusinessTheme}
 * passed to the constructor — palette, text scale, stroke colour. The
 * default constructor picks {@link BusinessTheme#modern()}; pass any
 * other theme (or a custom one) to render the same
 * {@link InvoiceDocumentSpec} in a different look without rewriting
 * the composition.</p>
 *
 * @author Artem Demchyshyn
 */
public final class InvoiceTemplateV2 implements InvoiceTemplate {
    private static final double TABLE_PADDING = 7.0;

    private final BusinessTheme theme;

    /**
     * Creates the template with {@link BusinessTheme#modern()}.
     */
    public InvoiceTemplateV2() {
        this(BusinessTheme.modern());
    }

    /**
     * Creates the template with an explicit theme.
     *
     * @param theme the visual theme used for every section, table, and
     *              accent
     */
    public InvoiceTemplateV2(BusinessTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public String getTemplateId() {
        return "invoice-v2";
    }

    @Override
    public String getTemplateName() {
        return "Invoice V2 (cinematic)";
    }

    @Override
    public String getDescription() {
        return "Theme-driven invoice template using soft panels, accent strips, "
                + "rich text, zebra rows, and a repeating totals header.";
    }

    @Override
    public void compose(DocumentSession document, InvoiceDocumentSpec spec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), spec);
        try {
            InvoiceData data = Objects.requireNonNull(spec, "spec").invoice();
            DocumentColor surfaceMuted = theme.palette().surfaceMuted();
            DocumentColor accent = theme.palette().accent();
            DocumentColor rule = theme.palette().rule();

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(theme.palette().primary())
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(theme.text().label().fontName())
                            .decoration(theme.text().label().decoration())
                            .size(theme.text().label().size())
                            .color(theme.palette().surface())
                            .build())
                    .build();
            DocumentTableStyle totalStyle = DocumentTableStyle.builder()
                    .fillColor(surfaceMuted)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(theme.text().label())
                    .build();

            document.dsl().pageFlow()
                    .name("InvoiceCinematicRoot")
                    .spacing(14)
                    .addSection("InvoiceHero", section -> section
                            .softPanel(surfaceMuted, 10, 14)
                            .accentLeft(accent, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text(data.title().isBlank() ? "Invoice" : data.title())
                                    .textStyle(theme.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Invoice ").bold(data.invoiceNumber())
                                    .plain("    Issued ").bold(data.issueDate())
                                    .plain("    Due ").bold(data.dueDate())
                                    .plain("    Status ").accent(safeStatus(data.status()), accent)))
                    .addRow("InvoiceParties", row -> row
                            .spacing(18)
                            .weights(1, 1)
                            .addSection("InvoiceFromParty", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("FROM")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.fromParty().name())
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.fromParty()))
                                            .textStyle(theme.text().body())
                                            .margin(DocumentInsets.zero())))
                            .addSection("InvoiceBillToParty", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("BILL TO")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.billToParty().name())
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.billToParty()))
                                            .textStyle(theme.text().body())
                                            .margin(DocumentInsets.zero()))))
                    .addTable(table -> {
                        TableBuilder configured = table
                                .name("InvoiceLineItems")
                                // Description gets the leftover width via auto;
                                // numeric columns are fixed so a long line-item
                                // description never blows the table past the
                                // page's inner width.
                                .columns(
                                        DocumentTableColumn.auto(),
                                        DocumentTableColumn.fixed(54),
                                        DocumentTableColumn.fixed(96),
                                        DocumentTableColumn.fixed(96))
                                .defaultCellStyle(bordered)
                                .headerRow("Description", "Qty", "Unit", "Amount")
                                .headerStyle(headerStyle)
                                .repeatHeader()
                                .zebra(surfaceMuted, theme.palette().surface());
                        for (InvoiceLineItem item : data.lineItems()) {
                            configured.row(
                                    composeDescription(item),
                                    item.quantity(),
                                    item.unitPrice(),
                                    item.amount());
                        }
                        // The InvoiceData spec convention is that the LAST summary
                        // row is the totals line — InvoiceData itself does not
                        // tag totals separately. Earlier rows render as plain
                        // body rows; the last one renders via TableBuilder.totalRow
                        // so it picks up the totals style + bold + accent fill.
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
                                    .accentLeft(accent, 3)
                                    .padding(0, 0, 0, 8)
                                    .spacing(3)
                                    .addParagraph(p -> p
                                            .text("Notes")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addList(list -> list.items(data.notes())))
                            .addSection("InvoicePaymentTerms", col -> col
                                    .accentLeft(accent, 3)
                                    .padding(0, 0, 0, 8)
                                    .spacing(3)
                                    .addParagraph(p -> p
                                            .text("Payment terms")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addList(list -> list.items(data.paymentTerms()))))
                    .build();

            if (!data.footerNote().isBlank()) {
                document.dsl().pageFlow()
                        .name("InvoiceCinematicFooter")
                        .addParagraph(p -> p
                                .text(data.footerNote())
                                .textStyle(theme.text().caption())
                                // Push the thank-you line down off the
                                // notes / payment-terms block above so it
                                // does not visually merge with them.
                                .margin(new DocumentInsets(14, 0, 0, 0)))
                        .build();
            }

            TemplateLifecycleLog.success(getTemplateId(), spec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), spec, startNanos, ex);
            throw ex;
        }
    }

    private static String safeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        return status;
    }

    private static String composeDescription(InvoiceLineItem item) {
        // Render only the headline description in the cell. The optional
        // {@code details} string can be a long marketing-style sentence;
        // including it would force the auto-sized description column to
        // measure against that natural width and overflow the inner page
        // for typical A4 invoices. Templates that need the details
        // alongside should compose them in a separate notes column or
        // section.
        return item.description();
    }

    private static String joinAddress(InvoiceParty party) {
        StringBuilder builder = new StringBuilder();
        for (String line : party.addressLines()) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        if (!party.email().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(party.email());
        }
        if (!party.phone().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(party.phone());
        }
        if (!party.taxId().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("Tax ID ").append(party.taxId());
        }
        return builder.toString();
    }

}
