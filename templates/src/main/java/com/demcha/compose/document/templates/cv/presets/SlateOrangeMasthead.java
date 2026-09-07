package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CONTACT_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CONTACT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CONTACT_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIALS_DIVIDER_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIALS_DIVIDER_X;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIALS_DIVIDER_Y;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HAIRLINE_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HAIRLINE_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HAIRLINE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HAIRLINE_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HALF_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.IDENTITY_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.IDENTITY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.INITIALS_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MASTHEAD_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MASTHEAD_INK;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MASTHEAD_MUTED;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MONOGRAM_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MONOGRAM_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MONOGRAM_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MONOGRAM_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.NAME_TO_ROLE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ROLE_LINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ROLE_TO_TAGLINE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_FAINT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.SIDEBAR_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.SLATE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.TAGLINE_SEPARATOR;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.TAGLINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.TILE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.inlineIcon;

/**
 * The page chrome and the band above the body: the monogram tile, the
 * identity strip, the orange hairline and the contact column.
 */
final class SlateOrangeMasthead {

    private SlateOrangeMasthead() {
    }

    /** One line of the contact column: a mark, a value and where it points. */
    private record Channel(String token, String value, String href) {
    }

    /**
     * The four fills whose geometry is a fact about the page rather than about
     * any content: the slate band, the orange tile over its left end, the
     * hairline dividing the two body columns, and the fainter one dividing the
     * two credential columns.
     *
     * <p>They are page backgrounds rather than section fills or lines: a fill
     * on a section is bounded by its content and would stop short of the paper
     * edge, while a page background takes its geometry from the canvas and
     * reaches all four. Order matters — the tile is listed after the band so
     * it paints over it.</p>
     */
    static void renderChrome(DocumentSession document) {
        document.pageBackgrounds(List.of(
                PageBackgroundFill.topBandPoints(MASTHEAD_HEIGHT, PAGE_HEIGHT, SLATE),
                new PageBackgroundFill(0.0, 0.0,
                        TILE_WIDTH / PAGE_WIDTH, MASTHEAD_HEIGHT / PAGE_HEIGHT, ACCENT),
                new PageBackgroundFill(
                        (PAGE_MARGIN + SIDEBAR_WIDTH + HALF_GAP) / PAGE_WIDTH,
                        (MASTHEAD_HEIGHT + BODY_TOP) / PAGE_HEIGHT,
                        RULE_THICKNESS / PAGE_WIDTH,
                        (PAGE_HEIGHT - MASTHEAD_HEIGHT - BODY_TOP - PAGE_MARGIN) / PAGE_HEIGHT,
                        RULE),
                new PageBackgroundFill(
                        CREDENTIALS_DIVIDER_X, CREDENTIALS_DIVIDER_Y,
                        RULE_THICKNESS / PAGE_WIDTH, CREDENTIALS_DIVIDER_HEIGHT,
                        RULE_FAINT)));
    }

    /**
     * The band's content, as four cells of one top-level row.
     *
     * <p>The row's height is set by the hairline cell rather than by whichever
     * text block happens to be tallest: that cell's padding and its line add
     * up to the band's height exactly, so the body below starts where the
     * slate fill ends however the type is later adjusted.</p>
     */
    static void render(PageFlowBuilder page, CvIdentity identity, ParagraphSection specialisms) {
        page.addRow("Masthead", row -> {
            row.spacing(0);
            row.columns(
                    DocumentRowColumn.fixed(TILE_WIDTH),
                    DocumentRowColumn.fixed(IDENTITY_WIDTH),
                    DocumentRowColumn.fixed(HAIRLINE_THICKNESS),
                    // The remainder, not a fourth measured width: four fixed
                    // columns summing to the page leave the engine nothing to
                    // round with, and it refuses the row by a third of a point.
                    DocumentRowColumn.weight(1.0));
            row.addSection("Monogram", tile -> renderTile(tile, identity));
            row.addSection("Identity", block -> renderIdentity(block, identity, specialisms));
            row.addSection("MastheadHairline", SlateOrangeMasthead::renderHairline);
            row.addSection("Contact", block -> renderContact(block, identity));
        });
    }

    // -- the tile ----------------------------------------------------------

