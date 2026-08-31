package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgPath;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT_DEEP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTACT_ICON_DROP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTACT_ICON_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTACT_PAIR_LIFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTACT_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DISPLAY_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.FULL_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.LOGO_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.LOGO_TO_IDENTITY;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.LOGO_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MARK_STROKE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MASTHEAD_TO_CONTACT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.NAME_TO_ROLE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.NAME_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PULSE_BOX_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PULSE_BOX_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PULSE_DROP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PULSE_SHIFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ROLE_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SEPARATOR;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SEPARATOR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SEPARATOR_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.gapRun;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.style;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.icon;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.tracked;

/**
 * The top of the sheet: the mark beside the name, the contact strip, and the
 * rule that closes them.
 */
final class TealPulseMasthead {

    private TealPulseMasthead() {
    }

    /** One channel of the contact strip: a mark, a value and where it points. */
    private record Channel(String token, String value, String href) {
    }

    static void render(PageFlowBuilder page, CvIdentity identity) {
        page.addRow("Masthead", row -> {
            row.columns(DocumentRowColumn.fixed(LOGO_WIDTH), DocumentRowColumn.weight(1));
            row.verticalAlign(RowVerticalAlign.CENTER);
            row.spacing(LOGO_TO_IDENTITY);
            row.add(brandMark());
            row.addSection("Identity", section -> renderIdentity(section, identity));
            row.margin(new DocumentInsets(0, 0, MASTHEAD_TO_CONTACT, 0));
        });
        renderContactStrip(page, identity);
        page.addLine(line -> line
                .name("MastheadRule")
                .horizontal(CONTENT_WIDTH)
                .thickness(FULL_RULE_THICKNESS)
                .color(RULE_STRONG)
                .margin(new DocumentInsets(0, 0, RULE_TO_BODY, 0)));
    }

    // -- the mark ----------------------------------------------------------

