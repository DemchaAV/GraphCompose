package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;

import java.util.Locale;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DIVIDER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_DUE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_DUE_ICON_COL;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_DUE_ICON_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_DUE_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_RULE_GAP_ABOVE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_RULE_GAP_BELOW;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_RULE_OVERHANG_L;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_RULE_OVERHANG_R;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SPLIT_X;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SUB_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SUPPORT_CELL_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SUPPORT_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SUPPORT_ICON_COL;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.FOOTER_SUPPORT_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_DISC_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.NOTES_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_TOP_BEARING;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.capOffset;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.glyphPad;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

/**
 * What closes the sheet: the notes, the rule under them, and the two-cell
 * document footer.
 *
 * <p>The footer is body content rather than page chrome: it carries glyphs and
 * a reachable address, and a header/footer zone takes only strings. Page
 * identity on a continuation page is carried by the page number instead.</p>
 */
final class PaymentsClosing {

    private PaymentsClosing() {
    }

    /**
     * Draws the closing blocks.
     *
     * @param page    the page flow
     * @param notes   the note block and the contacts the footer offers
     * @param payment the due notice and the closing line
     */
    static void render(PageFlowBuilder page, InvoiceNotesBlock notes,
                       InvoicePaymentBlock payment) {
        if (!notes.paragraphs().isEmpty()) {
            renderNotes(page, notes);
        }
        renderFooterRule(page);
        renderFooter(page, notes, payment);
    }

    /**
     * Glyph column and text column as two cells, so the second body line
     * returns to the text axis rather than to the glyph's. The notes introduce
     * the footer, and a page that ends on one and opens on the other reads as
     * two fragments — so they are kept together.
     */
    private static void renderNotes(PageFlowBuilder page, InvoiceNotesBlock notes) {
        page.addSection("Notes", block -> {
            block.spacing(0)
                    .padding(DocumentInsets.zero())
                    .margin(new DocumentInsets(px(1280 - 1252), 0, 0, NOTES_DISC_INSET))
                    .keepWithNext();
            block.addRow("NotesRow", row -> {
                row.spacing(0)
                        .verticalAlign(RowVerticalAlign.TOP)
                        .columns(DocumentRowColumn.fixed(NOTES_TEXT_INDENT),
                                DocumentRowColumn.weight(1));
                row.addSection("NotesDisc", disc -> disc
                        .spacing(0)
                        .add(PaymentsWidgets.disc(PaymentsIcons.DOCUMENT, NOTES_SURFACE,
                                NOTES_DISC_D, NOTES_GLYPH)));
                row.addSection("NotesText", text -> {
                    text.spacing(0);
                    text.addParagraph(p -> p
                            .name("NotesLabel")
                            .text(notes.heading())
                            .textStyle(style(NOTES_LABEL_SIZE, ACCENT,
                                    DocumentTextDecoration.BOLD))
                            .margin(new DocumentInsets(
                                    capOffset(1285 - 1280, NOTES_LABEL_SIZE), 0, 0, 0)));
                    text.addSection("NotesBody", lines -> {
                        lines.spacing(gap(NOTES_PITCH, PaymentsStyles.NOTES_BODY_SIZE))
                                .margin(new DocumentInsets(
                                        px(1307 - 1285) - LINE_BOX * NOTES_LABEL_SIZE
                                                + TEXT_TOP_BEARING * (NOTES_LABEL_SIZE
                                                - PaymentsStyles.NOTES_BODY_SIZE), 0, 0, 0));
                        PaymentsWidgets.textLines(lines, "NotesLine", notes.paragraphs(),
                                PaymentsStyles.NOTES_BODY_SIZE, MUTED);
                    });
                });
            });
        });
    }

