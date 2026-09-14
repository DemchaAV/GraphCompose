package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailProjects.projectDivider;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailProjects.renderProject;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailProjects.renderProjects;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.COLUMN_PAD_BOTTOM;
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
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DIVIDER_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DIVIDER_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MASTHEAD_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MASTHEAD_DIVIDER_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MASTHEAD_DIVIDER_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.NAME_TO_SUBTITLE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ROLE_PERIOD_SHARE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUBTITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUBTITLE_TO_RULE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUMMARY_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SUMMARY_LINE_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.italic;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.text;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.divider;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.heading;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.headingWithDash;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.layeredRow;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.entryLine;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.ringMarker;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.titleAndDate;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.spacedBy;

/**
 * The wide column: the masthead, the summary, the roles held on a rail, the
 * projects grid and the degrees.
 */
final class TerracottaRailMain {

    /** What a block of the column holds. */
    enum Kind {
        /** The name over the role. */
        MASTHEAD,
        /** The summary under its hairline. */
        SUMMARY,
        /** One role; the first carries the experience heading. */
        ROLE,
        /** One project; the first carries the projects heading. */
        PROJECT,
        /** The degrees. */
        EDUCATION
    }

    /**
     * One block of the column, and what it holds — which is what lets a page
     * put the roles it carries back on a single rail.
     *
     * @param kind  what the block holds
     * @param index the entry's index for a role or a project, otherwise zero
     * @param block the block as a plan measures it
     */
    record Piece(Kind kind, int index, ColumnPages.Block block) {
    }

    private TerracottaRailMain() {
    }

    static void compose(SectionBuilder main, CvIdentity identity, ParagraphSection summary,
                        EntriesSection experience, EntriesSection projects,
                        EntriesSection education) {
        main.name("MainColumn");
        pad(main);

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

    /**
     * The column as blocks that move to another page whole, for a CV longer
     * than the page: the masthead, the summary, each role, each project and
     * the degrees. A heading rides on the first entry it heads, and the
     * hairline above a block is its lead, left out when the block opens a
     * page. A later role's lead is the rail's gap between two entries, which
     * only the plan counts: on the page, the rail itself spaces them.
     */
    static List<Piece> pieces(CvIdentity identity, ParagraphSection summary,
                              EntriesSection experience, EntriesSection projects,
                              EntriesSection education) {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(Kind.MASTHEAD, 0, ColumnPages.Block.of("Masthead",
                main -> renderMasthead(main, identity))));
        if (hasBody(summary)) {
            pieces.add(new Piece(Kind.SUMMARY, 0, new ColumnPages.Block("Summary",
                    main -> divider(main, "AfterMasthead", MASTHEAD_DIVIDER_TOP,
                            MASTHEAD_DIVIDER_BOTTOM),
                    main -> renderSummary(main, summary))));
        }
        if (hasEntries(experience)) {
            for (int i = 0; i < experience.entries().size(); i++) {
                int index = i;
                pieces.add(new Piece(Kind.ROLE, index, new ColumnPages.Block("Role_" + index,
                        index == 0
                                ? main -> mainDivider(main, "AfterSummary")
                                : main -> main.addSpacer(spacer -> spacer
                                        .name("RoleGap_" + index)
                                        .height(ENTRY_GAP)),
                        main -> renderRoles(main, experience, index, index))));
            }
        }
        if (hasEntries(projects)) {
            for (int i = 0; i < projects.entries().size(); i++) {
                int index = i;
                pieces.add(new Piece(Kind.PROJECT, index, new ColumnPages.Block("Project_" + index,
                        index == 0
                                ? main -> mainDivider(main, "AfterExperience")
                                : main -> projectDivider(main, index - 1),
                        main -> renderProject(main, projects, index))));
            }
        }
        if (hasEntries(education)) {
            pieces.add(new Piece(Kind.EDUCATION, 0, new ColumnPages.Block("Education",
                    main -> mainDivider(main, "AfterProjects"),
                    main -> renderEducation(main, education))));
        }
        return pieces;
    }

