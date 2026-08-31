package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.COLUMN_PAD_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.EDUCATION_BAND_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.EDUCATION_BAND_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.EDUCATION_ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.EDUCATION_PERIOD_SHARE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.EMPLOYER_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ENTRY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.HIGHLIGHT_ITEM_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.HIGHLIGHT_LINE_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DIVIDER_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DIVIDER_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_HEADING_SPACER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MASTHEAD_DIVIDER_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MASTHEAD_DIVIDER_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.NAME_TO_SUBTITLE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_DESCRIPTION_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_DIVIDER_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_LINE_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_META_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_META_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_WEIGHTS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ROLE_PERIOD_SHARE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUBTITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUBTITLE_TO_RULE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUMMARY_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUMMARY_LINE_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.italic;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.text;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.divider;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.heading;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.headingWithDash;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.inlineIcon;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.layeredRow;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.railedLine;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.titleAndDate;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.trackedWide;

/**
 * The wide column: the masthead, the summary, the roles held on a rail, the
 * projects grid and the degrees.
 */
final class TerracottaRailMain {

    private TerracottaRailMain() {
    }

    static void compose(SectionBuilder main, CvIdentity identity, ParagraphSection summary,
                        EntriesSection experience, EntriesSection projects,
                        EntriesSection education) {
        main.name("MainColumn");
        main.spacing(0);
        main.padding((float) MAIN_PAD_TOP, (float) MAIN_PAD_RIGHT,
                (float) COLUMN_PAD_BOTTOM, (float) MAIN_PAD_LEFT);

        renderMasthead(main, identity);

        if (hasBody(summary)) {
            divider(main, "AfterMasthead", MASTHEAD_DIVIDER_TOP,
                    MASTHEAD_DIVIDER_BOTTOM);
            renderSummary(main, summary);
        }
        if (hasEntries(experience)) {
            mainDivider(main, "AfterSummary");
            renderExperience(main, experience);
        }
        if (hasEntries(projects)) {
            mainDivider(main, "AfterExperience");
            renderProjects(main, projects);
        }
        if (hasEntries(education)) {
            mainDivider(main, "AfterProjects");
            renderEducation(main, education);
        }
    }

    private static void mainDivider(SectionBuilder main, String name) {
        divider(main, name, MAIN_DIVIDER_TOP,
                MAIN_DIVIDER_BOTTOM);
    }

    // -- masthead ----------------------------------------------------------

    /** The name over its role, both letter-spaced, the role in terracotta. */
    private static void renderMasthead(SectionBuilder main, CvIdentity identity) {
        main.addSection("Masthead", block -> {
            block.spacing(0);
            block.addParagraph(p -> p
                    .name("FullName")
                    .text(trackedWide(identity.name().full().toUpperCase(Locale.ROOT)))
                    .textStyle(text(NAME_SIZE, INK, true))
                    .margin(0f, 0f, (float) NAME_TO_SUBTITLE_GAP, 0f));
            block.addParagraph(p -> p
                    .name("JobTitle")
                    .text(trackedWide(identity.jobTitle().toUpperCase(Locale.ROOT)))
                    .textStyle(text(SUBTITLE_SIZE, ACCENT, true))
                    .margin(0f, 0f, (float) SUBTITLE_TO_RULE_GAP, 0f));
        });
    }

    // -- summary -----------------------------------------------------------

    /** The summary, one paragraph per line the document wrote. */
    private static void renderSummary(SectionBuilder main, ParagraphSection summary) {
        main.addSection("Summary", block -> {
            block.spacing(0);
            headingWithDash(block, summary.title(), MAIN_HEADING_SPACER, MAIN_DASH_WIDTH);
            List<String> paragraphs = lines(summary.body());
            for (int i = 0; i < paragraphs.size(); i++) {
                int index = i;
                block.addParagraph(p -> p
                        .name("SummaryPara_" + index)
                        .text(paragraphs.get(index))
                        .textStyle(text(BODY_SIZE, INK, false))
                        .lineSpacing(SUMMARY_LINE_SPACING)
                        .margin(0f, 0f, (float) SUMMARY_LINE_GAP, 0f));
            }
        });
    }

    // -- experience --------------------------------------------------------

