package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
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
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_TO_SKILLS_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.CONTACT_TO_SKILLS_BELOW;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_DEGREE_AIR;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_DEGREE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_RAIL_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_RAIL_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TEXT_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TO_LANGUAGES_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TO_LANGUAGES_BELOW;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_HEAD_HEIGHT;
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
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_PRIMARY;
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

    // -- education -------------------------------------------------------

    /**
     * The education rail: a hairline down the left of the block with a dot
     * on each entry's first line.
     *
     * <p>The rail runs from the first dot to the foot of the last entry, which is what the
     * block is drawn as and what it now asks for. It used to be the section's left accent,
     * and the first entry filled its own head band with the sidebar colour, masked the rail's
     * protruding edge above its dot and redrew the rail below it, meaning to start the line
     * at the first marker. Measured against the render, that never happened: an accent draws
     * above the fill and above the mask, so the rail stayed visible for the 2.5pt above the
     * first dot and the only thing the construction achieved was drawing that stretch below
     * the dot twice. The mask and the redraw are gone; the rail is the timeline's and says
     * what it does.</p>
     */
    private static void renderEducation(SectionBuilder section, EntriesSection education) {
        sidebarHeading(section, education.title(), EDUCATION_HEADING_TO_BODY);
        SectionBuilder holder = new SectionBuilder();
        holder.name("EducationRailHolder");
        holder.spacing(0);
        // The axis column centres the rail on itself, so the timeline starts half a dot left
        // of the column the accent drew on and the line lands back on it.
        holder.margin(new DocumentInsets(0, 0, 0,
                EDUCATION_RAIL_X - EDUCATION_MARKER_DIAMETER / 2.0));
        holder.addTimeline(timeline -> {
            timeline.markerOnRail()
                    // Begin at the first dot, run on to the foot of the entries. That is
                    // what this block is drawn as, and what the mask-and-redraw it replaced
                    // was trying to fake — it could not be asked for while the rail's two
                    // ends were one value.
                    .rail(rail -> rail
                            .stroke(DocumentStroke.of(RULE_MUTED, EDUCATION_RAIL_WIDTH))
                            .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.ENTRY_BOUND))
                    .axisWidth(EDUCATION_MARKER_DIAMETER)
                    .markerGap(EDUCATION_TEXT_X - EDUCATION_MARKER_DIAMETER / 2.0)
                    .gutter(0)
                    .spacing(EDUCATION_ENTRY_GAP);
            List<CvEntry> entries = education.entries();
            for (int i = 0; i < entries.size(); i++) {
                int index = i;
                CvEntry entry = entries.get(i);
                timeline.entry(educationMarker(index), e -> e.content(body -> {
                    body.spacing(0);
                    renderEducationHead(body, entry, index);
                    body.addParagraph(p -> p
                            .name("EducationInstitution_" + index)
                            .text(entry.subtitle())
                            .textStyle(style(BODY_FONT, BODY_SIZE, TEXT_MUTED,
                                    DocumentTextDecoration.ITALIC))
                            .margin(new DocumentInsets(
                                    EDUCATION_LINE_GAP, 0, EDUCATION_LINE_GAP, 0)));
                    body.addParagraph(p -> p
                            .name("EducationDates_" + index)
                            .text(entry.date())
                            .textStyle(body())
                            .margin(DocumentInsets.zero()));
                }));
            }
        });
        DocumentNode educationRail = holder.build();
        section.addLayerStack(stack -> stack
                .name("EducationRail")
                .layer(educationRail, LayerAlign.TOP_LEFT, 0));
    }

    /**
     * The accent dot that marks a degree, in a box as tall as the head band it rides.
     *
     * <p>The dot used to be centred in that band by the container positioning it; a timeline
     * top-aligns its marker, so the band height is declared here and the dot carries the half
     * band above it. Same box, same centre, and the rail still runs through it.</p>
     */
    private static TimelineMarker educationMarker(int index) {
        double halfBand = (ENTRY_HEAD_HEIGHT - EDUCATION_MARKER_DIAMETER) / 2.0;
        return TimelineMarker.custom(EDUCATION_MARKER_DIAMETER, ENTRY_HEAD_HEIGHT,
                column -> column.addEllipse(ellipse -> ellipse
                        .name("EducationMarker_" + index)
                        .circle(EDUCATION_MARKER_DIAMETER)
                        .fillColor(ACCENT_PRIMARY)
                        .margin(new DocumentInsets(halfBand, 0, 0, 0))));
    }

    /**
     * The degree title — the entry's own first line, not a band of declared height.
     *
     * <p>It was a container of exactly {@code ENTRY_HEAD_HEIGHT} with the title centred in
     * it, which put the title's centre on the dot's, the dot being centred in a box of the
     * same height. That works for a title of one line and for no other. "MSc Advanced
     * Computer Science and Software Engineering" needs three lines in a sidebar this narrow,
     * and a box told to be 9.3pt tall stayed 9.3pt tall; under
     * {@link ClipPolicy#OVERFLOW_VISIBLE} the extra lines drew rather than vanished — 16.840pt
     * of them, over the institution and the dates below.</p>
     *
     * <p>Nothing about a one-line title moves. The band's surplus over one line is now
     * {@link ProfessionalSidebarStyles#EDUCATION_DEGREE_AIR} above and below the title, so it
     * occupies the same 9.3pt and its centre stays on the dot's; and the width is the same
     * number too, since the container declared
     * {@code SIDEBAR_INNER_WIDTH - EDUCATION_RAIL_X - EDUCATION_TEXT_X} and the timeline's
     * content column resolves to exactly that out of {@code axisWidth} and {@code markerGap}.
     * What changes is only that a title needing a second line now gets one.</p>
     */
    private static void renderEducationHead(SectionBuilder rail, CvEntry entry, int index) {
        rail.addParagraph(p -> p
                .name("EducationDegree_" + index)
                .text(entry.title())
                .textStyle(style(BODY_FONT, EDUCATION_DEGREE_SIZE, TEXT_PRIMARY,
                        DocumentTextDecoration.BOLD))
                .align(TextAlign.LEFT)
                .lineSpacing(BODY_LEADING)
                .margin(new DocumentInsets(
                        EDUCATION_DEGREE_AIR, 0, EDUCATION_DEGREE_AIR, 0)));
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
