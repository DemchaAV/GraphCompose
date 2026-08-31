package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.svg.SvgPath;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BAR_TO_CONTACT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CONTACT_SEPARATOR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CONTACT_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MARGIN_X;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.NAME_TO_BAR;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ON_DARK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ROLE_BAR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ROLE_BAR_SLANT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ROLE_BAR_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ROLE_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.SLASHES;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.SLASH_BLOCK_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.SLASH_BLOCK_X;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.px;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.style;

/**
 * The three bands above the body row: the two-tone name, the dark role bar with
 * its accent slashes, and the contact strip.
 *
 * <h2>Why the role bar is a layer stack and not a row</h2>
 *
 * <p>The plate's bounding box and the slash group's bounding box overlap
 * horizontally, and a row cannot place two cells that overlap. The plate is a
 * shape container so the white role text is its child — the text is inside the
 * bar, not laid beside it — and the three slashes are one path node carrying
 * three closed sub-paths, for the same overlap reason.</p>
 *
 * <p>Both shapes are described in SVG user space through
 * {@link SvgPath#parse(String, double, double, double, double)} rather than raw
 * path coordinates: a path builder's own coordinates are bottom-left-origin and
 * normalised, and a lean read the wrong way up is a silent mirror rather than
 * an error.</p>
 */
final class OrangeOpsMasthead {

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one — {@code +44 (0)20 7946 0832} dials
     * {@code +442079460832}. Left in, the digits would run together into a
     * number that reaches nobody.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private OrangeOpsMasthead() {
    }

    /**
     * Draws the three bands.
     *
     * @param page     the page flow
     * @param identity whose CV this is
     */
    static void render(PageFlowBuilder page, CvIdentity identity) {
        renderName(page, identity.name());
        renderRoleBar(page, identity.jobTitle());
        renderContactStrip(page, identity);
    }

    /**
     * The name: one paragraph, two inline runs.
     *
     * <p>The colour change between the given and the family name is a run
     * boundary rather than a second node, so the halves cannot drift apart or
     * wrap independently of each other.</p>
     */
    private static void renderName(PageFlowBuilder page, CvName name) {
        page.addSection("Masthead", block -> {
            block.spacing(0);
            block.padding(0f, (float) MARGIN_X, 0f, (float) MARGIN_X);
            block.addParagraph(p -> p
                    .name("Name")
                    .lineSpacing(0)
                    .textStyle(style(DISPLAY_FONT, NAME_SIZE, INK, true))
                    .inlineText(name.first().toUpperCase(Locale.ROOT),
                            style(DISPLAY_FONT, NAME_SIZE, INK, true))
                    .inlineText(" ", style(DISPLAY_FONT, NAME_SIZE, INK, true))
                    .inlineText(name.last().toUpperCase(Locale.ROOT),
                            style(DISPLAY_FONT, NAME_SIZE, ACCENT, true))
                    .margin(0f, 0f, (float) NAME_TO_BAR, 0f));
        });
    }

    /** The role bar: a dark trapezoid carrying the role, and three accent slashes. */
    private static void renderRoleBar(PageFlowBuilder page, String discipline) {
        page.addLayerStack(stack -> {
            stack.name("RoleBar");
            stack.margin(new DocumentInsets(0, 0, BAR_TO_CONTACT, 0));
            stack.position(slashes(), SLASH_BLOCK_X, 0, LayerAlign.TOP_LEFT, 0);
            stack.position(plate(discipline), 0, 0, LayerAlign.TOP_LEFT, 1);
        });
    }

    /**
     * The dark plate. Its right edge leans left going down by the band's shared
     * slant, which is the same lean every slash takes.
     */
    private static DocumentNode plate(String discipline) {
        String d = "M0,0 L" + px(ROLE_BAR_WIDTH) + ",0 L"
                + px(ROLE_BAR_WIDTH - ROLE_BAR_SLANT) + "," + px(ROLE_BAR_HEIGHT)
                + " L0," + px(ROLE_BAR_HEIGHT) + " Z";
        SvgPath shape = SvgPath.parse(d, 0, 0, px(ROLE_BAR_WIDTH), px(ROLE_BAR_HEIGHT));
        return new ShapeContainerBuilder()
                .name("RoleBarPlate")
                .path(ROLE_BAR_WIDTH, ROLE_BAR_HEIGHT, shape)
                .fillColor(INK)
                .position(new ParagraphBuilder()
                        .name("RoleText")
                        .text(discipline.toUpperCase(Locale.ROOT))
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, ROLE_SIZE, ON_DARK, true))
                        .build(), ROLE_TEXT_INSET, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /**
     * The three slashes as one node.
     *
     * <p>Their bounding boxes overlap — the second starts left of where the
     * first ends — so they cannot be three siblings with positive gaps between
     * them. Three closed sub-paths in one path is what the design shows.</p>
     */
    private static DocumentNode slashes() {
        StringBuilder d = new StringBuilder();
        for (double[] shape : SLASHES) {
            if (d.length() > 0) {
                d.append(' ');
            }
            d.append(slash(shape[0], shape[1]));
        }
        SvgPath shape = SvgPath.parse(d.toString(), 0, 0,
                px(SLASH_BLOCK_WIDTH), px(ROLE_BAR_HEIGHT));
        return new PathBuilder()
                .name("RoleSlashes")
                .size(SLASH_BLOCK_WIDTH, ROLE_BAR_HEIGHT)
                .svg(shape)
                .fillColor(ACCENT)
                .build();
    }

