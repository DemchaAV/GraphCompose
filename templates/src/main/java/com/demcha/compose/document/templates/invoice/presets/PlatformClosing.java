package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
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

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CAP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.FOOTER_LOCKUP_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.NOTE_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.NOTE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TINY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;

/**
 * The foot of the sheet: a rule, the closing note beside its mark, a second
 * rule, and the identity band pairing the lockup against the issuer's address.
 */
final class PlatformClosing {

    private PlatformClosing() {
    }

    /** The rule above the closing note. */
    static void renderNoteRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("ClosingNoteRule")
                .horizontal(CONTENT_W)
                .thickness(RULE_THICK)
                .color(HAIRLINE)
                .margin(new DocumentInsets(py(1364 - 1350), 0, 0, 0)));
    }

    /**
     * The closing note.
     *
     * <p>The note tells a reader where to go, and the places it can send them are
     * printed inside the sentence. Written as one string the whole sentence would
     * have to carry the target or none of it would, so each address it actually
     * contains becomes its own run, in the accent, and the text between them stays
     * plain.</p>
     */
    static void renderNote(PageFlowBuilder page, InvoiceNotesBlock notes,
                           InvoiceContactBlock supplier) {
        if (notes.heading().isBlank() && notes.paragraphs().isEmpty()) {
            return;
        }
        page.addRow("ClosingNote", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(NOTE_GUTTER), DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(
                            py(1392 - 1365) - CAP_INSET * NOTE_SIZE, 0, 0, 0));
            row.addSection("ClosingNoteIcon", gutter -> {
                gutter.spacing(0);
                gutter.addSvgIcon(PlatformIcons.icon(PlatformIcons.INFO_OUTLINE), NOTE_ICON);
            });
            row.addSection("ClosingNoteText", text -> {
                text.spacing(capPitch(22, NOTE_SIZE))
                        .keepWithNext();
                if (!notes.heading().isBlank()) {
                    text.addParagraph(p -> p
                            .name("ClosingNoteLead")
                            .text(notes.heading())
                            .textStyle(style(NOTE_SIZE, BODY)));
                }
                List<String> paragraphs = notes.paragraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    String prose = paragraphs.get(i);
                    int index = i;
                    text.addParagraph(p -> {
                        p.name("ClosingNoteLine_" + index);
                        writeReachable(p, prose, notes, supplier);
                    });
                }
            });
        });
    }

    /** The rule above the identity band. */
    static void renderIdentityRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("FooterIdentityRule")
                .horizontal(CONTENT_W)
                .thickness(RULE_THICK)
                .color(HAIRLINE)
                .margin(new DocumentInsets(
                        py(1447 - 1414) - (LINE_BOX - CAP_INSET) * NOTE_SIZE, 0, 0, 0)));
    }

    /**
     * The identity band: the lockup again, and the issuer's own name and address
     * set against the right margin.
     */
    static void renderIdentity(PageFlowBuilder page, InvoiceBrand brand,
                               InvoiceContactBlock supplier) {
        page.addRow("FooterIdentity", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(FOOTER_LOCKUP_W),
                            DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(py(1465 - 1448), 0, 0, px(1)));
            row.addSection("FooterLockup", cell -> {
                cell.spacing(0);
                if (brand.logo() != null) {
                    cell.add(new ImageBuilder()
                            .name("FooterLogo")
                            .source(brand.logo())
                            .width(FOOTER_LOCKUP_W)
                            .build());
                } else if (!brand.name().isBlank()) {
                    cell.addParagraph(p -> p
                            .name("FooterWordmark")
                            .text(brand.name())
                            .textStyle(bold(NOTE_SIZE, INK)));
                }
            });
            row.addSection("FooterAddress", text -> {
                text.spacing(capPitch(20, TINY_SIZE))
                        .margin(new DocumentInsets(
                                py(1471 - 1465) - CAP_INSET * TINY_SIZE, 0, 0, 0));
                text.addParagraph(p -> p
                        .name("FooterIdentityName")
                        .text(supplier.legalName())
                        .textStyle(style(TINY_SIZE, MUTED))
                        .align(TextAlign.RIGHT));
                text.addParagraph(p -> p
                        .name("FooterIdentityAddress")
                        .text(String.join(", ", supplier.addressLines()))
                        .textStyle(style(TINY_SIZE, MUTED))
                        .align(TextAlign.RIGHT));
            });
        });
    }

    private static void writeReachable(ParagraphBuilder paragraph, String prose,
                                       InvoiceNotesBlock notes, InvoiceContactBlock supplier) {
        List<Reachable> found = new ArrayList<>();
        addIfPresent(found, prose, notes.contactEmail(), ContactUri.mailLink(notes.contactEmail()));
        addIfPresent(found, prose, supplier.email(), ContactUri.mailLink(supplier.email()));
        addIfPresent(found, prose, supplier.website(), ContactUri.webLink(supplier.website()));
        addIfPresent(found, prose, notes.contactPhone(), ContactUri.telLink(notes.contactPhone()));
        found.sort(Comparator.comparingInt(Reachable::at));

        int cursor = 0;
        for (Reachable reachable : found) {
            if (reachable.at() < cursor) {
                // An address overlapping one already written — the site inside an
                // address, say. The first one wins; writing both would duplicate
                // the glyphs.
                continue;
            }
            if (reachable.at() > cursor) {
                paragraph.inlineText(prose.substring(cursor, reachable.at()),
                        style(NOTE_SIZE, BODY));
            }
            paragraph.inlineText(reachable.text(), style(NOTE_SIZE, ACCENT), reachable.link());
            cursor = reachable.at() + reachable.text().length();
        }
        if (cursor < prose.length()) {
            paragraph.inlineText(prose.substring(cursor), style(NOTE_SIZE, BODY));
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
