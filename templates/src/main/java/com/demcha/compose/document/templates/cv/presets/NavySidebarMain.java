package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
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
     * <p>The rail is the section's left accent, which runs the full height
     * of what the section holds — so the last role's body is composed in a
     * second section outside the rail, and the line stops at the last
     * marker instead of running past it to the foot of the block.</p>
     */
    private static void renderExperience(SectionBuilder section, EntriesSection experience) {
        sectionHeader(section, NavySidebarIcons.BRIEFCASE, experience.title());
        List<CvEntry> entries = experience.entries();
        int last = entries.size() - 1;
        section.addSection("ExperienceRail", rail -> {
            rail.spacing(0);
            // The rail starts where the first marker's band does, which is
            // half a title's overhang below the heading.
            rail.margin(new DocumentInsets(
                    HEADER_TO_BODY + TITLE_OVERFLOW, 0, 0, RAIL_MARGIN_LEFT));
            rail.accentLeft(RAIL, RAIL_WIDTH);
            for (int i = 0; i < entries.size(); i++) {
                renderEntryHead(rail, entries.get(i));
                if (i < last) {
                    renderEntryBody(rail, entries.get(i), ENTRY_GAP + TITLE_OVERFLOW);
                }
            }
        });
        section.addSection("ExperienceTail", tail -> {
            tail.spacing(0);
            tail.margin(new DocumentInsets(0, 0, 0, RAIL_MARGIN_LEFT));
            renderEntryBody(tail, entries.get(last), 0);
        });
    }

    /**
     * The marker, the position in capitals and the dates, all on the
     * marker's axis: the band is as tall as the marker, and the two pieces
     * of type are pulled up by half their own overhang to centre on it.
     */
    private static void renderEntryHead(SectionBuilder rail, CvEntry entry) {
        DocumentNode marker = new EllipseBuilder()
                .name("Marker_" + compact(entry.title()))
                .circle(MARKER_DIAMETER)
                .fillColor(NAVY)
                .build();
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
        rail.addContainer(head -> head
                .name("EntryHead_" + compact(entry.title()))
                .rectangle(ENTRY_WIDTH, ENTRY_HEAD_BAND_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(marker, -MARKER_DIAMETER / 2.0, 0, LayerAlign.CENTER_LEFT)
                .position(title, ENTRY_TEXT_INSET, -TITLE_OVERFLOW, LayerAlign.CENTER_LEFT)
                .position(dates, 0, -DATE_OVERFLOW, LayerAlign.CENTER_RIGHT));
    }

    /** The employer in accent, then one bullet per line of the entry body. */
    private static void renderEntryBody(SectionBuilder rail, CvEntry entry, double gapBelow) {
        rail.addParagraph(p -> p
                .name("Employer_" + compact(entry.title()))
                .text(entry.subtitle())
                .textStyle(style(BODY_SIZE, ACCENT, DocumentTextDecoration.DEFAULT))
                .margin(new DocumentInsets(
                        ENTRY_TITLE_TO_EMPLOYER + TITLE_OVERFLOW, 0,
                        ENTRY_EMPLOYER_TO_BULLETS, ENTRY_TEXT_INSET)));
        List<String> highlights = lines(entry.body());
        if (!highlights.isEmpty()) {
            rail.addList(list -> list
                    .name("Highlights_" + compact(entry.title()))
                    .bullet()
                    .items(highlights)
                    .textStyle(body())
                    .lineSpacing(BULLET_LEADING)
                    .itemSpacing(BULLET_ITEM_GAP)
                    .margin(new DocumentInsets(0, 0, gapBelow, ENTRY_TEXT_INSET)));
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
