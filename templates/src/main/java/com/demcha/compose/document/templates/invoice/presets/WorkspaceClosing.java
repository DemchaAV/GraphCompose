package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CLOSING_INNER_LIFT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CLOSING_RULE_LIFT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.FOOTER_LINK_COL;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.FOOTER_MARK_COL;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.FOOTER_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HAIRLINE_STRONG;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.THANKS_BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.THANKS_ICON;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.THANKS_ICON_COL;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.THANKS_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.THANKS_TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.blockGap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.style;

/**
 * What closes the sheet: the thank-you block and the brand band under it.
 *
 * <p>The band is body content rather than page chrome — it carries a wordmark
 * and a reachable address, and a header/footer zone takes only strings. Page
 * identity on a continuation page is carried by the page number instead.</p>
 */
final class WorkspaceClosing {

    private WorkspaceClosing() {
    }

    /**
     * Draws the closing blocks.
     *
     * @param page     the page flow
     * @param notes    the closing words and the address inside them
     * @param brand    the wordmark the band opens with
     * @param supplier who is invoicing, named again under the band
     */
    static void render(PageFlowBuilder page, InvoiceNotesBlock notes, InvoiceBrand brand,
                       InvoiceContactBlock supplier) {
        WorkspaceWidgets.fullWidthRule(page, "ClosingRule", HAIRLINE_STRONG, RULE_THIN,
                px(12.4) - CLOSING_RULE_LIFT, px(19.2) - CLOSING_INNER_LIFT);
        renderThankYou(page, notes);
        WorkspaceWidgets.fullWidthRule(page, "FooterRule", HAIRLINE_STRONG, RULE_THIN,
                blockGap(22.15, THANKS_BODY_SIZE, 0) - CLOSING_INNER_LIFT,
                blockGap(7.9, 0, FOOTER_SIZE));
        renderBand(page, brand, supplier);
    }

    /**
     * The closing words, opened by a mark in its own column so a wrapped second
     * line returns to the text axis rather than to the glyph's.
     *
     * <p>The address inside the sentence is a separate run, so it reaches the
     * PDF as a real annotation rather than as coloured text.</p>
     */
    private static void renderThankYou(PageFlowBuilder page, InvoiceNotesBlock notes) {
        List<String> paragraphs = notes.paragraphs();
        String title = notes.heading();
        String body = paragraphs.isEmpty() ? "" : paragraphs.get(0);
        page.addSection("ThankYou", block -> {
            block.spacing(0)
                    .padding(new DocumentInsets(0, 0, 0, THANKS_INDENT))
                    .keepWithNext();
            block.addRow("ThankYouRow", row -> {
                row.spacing(0)
                        .columns(DocumentRowColumn.fixed(THANKS_ICON_COL),
                                DocumentRowColumn.weight(1));
                row.addSection("ThankYouIcon", glyph -> glyph
                        .spacing(0)
                        .add(WorkspaceWidgets.glyph(WorkspaceIcons.INFO, THANKS_ICON,
                                HorizontalAlign.LEFT)));
                row.addSection("ThankYouText", text -> {
                    text.spacing(gap(24, THANKS_TITLE_SIZE));
                    text.addParagraph(p -> p
                            .name("ThankYouTitle")
                            .text(title)
                            .textStyle(style(THANKS_TITLE_SIZE, INK,
                                    DocumentTextDecoration.BOLD)));
                    text.addParagraph(p -> {
                        p.name("ThankYouBody");
                        DocumentTextStyle plain = style(THANKS_BODY_SIZE, MUTED,
                                DocumentTextDecoration.DEFAULT);
                        String email = notes.contactEmail();
                        if (email.isBlank() || !body.contains(email)) {
                            p.inlineText(body, plain);
                            return;
                        }
                        // The address is set apart inside the sentence, so the
                        // words on either side of it stay one paragraph.
                        int at = body.indexOf(email);
                        p.inlineText(body.substring(0, at), plain);
                        p.inlineText(email,
                                style(THANKS_BODY_SIZE, ACCENT, DocumentTextDecoration.DEFAULT),
                                new DocumentLinkOptions("mailto:" + email));
                        p.inlineText(body.substring(at + email.length()), plain);
                    });
                });
            });
        });
    }

    /** The brand band: the wordmark, who is invoicing, and where to find them. */
    private static void renderBand(PageFlowBuilder page, InvoiceBrand brand,
                                   InvoiceContactBlock supplier) {
        String website = supplier.website();
        page.addRow("DocumentFooter", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(FOOTER_MARK_COL),
                            DocumentRowColumn.weight(1),
                            DocumentRowColumn.fixed(FOOTER_LINK_COL));
            row.addParagraph(p -> p
                    .name("FooterWordmark")
                    .text(brand.name())
                    .textStyle(style(WORDMARK_SIZE, INK, DocumentTextDecoration.BOLD)));
            row.addSection("FooterAddress", text -> {
                text.spacing(gap(23, FOOTER_SIZE));
                text.addParagraph(p -> p
                        .name("FooterName")
                        .text(supplier.legalName())
                        .textStyle(style(FOOTER_SIZE, BODY, DocumentTextDecoration.BOLD)));
                text.addParagraph(p -> p
                        .name("FooterAddressLine")
                        .text(supplier.addressLines().isEmpty()
                                ? "" : String.join(", ", supplier.addressLines()))
                        .textStyle(style(FOOTER_SIZE, MUTED, DocumentTextDecoration.DEFAULT)));
            });
            row.addParagraph(p -> {
                p.name("FooterSite");
                DocumentTextStyle linkStyle =
                        style(FOOTER_SIZE, ACCENT, DocumentTextDecoration.DEFAULT);
                if (website.isBlank()) {
                    p.text("");
                } else {
                    p.inlineText(website, linkStyle, ContactUri.webLink(website));
                }
                p.align(TextAlign.RIGHT);
                p.margin(new DocumentInsets(0, px(3), 0, 0));
            });
        });
    }
}
