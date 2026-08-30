package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;
import com.demcha.compose.document.templates.data.proposal.ProposalGoals;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalSummaryBlock;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BAND_TOP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_DIVIDER_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_ICON;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_NOTE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_VALUE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FACT_VALUE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_AFTER_RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_BEFORE_GOALS_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_META;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_PARAGRAPH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_TITLE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GLANCE_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GLANCE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GOAL_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GOAL_ICON;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GOAL_TEXT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GOAL_TEXT_LINES;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HEADING_CAP_LEAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.META;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.META_SEPARATOR;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PANEL_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PANEL_PADDING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.RULE_QUIET;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_BODY;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_DESC_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_NUMBER;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_NUMBER_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_ROW_PAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_TITLE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SCOPE_TITLE_SPLIT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SUMMARY_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SUMMARY_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TABLE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TITLE_ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TITLE_INK;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TITLE_PITCH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.lineHeight;
import static com.demcha.compose.document.templates.proposal.presets.EditorialWidgets.sectionHeading;
import static com.demcha.compose.document.templates.proposal.presets.EditorialWidgets.stackedLines;

/**
 * Page one of the Editorial Proposal: the stacked title, the meta line, the
 * summary beside the fact card, the goal cells, and the numbered scope list.
 */
final class EditorialPageOne {

    private EditorialPageOne() {
    }

    static void compose(PageFlowBuilder page, StructuredProposalData data) {
        renderTitleBlock(page, data);
        renderTitleMeta(page, data);
        renderSummaryBand(page, data);
        renderGoalCells(page, data.goals());
        renderScopeOfWork(page, data.scope());
    }

    /** The three title lines, stacked on the reference's own pitch. */
    private static void renderTitleBlock(PageFlowBuilder page, StructuredProposalData data) {
        page.addSection("TitleBlock", block -> {
            block.margin(GAP_TITLE, 0f, 0f, 0f);
            block.spacing(0);
            block.add(stackedLines("TitleLines", CONTENT_WIDTH, TITLE_PITCH, TITLE_SIZE,
                    List.of(data.title().lead(), data.title().second(), data.title().third()),
                    List.of(TITLE_INK, TITLE_ACCENT, TITLE_ACCENT)));
        });
    }

    /** Prepared-for, prepared-by and date, split by accent pipes. */
    private static void renderTitleMeta(PageFlowBuilder page, StructuredProposalData data) {
        page.addSection("TitleMeta", section -> {
            section.margin(GAP_META, 0f, 0f, 0f);
            section.addParagraph(p -> p
                    .name("MetaLine")
                    .inlineText(data.meta().preparedFor(), META)
                    .inlineText("   |   ", META_SEPARATOR)
                    .inlineText(data.meta().preparedBy(), META)
                    .inlineText("   |   ", META_SEPARATOR)
                    .inlineText(data.meta().date(), META));
        });
    }

    /** The two-column band: summary and the goals heading left, fact card right. */
    private static void renderSummaryBand(PageFlowBuilder page, StructuredProposalData data) {
        page.addRow("SummaryBand", row -> {
            row.margin(DocumentInsets.top(BAND_TOP));
            row.verticalAlign(RowVerticalAlign.TOP)
                    .gap(SUMMARY_GAP)
                    .weights(SUMMARY_WEIGHT, GLANCE_WEIGHT);
            row.addSection("SummaryColumn", column -> {
                renderExecutiveSummary(column, data.executiveSummary());
                column.addSpacer(spacer -> spacer.height(GAP_BEFORE_GOALS_HEADING));
                column.add(sectionHeading(data.goals().heading()));
            });
            row.addSection("GlanceColumn", column -> {
                // The card's top edge lines up with the heading's CAP top, not
                // with its line box, which starts HEADING_CAP_LEAD of the type
                // size higher. Both children start at the band's top, so the
                // card is padded down by the difference — stated once here
                // rather than baked into BAND_TOP, which every band follows.
                column.padding((float) (HEADING_SIZE * HEADING_CAP_LEAD - 1.6), 0f, 0f, 0f);
                renderGlancePanel(column, data.glance());
            });
        });
    }

