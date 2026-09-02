package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_BODY;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_LEAD;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_LINE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.NOTE_RULE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.RULE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_CLOSING_TO_NOTE;

/**
 * The closing note: a rule, a mark, a lead line and the prose under it.
 */
final class MeteredClosing {

    private MeteredClosing() {
    }

    /**
     * The note.
     *
     * @param body     the page's body section
     * @param notes    the lead and its prose
     * @param supplier the addresses the prose may name
     */
    static void render(SectionBuilder body, InvoiceNotesBlock notes,
                       InvoiceContactBlock supplier) {
        if (notes.heading().isBlank() && notes.paragraphs().isEmpty()) {
            return;
        }
        body.addSection("ClosingNote", block -> {
            block.margin(SEAM_CLOSING_TO_NOTE);
            block.keepTogether();
            block.spacing(NOTE_RULE_GAP);
            block.addDivider(d -> d
                    .name("ClosingNoteRule")
                    .width(CONTENT_W)
                    .thickness(HAIRLINE)
                    .color(RULE));
            block.addRow("ClosingNoteBody", row -> row
                    .padding(new DocumentInsets(0, 0, 0, NOTE_INSET))
                    .columns(DocumentRowColumn.fixed(NOTE_ICON + NOTE_ICON_GAP),
                            DocumentRowColumn.auto())
                    .addSection("ClosingNoteIcon", cell -> cell
                            .addSvgIcon(MeteredIcons.icon(MeteredIcons.INFO), NOTE_ICON))
                    .addSection("ClosingNoteText", text -> {
                        text.spacing(NOTE_LINE_GAP);
                        if (!notes.heading().isBlank()) {
                            text.addParagraph(p -> p
                                    .name("ClosingNoteLead")
                                    .text(notes.heading())
                                    .textStyle(NOTE_LEAD));
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
                    }));
        });
    }

    /**
     * The prose, with every address it names made reachable.
     *
     * <p>The note tells a reader where to go, and the places it can send them —
     * the support address, the site — are printed inside the sentence. Written
     * as one string the whole sentence would have to carry the target or none of
     * it would, so each address it actually contains becomes its own run and the
     * text between them stays plain. The glyphs and their order are what the
     * sentence gave.</p>
     */
    private static void writeReachable(ParagraphBuilder paragraph, String prose,
                                       InvoiceNotesBlock notes, InvoiceContactBlock supplier) {
        List<Reachable> found = new ArrayList<>();
        addIfPresent(found, prose, notes.contactEmail(), InvoiceUri.mailLink(notes.contactEmail()));
        addIfPresent(found, prose, supplier.email(), InvoiceUri.mailLink(supplier.email()));
        addIfPresent(found, prose, notes.contactPhone(), InvoiceUri.telLink(notes.contactPhone()));
        addIfPresent(found, prose, supplier.website(), InvoiceUri.webLink(supplier.website()));
        found.sort(Comparator.comparingInt(Reachable::at));

        int cursor = 0;
        for (Reachable reachable : found) {
            if (reachable.at() < cursor) {
                // An address that overlaps one already written — the site inside
                // an address, say. The first one wins; writing both would
                // duplicate the glyphs.
                continue;
            }
            if (reachable.at() > cursor) {
                paragraph.inlineText(prose.substring(cursor, reachable.at()), NOTE_BODY);
            }
            paragraph.inlineText(reachable.text(), NOTE_BODY, reachable.link());
            cursor = reachable.at() + reachable.text().length();
        }
        if (cursor < prose.length()) {
            paragraph.inlineText(prose.substring(cursor), NOTE_BODY);
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
