package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;
import com.demcha.compose.document.templates.support.common.TemplateColumnSpec;
import com.demcha.compose.document.templates.support.common.TemplateComposeTarget;
import com.demcha.compose.document.templates.support.common.TemplateDividerSpec;
import com.demcha.compose.document.templates.support.common.TemplateLayoutPolicy;
import com.demcha.compose.document.templates.support.common.TemplateModuleBlock;
import com.demcha.compose.document.templates.support.common.TemplateModuleSpec;
import com.demcha.compose.document.templates.support.common.TemplateParagraphSpec;
import com.demcha.compose.document.templates.support.common.TemplateRowSpec;
import com.demcha.compose.document.templates.support.common.TemplateSceneSupport;
import com.demcha.compose.document.templates.support.common.TemplateTableSpec;
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
    private static final double HEADER_LEFT_WIDTH = 182.0;
    private static final double SUMMARY_WIDTH = 206.0;

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
        target.addRow(headerRow(width, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "InvoiceRule",
                width,
                sceneLayout.mainDividerThickness(),
                styles.accentColor(),
                layout.subsectionMargin()));

        target.addRow(partiesRow(width, safe));
        target.addModule(new TemplateModuleSpec(
                "InvoiceItems",
                p("InvoiceItemsHeading", "LINE ITEMS", styles.labelStyle(LABEL_SIZE), layout.sectionMargin()),
                List.of(
                        TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                                "InvoiceItemsRule",
                                sceneLayout.boundedRuleWidth(width, sceneLayout.invoiceItemsRuleWidth()),
                                sceneLayout.mainDividerThickness(),
                                styles.accentColor(),
                                layout.top(layout.rootSpacing()))),
                        TemplateModuleBlock.table(itemsTable(width, safe)),
                        TemplateModuleBlock.row(summaryNotesRow(width, safe)))));

        if (!safe.footerNote().isBlank()) {
            target.addModule(new TemplateModuleSpec(
                    "InvoiceFooter",
                    null,
                    List.of(
                            TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                                    "InvoiceFooterRule",
                                    width,
                                    sceneLayout.subtleDividerThickness(),
                                    styles.accentColor(),
                                    layout.sectionMargin())),
                            TemplateModuleBlock.paragraph(p(
                                    "InvoiceFooter",
                                    safe.footerNote(),
                                    styles.bodyStyle(9.3),
                                    sceneLayout.moduleBodyGap(layout.subsectionMargin()))))));
        }
        target.finishDocument();
    }

    private TemplateRowSpec headerRow(double pageWidth, InvoiceData data) {
        double rightWidth = Math.max(150.0, pageWidth - HEADER_LEFT_WIDTH - sceneLayout.columnGap());
        return new TemplateRowSpec(
                "InvoiceHeader",
                List.of(
                        TemplateColumnSpec.of(
                                "InvoiceHeaderMeta",
                                List.of(TemplateModuleBlock.table(headerMetaTable(data))),
                                0.0),
                        TemplateColumnSpec.of(
                                "InvoiceHeaderTitle",
                                List.of(
                                        TemplateModuleBlock.paragraph(p(
                                                "InvoiceTitle",
                                                valueOrFallback(data.title(), "Invoice"),
                                                styles.titleStyle(28.0),
                                                Margin.zero())),
                                        TemplateModuleBlock.paragraph(p(
                                                "InvoiceNumber",
                                                "Invoice #" + valueOrFallback(data.invoiceNumber(), "Draft"),
                                                styles.bodyBoldStyle(10.6),
                                                layout.top(11.0))),
                                        TemplateModuleBlock.paragraph(p(
                                                "InvoiceStatus",
                                                "Status: " + valueOrFallback(data.status(), "—"),
                                                styles.labelStyle(9.0),
                                                layout.top(1.5))),
                                        TemplateModuleBlock.paragraph(p(
                                                "InvoiceReference",
                                                "Reference: " + valueOrFallback(data.reference(), "—"),
                                                styles.metaStyle(9.0),
                                                layout.top(1.0)))),
                                0.0)),
                List.of(HEADER_LEFT_WIDTH, rightWidth),
                sceneLayout.columnGap(),
                Padding.zero(),
                Margin.zero());
    }

    private TemplateTableSpec headerMetaTable(InvoiceData data) {
        TableCellLayoutStyle valueStyle = chromeFreeCell(styles.bodyBoldStyle(10.2));
        TableCellLayoutStyle labelStyle = chromeFreeCell(styles.labelStyle(8.3));

        List<List<TableCellContent>> rows = List.of(
                List.of(
                        TableCellContent.text(valueOrFallback(data.issueDate(), "TBD")).withStyle(valueStyle),
                        TableCellContent.text("Issued").withStyle(labelStyle)),
                List.of(
                        TableCellContent.text(valueOrFallback(data.dueDate(), "TBD")).withStyle(valueStyle),
                        TableCellContent.text("Due").withStyle(labelStyle)),
                List.of(
                        TableCellContent.text(valueOrFallback(data.reference(), "—")).withStyle(valueStyle),
                        TableCellContent.text("Reference").withStyle(labelStyle)),
                List.of(
                        TableCellContent.text(valueOrFallback(data.status(), "—")).withStyle(valueStyle),
                        TableCellContent.text("Status").withStyle(labelStyle)));

        return new TemplateTableSpec(
                "InvoiceHeaderMetaTable",
                List.of(
                        TableColumnLayout.fixed(122.0),
                        TableColumnLayout.fixed(60.0)),
                rows,
                valueStyle,
                Map.of(),
                Map.of(),
                HEADER_LEFT_WIDTH,
                Padding.zero(),
                Margin.zero());
    }

    private TemplateRowSpec partiesRow(double pageWidth, InvoiceData data) {
        double columnWidth = sceneLayout.twoColumnWidth(pageWidth);
        return TemplateRowSpec.weighted(
                "InvoiceParties",
                List.of(
                        TemplateColumnSpec.of("InvoiceBillTo", partyBlocks("BILL TO", data.billToParty()), 0.0),
                        TemplateColumnSpec.of("InvoiceFrom", partyBlocks("FROM", data.fromParty()), 0.0)),
                List.of(155.0, Math.max(columnWidth, pageWidth - 155.0 - sceneLayout.columnGap())),
                sceneLayout.columnGap());
    }

    private List<TemplateModuleBlock> partyBlocks(String title, InvoiceParty party) {
        InvoiceParty safeParty = party == null ? new InvoiceParty("", List.of(), "", "", "") : party;
        List<TemplateModuleBlock> blocks = new ArrayList<>();
        blocks.add(TemplateModuleBlock.paragraph(p(
                "Invoice" + title.replace(" ", "") + "Heading",
                title,
                styles.labelStyle(LABEL_SIZE),
                Margin.zero())));
        blocks.add(TemplateModuleBlock.paragraph(p(
                "Invoice" + title.replace(" ", "") + "Name",
                valueOrFallback(safeParty.name(), "Not provided"),
                styles.bodyBoldStyle(BODY_SIZE),
                layout.top(7.0))));
        for (String line : safeParty.addressLines()) {
            blocks.add(TemplateModuleBlock.paragraph(p(
                    "Invoice" + title.replace(" ", "") + "Address",
                    line,
                    styles.bodyStyle(BODY_SIZE),
                    layout.top(1.0))));
        }
        addPartyContact(blocks, title, "Email: ", safeParty.email());
        addPartyContact(blocks, title, "Phone: ", safeParty.phone());
        addPartyContact(blocks, title, "Tax ID: ", safeParty.taxId());
        return List.copyOf(blocks);
    }

    private void addPartyContact(List<TemplateModuleBlock> blocks, String title, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        blocks.add(TemplateModuleBlock.paragraph(p(
                "Invoice" + title.replace(" ", "") + label.replace(": ", ""),
                label + value,
                styles.bodyStyle(BODY_SIZE),
                layout.top(1.0))));
    }

    private TemplateTableSpec itemsTable(double width, InvoiceData data) {
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

    private TemplateRowSpec summaryNotesRow(double pageWidth, InvoiceData data) {
        double notesWidth = Math.max(220, pageWidth - SUMMARY_WIDTH - sceneLayout.columnGap());
        return new TemplateRowSpec(
                "InvoiceSummaryNotes",
                List.of(
                        TemplateColumnSpec.of(
                                "InvoiceSummaryColumn",
                                List.of(TemplateModuleBlock.table(summaryTable(data))),
                                0.0),
                        TemplateColumnSpec.of(
                                "InvoiceNotesColumn",
                                notesBlocks(notesWidth, data),
                                0.0)),
                List.of(SUMMARY_WIDTH, notesWidth),
                sceneLayout.columnGap(),
                Padding.zero(),
                sceneLayout.moduleBodyGap(sceneLayout.notesSummaryMargin()));
    }

    private TemplateTableSpec summaryTable(InvoiceData data) {
        double labelWidth = Math.max(60, SUMMARY_WIDTH * 0.55);
        double valueWidth = Math.max(40, SUMMARY_WIDTH - labelWidth);
        List<InvoiceSummaryRow> rows = summaryRowsOrDefault(data);

        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(styles.softFill())
                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
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
                        TableColumnLayout.fixed(labelWidth),
                        TableColumnLayout.fixed(valueWidth)),
                cells,
                defaultStyle,
                rowStyles,
                columnStyles,
                SUMMARY_WIDTH,
                Padding.zero(),
                Margin.zero());
    }

    private List<TemplateModuleBlock> notesBlocks(double notesWidth, InvoiceData data) {
        List<TemplateModuleBlock> blocks = new ArrayList<>();
        appendTextSection(blocks, "InvoiceNotes", "NOTES", notesWidth, data.notes());
        appendTextSection(blocks, "InvoicePaymentTerms", "PAYMENT TERMS", notesWidth, data.paymentTerms());
        if (blocks.isEmpty()) {
            blocks.add(TemplateModuleBlock.paragraph(p(
                    "InvoiceNotesEmpty",
                    "",
                    styles.bodyStyle(BODY_SIZE),
                    Margin.zero())));
        }
        return List.copyOf(blocks);
    }

    private void appendTextSection(List<TemplateModuleBlock> blocks,
                                   String prefix,
                                   String title,
                                   double notesWidth,
                                   List<String> lines) {
        List<String> safeLines = TemplateSceneSupport.sanitizeLines(lines);
        if (safeLines.isEmpty()) {
            return;
        }
        Margin headingMargin = blocks.isEmpty() ? Margin.zero() : layout.top(9.0);
        blocks.add(TemplateModuleBlock.paragraph(p(prefix + "Heading", title, styles.labelStyle(LABEL_SIZE), headingMargin)));
        blocks.add(TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                prefix + "Rule",
                Math.min(notesWidth, sceneLayout.invoiceItemsRuleWidth()),
                sceneLayout.sectionDividerThickness(),
                styles.accentColor(),
                layout.top(2.0))));
        for (String line : wrapLines(safeLines, 72)) {
            blocks.add(TemplateModuleBlock.paragraph(p(
                    prefix + "Body",
                    line,
                    styles.bodyStyle(9.4),
                    layout.top(3.0))));
        }
    }

    private TemplateParagraphSpec p(String name,
                                    String text,
                                    com.demcha.compose.engine.components.content.text.TextStyle style,
                                    Margin margin) {
        return TemplateSceneSupport.paragraph(
                name,
                text,
                style,
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                margin);
    }

    private TableCellLayoutStyle chromeFreeCell(com.demcha.compose.engine.components.content.text.TextStyle style) {
        return TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(style)
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
    }

    private List<InvoiceSummaryRow> summaryRowsOrDefault(InvoiceData data) {
        return data.summaryRows().isEmpty()
                ? List.of(new InvoiceSummaryRow("Total", "-", true))
                : data.summaryRows();
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
