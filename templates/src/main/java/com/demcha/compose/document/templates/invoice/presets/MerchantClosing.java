package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CLOSING_BOLD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CLOSING_NOTE_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CLOSING_REG_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_ADDR_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_ADDR_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_MARK_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_NAME_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_ROW_TOP;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_RULE_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_SITE_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_SITE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_TEXT_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_TILE_H;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.FOOTER_TILE_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.NOTE_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.NOTE_LINE1_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.NOTE_LINE2_CAP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.NOTE_TOP_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ON_FILL;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.PANEL_BOTTOM_Y;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TILE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.style;

/**
 * The foot of the sheet: a rule, the closing note beside its mark, a second
 * rule, and the identity band pairing a marked tile against the issuer's own
 * details.
 *
 * <h2>What this port leaves out</h2>
 *
 * <p>The design closes with a row of three discs carrying social-platform marks.
 * Those marks belong to their platforms, and the templates artifact does not
 * redistribute other companies' trademarks, so neither they nor the rule that
 * divided them off are drawn. The band keeps the tile and the issuer's identity,
 * which is what identifies the sheet.</p>
 */
final class MerchantClosing {

    private MerchantClosing() {
    }

    /** The rule above the closing note. */
    static void renderNoteRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("ClosingNoteRule")
                .horizontal(CONTENT_W)
                .thickness(RULE_THIN)
                .color(RULE_SOFT)
                .margin(new DocumentInsets(py(1331 - PANEL_BOTTOM_Y), 0, 0, 0)));
    }

    /**
     * The closing note: a lead line and the prose under it, with any address the
     * prose names made reachable.
     */
    static void renderNote(PageFlowBuilder page, InvoiceNotesBlock notes,
                           InvoiceContactBlock supplier) {
        if (notes.heading().isBlank() && notes.paragraphs().isEmpty()) {
            return;
        }
        page.addRow("ClosingNote", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(NOTE_GUTTER), DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(py(NOTE_TOP_Y - 1331) - RULE_BOX, 0, 0, 0));
            row.addSection("ClosingNoteIcon", gutter -> {
                gutter.spacing(0);
                gutter.addSvgIcon(MerchantIcons.icon(MerchantIcons.INFO), NOTE_ICON);
            });
            row.addSection("ClosingNoteText", text -> {
                text.spacing(capGap(NOTE_LINE2_CAP_Y - NOTE_LINE1_CAP_Y,
                                CLOSING_BOLD_SIZE, true, CLOSING_REG_SIZE, false))
                        .keepWithNext()
                        .margin(capTop(NOTE_LINE1_CAP_Y - NOTE_TOP_Y, CLOSING_BOLD_SIZE, true));
                if (!notes.heading().isBlank()) {
                    text.addParagraph(p -> p
                            .name("ClosingNoteLead")
                            .text(notes.heading())
                            .textStyle(bold(CLOSING_BOLD_SIZE, INK)));
                }
                List<String> paragraphs = notes.paragraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    String prose = paragraphs.get(i);
                    int index = i;
                    text.addParagraph(p -> {
                        p.name("ClosingNoteProse_" + index);
                        writeReachable(p, prose, notes, supplier);
                    });
                }
            });
        });
    }

    /** The rule above the identity band. */
    static void renderIdentityRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("FooterRule")
                .horizontal(CONTENT_W)
                .thickness(RULE_THIN)
                .color(RULE_SOFT)
                .margin(new DocumentInsets(
                        Math.max(0, py(FOOTER_RULE_Y - NOTE_TOP_Y) - CLOSING_NOTE_H), 0, 0, 0)));
    }

    /**
     * The identity band: the issuer's mark on a filled tile, and its name,
     * address and site beside it.
     */
    static void renderIdentity(PageFlowBuilder page, InvoiceBrand brand,
                               InvoiceContactBlock supplier) {
        page.addRow("FooterIdentity", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(FOOTER_TILE_W),
                            DocumentRowColumn.weight(1));
            row.add(tile(brand));
            row.addSection("FooterText", text -> {
                text.spacing(0).padding(new DocumentInsets(0, 0, 0, FOOTER_TEXT_PAD_L));
                text.addParagraph(p -> p
                        .name("FooterName")
                        .text(supplier.legalName())
                        .textStyle(bold(FOOTER_NAME_SIZE, INK))
                        .margin(new DocumentInsets(
                                py(FOOTER_NAME_CAP_Y) - FOOTER_ROW_TOP
                                        - MerchantStyles.topBearing(FOOTER_NAME_SIZE, true),
                                0, 0, 0)));
                text.addParagraph(p -> p
                        .name("FooterAddress")
                        .text(String.join(", ", supplier.addressLines()))
                        .textStyle(style(FOOTER_ADDR_SIZE, INK))
                        .margin(new DocumentInsets(
                                capGap(FOOTER_ADDR_CAP_Y - FOOTER_NAME_CAP_Y,
                                        FOOTER_NAME_SIZE, true, FOOTER_ADDR_SIZE, false),
                                0, 0, 0)));
                if (!supplier.website().isBlank()) {
                    text.addParagraph(p -> {
                        p.name("FooterSite");
                        p.text(supplier.website());
                        p.textStyle(style(FOOTER_SITE_SIZE, ACCENT));
                        p.link(ContactUri.webLink(supplier.website()));
                        p.margin(new DocumentInsets(
                                capGap(FOOTER_SITE_CAP_Y - FOOTER_ADDR_CAP_Y,
                                        FOOTER_ADDR_SIZE, false, FOOTER_SITE_SIZE, false),
                                0, 0, 0));
                    });
                }
            });
        });
    }

    /**
     * The filled tile the issuer's mark sits on.
     *
     * <p>The mark is the caller's: the tile carries the brand's logo when there
     * is one and the brand's monogram otherwise. The templates artifact carries
     * no mark of its own, and the tile is the design's shape rather than a
     * container for a particular drawing.</p>
     */
    private static DocumentNode tile(InvoiceBrand brand) {
        DocumentNode content = brand.logo() != null
                ? new ImageBuilder()
                        .name("FooterMark")
                        .source(brand.logo())
                        .width(FOOTER_MARK_W)
                        .build()
                : new ParagraphBuilder()
                        .name("FooterMonogram")
                        .text(monogram(brand))
                        .textStyle(bold(FOOTER_NAME_SIZE, ON_FILL))
                        .align(TextAlign.CENTER)
                        .build();
        return new ShapeContainerBuilder()
                .name("FooterTile")
                .roundedRect(FOOTER_TILE_W, FOOTER_TILE_H, TILE_RADIUS)
                .fillColor(INK)
                .center(content)
                .build();
    }

    private static String monogram(InvoiceBrand brand) {
        String stated = (brand.monogramTop() + brand.monogramBottom()).trim();
        if (!stated.isBlank()) {
            return stated;
        }
        String name = brand.name().trim();
        return name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(java.util.Locale.ENGLISH);
    }

    private static void writeReachable(ParagraphBuilder paragraph, String prose,
                                       InvoiceNotesBlock notes, InvoiceContactBlock supplier) {
        List<Reachable> found = new ArrayList<>();
        addIfPresent(found, prose, notes.contactEmail(), ContactUri.mailLink(notes.contactEmail()));
        addIfPresent(found, prose, supplier.email(), ContactUri.mailLink(supplier.email()));
        addIfPresent(found, prose, supplier.website(), ContactUri.webLink(supplier.website()));
        found.sort(Comparator.comparingInt(Reachable::at));

        int cursor = 0;
        for (Reachable reachable : found) {
            if (reachable.at() < cursor) {
                continue;
            }
            if (reachable.at() > cursor) {
                paragraph.inlineText(prose.substring(cursor, reachable.at()),
                        style(CLOSING_REG_SIZE, INK));
            }
            paragraph.inlineText(reachable.text(), style(CLOSING_REG_SIZE, ACCENT),
                    reachable.link());
            cursor = reachable.at() + reachable.text().length();
        }
        if (cursor < prose.length()) {
            paragraph.inlineText(prose.substring(cursor), style(CLOSING_REG_SIZE, INK));
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
