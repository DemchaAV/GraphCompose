package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.core.identity.Contact;
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
import java.util.OptionalDouble;
import java.util.regex.Pattern;

import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_CONTENT_PAD;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_DIM;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_SMALL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_TEXT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CONTACT_ICON_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CONTACT_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.EDUCATION_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.EDUCATION_YEARS_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.FACE_SPACE_ADVANCE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_DOTS;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_DOT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_DOT_STROKE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_GROUP_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.LANGUAGE_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MONOGRAM_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MONOGRAM_PITCH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MONOGRAM_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MONOGRAM_STROKE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.NAME_DIVIDER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.SKILL_KNOB_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.SKILL_KNOB_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.SKILL_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.SKILL_TRACK_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.SKILL_TRACK_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.TRACK_DIM;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.WHITE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.asideText;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.px;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.style;

/**
 * The navy column: the monogram and name plate, then contact, education, skills
 * and languages, each under a heading and its rule.
 *
 * <p>The monogram is an outlined circle with no fill, so the plate shows
 * through it, and the initials are anchored on its centre — they are content
 * <em>of</em> the circle rather than a paragraph laid over it.</p>
 *
 * <p>A skill meter is three layers on one stack: the dim full-width track, the
 * bright filled run, and the knob positioned along the track in proportion to
 * the level. All three hang off {@link LayerAlign#CENTER_LEFT} so they share
 * the track's axis; seating a layer at the stack's top instead draws one meter
 * as three unaligned pieces.</p>
 */
final class MidnightNavyAside {

    /** The heading the design gives the contact block, which no section carries. */
    static final String CONTACT_HEADING = "CONTACT";

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one, so it is dropped from the dialled form.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private MidnightNavyAside() {
    }

    /**
     * Draws the column.
     *
     * @param column    the aside cell
     * @param identity  whose CV this is
     * @param education the degrees, or {@code null}
     * @param skills    the rated skills, or {@code null}
     * @param languages the rated languages, or {@code null}
     */
    static void compose(SectionBuilder column, CvIdentity identity, EntriesSection education,
                        SkillsSection skills, SkillsSection languages) {
        // Only the top inset belongs to the plate: the monogram is centred on
        // the plate's full width, not on the text column, so the horizontal
        // inset is owned by the blocks under it instead.
        column.spacing(0).padding(new DocumentInsets(ASIDE_PAD_TOP, 0, 0, 0));
        renderMonogram(column, identity);
        renderNamePlate(column, identity);
        renderContact(column, identity);
        if (SectionLookup.hasContent(education)) {
            renderEducation(column, education);
        }
        if (SectionLookup.hasContent(skills)) {
            renderSkills(column, skills);
        }
        if (SectionLookup.hasContent(languages)) {
            renderLanguages(column, languages);
        }
    }

    /** The outlined circle, carrying one initial per line of the name. */
    private static void renderMonogram(SectionBuilder column, CvIdentity identity) {
        List<String> initials = List.of(
                initial(identity.name().first()), initial(identity.name().last()));
        ShapeContainerBuilder circle = new ShapeContainerBuilder()
                .name("Monogram")
                .circle(MONOGRAM_DIAMETER)
                .stroke(DocumentStroke.of(WHITE, MONOGRAM_STROKE));
        double top = -MONOGRAM_PITCH * (initials.size() - 1) / 2.0;
        for (int i = 0; i < initials.size(); i++) {
            circle.position(new ParagraphBuilder()
                            .name("MonogramLine" + (i + 1))
                            .text(initials.get(i))
                            .textStyle(style(MONOGRAM_SIZE, WHITE, DocumentTextDecoration.DEFAULT))
                            .align(TextAlign.CENTER)
                            .build(),
                    0, top + i * MONOGRAM_PITCH, LayerAlign.CENTER);
        }
        column.addAligned(HorizontalAlign.CENTER, circle.build());
    }

