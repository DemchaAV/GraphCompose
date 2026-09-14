package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarEducation.renderEducation;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_TO_SKILLS_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_TO_SKILLS_BELOW;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TO_LANGUAGES_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TO_LANGUAGES_BELOW;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.HEADER_PLATE_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_DOTS;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_DOT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_RATING_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_RATING_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.LANGUAGE_ROW_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MONOGRAM_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MONOGRAM_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MONOGRAM_STROKE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MONOGRAM_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RATING_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RULE_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_BODY_TOP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_PAD_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILLS_TO_EDUCATION_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILLS_TO_EDUCATION_BELOW;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILL_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILL_ROW_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILL_TRACK_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SKILL_TRACK_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PLATE_BACKGROUND;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.STANDARD_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.body;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.clamp01;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.tracked;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.paragraph;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.sidebarDivider;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.sidebarHeading;

/**
 * The pale left column: the navy monogram plate, the contact channels, the
 * skill meters, the education rail and the language ratings.
 *
 * <p>The column has four berths in a fixed order, each with its own measured
 * gap to the one above. A berth with nothing to draw takes its leading
 * hairline with it, so a CV that carries no languages does not end on a rule
 * with empty space under it.</p>
 */
final class ProfessionalSidebarAside {

    /**
     * The contact heading. It is not read from the document because the
     * channels are not a section — they come off the identity, which carries
     * no title of its own.
     */
    static final String CONTACT_HEADING = "CONTACT";

    private ProfessionalSidebarAside() {
    }

    static void compose(SectionBuilder section,
                        CvIdentity identity,
                        SkillsSection skills,
                        EntriesSection education,
                        SkillsSection languages) {
        section.spacing(0);
        section.add(monogramPlate(identity));
        section.addSection("SidebarBody", body -> {
            body.spacing(0);
            body.padding(new DocumentInsets(SIDEBAR_BODY_TOP, SIDEBAR_PAD_X, 0, SIDEBAR_PAD_X));
            body.addSection("Contact", contact -> renderContact(contact, identity));
            if (SectionLookup.hasContent(skills)) {
                sidebarDivider(body, CONTACT_TO_SKILLS_ABOVE, CONTACT_TO_SKILLS_BELOW);
                body.addSection("Skills", host -> renderSkills(host, skills));
            }
            if (SectionLookup.hasContent(education)) {
                sidebarDivider(body, SKILLS_TO_EDUCATION_ABOVE, SKILLS_TO_EDUCATION_BELOW);
                body.addSection("Education", host -> renderEducation(host, education));
            }
            if (SectionLookup.hasContent(languages)) {
                sidebarDivider(body, EDUCATION_TO_LANGUAGES_ABOVE, EDUCATION_TO_LANGUAGES_BELOW);
                body.addSection("Languages", host -> renderLanguages(host, languages));
            }
        });
    }

    /**
     * The column's lists as blocks that move to another page whole, for a CV
     * longer than the sheet: the contact channels, then the skills, the
     * education and the languages, each led by the hairline above it, which
     * is left out when the list opens a page.
     */
    static List<ColumnPages.Block> blocks(CvIdentity identity,
                                          SkillsSection skills,
                                          EntriesSection education,
                                          SkillsSection languages) {
        List<ColumnPages.Block> blocks = new ArrayList<>();
        blocks.add(ColumnPages.Block.of("Contact",
                body -> body.addSection("Contact", contact -> renderContact(contact, identity))));
        if (SectionLookup.hasContent(skills)) {
            blocks.add(new ColumnPages.Block("Skills",
                    body -> sidebarDivider(body, CONTACT_TO_SKILLS_ABOVE, CONTACT_TO_SKILLS_BELOW),
                    body -> body.addSection("Skills", host -> renderSkills(host, skills))));
        }
        if (SectionLookup.hasContent(education)) {
            blocks.add(new ColumnPages.Block("Education",
                    body -> sidebarDivider(body, SKILLS_TO_EDUCATION_ABOVE, SKILLS_TO_EDUCATION_BELOW),
                    body -> body.addSection("Education", host -> renderEducation(host, education))));
        }
        if (SectionLookup.hasContent(languages)) {
            blocks.add(new ColumnPages.Block("Languages",
                    body -> sidebarDivider(body, EDUCATION_TO_LANGUAGES_ABOVE,
                            EDUCATION_TO_LANGUAGES_BELOW),
                    body -> body.addSection("Languages", host -> renderLanguages(host, languages))));
        }
        return blocks;
    }

