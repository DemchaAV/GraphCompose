package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
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

import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCEPTANCE_FIELD_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCEPTANCE_PADDING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BAND_TOP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.DELIVERABLES_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.DELIVERABLES_RIGHT_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GAP_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INVESTMENT_ROW_PAD;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INVESTMENT_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INVESTMENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.MONEY_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PANEL_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.RULE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.RULE_QUIET;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SIGNATURE_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SIGNATURE_LABEL_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TABLE_BODY;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TABLE_BODY_BOLD;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TABLE_HEADER;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TERMS_WEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TERMS_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TIMELINE_WEIGHTS;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.TOTAL_TEXT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineWidgets.renderBullet;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineWidgets.sectionHeading;

/**
 * Page two of the Northline proposal: the deliverable columns, the phase
 * grid, the investment / terms band, and the signing card.
 *
 * <p>Layout notes carried over from the ported template: the phase grid is
 * the one region with real vertical rules and therefore the one real
 * table; the investment block sits inside a row cell where a nested row is
 * refused, so it is a table too — with the recorded cost that its cell
 * strokes also draw a rule down the ITEM/AMOUNT boundary. Prose cells in
 * the phase grid are composed paragraphs (a plain-text cell reports its
 * whole string as the column's natural width and a narrower fixed column is
 * rejected rather than wrapped), while the phase cell stays a plain-text
 * cell — {@code DocumentTableTextAnchor} centres a text cell's value but
 * not a composed one's.</p>
 */
final class NorthlinePageTwo {

    private NorthlinePageTwo() {
    }

    static void compose(PageFlowBuilder page, StructuredProposalData data) {
        renderDeliverables(page, data.deliverables());
        renderTimelineTable(page, data.timeline());
        renderMoneyBand(page, data);
        renderAcceptance(page, data.acceptance());
    }