    private static void renderExecutiveSummary(SectionBuilder column,
                                               ProposalSummaryBlock summary) {
        column.spacing(0);
        column.add(sectionHeading(summary.heading()));
        List<String> paragraphs = summary.paragraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            String text = paragraphs.get(i);
            float top = i == 0 ? GAP_AFTER_RULE : GAP_PARAGRAPH;
            column.addParagraph(p -> p
                    .text(text)
                    .textStyle(BODY)
                    .lineSpacing(BODY_LEADING)
                    .margin(top, 0f, 0f, 0f));
        }
    }

    /**
     * The fact card. Unlike its teal sibling the card is untitled — the
     * data's glance heading is not drawn — and its facts are divided by
     * quiet rules inside a stroked panel.
     */
    private static void renderGlancePanel(SectionBuilder column, ProposalGlance glance) {
        List<ProposalGlance.Fact> facts = glance.facts();
        double inner = GLANCE_WIDTH - 2 * PANEL_PADDING;
        column.addSection("GlancePanel", panel -> {
            panel.spacing(0).softPanel(PANEL_BACKGROUND, PANEL_RADIUS, PANEL_PADDING,
                    DocumentStroke.of(RULE_QUIET, HAIRLINE));
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
     * One fact: the icon anchored left of a label-over-value stack. The
     * container's height derives from the lines the fact actually has, so an
     * optional note makes its own room.
     */
    private static DocumentNode renderGlanceFact(ProposalGlance.Fact fact, double width) {
        boolean hasNote = !fact.note().isBlank();
        double height = lineHeight(LABEL_SIZE)
                + lineHeight(FACT_VALUE_SIZE)
                + (hasNote ? lineHeight(LABEL_SIZE + 0.5) : 0);

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
            container.position(EditorialIcons.icon(fact.icon(), FACT_ICON),
                    0, 0, LayerAlign.CENTER_LEFT);
        }
        return container
                .position(text.build(), FACT_ICON + FACT_GAP, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /**
     * The goal cells, flattened into one row: the engine refuses a row inside
     * a row cell, so the column widths carry the structure. A cell opens with
     * the goal's own icon token, falling back to the band's — which is how
     * this design marks every cell alike, since its goals name no token.
     */
    private static void renderGoalCells(PageFlowBuilder page, ProposalGoals goals) {
        List<ProposalGoals.Goal> items = goals.items();
        if (items.isEmpty()) {
            return;
        }
        double dividerHeight = GOAL_TEXT_LINES * lineHeight(TABLE_SIZE);
        page.addRow("GoalCells", row -> {
            row.margin(DocumentInsets.top(BAND_TOP));
            row.verticalAlign(RowVerticalAlign.TOP).gap(GOAL_GAP);
            row.columns(goalColumns(items.size()));
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    row.addLine(line -> line
                            .vertical(dividerHeight)
                            .thickness(HAIRLINE)
                            .color(RULE));
                }
                ProposalGoals.Goal goal = items.get(i);
                // The goal's own token when it carries one; the band's token
                // otherwise, which is how this design marks every cell the
                // same. A goal that names neither keeps the column mapping of
                // the flattened row with a spacer.
                String mark = goal.icon().isBlank() ? goals.icon() : goal.icon();
                if (mark.isBlank()) {
                    row.addSpacer(GOAL_ICON);
                } else {
                    row.add(EditorialIcons.icon(mark, GOAL_ICON));
                }
                row.addParagraph(p -> p
                        .text(goal.text())
                        .textStyle(GOAL_TEXT)
                        .lineSpacing(BODY_LEADING));
            }
        });
    }

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

    /**
     * The numbered scope list: rows plus hairlines. The ordinal is plain
     * accent text — the teal sibling encloses the same number in a pill.
     */
    private static void renderScopeOfWork(PageFlowBuilder page, ProposalScope scope) {
        double titleWidth =
                CONTENT_WIDTH * SCOPE_TITLE_SPLIT - SCOPE_NUMBER_WIDTH - 2 * SCOPE_GAP;
        page.addSection("ScopeOfWork", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(scope.heading()));
            section.addLine(line -> line
                    .fill()
                    .thickness(HAIRLINE)
                    .color(RULE)
                    .margin(DocumentInsets.top(GAP_AFTER_RULE)));
            List<ProposalScope.Item> items = scope.items();
            for (int i = 0; i < items.size(); i++) {
                ProposalScope.Item item = items.get(i);
                section.addRow("ScopeRow" + item.number(), row -> {
                    row.verticalAlign(RowVerticalAlign.TOP)
                            .gap(SCOPE_GAP)
                            .padding(DocumentInsets.symmetric(SCOPE_ROW_PAD, 0))
                            .columns(DocumentRowColumn.fixed(SCOPE_NUMBER_WIDTH),
                                    DocumentRowColumn.fixed(titleWidth),
                                    DocumentRowColumn.fixed(CONTENT_WIDTH * SCOPE_DESC_WEIGHT));
                    row.addParagraph(p -> p.text(item.number()).textStyle(SCOPE_NUMBER));
                    row.addParagraph(p -> p.text(item.title()).textStyle(SCOPE_TITLE));
                    row.addParagraph(p -> p
                            .text(item.description())
                            .textStyle(SCOPE_BODY)
                            .lineSpacing(SCOPE_LEADING));
                });
                if (i < items.size() - 1) {
                    section.addLine(line -> line.fill().thickness(HAIRLINE).color(RULE));
                }
            }
        });
    }
}
