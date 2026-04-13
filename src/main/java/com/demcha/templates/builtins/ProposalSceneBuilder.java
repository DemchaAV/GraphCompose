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
import com.demcha.templates.data.ProposalData;
import com.demcha.templates.data.ProposalParty;
import com.demcha.templates.data.ProposalPricingRow;
import com.demcha.templates.data.ProposalSection;
import com.demcha.templates.data.ProposalTimelineItem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral scene builder for the built-in proposal template.
 */
final class ProposalSceneBuilder {
    private static final double ROOT_SPACING = 10;
    private static final double SECTION_SPACING = 5;
    private static final double COLUMN_GAP = 18;
    private static final double RULE_HEIGHT = 6;
    private static final double RULE_STROKE = 1.2;
    private static final double BODY_SIZE = 10.0;
    private static final double LABEL_SIZE = 8.5;
    private static final double META_SIZE = 9.2;
    private static final double TITLE_SIZE = 24;
    private static final String ROOT_NAME = "ProposalRoot";

    private final BusinessDocumentSceneStyles styles;

    ProposalSceneBuilder(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    void compose(DocumentComposer composer, ProposalData data) {
        designDocument(composer, safeData(data));
    }

    private void designDocument(DocumentComposer composer, ProposalData data) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        VContainerBuilder root = cb.vContainer(Align.left(ROOT_SPACING))
                .entityName(ROOT_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createHeader(cb, data, width));
        root.addChild(createRule(cb, width, Margin.top(2)));

        if (!data.executiveSummary().isBlank()) {
            root.addChild(createSectionHeader(cb, "EXECUTIVE SUMMARY", width, Margin.top(4)));
            root.addChild(createParagraph(cb, List.of(data.executiveSummary()), width, Margin.zero(), BODY_SIZE, "ProposalExecutiveSummary"));
        }

        for (int index = 0; index < data.sections().size(); index++) {
            root.addChild(createContentSection(cb, data.sections().get(index), width, index));
        }

        if (!data.timeline().isEmpty()) {
            root.addChild(createSectionHeader(cb, "TIMELINE", width, Margin.top(4)));
            root.addChild(createTimelineTable(cb, data.timeline(), width));
        }

        if (!data.pricingRows().isEmpty()) {
            root.addChild(createSectionHeader(cb, "PRICING", width, Margin.top(5)));
            root.addChild(createPricingTable(cb, data.pricingRows(), width));
        }

        if (!data.acceptanceTerms().isEmpty()) {
            root.addChild(createSectionHeader(cb, "ACCEPTANCE", width, Margin.top(5)));
            root.addChild(createBulletParagraph(cb, data.acceptanceTerms(), width, Margin.zero(), "ProposalAcceptance"));
        }

        if (!data.footerNote().isBlank()) {
            root.addChild(createRule(cb, width, Margin.top(5)));
            root.addChild(createParagraph(cb, List.of(data.footerNote()), width, Margin.top(3), META_SIZE, "ProposalFooter"));
        }

        root.build();
    }

    private Entity createHeader(ComponentBuilder cb, ProposalData data, double width) {
        VContainerBuilder root = cb.vContainer(Align.left(6))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createText(cb, data.title(), styles.titleStyle(TITLE_SIZE), Anchor.topLeft(), Margin.zero()));
        root.addChild(createText(cb, valueOrFallback(data.projectTitle(), "Project proposal"),
                styles.headingStyle(13), Anchor.topLeft(), Margin.zero()));