    private static String initial(String name) {
        return name.isBlank() ? "" : name.strip().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /** The name in two weights, the tracked role under it, and their divider. */
    private static void renderNamePlate(SectionBuilder column, CvIdentity identity) {
        column.addParagraph(p -> p
                .name("Name")
                .inlineText(identity.name().first().toUpperCase(Locale.ROOT) + " ",
                        style(NAME_SIZE, WHITE, DocumentTextDecoration.DEFAULT))
                .inlineText(identity.name().last().toUpperCase(Locale.ROOT),
                        style(NAME_SIZE, WHITE, DocumentTextDecoration.BOLD))
                .align(TextAlign.CENTER)
                .margin(new DocumentInsets(px(20.4), 0, 0, 0)));

        column.addParagraph(p -> p
                .name("Role")
                .text(MidnightNavyWidgets.tracked(identity.jobTitle().toUpperCase(Locale.ROOT)))
                .textStyle(style(ROLE_SIZE, WHITE, DocumentTextDecoration.DEFAULT))
                .align(TextAlign.CENTER)
                .margin(new DocumentInsets(px(13.1), 0, 0, 0)));

        MidnightNavyWidgets.nameDivider(column, NAME_DIVIDER_WIDTH,
                (ASIDE_WIDTH - NAME_DIVIDER_WIDTH) / 2.0, px(21.7));
    }

    /**
     * The contact block — a mark, then the value or the link's own label.
     *
     * <p>The three channels are always drawn: a {@code Contact} rejects a blank
     * field, so the triple is non-blank by construction. A link is drawn as its
     * own label with the address behind it, so the column's width does not
     * depend on how long an address happens to be.</p>
     */
    private static void renderContact(SectionBuilder column, CvIdentity identity) {
        Contact contact = identity.contact();
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(MidnightNavyIcons.PHONE, contact.phone(),
                telUri(contact.phone())));
        channels.add(new Channel(MidnightNavyIcons.EMAIL, contact.email(),
                "mailto:" + contact.email()));
        channels.add(new Channel(MidnightNavyIcons.LOCATION, contact.address(), null));
        for (Link link : identity.links()) {
            channels.add(new Channel(markFor(link), link.label(), link.url()));
        }

        column.addSection("Contact", block -> {
            block.spacing(0).padding(ASIDE_CONTENT_PAD)
                    .margin(new DocumentInsets(px(37.2), 0, 0, 0));
            MidnightNavyWidgets.asideHeading(block, CONTACT_HEADING, CONTACT_RULE_WIDTH, px(23.6));
            block.addSection("ContactRows", rows -> {
                rows.spacing(CONTACT_ROW_GAP);
                for (int i = 0; i < channels.size(); i++) {
                    Channel channel = channels.get(i);
                    int index = i;
                    rows.addParagraph(p -> {
                        p.name("Contact" + index);
                        p.inlineSvgIcon(MidnightNavyIcons.icon(channel.token()),
                                CONTACT_ICON_SIZE, InlineImageAlignment.CENTER);
                        p.inlineText("     ", asideText());
                        if (channel.href() == null) {
                            p.inlineText(channel.label(), asideText());
                        } else {
                            p.inlineText(channel.label(), asideText(),
                                    new DocumentLinkOptions(channel.href()));
                        }
                    });
                }
            });
        });
    }

