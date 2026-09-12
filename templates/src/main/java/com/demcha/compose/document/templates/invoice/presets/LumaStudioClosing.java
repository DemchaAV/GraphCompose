package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceTotalsBlock;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANK_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANK_PIPE_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANK_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANK_VALUE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANK_VALUE_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_CONTENT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_DISC_ICON_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_DISC_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_DUE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_FLOW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_SIGN_OFF;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BANNER_TEXT_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CLOSING_DIVIDER_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CLOSING_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.HAIRLINE_QUIET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.INK_SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.NOTE_TEXT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_RIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PANEL_HIGHLIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAPER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_HEADING_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SMALL_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TABLE_TO_TOTALS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTALS_AMOUNT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTALS_LABEL_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTALS_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTALS_TO_RULE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTALS_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTAL_AMOUNT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTAL_DUE_AMOUNT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTAL_DUE_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTAL_DUE_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TOTAL_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_TOTAL_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.leading;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.money;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.disc;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.paragraph;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.sectionHead;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.tracked;

/**
 * What closes the sheet: the totals stack, the notes and payment pair, and
 * the sign-off band.
 */
final class LumaStudioClosing {

    private LumaStudioClosing() {
    }

    // -- totals ----------------------------------------------------------

    /**
     * The totals stack, right-aligned by its own left margin, closing on the
     * filled total-due band.
     */
    static void renderTotals(SectionBuilder section, InvoiceTotalsBlock totals,
                             String currencySymbol) {
        section.name("Totals")
                .keepTogether()
                .spacing(0)
                .margin(new DocumentInsets(
                        TABLE_TO_TOTALS, 0, 0, CONTENT_WIDTH - TOTALS_WIDTH));
        for (InvoiceTotalsBlock.Row row : totals.rows()) {
            totalsRow(section, row.label(), money(currencySymbol, row.amount()), false);
        }
        totalsRow(section, totals.totalLabel(),
                money(currencySymbol, totals.totalAmount()), true);
    }

    private static void totalsRow(SectionBuilder section, String label, String amount,
                                  boolean emphasised) {
        section.addContainer(container -> {
            container.name(emphasised ? "TotalDueBand" : "TotalsRow")
                    .rectangle(TOTALS_WIDTH, emphasised ? TOTAL_DUE_HEIGHT : TOTALS_ROW_HEIGHT)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .position(tracked("TotalsLabel", label,
                                    emphasised ? TOTAL_DUE_LABEL : TOTAL_LABEL,
                                    TRACK_TOTAL_LABEL,
                                    emphasised ? PANEL_HIGHLIGHT : PAPER),
                            TOTALS_LABEL_INSET, 0, LayerAlign.CENTER_LEFT)
                    .position(paragraph(amount,
                                    emphasised ? TOTAL_DUE_AMOUNT : TOTAL_AMOUNT,
                                    TextAlign.RIGHT),
                            -TOTALS_AMOUNT_INSET, 0, LayerAlign.CENTER_RIGHT);
            if (emphasised) {
                container.fillColor(PANEL_HIGHLIGHT)
                        .margin(new DocumentInsets(LINE_PITCH * 0.23, 0, 0, 0));
            }
        });
    }

    /** The hairline that closes the totals and opens the pair below. */
    static void renderFooterRule(LineBuilder line) {
        line.name("FooterRule")
                .horizontal(CONTENT_WIDTH)
                .thickness(RULE_THICKNESS)
                .color(HAIRLINE)
                .margin(new DocumentInsets(TOTALS_TO_RULE, 0, 0, 0));
    }

    // -- notes and payment -----------------------------------------------

    /** The notes column: the disc heading, then the paragraphs under it. */
    static void renderNotes(SectionBuilder section, InvoiceNotesBlock notes) {
        section.name("Notes").keepTogether().spacing(0);
        sectionHead(section, LumaStudioIcons.NOTES, notes.heading(),
                CONTENT_WIDTH * CLOSING_LEFT_WEIGHT);
        SectionBuilder body = new SectionBuilder()
                .name("NotesBody")
                .spacing(LINE_PITCH * 0.75)
                .padding(new DocumentInsets(0, 0, 0, SECTION_HEADING_OFFSET))
                .margin(new DocumentInsets(LINE_PITCH * 0.18, 0, 0, 0));
        for (String note : notes.paragraphs()) {
            body.addParagraph(p -> p
                    .text(note)
                    .textStyle(NOTE_TEXT)
                    .lineSpacing(leading(LINE_PITCH * 0.91, NOTE_TEXT))
                    .margin(DocumentInsets.zero()));
        }
        section.add(body.build());
    }

