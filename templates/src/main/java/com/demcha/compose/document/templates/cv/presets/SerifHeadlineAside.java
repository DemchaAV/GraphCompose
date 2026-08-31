package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ASIDE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CAPTION_TO_ROWS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CAPTION_TO_SOFT_SKILLS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DEGREE_TO_INSTITUTION;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.EDUCATION_TO_SKILLS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GROUP_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HAIRLINE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HALF_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_CAPTION;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_EDUCATION;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INSTITUTION_TO_YEARS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SKILL_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SKILL_ROW_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SOFT_SKILL_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.STUB_RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.STUB_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.STUB_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.STUB_TO_DEGREE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TRACK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TRACK_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TRACK_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.YEARS_TO_STUB;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.body;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.small;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.tracked;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.sectionHeading;

/**
 * The narrow right column, hung off the hairline that divides the grid: the
 * degrees, then the skills as captioned groups of meters, closing with the
 * soft skills as plain lines.
 */
final class SerifHeadlineAside {

    private SerifHeadlineAside() {
    }

    static void compose(SectionBuilder section,
                        EntriesSection education,
                        SkillsSection skills,
                        ParagraphSection softSkills) {
        // The divider is this column's left accent, so it runs exactly as
        // deep as the column's content and no further.
        section.spacing(0)
                .padding(new DocumentInsets(0, 0, 0, HALF_GUTTER))
                .accentLeft(DIVIDER, HAIRLINE_THICKNESS);
        boolean hasEducation = SectionLookup.hasContent(education);
        if (hasEducation) {
            sectionHeading(section, education.title(), ASIDE_WIDTH, false, 0.0, HALF_GUTTER);
            renderEducation(section, education);
        }
        if (SectionLookup.hasContent(skills) || SectionLookup.hasContent(softSkills)) {
            sectionHeading(section, skillsHeading(skills), ASIDE_WIDTH, false,
                    hasEducation ? EDUCATION_TO_SKILLS : 0.0, HALF_GUTTER);
            renderSkills(section, skills, softSkills);
        }
    }

    /** The heading over both blocks, taken from the skills section when there is one. */
    private static String skillsHeading(SkillsSection skills) {
        return SectionLookup.hasContent(skills) ? skills.title() : "Skills";
    }

    // -- education -------------------------------------------------------

