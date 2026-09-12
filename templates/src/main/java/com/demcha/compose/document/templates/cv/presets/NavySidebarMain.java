package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BULLET_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BULLET_LEADING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.DATE_OVERFLOW;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_EMPLOYER_TO_BULLETS;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_HEAD_BAND_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_TITLE_TO_EMPLOYER;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.HEADER_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.JOB_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_RULE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAME_TO_ROLE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RAIL;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RAIL_MARGIN_LEFT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RAIL_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.ROLE_TO_SUMMARY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RULE_GAP_ABOVE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RULE_GAP_BELOW;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SUMMARY_RULE_GAP_BELOW;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SUMMARY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.TITLE_OVERFLOW;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.TROUGH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.body;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.tracked;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarWidgets.indentedList;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarWidgets.sectionHeader;

/**
 * The white right column: the name and role, the summary, then the roles
 * held on a timeline rail, the achievements and the certifications, each
 * opened by a hairline and a badged heading.
 */
final class NavySidebarMain {

    private NavySidebarMain() {
    }

    static void compose(SectionBuilder section,
                        CvIdentity identity,
                        ParagraphSection summary,
                        EntriesSection experience,
                        ParagraphSection achievements,
                        ParagraphSection certifications) {
        section.spacing(0)
                .padding(new DocumentInsets(MAIN_PAD_TOP, PAGE_MARGIN, PAGE_MARGIN, TROUGH));
        section.addSection("Identity", inner -> renderIdentity(inner, identity));
        if (SectionLookup.hasContent(summary)) {
            section.addSection("Summary", inner -> renderSummary(inner, summary));
        }
        if (SectionLookup.hasContent(experience)) {
            rule(section, SUMMARY_RULE_GAP_BELOW);
            section.addSection("Experience", inner -> renderExperience(inner, experience));
        }
        if (SectionLookup.hasContent(achievements)) {
            rule(section, RULE_GAP_BELOW);
            section.addSection("Achievements", inner ->
                    renderBulletBlock(inner, NavySidebarIcons.TROPHY, achievements,
                            "AchievementsList"));
        }
        if (SectionLookup.hasContent(certifications)) {
            rule(section, RULE_GAP_BELOW);
            section.addSection("Certifications", inner ->
                    renderBulletBlock(inner, NavySidebarIcons.CERTIFICATE, certifications,
                            "CertificationsList"));
        }
    }

    /**
     * The hairline that opens a block. It belongs to the block below it, so
     * a block the document does not fill takes its rule with it instead of
     * leaving one hanging. The gap below is authored per rule: the one that
     * opens the experience block is set four points wider than the two that
     * follow it.
     *
     * @param section  the main column
     * @param gapBelow the gap between the rule and the block it opens
     */
    private static void rule(SectionBuilder section, double gapBelow) {
        section.addLine(line -> line
                .name("SectionRule")
                .horizontal(MAIN_CONTENT_WIDTH)
                .thickness(MAIN_RULE_THICKNESS)
                .color(MAIN_RULE)
                .margin(new DocumentInsets(RULE_GAP_ABOVE, 0, gapBelow, 0)));
    }

    // -- identity --------------------------------------------------------

    private static void renderIdentity(SectionBuilder section, CvIdentity identity) {
        DocumentTextStyle nameStyle = style(NAME_SIZE, INK, DocumentTextDecoration.DEFAULT);
        section.addParagraph(p -> p
                .name("Name")
                .text(identity.name().full().toUpperCase(Locale.ROOT))
                .textStyle(nameStyle)
                .align(TextAlign.LEFT)
                .margin(new DocumentInsets(0, 0, NAME_TO_ROLE, 0)));

        DocumentTextStyle roleStyle = style(ROLE_SIZE, ACCENT, DocumentTextDecoration.BOLD);
        section.addParagraph(p -> {
            p.name("Role");
            p.textStyle(roleStyle);
            tracked(p, identity.jobTitle().toUpperCase(Locale.ROOT), roleStyle);
            p.align(TextAlign.LEFT);
            p.margin(new DocumentInsets(0, 0, ROLE_TO_SUMMARY, 0));
        });
    }

    // -- summary ---------------------------------------------------------

