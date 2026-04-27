package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
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

    /**
     * Creates an invoice scene composer with the supplied business document styles.
     *
     * @param styles shared invoice visual styles
     */
    public InvoiceTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
        this.sceneLayout = BusinessDocumentLayoutPolicy.standard();
        this.layout = sceneLayout.rhythm();
    }

    /**
     * Emits invoice header, parties, line items, notes, totals, and footer modules.
     *
     * @param target canonical template compose target
     * @param spec invoice document spec
     */
    public void compose(TemplateComposeTarget target, InvoiceDocumentSpec spec) {
        InvoiceData safe = Objects.requireNonNull(spec, "spec").invoice();
        double width = target.pageWidth();

        target.startDocument("InvoiceRoot", layout.rootSpacing());
        target.addTable(headerTable(target, safe));
        target.addTable(headerSubtitleTable(target, safe, headerRightOffset(width)));
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
                        TemplateModuleBlock.table(summaryTable(target, safe)),
                        TemplateModuleBlock.table(notesTable(target, safe)))));

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
        // README-style header: a 2-row chip grid on the left (value + small
        // muted label per chip) plus a large "Invoice" title hugging the
        // right edge. The title cell visually anchors the top-right corner
        // while the chip rows fill the leading columns.
        double chipValueA = 86.0;
        double chipLabelA = 56.0;
        double chipValueB = Math.max(140.0, (width - 200.0) * 0.40);
        double chipLabelB = 64.0;
        double titleWidth = Math.max(140.0, width - chipValueA - chipLabelA - chipValueB - chipLabelB);

        TableCellLayoutStyle valueStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyBoldStyle(10.2))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle labelStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.labelStyle(8.4))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle titleStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.titleStyle(28.0))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle emptyStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();

        String docTitle = valueOrFallback(data.title(), "Invoice");
        String issued = valueOrFallback(data.issueDate(), "TBD");
        String due = valueOrFallback(data.dueDate(), "TBD");
        String reference = valueOrFallback(data.reference(), "—");
        String status = valueOrFallback(data.status(), "—");

        List<List<TableCellContent>> rows = List.of(
                List.of(
                        TableCellContent.text(issued).withStyle(valueStyle),
                        TableCellContent.text("Issued").withStyle(labelStyle),
                        TableCellContent.text(reference).withStyle(valueStyle),
                        TableCellContent.text("Reference").withStyle(labelStyle),
                        TableCellContent.text(docTitle).withStyle(titleStyle)),
                List.of(
                        TableCellContent.text(due).withStyle(valueStyle),
                        TableCellContent.text("Due").withStyle(labelStyle),
                        TableCellContent.text(status).withStyle(valueStyle),
                        TableCellContent.text("Status").withStyle(labelStyle),
                        TableCellContent.text("").withStyle(emptyStyle)));

        return new TemplateTableSpec(
                "InvoiceHeader",
                List.of(
                        TableColumnLayout.fixed(chipValueA),
                        TableColumnLayout.fixed(chipLabelA),
                        TableColumnLayout.fixed(chipValueB),
                        TableColumnLayout.fixed(chipLabelB),
                        TableColumnLayout.fixed(titleWidth)),
                rows,
                emptyStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.zero());
    }

    private TemplateTableSpec headerSubtitleTable(TemplateComposeTarget target, InvoiceData data, double leftOffset) {
        double width = target.pageWidth();
        TableCellLayoutStyle baseStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyBoldStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        List<String> lines = new ArrayList<>();
        lines.add("Invoice #" + valueOrFallback(data.invoiceNumber(), "Draft"));
        if (!data.status().isBlank()) {
            lines.add("Status: " + data.status());
        }
        if (!data.reference().isBlank()) {
            lines.add("Reference: " + data.reference());
        }
        return new TemplateTableSpec(
                "InvoiceHeaderSubtitle",
                List.of(TableColumnLayout.fixed(width - leftOffset)),
                List.of(List.of(TableCellContent.of(lines).withStyle(baseStyle))),
                baseStyle,
                Map.of(),
                Map.of(),
                width - leftOffset,
                Padding.zero(),
                new Margin(0, 0, 0, leftOffset));
    }

    private TemplateTableSpec partiesTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double columnWidth = sceneLayout.twoColumnWidth(width);
        TableCellLayoutStyle style = TableCellLayoutStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        return new TemplateTableSpec(
                "InvoiceParties",
                List.of(TableColumnLayout.fixed(columnWidth), TableColumnLayout.fixed(columnWidth)),
                List.of(List.of(
                        TableCellContent.of(partyLines("BILL TO", data.billToParty())),
                        TableCellContent.of(partyLines("FROM", data.fromParty())))),
                style,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                layout.sectionMargin());
    }

    private TemplateTableSpec itemsTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
                .textStyle(styles.bodyStyle(9.3))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        Map<Integer, TableCellLayoutStyle> rowStyles = new LinkedHashMap<>();
        rowStyles.put(0, TableCellLayoutStyle.builder()
                .fillColor(styles.strongFill())
                .textStyle(styles.headingStyle(9.0))
                .build());

        Map<Integer, TableCellLayoutStyle> columnStyles = new LinkedHashMap<>();
        columnStyles.put(1, TableCellLayoutStyle.builder().textAnchor(Anchor.center()).build());
        columnStyles.put(2, TableCellLayoutStyle.builder().textAnchor(Anchor.centerRight()).build());
        columnStyles.put(3, TableCellLayoutStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<List<TableCellContent>> rows = new ArrayList<>();
        rows.add(List.of(
                TableCellContent.text("Description"),
                TableCellContent.text("Qty"),
                TableCellContent.text("Unit Price"),
                TableCellContent.text("Amount")));
        List<InvoiceLineItem> items = data.lineItems().isEmpty()
                ? List.of(new InvoiceLineItem("No line items provided", "", "-", "-", "-"))
                : data.lineItems();
        for (InvoiceLineItem item : items) {
            rows.add(List.of(
                    TableCellContent.text(composeItemDescription(item)),
                    TableCellContent.text(valueOrFallback(item.quantity(), "-")),
                    TableCellContent.text(valueOrFallback(item.unitPrice(), "-")),
                    TableCellContent.text(valueOrFallback(item.amount(), "-"))));
        }

        return new TemplateTableSpec(
                "InvoiceItemsTable",
                List.of(
                        TableColumnLayout.fixed(Math.max(220, width - 250)),
                        TableColumnLayout.fixed(68),
                        TableColumnLayout.fixed(88),
                        TableColumnLayout.fixed(94)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                sceneLayout.moduleBodyGap(layout.subsectionMargin()));
    }

    private TemplateTableSpec summaryTable(TemplateComposeTarget target, InvoiceData data) {
        double summaryWidth = sceneLayout.invoiceSummaryWidth();
        double summaryLabelWidth = Math.max(60, summaryWidth * 0.55);
        double summaryValueWidth = Math.max(40, summaryWidth - summaryLabelWidth);
        List<InvoiceSummaryRow> rows = summaryRowsOrDefault(data);

        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        Map<Integer, TableCellLayoutStyle> rowStyles = new LinkedHashMap<>();
        for (int row = 0; row < rows.size(); row++) {
            if (rows.get(row).emphasized()) {
                rowStyles.put(row, TableCellLayoutStyle.builder()
                        .textStyle(styles.bodyBoldStyle(BODY_SIZE))
                        .build());
            }
        }
        Map<Integer, TableCellLayoutStyle> columnStyles = new LinkedHashMap<>();
        columnStyles.put(1, TableCellLayoutStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<List<TableCellContent>> cells = new ArrayList<>();
        for (InvoiceSummaryRow row : rows) {
            cells.add(List.of(
                    TableCellContent.text(row.label()),
                    TableCellContent.text(row.value())));
        }

        return new TemplateTableSpec(
                "InvoiceSummary",
                List.of(
                        TableColumnLayout.fixed(summaryLabelWidth),
                        TableColumnLayout.fixed(summaryValueWidth)),
                cells,
                defaultStyle,
                rowStyles,
                columnStyles,
                summaryWidth,
                Padding.zero(),
                sceneLayout.moduleBodyGap(sceneLayout.notesSummaryMargin()));
    }

    private TemplateTableSpec notesTable(TemplateComposeTarget target, InvoiceData data) {
        double width = target.pageWidth();
        double summaryWidth = sceneLayout.invoiceSummaryWidth();
        double notesWidth = Math.max(220, width - summaryWidth - sceneLayout.columnGap());
        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(Padding.zero())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle notesStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .build();

        return new TemplateTableSpec(
                "InvoiceNotes",
                List.of(TableColumnLayout.fixed(notesWidth)),
                List.of(List.of(
                        TableCellContent.of(notesAndTermsLines(data)).withStyle(notesStyle))),
                defaultStyle,
                Map.of(),
                Map.of(),
                notesWidth,
                Padding.zero(),
                new Margin(-20, 0, 0, summaryWidth + sceneLayout.columnGap()));
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

    private List<InvoiceSummaryRow> summaryRowsOrDefault(InvoiceData data) {
        return data.summaryRows().isEmpty()
                ? List.of(new InvoiceSummaryRow("Total", "-", true))
                : data.summaryRows();
    }

    private double headerRightOffset(double width) {
        double chipValueA = 86.0;
        double chipLabelA = 56.0;
        double chipValueB = Math.max(140.0, (width - 200.0) * 0.40);
        double chipLabelB = 64.0;
        return chipValueA + chipLabelA + chipValueB + chipLabelB;
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
