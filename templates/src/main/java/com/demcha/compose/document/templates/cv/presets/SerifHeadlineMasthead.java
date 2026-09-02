package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTACT_GLYPH_BOX;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTACT_ROW_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTACT_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTACT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DISPLAY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GOLD;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GOLD_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GOLD_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.IDENTITY_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MASTHEAD_TO_SUMMARY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.NAME_TO_ROLE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ROLE_TO_GOLD_RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SUMMARY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SUMMARY_RIGHT_INSET;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SUMMARY_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.body;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.tracked;

/**
 * The top of the sheet: the serif name with its role and gold rule, the
 * contact channels stacked at the right, the summary beneath, and the rule
 * that closes the block.
 */
final class SerifHeadlineMasthead {

    private SerifHeadlineMasthead() {
    }

    static void compose(PageFlowBuilder page, CvIdentity identity, ParagraphSection summary) {
        page.addRow("Masthead", row -> {
            row.spacing(0);
            row.gap(0);
            row.weights(IDENTITY_WEIGHT, 1.0 - IDENTITY_WEIGHT);
            row.verticalAlign(RowVerticalAlign.TOP);
            row.margin(new DocumentInsets(0, 0, MASTHEAD_TO_SUMMARY, 0));
            row.addSection("Identity", section -> renderIdentity(section, identity));
            row.addSection("Contact", section -> renderContact(section, identity));
        });
        if (SectionLookup.hasContent(summary)) {
            renderSummary(page, summary);
        }
        page.addLine(line -> line
                .name("MastheadRule")
                .horizontal(CONTENT_WIDTH)
                .thickness(RULE_THICKNESS)
                .color(RULE)
                .margin(new DocumentInsets(0, 0, RULE_TO_BODY, 0)));
    }

    // -- identity --------------------------------------------------------

    /**
     * The name in the display serif, the role in tracked capitals, and the
     * short gold rule that closes them.
     */
    private static void renderIdentity(SectionBuilder section, CvIdentity identity) {
        section.spacing(0).padding(DocumentInsets.zero());

        DocumentTextStyle nameStyle = TextStyles.of(DISPLAY_FONT, NAME_SIZE,
                DocumentTextDecoration.DEFAULT, INK);
        section.addParagraph(p -> p
                .name("Name")
                .text(identity.name().full().toUpperCase(Locale.ROOT))
                .textStyle(nameStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(DISPLAY_LEADING)
                .margin(new DocumentInsets(0, 0, NAME_TO_ROLE, 0)));

        DocumentTextStyle roleStyle = style(ROLE_SIZE, INK, DocumentTextDecoration.DEFAULT);
        section.addParagraph(p -> {
            p.name("Role");
            p.textStyle(roleStyle);
            tracked(p, identity.jobTitle().toUpperCase(Locale.ROOT), roleStyle);
            p.align(TextAlign.LEFT);
            p.lineSpacing(TIGHT_LEADING);
            p.margin(new DocumentInsets(0, 0, ROLE_TO_GOLD_RULE, 0));
        });

        section.addLine(line -> line
                .name("GoldRule")
                .horizontal(GOLD_RULE_WIDTH)
                .thickness(GOLD_RULE_THICKNESS)
                .color(GOLD)
                .margin(DocumentInsets.zero()));
    }

    // -- contact ---------------------------------------------------------

    /**
     * The contact channels, in the order the design sets them: phone, email,
     * address, then whatever links the identity carries.
     *
     * <p>Each row is a band with the mark anchored at the left and the value
     * at a fixed inset, so the values line up whatever their marks measure —
     * and, unlike a single paragraph, a long value wraps under itself rather
     * than under the mark.</p>
     *
     * <p>The phone and the email carry {@code tel:} and {@code mailto:}
     * targets built from the values, and each link its own url.</p>
     */
    private static void renderContact(SectionBuilder section, CvIdentity identity) {
        section.spacing(CONTACT_ROW_GAP).padding(DocumentInsets.zero());
        for (Channel channel : channels(identity)) {
            renderChannel(section, channel);
        }
    }

    private static void renderChannel(SectionBuilder section, Channel channel) {
        DocumentNode glyph = new ImageBuilder()
                .name("ContactGlyph_" + channel.token())
                .source(SerifHeadlineIcons.image(channel.token()))
                .fitToBounds(CONTACT_GLYPH_BOX, CONTACT_GLYPH_BOX)
                .margin(DocumentInsets.zero())
                .build();

        DocumentLinkOptions link = channel.href() == null
                ? null
                : new DocumentLinkOptions(channel.href());
        ParagraphBuilder valueText = new ParagraphBuilder()
                .name("ContactValue_" + channel.token())
                .textStyle(body())
                .lineSpacing(BODY_LEADING);
        if (link == null) {
            valueText.inlineText(channel.value(), body());
        } else {
            valueText.inlineText(channel.value(), body(), link);
        }
        DocumentNode value = valueText.margin(DocumentInsets.zero()).build();

        section.addContainer(band -> band
                .name("ContactRow_" + channel.token())
                .rectangle(CONTACT_WIDTH, CONTACT_ROW_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .padding(DocumentInsets.zero())
                .position(glyph, 0, 0, LayerAlign.CENTER_LEFT)
                .position(value, CONTACT_TEXT_INSET, 0, LayerAlign.CENTER_LEFT));
    }

    private static List<Channel> channels(CvIdentity identity) {
        List<Channel> channels = new ArrayList<>();
        String phone = identity.contact().phone();
        channels.add(new Channel(SerifHeadlineIcons.PHONE, phone, telUri(phone)));
        String email = identity.contact().email();
        channels.add(new Channel(SerifHeadlineIcons.EMAIL, email, "mailto:" + email));
        channels.add(new Channel(SerifHeadlineIcons.LOCATION,
                identity.contact().address(), null));
        for (Link link : identity.links()) {
            channels.add(new Channel(linkToken(link), link.label(), link.url()));
        }
        return channels;
    }

    /**
     * The dial target for a phone number: its digits, keeping a leading
     * {@code +} so an international number stays international.
     */
    private static String telUri(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    /**
     * The mark for a link. This design packages the two networks it shows;
     * anything else takes the LinkedIn mark, because there is no globe in
     * the set to fall back to.
     */
    private static String linkToken(Link link) {
        String haystack = SectionLookup.normalize(link.label() + " " + link.url());
        return haystack.contains("github")
                ? SerifHeadlineIcons.GITHUB
                : SerifHeadlineIcons.LINKEDIN;
    }

    private record Channel(String token, String value, String href) {
    }

    // -- summary ---------------------------------------------------------

    /**
     * The opening paragraph. It stops at the column divider rather than
     * running the width of the page, and this design gives it no heading, so
     * the section's title is not drawn.
     */
    private static void renderSummary(PageFlowBuilder page, ParagraphSection summary) {
        page.addParagraph(p -> p
                .name("Summary")
                .text(summary.body())
                .textStyle(body())
                .lineSpacing(SUMMARY_LEADING)
                .margin(new DocumentInsets(0, SUMMARY_RIGHT_INSET, SUMMARY_TO_RULE, 0)));
    }
}
