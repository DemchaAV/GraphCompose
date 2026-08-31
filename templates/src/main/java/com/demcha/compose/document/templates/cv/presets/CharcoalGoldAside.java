package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
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

import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.BLOCK_TO_DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CONTACT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.DIVIDER_TO_HEADING;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EDUCATION_ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EDUCATION_INDENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EDUCATION_LINE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.LANGUAGE_NAME_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.LANGUAGE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.PHOTO_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.PHOTO_RING_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.PHOTO_TO_CONTACT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RATING_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RATING_EMPTY;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_INK;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_PAD;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SKILL_BULLET_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SKILL_BULLET_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SKILL_NAME_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SKILL_PITCH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.textStyle;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.inlineGap;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.layeredRow;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.sidebarHeading;

/**
 * The charcoal left column: the ringed photograph, the contact channels
 * behind their marks, the rated skills, the languages and the degrees, with
 * a hairline between each block.
 */
final class CharcoalGoldAside {

    /**
     * The contact heading. It is not read from the document because the
     * channels are not a section — they come off the identity, which carries
     * no title of its own.
     */
    static final String CONTACT_HEADING = "CONTACT";

    /** How many dots a skill rating is drawn out of. */
    static final int RATING_SCALE = 5;

    private CharcoalGoldAside() {
    }

    static void compose(SectionBuilder side,
                        CvIdentity identity,
                        SkillsSection skills,
                        RowsSection languages,
                        EntriesSection education) {
        side.name("Sidebar");
        side.spacing(0);
        // No bottom padding: the sidebar's fill is a page background that
        // already reaches the paper edge, so padding here would only shorten
        // the column.
        side.padding((float) SIDEBAR_PAD_TOP, (float) SIDEBAR_PAD, 0f, (float) SIDEBAR_PAD);

        identity.portrait().ifPresent(photo -> renderPhoto(side, photo));
        renderContact(side, identity, identity.portrait().isPresent());
        if (SectionLookup.hasContent(skills)) {
            divider(side, "AfterContact");
            renderSkills(side, skills);
        }
        if (SectionLookup.hasContent(languages)) {
            divider(side, "AfterSkills");
            renderLanguages(side, languages);
        }
        if (SectionLookup.hasContent(education)) {
            divider(side, "AfterLanguages");
            renderEducation(side, education);
        }
    }

    private static void divider(SectionBuilder side, String name) {
        side.addLine(line -> line
                .name("Divider_" + name)
                .fill()
                .thickness(RULE_THICKNESS)
                .color(SIDEBAR_RULE)
                .margin(new DocumentInsets(BLOCK_TO_DIVIDER, 0, 0, 0)));
    }

    // -- photograph ------------------------------------------------------

    /**
     * The photograph, clipped to a disc inside a pale ring. It is drawn with
     * {@code COVER}, so a picture that is not square fills the circle and is
     * cut rather than squeezed.
     */
    private static void renderPhoto(SectionBuilder side, DocumentImageData photo) {
        DocumentNode portrait = new ImageBuilder()
                .name("PortraitImage")
                .source(photo)
                .size(PHOTO_DIAMETER, PHOTO_DIAMETER)
                .fitMode(DocumentImageFitMode.COVER)
                .build();
        DocumentNode circle = new ShapeContainerBuilder()
                .name("ProfilePhoto")
                .circle(PHOTO_DIAMETER)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .fillColor(SIDEBAR)
                .stroke(DocumentStroke.of(RULE, PHOTO_RING_WIDTH))
                .center(portrait)
                .build();
        side.addAligned(HorizontalAlign.CENTER, circle);
    }

    // -- contact ---------------------------------------------------------

