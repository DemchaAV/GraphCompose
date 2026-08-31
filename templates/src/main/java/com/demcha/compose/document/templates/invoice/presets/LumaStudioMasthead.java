package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CONTACT_ICON_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CONTACT_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.CONTACT_TEXT_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.HAIRLINE_QUIET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.INVOICE_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.META_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.META_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.META_VALUE_SPLIT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.META_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MASTHEAD_TO_RULE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MICRO_MUTED;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAPER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PARTIES_DIVIDER_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_HEADING;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SUPPLIER_NAME;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SUPPLIER_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TEXT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TEXT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TITLE_BLOCK_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TITLE_CAP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TITLE_RULE_THICKNESS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TITLE_RULE_TO_METADATA;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TITLE_RULE_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_META_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_SECTION_HEADING;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.leading;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.linkedParagraph;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.paragraph;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.tracked;

/**
 * The top of the sheet: the sender's block on the left, the title and its
 * metadata on the right, and the two party blocks below the rule.
 */
final class LumaStudioMasthead {

    /** The break this sheet stacks address lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    /** A parenthesised trunk prefix, which an international dial target drops. */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private LumaStudioMasthead() {
    }

    // -- supplier --------------------------------------------------------

    /**
     * The sender: the legal name, the address, the contact rows behind their
     * marks, and the registration line.
     *
     * <p>The channels carry {@code tel:} and {@code mailto:} targets built
     * from the values, and the website its own {@code https:} — so a reader
     * can act on them and the document does not carry each address twice.</p>
     */
    static void renderSupplier(SectionBuilder section, InvoiceContactBlock supplier) {
        section.name("SupplierHeader")
                .spacing(0)
                .padding(new DocumentInsets(0, 0, 0, TEXT_INSET));
        section.addParagraph(p -> p
                .name("SupplierName")
                .text(supplier.legalName())
                .textStyle(SUPPLIER_NAME)
                .lineSpacing(0)
                .margin(DocumentInsets.zero()));
        section.addParagraph(p -> p
                .name("SupplierAddress")
                .text(String.join(NEWLINE, supplier.addressLines()))
                .textStyle(BODY)
                .lineSpacing(leading(LINE_PITCH, BODY))
                .margin(new DocumentInsets(LINE_PITCH * 0.50, 0, 0, 0)));

        SectionBuilder contacts = new SectionBuilder()
                .name("ContactRows")
                .spacing(0)
                .margin(new DocumentInsets(LINE_PITCH * 0.79, 0, 0, 0));
        contactRow(contacts, LumaStudioIcons.PHONE, supplier.phone(),
                telUri(supplier.phone()));
        contactRow(contacts, LumaStudioIcons.EMAIL, supplier.email(),
                supplier.email().isBlank() ? null : "mailto:" + supplier.email());
        contactRow(contacts, LumaStudioIcons.WEBSITE, supplier.website(),
                webUri(supplier.website()));
        section.add(contacts.build());

        section.addParagraph(p -> p
                .name("Registration")
                .text(registrationLine(supplier))
                .textStyle(MICRO_MUTED)
                .lineSpacing(0)
                .margin(new DocumentInsets(LINE_PITCH * 0.84, 0, 0, 0)));
    }