    /**
     * A heart outline crossed by a flat pulse.
     *
     * <p>No single glyph has this geometry — every heartbeat icon puts the
     * pulse in the left half — so the mark is two vectors centred in one box.
     * Each child is wider or taller than that box, and a shape container
     * clamps an over-sized child to the top left rather than centring it, so
     * each is pulled back by half its own overflow. Those two corrections
     * exist only to cancel the clamp.</p>
     */
    private static DocumentNode brandMark() {
        double heartSize = TealPulseIcons.size(TealPulseIcons.BRAND_HEART);
        double heartLift = -(heartSize - LOGO_HEIGHT) / 2.0;
        return new ShapeContainerBuilder()
                .name("BrandMark")
                .rectangle(LOGO_WIDTH, LOGO_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(icon(TealPulseIcons.BRAND_HEART), 0, heartLift, LayerAlign.CENTER)
                .position(pulse(), PULSE_SHIFT, PULSE_DROP, LayerAlign.CENTER)
                .build();
    }

    /**
     * The pulse, drawn from the packaged glyph's own geometry at a box this
     * preset chooses, so the line spans the mark while the spike keeps the
     * height the design draws. The stroke is the design's, not the glyph's.
     */
    private static DocumentNode pulse() {
        SvgPath geometry = TealPulseIcons.icon(TealPulseIcons.BRAND_PULSE)
                .layers().get(0).geometry();
        return new PathBuilder()
                .name("BrandPulse")
                .size(PULSE_BOX_WIDTH, PULSE_BOX_HEIGHT)
                .svg(geometry)
                .stroke(DocumentStroke.of(ACCENT, MARK_STROKE))
                .lineCap(DocumentLineCap.ROUND)
                .lineJoin(DocumentLineJoin.ROUND)
                .build();
    }

    // -- the name ----------------------------------------------------------

    /** The tracked display name over the tracked role, both in capitals. */
    private static void renderIdentity(SectionBuilder section, CvIdentity identity) {
        section.spacing(0);
        DocumentTextStyle nameStyle =
                style(DISPLAY_FONT, NAME_SIZE, DISPLAY_TEXT, DocumentTextDecoration.DEFAULT);
        ParagraphBuilder name = new ParagraphBuilder()
                .name("DisplayName")
                .textStyle(nameStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0);
        tracked(name, identity.name().full().toUpperCase(Locale.ROOT), nameStyle,
                NAME_TRACKING_EM);
        section.add(name.margin(new DocumentInsets(0, 0, NAME_TO_ROLE, 0)).build());

        DocumentTextStyle roleStyle =
                style(DISPLAY_FONT, ROLE_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
        ParagraphBuilder role = new ParagraphBuilder()
                .name("ProfessionalTitle")
                .textStyle(roleStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0);
        tracked(role, identity.jobTitle().toUpperCase(Locale.ROOT), roleStyle, ROLE_TRACKING_EM);
        section.add(role.margin(DocumentInsets.zero()).build());
    }

    // -- the strip ---------------------------------------------------------

    /**
     * The contact strip: the three channels and a row per link, each a mark
     * and a value, divided by short rules.
     *
     * <p>Each pair is one paragraph, so the row's distribution can never split
     * a mark from its value. Spreading the children pins the run to both
     * margins and makes the gaps even, which a fixed gap would reach only by
     * luck of the font's metrics.</p>
     *
     * <p>A link shows its own label and carries the address behind it, so the
     * strip's gaps do not depend on how long a profile's URL happens to be.</p>
     */
    private static void renderContactStrip(PageFlowBuilder page, CvIdentity identity) {
        List<Channel> channels = channels(identity);
        page.addRow("ContactStrip", row -> {
            row.verticalAlign(RowVerticalAlign.CENTER);
            row.arrangement(RowArrangement.SPACE_BETWEEN);
            for (int index = 0; index < channels.size(); index++) {
                if (index > 0) {
                    separator(row, index);
                }
                row.add(channelPair(channels.get(index), index));
            }
            row.margin(new DocumentInsets(0, 0, CONTACT_TO_RULE, 0));
        });
    }

    /** The strip's order: where, then how to write, then how to call, then links. */
    private static List<Channel> channels(CvIdentity identity) {
        Contact contact = identity.contact();
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(TealPulseIcons.LOCATION, contact.address(), null));
        channels.add(new Channel(TealPulseIcons.EMAIL, contact.email(),
                "mailto:" + contact.email()));
        channels.add(new Channel(TealPulseIcons.PHONE, contact.phone(), telUri(contact.phone())));
        for (Link link : identity.links()) {
            channels.add(new Channel(TealPulseIcons.LINKEDIN, link.label(), link.url()));
        }
        return channels;
    }

    private static DocumentNode channelPair(Channel channel, int index) {
        DocumentTextStyle textStyle =
                style(BODY_FONT, CONTACT_SIZE, BODY_TEXT, DocumentTextDecoration.DEFAULT);
        DocumentLinkOptions link = channel.href() == null || channel.href().isBlank()
                ? null
                : new DocumentLinkOptions(channel.href());
        ParagraphBuilder pair = new ParagraphBuilder()
                .name("Contact_" + index + "_" + compact(channel.token()))
                .textStyle(textStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0)
                .inlineSvgIcon(TealPulseIcons.icon(channel.token()),
                        TealPulseIcons.size(channel.token()), InlineImageAlignment.CENTER,
                        CONTACT_ICON_DROP, link);
        String value = gapRun(CONTACT_ICON_GAP, CONTACT_SIZE) + channel.value();
        if (link == null) {
            pair.inlineText(value, textStyle);
        } else {
            pair.inlineText(value, textStyle, link);
        }
        // The pair still sits below the axis the design uses. It is centred in
        // a row whose height the separators set, so a NEGATIVE top margin of
        // twice the wanted lift raises it by half of it. Negative rather than a
        // positive bottom margin: both shift a centred child equally, but a
        // bottom margin would grow the box past the separators and take the
        // whole strip — and everything under it — down the page.
        return pair.margin(new DocumentInsets(-2 * CONTACT_PAIR_LIFT, 0, 0, 0)).build();
    }

    private static void separator(RowBuilder row, int index) {
        row.addLine(line -> line
                .name("ContactSeparator_" + index)
                .vertical(SEPARATOR_HEIGHT)
                .thickness(SEPARATOR_THICKNESS)
                .color(SEPARATOR)
                .margin(DocumentInsets.zero()));
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
}
