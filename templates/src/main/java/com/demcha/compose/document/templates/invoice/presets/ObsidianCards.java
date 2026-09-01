package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_ICON;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_ICON_COL;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_PAD_H;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_STROKE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAY_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PAY_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.topBearing;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.glyph;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.layeredRow;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.linked;

/**
 * The two information cards under the totals: the notes beside the payment
 * details, each opening with a marked heading.
 */
final class ObsidianCards {

    private ObsidianCards() {
    }

    /**
     * The cards row.
     *
     * @param page    the page flow
     * @param notes   the note the card on the left carries
     * @param payment the payment details the card on the right carries
     */
    static void render(PageFlowBuilder page, InvoiceNotesBlock notes,
                       InvoicePaymentBlock payment) {
        page.addRow("InfoCardsRow", row -> {
            row.spacing(CARD_GUTTER)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(new DocumentInsets(px(15), 0, 0, 0))
                    .weights(HALF, HALF);
            row.addSection("NotesCard", cell -> renderNotes(cell, notes));
            row.addSection("PaymentCard", cell -> renderPayment(cell, payment));
        });
    }

    private static void renderNotes(SectionBuilder cell, InvoiceNotesBlock notes) {
        chrome(cell, 1375 - 1331, CARD_BODY_SIZE);
        head(cell, "Notes", ObsidianIcons.NOTES, notes.heading());
        cell.addSection("NotesBody", body -> {
            body.spacing(0).padding(new DocumentInsets(0, 0, 0, CARD_ICON_COL - CARD_PAD_H));
            List<String> paragraphs = notes.paragraphs();
            for (int i = 0; i < paragraphs.size(); i++) {
                String prose = paragraphs.get(i);
                int index = i;
                double above = index == 0
                        ? px(1222 - 1182) - CARD_ICON - topBearing(CARD_BODY_SIZE, false)
                        : capGap(1308 - 1267, CARD_BODY_SIZE, false, CARD_BODY_SIZE, false);
                double gapAbove = Math.max(0, above);
                body.addParagraph(p -> p
                        .name("NotesParagraph_" + index)
                        .text(prose)
                        .textStyle(plain(CARD_BODY_SIZE, MUTED))
                        .lineSpacing(Math.max(0, px(22.5) - CARD_BODY_SIZE))
                        .margin(new DocumentInsets(gapAbove, 0, 0, 0)));
            }
        });
    }

    private static void renderPayment(SectionBuilder cell, InvoicePaymentBlock payment) {
        chrome(cell, 1375 - 1339, PAY_VALUE_SIZE);
        head(cell, "Payment", ObsidianIcons.PAYMENT, payment.heading());
        if (!payment.instruction().isBlank()) {
            cell.addParagraph(p -> p
                    .name("PaymentIntro")
                    .text(payment.instruction())
                    .textStyle(plain(CARD_BODY_SIZE, MUTED))
                    .lineSpacing(Math.max(0, px(19.4) - CARD_BODY_SIZE))
                    .margin(new DocumentInsets(Math.max(0, px(1221 - 1182) - CARD_ICON
                            - topBearing(CARD_BODY_SIZE, false)), 0, 0, 0)));
        }
        cell.addSection("PaymentFields", fields -> {
            fields.spacing(Math.max(0, px(22.7) - PAY_VALUE_SIZE))
                    .margin(new DocumentInsets(Math.max(0, capGap(
                            1271 - 1240, CARD_BODY_SIZE, false, PAY_LABEL_SIZE, false)),
                            0, 0, 0));
            List<InvoicePaymentBlock.Field> entries = payment.fields();
            for (int i = 0; i < entries.size(); i++) {
                InvoicePaymentBlock.Field field = entries.get(i);
                int index = i;
                String name = "PaymentField_" + index;
                layeredRow(fields, name, row -> {
                    row.spacing(0)
                            .columns(DocumentRowColumn.fixed(px(150)),
                                    DocumentRowColumn.weight(1));
                    row.addParagraph(p -> p
                            .name(name + "Label")
                            .text(field.label())
                            .textStyle(plain(PAY_LABEL_SIZE, MUTED)));
                    row.addParagraph(p -> linked(p
                                    .name(name + "Value")
                                    .text(field.value())
                                    .textStyle(plain(PAY_VALUE_SIZE, INK)),
                            // A bank detail is a reference, not a destination —
                            // except an address, the one field a reader would act on.
                            field.value().contains("@")
                                    ? InvoiceUri.mailLink(field.value())
                                    : null));
                });
            }
        });
    }

    /**
     * A card's fill, border and lower padding.
     *
     * <p>The padding is solved from the last line's cap to the card's measured
     * bottom, so a card with a different number of lines still closes the same
     * distance under the last of them.</p>
     */
    private static void chrome(SectionBuilder cell, double lastCapToBottomPx, double lastSize) {
        cell.spacing(0)
                .fillColor(SURFACE)
                .stroke(CARD_STROKE)
                .cornerRadius(CARD_RADIUS)
                .padding(new DocumentInsets(0, CARD_PAD_H,
                        Math.max(0, capGap(lastCapToBottomPx, lastSize, false, 0, false)),
                        CARD_PAD_H))
                .keepTogether();
    }

    private static void head(SectionBuilder cell, String prefix, String token, String heading) {
        layeredRow(cell, prefix + "Head", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(new DocumentInsets(px(1182 - 1157), 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(CARD_ICON_COL - CARD_PAD_H),
                            DocumentRowColumn.weight(1));
            row.addSection(prefix + "Icon", cell2 -> cell2
                    .spacing(0)
                    .add(glyph(ObsidianIcons.icon(token), token, CARD_ICON,
                            HorizontalAlign.LEFT)));
            row.addParagraph(p -> p
                    .name(prefix + "Heading")
                    .text(heading)
                    .textStyle(bold(CARD_HEAD_SIZE, ACCENT))
                    .margin(capTop(1187 - 1182, CARD_HEAD_SIZE, true)));
        });
    }
}
