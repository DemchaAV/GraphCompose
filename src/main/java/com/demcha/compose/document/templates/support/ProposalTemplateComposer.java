package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.ProposalData;
import com.demcha.compose.document.templates.data.ProposalParty;
import com.demcha.compose.document.templates.data.ProposalPricingRow;
import com.demcha.compose.document.templates.data.ProposalSection;
import com.demcha.compose.document.templates.data.ProposalTimelineItem;
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
 * Shared scene composer for the proposal template.
 */
public final class ProposalTemplateComposer {
    private static final double ROOT_SPACING = 10;
    private static final double BODY_SIZE = 10.0;
    private static final double LABEL_SIZE = 8.5;
    private static final double TITLE_SIZE = 24;
    private static final double COLUMN_GAP = 18;

    private final BusinessDocumentSceneStyles styles;

    public ProposalTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public void compose(TemplateComposeTarget target, ProposalData data) {
        ProposalData safe = Objects.requireNonNull(data, "data");
        double width = target.pageWidth();

        target.startDocument("ProposalRoot", ROOT_SPACING);
        target.addTable(headerTable(target, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "ProposalRule",
                width,
                1.2,
                styles.accentColor(),
                Margin.top(2)));

        if (!safe.executiveSummary().isBlank()) {
            TemplateSceneSupport.addSectionHeader(target, "ProposalSummary", "EXECUTIVE SUMMARY",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 170), styles.accentColor(), 1.1, Margin.top(4));
            target.addParagraph(TemplateSceneSupport.blockParagraph(
                    "ProposalExecutiveSummary",
                    safe.executiveSummary(),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    "",
                    com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy.FIRST_LINE,
                    Padding.zero(),
                    Margin.zero()));
        }

        for (int index = 0; index < safe.sections().size(); index++) {
            ProposalSection section = safe.sections().get(index);
            TemplateSceneSupport.addSectionHeader(target, "ProposalSection" + index,
                    valueOrFallback(section.title(), "SECTION"),
                    styles.labelStyle(LABEL_SIZE),
                    Math.min(width, 132),
                    styles.accentColor(),
                    1.0,
                    Margin.top(index == 0 ? 2 : 4));
            target.addParagraph(TemplateSceneSupport.blockParagraph(
                    "ProposalSection_" + index,
                    String.join("\n", section.paragraphs().isEmpty()
                            ? List.of("Content is intentionally left blank.")
                            : section.paragraphs()),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    "",
                    com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy.FIRST_LINE,
                    Padding.zero(),
                    Margin.zero()));
        }

