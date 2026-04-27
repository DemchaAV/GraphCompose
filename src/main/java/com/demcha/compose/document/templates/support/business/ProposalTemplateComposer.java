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
    private static final double HEADER_LEFT_WIDTH = 340.0;
    private static final double META_WIDTH = 190.0;
    private static final double PROPOSAL_ROOT_SPACING = 7.0;
    private static final double PROPOSAL_BODY_GAP = 7.0;

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

        target.startDocument("ProposalRoot", PROPOSAL_ROOT_SPACING);
        target.addRow(headerRow(width, safe));
        target.addDivider(TemplateSceneSupport.divider(
                "ProposalRule",
                width,
                sceneLayout.mainDividerThickness(),
                styles.accentColor(),
                layout.top(3.0)));
        target.addRow(partiesRow(width, safe));

        if (!safe.executiveSummary().isBlank()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalSummary",
                    "EXECUTIVE SUMMARY",
                    sceneLayout.proposalSummaryRuleWidth(),
                    sceneLayout.proposalSummaryDividerThickness(),
                    layout.top(4.0),
                    TemplateModuleBlock.paragraph(TemplateSceneSupport.paragraph(
                            "ProposalExecutiveSummary",
                            safe.executiveSummary(),
                            styles.bodyStyle(BODY_SIZE),
                            TextAlign.LEFT,
                            layout.bodyLineSpacing(),
                            Padding.zero(),
                            layout.top(PROPOSAL_BODY_GAP)))));
        }

        if (!safe.sections().isEmpty()) {
            if (safe.sections().size() <= 2) {
                target.addRow(sectionsRow(width, safe.sections()));
            } else {
                for (int index = 0; index < safe.sections().size(); index++) {
                    ProposalSection section = safe.sections().get(index);
                    target.addModule(sectionModule(
                            width,
                            "ProposalSection" + index,
                            valueOrFallback(section.title(), "SECTION"),
                            sceneLayout.proposalSectionRuleWidth(),
                            sceneLayout.sectionDividerThickness(),
                            layout.top(4.0),
                            TemplateModuleBlock.paragraph(TemplateSceneSupport.paragraph(
                                    "ProposalSection_" + index,
                                    sectionText(section),
                                    styles.bodyStyle(BODY_SIZE),
                                    TextAlign.LEFT,
                                    layout.bodyLineSpacing(),
                                    Padding.zero(),
                                    layout.top(PROPOSAL_BODY_GAP)))));
                }
            }
        }

        if (!safe.timeline().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalTimeline",
                    "TIMELINE",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.top(5.0),
                    TemplateModuleBlock.table(timelineTable(target, safe.timeline()))));
        }

        if (!safe.pricingRows().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalPricing",
                    "PRICING",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.top(5.0),
                    TemplateModuleBlock.table(pricingTable(target, safe.pricingRows()))));
        }

        if (!safe.acceptanceTerms().isEmpty()) {
            target.addModule(sectionModule(
                    width,
                    "ProposalAcceptance",
                    "ACCEPTANCE",
                    sceneLayout.proposalSectionRuleWidth(),
                    sceneLayout.sectionDividerThickness(),
                    layout.top(5.0),
                    TemplateModuleBlock.list(TemplateSceneSupport.list(
                            "ProposalAcceptanceTerms",
                            TemplateSceneSupport.sanitizeLines(safe.acceptanceTerms()),
                            com.demcha.compose.document.node.ListMarker.bullet(),
                            styles.bodyStyle(9.6),
                            TextAlign.LEFT,
                            0.8,
                            0.8,
                            Padding.zero(),
                            layout.top(PROPOSAL_BODY_GAP)))));
        }

        if (!safe.footerNote().isBlank()) {
            TemplateDividerSpec footerRule = TemplateSceneSupport.divider(
                    "ProposalFooterRule",
                    width,
                    sceneLayout.subtleDividerThickness(),
                    styles.accentColor(),
                    layout.top(5.0));
            TemplateParagraphSpec footerNote = TemplateSceneSupport.paragraph(
                    "ProposalFooter",
                    safe.footerNote(),
                    styles.bodyStyle(9.2),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    layout.top(5.0));

            target.addModule(new TemplateModuleSpec(
                    "ProposalFooter",
                    null,
                    List.of(
                            TemplateModuleBlock.divider(footerRule),
                            TemplateModuleBlock.paragraph(footerNote))));
        }

        target.finishDocument();
    }

    private TemplateRowSpec headerRow(double pageWidth, ProposalData data) {
        double metaWidth = Math.min(META_WIDTH, pageWidth - HEADER_LEFT_WIDTH - sceneLayout.columnGap());
        return new TemplateRowSpec(
                "ProposalHeader",
                List.of(
                        TemplateColumnSpec.of(
                                "ProposalHeaderTitle",
                                List.of(
                                        TemplateModuleBlock.paragraph(p(
                                                "ProposalTitle",
                                                valueOrFallback(data.title(), "Proposal"),
                                                styles.titleStyle(27.0),
                                                Margin.zero())),
                                        TemplateModuleBlock.paragraph(p(
                                                "ProposalProjectTitle",
                                                valueOrFallback(data.projectTitle(), "Project proposal"),
                                                styles.bodyBoldStyle(11.4),
                                                layout.top(8.0))),
                                        TemplateModuleBlock.paragraph(p(
                                                "ProposalNumber",
                                                "Proposal #" + valueOrFallback(data.proposalNumber(), "Draft"),
                                                styles.labelStyle(9.0),
                                                layout.top(3.0)))),
                                0.0),
                        TemplateColumnSpec.of(
                                "ProposalHeaderMeta",
                                List.of(TemplateModuleBlock.table(headerMetaTable(data, metaWidth))),
                                0.0)),
                List.of(Math.max(220.0, pageWidth - metaWidth - sceneLayout.columnGap()), metaWidth),
                sceneLayout.columnGap(),
                Padding.zero(),
                Margin.zero());
    }

    private TemplateTableSpec headerMetaTable(ProposalData data, double width) {
        TableCellLayoutStyle baseStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyStyle(BODY_SIZE))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle valueStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.bodyBoldStyle(9.7))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();
        TableCellLayoutStyle labelStyle = TableCellLayoutStyle.builder()
                .padding(layout.compactCellPadding())
                .fillColor(Color.WHITE)
                .stroke(new Stroke(Color.WHITE, 0.0))
                .textStyle(styles.labelStyle(8.3))
                .textAnchor(Anchor.topLeft())
                .lineSpacing(layout.tableLineSpacing())
                .build();

        return new TemplateTableSpec(
                "ProposalHeaderMetaTable",
                List.of(TableColumnLayout.fixed(width * 0.56), TableColumnLayout.fixed(width * 0.44)),
                List.of(
                        List.of(
                                TableCellContent.text(valueOrFallback(data.preparedDate(), "TBD")).withStyle(valueStyle),
                                TableCellContent.text("Prepared").withStyle(labelStyle)),
                        List.of(
                                TableCellContent.text(valueOrFallback(data.validUntil(), "TBD")).withStyle(valueStyle),
                                TableCellContent.text("Valid Until").withStyle(labelStyle)),
                        List.of(
                                TableCellContent.text(valueOrFallback(data.proposalNumber(), "Draft")).withStyle(valueStyle),
                                TableCellContent.text("Reference").withStyle(labelStyle))),
                baseStyle,
                Map.of(),
                Map.of(),
                width,
                Padding.zero(),
                Margin.zero());
    }

    private TemplateRowSpec partiesRow(double pageWidth, ProposalData data) {
        double columnWidth = sceneLayout.twoColumnWidth(pageWidth);
        return TemplateRowSpec.weighted(
                "ProposalParties",
                List.of(
                        TemplateColumnSpec.of("ProposalPreparedBy", partyBlocks("PREPARED BY", data.sender()), 0.0),
                        TemplateColumnSpec.of("ProposalPreparedFor", partyBlocks("PREPARED FOR", data.recipient()), 0.0)),
                List.of(columnWidth, columnWidth),
                sceneLayout.columnGap());
    }

    private List<TemplateModuleBlock> partyBlocks(String label, ProposalParty party) {
        ProposalParty safeParty = party == null ? new ProposalParty("", List.of(), "", "", "") : party;
        List<TemplateModuleBlock> blocks = new ArrayList<>();
        String prefix = "Proposal" + label.replace(" ", "");
        blocks.add(TemplateModuleBlock.paragraph(p(prefix + "Heading", label, styles.labelStyle(LABEL_SIZE), Margin.zero())));
        if (!safeParty.name().isBlank()) {
            blocks.add(TemplateModuleBlock.paragraph(p(prefix + "Name", safeParty.name(), styles.bodyBoldStyle(BODY_SIZE), layout.top(7.0))));
        }
        for (String line : safeParty.addressLines()) {
            blocks.add(TemplateModuleBlock.paragraph(p(prefix + "Address", line, styles.bodyStyle(BODY_SIZE), layout.top(1.0))));
        }
        addPartyContact(blocks, prefix, "Email: ", safeParty.email());
        addPartyContact(blocks, prefix, "Phone: ", safeParty.phone());
        addPartyContact(blocks, prefix, "Web: ", safeParty.website());
        return List.copyOf(blocks);
    }

    private void addPartyContact(List<TemplateModuleBlock> blocks, String prefix, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        blocks.add(TemplateModuleBlock.paragraph(p(
                prefix + label.replace(": ", ""),
                label + value,
                styles.bodyStyle(BODY_SIZE),
                layout.top(1.0))));
    }

    private TemplateRowSpec sectionsRow(double pageWidth, List<ProposalSection> sections) {
        double columnWidth = sceneLayout.twoColumnWidth(pageWidth);
        List<TemplateColumnSpec> columns = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            ProposalSection section = sections.get(index);
            columns.add(TemplateColumnSpec.of(
                    "ProposalSection" + index,
                    sectionBlocks(index, columnWidth, section),
                    0.0));
            weights.add(columnWidth);
        }
        return new TemplateRowSpec(
                "ProposalSections",
                columns,
                weights,
                sceneLayout.columnGap(),
                Padding.zero(),
                layout.top(5.0));
    }

    private List<TemplateModuleBlock> sectionBlocks(int index, double width, ProposalSection section) {
        String title = valueOrFallback(section.title(), "SECTION");
        return List.of(
                TemplateModuleBlock.paragraph(p("ProposalSection" + index + "Heading", title, styles.labelStyle(LABEL_SIZE), Margin.zero())),
                TemplateModuleBlock.divider(TemplateSceneSupport.divider(
                        "ProposalSection" + index + "Rule",
                        sceneLayout.boundedRuleWidth(width, sceneLayout.proposalSectionRuleWidth()),
                        sceneLayout.sectionDividerThickness(),
                        styles.accentColor(),
                        layout.top(2.0))),
                TemplateModuleBlock.paragraph(TemplateSceneSupport.paragraph(
                        "ProposalSection_" + index,
                        sectionText(section),
                        styles.bodyStyle(9.6),
                        TextAlign.LEFT,
                        1.0,
                        Padding.zero(),
                        layout.top(7.0))));
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
                layout.top(PROPOSAL_BODY_GAP));
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
                layout.top(PROPOSAL_BODY_GAP));
    }

    private List<String> partyLines(String label, ProposalParty party) {
        ProposalParty safeParty = party == null ? new ProposalParty("", List.of(), "", "", "") : party;
        List<String> lines = new ArrayList<>();
        lines.add(label);
        lines.add("");
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
        lines.add("Proposal #" + valueOrFallback(data.proposalNumber(), "Draft"));
        lines.add("Prepared: " + valueOrFallback(data.preparedDate(), "TBD"));
        lines.add("Valid Until: " + valueOrFallback(data.validUntil(), "TBD"));
        if (data.sender() != null) {
            lines.add("");
            lines.addAll(partyLines("PREPARED BY", data.sender()));
        }
        return lines;
    }

    private static String sectionText(ProposalSection section) {
        return String.join("\n", section.paragraphs().isEmpty()
                ? List.of("Content is intentionally left blank.")
                : section.paragraphs());
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
                layout.top(PROPOSAL_BODY_GAP))));
        blocks.addAll(List.of(bodyBlocks));
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