    /**
     * One page of the column: the monogram plate on the first page only, then
     * the padded body every page carries, holding the blocks the plan put
     * there.
     */
    static void composePage(SectionBuilder section, CvIdentity identity, boolean firstPage,
                            List<ColumnPages.Block> blocks, List<Integer> page) {
        section.spacing(0);
        if (firstPage) {
            section.add(monogramPlate(identity));
        }
        section.addSection("SidebarBody", body -> {
            body.spacing(0);
            body.padding(new DocumentInsets(SIDEBAR_BODY_TOP, SIDEBAR_PAD_X, 0, SIDEBAR_PAD_X));
            ColumnPages.compose(body, blocks, page);
        });
    }

    // -- monogram --------------------------------------------------------

    /**
     * The navy plate that caps the column, with the initials set inside a
     * hairline ring.
     *
     * <p>The plate is drawn rather than painted as a page background because
     * it is the first thing in the flow: the sidebar content stacks under it,
     * and a background would not push anything down.</p>
     */
    private static DocumentNode monogramPlate(CvIdentity identity) {
        DocumentTextStyle initialsStyle = style(BODY_FONT, MONOGRAM_SIZE,
                DocumentColor.WHITE, DocumentTextDecoration.DEFAULT);
        ParagraphBuilder initials = new ParagraphBuilder()
                .name("MonogramInitials")
                .textStyle(initialsStyle)
                .align(TextAlign.CENTER);
        tracked(initials, initials(identity), initialsStyle, MONOGRAM_TRACKING_EM);

        DocumentNode ring = new ShapeContainerBuilder()
                .name("MonogramRing")
                .circle(MONOGRAM_DIAMETER)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .stroke(DocumentStroke.of(DocumentColor.WHITE, MONOGRAM_STROKE))
                .center(initials.margin(DocumentInsets.zero()).build())
                .build();

        return new ShapeContainerBuilder()
                .name("MonogramPlate")
                .rectangle(SIDEBAR_WIDTH, HEADER_PLATE_HEIGHT)
                .fillColor(PLATE_BACKGROUND)
                .center(ring)
                .build();
    }

    /** First letter of the given name, first letter of the family name. */
    private static String initials(CvIdentity identity) {
        String first = initial(identity.name().first());
        String last = initial(identity.name().last());
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private static String initial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }

    // -- contact ---------------------------------------------------------

