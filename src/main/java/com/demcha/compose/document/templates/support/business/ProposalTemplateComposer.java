package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalParty;
import com.demcha.compose.document.templates.data.proposal.ProposalPricingRow;
import com.demcha.compose.document.templates.data.proposal.ProposalSection;
import com.demcha.compose.document.templates.data.proposal.ProposalTimelineItem;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared scene composer for the proposal template.
 */
public final class ProposalTemplateComposer {
    private static final double BODY_SIZE = 10.0;
    private static final double LABEL_SIZE = 8.5;

    private final BusinessDocumentSceneStyles styles;
    private final BusinessDocumentLayoutPolicy sceneLayout;
    private final TemplateLayoutPolicy layout;

    /**
     * Creates a proposal scene composer with the supplied business document styles.
     *
     * @param styles shared proposal visual styles
     */
    public ProposalTemplateComposer(BusinessDocumentSceneStyles styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
        this.sceneLayout = BusinessDocumentLayoutPolicy.standard();
        this.layout = sceneLayout.rhythm();
    }

    /**
     * Emits proposal header, sections, timeline, pricing, acceptance, and footer modules.
     *
     * @param target canonical template compose target
     * @param spec proposal document spec
     */
    public void compose(TemplateComposeTarget target, ProposalDocumentSpec spec) {
        ProposalData safe = Objects.requireNonNull(spec, "spec").proposal();
        double width = target.pageWidth();

        target.startDocument("ProposalRoot", layout.rootSpacing());
        target.addTable(headerTable(target, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "ProposalRule",
                width,
                sceneLayout.mainDividerThickness(),
                styles.accentColor(),
                layout.subsectionMargin()));

        if (!safe.executiveSummary().isBlank()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalSummary",
                    "EXECUTIVE SUMMARY",
                    sceneLayout.proposalSummaryRuleWidth(),
                    sceneLayout.proposalSummaryDividerThickness(),
                    layout.subsectionMargin(),
                    TemplateModuleBlock.paragraph(TemplateSceneSupport.blockParagraph(
                            "ProposalExecutiveSummary",
                            safe.executiveSummary(),
                            styles.bodyStyle(BODY_SIZE),
                            TextAlign.LEFT,
                            layout.bodyLineSpacing(),
                            "",
                            com.demcha.compose.engine.components.content.text.TextIndentStrategy.FIRST_LINE,
                            layout.bodyPadding(),
                            sceneLayout.moduleBodyGap(Margin.zero())))));
        }

        for (int index = 0; index < safe.sections().size(); index++) {
            ProposalSection section = safe.sections().get(index);
            target.addModule(sectionModule(
                    width,
                    "ProposalSection" + index,
                    valueOrFallback(section.title(), "SECTION"),
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.subsectionMargin(),
                    TemplateModuleBlock.paragraph(TemplateSceneSupport.blockParagraph(
                            "ProposalSection_" + index,
                            String.join("\n", section.paragraphs().isEmpty()
                                    ? List.of("Content is intentionally left blank.")
                                    : section.paragraphs()),
                            styles.bodyStyle(BODY_SIZE),
                            TextAlign.LEFT,
                            layout.bodyLineSpacing(),
                            "",
                            com.demcha.compose.engine.components.content.text.TextIndentStrategy.FIRST_LINE,
                            layout.bodyPadding(),
                            sceneLayout.moduleBodyGap(Margin.zero())))));
        }

        if (!safe.timeline().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalTimeline",
                    "TIMELINE",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.sectionMargin(),
                    TemplateModuleBlock.table(timelineTable(target, safe.timeline()))));
        }

        if (!safe.pricingRows().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalPricing",
                    "PRICING",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.sectionMargin(),
                    TemplateModuleBlock.table(pricingTable(target, safe.pricingRows()))));
        }

        if (!safe.acceptanceTerms().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalAcceptance",
                    "ACCEPTANCE",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.sectionMargin(),
                    TemplateModuleBlock.list(TemplateSceneSupport.list(
                            "ProposalAcceptanceTerms",
                            TemplateSceneSupport.sanitizeLines(safe.acceptanceTerms()),
                            com.demcha.compose.document.node.ListMarker.bullet(),
                            styles.bodyStyle(BODY_SIZE),
                            TextAlign.LEFT,
                            layout.bodyLineSpacing(),
                            layout.bodyItemSpacing(),
                            layout.bodyPadding(),
                            sceneLayout.moduleBodyGap(layout.blockMargin())))));
        }

        if (!safe.footerNote().isBlank()) {
            TemplateDividerSpec footerRule = TemplateSceneSupport.divider(
                    "ProposalFooterRule",
                    width,
                    sceneLayout.subtleDividerThickness(),
                    styles.accentColor(),
                    layout.sectionMargin());
            TemplateParagraphSpec footerNote = TemplateSceneSupport.paragraph(
                    "ProposalFooter",
                    safe.footerNote(),
                    styles.metaStyle(9.2),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    sceneLayout.moduleBodyGap(layout.blockMargin()));

            target.addModule(new TemplateModuleSpec(
                    "ProposalFooter",
                    null,
                    List.of(
                            TemplateModuleBlock.divider(footerRule),
                            TemplateModuleBlock.paragraph(footerNote))));
        }

        target.finishDocument();
    }

    private TemplateTableSpec headerTable(TemplateComposeTarget target, ProposalData data) {
        double width = target.pageWidth();
        double leftWidth = sceneLayout.leftWidthForReservedRight(
                width,
                200,
                sceneLayout.proposalHeaderReservedWidth());
        double rightWidth = sceneLayout.rightWidth(width, leftWidth);
        TableCellLayoutStyle baseStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();

        return new TemplateTableSpec(
                "ProposalHeader",
                List.of(
                        TableColumnLayout.fixed(leftWidth),
                        TableColumnLayout.fixed(sceneLayout.columnGap()),
                        TableColumnLayout.fixed(rightWidth)),
                List.of(List.of(
                        TableCellContent.of(headerLeftLines(data)).withStyle(TableCellLayoutStyle.builder()
                                .textStyle(styles.bodyStyle(BODY_SIZE))
                                .textAnchor(Anchor.topLeft())
                                .build()),
                        TableCellContent.text(""),
                        TableCellContent.of(partyLines("PREPARED FOR", data.recipient())).withStyle(TableCellLayoutStyle.builder()
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
        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        Map<Integer, TableCellLayoutStyle> rowStyles = Map.of(
                0, TableCellLayoutStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.headingStyle(9.0))
                        .build());
        Map<Integer, TableCellLayoutStyle> columnStyles = Map.of(
                1, TableCellLayoutStyle.builder().textAnchor(Anchor.center()).build());

        List<List<TableCellContent>> rows = new ArrayList<>();
        rows.add(List.of(TableCellContent.text("Phase"), TableCellContent.text("Duration"), TableCellContent.text("Deliverable")));
        for (ProposalTimelineItem item : items) {
            rows.add(List.of(
                    TableCellContent.text(valueOrFallback(item.phase(), "Phase")),
                    TableCellContent.text(valueOrFallback(item.duration(), "-")),
                    TableCellContent.text(shorten(valueOrFallback(item.details(), "-"), 56))));
        }
        return new TemplateTableSpec(
                "ProposalTimelineTable",
                List.of(
                        TableColumnLayout.fixed(126),
                        TableColumnLayout.fixed(86),
                        TableColumnLayout.fixed(width - 212)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                sceneLayout.moduleBodyGap(layout.blockMargin()));
    }

    private TemplateTableSpec pricingTable(TemplateComposeTarget target, List<ProposalPricingRow> rowsData) {
        double width = target.pageWidth();
        TableCellLayoutStyle defaultStyle = TableCellLayoutStyle.builder()
                .padding(layout.contentCellPadding())
                .fillColor(styles.softFill())
                .stroke(new Stroke(styles.borderColor(), sceneLayout.tableBorderThickness()))
                .textStyle(styles.bodyStyle(9.4))
                .textAnchor(Anchor.centerLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        Map<Integer, TableCellLayoutStyle> rowStyles = new LinkedHashMap<>();
        rowStyles.put(0, TableCellLayoutStyle.builder()
                .fillColor(styles.strongFill())
                .textStyle(styles.headingStyle(9.0))
                .build());
        Map<Integer, TableCellLayoutStyle> columnStyles = Map.of(
                2, TableCellLayoutStyle.builder().textAnchor(Anchor.centerRight()).build());

        List<List<TableCellContent>> rows = new ArrayList<>();
        rows.add(List.of(TableCellContent.text("Item"), TableCellContent.text("Description"), TableCellContent.text("Amount")));
        for (int index = 0; index < rowsData.size(); index++) {
            ProposalPricingRow row = rowsData.get(index);
            rows.add(List.of(
                    TableCellContent.text(valueOrFallback(row.label(), "Item")),
                    TableCellContent.text(shorten(valueOrFallback(row.description(), "-"), 60)),
                    TableCellContent.text(valueOrFallback(row.amount(), "-"))));
            if (row.emphasized()) {
                rowStyles.put(index + 1, TableCellLayoutStyle.builder()
                        .fillColor(styles.strongFill())
                        .textStyle(styles.bodyBoldStyle(9.6))
                        .build());
            }
        }

        return new TemplateTableSpec(
                "ProposalPricingTable",
                List.of(
                        TableColumnLayout.fixed(128),
                        TableColumnLayout.fixed(width - 230),
                        TableColumnLayout.fixed(102)),
                rows,
                defaultStyle,
                rowStyles,
                columnStyles,
                width,
                Padding.zero(),
                sceneLayout.moduleBodyGap(layout.blockMargin()));
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

    private TemplateModuleSpec sectionModule(double width,
                                             String moduleName,
                                             String title,
                                             double ruleWidth,
                                             double ruleThickness,
                                             Margin titleMargin,
                                             TemplateModuleBlock... bodyBlocks) {
        List<TemplateModuleBlock> blocks = new ArrayList<>();
        blocks.add(TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                moduleName + "Rule",
                sceneLayout.boundedRuleWidth(width, ruleWidth),
                ruleThickness,
                styles.accentColor(),
                layout.top(layout.rootSpacing()))));
        blocks.addAll(Arrays.asList(bodyBlocks));
        return new TemplateModuleSpec(
                moduleName,
                TemplateSceneSupport.paragraph(
                        moduleName + "Heading",
                        title,
                        styles.labelStyle(LABEL_SIZE),
                        TextAlign.LEFT,
                        1.0,
                        Padding.zero(),
                        titleMargin),
                blocks);
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
