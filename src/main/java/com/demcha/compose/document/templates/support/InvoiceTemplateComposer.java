package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.InvoiceData;
import com.demcha.compose.document.templates.data.InvoiceLineItem;
import com.demcha.compose.document.templates.data.InvoiceParty;
import com.demcha.compose.document.templates.data.InvoiceSummaryRow;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared scene composer for the invoice template.
 */
public final class InvoiceTemplateComposer {
    private static final double ROOT_SPACING = 10;
    private static final double BODY_SIZE = 10.1;
    private static final double LABEL_SIZE = 8.5;
    private static final double TITLE_SIZE = 26;
    private static final double COLUMN_GAP = 18;
    private static final double SUMMARY_WIDTH = 206;
    private static final double HEADER_RULE_GAP = 4;
    private static final double SECTION_GAP = 6;
    private static final double SUBSECTION_GAP = 4;
    private static final double SUMMARY_GAP = 8;
    private static final double FOOTER_GAP = 6;
    private static final double FOOTER_NOTE_GAP = 4;
    private static final Padding HEADER_CELL_PADDING = new Padding(2, 0, 2, 0);
    private static final Padding CONTENT_CELL_PADDING = new Padding(7, 8, 7, 8);
    private static final Padding NOTES_CELL_PADDING = new Padding(2, 0, 2, 0);

    private final BusinessDocumentSceneStyles styles;

    public InvoiceTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public void compose(TemplateComposeTarget target, InvoiceData data) {
        InvoiceData safe = Objects.requireNonNull(data, "data");
        double width = target.pageWidth();

        target.startDocument("InvoiceRoot", ROOT_SPACING);
        target.addTable(headerTable(target, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "InvoiceRule",
                width,
                1.2,
                styles.accentColor(),
                Margin.top(HEADER_RULE_GAP)));

        target.addTable(partiesTable(target, safe));
        TemplateSceneSupport.addSectionHeader(target, "InvoiceItems", "LINE ITEMS",
                styles.labelStyle(LABEL_SIZE), Math.min(width, 128), styles.accentColor(), 1.2, Margin.top(SECTION_GAP));
        target.addTable(itemsTable(target, safe));
        target.addTable(notesAndSummaryTable(target, safe));

        if (!safe.footerNote().isBlank()) {
            target.addDivider(TemplateSceneSupport.divider(
                "InvoiceFooterRule",
                width,
                    1.0,
                    styles.accentColor(),
                    Margin.top(FOOTER_GAP)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "InvoiceFooter",
                    safe.footerNote(),
                    styles.metaStyle(9.3),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(FOOTER_NOTE_GAP)));
        }
        target.finishDocument();
    }

    private TemplateTableSpec headerTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double leftWidth = Math.max(220, width - 188);
        double rightWidth = width - leftWidth - COLUMN_GAP;
        TableCellStyle baseStyle = TableCellStyle.builder()
                .padding(HEADER_CELL_PADDING)
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .build();

