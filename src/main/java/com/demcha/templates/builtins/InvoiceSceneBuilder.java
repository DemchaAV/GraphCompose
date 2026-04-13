package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.ElementBuilder;
import com.demcha.compose.layout_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.data.InvoiceData;
import com.demcha.templates.data.InvoiceLineItem;
import com.demcha.templates.data.InvoiceParty;
import com.demcha.templates.data.InvoiceSummaryRow;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral scene builder for the built-in invoice template.
 */
final class InvoiceSceneBuilder {
    private static final double ROOT_SPACING = 10;
    private static final double SECTION_SPACING = 6;
    private static final double COLUMN_GAP = 18;
    private static final double RULE_HEIGHT = 6;
    private static final double RULE_STROKE = 1.2;
    private static final double BODY_SIZE = 10.1;
    private static final double LABEL_SIZE = 8.5;
    private static final double META_SIZE = 9.3;
    private static final double TITLE_SIZE = 26;
    private static final double SUBTITLE_SIZE = 11;
    private static final double SUMMARY_WIDTH = 206;
    private static final double NOTES_MIN_HEIGHT = 12;
    private static final String ROOT_NAME = "InvoiceRoot";

    private final BusinessDocumentSceneStyles styles;

    InvoiceSceneBuilder(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    void compose(DocumentComposer composer, InvoiceData data) {
        designDocument(composer, safeData(data));
    }

    private void designDocument(DocumentComposer composer, InvoiceData data) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        VContainerBuilder root = cb.vContainer(Align.left(ROOT_SPACING))
                .entityName(ROOT_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createHeader(cb, data, width));
        root.addChild(createRule(cb, width, Margin.top(2)));
        root.addChild(createBillingRow(cb, data, width));
        root.addChild(createSectionHeader(cb, "LINE ITEMS", width, Margin.top(4)));
        root.addChild(createItemsTable(cb, data, width));
        root.addChild(createNotesAndSummary(cb, data, width));

        if (!data.footerNote().isBlank()) {
            root.addChild(createRule(cb, width, Margin.top(4)));
            root.addChild(createParagraph(cb, List.of(data.footerNote()), width, Margin.top(4), META_SIZE, "InvoiceFooter"));
        }

        root.build();
    }

    private Entity createHeader(ComponentBuilder cb, InvoiceData data, double width) {
        double leftWidth = Math.max(220, width - 188);
        double rightWidth = width - leftWidth - COLUMN_GAP;

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        row.addChild(createAlignedCell(cb, createHeaderLeft(cb, data, leftWidth), leftWidth, Anchor.topLeft()));
        row.addChild(createSpacer(cb, COLUMN_GAP, 1));
        row.addChild(createAlignedCell(cb, createHeaderRight(cb, data, rightWidth), rightWidth, Anchor.topRight()));
        return row.build();
    }

    private Entity createHeaderLeft(ComponentBuilder cb, InvoiceData data, double width) {
        VContainerBuilder column = cb.vContainer(Align.left(SECTION_SPACING))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        column.addChild(createText(cb, data.title(), styles.titleStyle(TITLE_SIZE), Anchor.topLeft(), Margin.zero()));
        column.addChild(createText(cb, "Invoice #" + valueOrFallback(data.invoiceNumber(), "Draft"),
                styles.headingStyle(SUBTITLE_SIZE + 1), Anchor.topLeft(), Margin.zero()));

        if (!data.status().isBlank()) {
            column.addChild(createText(cb, "Status: " + data.status(), styles.labelStyle(LABEL_SIZE),
                    Anchor.topLeft(), Margin.zero()));
        }

        if (!data.reference().isBlank()) {
            column.addChild(createText(cb, "Reference: " + data.reference(), styles.metaStyle(META_SIZE),
                    Anchor.topLeft(), Margin.zero()));
        }

        return column.build();
    }

    private Entity createHeaderRight(ComponentBuilder cb, InvoiceData data, double width) {
        VContainerBuilder column = cb.vContainer(Align.left(SECTION_SPACING - 1))
                .size(width, 0)
                .anchor(Anchor.topRight());

        column.addChild(createMetaLine(cb, "Issued", valueOrFallback(data.issueDate(), "TBD"), width));
        column.addChild(createMetaLine(cb, "Due", valueOrFallback(data.dueDate(), "TBD"), width));

        if (!data.reference().isBlank()) {
            column.addChild(createMetaLine(cb, "Reference", data.reference(), width));
        }

        if (!data.status().isBlank()) {
            column.addChild(createMetaLine(cb, "Status", data.status(), width));
        }

        return column.build();
    }