    /**
     * The opening paragraph. It is the one block this design gives no
     * heading, so the section's title is not drawn.
     */
    private static void renderSummary(SectionBuilder section, ParagraphSection summary) {
        section.addParagraph(p -> p
                .name("Summary")
                .text(summary.body())
                .textStyle(style(SUMMARY_SIZE, BODY, DocumentTextDecoration.DEFAULT))
                .lineSpacing(BODY_LEADING)
                .margin(DocumentInsets.zero()));
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
        sectionHeader(section, NavySidebarIcons.BRIEFCASE, experience.title());
        SectionBuilder holder = new SectionBuilder();
        holder.name("ExperienceRailHolder");
        holder.spacing(0);
        // The rail starts where the first marker's band does, which is half a title's
        // overhang below the heading; the axis column centres the rail on itself, so the
        // timeline starts half a marker further left than the accent did.
        holder.margin(new DocumentInsets(HEADER_TO_BODY + TITLE_OVERFLOW, 0, 0,
                RAIL_MARGIN_LEFT - MARKER_DIAMETER / 2.0));
        holder.addTimeline(timeline -> {
            timeline.markerOnRail()
                    .rail(rail -> rail
                            .stroke(DocumentStroke.of(RAIL, RAIL_WIDTH))
                            .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                    .axisWidth(MARKER_DIAMETER)
                    .markerGap(ENTRY_TEXT_INSET - MARKER_DIAMETER / 2.0)
                    .gutter(0)
                    .spacing(ENTRY_GAP + TITLE_OVERFLOW);
            for (CvEntry entry : experience.entries()) {
                timeline.entry(entryMarker(entry), e -> e.content(body -> renderEntry(body, entry)));
            }
        });
        DocumentNode node = holder.build();
        // The timeline goes in through a layer stack rather than straight into the
        // section. A timeline publishes a row, a row cannot nest inside a row cell,
        // and this section can sit in one — the stack is what insulates it.
        // Unwinding the stacks that turn out not to need it buys no capability, so
        // they stay until that is done as its own deliberate cleanup.
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
                        .fillColor(NAVY)));
    }

    /** One role: the head band carrying its title and dates, then the body. */
    private static void renderEntry(SectionBuilder body, CvEntry entry) {
        body.spacing(0);
        renderEntryHead(body, entry);
        renderEntryBody(body, entry);
    }

    /**
     * The marker, the position in capitals and the dates, all on the
     * marker's axis: the band is as tall as the marker, and the two pieces
     * of type are pulled up by half their own overhang to centre on it.
     */
    private static void renderEntryHead(SectionBuilder rail, CvEntry entry) {
        DocumentNode title = new ParagraphBuilder()
                .name("JobTitle_" + compact(entry.title()))
                .text(entry.title().toUpperCase(Locale.ROOT))
                .textStyle(style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                .margin(DocumentInsets.zero())
                .build();
        DocumentNode dates = new ParagraphBuilder()
                .name("JobDates_" + compact(entry.title()))
                .text(entry.date())
                .textStyle(body())
                .align(TextAlign.RIGHT)
                .margin(DocumentInsets.zero())
                .build();
        // The marker has left the band for the timeline's axis column, so the band starts at
        // the content column and the title no longer walks in past the rail.
        rail.addContainer(head -> head
                .name("EntryHead_" + compact(entry.title()))
                .rectangle(ENTRY_WIDTH - ENTRY_TEXT_INSET, ENTRY_HEAD_BAND_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(title, 0, -TITLE_OVERFLOW, LayerAlign.CENTER_LEFT)
                .position(dates, 0, -DATE_OVERFLOW, LayerAlign.CENTER_RIGHT));
    }

    /** The employer in accent, then one bullet per line of the entry body. */
    private static void renderEntryBody(SectionBuilder rail, CvEntry entry) {
        // No left inset: the content column already starts where the text did, and the gap
        // below an entry is the timeline's spacing rather than the last list's bottom margin.
        rail.addParagraph(p -> p
                .name("Employer_" + compact(entry.title()))
                .text(entry.subtitle())
                .textStyle(style(BODY_SIZE, ACCENT, DocumentTextDecoration.DEFAULT))
                .margin(new DocumentInsets(
                        ENTRY_TITLE_TO_EMPLOYER + TITLE_OVERFLOW, 0,
                        ENTRY_EMPLOYER_TO_BULLETS, 0)));
        List<String> highlights = lines(entry.body());
        if (!highlights.isEmpty()) {
            rail.addList(list -> list
                    .name("Highlights_" + compact(entry.title()))
                    .bullet()
                    .items(highlights)
                    .textStyle(body())
                    .lineSpacing(BULLET_LEADING)
                    .itemSpacing(BULLET_ITEM_GAP)
                    .margin(DocumentInsets.zero()));
        }
    }

    // -- closing blocks --------------------------------------------------

    /** A badged heading over an indented bullet list, one bullet per line. */
    private static void renderBulletBlock(SectionBuilder section, String iconToken,
                                          ParagraphSection block, String listName) {
        sectionHeader(section, iconToken, block.title());
        indentedList(section, listName, lines(block.body()));
    }

    /**
     * One bullet per non-blank line — the family's way of carrying a list in
     * a field the model types as a single string.
     */
    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split("\\R")) {
            String clean = MarkdownInline.plainText(line).trim();
            if (!clean.isBlank()) {
                out.add(clean);
            }
        }
        return out;
    }
}
