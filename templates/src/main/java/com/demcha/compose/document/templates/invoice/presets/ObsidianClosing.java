package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CLOSING_RIGHT_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CLOSING_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_CLOSING;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.disc;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.initials;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.initialsDisc;

/**
 * The closing band: a disc, the sign-off and the due sentence beside it, and the
 * issuer's own identity against the right margin.
 */
final class ObsidianClosing {

    private ObsidianClosing() {
    }

    /**
     * The band.
     *
     * <p>Bottom-aligned, not centred. Measured, the disc and both text columns
     * end on the same line — they sit on the disc's lower edge rather than on its
     * middle, and centring them lifts every line a few pixels.</p>
     *
     * @param page     the page flow
     * @param brand    the issuer's brand, whose logo the disc carries
     * @param supplier the issuer
     * @param payment  the sign-off and the due sentence
     */
    static void render(PageFlowBuilder page, InvoiceBrand brand,
                       InvoiceContactBlock supplier, InvoicePaymentBlock payment) {
        page.addRow("ClosingBand", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.BOTTOM)
                    .padding(new DocumentInsets(0, px(6), 0, px(5)))
                    .columns(DocumentRowColumn.fixed(px(73)),
                            DocumentRowColumn.weight(1),
                            DocumentRowColumn.fixed(px(160)));
            row.addSection("ClosingDisc", cell -> cell.spacing(0).add(closingDisc(brand, supplier)));
            row.addSection("ClosingText", text -> {
                text.spacing(Math.max(0, px(1448 - 1423) - LINE_BOX * CLOSING_SIZE));
                if (!payment.signOff().isBlank()) {
                    text.addParagraph(p -> p
                            .name("ClosingSignOff")
                            .text(payment.signOff())
                            .textStyle(bold(CLOSING_SIZE, INK)));
                }
                if (!payment.dueNotice().isBlank()) {
                    text.addParagraph(p -> {
                        p.name("ClosingDue");
                        writeEmphasised(p, payment.dueNotice(), payment.dueNoticeEmphasis());
                    });
                }
            });
            row.addSection("ClosingContact", text -> {
                text.spacing(Math.max(0, px(1448 - 1423) - LINE_BOX * CLOSING_RIGHT_SIZE));
                text.addParagraph(p -> p
                        .name("ClosingName")
                        .text(supplier.legalName())
                        .textStyle(plain(CLOSING_RIGHT_SIZE, INK))
                        .align(TextAlign.RIGHT));
                if (!supplier.email().isBlank()) {
                    text.addParagraph(p -> {
                        p.name("ClosingContact").align(TextAlign.RIGHT);
                        p.inlineText(supplier.email(), plain(CLOSING_RIGHT_SIZE, MUTED),
                                ContactUri.mailLink(supplier.email()));
                    });
                }
            });
        });
    }

    /** The same mark the issuer's card carries, at the band's smaller size. */
    private static DocumentNode closingDisc(InvoiceBrand brand, InvoiceContactBlock supplier) {
        double diameter = px(46);
        if (brand.logo() != null) {
            return disc("ClosingDiscShape", DISC_CLOSING, new ImageBuilder()
                    .name("ClosingDiscLogo")
                    .source(brand.logo())
                    .width(px(21))
                    .build(), diameter);
        }
        String monogram = (brand.monogramTop() + brand.monogramBottom()).trim();
        String letters = monogram.isBlank()
                ? initials(brand.name().isBlank() ? supplier.legalName() : brand.name())
                : monogram;
        return ObsidianWidgets.disc("ClosingDiscShape", DISC_CLOSING,
                new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .name("ClosingDiscInitials")
                        .text(letters)
                        .textStyle(bold(CLOSING_SIZE, INK))
                        .align(TextAlign.CENTER)
                        .build(),
                diameter);
    }

    /**
     * The due sentence, with the date inside it set in the accent.
     *
     * <p>The design colours the date rather than breaking it out, so it is a run
     * of the same sentence: the emphasis names a substring, the preset finds it,
     * and the text around it stays as it was written. A sentence that does not
     * contain the emphasis is written whole — the sheet still says what it
     * says.</p>
     */
    private static void writeEmphasised(com.demcha.compose.document.dsl.ParagraphBuilder paragraph,
                                        String prose, String emphasis) {
        int at = emphasis.isBlank() ? -1 : prose.indexOf(emphasis);
        if (at < 0) {
            paragraph.inlineText(prose, plain(CLOSING_SIZE, MUTED));
            return;
        }
        if (at > 0) {
            paragraph.inlineText(prose.substring(0, at), plain(CLOSING_SIZE, MUTED));
        }
        paragraph.inlineText(emphasis, bold(CLOSING_SIZE, ACCENT));
        int after = at + emphasis.length();
        if (after < prose.length()) {
            paragraph.inlineText(prose.substring(after), plain(CLOSING_SIZE, MUTED));
        }
    }
}