    private Entity createBillingRow(ComponentBuilder cb, InvoiceData data, double width) {
        double columnWidth = (width - COLUMN_GAP) / 2.0;
        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        row.addChild(createAlignedCell(cb, createPartyColumn(cb, "FROM", data.fromParty(), columnWidth), columnWidth, Anchor.topLeft()));
        row.addChild(createSpacer(cb, COLUMN_GAP, 1));
        row.addChild(createAlignedCell(cb, createPartyColumn(cb, "BILL TO", data.billToParty(), columnWidth), columnWidth, Anchor.topLeft()));
        return row.build();
    }

    private Entity createPartyColumn(ComponentBuilder cb, String title, InvoiceParty party, double width) {
        InvoiceParty safeParty = party == null ? new InvoiceParty("", List.of(), "", "", "") : party;

        VContainerBuilder column = cb.vContainer(Align.left(4))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        column.addChild(createText(cb, title, styles.labelStyle(LABEL_SIZE), Anchor.topLeft(), Margin.top(2)));
        column.addChild(createText(cb, valueOrFallback(safeParty.name(), "Not provided"), styles.bodyBoldStyle(BODY_SIZE + 1),
                Anchor.topLeft(), Margin.zero()));

        List<String> lines = new ArrayList<>(safeParty.addressLines());
        if (!safeParty.email().isBlank()) {
            lines.add("Email: " + safeParty.email());
        }
        if (!safeParty.phone().isBlank()) {
            lines.add("Phone: " + safeParty.phone());
        }
        if (!safeParty.taxId().isBlank()) {
            lines.add("Tax ID: " + safeParty.taxId());
        }
        if (lines.isEmpty()) {
            lines = List.of("No additional contact details provided.");
        }

        column.addChild(createParagraph(cb, lines, width, Margin.zero(), BODY_SIZE, title + "Details"));
        return column.build();
    }

