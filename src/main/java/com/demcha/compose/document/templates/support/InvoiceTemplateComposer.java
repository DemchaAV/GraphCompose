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

    private final BusinessDocumentSceneStyles styles;

    public InvoiceTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public void compose(TemplateComposeTarget target, InvoiceData data) {
        InvoiceData safe = Objects.requireNonNull(data, "data");
        double width = target.pageWidth();

        target.startDocument("InvoiceRoot", ROOT_SPACING);
        target.addParagraph(TemplateSceneSupport.paragraph(
                "InvoiceTitle",
                safe.title(),
                styles.titleStyle(TITLE_SIZE),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "InvoiceMeta",
                TemplateSceneSupport.joinNonBlank(" | ",
                        "Invoice #" + valueOrFallback(safe.invoiceNumber(), "Draft"),
                        "Issued: " + valueOrFallback(safe.issueDate(), "TBD"),
                        "Due: " + valueOrFallback(safe.dueDate(), "TBD"),
                        safe.status().isBlank() ? "" : "Status: " + safe.status(),
                        safe.reference().isBlank() ? "" : "Reference: " + safe.reference()),
                styles.bodyBoldStyle(9.8),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));
        target.addDivider(TemplateSceneSupport.divider(
                "InvoiceRule",
                width,
                1.2,
                styles.accentColor(),
                Margin.top(2)));

        target.addTable(partiesTable(target, safe));
        TemplateSceneSupport.addSectionHeader(target, "InvoiceItems", "LINE ITEMS",
                styles.labelStyle(LABEL_SIZE), Math.min(width, 128), styles.accentColor(), 1.2, Margin.top(4));
        target.addTable(itemsTable(target, safe));

        if (!safe.notes().isEmpty()) {
            TemplateSceneSupport.addSectionHeader(target, "InvoiceNotes", "NOTES",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 120), styles.accentColor(), 1.1, Margin.top(5));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "InvoiceNotesBody",
                    String.join("\n", TemplateSceneSupport.sanitizeLines(safe.notes())),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(3)));
        }

        if (!safe.paymentTerms().isEmpty()) {
            TemplateSceneSupport.addSectionHeader(target, "InvoiceTerms", "PAYMENT TERMS",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 152), styles.accentColor(), 1.1, Margin.top(5));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "InvoicePaymentTerms",
                    TemplateSceneSupport.bulletText(safe.paymentTerms()),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(3)));
        }

        target.addTable(summaryTable(target, safe));

        if (!safe.footerNote().isBlank()) {
            target.addDivider(TemplateSceneSupport.divider(
                    "InvoiceFooterRule",
                    width,
                    1.0,
                    styles.accentColor(),
                    Margin.top(5)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "InvoiceFooter",
                    safe.footerNote(),
                    styles.metaStyle(9.3),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(3)));
        }
        target.finishDocument();
    }

    private TemplateTableSpec partiesTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double columnWidth = (width - 18) / 2.0;
        TableCellStyle style = TableCellStyle.builder()
                .padding(new Padding(6, 8, 6, 8))
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .build();
        return new TemplateTableSpec(
                "InvoiceParties",
                List.of(TableColumnSpec.fixed(columnWidth), TableColumnSpec.fixed(columnWidth)),
                List.of(List.of(
                        TableCellSpec.lines(partyLines("FROM", data.fromParty())),
                        TableCellSpec.lines(partyLines("BILL TO", data.billToParty())))),
                style,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.top(4));
    }

    private TemplateTableSpec itemsTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(new Padding(7, 8, 7, 8))
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
                Margin.top(2));
    }

    private TemplateTableSpec summaryTable(TemplateComposeTarget target, InvoiceData data) {
        double width = Math.min(220, target.pageWidth());
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(new Padding(6, 8, 6, 8))
                .fillColor(styles.softFill())
                .stroke(new Stroke(styles.borderColor(), 1.3))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .build();
        Map<Integer, TableCellStyle> columnStyles = Map.of(
                1, TableCellStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<InvoiceSummaryRow> rows = data.summaryRows().isEmpty()
                ? List.of(new InvoiceSummaryRow("Total", "-", true))
                : data.summaryRows();
        Map<Integer, TableCellStyle> rowStyles = new LinkedHashMap<>();
        List<List<TableCellSpec>> cells = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            InvoiceSummaryRow row = rows.get(index);
            cells.add(List.of(TableCellSpec.text(row.label()), TableCellSpec.text(row.value())));
            if (row.emphasized()) {
                rowStyles.put(index, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.bodyBoldStyle(9.6))
                        .build());
            }
        }

        return new TemplateTableSpec(
                "InvoiceSummaryTable",
                List.of(TableColumnSpec.fixed(width - 74), TableColumnSpec.fixed(74)),
                cells,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                Margin.top(6));
    }

    private List<String> partyLines(String title, InvoiceParty party) {
        InvoiceParty safeParty = party == null ? new InvoiceParty("", List.of(), "", "", "") : party;
        List<String> lines = new ArrayList<>();
        lines.add(title);
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

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String composeItemDescription(InvoiceLineItem item) {
        String description = valueOrFallback(item.description(), "Line item");
        if (item.details().isBlank()) {
            return description;
        }
        return shorten(description + " - " + item.details(), 60);
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