    /**
     * The payment column: the disc heading, the instruction, who is paid, and
     * the bank fields as label / pipe / value on one axis.
     *
     * <p>The divider between the two columns is this column's own left
     * border, so it takes its height from what this column holds.</p>
     */
    static void renderPayment(SectionBuilder section, InvoicePaymentBlock payment) {
        section.name("PaymentDetails")
                .keepTogether()
                .spacing(0)
                .accentLeft(HAIRLINE_QUIET, RULE_THICKNESS)
                .padding(new DocumentInsets(0, 0, 0, CLOSING_DIVIDER_INSET));
        double blockWidth = CONTENT_WIDTH * (1.0 - CLOSING_LEFT_WEIGHT) - CLOSING_DIVIDER_INSET;
        sectionHead(section, LumaStudioIcons.BANK, payment.heading(), blockWidth);

        SectionBuilder body = new SectionBuilder()
                .name("PaymentBody")
                .spacing(0)
                .padding(new DocumentInsets(0, 0, 0, SECTION_HEADING_OFFSET))
                .margin(new DocumentInsets(LINE_PITCH * 0.18, 0, 0, 0));
        body.addParagraph(p -> p
                        .name("PaymentInstruction")
                        .text(payment.instruction())
                        .textStyle(NOTE_TEXT)
                        .lineSpacing(0)
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .name("AccountHolder")
                        .text(payment.accountHolder())
                        .textStyle(SMALL_BOLD)
                        .lineSpacing(0)
                        .margin(new DocumentInsets(
                                LINE_PITCH * 0.45, 0, LINE_PITCH * 0.55, 0)));

        double fieldWidth = blockWidth - SECTION_HEADING_OFFSET;
        for (InvoicePaymentBlock.Field field : payment.fields()) {
            body.addContainer(container -> container
                    .name("PaymentField")
                    .rectangle(fieldWidth, BANK_ROW_HEIGHT)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .centerLeft(paragraph(field.label(), BANK_LABEL, TextAlign.LEFT))
                    .position(pipe(), BANK_PIPE_OFFSET, 0, LayerAlign.CENTER_LEFT)
                    .position(paragraph(field.value(), BANK_VALUE, TextAlign.LEFT),
                            BANK_VALUE_OFFSET, 0, LayerAlign.CENTER_LEFT));
        }
        section.add(body.build());
    }

    private static DocumentNode pipe() {
        return new LineBuilder()
                .name("BankPipe")
                .vertical(BANK_ROW_HEIGHT * 0.66)
                .thickness(0.8)
                .color(ACCENT)
                .margin(DocumentInsets.zero())
                .build();
    }

    // -- the closing band ------------------------------------------------

    /**
     * The sign-off band: the dark strip, the heart on its disc, and the two
     * closing lines.
     *
     * <p>The strip is drawn by this block rather than left to the page
     * background that closes every page. On a one-page invoice the two
     * coincide exactly — the flow ends at the foot, so the block lands on the
     * background band and nothing about the sheet changes. On a longer one the
     * flow ends wherever the closing blocks end, and a block that relied on the
     * background would set white words on pale paper. Carrying its own ground
     * is what makes the sign-off legible on whatever page it lands.</p>
     *
     * <p>The strip bleeds to both paper edges through negative side margins,
     * and the ground is a layer rather than the container's fill: a container's
     * own fill is dropped under a large negative margin. The block's height in
     * the flow is almost nothing — it reaches down into the margin the page
     * reserves for the band instead of pushing the flow.</p>
     *
     * <p>It is flow content only because footer chrome carries text today: one
     * size and one colour across three slots, and no glyph, where this band
     * needs two faces and a disc. Once a footer zone can hold a node, the
     * sign-off belongs in one — pinned to the foot of the last page, where the
     * design draws it — and this block and its strip go away.</p>
     */
    static void renderBanner(SectionBuilder section, InvoicePaymentBlock payment) {
        section.name("ClosingBanner").keepTogether().spacing(0);
        SectionBuilder lines = new SectionBuilder().name("BannerLines").spacing(0);
        lines.addParagraph(p -> p
                        .name("SignOff")
                        .text(payment.signOff())
                        .textStyle(BANNER_SIGN_OFF)
                        .lineSpacing(0)
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .name("DueNotice")
                        .text(payment.dueNotice())
                        .textStyle(BANNER_DUE)
                        .lineSpacing(0)
                        .margin(new DocumentInsets(LINE_PITCH * 0.25, 0, 0, 0)));
        section.addContainer(container -> container
                .name("BannerContent")
                .rectangle(PAGE.width(), BANNER_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(
                        0, -PAGE_MARGIN_RIGHT, BANNER_FLOW_HEIGHT - BANNER_HEIGHT,
                        -PAGE_MARGIN_LEFT))
                .position(band(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(disc(BANNER_DISC_SIZE, LumaStudioIcons.HEART, BANNER_DISC_ICON_SIZE),
                        BANNER_CONTENT_INSET, 0, LayerAlign.CENTER_LEFT, 1)
                .position(lines.build(), BANNER_CONTENT_INSET + BANNER_TEXT_OFFSET, 0,
                        LayerAlign.CENTER_LEFT, 1));
    }

    /** The strip the sign-off is set on, paper edge to paper edge. */
    private static DocumentNode band() {
        return new ShapeBuilder()
                .name("BannerBand")
                .size(PAGE.width(), BANNER_HEIGHT)
                .fillColor(INK_SURFACE)
                .margin(DocumentInsets.zero())
                .build();
    }
}
