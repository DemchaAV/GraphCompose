package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;
import com.demcha.compose.document.templates.data.proposal.ProposalGoals;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalSummaryBlock;
import com.demcha.compose.document.templates.data.proposal.ProposalTitleLines;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BAND_TOP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_DIVIDER_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_ICON;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_LINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_NOTE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_VALUE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_VALUE_LINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FACT_VALUE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GAP_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GAP_SECTION;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GLANCE_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GLANCE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GOAL_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GOAL_ICON;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GOAL_TEXT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GOAL_TEXT_LINES;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.LATO_LINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.META;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.META_SEPARATOR;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_PADDING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.RULE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.RULE_QUIET;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_BADGE_H;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_BADGE_W;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_BODY;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_NUMBER;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_ROW_PAD;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_TITLE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SCOPE_TITLE_SPLIT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SUMMARY_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SUMMARY_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SUMMARY_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TABLE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TITLE_ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TITLE_INK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TITLE_LAST_LINE_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TITLE_PITCH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TITLE_RULE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineWidgets.sectionHeading;

/**
 * Page one of the Northline proposal: the title block, the summary band
 * with the glance card, the goal cells, and the numbered scope list.
 *
 * <p>Layout notes carried over from the ported template: the title lines
 * stack as anchored children of one container because the reference sets
 * the title at a pitch tighter than the font's own line height, which text
 * leading (additive, non-negative) cannot reach; the goal cells flatten
 * every icon/text pair into one row because a row is refused inside a row
 * cell; the scope list is rows plus hairlines rather than a table because
 * the reference has horizontal separators and no vertical rules.</p>
 */
final class NorthlinePageOne {

    private NorthlinePageOne() {
    }

    static void compose(PageFlowBuilder page, StructuredProposalData data) {
        renderTitleBlock(page, data.title());
        renderTitleMeta(page, data);
        renderSummaryBand(page, data);
        renderGoalCells(page, data.goals());
        renderScopeOfWork(page, data.scope());
    }

    /** Accent rule, then the three title lines — ink lead, accent remainder. */
    private static void renderTitleBlock(PageFlowBuilder page, ProposalTitleLines title) {
        page.addSection("TitleBlock", block -> {
            block.margin(BAND_TOP, 0f, 0f, 0f);
            block.spacing(0);
            block.addLine(line -> line
                    .horizontal(TITLE_RULE_WIDTH)
                    .thickness(2.6)
                    .color(ACCENT)
                    .margin(DocumentInsets.bottom(2)));
            block.add(renderTitleLines(title));
        });
    }