    /**
     * The blocks of a column's pieces, in order, as a plan measures them.
     *
     * @param pieces the column's pieces
     * @return their blocks
     */
    static List<ColumnPages.Block> blocks(List<Piece> pieces) {
        List<ColumnPages.Block> blocks = new ArrayList<>(pieces.size());
        for (Piece piece : pieces) {
            blocks.add(piece.block());
        }
        return blocks;
    }

    /**
     * One page of the column: its padding on every page and the blocks the
     * plan put there, the page's roles drawn on one rail.
     *
     * @param main       the column's section on the page
     * @param pieces     all of the column's pieces
     * @param experience the roles, for drawing a page's run of them together
     * @param page       the indices of the pieces on the page
     */
    static void composePage(SectionBuilder main, List<Piece> pieces, EntriesSection experience,
                            List<Integer> page) {
        main.name("MainColumn");
        pad(main);
        int at = 0;
        while (at < page.size()) {
            Piece piece = pieces.get(page.get(at));
            if (at > 0 && piece.block().lead() != null) {
                piece.block().lead().accept(main);
            }
            if (piece.kind() == Kind.ROLE) {
                int last = at;
                while (last + 1 < page.size()
                        && pieces.get(page.get(last + 1)).kind() == Kind.ROLE) {
                    last++;
                }
                renderRoles(main, experience, piece.index(), pieces.get(page.get(last)).index());
                at = last + 1;
            } else {
                piece.block().content().accept(main);
                at++;
            }
        }
    }