    /** Heading plus two bullet columns. */
    private static void renderDeliverables(PageFlowBuilder page,
                                           ProposalDeliverables deliverables) {
        page.addSection("Deliverables", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(
                    deliverables.heading(), deliverables.icon(), CONTENT_WIDTH));
            section.addRow("DeliverableColumns", row -> {
                row.verticalAlign(RowVerticalAlign.TOP)
                        .gap(0)
                        .padding(DocumentInsets.top(GAP_HEADING))
                        .weights(DELIVERABLES_LEFT_WEIGHT, DELIVERABLES_RIGHT_WEIGHT);
                row.addSection("DeliverablesLeft", column -> {
                    column.spacing(0);
                    deliverables.leftColumn().forEach(item -> renderBullet(column, item));
                });
                row.addSection("DeliverablesRight", column -> {
                    column.spacing(0);
                    deliverables.rightColumn().forEach(item -> renderBullet(column, item));
                });
            });
        });
    }

    /**
     * The phase grid. Requires one authored header per column: the grid
     * draws four columns, so four headers — anything else is a data error
     * reported here rather than an index error deep in the table. Skipped
     * entirely when the data has no phases, including that header-count
     * validation — a grid that never renders constrains nothing.
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
                .padding(DocumentInsets.symmetric(7, 10))
                .textStyle(TABLE_HEADER)
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .build();
        DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, HAIRLINE))
                .padding(DocumentInsets.symmetric(9, 10))
                .textStyle(TABLE_BODY)
                .textAnchor(DocumentTableTextAnchor.TOP_LEFT)
                .lineSpacing(BODY_LEADING)
                .build();
        DocumentTableStyle centredCellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, HAIRLINE))
                .padding(DocumentInsets.symmetric(9, 10))
                .textStyle(TABLE_BODY)
                .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
                .lineSpacing(BODY_LEADING)
                .build();

        page.addSection("Timeline", section -> {
            section.margin(BAND_TOP, 0f, 0f, 0f);
            section.spacing(0);
            section.add(sectionHeading(timeline.heading(), timeline.icon(), CONTENT_WIDTH));
            section.addTable(table -> {
                table.name("TimelineTable")
                        .width(CONTENT_WIDTH)
                        .margin(DocumentInsets.top(GAP_HEADING))
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

    /** The two-column money band: the priced table left, terms right. */
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

    /** The priced block, with the last band always the labelled total. */
    private static void renderInvestmentTable(SectionBuilder column,
                                              ProposalInvestment investment) {
        double labelWidth = INVESTMENT_WIDTH * 0.62;
        double amountWidth = INVESTMENT_WIDTH - labelWidth;

        DocumentTableStyle headerLabel = investmentStyle(INK, TABLE_HEADER,
                DocumentTableTextAnchor.CENTER_LEFT);
        DocumentTableStyle headerAmount = investmentStyle(INK, TABLE_HEADER,
                DocumentTableTextAnchor.CENTER_RIGHT);
        DocumentTableStyle rowLabel = investmentStyle(null, TABLE_BODY,
                DocumentTableTextAnchor.CENTER_LEFT);
        DocumentTableStyle rowAmount = investmentStyle(null, TABLE_BODY,
                DocumentTableTextAnchor.CENTER_RIGHT);
        DocumentTableStyle subtotalLabel = investmentStyle(PANEL_BACKGROUND, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_LEFT);
        DocumentTableStyle subtotalAmount = investmentStyle(PANEL_BACKGROUND, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_RIGHT);
        DocumentTableStyle optionalAmount = investmentStyle(null, TABLE_BODY_BOLD,
                DocumentTableTextAnchor.CENTER_RIGHT);
        DocumentTableStyle totalLabel = investmentStyle(ACCENT, TOTAL_TEXT,
                DocumentTableTextAnchor.CENTER_LEFT);
        DocumentTableStyle totalAmount = investmentStyle(ACCENT, TOTAL_TEXT,
                DocumentTableTextAnchor.CENTER_RIGHT);

        column.spacing(0);
        column.add(sectionHeading(
                investment.heading(), investment.icon(), INVESTMENT_WIDTH));
        column.addTable(table -> {
            table.name("InvestmentTable")
                    .width(INVESTMENT_WIDTH)
                    .margin(DocumentInsets.top(GAP_HEADING))
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
        });
    }

    /** One investment cell style; {@code fill} may be null for the plain rows. */
    private static DocumentTableStyle investmentStyle(DocumentColor fill,
                                                      DocumentTextStyle text,
                                                      DocumentTableTextAnchor anchor) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE_QUIET, HAIRLINE))
                .padding(DocumentInsets.symmetric(INVESTMENT_ROW_PAD, 10))
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
        column.add(sectionHeading(terms.heading(), terms.icon(), TERMS_WIDTH));
        column.addSection("TermsList", list -> {
            list.spacing(0).padding((float) GAP_HEADING, 0f, 0f, 0f);
            terms.items().forEach(item -> renderBullet(list, item));
        });
    }

    /**
     * The signing card. The signature fields are flat row columns — label,
     * rule per field — with the column spec derived from the field count,
     * so no pair nests inside a cell; the row is skipped when the data has
     * no fields.
     */
    private static void renderAcceptance(PageFlowBuilder page, ProposalAcceptance acceptance) {
        double inner = CONTENT_WIDTH - 2 * ACCEPTANCE_PADDING;
        page.addSection("Acceptance", card -> {
            card.margin(BAND_TOP, 0f, 0f, 0f);
            card.spacing(0).softPanel(PANEL_BACKGROUND, PANEL_RADIUS, ACCEPTANCE_PADDING,
                    DocumentStroke.of(RULE, HAIRLINE));
            card.add(sectionHeading(acceptance.heading(), acceptance.icon(), inner));
            card.addParagraph(p -> p
                    .text(acceptance.statement())
                    .textStyle(BODY)
                    .lineSpacing(BODY_LEADING)
                    .margin((float) GAP_HEADING, 0f, 0f, 0f));
            List<String> fields = acceptance.fields();
            if (fields.isEmpty()) {
                return;
            }
            card.addRow("SignatureFields", row -> {
                row.verticalAlign(RowVerticalAlign.BOTTOM)
                        .gap(6)
                        .padding(DocumentInsets.top(ACCEPTANCE_FIELD_GAP))
                        .columns(signatureColumns(fields.size()));
                for (String field : fields) {
                    row.addParagraph(p -> p.text(field).textStyle(SIGNATURE_LABEL));
                    row.addLine(line -> line.fill().thickness(HAIRLINE).color(INK));
                }
            });
        });
    }

    /** Label-strip + growing-rule column pair per signature field. */
    private static DocumentRowColumn[] signatureColumns(int fieldCount) {
        DocumentRowColumn[] columns = new DocumentRowColumn[fieldCount * 2];
        for (int i = 0; i < fieldCount; i++) {
            columns[i * 2] = DocumentRowColumn.fixed(SIGNATURE_LABEL_WIDTH);
            columns[i * 2 + 1] = DocumentRowColumn.weight(1);
        }
        return columns;
    }
}
