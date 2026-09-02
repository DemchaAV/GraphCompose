package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.data.proposal.ProposalInvestment;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalTermsBlock;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.columnDivider;
import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.layeredRow;
import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.rule;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.DIVIDER_LOWER_H_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.DIVIDER_LOWER_TOP_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HALF;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_CELL_PAD_R;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_FIRST_ROW_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_HEADING_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_ROW_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_RULE_OFFSET_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INVEST_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LOWER_GAP_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LOWER_LEFT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LOWER_RIGHT_INSET;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LOWER_RIGHT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.NOTES_FIRST_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.NOTES_HEADING_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.NOTES_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.NOTE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.OVERVIEW_HEADING_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.OVERVIEW_INTRO_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.OVERVIEW_INTRO_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.OVERVIEW_MEASURE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.OVERVIEW_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_THIN;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_CIRCLE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_CIRCLE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_COL_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_CONNECTOR_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_FIRST_CIRCLE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_NUM_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_SUB_OFFSET_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_SUB_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_TITLE_OFFSET_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.STEP_TITLE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TINT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_CARD_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_CARD_H_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_CARD_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_FIGURE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_FIGURE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_PAD_L;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TOTAL_PAD_R;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.bold;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.boxBottomPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.plain;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.px;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.toPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.topBearing;

/**
 * The lower half of the sheet: the numbered plan at the left, the money at the
 * right.
 */
final class IndigoLower {

    /** Every cell's stroke is white and weightless; the rules are lines inside them. */
    private static final DocumentTableStyle CELL_STYLE = DocumentTableStyle.builder()
            .padding(new DocumentInsets(0, INVEST_CELL_PAD_R, 0, 0))
            .stroke(DocumentStroke.of(INK, 0))
            .build();

    private IndigoLower() {
    }