    private static void renderFooterRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("FooterRule")
                .horizontal(FOOTER_RULE_W)
                .thickness(RULE_THIN)
                .color(RULE_STRONG)
                // One design px past the content box on the left, two on the
                // right — measured, and small enough to look like a rounding
                // error until the renderer refuses a line wider than its box.
                .margin(new DocumentInsets(
                        FOOTER_RULE_GAP_ABOVE,
                        -FOOTER_RULE_OVERHANG_R,
                        FOOTER_RULE_GAP_BELOW,
                        -FOOTER_RULE_OVERHANG_L)));
    }

    private static void renderFooter(PageFlowBuilder page, InvoiceNotesBlock notes,
                                     InvoicePaymentBlock payment) {
        page.addRow("DocumentFooter", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(FOOTER_SPLIT_X),
                            DocumentRowColumn.weight(1));
            row.addSection("FooterDue", cell -> renderDue(cell, payment));
            row.addSection("FooterSupport", cell -> {
                cell.spacing(0)
                        .borders(DocumentBorders.left(DocumentStroke.of(DIVIDER, RULE_THIN)))
                        .padding(new DocumentInsets(0, 0, 0, FOOTER_SUPPORT_CELL_INSET));
                renderSupport(cell, notes, payment);
            });
        });
    }

    /** When the money is wanted, and what to quote when sending it. */
    private static void renderDue(SectionBuilder cell, InvoicePaymentBlock payment) {
        cell.spacing(0).padding(new DocumentInsets(0, 0, 0, FOOTER_DUE_ICON_INSET));
        PaymentsWidgets.layeredRow(cell, "FooterDueRow", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(new DocumentInsets(px(1404 - 1397), 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(FOOTER_DUE_ICON_COL),
                            DocumentRowColumn.weight(1));
            row.addSection("FooterDueIcon", glyph -> glyph
                    .spacing(0)
                    // A disc centres its glyph, so the icon box's own padding
                    // cancels there. A bare glyph leading a text row does not:
                    // the padding sits between the column's edge and the ink and
                    // has to be taken back off.
                    .margin(new DocumentInsets(-glyphPad(37), 0, 0, -glyphPad(37)))
                    .add(PaymentsWidgets.glyphNode(PaymentsIcons.CALENDAR, FOOTER_DUE_ICON,
                            HorizontalAlign.LEFT)));
            row.addSection("FooterDueText", text -> {
                text.spacing(0);
                text.addParagraph(p -> p
                        .name("FooterDueTitle")
                        .text(dueTitle(payment))
                        .textStyle(style(FOOTER_DUE_TITLE_SIZE, INK,
                                DocumentTextDecoration.BOLD))
                        .margin(new DocumentInsets(
                                capOffset(1408 - 1404, FOOTER_DUE_TITLE_SIZE), 0, 0, 0)));
                if (!payment.instruction().isBlank()) {
                    text.addParagraph(p -> p
                            .name("FooterDueSubtitle")
                            .text(payment.instruction())
                            .textStyle(style(FOOTER_SUB_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT))
                            .margin(new DocumentInsets(
                                    px(1428 - 1408) - LINE_BOX * FOOTER_DUE_TITLE_SIZE
                                            + TEXT_TOP_BEARING
                                            * (FOOTER_DUE_TITLE_SIZE - FOOTER_SUB_SIZE),
                                    0, 0, 0)));
                }
            });
        });
    }

    /**
     * The design sets this line in capitals, so the preset uppercases whichever
     * of the two due strings the document filled — the emphasis when it has one,
     * the notice itself otherwise.
     */
    private static String dueTitle(InvoicePaymentBlock payment) {
        String text = payment.dueNoticeEmphasis().isBlank()
                ? payment.dueNotice()
                : payment.dueNoticeEmphasis();
        return text.toUpperCase(Locale.ROOT);
    }

    /** Who to ask, and how to reach them. */
    private static void renderSupport(SectionBuilder cell, InvoiceNotesBlock notes,
                                      InvoicePaymentBlock payment) {
        PaymentsWidgets.layeredRow(cell, "FooterSupportRow", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(new DocumentInsets(px(1405 - 1397), 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(FOOTER_SUPPORT_ICON_COL),
                            DocumentRowColumn.weight(1));
            row.addSection("FooterSupportIcon", glyph -> glyph
                    .spacing(0)
                    .margin(new DocumentInsets(-glyphPad(39), 0, 0, -glyphPad(39)))
                    .add(PaymentsWidgets.glyphNode(PaymentsIcons.SUPPORT, FOOTER_SUPPORT_ICON,
                            HorizontalAlign.LEFT)));
            row.addSection("FooterSupportText", text -> {
                text.spacing(0);
                if (!payment.signOff().isBlank()) {
                    text.addParagraph(p -> p
                            .name("FooterSupportTitle")
                            .text(payment.signOff())
                            .textStyle(style(FOOTER_SUPPORT_TITLE_SIZE, INK,
                                    DocumentTextDecoration.BOLD))
                            .margin(new DocumentInsets(
                                    capOffset(1408 - 1405, FOOTER_SUPPORT_TITLE_SIZE), 0, 0, 0)));
                }
                // Separate runs, so each address reaches the PDF as a real
                // annotation rather than as coloured text.
                text.addParagraph(p -> {
                    p.name("FooterSupportContacts");
                    boolean hasEmail = !notes.contactEmail().isBlank();
                    if (hasEmail) {
                        p.inlineText(notes.contactEmail(),
                                style(FOOTER_SUB_SIZE, ACCENT, DocumentTextDecoration.DEFAULT),
                                new DocumentLinkOptions("mailto:" + notes.contactEmail()));
                    }
                    if (!notes.contactPhone().isBlank()) {
                        if (hasEmail) {
                            p.inlineText("    |    ", style(FOOTER_SUB_SIZE, MUTED,
                                    DocumentTextDecoration.DEFAULT));
                        }
                        String dialled = ContactUri.tel(notes.contactPhone());
                        if (dialled == null) {
                            p.inlineText(notes.contactPhone(), style(FOOTER_SUB_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT));
                        } else {
                            p.inlineText(notes.contactPhone(), style(FOOTER_SUB_SIZE, BODY,
                                    DocumentTextDecoration.DEFAULT),
                                    new DocumentLinkOptions(dialled));
                        }
                    }
                    p.margin(new DocumentInsets(
                            px(1432 - 1408) - LINE_BOX * FOOTER_SUPPORT_TITLE_SIZE
                                    + TEXT_TOP_BEARING
                                    * (FOOTER_SUPPORT_TITLE_SIZE - FOOTER_SUB_SIZE), 0, 0, 0));
                });
            });
        });
    }
}