    private static void contactRow(SectionBuilder section, String iconToken,
                                   String value, String href) {
        section.addContainer(container -> container
                .name("ContactRow")
                .rectangle(SUPPLIER_WIDTH, CONTACT_ROW_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(LumaStudioIcons.icon(iconToken, CONTACT_ICON_SIZE))
                .position(linkedParagraph(value, href, BODY), CONTACT_TEXT_OFFSET, 0,
                        LayerAlign.CENTER_LEFT));
    }

    /**
     * The registration line: one labelled number, or both behind a pale pipe
     * when the sender carries a tax registration as well.
     */
    private static String registrationLine(InvoiceContactBlock supplier) {
        String first = pair(supplier.registrationLabel(), supplier.registrationNumber());
        String second = pair(supplier.taxRegistrationLabel(), supplier.taxRegistrationNumber());
        if (first.isBlank()) {
            return second;
        }
        return second.isBlank() ? first : first + "   |   " + second;
    }

    private static String pair(String label, String value) {
        if (label.isBlank() && value.isBlank()) {
            return "";
        }
        return label.isBlank() ? value : label + "  " + value;
    }

    /**
     * The dial target for a phone number: its digits, keeping a leading
     * {@code +} so an international number stays international.
     *
     * <p>A parenthesised trunk prefix — the {@code (0)} in
     * {@code +44 (0)20 7946 0832} — is the digit a caller drops when dialling
     * from abroad, so it is dropped here too. A parenthesised area code is
     * not, which is why only an all-zero group goes.</p>
     */
    private static String telUri(String phone) {
        String dialled = TRUNK_PREFIX.matcher(phone).replaceAll("");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    /** A website target: what the document wrote, given a scheme if it has none. */
    private static String webUri(String website) {
        if (website.isBlank()) {
            return null;
        }
        return website.startsWith("http") ? website : "https://" + website;
    }

    // -- title and metadata ----------------------------------------------

    /**
     * The document title over its accent rule, and the metadata rows beneath.
     *
     * <p>The title and the rule share a band so the rule sits at the block's
     * foot whatever the title measures: the title is seated to the top of the
     * band and the rule anchored to its bottom.</p>
     */
    static void renderTitle(SectionBuilder section, InvoiceMasthead masthead) {
        section.name("InvoiceHeader").spacing(0);
        section.addContainer(container -> container
                .name("InvoiceTitleBlock")
                .rectangle(META_WIDTH, TITLE_BLOCK_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(tracked("InvoiceTitle", masthead.title(), INVOICE_TITLE,
                                TRACK_TITLE, PAPER, TextVerticalAlign.TOP),
                        0, TITLE_CAP_INSET, LayerAlign.TOP_LEFT)
                .position(new LineBuilder()
                                .name("TitleRule")
                                .horizontal(TITLE_RULE_WIDTH)
                                .thickness(TITLE_RULE_THICKNESS)
                                .color(ACCENT)
                                .margin(DocumentInsets.zero())
                                .build(),
                        0, 0, LayerAlign.BOTTOM_LEFT)
                .margin(new DocumentInsets(0, 0, TITLE_RULE_TO_METADATA, 0)));
        for (InvoiceMasthead.Entry entry : masthead.entries()) {
            section.addContainer(container -> container
                    .name("MetadataRow")
                    .rectangle(META_WIDTH, META_ROW_HEIGHT)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .centerLeft(tracked("MetaLabel", entry.label(), META_LABEL,
                            TRACK_META_LABEL, PAPER))
                    .position(paragraph(entry.value(), BODY, TextAlign.LEFT),
                            META_WIDTH * META_VALUE_SPLIT, 0, LayerAlign.CENTER_LEFT));
        }
    }

    // -- the rule between the masthead and the parties ---------------------

    /**
     * The masthead rule, which stops at the text column rather than running
     * to the table's edge — the only rule on the sheet that does.
     *
     * @param line the rule to draw
     */
    static void renderRule(LineBuilder line) {
        line.name("HeaderRule")
                .horizontal(TEXT_WIDTH)
                .thickness(RULE_THICKNESS)
                .color(HAIRLINE_QUIET)
                .margin(new DocumentInsets(MASTHEAD_TO_RULE, 0, 0, TEXT_INSET));
    }

    // -- parties ---------------------------------------------------------

    /**
     * One party block: the tracked heading over its address.
     *
     * @param section the host column
     * @param party   the recipient to draw
     * @param divided true for the right-hand block, which carries the
     *                hairline between the two as its own left border
     */
    static void renderParty(SectionBuilder section, InvoiceRecipient party, boolean divided) {
        // Named after the heading it draws, so the two columns are told apart
        // in a layout snapshot; a party that states no heading still needs a
        // name, so it falls back to its role.
        String heading = party.heading();
        section.name(heading.isBlank()
                ? (divided ? "ShipTo" : "BillTo")
                : heading.replace(" ", "")).spacing(0);
        if (divided) {
            section.accentLeft(HAIRLINE_QUIET, RULE_THICKNESS)
                    .padding(new DocumentInsets(0, 0, 0, PARTIES_DIVIDER_INSET));
        } else {
            section.padding(new DocumentInsets(0, 0, 0, TEXT_INSET));
        }
        section.add(tracked("PartyHeading", heading, SECTION_HEADING,
                        TRACK_SECTION_HEADING, PAPER))
                .addParagraph(p -> p
                        .name("PartyAddress")
                        .text(String.join(NEWLINE, addressOf(party)))
                        .textStyle(BODY)
                        .lineSpacing(leading(LINE_PITCH * 1.183, BODY))
                        .margin(new DocumentInsets(LINE_PITCH * 0.72, 0, 0, 0)));
    }

    /**
     * The lines a party is drawn as: the name, the subline when it carries
     * one, then the address. This design sets them as one block, so a
     * recipient that names an attention line gets it on the second line.
     */
    private static List<String> addressOf(InvoiceRecipient party) {
        List<String> lines = new ArrayList<>();
        if (!party.name().isBlank()) {
            lines.add(party.name());
        }
        if (!party.subline().isBlank()) {
            lines.add(party.subline());
        }
        lines.addAll(party.addressLines());
        return lines;
    }
}