    /**
     * Each degree as three lines — the qualification, the institution in
     * accent, and the years beside the place — with a short stub rule
     * between neighbours.
     */
    private static void renderEducation(SectionBuilder section, EntriesSection education) {
        List<CvEntry> entries = education.entries();
        for (int i = 0; i < entries.size(); i++) {
            CvEntry entry = entries.get(i);
            boolean first = i == 0;
            section.addParagraph(p -> p
                    .name("Degree_" + compact(entry.title()))
                    .text(entry.title())
                    .textStyle(style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                    .lineSpacing(TIGHT_LEADING)
                    .margin(new DocumentInsets(
                            first ? HEADING_TO_EDUCATION : 0, 0, DEGREE_TO_INSTITUTION, 0)));
            section.addParagraph(p -> p
                    .name("Institution_" + compact(entry.title()))
                    .text(entry.subtitle())
                    .textStyle(style(SMALL_SIZE, ACCENT, DocumentTextDecoration.DEFAULT))
                    .lineSpacing(TIGHT_LEADING)
                    .margin(new DocumentInsets(0, 0, INSTITUTION_TO_YEARS, 0)));
            section.addParagraph(p -> {
                p.name("Years_" + compact(entry.title()))
                        .textStyle(small())
                        .inlineText(entry.date(), small());
                if (!entry.place().isBlank()) {
                    p.inlineText("   |   ",
                                    style(SMALL_SIZE, RULE, DocumentTextDecoration.DEFAULT))
                            .inlineText(entry.place(), small());
                }
                p.lineSpacing(TIGHT_LEADING).margin(DocumentInsets.zero());
            });
            if (i < entries.size() - 1) {
                section.addLine(line -> line
                        .name("EducationStub_" + compact(entry.title()))
                        .horizontal(STUB_RULE_WIDTH)
                        .thickness(STUB_RULE_THICKNESS)
                        .color(STUB_RULE)
                        .margin(new DocumentInsets(YEARS_TO_STUB, 0, STUB_TO_DEGREE, 0)));
            }
        }
    }

    // -- skills ----------------------------------------------------------

    /**
     * Each skill group under its own tracked caption, then the soft skills
     * under theirs — the one block on this sheet that is prose rather than
     * meters, so it takes the section's title as its caption.
     */
    private static void renderSkills(SectionBuilder section, SkillsSection skills,
                                     ParagraphSection softSkills) {
        boolean firstCaption = true;
        if (SectionLookup.hasContent(skills)) {
            List<SkillGroup> groups = skills.groups();
            for (int i = 0; i < groups.size(); i++) {
                SkillGroup group = groups.get(i);
                caption(section, group.category(),
                        i == 0 ? HEADING_TO_CAPTION : GROUP_GAP);
                section.addSection("SkillGroup_" + compact(group.category()), rows -> {
                    rows.spacing(SKILL_ROW_GAP).padding(DocumentInsets.zero());
                    rows.margin(new DocumentInsets(CAPTION_TO_ROWS, 0, 0, 0));
                    for (CvSkill skill : group.entries()) {
                        renderSkillRow(rows, skill);
                    }
                });
            }
            firstCaption = groups.isEmpty();
        }
        if (SectionLookup.hasContent(softSkills)) {
            caption(section, softSkills.title(), firstCaption ? HEADING_TO_CAPTION : GROUP_GAP);
            renderSoftSkills(section, softSkills);
        }
    }

    private static void caption(SectionBuilder section, String text, double gapAbove) {
        DocumentTextStyle captionStyle = style(SMALL_SIZE, INK, DocumentTextDecoration.BOLD);
        section.addParagraph(p -> {
            p.name("SkillCaption_" + compact(text));
            p.textStyle(captionStyle);
            tracked(p, text.toUpperCase(Locale.ROOT), captionStyle);
            p.lineSpacing(TIGHT_LEADING);
            p.margin(new DocumentInsets(gapAbove, 0, 0, 0));
        });
    }

    /**
     * A skill row: the name at the left of a band, the meter flush right. A
     * skill the document leaves unlevelled draws its name alone rather than
     * an empty track.
     */
    private static void renderSkillRow(SectionBuilder rows, CvSkill skill) {
        DocumentNode label = new ParagraphBuilder()
                .name("SkillLabel_" + compact(skill.name()))
                .text(skill.name())
                .textStyle(small())
                .lineSpacing(TIGHT_LEADING)
                .margin(DocumentInsets.zero())
                .build();
        rows.addContainer(band -> {
            band.name("SkillRow_" + compact(skill.name()))
                    .rectangle(ASIDE_WIDTH, SKILL_ROW_HEIGHT)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .padding(DocumentInsets.zero())
                    .position(label, 0, 0, LayerAlign.CENTER_LEFT);
            if (skill.level().isPresent()) {
                band.position(meter(skill), 0, 0, LayerAlign.CENTER_RIGHT);
            }
        });
    }

    private static DocumentNode meter(CvSkill skill) {
        double level = Math.max(0.0, Math.min(1.0, skill.level().getAsDouble()));
        // A shape, not a shape container: a container must own at least one
        // layer, and the fill is a bare rounded rectangle with nothing in it.
        DocumentNode fill = new ShapeBuilder()
                .name("SkillFill_" + compact(skill.name()))
                .size(TRACK_WIDTH * level, TRACK_THICKNESS)
                .cornerRadius(TRACK_THICKNESS / 2.0)
                .fillColor(INK)
                .margin(DocumentInsets.zero())
                .build();
        return new ShapeContainerBuilder()
                .name("SkillTrack_" + compact(skill.name()))
                .roundedRect(TRACK_WIDTH, TRACK_THICKNESS, TRACK_THICKNESS / 2.0)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .fillColor(TRACK)
                .padding(DocumentInsets.zero())
                .position(fill, 0, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /** One line per line of the block's body, set as plain prose. */
    private static void renderSoftSkills(SectionBuilder section, ParagraphSection softSkills) {
        List<String> lines = SerifHeadlineText.lines(softSkills.body());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean first = i == 0;
            section.addParagraph(p -> p
                    .name("SoftSkill_" + compact(line))
                    .text(line)
                    .textStyle(body())
                    .lineSpacing(SOFT_SKILL_LEADING)
                    .margin(new DocumentInsets(first ? CAPTION_TO_SOFT_SKILLS : 0, 0, 0, 0)));
        }
    }
}
