package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailExtent;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BULLET_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BULLET_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BULLETS_TO_SEPARATOR;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BULLET_MARKER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BULLET_MARKER_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CARD_BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CARD_TECH_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CARD_TITLE_TO_TECH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DATE_OVERFLOW;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.EMPLOYER_TO_BULLETS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ENTRY_HEAD_BAND_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ENTRY_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.EXPERIENCE_TO_PROJECTS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GLYPH_CLEARANCE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HAIRLINE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HALF_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_CARDS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_ENTRY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.JOB_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MAIN_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PLATE_HANG;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PROJECT_GLYPH_TO_TEXT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PROJECT_PLATE_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PROJECT_SEPARATOR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RAIL_MARGIN_LEFT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RAIL_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SEPARATOR_TO_ENTRY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TITLE_OVERFLOW;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TITLE_TO_EMPLOYER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.body;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.small;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.BandColumn;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.columnBand;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.plate;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.sectionHeading;

/**
 * The wide left column: the roles held on a timeline rail, and the projects
 * as a band of marked cards beneath them.
 */
final class SerifHeadlineMain {

    private SerifHeadlineMain() {
    }

    static void compose(SectionBuilder section,
                        EntriesSection experience,
                        EntriesSection projects) {
        section.spacing(0).padding(new DocumentInsets(0, HALF_GUTTER, 0, 0));
        if (SectionLookup.hasContent(experience)) {
            sectionHeading(section, experience.title(), MAIN_WIDTH, false, 0.0, MARGIN);
            renderExperience(section, experience);
        }
        if (SectionLookup.hasContent(projects)) {
            sectionHeading(section, projects.title(), MAIN_WIDTH, true,
                    EXPERIENCE_TO_PROJECTS, MARGIN);
            renderProjects(section, projects);
        }
    }

    // -- experience ------------------------------------------------------

    /**
     * The roles held, strung on a vertical rail with a filled marker at each
     * one.
     *
     * <p>The rail stops at the last marker, which is what {@code MARKER_TO_MARKER} means. It
     * used to be the section's left accent, and an accent runs the full height of what its
     * section holds — so the last role's body was composed in a second section outside the
     * rail to keep the line from running on to the foot of the block. The extent says it
     * directly now, and that sibling section is gone.</p>
     *
     * <p>Wrapped in a layer because a row nested in a row cell is refused and this sheet's
     * main column is one.</p>
     */
    private static void renderExperience(SectionBuilder section, EntriesSection experience) {
        List<CvEntry> entries = experience.entries();
        int last = entries.size() - 1;
        SectionBuilder holder = new SectionBuilder();
        holder.name("ExperienceRailHolder");
        holder.spacing(0).padding(DocumentInsets.zero());
        // The axis column centres the rail on itself, so the timeline starts half a marker
        // further left than the accent did and the line lands back where it was.
        holder.margin(new DocumentInsets(HEADING_TO_ENTRY + TITLE_OVERFLOW, 0, 0,
                RAIL_MARGIN_LEFT - MARKER_DIAMETER / 2.0));
        holder.addTimeline(timeline -> {
            timeline.markerOnRail()
                    .rail(rail -> rail
                            .stroke(DocumentStroke.of(DIVIDER, RAIL_THICKNESS))
                            .extent(TimelineRailExtent.MARKER_TO_MARKER))
                    .axisWidth(MARKER_DIAMETER)
                    .markerGap(ENTRY_TEXT_INSET - MARKER_DIAMETER / 2.0)
                    .gutter(0)
                    // The rhythm between roles is the separator's own margins, as before.
                    .spacing(0);
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                boolean separated = i < last;
                timeline.entry(entryMarker(entry), e -> e.content(body -> {
                    body.spacing(0);
                    renderEntryHead(body, entry);
                    renderEntryBody(body, entry);
                    if (separated) {
                        renderEntrySeparator(body, entry);
                    }
                }));
            }
        });
        DocumentNode node = holder.build();
        section.addLayerStack(stack -> stack
                .name("ExperienceRail")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }

    /** The filled disc that marks a role, in a box as tall as the head band it rides. */
    private static TimelineMarker entryMarker(CvEntry entry) {
        return TimelineMarker.custom(MARKER_DIAMETER, ENTRY_HEAD_BAND_HEIGHT,
                column -> column.addEllipse(ellipse -> ellipse
                        .name("Marker_" + compact(entry.title()))
                        .circle(MARKER_DIAMETER)
                        .fillColor(INK)
                        .margin(DocumentInsets.zero())));
    }

