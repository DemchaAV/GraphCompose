package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.AVATAR_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.AVATAR_RING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.AVATAR_RING_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.AVATAR_TO_HEADING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BULLET_LEADING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.EDU_DEGREE_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.EDU_ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.EDU_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.LANGUAGE_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.LANGUAGE_VALUE_X;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.LIST_BULLET_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_BAND_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_PAD_X;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_STRONG;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_TEXT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SKILL_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.sidebarBody;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarWidgets.sidebarHeading;

/**
 * The navy left column: the ringed portrait, the contact channels, the
 * degrees, the skills and the languages.
 */
final class NavySidebarAside {

    /**
     * The contact heading. It is not read from the document because the
     * channels are not a section — they come off the identity, which carries
     * no title of its own.
     */
    static final String CONTACT_HEADING = "Contact";

    private NavySidebarAside() {
    }

    static void compose(SectionBuilder section,
                        CvIdentity identity,
                        EntriesSection education,
                        SkillsSection skills,
                        RowsSection languages) {
        section.spacing(0)
                .padding(new DocumentInsets(
                        SIDEBAR_PAD_TOP, SIDEBAR_PAD_X, PAGE_MARGIN, SIDEBAR_PAD_X));
        section.addSection("Avatar", inner -> {
            inner.padding(new DocumentInsets(0, 0, AVATAR_TO_HEADING, 0));
            renderPortrait(inner, identity);
        });
        section.addSection("Contact", inner -> renderContact(inner, identity));
        if (SectionLookup.hasContent(education)) {
            section.addSection("Education", inner -> renderEducation(inner, education));
        }
        if (SectionLookup.hasContent(skills)) {
            section.addSection("Skills", inner -> renderSkills(inner, skills));
        }
        if (SectionLookup.hasContent(languages)) {
            section.addSection("Languages", inner -> renderLanguages(inner, languages));
        }
    }

    // -- portrait --------------------------------------------------------

    /**
     * The photograph, clipped to a disc inside a pale ring. An identity
     * carrying no portrait draws the ring around a navy disc, which is the
     * same shape with nothing in it rather than a hole in the column.
     */
    private static void renderPortrait(SectionBuilder section, CvIdentity identity) {
        DocumentNode circle = identity.portrait()
                .map(NavySidebarAside::clippedPhoto)
                .orElseGet(NavySidebarAside::emptyDisc);
        section.addContainer(ring -> ring
                .name("AvatarRing")
                .circle(AVATAR_DIAMETER + 2.0 * AVATAR_RING_WIDTH)
                .fillColor(AVATAR_RING)
                .center(circle));
    }

    private static DocumentNode clippedPhoto(DocumentImageData image) {
        return new ShapeContainerBuilder()
                .name("AvatarCircle")
                .circle(AVATAR_DIAMETER)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .fillColor(NAVY)
                // size(), not fitToBounds(): the disc clips, so the photo is
                // drawn to fill it and whatever falls outside is cut away.
                .center(new ImageBuilder()
                        .name("AvatarPhoto")
                        .source(image)
                        .size(AVATAR_DIAMETER, AVATAR_DIAMETER)
                        .build())
                .build();
    }

    /**
     * The disc with no photograph in it. It is an ellipse rather than the
     * same clipping container holding nothing, because a shape container
     * with no layer is rejected — the clip needs something to cut.
     */
    private static DocumentNode emptyDisc() {
        return new EllipseBuilder()
                .name("AvatarCircle")
                .circle(AVATAR_DIAMETER)
                .fillColor(NAVY)
                .build();
    }

    // -- contact ---------------------------------------------------------

    /**
     * The contact channels, in the order the design sets them: phone, email,
     * address, then whatever links the identity carries.
     *
     * <p>The rows carry no link targets. The design draws them as text, and
     * the promotion keeps that — a clickable channel would be a change to
     * the sheet rather than a port of it.</p>
     */
    private static void renderContact(SectionBuilder section, CvIdentity identity) {
        sidebarHeading(section, CONTACT_HEADING, true);
        for (Channel channel : channels(identity)) {
            renderChannel(section, channel);
        }
    }

    private static void renderChannel(SectionBuilder section, Channel channel) {
        double size = NavySidebarIcons.size(channel.token());
        section.addParagraph(p -> p
                .name("Contact_" + compact(channel.token()))
                .textStyle(sidebarBody())
                .inlineImage(NavySidebarIcons.image(channel.token()), size, size,
                        InlineImageAlignment.CENTER, 0.0, null)
                // The gap between the mark and the value is set as spaces
                // rather than an indent: the row is one paragraph, so the
                // mark and the text share a baseline and wrap together.
                .inlineText("   " + channel.value(), sidebarBody())
                .margin(new DocumentInsets(0, 0, CONTACT_ROW_GAP, 0)));
    }