    /**
     * The roles held, each on a rail: a ringed marker on a grey hairline that
     * runs from one entry to the next. The last entry carries no rail, so the
     * line stops at the last marker instead of running past it.
     */
    private static void renderExperience(SectionBuilder main, EntriesSection experience) {
        main.addSection("Experience", block -> {
            block.spacing(0);
            heading(block, experience.title(), MAIN_HEADING_SPACER);
            List<CvEntry> entries = experience.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                boolean last = i == entries.size() - 1;
                int index = i;
                block.addSection("ExperienceEntry_" + index, body -> {
                    body.spacing(0);
                    if (!last) {
                        body.accentLeft(RULE, RULE_THICKNESS);
                    }
                    body.padding(0f, 0f, last ? 0f : (float) ENTRY_GAP, (float) ENTRY_INDENT);
                    railedLine(body, "Role", index,
                            titleAndDate("RoleTable_" + index, entry.title(), entry.date(),
                                    ENTRY_WIDTH, ROLE_PERIOD_SHARE));
                    body.addParagraph(p -> p
                            .name("Employer_" + index)
                            .text(entry.subtitle())
                            .textStyle(italic(BODY_SIZE, MUTED))
                            .margin(0f, 0f, (float) EMPLOYER_GAP, 0f));
                    List<String> highlights = lines(entry.body());
                    if (!highlights.isEmpty()) {
                        body.addList(list -> list
                                .name("Highlights_" + index)
                                .items(highlights)
                                .marker(ListMarker.bullet())
                                .textStyle(text(BODY_SIZE, INK, false))
                                .itemSpacing(HIGHLIGHT_ITEM_SPACING)
                                .lineSpacing(HIGHLIGHT_LINE_SPACING));
                    }
                });
            }
        });
    }

    // -- projects ----------------------------------------------------------

    /**
     * The projects grid: a sketch, the project's own three lines, and its
     * description across a hairline.
     *
     * <p>The mark is an inline run inside a paragraph rather than a block
     * icon node: a block icon in a row cell makes the whole row lay its cells
     * out vertically.</p>
     */
    private static void renderProjects(SectionBuilder main, EntriesSection projects) {
        main.addSection("SelectedProjects", block -> {
            block.spacing(0);
            headingWithDash(block, projects.title(), MAIN_HEADING_SPACER, MAIN_DASH_WIDTH);
            List<CvEntry> entries = projects.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                boolean last = i == entries.size() - 1;
                int index = i;
                layeredRow(block, "Project_" + index, 0.0, PROJECT_ROW_GAP, row -> {
                    row.weights(PROJECT_WEIGHTS[0], PROJECT_WEIGHTS[1], PROJECT_WEIGHTS[2]);
                    row.addParagraph(p -> {
                        p.name("ProjIcon_" + index);
                        if (!entry.icon().isBlank()) {
                            inlineIcon(p, entry.icon(), TerracottaRailIcons.PROJECT_SIZE);
                        }
                    });
                    row.addSection("ProjMeta_" + index, meta -> {
                        meta.spacing(0);
                        meta.padding(0f, (float) PROJECT_META_PAD_RIGHT, 0f,
                                (float) PROJECT_META_PAD_LEFT);
                        meta.addParagraph(p -> {
                            p.name("ProjTitle_" + index)
                                    .text(entry.title())
                                    .textStyle(text(ITEM_TITLE_SIZE, INK, true))
                                    .margin(0f, 0f, 1.0f, 0f);
                            if (!entry.link().isBlank()) {
                                p.link(new DocumentLinkOptions(entry.link()));
                            }
                        });
                        meta.addParagraph(p -> p
                                .name("ProjSub_" + index)
                                .text(entry.subtitle())
                                .textStyle(italic(DETAIL_SIZE, MUTED))
                                .margin(0f, 0f, 1.0f, 0f));
                        meta.addParagraph(p -> p
                                .name("ProjLoc_" + index)
                                .text(entry.place())
                                .textStyle(text(DETAIL_SIZE, INK, false)));
                    });
                    row.addSection("ProjDesc_" + index, description -> {
                        description.spacing(0);
                        description.accentLeft(RULE, RULE_THICKNESS);
                        description.padding(0f, 0f, 0f, (float) PROJECT_DESCRIPTION_PAD_LEFT);
                        description.addParagraph(p -> p
                                .name("ProjDescText_" + index)
                                .text(entry.body())
                                .textStyle(text(BODY_SIZE, INK, false))
                                .lineSpacing(PROJECT_LINE_SPACING));
                    });
                });
                if (!last) {
                    block.addLine(line -> line
                            .name("ProjDiv_" + index)
                            .fill()
                            .thickness(RULE_THICKNESS)
                            .color(RULE)
                            .margin(new DocumentInsets(
                                    PROJECT_DIVIDER_GAP, 0, PROJECT_DIVIDER_GAP, 0)));
                }
            }
        });
    }

    // -- education ---------------------------------------------------------

    /**
     * The degrees, on a rail of their own.
     *
     * <p>Their rail is terracotta where the roles' is grey, and the block
     * stops short of the column: the design closes it with a hairline at
     * about seven tenths of the main column, which is what the empty band
     * beside it is for.</p>
     */
    private static void renderEducation(SectionBuilder main, EntriesSection education) {
        main.addSection("Education", block -> {
            block.spacing(0);
            headingWithDash(block, education.title(), MAIN_HEADING_SPACER, MAIN_DASH_WIDTH);
            layeredRow(block, "EducationBand", 0.0, 0.0, band -> {
                band.weights(EDUCATION_BAND_WEIGHT, 1.0 - EDUCATION_BAND_WEIGHT);
                band.addSection("EducationEntries", cell -> {
                    cell.spacing(0);
                    cell.accentRight(RULE, RULE_THICKNESS);
                    cell.padding(0f, (float) EDUCATION_BAND_PAD_RIGHT, 0f, 0f);
                    List<CvEntry> entries = education.entries();
                    for (int i = 0; i < entries.size(); i++) {
                        CvEntry entry = entries.get(i);
                        boolean last = i == entries.size() - 1;
                        int index = i;
                        cell.addSection("EducationEntry_" + index, body -> {
                            body.spacing(0);
                            if (!last) {
                                body.accentLeft(ACCENT, RULE_THICKNESS);
                            }
                            body.padding(0f, 0f, last ? 0f : (float) ENTRY_GAP,
                                    (float) ENTRY_INDENT);
                            railedLine(body, "Edu", index,
                                    titleAndDate("EduTable_" + index, entry.title(), entry.date(),
                                            EDUCATION_ENTRY_WIDTH, EDUCATION_PERIOD_SHARE));
                            body.addParagraph(p -> p
                                    .name("Institution_" + index)
                                    .text(entry.subtitle())
                                    .textStyle(italic(BODY_SIZE, MUTED)));
                        });
                    }
                });
                band.addSection("EducationBandRight", empty -> empty.spacing(0));
            });
        });
    }

    // -- shared ------------------------------------------------------------

    /** A body, one entry per line the document wrote. */
    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }

    private static boolean hasBody(ParagraphSection section) {
        return section != null && !section.body().isBlank();
    }

    private static boolean hasEntries(EntriesSection section) {
        return section != null && !section.entries().isEmpty();
    }
}
