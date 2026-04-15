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

    private final BusinessDocumentSceneStyles styles;

    public ProposalTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public void compose(TemplateComposeTarget target, ProposalData data) {
        ProposalData safe = Objects.requireNonNull(data, "data");
        double width = target.pageWidth();

        target.startDocument("ProposalRoot", ROOT_SPACING);
        target.addParagraph(TemplateSceneSupport.paragraph(
                "ProposalTitle",
                safe.title(),
                styles.titleStyle(TITLE_SIZE),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "ProposalProjectTitle",
                valueOrFallback(safe.projectTitle(), "Project proposal"),
                styles.headingStyle(13),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "ProposalMeta",
                TemplateSceneSupport.joinNonBlank(" | ",
                        "Proposal: " + valueOrFallback(safe.proposalNumber(), "Draft"),
                        "Prepared: " + valueOrFallback(safe.preparedDate(), "TBD"),
                        "Valid Until: " + valueOrFallback(safe.validUntil(), "TBD")),
                styles.metaStyle(9.2),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.top(2)));
        if (safe.recipient() != null) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ProposalRecipient",
                    String.join("\n", partyLines("Prepared For", safe.recipient())),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(3)));
        }
        if (safe.sender() != null) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ProposalSender",
                    String.join("\n", partyLines("Prepared By", safe.sender())),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(2)));
        }
        target.addDivider(TemplateSceneSupport.divider(
                "ProposalRule",
                width,
                1.2,
                styles.accentColor(),
                Margin.top(2)));

        if (!safe.executiveSummary().isBlank()) {
            TemplateSceneSupport.addSectionHeader(target, "ProposalSummary", "EXECUTIVE SUMMARY",
                    styles.labelStyle(LABEL_SIZE), Math.min(width, 170), styles.accentColor(), 1.1, Margin.top(4));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ProposalExecutiveSummary",
                    safe.executiveSummary(),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(3)));
        }

        for (int index = 0; index < safe.sections().size(); index++) {
            ProposalSection section = safe.sections().get(index);
            TemplateSceneSupport.addSectionHeader(target, "ProposalSection" + index,
                    valueOrFallback(section.title(), "SECTION"),
                    styles.labelStyle(LABEL_SIZE),
                    Math.min(width, 132),
                    styles.accentColor(),
                    1.0,
                    Margin.top(index == 0 ? 4 : 5));
            for (int paragraphIndex = 0; paragraphIndex < section.paragraphs().size(); paragraphIndex++) {
                target.addParagraph(TemplateSceneSupport.paragraph(
                        "ProposalSection_" + index + "_" + paragraphIndex,
                        section.paragraphs().get(paragraphIndex),
                        styles.bodyStyle(BODY_SIZE),
                        TextAlign.LEFT,
                        2.0,
                        Padding.zero(),
                        Margin.top(paragraphIndex == 0 ? 3 : 2)));
            }
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
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ProposalAcceptanceTerms",
                    TemplateSceneSupport.bulletText(safe.acceptanceTerms()),
                    styles.bodyStyle(BODY_SIZE),
                    TextAlign.LEFT,
                    2.0,
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
                    TableCellSpec.text(shorten(valueOrFallback(item.details(), "-"), 64))));
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
                    TableCellSpec.text(shorten(valueOrFallback(row.description(), "-"), 64)),
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
        List<String> lines = new ArrayList<>();
        lines.add(label);
        if (!party.name().isBlank()) {
            lines.add(party.name());
        }
        lines.addAll(party.addressLines());
        if (!party.email().isBlank()) {
            lines.add("Email: " + party.email());
        }
        if (!party.phone().isBlank()) {
            lines.add("Phone: " + party.phone());
        }
        if (!party.website().isBlank()) {
            lines.add("Web: " + party.website());
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