        return new TemplateTableSpec(
                "InvoiceHeader",
                List.of(
                        TableColumnSpec.fixed(leftWidth),
                        TableColumnSpec.fixed(COLUMN_GAP),
                        TableColumnSpec.fixed(rightWidth)),
                List.of(List.of(
                        TableCellSpec.of(headerLeftLines(data)).withStyle(TableCellStyle.builder()
                                .textStyle(styles.bodyStyle(BODY_SIZE))
                                .textAnchor(Anchor.topLeft())
                                .build()),
                        TableCellSpec.text(""),
                        TableCellSpec.of(headerRightLines(data)).withStyle(TableCellStyle.builder()
                                .textStyle(styles.bodyBoldStyle(9.3))
                                .textAnchor(Anchor.topRight())
                                .build()))),
                baseStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.zero());
    }

    private TemplateTableSpec partiesTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double columnWidth = (width - 18) / 2.0;
        TableCellStyle style = TableCellStyle.builder()
                .padding(CONTENT_CELL_PADDING)
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .build();
        return new TemplateTableSpec(
                "InvoiceParties",
                List.of(TableColumnSpec.fixed(columnWidth), TableColumnSpec.fixed(columnWidth)),
                List.of(List.of(
                        TableCellSpec.of(partyLines("FROM", data.fromParty())),
                        TableCellSpec.of(partyLines("BILL TO", data.billToParty())))),
                style,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.top(SECTION_GAP));
    }

    private TemplateTableSpec itemsTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(CONTENT_CELL_PADDING)
                .fillColor(Color.WHITE)
                .stroke(new Stroke(styles.borderColor(), 1.3))
                .textStyle(styles.bodyStyle(9.3))
                .textAnchor(Anchor.centerLeft())
                .build();
        Map<Integer, TableCellStyle> rowStyles = new LinkedHashMap<>();
        rowStyles.put(0, TableCellStyle.builder()
                .fillColor(styles.strongFill())
                .textStyle(styles.headingStyle(9.0))
                .build());

        Map<Integer, TableCellStyle> columnStyles = new LinkedHashMap<>();
        columnStyles.put(1, TableCellStyle.builder().textAnchor(Anchor.center()).build());
        columnStyles.put(2, TableCellStyle.builder().textAnchor(Anchor.centerRight()).build());
        columnStyles.put(3, TableCellStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<List<TableCellSpec>> rows = new ArrayList<>();
        rows.add(List.of(
                TableCellSpec.text("Description"),
                TableCellSpec.text("Qty"),
                TableCellSpec.text("Unit Price"),
                TableCellSpec.text("Amount")));
        List<InvoiceLineItem> items = data.lineItems().isEmpty()
                ? List.of(new InvoiceLineItem("No line items provided", "", "-", "-", "-"))
                : data.lineItems();
        for (InvoiceLineItem item : items) {
            rows.add(List.of(
                    TableCellSpec.text(composeItemDescription(item)),
                    TableCellSpec.text(valueOrFallback(item.quantity(), "-")),
                    TableCellSpec.text(valueOrFallback(item.unitPrice(), "-")),
                    TableCellSpec.text(valueOrFallback(item.amount(), "-"))));
        }

        return new TemplateTableSpec(
                "InvoiceItemsTable",
                List.of(
                        TableColumnSpec.fixed(Math.max(220, width - 250)),
                        TableColumnSpec.fixed(68),
                        TableColumnSpec.fixed(88),
                        TableColumnSpec.fixed(94)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                Margin.top(SUBSECTION_GAP));
    }

    private TemplateTableSpec notesAndSummaryTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double leftWidth = Math.max(220, width - SUMMARY_WIDTH - COLUMN_GAP);
        double rightWidth = width - leftWidth - COLUMN_GAP;
        List<String> noteLines = notesAndTermsLines(data);
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(Padding.zero())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .build();
        return new TemplateTableSpec(
                "InvoiceNotesSummary",
                List.of(
                        TableColumnSpec.fixed(leftWidth),
                        TableColumnSpec.fixed(COLUMN_GAP),
                        TableColumnSpec.fixed(rightWidth)),
                List.of(List.of(
                        TableCellSpec.of(noteLines).withStyle(TableCellStyle.builder()
                                .padding(NOTES_CELL_PADDING)
                                .fillColor(Color.WHITE)
                                .stroke(new Stroke(Color.WHITE, 0.0))
                                .textStyle(styles.bodyStyle(BODY_SIZE))
                                .textAnchor(Anchor.topLeft())
                                .build()),
                        TableCellSpec.text(""),
                        TableCellSpec.of(summaryLines(data)).withStyle(TableCellStyle.builder()
                                .padding(CONTENT_CELL_PADDING)
                                .fillColor(styles.softFill())
                                .stroke(new Stroke(styles.borderColor(), 1.3))
                                .textStyle(styles.bodyBoldStyle(9.6))
                                .textAnchor(Anchor.topLeft())
                                .build()))),
                defaultStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.top(SUMMARY_GAP));
    }

    private List<String> headerLeftLines(InvoiceData data) {
        List<String> lines = new ArrayList<>();
        lines.add(valueOrFallback(data.title(), "Invoice"));
        lines.add("Invoice #" + valueOrFallback(data.invoiceNumber(), "Draft"));
        if (!data.status().isBlank()) {
            lines.add("Status: " + data.status());
        }
        if (!data.reference().isBlank()) {
            lines.add("Reference: " + data.reference());
        }
        return lines;
    }

    private List<String> headerRightLines(InvoiceData data) {
        List<String> lines = new ArrayList<>();
        lines.add("Issued: " + valueOrFallback(data.issueDate(), "TBD"));
        lines.add("Due: " + valueOrFallback(data.dueDate(), "TBD"));
        if (!data.reference().isBlank()) {
            lines.add("Reference: " + data.reference());
        }
        if (!data.status().isBlank()) {
            lines.add("Status: " + data.status());
        }
        return lines;
    }

    private List<String> partyLines(String title, InvoiceParty party) {
        InvoiceParty safeParty = party == null ? new InvoiceParty("", List.of(), "", "", "") : party;
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("");
        lines.add(valueOrFallback(safeParty.name(), "Not provided"));
        lines.addAll(safeParty.addressLines());
        if (!safeParty.email().isBlank()) {
            lines.add("Email: " + safeParty.email());
        }
        if (!safeParty.phone().isBlank()) {
            lines.add("Phone: " + safeParty.phone());
        }
        if (!safeParty.taxId().isBlank()) {
            lines.add("Tax ID: " + safeParty.taxId());
        }
        return lines;
    }

    private List<String> notesAndTermsLines(InvoiceData data) {
        List<String> lines = new ArrayList<>();
        if (!data.notes().isEmpty()) {
            lines.add("NOTES");
            lines.add("");
            lines.addAll(wrapLines(TemplateSceneSupport.sanitizeLines(data.notes()), 46));
        }
        if (!data.paymentTerms().isEmpty()) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            lines.add("PAYMENT TERMS");
            lines.add("");
            lines.addAll(wrapLines(TemplateSceneSupport.sanitizeLines(data.paymentTerms()), 46));
        }
        if (lines.isEmpty()) {
            return List.of("");
        }
        return List.copyOf(lines);
    }

    private List<String> summaryLines(InvoiceData data) {
        List<InvoiceSummaryRow> rows = data.summaryRows().isEmpty()
                ? List.of(new InvoiceSummaryRow("Total", "-", true))
                : data.summaryRows();
        List<String> lines = new ArrayList<>(rows.size());
        for (InvoiceSummaryRow row : rows) {
            lines.add(row.label() + " " + row.value());
        }
        return List.copyOf(lines);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String composeItemDescription(InvoiceLineItem item) {
        String description = valueOrFallback(item.description(), "Line item");
        if (item.details().isBlank()) {
            return description;
        }
        return shorten(description + " - " + item.details(), 44);
    }

    private static List<String> wrapLines(List<String> values, int maxLength) {
        List<String> wrapped = new ArrayList<>();
        for (String value : values) {
            wrapped.addAll(wrapLine(value, maxLength));
        }
        return List.copyOf(wrapped);
    }

    private static List<String> wrapLine(String value, int maxLength) {
        String normalized = valueOrFallback(value, "").trim();
        if (normalized.isBlank() || normalized.length() <= maxLength) {
            return List.of(normalized);
        }

        List<String> parts = new ArrayList<>();
        String remaining = normalized;
        while (remaining.length() > maxLength) {
            int splitAt = remaining.lastIndexOf(' ', maxLength);
            if (splitAt <= 0) {
                splitAt = maxLength;
            }
            parts.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (!remaining.isBlank()) {
            parts.add(remaining);
        }
        return List.copyOf(parts);
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