    private Entity createItemsTable(ComponentBuilder cb, InvoiceData data, double width) {
        TableBuilder table = cb.table()
                .entityName("InvoiceItemsTable")
                .anchor(Anchor.topLeft())
                .columns(
                        TableColumnSpec.fixed(Math.max(220, width - 250)),
                        TableColumnSpec.fixed(68),
                        TableColumnSpec.fixed(88),
                        TableColumnSpec.fixed(94))
                .width(width)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(new Padding(7, 8, 7, 8))
                        .fillColor(Color.WHITE)
                        .stroke(new Stroke(styles.borderColor(), 1.3))
                        .textStyle(styles.bodyStyle(9.3))
                        .textAnchor(Anchor.centerLeft())
                        .build())
                .rowStyle(0, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.headingStyle(9.0))
                        .build())
                .columnStyle(1, TableCellStyle.builder()
                        .textAnchor(Anchor.center())
                        .build())
                .columnStyle(2, TableCellStyle.builder()
                        .textAnchor(Anchor.centerRight())
                        .build())
                .columnStyle(3, TableCellStyle.builder()
                        .textAnchor(Anchor.centerRight())
                        .build())
                .row("Description", "Qty", "Unit Price", "Amount");

        List<InvoiceLineItem> items = data.lineItems().isEmpty()
                ? List.of(new InvoiceLineItem("No line items provided", "", "-", "-", "-"))
                : data.lineItems();

        for (InvoiceLineItem item : items) {
            table.row(composeItemDescription(item), valueOrFallback(item.quantity(), "-"),
                    valueOrFallback(item.unitPrice(), "-"), valueOrFallback(item.amount(), "-"));
        }

        return table.build();
    }

    private Entity createNotesAndSummary(ComponentBuilder cb, InvoiceData data, double width) {
        double leftWidth = Math.max(220, width - SUMMARY_WIDTH - COLUMN_GAP);
        double rightWidth = width - leftWidth - COLUMN_GAP;

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.top(6));

        row.addChild(createAlignedCell(cb, createNotesColumn(cb, data, leftWidth), leftWidth, Anchor.topLeft()));
        row.addChild(createSpacer(cb, COLUMN_GAP, 1));
        row.addChild(createAlignedCell(cb, createSummaryColumn(cb, data, rightWidth), rightWidth, Anchor.topRight()));
        return row.build();
    }

    private Entity createNotesColumn(ComponentBuilder cb, InvoiceData data, double width) {
        VContainerBuilder column = cb.vContainer(Align.left(6))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        if (!data.notes().isEmpty()) {
            column.addChild(createSectionHeader(cb, "NOTES", width, Margin.zero()));
            column.addChild(createParagraph(cb, data.notes(), width, Margin.zero(), BODY_SIZE, "InvoiceNotes"));
        }

        if (!data.paymentTerms().isEmpty()) {
            column.addChild(createSectionHeader(cb, "PAYMENT TERMS", width, data.notes().isEmpty() ? Margin.zero() : Margin.top(4)));
            column.addChild(createBulletParagraph(cb, data.paymentTerms(), width, Margin.zero(), "InvoicePaymentTerms"));
        }

        if (data.notes().isEmpty() && data.paymentTerms().isEmpty()) {
            column.addChild(createSpacer(cb, width, NOTES_MIN_HEIGHT));
        }

        return column.build();
    }

    private Entity createSummaryColumn(ComponentBuilder cb, InvoiceData data, double width) {
        List<InvoiceSummaryRow> rows = data.summaryRows().isEmpty()
                ? List.of(new InvoiceSummaryRow("Total", "-", true))
                : data.summaryRows();

        TableBuilder table = cb.table()
                .entityName("InvoiceSummaryTable")
                .anchor(Anchor.topRight())
                .columns(TableColumnSpec.fixed(width - 74), TableColumnSpec.fixed(74))
                .width(width)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(new Padding(6, 8, 6, 8))
                        .fillColor(styles.softFill())
                        .stroke(new Stroke(styles.borderColor(), 1.3))
                        .textStyle(styles.bodyStyle(9.4))
                        .textAnchor(Anchor.centerLeft())
                        .build())
                .columnStyle(1, TableCellStyle.builder()
                        .textAnchor(Anchor.centerRight())
                        .build());

        for (int index = 0; index < rows.size(); index++) {
            InvoiceSummaryRow row = rows.get(index);
            table.row(row.label(), row.value());
            if (row.emphasized()) {
                table.rowStyle(index, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.bodyBoldStyle(9.6))
                        .build());
            }
        }

        return table.build();
    }

    private Entity createMetaLine(ComponentBuilder cb, String label, String value, double width) {
        Entity left = createText(cb, label, styles.labelStyle(LABEL_SIZE), Anchor.topLeft(), Margin.zero());
        Entity right = createText(cb, value, styles.bodyBoldStyle(META_SIZE), Anchor.topRight(), Margin.zero());

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        row.addChild(createAlignedCell(cb, left, Math.max(56, width - 118), Anchor.topLeft()));
        row.addChild(createSpacer(cb, 10, 1));
        row.addChild(createAlignedCell(cb, right, 108, Anchor.topRight()));
        return row.build();
    }

    private Entity createSectionHeader(ComponentBuilder cb, String value, double width, Margin margin) {
        VContainerBuilder header = cb.vContainer(Align.left(1))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(margin);
        header.addChild(createText(cb, value, styles.labelStyle(LABEL_SIZE), Anchor.topLeft(), Margin.zero()));
        header.addChild(createRule(cb, Math.min(width, 128), Margin.zero()));
        return header.build();
    }

    private Entity createRule(ComponentBuilder cb, double width, Margin margin) {
        return cb.line()
                .horizontal()
                .size(width, RULE_HEIGHT)
                .padding(Padding.of(1))
                .stroke(new Stroke(styles.accentColor(), RULE_STROKE))
                .anchor(Anchor.topLeft())
                .margin(margin)
                .build();
    }

    private Entity createParagraph(ComponentBuilder cb,
                                   List<String> paragraphs,
                                   double width,
                                   Margin margin,
                                   double fontSize,
                                   String entityName) {
        List<String> sanitized = sanitizeLines(paragraphs);
        BlockTextBuilder builder = cb.blockText(Align.left(2), styles.bodyStyle(fontSize))
                .entityName(entityName)
                .size(width, 2)
                .strategy(BlockIndentStrategy.FIRST_LINE)
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, styles.bodyStyle(fontSize), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createBulletParagraph(ComponentBuilder cb, List<String> items, double width, Margin margin, String entityName) {
        List<String> sanitized = sanitizeLines(items);
        BlockTextBuilder builder = cb.blockText(Align.left(2), styles.bodyStyle(BODY_SIZE))
                .entityName(entityName)
                .size(width, 2)
                .strategy(BlockIndentStrategy.FROM_SECOND_LINE)
                .bulletOffset("•")
                .anchor(Anchor.topLeft())
                .margin(margin)
                .padding(Padding.zero())
                .text(sanitized, styles.bodyStyle(BODY_SIZE), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createText(ComponentBuilder cb, String value, TextStyle style, Anchor anchor, Margin margin) {
        TextBuilder builder = cb.text()
                .textWithAutoSize(Objects.requireNonNullElse(value, ""))
                .textStyle(style)
                .anchor(anchor)
                .margin(margin);
        return builder.build();
    }

    private Entity createAlignedCell(ComponentBuilder cb, Entity child, double width, Anchor anchor) {
        VContainerBuilder cell = cb.vContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        child.addComponent(anchor);
        cell.addChild(child);
        return cell.build();
    }

    private Entity createSpacer(ComponentBuilder cb, double width, double height) {
        ElementBuilder spacer = cb.element()
                .size(width, height)
                .anchor(Anchor.topLeft());
        return spacer.build();
    }

    private InvoiceData safeData(InvoiceData data) {
        return Objects.requireNonNull(data, "data");
    }

    private static List<String> sanitizeLines(List<String> values) {
        return values.stream()
                .map(value -> Objects.requireNonNullElse(value, "").trim())
                .filter(value -> !value.isBlank())
                .toList();
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

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