    /**
     * The contact channels, in the order the design sets them: phone, email,
     * address, then whatever links the identity carries. The phone and the
     * email carry {@code tel:} and {@code mailto:} targets built from the
     * values, and each link its own url.
     */
    private static void renderContact(SectionBuilder side, CvIdentity identity,
                                      boolean afterPhoto) {
        side.addSection("Contact", block -> {
            block.spacing(0);
            block.margin(afterPhoto ? (float) PHOTO_TO_CONTACT : 0f, 0f, 0f, 0f);
            sidebarHeading(block, CONTACT_HEADING);
            List<Channel> channels = channels(identity);
            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.get(i);
                boolean first = i == 0;
                int index = i;
                block.addParagraph(p -> {
                    p.name("Contact_" + index);
                    p.inlineSvgIcon(CharcoalGoldIcons.icon(channel.token()),
                            CharcoalGoldIcons.CONTACT_SIZE, InlineImageAlignment.CENTER);
                    inlineGap(p, textStyle(DETAIL_SIZE, SIDEBAR_INK, false));
                    if (channel.href() == null) {
                        p.inlineText(channel.value(), textStyle(DETAIL_SIZE, SIDEBAR_INK, false));
                    } else {
                        p.inlineText(channel.value(), textStyle(DETAIL_SIZE, SIDEBAR_INK, false),
                                new DocumentLinkOptions(channel.href()));
                    }
                    p.margin(first ? (float) HEADING_TO_BODY : 0f, 0f,
                            (float) gap(CONTACT_PITCH, DETAIL_SIZE), 0f);
                });
            }
        });
    }

    private static List<Channel> channels(CvIdentity identity) {
        List<Channel> channels = new ArrayList<>();
        String phone = identity.contact().phone();
        channels.add(new Channel(CharcoalGoldIcons.PHONE, phone, telUri(phone)));
        String email = identity.contact().email();
        channels.add(new Channel(CharcoalGoldIcons.EMAIL, email, "mailto:" + email));
        channels.add(new Channel(CharcoalGoldIcons.LOCATION,
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
     * The mark for a link: the network's own when it names one, the globe
     * otherwise.
     */
    private static String linkToken(Link link) {
        String haystack = SectionLookup.normalize(link.label() + " " + link.url());
        return haystack.contains("linkedin")
                ? CharcoalGoldIcons.LINKEDIN
                : CharcoalGoldIcons.WEBSITE;
    }

    private record Channel(String token, String value, String href) {
    }

    // -- skills ----------------------------------------------------------

    /**
     * Skill rows: a gold bullet, the name, and a rating drawn as five dots
     * of which the level fills the nearest whole number.
     *
     * <p>A skill the document leaves unlevelled draws no rating at all,
     * rather than five empty dots that read as a score of zero.</p>
     */
    private static void renderSkills(SectionBuilder side, SkillsSection skills) {
        side.addSection("Skills", block -> {
            block.spacing(0);
            block.margin((float) DIVIDER_TO_HEADING, 0f, 0f, 0f);
            sidebarHeading(block, skills.title());
            List<CvSkill> items = flatten(skills);
            for (int i = 0; i < items.size(); i++) {
                CvSkill skill = items.get(i);
                boolean first = i == 0;
                int index = i;
                layeredRow(block, "Skill_" + index,
                        first ? HEADING_TO_BODY : 0.0,
                        gap(SKILL_PITCH, DETAIL_SIZE),
                        row -> {
                            row.verticalAlign(RowVerticalAlign.CENTER);
                            row.columns(
                                    DocumentRowColumn.fixed(SKILL_BULLET_COLUMN),
                                    DocumentRowColumn.weight(SKILL_NAME_WEIGHT),
                                    DocumentRowColumn.weight(1.0 - SKILL_NAME_WEIGHT));
                            row.addParagraph(p -> p
                                    .name("SkillBullet_" + index)
                                    .align(TextAlign.CENTER)
                                    // Styled even though it holds no text: a
                                    // paragraph takes its line box from its
                                    // type whether or not a glyph uses it, and
                                    // the default size would set the row's
                                    // pitch instead of this one.
                                    .textStyle(textStyle(DETAIL_SIZE, SIDEBAR_INK, false))
                                    .dot(SKILL_BULLET_DIAMETER, ACCENT));
                            row.addParagraph(p -> p
                                    .name("SkillName_" + index)
                                    .text(skill.name())
                                    .textStyle(textStyle(DETAIL_SIZE, SIDEBAR_INK, false)));
                            row.addParagraph(p -> renderRating(p, index, skill));
                        });
            }
        });
    }

    private static void renderRating(ParagraphBuilder p, int index, CvSkill skill) {
        p.name("SkillRating_" + index);
        p.align(TextAlign.RIGHT);
        if (skill.level().isEmpty()) {
            return;
        }
        long filled = Math.round(Math.max(0.0, Math.min(1.0, skill.level().getAsDouble()))
                * RATING_SCALE);
        for (int d = 0; d < RATING_SCALE; d++) {
            p.dot(RATING_DOT_DIAMETER, d < filled ? ACCENT : RATING_EMPTY);
            if (d < RATING_SCALE - 1) {
                p.inlineText(" ", textStyle(DETAIL_SIZE, SIDEBAR_INK, false));
            }
        }
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

    // -- languages -------------------------------------------------------

    /**
     * Language rows: the name and the level in two columns, so the levels
     * line up whatever the names measure.
     *
     * <p>They are {@link CvRow}s rather than levelled skills because this
     * design writes the level out — "Native", "B2 – Upper Intermediate" —
     * which a number could not carry back. The {@code RowStyle} is not
     * read.</p>
     */
    private static void renderLanguages(SectionBuilder side, RowsSection languages) {
        side.addSection("Languages", block -> {
            block.spacing(0);
            block.margin((float) DIVIDER_TO_HEADING, 0f, 0f, 0f);
            sidebarHeading(block, languages.title());
            List<CvRow> items = languages.rows();
            for (int i = 0; i < items.size(); i++) {
                CvRow language = items.get(i);
                boolean first = i == 0;
                int index = i;
                layeredRow(block, "Language_" + index,
                        first ? HEADING_TO_BODY : 0.0,
                        gap(LANGUAGE_PITCH, BODY_SIZE),
                        row -> {
                            row.weights(LANGUAGE_NAME_WEIGHT, 1.0 - LANGUAGE_NAME_WEIGHT);
                            row.addParagraph(p -> p
                                    .name("LanguageName_" + index)
                                    .text(language.label())
                                    .textStyle(textStyle(BODY_SIZE, SIDEBAR_INK, false)));
                            row.addParagraph(p -> p
                                    .name("LanguageLevel_" + index)
                                    .text(language.body())
                                    .textStyle(textStyle(BODY_SIZE, SIDEBAR_INK, false)));
                        });
            }
        });
    }

    // -- education -------------------------------------------------------

    /**
     * Each degree as three lines: the qualification behind a gold dot, then
     * the institution and the years indented under it.
     */
    private static void renderEducation(SectionBuilder side, EntriesSection education) {
        side.addSection("Education", block -> {
            block.spacing(0);
            block.margin((float) DIVIDER_TO_HEADING, 0f, 0f, 0f);
            sidebarHeading(block, education.title());
            List<CvEntry> items = education.entries();
            for (int i = 0; i < items.size(); i++) {
                CvEntry entry = items.get(i);
                boolean first = i == 0;
                int index = i;
                block.addSection("EducationEntry_" + index, e -> {
                    e.spacing(0);
                    e.margin(first ? (float) HEADING_TO_BODY : (float) EDUCATION_ENTRY_GAP,
                            0f, 0f, 0f);
                    e.addParagraph(p -> {
                        p.name("Degree_" + index);
                        p.dot(RATING_DOT_DIAMETER, ACCENT);
                        inlineGap(p, textStyle(DETAIL_SIZE, SIDEBAR_INK, false));
                        p.inlineText(entry.title(), textStyle(DETAIL_SIZE, SIDEBAR_INK, true));
                        p.margin(0f, 0f, (float) gap(EDUCATION_LINE_PITCH, DETAIL_SIZE), 0f);
                    });
                    e.addSection("EducationDetail_" + index, d -> {
                        d.spacing(0);
                        d.padding(0f, 0f, 0f, (float) EDUCATION_INDENT);
                        d.addParagraph(p -> p
                                .name("Institution_" + index)
                                .text(entry.subtitle())
                                .textStyle(textStyle(DETAIL_SIZE, SIDEBAR_INK, false))
                                .margin(0f, 0f,
                                        (float) gap(EDUCATION_LINE_PITCH, DETAIL_SIZE), 0f));
                        d.addParagraph(p -> p
                                .name("Period_" + index)
                                .text(entry.date())
                                .textStyle(textStyle(DETAIL_SIZE, SIDEBAR_INK, false)));
                    });
                });
            }
        });
    }
}