    private static List<Channel> channels(CvIdentity identity) {
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(NavySidebarIcons.PHONE, identity.contact().phone()));
        channels.add(new Channel(NavySidebarIcons.EMAIL, identity.contact().email()));
        channels.add(new Channel(NavySidebarIcons.LOCATION, identity.contact().address()));
        for (Link link : identity.links()) {
            channels.add(new Channel(NavySidebarIcons.LINKEDIN, link.label()));
        }
        return channels;
    }

    /**
     * A contact row. The packaged set has one network mark, so every link
     * takes it — this design ships no globe.
     */
    private record Channel(String token, String value) {
    }

    // -- education -------------------------------------------------------

    /**
     * Each degree as four lines: the qualification in capitals, the
     * institution, where it was, and the years.
     */
    private static void renderEducation(SectionBuilder section, EntriesSection education) {
        sidebarHeading(section, education.title(), false);
        for (CvEntry entry : education.entries()) {
            section.addParagraph(p -> p
                    .name("Degree_" + compact(entry.title()))
                    .text(entry.title().toUpperCase(Locale.ROOT))
                    .textStyle(style(BODY_SIZE, SIDEBAR_STRONG, DocumentTextDecoration.BOLD))
                    .lineSpacing(BULLET_LEADING)
                    .margin(new DocumentInsets(0, 0, EDU_DEGREE_GAP, 0)));
            line(section, "Institution_" + compact(entry.title()), entry.subtitle(),
                    EDU_LINE_GAP);
            line(section, "Campus_" + compact(entry.title()), entry.body(), EDU_LINE_GAP);
            line(section, "Years_" + compact(entry.title()), entry.date(), EDU_ENTRY_GAP);
        }
    }

    private static void line(SectionBuilder section, String name, String text, double gapBelow) {
        section.addParagraph(p -> p
                .name(name)
                .text(text)
                .textStyle(sidebarBody())
                .margin(new DocumentInsets(0, 0, gapBelow, 0)));
    }

    // -- skills ----------------------------------------------------------

    /**
     * The skills as a plain bullet list. This design draws no meters, so a
     * level the document carries is not read here.
     */
    private static void renderSkills(SectionBuilder section, SkillsSection skills) {
        sidebarHeading(section, skills.title(), false);
        section.addList(list -> list
                .name("SkillsList")
                .bullet()
                .items(names(skills))
                .textStyle(sidebarBody())
                .lineSpacing(BULLET_LEADING)
                .itemSpacing(SKILL_ITEM_GAP)
                .margin(DocumentInsets.zero()));
    }

    /**
     * The section's skills as one list of names. This design sets no group
     * headings, so a document that groups its skills gets them in the order
     * the groups were declared.
     */
    private static List<String> names(SkillsSection section) {
        List<String> out = new ArrayList<>();
        for (SkillGroup group : section.groups()) {
            for (CvSkill skill : group.entries()) {
                out.add(skill.name());
            }
        }
        return out;
    }

    // -- languages -------------------------------------------------------

    /**
     * Language rows: a dotted label at the left, the proficiency anchored at
     * a fixed offset behind a dash so the second column lines up whatever
     * the names measure.
     *
     * <p>The rows are {@link CvRow}s rather than levelled skills because
     * this design writes the proficiency out — "Native", "Advanced" — and a
     * number could not carry those words back.</p>
     */
    private static void renderLanguages(SectionBuilder section, RowsSection languages) {
        sidebarHeading(section, languages.title(), false);
        // The row gap is section spacing rather than a margin on each band:
        // the bands are shape containers, and a container's margin would sit
        // inside the clip rather than between the rows.
        section.spacing(LANGUAGE_ROW_GAP);
        for (CvRow row : languages.rows()) {
            DocumentNode label = new ParagraphBuilder()
                    .name("Language_" + compact(row.label()))
                    .textStyle(sidebarBody())
                    .dot(LIST_BULLET_DIAMETER, SIDEBAR_TEXT)
                    .inlineText("  " + row.label(), sidebarBody())
                    .margin(DocumentInsets.zero())
                    .build();
            DocumentNode value = new ParagraphBuilder()
                    .name("Proficiency_" + compact(row.label()))
                    .text("–   " + row.body())
                    .textStyle(sidebarBody())
                    .margin(DocumentInsets.zero())
                    .build();
            section.addContainer(band -> band
                    .name("LanguageRow_" + compact(row.label()))
                    .rectangle(SIDEBAR_INNER_WIDTH, SIDEBAR_BAND_HEIGHT)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .centerLeft(label)
                    .position(value, LANGUAGE_VALUE_X, 0, LayerAlign.CENTER_LEFT));
        }
    }
}
