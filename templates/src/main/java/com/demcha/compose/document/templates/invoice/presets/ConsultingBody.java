package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY_ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.EMPHASIS_FILL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.ROW_ALT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TABLE_HEADER;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TABLE_RATIOS;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TOTALS_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TOTAL_AMOUNT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TOTAL_AMOUNT_RIGHT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.TOTAL_LABEL_LEFT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.tableStyle;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.decimal;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.link;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.money;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.titleBreakerWidth;

/**
 * The body of the Consulting Invoice: who is billed and for what, the
 * priced service lines, and the totals stack.
 */
final class ConsultingBody {

    private ConsultingBody() {
    }

    /** The billed-to block: name, attention line, address, then the email. */
    static void renderBilledTo(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceRecipient billTo = data.billTo();
        ConsultingMasthead.sectionHeading(section, billTo.heading());
        section.spacing(1.5).addParagraph(billTo.name(), BODY_BOLD);
        // A recipient without an attention line gets no line box for it.
        if (!billTo.subline().isBlank()) {
            section.addParagraph(billTo.subline(), BODY);
        }
        section.addParagraph(String.join("\n", billTo.addressLines()), BODY);
        if (!billTo.email().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .rich(rich -> rich
                            .style(billTo.emailLabel() + "  ", BODY_BOLD)
                            .with(billTo.email(), BODY, link("mailto:" + billTo.email())))
                    .margin(DocumentInsets.top(5)));
        }
    }

    /** What the invoice covers, and the period it covers. */
    static void renderSummary(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceSummaryBlock summary = data.summary();
        ConsultingMasthead.sectionHeading(section, summary.heading());
        section.spacing(1)
                .addParagraph(paragraph -> paragraph
                        .text(summary.intro())
                        .textStyle(BODY)
                        .lineSpacing(1.35)
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(summary.servicePeriod())
                        .textStyle(BODY_BOLD)
                        .lineSpacing(1.15)
                        .margin(DocumentInsets.zero()));
    }

    /**
     * The line-items table: a repeating header on the accent fill, and one
     * zebra-filled row per service line. The unit-price and amount headers
     * carry the currency on their second line.
     */
    static void renderServiceLines(TableBuilder table, StructuredInvoiceData data) {
        InvoiceServiceLines serviceLines = data.serviceLines();
        InvoiceServiceLines.Columns columns = serviceLines.columns();
        // A document that states no currency prints no parenthetical.
        String currency = data.currencyCode().isBlank()
                ? "" : "(" + data.currencyCode() + ")";

        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .padding(DocumentInsets.symmetric(4.5, 4.0))
                .fillColor(ACCENT_PRIMARY)
                .stroke(DocumentStroke.of(ACCENT_PRIMARY, 0.6))
                .textStyle(TABLE_HEADER)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(1.0)
                .build();

        table.name("LineItems")
                .columns(column(0), column(1), column(2), column(3), column(4), column(5))
                .headerCells(
                        DocumentTableCell.text(columns.index()),
                        DocumentTableCell.text(columns.description()),
                        DocumentTableCell.text(columns.servicePeriod()),
                        DocumentTableCell.text(columns.quantity()),
                        DocumentTableCell.lines(columns.unitPrice(), currency),
                        DocumentTableCell.lines(columns.amount(), currency))
                .headerStyle(headerStyle)
                .repeatHeader()
                .padding(DocumentInsets.zero())
                .margin(DocumentInsets.zero());

        List<InvoiceServiceLines.Line> lines = serviceLines.lines();
        for (int index = 0; index < lines.size(); index++) {
            InvoiceServiceLines.Line line = lines.get(index);
            DocumentColor fill = index % 2 == 0 ? PAGE_BACKGROUND : ROW_ALT;
            table.rowCells(
                    DocumentTableCell.text(Integer.toString(line.lineNumber()))
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER)),
                    DocumentTableCell.node(descriptionNode(line, fill))
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER_LEFT)),
                    DocumentTableCell.text(line.servicePeriod())
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER)),
                    DocumentTableCell.lines(decimal(line.quantity()), line.unit())
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER)),
                    DocumentTableCell.text(money(line.unitPrice()))
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER_RIGHT)),
                    DocumentTableCell.text(money(line.amount()))
                            .withStyle(tableStyle(fill, DocumentTableTextAnchor.CENTER_RIGHT)));
        }
    }

    private static DocumentTableColumn column(int index) {
        return DocumentTableColumn.fixed(CONTENT_WIDTH * TABLE_RATIOS[index]);
    }

    /**
     * The description cell: the title, an invisible breaker that fills the
     * rest of the title line, then the description on its own line. The
     * breaker is a zero-height rectangle in the row's own fill — the engine
     * has no rich-text line break (see
     * {@link ConsultingText#titleBreakerWidth(String)}).
     */
    private static DocumentNode descriptionNode(InvoiceServiceLines.Line line,
                                                DocumentColor fill) {
        return new ParagraphBuilder()
                .name("Description")
                .align(TextAlign.LEFT)
                .textStyle(SMALL)
                .lineSpacing(1.15)
                .margin(DocumentInsets.zero())
                .rich(rich -> rich
                        .style(line.title(), SMALL_BOLD)
                        .shape(new ShapeOutline.Rectangle(titleBreakerWidth(line.title()), 0.1), fill)
                        .style(line.description(), SMALL))
                .build();
    }

    /**
     * The totals stack, pinned to the right of the content box and kept on
     * one page: the rows above the rule, then the emphasized total band.
     */
    static void renderTotals(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceTotalsBlock totals = data.totals();
        section.keepTogether()
                .margin(6, 0, 0, (float) (CONTENT_WIDTH - TOTALS_WIDTH))
                .spacing(1);
        for (InvoiceTotalsBlock.Row row : totals.rows()) {
            totalsBand(section, row.label(), money(row.amount()), false);
        }
        section.addLine(line -> line
                .horizontal(TOTALS_WIDTH)
                .thickness(1.0)
                .color(ACCENT_PRIMARY)
                .margin(DocumentInsets.top(2)));
        String totalLabel = data.currencyCode().isBlank()
                ? totals.totalLabel()
                : totals.totalLabel() + " (" + data.currencyCode() + ")";
        totalsBand(section, totalLabel, money(totals.totalAmount()), true);
    }

    private static void totalsBand(SectionBuilder section,
                                   String label,
                                   String amount,
                                   boolean emphasized) {
        double height = emphasized ? 27 : 15;
        section.addContainer(container -> {
            container.name(emphasized ? "TotalDue" : "TotalRow")
                    .rectangle(TOTALS_WIDTH, height)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .centerLeft(insetParagraph(
                            label,
                            emphasized ? BODY_ACCENT : SMALL_BOLD,
                            TextAlign.LEFT,
                            0,
                            TOTAL_LABEL_LEFT_INSET))
                    .centerRight(insetParagraph(
                            amount,
                            emphasized ? TOTAL_AMOUNT : BODY_BOLD,
                            TextAlign.RIGHT,
                            TOTAL_AMOUNT_RIGHT_INSET,
                            0));
            if (emphasized) {
                container.fillColor(EMPHASIS_FILL);
            }
        });
    }

    private static DocumentNode insetParagraph(String text,
                                               DocumentTextStyle style,
                                               TextAlign align,
                                               double right,
                                               double left) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(align)
                .lineSpacing(1.15)
                .padding(new DocumentInsets(0, (float) right, 0, (float) left))
                .margin(DocumentInsets.zero())
                .build();
    }
}