    /**
     * The initials and the short rule under them, centred in the tile.
     *
     * <p>Both are centred on the cell rather than positioned at a measured x,
     * so they follow the same constant the tile's fill uses. The initials come
     * from the name itself: a document states its name once, and a second
     * place to keep true is a second place to be wrong.</p>
     */
    private static void renderTile(SectionBuilder tile, CvIdentity identity) {
        tile.spacing(0);
        tile.padding((float) MONOGRAM_TOP, 0f, 0f, 0f);
        tile.addParagraph(p -> p
                .name("Initials")
                .text(initials(identity))
                .align(TextAlign.CENTER)
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, MONOGRAM_SIZE, MASTHEAD_INK, true))
                .margin(0f, 0f, (float) INITIALS_TO_RULE, 0f));
        tile.addAligned(HorizontalAlign.CENTER, new LineBuilder()
                .name("MonogramRule")
                .horizontal(MONOGRAM_RULE_WIDTH)
                .thickness(MONOGRAM_RULE_THICKNESS)
                .color(MASTHEAD_INK)
                .build());
    }

    private static String initials(CvIdentity identity) {
        StringBuilder out = new StringBuilder(2);
        appendInitial(out, identity.name().first());
        appendInitial(out, identity.name().last());
        return out.toString();
    }

    private static void appendInitial(StringBuilder out, String part) {
        if (!part.isBlank()) {
            out.append(Character.toUpperCase(part.charAt(0)));
        }
    }

    // -- the identity strip ------------------------------------------------

    /** Name, role line and specialism strip, stacked on one left edge. */
    private static void renderIdentity(SectionBuilder block, CvIdentity identity,
                                       ParagraphSection specialisms) {
        block.spacing(0);
        block.padding((float) IDENTITY_TOP, 0f, 0f, (float) PAGE_MARGIN);
        block.addParagraph(p -> p
                .name("Name")
                .text(identity.name().full().toUpperCase(Locale.ROOT))
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, NAME_SIZE, MASTHEAD_INK, true))
                .margin(0f, 0f, (float) NAME_TO_ROLE, 0f));
        block.addParagraph(p -> p
                .name("RoleLine")
                .text(identity.jobTitle().toUpperCase(Locale.ROOT))
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, ROLE_LINE_SIZE, ACCENT, true))
                .margin(0f, 0f, (float) ROLE_TO_TAGLINE, 0f));
        if (specialisms == null || specialisms.body().isBlank()) {
            return;
        }
        block.addParagraph(p -> p
                .name("Tagline")
                .text(String.join(TAGLINE_SEPARATOR, lines(specialisms.body())))
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, TAGLINE_SIZE, MASTHEAD_MUTED, false)));
    }

    /** The specialisms, one per line the document wrote. */
    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    // -- the hairline ------------------------------------------------------

    /**
     * The vertical accent between the two halves of the band. It is a cell of
     * the row, so its horizontal position comes from the row's own column
     * widths rather than from a measured x — and its padding is what gives the
     * row its height.
     */
    private static void renderHairline(SectionBuilder cell) {
        cell.spacing(0);
        cell.padding((float) HAIRLINE_TOP, 0f, (float) HAIRLINE_BOTTOM, 0f);
        cell.addLine(line -> line
                .name("MastheadHairlineRule")
                .vertical(HAIRLINE_HEIGHT)
                .thickness(HAIRLINE_THICKNESS)
                .color(ACCENT));
    }

    // -- the contact column ------------------------------------------------

    /**
     * The contact lines.
     *
     * <p>Each is one paragraph rather than a table: the mark is an inline run
     * measured into the line box, so it shares a baseline with its value by
     * construction, and nothing here has to line up with a column. Where the
     * value points somewhere the run becomes an inline link, so the target
     * reaches the PDF as a real annotation rather than as text that merely
     * looks like one.</p>
     *
     * <p>A link shows its own label and carries the address behind it, so the
     * column's width does not depend on how long a profile happens to be
     * called.</p>
     */
    private static void renderContact(SectionBuilder block, CvIdentity identity) {
        block.spacing(0);
        block.padding((float) CONTACT_TOP, 0f, 0f, (float) CONTACT_PAD_LEFT);
        DocumentTextStyle valueStyle = style(BODY_FONT, CONTACT_SIZE, MASTHEAD_INK, false);
        List<Channel> channels = channels(identity);
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            boolean last = i == channels.size() - 1;
            int index = i;
            block.addParagraph(p -> {
                p.name("Contact_" + index + "_" + channel.token());
                p.lineSpacing(0);
                inlineIcon(p, channel.token());
                p.inlineText("   ", valueStyle);
                if (channel.href() == null || channel.href().isBlank()) {
                    p.inlineText(channel.value(), valueStyle);
                } else {
                    p.inlineText(channel.value(), valueStyle,
                            new DocumentLinkOptions(channel.href()));
                }
                p.margin(0f, 0f,
                        last ? 0f : (float) gap(CONTACT_PITCH, CONTACT_SIZE * LINE_FACTOR), 0f);
            });
        }
    }

    /** The column's order: how to call, how to write, the links, then where. */
    private static List<Channel> channels(CvIdentity identity) {
        Contact contact = identity.contact();
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(SlateOrangeIcons.PHONE, contact.phone(),
                ContactUri.tel(contact.phone())));
        channels.add(new Channel(SlateOrangeIcons.EMAIL, contact.email(),
                "mailto:" + contact.email()));
        for (Link link : identity.links()) {
            channels.add(new Channel(SlateOrangeIcons.LINKEDIN, link.label(), link.url()));
        }
        channels.add(new Channel(SlateOrangeIcons.LOCATION, contact.address(), null));
        return channels;
    }
}