    /** The degrees — the qualification, its institution and its years. */
    private static void renderEducation(SectionBuilder column, EntriesSection education) {
        column.addSection("Education", block -> {
            block.spacing(0).padding(ASIDE_CONTENT_PAD)
                    .margin(new DocumentInsets(px(55.4), 0, 0, 0));
            MidnightNavyWidgets.asideHeading(block, education.title(), ASIDE_INNER_WIDTH, px(20.4));
            block.addSection("EducationEntries", rows -> {
                rows.spacing(px(29.8));
                for (CvEntry entry : education.entries()) {
                    rows.addSection("EducationEntry", item -> {
                        item.spacing(0).keepTogether();
                        item.addParagraph(p -> {
                            p.name("Degree");
                            p.lineSpacing(leading(20, EDUCATION_TITLE_SIZE));
                            p.textStyle(style(EDUCATION_TITLE_SIZE, WHITE,
                                    DocumentTextDecoration.BOLD));
                            if (entry.link().isBlank()) {
                                p.inlineText(entry.title(), style(EDUCATION_TITLE_SIZE, WHITE,
                                        DocumentTextDecoration.BOLD));
                            } else {
                                p.inlineText(entry.title(), style(EDUCATION_TITLE_SIZE, WHITE,
                                                DocumentTextDecoration.BOLD),
                                        new DocumentLinkOptions(entry.link()));
                            }
                        });
                        if (!entry.subtitle().isBlank()) {
                            item.addParagraph(p -> p
                                    .name("Institution")
                                    .text(entry.subtitle())
                                    .textStyle(style(ASIDE_SMALL_SIZE, ASIDE_DIM,
                                            DocumentTextDecoration.DEFAULT))
                                    .lineSpacing(leading(18, ASIDE_SMALL_SIZE))
                                    .margin(new DocumentInsets(px(6.4), 0, 0, 0)));
                        }
                        if (!entry.date().isBlank()) {
                            item.addParagraph(p -> p
                                    .name("Years")
                                    .text(entry.date())
                                    .textStyle(style(EDUCATION_YEARS_SIZE, ASIDE_DIM,
                                            DocumentTextDecoration.DEFAULT))
                                    .margin(new DocumentInsets(px(11.5), 0, 0, 0)));
                        }
                    });
                }
            });
        });
    }

    /** The skills — a label beside a meter, one row each. */
    private static void renderSkills(SectionBuilder column, SkillsSection skills) {
        List<CvSkill> entries = flatten(skills);
        column.addSection("Skills", block -> {
            block.spacing(0).padding(ASIDE_CONTENT_PAD)
                    .margin(new DocumentInsets(px(37.4), 0, 0, 0));
            MidnightNavyWidgets.asideHeading(block, skills.title(), ASIDE_INNER_WIDTH, px(21));
            block.addSection("SkillRows", rows -> {
                rows.spacing(SKILL_ROW_GAP);
                for (CvSkill skill : entries) {
                    MidnightNavyWidgets.layeredRow(rows, "SkillRow", row -> {
                        row.spacing(0)
                                .verticalAlign(RowVerticalAlign.CENTER)
                                .columns(DocumentRowColumn.weight(1),
                                        DocumentRowColumn.fixed(SKILL_TRACK_WIDTH));
                        row.addParagraph(p -> p
                                .name("SkillLabel")
                                .text(skill.name())
                                .textStyle(style(ASIDE_SMALL_SIZE, ASIDE_DIM,
                                        DocumentTextDecoration.DEFAULT)));
                        row.addSection("SkillMeterCell", cell ->
                                renderSkillMeter(cell, skill.level()));
                    });
                }
            });
        });
    }

    /** The three layers of one meter, all on the track's axis. */
    private static void renderSkillMeter(SectionBuilder cell, OptionalDouble level) {
        double clamped = Math.max(0.0, Math.min(1.0, level.orElse(0.0)));
        double filled = SKILL_TRACK_WIDTH * clamped;
        cell.addLayerStack(stack -> {
            stack.name("SkillMeter");
            stack.layer(new LineBuilder()
                    .name("SkillTrack")
                    .horizontal(SKILL_TRACK_WIDTH)
                    .thickness(SKILL_TRACK_THICKNESS)
                    .color(TRACK_DIM)
                    .build(), LayerAlign.CENTER_LEFT, 0);
            stack.layer(new LineBuilder()
                    .name("SkillTrackFilled")
                    .horizontal(filled)
                    .thickness(SKILL_TRACK_THICKNESS)
                    .color(WHITE)
                    .build(), LayerAlign.CENTER_LEFT, 1);
            stack.position(new ShapeBuilder()
                            .name("SkillKnob")
                            .size(SKILL_KNOB_WIDTH, SKILL_KNOB_HEIGHT)
                            .cornerRadius(SKILL_KNOB_HEIGHT / 2.0)
                            .fillColor(WHITE)
                            .build(),
                    filled - SKILL_KNOB_WIDTH / 2.0, 0, LayerAlign.CENTER_LEFT, 2);
        });
    }