    /**
     * One slash, from where its top edge starts and how wide it is.
     *
     * <p>Two numbers, not four: a slash leans by the band's shared slant like
     * everything else on it, so its foot is derived rather than measured.</p>
     */
    private static String slash(double topLeftPx, double widthPx) {
        double h = px(ROLE_BAR_HEIGHT);
        double top = topLeftPx * OrangeOpsStyles.PX;
        double bottom = top - ROLE_BAR_SLANT;
        double width = widthPx * OrangeOpsStyles.PX;
        return "M" + px(top) + ",0 L" + px(top + width) + ",0 L"
                + px(bottom + width) + "," + h + " L" + px(bottom) + "," + h + " Z";
    }

    /**
     * The contact strip: content-sized items with hairlines between them.
     *
     * <p>A top-level row. The item cells are {@link DocumentRowColumn#auto()} so
     * each is exactly as wide as its content, and the separator cells are
     * {@code weight(1)} so whatever width is left over is split evenly between
     * them — which is what lands the last item on the right margin without
     * measuring any of the strings.</p>
     *
     * <p>The three channels are always drawn: a {@code Contact} rejects a blank
     * field, so the triple is non-blank by construction. Each link is drawn as
     * its own label with the address behind it, so the strip's width does not
     * depend on how long an address happens to be.</p>
     */
    private static void renderContactStrip(PageFlowBuilder page, CvIdentity identity) {
        Contact contact = identity.contact();
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(OrangeOpsIcons.PHONE, contact.phone(), telUri(contact.phone())));
        channels.add(new Channel(OrangeOpsIcons.EMAIL, contact.email(),
                "mailto:" + contact.email()));
        channels.add(new Channel(OrangeOpsIcons.LOCATION, contact.address(), null));
        for (Link link : identity.links()) {
            channels.add(new Channel(markFor(link), link.label(), link.url()));
        }

        page.addRow("ContactStrip", row -> {
            row.spacing(0);
            row.padding(new DocumentInsets(0, MARGIN_X, CONTACT_TO_BODY, MARGIN_X));
            row.verticalAlign(RowVerticalAlign.CENTER);
            DocumentRowColumn[] columns = new DocumentRowColumn[2 * channels.size() - 1];
            for (int i = 0; i < columns.length; i++) {
                columns[i] = (i % 2 == 0) ? DocumentRowColumn.auto() : DocumentRowColumn.weight(1.0);
            }
            row.columns(columns);
            for (int i = 0; i < channels.size(); i++) {
                int index = i;
                if (i > 0) {
                    row.addSection("ContactSeparator" + index, gap -> gap
                            .spacing(0)
                            .addAligned(HorizontalAlign.CENTER, new LineBuilder()
                                    .name("ContactSeparator" + index + "Line")
                                    .vertical(CONTACT_SEPARATOR_HEIGHT)
                                    .thickness(RULE_THICKNESS)
                                    .color(RULE)
                                    .build()));
                }
                Channel channel = channels.get(i);
                row.addParagraph(p -> {
                    p.name("Contact" + index);
                    p.lineSpacing(0);
                    p.textStyle(style(BODY_FONT, CONTACT_SIZE, BODY, false));
                    OrangeOpsWidgets.mark(p, channel.token());
                    p.inlineText("  ", style(BODY_FONT, CONTACT_SIZE, BODY, false));
                    if (channel.href() == null) {
                        p.inlineText(channel.label(), style(BODY_FONT, CONTACT_SIZE, BODY, false));
                    } else {
                        p.inlineText(channel.label(), style(BODY_FONT, CONTACT_SIZE, BODY, false),
                                new DocumentLinkOptions(channel.href()));
                    }
                });
            }
        });
    }

    /** The mark for the network a link points at, or the globe. */
    private static String markFor(Link link) {
        return link.url().toLowerCase(Locale.ROOT).contains("linkedin.")
                ? OrangeOpsIcons.LINKEDIN
                : OrangeOpsIcons.WEBSITE;
    }

    /** A printed number as something a reader can dial, or {@code null}. */
    private static String telUri(String phone) {
        String dialled = TRUNK_PREFIX.matcher(phone).replaceAll("");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    /** One item of the strip: its mark, what it reads, and where it points. */
    private record Channel(String token, String label, String href) {
    }
}