    /**
     * The contact channels, in the order the design sets them: phone, email,
     * address, then whatever links the identity carries.
     *
     * <p>The phone and email rows link to {@code tel:} and {@code mailto:}
     * targets built from the value, so the channels are dialable and
     * writable from the PDF without the document carrying the URI twice.</p>
     */
    private static void renderContact(SectionBuilder section, CvIdentity identity) {
        sidebarHeading(section, CONTACT_HEADING, CONTACT_HEADING_TO_BODY);
        List<Channel> channels = channels(identity);
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            double gapBelow = i + 1 < channels.size() ? CONTACT_ROW_GAP : 0;
            renderChannel(section, channel, gapBelow);
        }
    }

    private static void renderChannel(SectionBuilder section, Channel channel,
                                      double gapBelow) {
        DocumentImageData icon = ProfessionalSidebarIcons.image(channel.token());
        double size = ProfessionalSidebarIcons.size(channel.token());
        DocumentLinkOptions link = channel.href() == null
                ? null
                : new DocumentLinkOptions(channel.href());
        ParagraphBuilder row = new ParagraphBuilder()
                .name("Contact_" + compact(channel.token()))
                .textStyle(body())
                .inlineImage(icon, size, size, InlineImageAlignment.CENTER, 0, link);
        // The gap between the mark and the value is set as spaces rather than
        // an indent: the row is one paragraph, so the mark and the text share
        // a baseline and wrap together.
        if (link == null) {
            row.inlineText("    " + channel.value(), body());
        } else {
            row.inlineText("    " + channel.value(), body(), link);
        }
        section.add(row.margin(new DocumentInsets(0, 0, gapBelow, 0)).build());
    }

    private static List<Channel> channels(CvIdentity identity) {
        List<Channel> channels = new ArrayList<>();
        String phone = identity.contact().phone();
        channels.add(new Channel(ProfessionalSidebarIcons.PHONE, phone, ContactUri.tel(phone)));
        String email = identity.contact().email();
        channels.add(new Channel(ProfessionalSidebarIcons.EMAIL, email, "mailto:" + email));
        channels.add(new Channel(ProfessionalSidebarIcons.LOCATION,
                identity.contact().address(), null));
        for (Link link : identity.links()) {
            channels.add(new Channel(linkToken(link), link.label(), link.url()));
        }
        return channels;
    }

    /**
     * The mark for a link. The packaged set names one network; everything
     * else is a site, which is what the globe stands for.
     */
    private static String linkToken(Link link) {
        String haystack = SectionLookup.normalize(link.label() + " " + link.url());
        return haystack.contains("linkedin")
                ? ProfessionalSidebarIcons.LINKEDIN
                : ProfessionalSidebarIcons.WEBSITE;
    }

    private record Channel(String token, String value, String href) {
    }

    // -- skills ----------------------------------------------------------

    /**
     * Skill rows: the name at the left of a fixed-height band, the meter at
     * the right. A skill the document leaves unlevelled draws its name alone
     * rather than an empty track.
     */
    private static void renderSkills(SectionBuilder section, SkillsSection skills) {
        sidebarHeading(section, skills.title(), STANDARD_HEADING_TO_BODY);
        section.addSection("SkillRows", rows -> {
            rows.spacing(SKILL_ROW_GAP);
            for (CvSkill skill : flatten(skills)) {
                DocumentNode label = paragraph("Skill_" + compact(skill.name()),
                        skill.name(), body(), TextAlign.LEFT);
                rows.addContainer(row -> {
                    row.name("SkillRow_" + compact(skill.name()))
                            .rectangle(SIDEBAR_INNER_WIDTH, SKILL_ROW_HEIGHT)
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .centerLeft(label);
                    if (skill.level().isPresent()) {
                        row.centerRight(skillMeter(skill.level().getAsDouble()));
                    }
                });
            }
        });
    }

    private static DocumentNode skillMeter(double level) {
        DocumentNode fill = new ShapeBuilder()
                .name("SkillMeterFill")
                .size(SKILL_TRACK_WIDTH * clamp01(level), SKILL_TRACK_HEIGHT)
                .fillColor(PLATE_BACKGROUND)
                .build();
        return new ShapeContainerBuilder()
                .name("SkillMeter")
                .rectangle(SKILL_TRACK_WIDTH, SKILL_TRACK_HEIGHT)
                .fillColor(RULE_MUTED)
                .centerLeft(fill)
                .build();
    }

    // -- languages -------------------------------------------------------

    /**
     * Language rows: the name at the left, a five-dot rating anchored at a
     * fixed offset so the ratings line up whatever the names measure. A
     * language the document leaves unlevelled draws its name alone.
     */
    private static void renderLanguages(SectionBuilder section, SkillsSection languages) {
        sidebarHeading(section, languages.title(), STANDARD_HEADING_TO_BODY);
        section.addSection("LanguageRows", rows -> {
            rows.spacing(LANGUAGE_ROW_GAP);
            for (CvSkill language : flatten(languages)) {
                DocumentNode label = paragraph("Language_" + compact(language.name()),
                        language.name(), body(), TextAlign.LEFT);
                rows.addContainer(row -> {
                    row.name("LanguageRow_" + compact(language.name()))
                            .rectangle(SIDEBAR_INNER_WIDTH, LANGUAGE_ROW_HEIGHT)
                            .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                            .centerLeft(label);
                    if (language.level().isPresent()) {
                        row.position(ratingDots(language.level().getAsDouble()),
                                LANGUAGE_RATING_X, 0, LayerAlign.CENTER_LEFT);
                    }
                });
            }
        });
    }

    /**
     * The rating: five dots, of which the level fills the nearest whole
     * number — the model carries a fraction, and this design counts.
     */
    private static DocumentNode ratingDots(double level) {
        long filled = Math.round(clamp01(level) * LANGUAGE_DOTS);
        ShapeContainerBuilder rating = new ShapeContainerBuilder()
                .name("LanguageRating")
                .rectangle(LANGUAGE_RATING_WIDTH, LANGUAGE_DOT_DIAMETER)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE);
        for (int i = 0; i < LANGUAGE_DOTS; i++) {
            DocumentNode dot = new EllipseBuilder()
                    .name("LanguageDot_" + i)
                    .circle(LANGUAGE_DOT_DIAMETER)
                    .fillColor(i < filled ? PLATE_BACKGROUND : RATING_MUTED)
                    .build();
            rating.position(dot, i * LANGUAGE_DOT_PITCH, 0, LayerAlign.CENTER_LEFT);
        }
        return rating.build();
    }

    /**
     * The section's skills as one list. This design sets no group headings,
     * so a document that groups its skills gets them in the order the groups
     * were declared.
     */
    private static List<CvSkill> flatten(SkillsSection section) {
        List<CvSkill> out = new ArrayList<>();
        for (SkillGroup group : section.groups()) {
            out.addAll(group.entries());
        }
        return out;
    }
}
