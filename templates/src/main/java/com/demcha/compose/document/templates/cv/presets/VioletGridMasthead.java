package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT_LIGHT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTACT_COLUMN_X;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTACT_ICON_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTACT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTACT_TOP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISCIPLINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISCIPLINE_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISCIPLINE_TRACKING;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADER_TO_SUMMARY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.IDENTITY_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.IDENTITY_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SUMMARY_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SUMMARY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.style;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.inlineIcon;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.lines;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.tracked;

/**
 * The top of the sheet: the two-tone name beside the contact column, and the
 * opening lines under them.
 */
final class VioletGridMasthead {

    private VioletGridMasthead() {
    }

    /** One line of the contact column: a mark, a value and where it points. */
    private record Channel(String token, String value, String href) {
    }

    /**
     * The masthead, as a top-level row split where the contact marks begin —
     * the column is left-aligned on its marks rather than right-aligned to the
     * margin, so that x is the split.
     */
    static void render(PageFlowBuilder page, CvIdentity identity, ParagraphSection summary) {
        page.addRow("Header", row -> {
            row.spacing(0);
            row.columns(
                    DocumentRowColumn.fixed(CONTACT_COLUMN_X),
                    DocumentRowColumn.weight(1.0));
            row.addSection("Identity", block -> renderIdentity(block, identity));
            row.addSection("Contact", block -> renderContact(block, identity));
        });
        renderSummary(page, summary);
    }

    // -- the name ----------------------------------------------------------

    /**
     * The name, the discipline line and the accent rule under it.
     *
     * <p>Both names are inline runs of ONE paragraph, so the gap between them
     * is a real word space that follows the type size rather than a measured
     * offset that would not.</p>
     */
    private static void renderIdentity(SectionBuilder block, CvIdentity identity) {
        block.spacing(0);
        DocumentTextStyle given = style(DISPLAY_FONT, NAME_SIZE, INK, false);
        DocumentTextStyle family = style(DISPLAY_FONT, NAME_SIZE, ACCENT_LIGHT, false);
        block.addParagraph(p -> p
                .name("Name")
                .lineSpacing(0)
                .textStyle(given)
                .inlineText(identity.name().first().toUpperCase(Locale.ROOT), given)
                .inlineText(" ", given)
                .inlineText(identity.name().last().toUpperCase(Locale.ROOT), family));
        block.add(tracked("Discipline", identity.jobTitle().toUpperCase(Locale.ROOT),
                DISPLAY_FONT, DISCIPLINE_SIZE, ACCENT, true, DISCIPLINE_TRACKING)
                .margin(0f, 0f, (float) DISCIPLINE_TO_RULE, 0f)
                .build());
        block.addLine(line -> line
                .name("IdentityRule")
                .horizontal(IDENTITY_RULE_WIDTH)
                .thickness(IDENTITY_RULE_THICKNESS)
                .color(ACCENT));
    }

    // -- the contact column ------------------------------------------------

    /**
     * The contact lines.
     *
     * <p>Each is one paragraph rather than a two-column table: the mark is an
     * inline run measured into the line box, so it shares a baseline with its
     * value by construction, and nothing in this column has to line up with
     * anything outside it. Where the value points somewhere the run becomes an
     * inline link, so the target reaches the PDF as a real annotation.</p>
     *
     * <p>A link shows its own label and carries the address behind it, so the
     * column's width does not depend on how long an address happens to be.</p>
     */
    private static void renderContact(SectionBuilder block, CvIdentity identity) {
        block.spacing(0);
        block.padding((float) CONTACT_TOP, 0f, 0f, 0f);
        DocumentTextStyle value = style(BODY_FONT, CONTACT_SIZE, BODY, false);
        // The line box comes from the paragraph's own style, and an inline run
        // taller than it is drawn clipped: take whichever of the mark and the
        // text needs more room.
        double boxSize = Math.max(CONTACT_SIZE,
                VioletGridIcons.size(VioletGridIcons.EMAIL) / LINE_FACTOR);
        DocumentTextStyle box = style(BODY_FONT, boxSize, BODY, false);
        List<Channel> channels = channels(identity);
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            boolean last = i == channels.size() - 1;
            int index = i;
            block.addParagraph(p -> {
                p.name("Contact_" + index + "_" + channel.token());
                p.lineSpacing(0);
                p.textStyle(box);
                inlineIcon(p, channel.token());
                p.inlineText(CONTACT_ICON_GAP, value);
                if (channel.href() == null || channel.href().isBlank()) {
                    p.inlineText(channel.value(), value);
                } else {
                    p.inlineText(channel.value(), value, new DocumentLinkOptions(channel.href()));
                }
                p.margin(0f, 0f,
                        last ? 0f : (float) gap(CONTACT_PITCH, boxSize * LINE_FACTOR), 0f);
            });
        }
    }

    /** The column's order: how to write, how to call, where, then the links. */
    private static List<Channel> channels(CvIdentity identity) {
        Contact contact = identity.contact();
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(VioletGridIcons.EMAIL, contact.email(),
                "mailto:" + contact.email()));
        channels.add(new Channel(VioletGridIcons.PHONE, contact.phone(),
                telUri(contact.phone())));
        channels.add(new Channel(VioletGridIcons.LOCATION, contact.address(), null));
        for (Link link : identity.links()) {
            channels.add(new Channel(markFor(link), link.label(), link.url()));
        }
        return channels;
    }

    /** A link takes the mark of the network it points at, or the globe. */
    private static String markFor(Link link) {
        String target = (link.url() + " " + link.label()).toLowerCase(Locale.ROOT);
        return target.contains("linkedin") ? VioletGridIcons.LINKEDIN : VioletGridIcons.WEBSITE;
    }

    /**
     * The dial target for a phone number: its digits, keeping a leading
     * {@code +} so an international number stays international, and dropping a
     * parenthesised trunk prefix the way a caller dialling from abroad does.
     */
    private static String telUri(String phone) {
        String dialled = phone.replaceAll("\\(0+\\)", "");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    // -- the opening lines -------------------------------------------------

    /**
     * The opening block — one paragraph per line the document wrote.
     *
     * <p>Not a wrapped paragraph, and that is a reading of the design rather
     * than a convenience: its first line ends with room for the next word
     * several times over, so no greedy wrap could produce it. The lines are
     * sentences, set as lines, and the document says so.</p>
     */
    private static void renderSummary(PageFlowBuilder page, ParagraphSection summary) {
        if (summary == null || summary.body().isBlank()) {
            return;
        }
        DocumentTextStyle line = style(BODY_FONT, SUMMARY_SIZE, BODY, false);
        List<String> body = lines(summary.body());
        for (int i = 0; i < body.size(); i++) {
            boolean last = i == body.size() - 1;
            int index = i;
            page.addParagraph(p -> p
                    .name("SummaryLine_" + index)
                    .text(body.get(index))
                    .lineSpacing(0)
                    .textStyle(line)
                    .margin(index == 0 ? (float) HEADER_TO_SUMMARY : 0f, 0f,
                            last ? 0f : (float) gap(SUMMARY_PITCH, SUMMARY_SIZE * LINE_FACTOR),
                            0f));
        }
    }
}