    /**
     * The lower row.
     *
     * @param page       the page flow
     * @param scope      the plan: a heading, an opening paragraph and numbered steps
     * @param investment the money: a heading, its rows and the total
     * @param terms      the notes under the total
     * @param flow       the sheet's cursor
     */
    static void render(PageFlowBuilder page, ProposalScope scope, ProposalInvestment investment,
                       ProposalTermsBlock terms, IndigoFlow flow) {
        double rowTopPx = OVERVIEW_HEADING_CAP - toPx(topBearing(LABEL_SIZE, true));
        // How far the row reaches is the last step's, and a plan with no steps
        // is measured as one: a count of nothing would put the row's foot above
        // where its first step would have started.
        int steps = Math.max(1, scope.items().size());
        double bottomPx = boxBottomPx(STEP_FIRST_CIRCLE_AT + (steps - 1) * STEP_PITCH_PX
                + STEP_SUB_OFFSET_PX, STEP_SUB_SIZE, false);
        double top = flow.boxAt(rowTopPx, bottomPx - rowTopPx);
        page.addRow("LowerRow", row -> {
            row.spacing(0)
                    .margin(new DocumentInsets(top, 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(LOWER_LEFT_W),
                            DocumentRowColumn.fixed(LOWER_GAP_W),
                            DocumentRowColumn.weight(1));
            row.addSection("LowerLeft", left -> {
                left.spacing(0);
                renderPlan(left, scope, new IndigoFlow(rowTopPx));
            });
            columnDivider(row, "ColumnDividerLower",
                    DIVIDER_LOWER_TOP_PX - rowTopPx, DIVIDER_LOWER_H_PX);
            row.addSection("LowerRight", right -> {
                right.spacing(0).padding(new DocumentInsets(0, LOWER_RIGHT_INSET, 0, 0));
                IndigoFlow cell = new IndigoFlow(rowTopPx);
                renderInvestment(right, investment, cell);
                renderTotal(right, investment, cell);
                renderNotes(right, terms, cell);
            });
        });
    }

    private static void renderPlan(SectionBuilder left, ProposalScope scope, IndigoFlow cell) {
        if (!scope.heading().isBlank()) {
            left.addParagraph(p -> p
                    .name("PlanHeading")
                    .text(scope.heading())
                    .textStyle(bold(LABEL_SIZE, ACCENT))
                    .margin(new DocumentInsets(
                            cell.capAt(OVERVIEW_HEADING_CAP, LABEL_SIZE, true), 0, 0, 0)));
        }
        if (!scope.intro().isBlank()) {
            double measureInset = Math.max(0, LOWER_LEFT_W - px(OVERVIEW_MEASURE_PX));
            double gap = cell.capAt(OVERVIEW_INTRO_CAP, OVERVIEW_SIZE, false);
            left.addParagraph(p -> p
                    .name("PlanIntro")
                    .text(scope.intro())
                    .textStyle(plain(OVERVIEW_SIZE, BODY))
                    .lineSpacing(px(OVERVIEW_INTRO_PITCH_PX) - OVERVIEW_SIZE)
                    .margin(new DocumentInsets(gap, measureInset, 0, 0)));
            // Three lines at this measure, which the map states rather than the
            // engine reporting afterwards.
            cell.advanceTo(OVERVIEW_INTRO_CAP + 2 * OVERVIEW_INTRO_PITCH_PX
                    + toPx(OVERVIEW_SIZE));
        }
        renderSteps(left, scope.items(), cell);
    }

    private static void renderSteps(SectionBuilder left, List<ProposalScope.Item> items,
                                    IndigoFlow cell) {
        for (int i = 0; i < items.size(); i++) {
            ProposalScope.Item item = items.get(i);
            int index = i;
            boolean last = i == items.size() - 1;
            double circleAt = STEP_FIRST_CIRCLE_AT + i * STEP_PITCH_PX;
            double top = cell.boxAt(circleAt, last ? STEP_CIRCLE_PX : STEP_PITCH_PX);
            layeredRow(left, "Step_" + index, row -> {
                row.spacing(0)
                        .margin(new DocumentInsets(top, 0, 0, 0))
                        .columns(DocumentRowColumn.fixed(STEP_COL_W),
                                DocumentRowColumn.weight(1));
                row.addSection("StepMark_" + index, mark -> {
                    mark.spacing(0);
                    mark.add(numberedDisc(index, item.number()));
                    if (!last) {
                        // The tick that joins this circle to the next. It is drawn
                        // per step rather than as one rail behind them all,
                        // because the design's gap is the shape's, not a rule's.
                        double gap = px(STEP_PITCH_PX - STEP_CIRCLE_PX);
                        double indent = (STEP_CIRCLE - STEP_CONNECTOR_W) * HALF;
                        mark.addLine(line -> line
                                .name("StepConnector_" + index)
                                .vertical(gap)
                                .thickness(STEP_CONNECTOR_W)
                                .color(TINT)
                                .margin(new DocumentInsets(0, 0, 0, indent)));
                    }
                });
                row.addSection("StepText_" + index, text -> {
                    text.spacing(0);
                    IndigoFlow stack = new IndigoFlow(circleAt);
                    text.addParagraph(p -> p
                            .name("StepTitle_" + index)
                            .text(item.title())
                            .textStyle(bold(STEP_TITLE_SIZE, INK))
                            .margin(new DocumentInsets(stack.capAt(
                                    circleAt + STEP_TITLE_OFFSET_PX, STEP_TITLE_SIZE, true),
                                    0, 0, 0)));
                    text.addParagraph(p -> p
                            .name("StepSubtitle_" + index)
                            .text(item.description())
                            .textStyle(plain(STEP_SUB_SIZE, BODY))
                            .margin(new DocumentInsets(stack.capAt(
                                    circleAt + STEP_SUB_OFFSET_PX, STEP_SUB_SIZE, false),
                                    0, 0, 0)));
                });
            });
        }
    }

    /**
     * A step's numbered disc.
     *
     * <p>The number is the document's: a plan that continues one from elsewhere
     * says so, and one that states none is numbered by its position.</p>
     */
    private static DocumentNode numberedDisc(int index, String stated) {
        String number = stated.isBlank() ? String.format("%02d", index + 1) : stated;
        return new ShapeContainerBuilder()
                .name("StepCircle_" + index)
                .circle(STEP_CIRCLE)
                .fillColor(TINT)
                .center(new ParagraphBuilder()
                        .name("StepNumber_" + index)
                        .text(number)
                        .textStyle(bold(STEP_NUM_SIZE, ACCENT))
                        .align(TextAlign.CENTER)
                        .build())
                .build();
    }

    /**
     * The money table.
     *
     * <p>The heading travels with it. {@code keepWithNext} is a section's and a
     * line's property and not a paragraph's, so rather than ask the label to
     * hold on to what follows it, the label and the table are one kept-together
     * section — a heading left alone at the foot of a page is what that
     * prevents.</p>
     */
    private static void renderInvestment(SectionBuilder right, ProposalInvestment investment,
                                         IndigoFlow cell) {
        List<ProposalInvestment.Row> rows = investment.rows();
        double headingTopPx = INVEST_HEADING_CAP - toPx(topBearing(LABEL_SIZE, true));
        double tableTopPx = INVEST_FIRST_ROW_CAP - toPx(topBearing(INVEST_SIZE, false));
        double tableBottomPx = boxBottomPx(INVEST_FIRST_ROW_CAP
                + Math.max(0, rows.size() - 1) * INVEST_ROW_PITCH_PX, INVEST_SIZE, false);
        double top = cell.boxAt(headingTopPx, tableBottomPx - headingTopPx);
        right.addSection("Investment", group -> {
            group.spacing(0)
                    .keepTogether()
                    .margin(new DocumentInsets(top, 0, 0, 0));
            IndigoFlow inner = new IndigoFlow(headingTopPx);
            if (!investment.heading().isBlank()) {
                group.addParagraph(p -> p
                        .name("InvestmentHeading")
                        .text(investment.heading())
                        .textStyle(bold(LABEL_SIZE, ACCENT))
                        .margin(new DocumentInsets(
                                inner.capAt(INVEST_HEADING_CAP, LABEL_SIZE, true), 0, 0, 0)));
            }
            if (rows.isEmpty()) {
                return;
            }
            double tableTop = inner.boxAt(tableTopPx, tableBottomPx - tableTopPx);
            group.addTable(table -> {
                table.name("InvestmentTable")
                        .width(LOWER_RIGHT_W)
                        .columns(DocumentTableColumn.fixed(LOWER_RIGHT_W))
                        // A cell's stroke draws all four edges and there is no
                        // per-edge control, so the default put a grid box round
                        // every row. The rules the design does draw are lines
                        // inside the cells.
                        .defaultCellStyle(CELL_STYLE)
                        .margin(new DocumentInsets(tableTop, 0, 0, 0));
                for (int i = 0; i < rows.size(); i++) {
                    table.rowCells(DocumentTableCell
                            .node(investmentRow(rows.get(i), i, i == rows.size() - 1)));
                }
            });
        });
    }

    private static DocumentNode investmentRow(ProposalInvestment.Row entry, int index,
                                              boolean last) {
        SectionBuilder cell = new SectionBuilder();
        cell.name("InvestmentCell_" + index).spacing(0);
        cell.addRow("InvestmentRow_" + index, row -> {
            row.spacing(0).weights(HALF, HALF);
            row.addParagraph(p -> p
                    .name("InvestmentLabel_" + index)
                    .text(entry.label())
                    .textStyle(plain(INVEST_SIZE, INK)));
            row.addParagraph(p -> p
                    .name("InvestmentAmount_" + index)
                    .text(entry.amount())
                    .textStyle(plain(INVEST_SIZE, INK))
                    .align(TextAlign.RIGHT));
        });
        if (!last) {
            double gap = px(INVEST_RULE_OFFSET_PX) - INVEST_SIZE
                    + topBearing(INVEST_SIZE, false);
            cell.addLine(line -> rule(line, "InvestmentRule_" + index, LOWER_RIGHT_W, RULE_SOFT)
                    .margin(new DocumentInsets(Math.max(0, gap), 0, 0, 0)));
            cell.addSpacer(spacer -> spacer.height(Math.max(0,
                    px(INVEST_ROW_PITCH_PX) - INVEST_SIZE - Math.max(0, gap) - RULE_THIN)));
        }
        return cell.build();
    }

    /**
     * The total's card.
     *
     * <p>The design's card is a fixed band, but its content is only the one row:
     * without a bottom pad it closes early, and everything under it rides up by
     * that much. The pad is the difference, computed rather than tuned.</p>
     */
    private static void renderTotal(SectionBuilder right, ProposalInvestment investment,
                                    IndigoFlow cell) {
        if (investment.totalLabel().isBlank() && investment.totalAmount().isBlank()) {
            return;
        }
        double top = cell.boxAt(TOTAL_CARD_AT, TOTAL_CARD_H_PX);
        double figureBoxTopPx = TOTAL_FIGURE_CAP - toPx(topBearing(TOTAL_FIGURE_SIZE, true));
        double contentBottomPx = figureBoxTopPx + toPx(TOTAL_FIGURE_SIZE);
        double padBottom = Math.max(0, px(TOTAL_CARD_AT + TOTAL_CARD_H_PX - contentBottomPx));
        right.addSection("TotalCard", card -> {
            card.spacing(0)
                    .keepTogether()
                    .fillColor(TINT)
                    .cornerRadius(TOTAL_CARD_RADIUS)
                    .padding(new DocumentInsets(0, TOTAL_PAD_R, padBottom, TOTAL_PAD_L))
                    .margin(new DocumentInsets(top, 0, 0, 0));
            IndigoFlow inner = new IndigoFlow(TOTAL_CARD_AT);
            double rowTop = inner.boxAt(figureBoxTopPx, TOTAL_CARD_H_PX);
            layeredRow(card, "TotalRow", row -> {
                row.spacing(0)
                        // The label's ink band and the larger figure's share a
                        // centre line, not a baseline.
                        .verticalAlign(RowVerticalAlign.CENTER)
                        .margin(new DocumentInsets(rowTop, 0, 0, 0))
                        .weights(HALF, HALF);
                row.addParagraph(p -> p
                        .name("TotalLabel")
                        .text(investment.totalLabel())
                        .textStyle(plain(TOTAL_LABEL_SIZE, ACCENT)));
                row.addParagraph(p -> p
                        .name("TotalFigure")
                        .text(investment.totalAmount())
                        .textStyle(bold(TOTAL_FIGURE_SIZE, ACCENT))
                        .align(TextAlign.RIGHT));
            });
        });
    }

    private static void renderNotes(SectionBuilder right, ProposalTermsBlock terms,
                                    IndigoFlow cell) {
        if (terms.heading().isBlank() && terms.items().isEmpty()) {
            return;
        }
        if (!terms.heading().isBlank()) {
            right.addParagraph(p -> p
                    .name("NotesHeading")
                    .text(terms.heading())
                    .textStyle(bold(LABEL_SIZE, ACCENT))
                    .margin(new DocumentInsets(
                            cell.capAt(NOTES_HEADING_CAP, LABEL_SIZE, true), 0, 0, 0)));
        }
        if (terms.items().isEmpty()) {
            return;
        }
        double top = cell.capAt(NOTES_FIRST_CAP, NOTE_SIZE, false);
        right.addList(list -> list
                .name("NotesList")
                .items(terms.items())
                .bullet()
                .textStyle(plain(NOTE_SIZE, BODY))
                .itemSpacing(Math.max(0, px(NOTES_PITCH_PX) - NOTE_SIZE))
                .margin(new DocumentInsets(top, 0, 0, 0)));
        cell.advanceTo(NOTES_FIRST_CAP + (terms.items().size() - 1) * NOTES_PITCH_PX
                + toPx(NOTE_SIZE));
    }
}
