package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.proposal.ProposalAcceptance;
import com.demcha.compose.document.templates.data.proposal.ProposalDeliverables;
import com.demcha.compose.document.templates.data.proposal.ProposalInvestment;
import com.demcha.compose.document.templates.data.proposal.ProposalPhaseGrid;
import com.demcha.compose.document.templates.data.proposal.ProposalTermsBlock;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.ACCEPTANCE_FIELD_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.ACCEPTANCE_PADDING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.ACCEPTANCE_SIDE_PADDING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BAND_TOP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DELIVERABLES_COLUMN_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DELIVERABLES_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_AFTER_RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_BEFORE_TABLE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_BULLET;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_PAGE_TWO_OPENING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_EMPHASIS_PAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_LABEL_SPLIT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_ROW_PAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_TOTAL_PAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INVESTMENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.MONEY_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PANEL_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.RULE_QUIET;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SIGNATURE_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SIGNATURE_LABEL_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SIGNATURE_RULE_WEIGHTS;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SIGNATURE_SPACER;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SUBTOTAL_FILL;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TABLE_BODY;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TABLE_BODY_BOLD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TABLE_CELL_INSET;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TABLE_HEADER;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TERMS_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TIMELINE_ROW_PAD;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TIMELINE_WEIGHTS;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.TOTAL_TEXT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialWidgets.renderBullet;
import static com.demcha.compose.document.templates.proposal.presets.EditorialWidgets.sectionHeading;

/**
 * Page two of the Editorial Proposal: the deliverable columns, the phase
 * grid, the investment / terms band, and the signing card.
 */
final class EditorialPageTwo {

    private EditorialPageTwo() {
    }

    static void compose(PageFlowBuilder page, StructuredProposalData data) {
        renderDeliverables(page, data.deliverables());
        renderTimelineTable(page, data.timeline());
        renderMoneyBand(page, data);
        renderAcceptance(page, data.acceptance());
    }

    /** Heading plus two bullet columns of measured width. */
    private static void renderDeliverables(PageFlowBuilder page,
                                           ProposalDeliverables deliverables) {
        page.addSection("Deliverables", section -> {
            section.margin(GAP_PAGE_TWO_OPENING, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(deliverables.heading()));
            section.addRow("DeliverableColumns", row -> {
                row.verticalAlign(RowVerticalAlign.TOP)
                        .gap(0)
                        .padding(DocumentInsets.top(GAP_AFTER_RULE))
                        // Fixed, not weights: weights would divide the whole
                        // band between the two columns and neither would wrap
                        // where the reference wraps. The left column places the
                        // right one; the right one is its own measure, and the
                        // rest of the band stays empty as the reference leaves it.
                        .columns(DocumentRowColumn.fixed(
                                        CONTENT_WIDTH * DELIVERABLES_LEFT_WEIGHT),
                                DocumentRowColumn.fixed(
                                        CONTENT_WIDTH * DELIVERABLES_COLUMN_WEIGHT));
                row.addSection("DeliverablesLeft", column -> {
                    column.spacing(GAP_BULLET);
                    deliverables.leftColumn().forEach(item -> renderBullet(column, item));
                });
                row.addSection("DeliverablesRight", column -> {
                    column.spacing(GAP_BULLET);
                    deliverables.rightColumn().forEach(item -> renderBullet(column, item));
                });
            });
        });
    }

