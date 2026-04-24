package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;
import com.demcha.compose.engine.components.components_builders.TableCellSpec;
import com.demcha.compose.engine.components.components_builders.TableCellStyle;
import com.demcha.compose.engine.components.components_builders.TableColumnSpec;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

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
    private static final double BODY_SIZE = 10.1;
    private static final double LABEL_SIZE = 8.5;

    private final BusinessDocumentSceneStyles styles;
    private final BusinessDocumentLayoutPolicy sceneLayout;
    private final TemplateLayoutPolicy layout;

    public InvoiceTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
        this.sceneLayout = BusinessDocumentLayoutPolicy.standard();
        this.layout = sceneLayout.rhythm();
    }

    public void compose(TemplateComposeTarget target, InvoiceDocumentSpec spec) {
        InvoiceData safe = Objects.requireNonNull(spec, "spec").invoice();
        double width = target.pageWidth();

        target.startDocument("InvoiceRoot", layout.rootSpacing());
        target.addTable(headerTable(target, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "InvoiceRule",
                width,
                sceneLayout.mainDividerThickness(),
                styles.accentColor(),
                layout.subsectionMargin()));

        target.addTable(partiesTable(target, safe));
        target.addModule(new TemplateModuleSpec(
                "InvoiceItems",
                TemplateSceneSupport.paragraph(
                        "InvoiceItemsHeading",
                        "LINE ITEMS",
                        styles.labelStyle(LABEL_SIZE),
                        TextAlign.LEFT,
                        1.0,
                        Padding.zero(),
                        layout.sectionMargin()),
                List.of(
                        TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                                "InvoiceItemsRule",
                                sceneLayout.boundedRuleWidth(width, sceneLayout.invoiceItemsRuleWidth()),
                                sceneLayout.mainDividerThickness(),
                                styles.accentColor(),
                                layout.top(layout.rootSpacing()))),
                        TemplateModuleBlock.table(itemsTable(target, safe)),
                        TemplateModuleBlock.table(notesAndSummaryTable(target, safe)))));

        if (!safe.footerNote().isBlank()) {
            TemplateDividerSpec footerRule = TemplateSceneSupport.divider(
                    "InvoiceFooterRule",
                    width,
                    sceneLayout.subtleDividerThickness(),
                    styles.accentColor(),
                    layout.sectionMargin());
            TemplateParagraphSpec footerNote = TemplateSceneSupport.paragraph(
                    "InvoiceFooter",
                    safe.footerNote(),
                    styles.metaStyle(9.3),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    sceneLayout.moduleBodyGap(layout.subsectionMargin()));

            target.addModule(new TemplateModuleSpec(
                    "InvoiceFooter",
                    null,
                    List.of(
                            TemplateModuleBlock.divider(footerRule),
                            TemplateModuleBlock.paragraph(footerNote))));
        }
        target.finishDocument();
    }

    private TemplateTableSpec headerTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double leftWidth = sceneLayout.leftWidthForReservedRight(
                width,
                220,
                sceneLayout.invoiceHeaderReservedWidth());
        double rightWidth = sceneLayout.rightWidth(width, leftWidth);
        TableCellStyle baseStyle = TableCellStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();

        return new TemplateTableSpec(
                "InvoiceHeader",
                List.of(
                        TableColumnSpec.fixed(leftWidth),
                        TableColumnSpec.fixed(sceneLayout.columnGap()),
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
        double columnWidth = sceneLayout.twoColumnWidth(width);
        TableCellStyle style = TableCellStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
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
                layout.sectionMargin());
    }

    private TemplateTableSpec itemsTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
                .textStyle(styles.bodyStyle(9.3))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
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
                sceneLayout.moduleBodyGap(layout.subsectionMargin()));
    }

    private TemplateTableSpec notesAndSummaryTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double leftWidth = Math.max(220, width - sceneLayout.invoiceSummaryWidth() - sceneLayout.columnGap());
        double rightWidth = sceneLayout.rightWidth(width, leftWidth);
        List<String> noteLines = notesAndTermsLines(data);
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(Padding.zero())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        return new TemplateTableSpec(
                "InvoiceNotesSummary",
                List.of(
                        TableColumnSpec.fixed(leftWidth),
                        TableColumnSpec.fixed(sceneLayout.columnGap()),
                        TableColumnSpec.fixed(rightWidth)),
                List.of(List.of(
                        TableCellSpec.of(noteLines).withStyle(TableCellStyle.builder()
                                .padding(layout.compactCellPadding())
                                .fillColor(Color.WHITE)
                                .stroke(new Stroke(Color.WHITE, 0.0))
                                .textStyle(styles.bodyStyle(BODY_SIZE))
                                .textAnchor(Anchor.topLeft())
                                .build()),
                        TableCellSpec.text(""),
                        TableCellSpec.of(summaryLines(data)).withStyle(TableCellStyle.builder()
                                .padding(layout.contentCellPadding())
                                .fillColor(styles.softFill())
                                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
                                .textStyle(styles.bodyBoldStyle(9.6))
                                .textAnchor(Anchor.topLeft())
                                .build()))),
                defaultStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                sceneLayout.moduleBodyGap(sceneLayout.notesSummaryMargin()));
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
