package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.BAND_FILL;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_ICON_COLUMN;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_ICON_LIFT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_ICON_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_PAD_B;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_PAD_T;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CLOSE_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_NOTE_TO_STRIP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_PAYMENT_TO_NOTE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_STRIP_TO_BAND;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_TABLE_TO_PAYMENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAGE_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAYMENT_PX;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_COLUMNS_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_DIVIDER;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_ICON_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_LABEL_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_NOTE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_OUTER_NUDGE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_VALUE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PAY_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.STRIP_H;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.STRIP_PX;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.STRIP_TOTAL_PX;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.MARGIN_X;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.baselineGap;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.cycle;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionWidgets.headingPlaque;

/**
 * The foot of the sheet: the payment band, the reference note under it, the
 * four-segment strip, and the closing band that bleeds to three paper edges.
 */
final class SubscriptionClosing {

    private SubscriptionClosing() {
    }

    /**
     * The payment band: one marked cell per field, divided by hairlines.
     *
     * <p>The design's outer two cells are not centred on the space the dividers
     * give them — their content sits inboard — while the middle three are centred
     * to within three pixels, so the nudge is on the outer pair only.</p>
     *
     * <p>The cells take their widths from the design's five, so a payment block
     * with a different number of fields divides the band evenly instead.</p>
     */
    static void renderPaymentBand(PageFlowBuilder page, InvoicePaymentBlock payment) {
        List<InvoicePaymentBlock.Field> fields = payment.fields();
        if (fields.isEmpty()) {
            return;
        }
        page.addSection("PaymentDetails", section -> {
            section.padding(CONTENT_PAD);
            section.margin(new DocumentInsets(GAP_TABLE_TO_PAYMENT, 0, 0, 0));
            section.spacing(0);
            section.keepTogether();
            headingPlaque(section, "PaymentHeading", payment.heading(), HEADING_SIZE, ACCENT);
            section.addRow("PaymentColumns", row -> {
                row.spacing(0);
                row.margin(new DocumentInsets(PAY_COLUMNS_GAP, 0, 0, 0));
                row.weights(weights(fields.size()));
                for (int i = 0; i < fields.size(); i++) {
                    InvoicePaymentBlock.Field field = fields.get(i);
                    SvgIcon mark = SubscriptionIcons.band(i);
                    boolean divided = i > 0;
                    boolean outer = i == 0 || i == fields.size() - 1;
                    int index = i;
                    row.addSection("PaymentColumn_" + index, cell -> {
                        cell.spacing(0);
                        if (outer) {
                            cell.padding(new DocumentInsets(0, PAY_OUTER_NUDGE, 0, 0));
                        }
                        if (divided) {
                            cell.accentLeft(HAIRLINE, PAY_DIVIDER);
                        }
                        if (mark != null) {
                            cell.addSvgIcon(mark, PAY_ICON_W, HorizontalAlign.CENTER);
                        }
                        cell.addParagraph(p -> p
                                .name("PaymentLabel_" + index)
                                .text(field.label())
                                .textStyle(bold(PAY_LABEL_SIZE, INK))
                                .align(TextAlign.CENTER)
                                .margin(new DocumentInsets(PAY_LABEL_GAP, 0, 0, 0)));
                        cell.addParagraph(p -> p
                                .name("PaymentValue_" + index)
                                .text(field.value())
                                .textStyle(plain(PAY_VALUE_SIZE, INK))
                                .align(TextAlign.CENTER)
                                .margin(new DocumentInsets(PAY_VALUE_GAP, 0, 0, 0)));
                    });
                }
            });
        });
    }

    /**
     * The design's five column widths when the band has five cells, and an even
     * division when it has any other number — measured shares only describe the
     * band they were measured on.
     */
    private static double[] weights(int cells) {
        double[] weights = new double[cells];
        if (cells == PAYMENT_PX.length) {
            double total = 0;
            for (double share : PAYMENT_PX) {
                total += share;
            }
            for (int i = 0; i < cells; i++) {
                weights[i] = PAYMENT_PX[i] / total;
            }
            return weights;
        }
        for (int i = 0; i < cells; i++) {
            weights[i] = 1.0 / cells;
        }
        return weights;
    }

    /** The instruction under the band. */
    static void renderPaymentNote(PageFlowBuilder page, InvoicePaymentBlock payment) {
        if (payment.instruction().isBlank()) {
            return;
        }
        page.addSection("PaymentNote", section -> {
            section.padding(CONTENT_PAD);
            section.margin(new DocumentInsets(GAP_PAYMENT_TO_NOTE, 0, 0, 0));
            section.spacing(0);
            section.addParagraph(p -> p
                    .name("PaymentNote")
                    .text(payment.instruction())
                    .textStyle(plain(PAY_NOTE_SIZE, INK)));
        });
    }