        if (!safe.timeline().isEmpty()) {
            TemplateSceneSupport.addSectionHeader(target, "ProposalTimeline", "TIMELINE",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 132), styles.accentColor(), 1.0, Margin.top(5));
            target.addTable(timelineTable(target, safe.timeline()));
        }

        if (!safe.pricingRows().isEmpty()) {
            TemplateSceneSupport.addSectionHeader(target, "ProposalPricing", "PRICING",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 132), styles.accentColor(), 1.0, Margin.top(5));
            target.addTable(pricingTable(target, safe.pricingRows()));
        }

        if (!safe.acceptanceTerms().isEmpty()) {
            TemplateSceneSupport.addSectionHeader(target, "ProposalAcceptance", "ACCEPTANCE",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 132), styles.accentColor(), 1.0, Margin.top(5));
            target.addParagraph(TemplateSceneSupport.blockParagraph(
                    "ProposalAcceptanceTerms",
                    String.join("\n", TemplateSceneSupport.sanitizeLines(safe.acceptanceTerms())),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    "\u2022",
                    com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy.FROM_SECOND_LINE,
                    Padding.zero(),
                    Margin.top(3)));
        }

        if (!safe.footerNote().isBlank()) {
            target.addDivider(TemplateSceneSupport.divider(
                    "ProposalFooterRule",
                    width,
                    1.0,
                    styles.accentColor(),
                    Margin.top(5)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ProposalFooter",
                    safe.footerNote(),
                    styles.metaStyle(9.2),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(3)));
        }

        target.finishDocument();
    }

    private TemplateTableSpec headerTable(TemplateComposeTarget target, ProposalData data) {
        double width = target.pageWidth();
        double leftWidth = Math.max(200, width - 212);
        double rightWidth = width - leftWidth - COLUMN_GAP;
        TableCellStyle baseStyle = TableCellStyle.builder()
                .padding(Padding.zero())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .build();

        return new TemplateTableSpec(
                "ProposalHeader",
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
                        TableCellSpec.of(partyLines("PREPARED FOR", data.recipient())).withStyle(TableCellStyle.builder()
                                .textStyle(styles.bodyStyle(BODY_SIZE))
                                .textAnchor(Anchor.topLeft())
                                .build()))),
                baseStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.zero());
    }

    private TemplateTableSpec timelineTable(TemplateComposeTarget target, List<ProposalTimelineItem> items) {
        double width = target.pageWidth();
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(new Padding(7, 8, 7, 8))
                .fillColor(Color.WHITE)
                .stroke(new Stroke(styles.borderColor(), 1.3))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .build();
        Map<Integer, TableCellStyle> rowStyles = Map.of(
                0, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.headingStyle(9.0))
                        .build());
        Map<Integer, TableCellStyle> columnStyles = Map.of(
                1, TableCellStyle.builder().textAnchor(Anchor.center()).build());

        List<List<TableCellSpec>> rows = new ArrayList<>();
        rows.add(List.of(TableCellSpec.text("Phase"), TableCellSpec.text("Duration"), TableCellSpec.text("Deliverable")));
        for (ProposalTimelineItem item : items) {
            rows.add(List.of(
                    TableCellSpec.text(valueOrFallback(item.phase(), "Phase")),
                    TableCellSpec.text(valueOrFallback(item.duration(), "-")),
                    TableCellSpec.text(shorten(valueOrFallback(item.details(), "-"), 56))));
        }
        return new TemplateTableSpec(
                "ProposalTimelineTable",
                List.of(
                        TableColumnSpec.fixed(126),
                        TableColumnSpec.fixed(86),
                        TableColumnSpec.fixed(width - 212)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                Margin.top(3));
    }

    private TemplateTableSpec pricingTable(TemplateComposeTarget target, List<ProposalPricingRow> rowsData) {
        double width = target.pageWidth();
        TableCellStyle defaultStyle = TableCellStyle.builder()
                .padding(new Padding(7, 8, 7, 8))
                .fillColor(styles.softFill())
                .stroke(new Stroke(styles.borderColor(), 1.3))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .build();
        Map<Integer, TableCellStyle> rowStyles = new LinkedHashMap<>();
        rowStyles.put(0, TableCellStyle.builder()
                .fillColor(styles.strongFill())
                .textStyle(styles.headingStyle(9.0))
                .build());
        Map<Integer, TableCellStyle> columnStyles = Map.of(
                2, TableCellStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<List<TableCellSpec>> rows = new ArrayList<>();
        rows.add(List.of(TableCellSpec.text("Item"), TableCellSpec.text("Description"), TableCellSpec.text("Amount")));
        for (int index = 0; index < rowsData.size(); index++) {
            ProposalPricingRow row = rowsData.get(index);
            rows.add(List.of(
                    TableCellSpec.text(valueOrFallback(row.label(), "Item")),
                    TableCellSpec.text(shorten(valueOrFallback(row.description(), "-"), 60)),
                    TableCellSpec.text(valueOrFallback(row.amount(), "-"))));
            if (row.emphasized()) {
                rowStyles.put(index + 1, TableCellStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.bodyBoldStyle(9.6))
                        .build());
            }
        }

        return new TemplateTableSpec(
                "ProposalPricingTable",
                List.of(
                        TableColumnSpec.fixed(128),
                        TableColumnSpec.fixed(width - 230),
                        TableColumnSpec.fixed(102)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                Margin.top(3));
    }

    private List<String> partyLines(String label, ProposalParty party) {
        ProposalParty safeParty = party == null ? new ProposalParty("", List.of(), "", "", "") : party;
        List<String> lines = new ArrayList<>();
        lines.add(label);
        if (!safeParty.name().isBlank()) {
            lines.add(safeParty.name());
        }
        lines.addAll(safeParty.addressLines());
        if (!safeParty.email().isBlank()) {
            lines.add("Email: " + safeParty.email());
        }
        if (!safeParty.phone().isBlank()) {
            lines.add("Phone: " + safeParty.phone());
        }
        if (!safeParty.website().isBlank()) {
            lines.add("Web: " + safeParty.website());
        }
        return lines;
    }

    private List<String> headerLeftLines(ProposalData data) {
        List<String> lines = new ArrayList<>();
        lines.add(valueOrFallback(data.title(), "Proposal"));
        lines.add(valueOrFallback(data.projectTitle(), "Project proposal"));
        lines.add("Proposal" + valueOrFallback(data.proposalNumber(), "Draft"));
        lines.add("Prepared" + valueOrFallback(data.preparedDate(), "TBD"));
        lines.add("Valid Until" + valueOrFallback(data.validUntil(), "TBD"));
        if (data.sender() != null) {
            lines.addAll(partyLines("PREPARED BY", data.sender()));
        }
        return lines;
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