    /**
     * The marker, the position and the dates, all on the marker's axis: the
     * band is as tall as the marker, and the two pieces of type are pulled up
     * by half their own overhang to centre on it.
     */
    private static void renderEntryHead(SectionBuilder rail, CvEntry entry) {
        ParagraphBuilder titleText = new ParagraphBuilder()
                .name("JobTitle_" + compact(entry.title()))
                .textStyle(style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                .lineSpacing(TIGHT_LEADING);
        SerifHeadlineText.title(titleText, entry,
                style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD));
        DocumentNode title = titleText.margin(DocumentInsets.zero()).build();
        DocumentNode dates = new ParagraphBuilder()
                .name("JobDates_" + compact(entry.title()))
                .text(entry.date())
                .textStyle(body())
                .align(TextAlign.RIGHT)
                .lineSpacing(TIGHT_LEADING)
                .margin(DocumentInsets.zero())
                .build();
        // The marker has left the band for the timeline's axis column, so the band starts at
        // the content column and the title no longer walks in past the rail.
        rail.addContainer(head -> head
                .name("EntryHead_" + compact(entry.title()))
                .rectangle(ENTRY_WIDTH - ENTRY_TEXT_INSET, ENTRY_HEAD_BAND_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .padding(DocumentInsets.zero())
                .position(title, 0, -TITLE_OVERFLOW, LayerAlign.CENTER_LEFT)
                .position(dates, 0, -DATE_OVERFLOW, LayerAlign.CENTER_RIGHT));
    }

    /**
     * The employer in accent and the location in body, held apart by a pale
     * pipe — three runs in one line, which is why the location is its own
     * field rather than part of the subtitle.
     */
    private static void renderEntryBody(SectionBuilder rail, CvEntry entry) {
        rail.addParagraph(p -> {
            p.name("Employer_" + compact(entry.title()))
                    .textStyle(small())
                    .inlineText(entry.subtitle(),
                            style(SMALL_SIZE, ACCENT, DocumentTextDecoration.DEFAULT));
            if (!entry.place().isBlank()) {
                p.inlineText("   |   ", style(SMALL_SIZE, RULE, DocumentTextDecoration.DEFAULT))
                        .inlineText(entry.place(), small());
            }
            p.lineSpacing(TIGHT_LEADING)
                    .margin(new DocumentInsets(
                            TITLE_TO_EMPLOYER + TITLE_OVERFLOW, 0,
                            EMPLOYER_TO_BULLETS, 0));
        });
        List<String> highlights = SerifHeadlineText.lines(entry.body());
        if (!highlights.isEmpty()) {
            // The gap after the dot is a measurement, so a highlight that wraps
            // resumes on its own text instead of the marker column.
            rail.addList(list -> list
                    .name("Highlights_" + compact(entry.title()))
                    .marker(BULLET_MARKER)
                    .markerGap(BULLET_MARKER_GAP)
                    .hangingIndent(true)
                    .items(highlights)
                    .textStyle(body())
                    .lineSpacing(BULLET_LEADING)
                    .itemSpacing(BULLET_ITEM_GAP)
                    .margin(DocumentInsets.zero()));
        }
    }

    private static void renderEntrySeparator(SectionBuilder rail, CvEntry entry) {
        rail.addLine(line -> line
                .name("EntrySeparator_" + compact(entry.title()))
                .horizontal(ENTRY_WIDTH - ENTRY_TEXT_INSET)
                .thickness(HAIRLINE_THICKNESS)
                .color(RULE)
                .margin(new DocumentInsets(
                        BULLETS_TO_SEPARATOR, 0,
                        SEPARATOR_TO_ENTRY + TITLE_OVERFLOW, 0)));
    }

    // -- projects --------------------------------------------------------

    private static void renderProjects(SectionBuilder section, EntriesSection projects) {
        List<CvEntry> entries = projects.entries();
        columnBand(section, "Projects", MAIN_WIDTH, entries.size(),
                HEADING_TO_CARDS, PROJECT_SEPARATOR_HEIGHT,
                index -> projectColumn(entries.get(index), index));
    }

    /**
     * One project card: the plate with the entry's mark, then the title, the
     * stack it was built on, and what it does.
     *
     * <p>The first plate hangs a little left of the band so it lines up with
     * the marks above it; the rest clear their column edge instead.</p>
     */
    private static BandColumn projectColumn(CvEntry project, int index) {
        double lead = index == 0 ? PLATE_HANG : GLYPH_CLEARANCE;
        SectionBuilder column = new SectionBuilder();
        column.name("ProjectCard_" + compact(project.title()))
                .spacing(0)
                .padding(new DocumentInsets(0, 0, 0, lead + PROJECT_GLYPH_TO_TEXT));
        column.addParagraph(p -> {
            p.name("ProjectTitle_" + compact(project.title()))
                    .textStyle(style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                    .lineSpacing(TIGHT_LEADING);
            SerifHeadlineText.title(p, project,
                    style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD));
            p.margin(new DocumentInsets(0, 0, CARD_TITLE_TO_TECH, 0));
        });
        column.addParagraph(p -> p
                .name("ProjectStack_" + compact(project.title()))
                .text(project.subtitle())
                .textStyle(style(SMALL_SIZE, ACCENT, DocumentTextDecoration.ITALIC))
                .lineSpacing(TIGHT_LEADING)
                .margin(new DocumentInsets(0, 0, CARD_TECH_TO_BODY, 0)));
        column.addParagraph(p -> p
                .name("ProjectBody_" + compact(project.title()))
                .text(project.body())
                .textStyle(small())
                .lineSpacing(CARD_BODY_LEADING)
                .margin(DocumentInsets.zero()));
        DocumentNode mark = project.icon().isBlank()
                ? null
                : plate("ProjectPlate_" + compact(project.title()),
                        project.icon(), PROJECT_PLATE_DIAMETER);
        return new BandColumn(column, mark, lead);
    }
}