        HContainerBuilder metaRow = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());
        double leftWidth = Math.max(200, width - 212);
        metaRow.addChild(createAlignedCell(cb, createProposalMetaBlock(cb, data, leftWidth), leftWidth, Anchor.topLeft()));
        metaRow.addChild(createSpacer(cb, COLUMN_GAP, 1));
        metaRow.addChild(createAlignedCell(cb, createPartyColumn(cb, "PREPARED FOR", data.recipient(), 194), 194, Anchor.topLeft()));
        root.addChild(metaRow.build());

        if (data.sender() != null) {
            root.addChild(createPartyColumn(cb, "PREPARED BY", data.sender(), width));
        }

        return root.build();
    }

    private Entity createProposalMetaBlock(ComponentBuilder cb, ProposalData data, double width) {
        VContainerBuilder column = cb.vContainer(Align.left(4))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        column.addChild(createMetaLine(cb, "Proposal", valueOrFallback(data.proposalNumber(), "Draft"), width));
        column.addChild(createMetaLine(cb, "Prepared", valueOrFallback(data.preparedDate(), "TBD"), width));
        column.addChild(createMetaLine(cb, "Valid Until", valueOrFallback(data.validUntil(), "TBD"), width));
        return column.build();
    }

    private Entity createPartyColumn(ComponentBuilder cb, String title, ProposalParty party, double width) {
        ProposalParty safeParty = party == null ? new ProposalParty("", List.of(), "", "", "") : party;

        VContainerBuilder column = cb.vContainer(Align.left(4))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.top(2));

        column.addChild(createText(cb, title, styles.labelStyle(LABEL_SIZE), Anchor.topLeft(), Margin.zero()));
        if (!safeParty.name().isBlank()) {
            column.addChild(createText(cb, safeParty.name(), styles.bodyBoldStyle(BODY_SIZE + 0.7),
                    Anchor.topLeft(), Margin.zero()));
        }

        List<String> lines = new ArrayList<>(safeParty.addressLines());
        if (!safeParty.email().isBlank()) {
            lines.add("Email: " + safeParty.email());
        }
        if (!safeParty.phone().isBlank()) {
            lines.add("Phone: " + safeParty.phone());
        }
        if (!safeParty.website().isBlank()) {
            lines.add("Web: " + safeParty.website());
        }

        if (!lines.isEmpty()) {
            column.addChild(createParagraph(cb, lines, width, Margin.zero(), BODY_SIZE, title + "Party"));
        }

        return column.build();
    }

    private Entity createContentSection(ComponentBuilder cb, ProposalSection section, double width, int index) {
        VContainerBuilder block = cb.vContainer(Align.left(SECTION_SPACING))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(index == 0 ? Margin.top(2) : Margin.top(4));

        block.addChild(createSectionHeader(cb, valueOrFallback(section.title(), "SECTION"), width, Margin.zero()));

        List<String> paragraphs = section.paragraphs().isEmpty()
                ? List.of("Content is intentionally left blank.")
                : section.paragraphs();

        for (int paragraphIndex = 0; paragraphIndex < paragraphs.size(); paragraphIndex++) {
            block.addChild(createParagraph(cb, List.of(paragraphs.get(paragraphIndex)), width,
                    paragraphIndex == 0 ? Margin.zero() : Margin.top(1.5), BODY_SIZE,
                    "ProposalSection_" + index + "_" + paragraphIndex));
        }

        return block.build();
    }

    private Entity createTimelineTable(ComponentBuilder cb, List<ProposalTimelineItem> items, double width) {
        TableBuilder table = cb.table()
                .entityName("ProposalTimelineTable")
                .anchor(Anchor.topLeft())
                .columns(
                        TableColumnSpec.fixed(126),
                        TableColumnSpec.fixed(86),
                        TableColumnSpec.fixed(width - 212))
                .width(width)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(new Padding(7, 8, 7, 8))
                        .fillColor(Color.WHITE)
                        .stroke(new Stroke(styles.borderColor(), 1.3))
                        .textStyle(styles.bodyStyle(9.4))
                        .textAnchor(Anchor.centerLeft())
                        .build())
                .rowStyle(0, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.headingStyle(9.0))
                        .build())
                .columnStyle(1, TableCellStyle.builder()
                        .textAnchor(Anchor.center())
                        .build())
                .row("Phase", "Duration", "Deliverable");

        for (ProposalTimelineItem item : items) {
            table.row(valueOrFallback(item.phase(), "Phase"),
                    valueOrFallback(item.duration(), "-"),
                    shorten(valueOrFallback(item.details(), "-"), 56));
        }
        return table.build();
    }

    private Entity createPricingTable(ComponentBuilder cb, List<ProposalPricingRow> rows, double width) {
        TableBuilder table = cb.table()
                .entityName("ProposalPricingTable")
                .anchor(Anchor.topLeft())
                .columns(
                        TableColumnSpec.fixed(128),
                        TableColumnSpec.fixed(width - 230),
                        TableColumnSpec.fixed(102))
                .width(width)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(new Padding(7, 8, 7, 8))
                        .fillColor(styles.softFill())
                        .stroke(new Stroke(styles.borderColor(), 1.3))
                        .textStyle(styles.bodyStyle(9.4))
                        .textAnchor(Anchor.centerLeft())
                        .build())
                .rowStyle(0, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.headingStyle(9.0))
                        .build())
                .columnStyle(2, TableCellStyle.builder()
                        .textAnchor(Anchor.centerRight())
                        .build())
                .row("Item", "Description", "Amount");

        for (int index = 0; index < rows.size(); index++) {
            ProposalPricingRow row = rows.get(index);
            table.row(valueOrFallback(row.label(), "Item"),
                    shorten(valueOrFallback(row.description(), "-"), 60),
                    valueOrFallback(row.amount(), "-"));
            if (row.emphasized()) {
                table.rowStyle(index + 1, TableCellStyle.builder()
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
        row.addChild(createAlignedCell(cb, left, Math.max(74, width - 138), Anchor.topLeft()));
        row.addChild(createSpacer(cb, 12, 1));
        row.addChild(createAlignedCell(cb, right, 126, Anchor.topRight()));
        return row.build();
    }

    private Entity createSectionHeader(ComponentBuilder cb, String value, double width, Margin margin) {
        VContainerBuilder header = cb.vContainer(Align.left(1))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(margin);
        header.addChild(createText(cb, value, styles.labelStyle(LABEL_SIZE), Anchor.topLeft(), Margin.zero()));
        header.addChild(createRule(cb, Math.min(width, 132), Margin.zero()));
        return header.build();
    }

    private Entity createRule(ComponentBuilder cb, double width, Margin margin) {
        return cb.line()
                .horizontal()
                .size(width, 6)
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

    private ProposalData safeData(ProposalData data) {
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

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