    private static void pad(SectionBuilder main) {
        main.spacing(0);
        main.padding((float) MAIN_PAD_TOP, (float) MAIN_PAD_RIGHT,
                (float) COLUMN_PAD_BOTTOM, (float) MAIN_PAD_LEFT);
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
                    .text(identity.name().full().toUpperCase(Locale.ROOT))
                    .textStyle(spacedBy(text(NAME_SIZE, INK, true), MASTHEAD_TRACKING_EM))
                    .margin(0f, 0f, (float) NAME_TO_SUBTITLE_GAP, 0f));
            block.addParagraph(p -> p
                    .name("JobTitle")
                    .text(identity.jobTitle().toUpperCase(Locale.ROOT))
                    .textStyle(spacedBy(text(SUBTITLE_SIZE, ACCENT, true), MASTHEAD_TRACKING_EM))
                    .margin(0f, 0f, (float) SUBTITLE_TO_RULE_GAP, 0f));
        });
    }

    // -- summary -----------------------------------------------------------

    /** The summary, one paragraph per line the document wrote. */
    private static void renderSummary(SectionBuilder main, ParagraphSection summary) {
        main.addSection("Summary", block -> {
            block.spacing(0);
            headingWithDash(block, summary.title(), MAIN_HEADING_TRACKING_EM, MAIN_DASH_WIDTH);
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
        renderRoles(main, experience, 0, experience.entries().size() - 1);
    }

    /**
     * The roles from {@code from} to {@code to} on one rail, under the
     * experience heading when the first of them is the first role.
     */
    private static void renderRoles(SectionBuilder main, EntriesSection experience,
                                    int from, int to) {
        main.addSection(from == 0 ? "Experience" : "Experience_" + from, block -> {
            block.spacing(0);
            if (from == 0) {
                heading(block, experience.title(), MAIN_HEADING_TRACKING_EM);
            }
            SectionBuilder holder = new SectionBuilder();
            holder.name("ExperienceRailHolder");
            holder.spacing(0);
            // The axis column centres the rail on itself, so the timeline starts half a ring
            // left of the edge the accents drew on and the line lands back on it.
            holder.margin(new DocumentInsets(0, 0, 0, -MARKER_DIAMETER / 2.0));
            holder.addTimeline(timeline -> {
                timeline.markerOnRail()
                        .rail(rail -> rail
                                .stroke(DocumentStroke.of(RULE, RULE_THICKNESS))
                                .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                        .axisWidth(MARKER_DIAMETER)
                        .markerGap(ENTRY_INDENT - MARKER_DIAMETER / 2.0)
                        .gutter(0)
                        .spacing(ENTRY_GAP);
                List<CvEntry> entries = experience.entries();
                for (int i = from; i <= to; i++) {
                    CvEntry entry = entries.get(i);
                    int index = i;
                    timeline.entry(ringMarker("Role", index), e -> e.content(body -> {
                        body.spacing(0);
                        entryLine(body, "Role", index,
                                titleAndDate("RoleTable_" + index, entry.title(), entry.date(),
                                        ENTRY_WIDTH, ROLE_PERIOD_SHARE, entry.link()));
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
                    }));
                }
            });
            DocumentNode experienceRail = holder.build();
            // The timeline goes in through a layer stack rather than straight into
            // the block. A timeline publishes a row, a row cannot nest inside a row
            // cell, and this block can sit in one — the stack is what insulates it.
            // Unwinding the stacks that turn out not to need it buys no capability,
            // so they stay until that is done as its own deliberate cleanup.
            block.addLayerStack(stack -> stack
                    .name("ExperienceRail")
                    .layer(experienceRail, LayerAlign.TOP_LEFT, 0));
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
            headingWithDash(block, education.title(), MAIN_HEADING_TRACKING_EM, MAIN_DASH_WIDTH);
            layeredRow(block, "EducationBand", 0.0, 0.0, band -> {
                band.weights(EDUCATION_BAND_WEIGHT, 1.0 - EDUCATION_BAND_WEIGHT);
                band.addSection("EducationEntries", cell -> {
                    cell.spacing(0);
                    cell.accentRight(RULE, RULE_THICKNESS);
                    cell.padding(0f, (float) EDUCATION_BAND_PAD_RIGHT, 0f, 0f);
                    // A second rail, terracotta rather than grey, and its own timeline: two
                    // timelines on a page are two rail owners and never merge into one line.
                    SectionBuilder holder = new SectionBuilder();
                    holder.name("EducationRailHolder");
                    holder.spacing(0);
                    holder.margin(new DocumentInsets(0, 0, 0, -MARKER_DIAMETER / 2.0));
                    holder.addTimeline(timeline -> {
                        timeline.markerOnRail()
                                .rail(rail -> rail
                                        .stroke(DocumentStroke.of(ACCENT, RULE_THICKNESS))
                                        .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                                .axisWidth(MARKER_DIAMETER)
                                .markerGap(ENTRY_INDENT - MARKER_DIAMETER / 2.0)
                                .gutter(0)
                                .spacing(ENTRY_GAP);
                        List<CvEntry> entries = education.entries();
                        for (int i = 0; i < entries.size(); i++) {
                            CvEntry entry = entries.get(i);
                            int index = i;
                            timeline.entry(ringMarker("Edu", index), e -> e.content(body -> {
                                body.spacing(0);
                                entryLine(body, "Edu", index,
                                        titleAndDate("EduTable_" + index, entry.title(), entry.date(),
                                                EDUCATION_ENTRY_WIDTH, EDUCATION_PERIOD_SHARE,
                                                entry.link()));
                                body.addParagraph(p -> p
                                        .name("Institution_" + index)
                                        .text(entry.subtitle())
                                        .textStyle(italic(BODY_SIZE, MUTED)));
                            }));
                        }
                    });
                    DocumentNode educationRail = holder.build();
                    // The band's own stack wraps the band row, one level above this cell, so
                    // it does not insulate a timeline placed inside the cell: this is a row
                    // slot and needs a layer of its own.
                    cell.addLayerStack(stack -> stack
                            .name("EducationRail")
                            .layer(educationRail, LayerAlign.TOP_LEFT, 0));
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