    /** The four-segment strip, at the design's measured widths across the paper. */
    static void renderStrip(PageFlowBuilder page) {
        page.addRow("BrandStrip", row -> {
            row.margin(new DocumentInsets(GAP_NOTE_TO_STRIP, 0, 0, 0));
            row.spacing(0);
            DocumentRowColumn[] columns = new DocumentRowColumn[STRIP_PX.length];
            for (int i = 0; i < STRIP_PX.length; i++) {
                columns[i] = DocumentRowColumn.fixed(PAGE_W * STRIP_PX[i] / STRIP_TOTAL_PX);
            }
            row.columns(columns);
            for (int i = 0; i < STRIP_PX.length; i++) {
                DocumentColor colour = cycle(i);
                double width = PAGE_W * STRIP_PX[i] / STRIP_TOTAL_PX;
                int index = i;
                row.addShape(shape -> shape
                        .name("BrandStripSegment_" + index)
                        .size(width, STRIP_H)
                        .fillColor(colour));
            }
        });
    }

    /**
     * The closing band.
     *
     * <p>It reaches three paper edges by bleeding rather than by being the last
     * thing on the page, which is what lets the enumeration sit over its fill
     * without the band giving up any of its own width.</p>
     *
     * <p>Its lines come from the payment block's sign-off and due notice: the
     * sign-off is the headline, and the notice's own lines are the prose under
     * it, with any address the prose names made reachable.</p>
     */
    static void renderBand(PageFlowBuilder page, InvoicePaymentBlock payment,
                           InvoiceContactBlock supplier) {
        if (payment.signOff().isBlank() && payment.dueNotice().isBlank()) {
            return;
        }
        page.addSection("ClosingBand", section -> {
            section.name("ClosingBand");
            section.spacing(0);
            section.keepTogether();
            section.margin(new DocumentInsets(GAP_STRIP_TO_BAND, 0, 0, 0));
            section.padding(new DocumentInsets(CLOSE_PAD_T, MARGIN_X, CLOSE_PAD_B, CLOSE_PAD_L));
            section.fillColor(BAND_FILL);
            section.bleedToEdge(DocumentEdge.LEFT, DocumentEdge.RIGHT, DocumentEdge.BOTTOM);
            section.addRow("ClosingLockup", row -> {
                row.spacing(0);
                row.verticalAlign(RowVerticalAlign.CENTER);
                row.columns(DocumentRowColumn.fixed(CLOSE_ICON_COLUMN),
                        DocumentRowColumn.weight(1));
                row.addSection("ClosingMark", cell -> {
                    cell.spacing(0);
                    cell.margin(new DocumentInsets(0, 0, CLOSE_ICON_LIFT, 0));
                    cell.addSvgIcon(SubscriptionIcons.icon(SubscriptionIcons.SHIELD),
                            CLOSE_ICON_W, HorizontalAlign.LEFT);
                });
                row.addSection("ClosingMessage", cell -> {
                    cell.spacing(0);
                    cell.padding(new DocumentInsets(0, 0, 0, CLOSE_TEXT_INDENT));
                    if (!payment.signOff().isBlank()) {
                        cell.addParagraph(p -> p
                                .name("ClosingHeadline")
                                .text(payment.signOff())
                                .textStyle(bold(CLOSE_HEAD_SIZE, INK)));
                    }
                    String[] lines = payment.dueNotice().isBlank()
                            ? new String[0]
                            : payment.dueNotice().split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String prose = lines[i];
                        int index = i;
                        cell.addParagraph(p -> {
                            p.name("ClosingLine_" + index);
                            p.margin(new DocumentInsets(
                                    baselineGap(CLOSE_LINE_PITCH,
                                            index == 0 ? CLOSE_HEAD_SIZE : CLOSE_BODY_SIZE,
                                            CLOSE_BODY_SIZE),
                                    0, 0, 0));
                            writeReachable(p, prose, supplier);
                        });
                    }
                });
            });
        });
    }

    /**
     * The prose, with any address it names made reachable.
     *
     * <p>The band tells a reader where to write, and the address is printed
     * inside the sentence. Written as one string the whole sentence would have to
     * carry the target or none of it would, so the address becomes its own run
     * and the text around it stays plain.</p>
     */
    private static void writeReachable(ParagraphBuilder paragraph, String prose,
                                       InvoiceContactBlock supplier) {
        List<Reachable> found = new ArrayList<>();
        addIfPresent(found, prose, supplier.email(), ContactUri.mailLink(supplier.email()));
        addIfPresent(found, prose, supplier.website(), ContactUri.webLink(supplier.website()));
        addIfPresent(found, prose, supplier.phone(), ContactUri.telLink(supplier.phone()));
        found.sort(Comparator.comparingInt(Reachable::at));

        int cursor = 0;
        for (Reachable reachable : found) {
            if (reachable.at() < cursor) {
                continue;
            }
            if (reachable.at() > cursor) {
                paragraph.inlineText(prose.substring(cursor, reachable.at()),
                        plain(CLOSE_BODY_SIZE, INK));
            }
            paragraph.inlineText(reachable.text(), plain(CLOSE_BODY_SIZE, INK), reachable.link());
            cursor = reachable.at() + reachable.text().length();
        }
        if (cursor < prose.length()) {
            paragraph.inlineText(prose.substring(cursor), plain(CLOSE_BODY_SIZE, INK));
        }
    }

    private static void addIfPresent(List<Reachable> found, String prose, String text,
                                     DocumentLinkOptions link) {
        if (text == null || text.isBlank() || link == null) {
            return;
        }
        int at = prose.indexOf(text);
        if (at >= 0) {
            found.add(new Reachable(at, text, link));
        }
    }

    private record Reachable(int at, String text, DocumentLinkOptions link) {
    }
}