    /**
     * The three title lines, stacked on the reference's own pitch: each
     * line is a child of one container placed {@code TITLE_PITCH} apart —
     * this stacks what the data already declares as separate lines.
     */
    private static DocumentNode renderTitleLines(ProposalTitleLines title) {
        return new ShapeContainerBuilder()
                .name("TitleLines")
                .rectangle(CONTENT_WIDTH, 2 * TITLE_PITCH + TITLE_LAST_LINE_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(titleLine("TitleLead", title.lead(), TITLE_INK),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(titleLine("TitleLine1", title.second(), TITLE_ACCENT),
                        0, TITLE_PITCH, LayerAlign.TOP_LEFT)
                .position(titleLine("TitleLine2", title.third(), TITLE_ACCENT),
                        0, 2 * TITLE_PITCH, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode titleLine(String name, String text, DocumentTextStyle style) {
        return new ParagraphBuilder()
                .name(name).text(text).textStyle(style).build();
    }

    /** Prepared-for, prepared-by and date, joined by a quieter separator. */
    private static void renderTitleMeta(PageFlowBuilder page, StructuredProposalData data) {
        page.addSection("TitleMeta", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.addParagraph(p -> p
                    .name("MetaLine")
                    .inlineText(data.meta().preparedFor(), META)
                    .inlineText("   |   ", META_SEPARATOR)
                    .inlineText(data.meta().preparedBy(), META)
                    .inlineText("   |   ", META_SEPARATOR)
                    .inlineText(data.meta().date(), META));
        });
    }

    /** The two-column band: summary column left, glance card right. */
    private static void renderSummaryBand(PageFlowBuilder page, StructuredProposalData data) {
        page.addRow("SummaryBand", row -> {
            row.margin(DocumentInsets.top(BAND_TOP));
            row.verticalAlign(RowVerticalAlign.TOP)
                    .gap(SUMMARY_GAP)
                    .weights(SUMMARY_WEIGHT, GLANCE_WEIGHT);
            row.addSection("SummaryColumn", column -> {
                renderExecutiveSummary(column, data.executiveSummary());
                // The goals heading sits in this column, level with the
                // bottom of the glance card — it belongs to the band.
                column.add(sectionHeading(
                        data.goals().heading(), data.goals().icon(), SUMMARY_WIDTH));
            });
            row.addSection("GlanceColumn", column -> renderGlancePanel(column, data.glance()));
        });
    }

    /** Heading plus the summary paragraphs. */
    private static void renderExecutiveSummary(SectionBuilder column,
                                               ProposalSummaryBlock summary) {
        column.spacing(0);
        column.add(sectionHeading(summary.heading(), summary.icon(), SUMMARY_WIDTH));
        List<String> paragraphs = summary.paragraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            String text = paragraphs.get(i);
            float top = i == 0 ? (float) GAP_HEADING : (float) GAP_SECTION * 0.6f;
            column.addParagraph(p -> p
                    .text(text)
                    .textStyle(BODY)
                    .lineSpacing(BODY_LEADING)
                    .margin(top, 0f, 0f, 0f));
        }
        // Clears the last paragraph before the goals heading.
        column.addSpacer(spacer -> spacer.height(GAP_SECTION));
    }

    /**
     * The quiet card of project facts. {@code softPanel} colours the
     * section that already owns the facts, so the card is their parent
     * rather than a rectangle drawn behind them.
     */
    private static void renderGlancePanel(SectionBuilder column, ProposalGlance glance) {
        double inner = GLANCE_WIDTH - 2 * PANEL_PADDING;
        column.addSection("GlancePanel", panel -> {
            panel.spacing(0).softPanel(PANEL_BACKGROUND, PANEL_RADIUS, PANEL_PADDING);
            panel.addParagraph(p -> p
                    .text(glance.heading())
                    .textStyle(PANEL_HEADING)
                    .margin(0f, 0f, (float) GAP_HEADING, 0f));
            List<ProposalGlance.Fact> facts = glance.facts();
            for (int i = 0; i < facts.size(); i++) {
                panel.add(renderGlanceFact(facts.get(i), inner));
                if (i < facts.size() - 1) {
                    panel.addLine(line -> line
                            .fill()
                            .thickness(HAIRLINE)
                            .color(RULE_QUIET)
                            .margin(DocumentInsets.symmetric(FACT_DIVIDER_GAP, 0)));
                }
            }
        });
    }

    /**
     * One fact: the icon anchored at the left of a label-over-value stack.
     * The container's height derives from the number of text lines the fact
     * actually has, so the optional note line makes its own room.
     */
    private static DocumentNode renderGlanceFact(ProposalGlance.Fact fact, double width) {
        boolean hasNote = !fact.note().isBlank();
        double height = LABEL_SIZE * FACT_LINE + FACT_VALUE_SIZE * FACT_VALUE_LINE
                + (hasNote ? (LABEL_SIZE + 0.5) * FACT_LINE : 0);

        SectionBuilder text = new SectionBuilder();
        text.name("FactText");
        text.spacing(0);
        text.addParagraph(p -> p.text(fact.label()).textStyle(FACT_LABEL));
        text.addParagraph(p -> p.text(fact.value()).textStyle(FACT_VALUE));
        if (hasNote) {
            text.addParagraph(p -> p.text(fact.note()).textStyle(FACT_NOTE));
        }

        ShapeContainerBuilder container = new ShapeContainerBuilder()
                .name("GlanceFact")
                .rectangle(width, height)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE);
        if (!fact.icon().isBlank()) {
            container.position(NorthlineWidgets.icon(fact.icon(), FACT_ICON),
                    0, 0, LayerAlign.CENTER_LEFT);
        }
        return container
                .position(text.build(), FACT_ICON + FACT_GAP, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /**
     * The goal cells, flattened deliberately: the icon/text pairs and the
     * separators are all direct children of one row, and the column widths
     * carry the structure. Skipped entirely when the data has no goals —
     * the flattened column spec cannot describe zero cells.
     */
    private static void renderGoalCells(PageFlowBuilder page, ProposalGoals goals) {
        List<ProposalGoals.Goal> items = goals.items();
        if (items.isEmpty()) {
            return;
        }
        // Three goal-text lines set the separator height; the pitch is the
        // font line plus the additive leading.
        double separatorHeight = GOAL_TEXT_LINES * (TABLE_SIZE * LATO_LINE + BODY_LEADING);
        page.addRow("GoalCells", row -> {
            row.margin(DocumentInsets.top(BAND_TOP));
            row.verticalAlign(RowVerticalAlign.TOP).gap(GOAL_GAP);
            row.columns(goalColumns(items.size()));
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    row.addLine(line -> line
                            .vertical(separatorHeight)
                            .thickness(HAIRLINE)
                            .color(RULE));
                }
                ProposalGoals.Goal goal = items.get(i);
                if (goal.icon().isBlank()) {
                    // Keep the child-per-column mapping of the flattened row.
                    row.addSpacer(GOAL_ICON);
                } else {
                    row.addImage(image -> NorthlineWidgets.configureIcon(
                            image, goal.icon(), GOAL_ICON));
                }
                row.addParagraph(p -> p
                        .text(goal.text())
                        .textStyle(GOAL_TEXT)
                        .lineSpacing(BODY_LEADING));
            }
        });
    }

    /** Column spec for the flattened goal row. */
    private static DocumentRowColumn[] goalColumns(int goalCount) {
        DocumentRowColumn[] columns = new DocumentRowColumn[goalCount * 2 + (goalCount - 1)];
        int at = 0;
        for (int i = 0; i < goalCount; i++) {
            if (i > 0) {
                columns[at++] = DocumentRowColumn.fixed(HAIRLINE);
            }
            columns[at++] = DocumentRowColumn.fixed(GOAL_ICON);
            columns[at++] = DocumentRowColumn.weight(1);
        }
        return columns;
    }

    /** The numbered scope list: rows plus hairlines at the flow's top level. */
    private static void renderScopeOfWork(PageFlowBuilder page, ProposalScope scope) {
        double titleWidth = CONTENT_WIDTH * SCOPE_TITLE_SPLIT - SCOPE_BADGE_W - 2 * SCOPE_GAP;
        page.addSection("ScopeOfWork", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(scope.heading(), scope.icon(), CONTENT_WIDTH));
            section.addLine(line -> line
                    .fill()
                    .thickness(HAIRLINE)
                    .color(RULE)
                    .margin(DocumentInsets.top(GAP_HEADING)));
            List<ProposalScope.Item> items = scope.items();
            for (int i = 0; i < items.size(); i++) {
                ProposalScope.Item item = items.get(i);
                section.addRow("ScopeRow" + item.number(), row -> {
                    row.verticalAlign(RowVerticalAlign.TOP)
                            .gap(SCOPE_GAP)
                            .padding(DocumentInsets.symmetric(SCOPE_ROW_PAD, 0))
                            .columns(DocumentRowColumn.fixed(SCOPE_BADGE_W),
                                    DocumentRowColumn.fixed(titleWidth),
                                    DocumentRowColumn.weight(1));
                    row.add(renderScopeBadge(item.number()));
                    row.addParagraph(p -> p.text(item.title()).textStyle(SCOPE_TITLE));
                    row.addParagraph(p -> p
                            .text(item.description())
                            .textStyle(SCOPE_BODY)
                            .lineSpacing(BODY_LEADING));
                });
                if (i < items.size() - 1) {
                    section.addLine(line -> line.fill().thickness(HAIRLINE).color(RULE));
                }
            }
        });
    }

    /** The teal step badge and the number it owns. */
    private static DocumentNode renderScopeBadge(String number) {
        return new ShapeContainerBuilder()
                .name("ScopeBadge")
                .roundedRect(SCOPE_BADGE_W, SCOPE_BADGE_H, 2.0)
                .fillColor(ACCENT)
                .center(NorthlineWidgets.paragraph("ScopeNumber", number, SCOPE_NUMBER,
                        TextAlign.CENTER))
                .build();
    }
}