    /**
     * The phase grid. Requires one authored header per rendered column, and
     * is skipped entirely when the data carries no phases.
     *
     * <p>The two single-line columns are centred in their row rather than
     * sat on its first line; the prose columns keep TOP because they are
     * what makes the row tall, so centring them would be a no-op.</p>
     */
    private static void renderTimelineTable(PageFlowBuilder page, ProposalPhaseGrid timeline) {
        if (timeline.phases().isEmpty()) {
            return;
        }
        List<String> headers = timeline.columnHeaders();
        if (headers.size() != TIMELINE_WEIGHTS.length) {
            throw new IllegalArgumentException(
                    "The phase grid renders " + TIMELINE_WEIGHTS.length
                            + " columns and needs exactly one header per column; "
                            + "columnHeaders carries " + headers.size() + ".");
        }
        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(INK)
                .stroke(DocumentStroke.of(INK, HAIRLINE))
                .padding(DocumentInsets.symmetric(7, TABLE_CELL_INSET))
                .textStyle(TABLE_HEADER)
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .build();
        DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, HAIRLINE))
                .padding(DocumentInsets.symmetric(TIMELINE_ROW_PAD, TABLE_CELL_INSET))
                .textStyle(TABLE_BODY)
                .textAnchor(DocumentTableTextAnchor.TOP_LEFT)
                .lineSpacing(BODY_LEADING)
                .build();
        DocumentTableStyle centredCellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, HAIRLINE))
                .padding(DocumentInsets.symmetric(TIMELINE_ROW_PAD, TABLE_CELL_INSET))
                .textStyle(TABLE_BODY)
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .build();

        page.addSection("Timeline", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(timeline.heading()));
            section.addTable(table -> {
                table.name("TimelineTable")
                        .width(CONTENT_WIDTH)
                        .margin(DocumentInsets.top(GAP_BEFORE_TABLE))
                        .columns(DocumentTableColumn.fixed(CONTENT_WIDTH * TIMELINE_WEIGHTS[0]),
                                DocumentTableColumn.fixed(CONTENT_WIDTH * TIMELINE_WEIGHTS[1]),
                                DocumentTableColumn.fixed(CONTENT_WIDTH * TIMELINE_WEIGHTS[2]),
                                DocumentTableColumn.fixed(CONTENT_WIDTH * TIMELINE_WEIGHTS[3]))
                        .defaultCellStyle(cellStyle);
                table.headerCells(
                        DocumentTableCell.text(headers.get(0)).withStyle(headerStyle),
                        DocumentTableCell.text(headers.get(1)).withStyle(headerStyle),
                        DocumentTableCell.text(headers.get(2)).withStyle(headerStyle),
                        DocumentTableCell.text(headers.get(3)).withStyle(headerStyle));
                for (ProposalPhaseGrid.Phase phase : timeline.phases()) {
                    table.rowCells(
                            DocumentTableCell.text(phase.number() + "   " + phase.name())
                                    .withStyle(centredCellStyle),
                            DocumentTableCell.node(wrappingCell(phase.focus()))
                                    .withStyle(cellStyle),
                            DocumentTableCell.text(phase.duration())
                                    .withStyle(centredCellStyle),
                            DocumentTableCell.node(wrappingCell(phase.output()))
                                    .withStyle(cellStyle));
                }
                table.repeatHeader();
            });
        });
    }

    /** A table cell that has to wrap: a composed paragraph wraps to its column. */
    private static DocumentNode wrappingCell(String text) {
        return new ParagraphBuilder()
                .name("Cell")
                .text(text)
                .textStyle(TABLE_BODY)
                .lineSpacing(BODY_LEADING)
                .build();
    }

    /** The two-column money band: the priced table left, the terms right. */
    private static void renderMoneyBand(PageFlowBuilder page, StructuredProposalData data) {
        page.addRow("MoneyBand", row -> {
            row.margin(DocumentInsets.top(BAND_TOP));
            row.verticalAlign(RowVerticalAlign.TOP)
                    .gap(MONEY_GAP)
                    .weights(INVESTMENT_WEIGHT, TERMS_WEIGHT);
            row.addSection("InvestmentColumn",
                    column -> renderInvestmentTable(column, data.investment()));
            row.addSection("TermsColumn", column -> renderTerms(column, data.terms()));
        });
    }

    /** The priced block; the total band closes it in the accent. */
    private static void renderInvestmentTable(SectionBuilder column,
                                              ProposalInvestment investment) {
        double labelWidth = INVESTMENT_WIDTH * INVESTMENT_LABEL_SPLIT;
        double amountWidth = INVESTMENT_WIDTH - labelWidth;

        DocumentTableStyle headerLabel = investmentStyle(INK, TABLE_HEADER,
                DocumentTableTextAnchor.CENTER_LEFT, INVESTMENT_ROW_PAD + 1.5, INK);
        DocumentTableStyle headerAmount = investmentStyle(INK, TABLE_HEADER,
                DocumentTableTextAnchor.CENTER_RIGHT, INVESTMENT_ROW_PAD + 1.5, INK);
        DocumentTableStyle rowLabel = investmentStyle(null, TABLE_BODY,
                DocumentTableTextAnchor.CENTER_LEFT, INVESTMENT_ROW_PAD, RULE_QUIET);
        DocumentTableStyle rowAmount = investmentStyle(null, TABLE_BODY,
                DocumentTableTextAnchor.CENTER_RIGHT, INVESTMENT_ROW_PAD, RULE_QUIET);
        DocumentTableStyle subtotalLabel = investmentStyle(SUBTOTAL_FILL, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_LEFT, INVESTMENT_EMPHASIS_PAD, RULE_QUIET);
        DocumentTableStyle subtotalAmount = investmentStyle(SUBTOTAL_FILL, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_RIGHT, INVESTMENT_EMPHASIS_PAD, RULE_QUIET);
        DocumentTableStyle optionalAmount = investmentStyle(null, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_RIGHT, INVESTMENT_ROW_PAD, RULE_QUIET);
        DocumentTableStyle totalLabel = investmentStyle(ACCENT, TOTAL_TEXT,
                DocumentTableTextAnchor.CENTER_LEFT, INVESTMENT_TOTAL_PAD, ACCENT);
        DocumentTableStyle totalAmount = investmentStyle(ACCENT, TOTAL_TEXT,
                DocumentTableTextAnchor.CENTER_RIGHT, INVESTMENT_TOTAL_PAD, ACCENT);

        column.spacing(0);
        column.add(sectionHeading(investment.heading()));
        column.addTable(table -> {
            table.name("InvestmentTable")
                    .width(INVESTMENT_WIDTH)
                    .margin(DocumentInsets.top(GAP_BEFORE_TABLE))
                    .columns(DocumentTableColumn.fixed(labelWidth),
                            DocumentTableColumn.fixed(amountWidth))
                    .defaultCellStyle(rowLabel);
            table.headerCells(
                    DocumentTableCell.text(investment.itemHeader()).withStyle(headerLabel),
                    DocumentTableCell.text(investment.amountHeader()).withStyle(headerAmount));
            for (ProposalInvestment.Row line : investment.rows()) {
                switch (line.role()) {
                    case SUBTOTAL -> table.rowCells(
                            DocumentTableCell.text(line.label()).withStyle(subtotalLabel),
                            DocumentTableCell.text(line.amount()).withStyle(subtotalAmount));
                    case OPTIONAL -> table.rowCells(
                            DocumentTableCell.text(line.label()).withStyle(rowLabel),
                            DocumentTableCell.text(line.amount()).withStyle(optionalAmount));
                    default -> table.rowCells(
                            DocumentTableCell.text(line.label()).withStyle(rowLabel),
                            DocumentTableCell.text(line.amount()).withStyle(rowAmount));
                }
            }
            table.rowCells(
                    DocumentTableCell.text(investment.totalLabel()).withStyle(totalLabel),
                    DocumentTableCell.text(investment.totalAmount()).withStyle(totalAmount));
            table.repeatHeader();
        });
    }

    /** One investment cell style; {@code fill} may be null for the plain rows. */
    private static DocumentTableStyle investmentStyle(DocumentColor fill,
                                                      DocumentTextStyle text,
                                                      DocumentTableTextAnchor anchor,
                                                      double verticalPad,
                                                      DocumentColor strokeColor) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(strokeColor, HAIRLINE))
                .padding(DocumentInsets.symmetric(verticalPad, TABLE_CELL_INSET))
                .textStyle(text)
                .textAnchor(anchor);
        if (fill != null) {
            builder.fillColor(fill);
        }
        return builder.build();
    }

    /** Heading plus the bulleted terms, reusing the deliverables bullet. */
    private static void renderTerms(SectionBuilder column, ProposalTermsBlock terms) {
        column.spacing(0);
        column.add(sectionHeading(terms.heading()));
        column.addSection("TermsList", list -> {
            list.spacing(GAP_BULLET).padding(GAP_AFTER_RULE, 0f, 0f, 0f);
            terms.items().forEach(item -> renderBullet(list, item));
        });
    }

    /**
     * The signing card. {@code softPanel} carries one padding for all four
     * sides, so the wider horizontal inset the reference uses belongs to the
     * body section inside it.
     */
    private static void renderAcceptance(PageFlowBuilder page, ProposalAcceptance acceptance) {
        double side = ACCEPTANCE_SIDE_PADDING - ACCEPTANCE_PADDING;
        page.addSection("Acceptance", card -> {
            card.margin(BAND_TOP, 0f, 0f, 0f);
            card.spacing(0).keepTogether().softPanel(PANEL_BACKGROUND, PANEL_RADIUS,
                    ACCEPTANCE_PADDING, DocumentStroke.of(RULE, HAIRLINE));
            card.addSection("AcceptanceBody", body -> {
                body.spacing(0).padding(0f, (float) side, 0f, (float) side);
                body.add(sectionHeading(acceptance.heading()));
                body.addParagraph(p -> p
                        .text(acceptance.statement())
                        .textStyle(BODY)
                        .lineSpacing(BODY_LEADING)
                        .margin(GAP_AFTER_RULE, 0f, 0f, 0f));
                if (!acceptance.fields().isEmpty()) {
                    body.addRow("SignatureFields",
                            row -> renderSignatureFields(row, acceptance.fields()));
                }
            });
        });
    }

    /** label, rule, spacer, label, rule, spacer, label, rule. */
    private static void renderSignatureFields(RowBuilder row, List<String> fields) {
        DocumentRowColumn[] columns = new DocumentRowColumn[fields.size() * 3 - 1];
        int at = 0;
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                columns[at++] = DocumentRowColumn.fixed(SIGNATURE_SPACER);
            }
            columns[at++] = DocumentRowColumn.auto();
            columns[at++] = DocumentRowColumn.weight(signatureRuleWeight(i, fields.size()));
        }
        row.verticalAlign(RowVerticalAlign.BOTTOM)
                .gap(SIGNATURE_LABEL_GAP)
                .padding(DocumentInsets.top(ACCEPTANCE_FIELD_GAP))
                .columns(columns);
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                row.addSpacer(SIGNATURE_SPACER);
            }
            String field = fields.get(i);
            row.addParagraph(p -> p.text(field).textStyle(SIGNATURE_LABEL));
            row.addLine(line -> line.fill().thickness(HAIRLINE).color(INK));
        }
    }

    /**
     * The measured rule widths apply to the three-field card the reference
     * sets; any other count divides the row evenly.
     */
    private static double signatureRuleWeight(int index, int fieldCount) {
        return fieldCount == SIGNATURE_RULE_WEIGHTS.length
                ? SIGNATURE_RULE_WEIGHTS[index]
                : 1.0 / fieldCount;
    }
}