    /**
     * The languages — a label beside five dots, filled to the rating.
     *
     * <p>The rating is a fraction like every other level in the model, and the
     * design shows it in fifths, so it is rounded to the nearest dot.</p>
     */
    private static void renderLanguages(SectionBuilder column, SkillsSection languages) {
        List<CvSkill> entries = flatten(languages);
        column.addSection("Languages", block -> {
            block.spacing(0).padding(ASIDE_CONTENT_PAD)
                    .margin(new DocumentInsets(px(52.7), 0, 0, 0));
            MidnightNavyWidgets.asideHeading(block, languages.title(), ASIDE_INNER_WIDTH, px(26.6));
            block.addSection("LanguageRows", rows -> {
                rows.spacing(LANGUAGE_ROW_GAP);
                for (CvSkill language : entries) {
                    int filled = (int) Math.round(
                            Math.max(0.0, Math.min(1.0, language.level().orElse(0.0)))
                                    * LANGUAGE_DOTS);
                    MidnightNavyWidgets.layeredRow(rows, "LanguageRow", row -> {
                        row.spacing(0)
                                .verticalAlign(RowVerticalAlign.CENTER)
                                .columns(DocumentRowColumn.weight(1),
                                        DocumentRowColumn.fixed(LANGUAGE_GROUP_WIDTH));
                        row.addParagraph(p -> p
                                .name("LanguageLabel")
                                .text(language.name())
                                .textStyle(style(ASIDE_TEXT_SIZE, WHITE,
                                        DocumentTextDecoration.DEFAULT)));
                        row.addParagraph(p -> {
                            p.name("LanguageDots");
                            for (int i = 0; i < LANGUAGE_DOTS; i++) {
                                if (i > 0) {
                                    p.inlineText(dotGap(), asideText());
                                }
                                if (i < filled) {
                                    p.dot(LANGUAGE_DOT_DIAMETER, WHITE);
                                } else {
                                    p.dot(LANGUAGE_DOT_DIAMETER, NAVY,
                                            DocumentStroke.of(WHITE, LANGUAGE_DOT_STROKE));
                                }
                            }
                        });
                    });
                }
            });
        });
    }

    /**
     * The gap between two dots, as spaces at the row's own type size. There is
     * no inline advance control, so the count comes from the measured pitch and
     * the face's space advance rather than from the eye.
     */
    private static String dotGap() {
        int spaces = (int) Math.max(1, Math.round(
                (LANGUAGE_DOT_PITCH - LANGUAGE_DOT_DIAMETER)
                        / (FACE_SPACE_ADVANCE * ASIDE_TEXT_SIZE)));
        return " ".repeat(spaces);
    }

    /** Every rated entry of every group, in the order they were given. */
    private static List<CvSkill> flatten(SkillsSection section) {
        List<CvSkill> entries = new ArrayList<>();
        for (SkillGroup group : section.groups()) {
            entries.addAll(group.entries());
        }
        return entries;
    }

    /** The mark for the network a link points at, or the globe. */
    private static String markFor(Link link) {
        return link.url().toLowerCase(Locale.ROOT).contains("linkedin.")
                ? MidnightNavyIcons.LINKEDIN
                : MidnightNavyIcons.WEBSITE;
    }

    /** A printed number as something a reader can dial, or {@code null}. */
    private static String telUri(String phone) {
        String dialled = TRUNK_PREFIX.matcher(phone).replaceAll("");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    /** One contact row: its mark, what it reads, and where it points. */
    private record Channel(String token, String label, String href) {
    }
}
