package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BULLET_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BULLET_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BULLET_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DATE_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DATE_TO_MARKER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EMPLOYER_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ENTRY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_ENTRIES;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_SKILLS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_TOOLS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HIGHLIGHT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ICON_TO_LABEL;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LABEL_TO_DESC;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LOCATION_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LOCATION_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.MARKER_TO_COPY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PERIOD_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PIPE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RAIL_INDENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RAIL_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ROLE_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILLS_TO_TOOLS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_COLUMN_INSET;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DASH_OFF;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DASH_ON;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DESC_MEASURE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DESC_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DESC_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_DIVIDER_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SKILL_RAIL_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SUMMARY_TO_SKILLS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TITLE_TO_BULLETS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TOOLS_TO_EXPERIENCE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TOOL_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TOOL_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TOOL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.centred;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.style;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.headingRow;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.lines;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.markerDot;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.text;

/**
 * The middle of the sheet: the six-up skills grid, the tools strip and the
 * dated timeline.
 */
final class VioletGridBody {

    private VioletGridBody() {
    }

    // -- the skills grid ---------------------------------------------------

    /**
     * The skills grid — one row with 2n−1 columns: n equal content columns
     * interleaved with n−1 fixed hairline columns.
     *
     * <p>Building the separators as cells of the same row as the content is
     * what stops them drifting from the boundaries they mark, and dividing by
     * the entry count is what lets a seventh skill be a data change.</p>
     */
    static void renderSkills(PageFlowBuilder page, EntriesSection skills) {
        if (skills == null || skills.entries().isEmpty()) {
            return;
        }
        page.add(headingRow("Skills", skills.title(), SUMMARY_TO_SKILLS));
        List<CvEntry> entries = skills.entries();
        int count = entries.size();
        double columnWidth = (CONTENT_WIDTH - (count - 1) * SKILL_DIVIDER_THICKNESS) / count;
        double innerWidth = columnWidth - 2.0 * SKILL_COLUMN_INSET;
        double descriptionInset = Math.max(0.0, (innerWidth - SKILL_DESC_MEASURE) / 2.0);
        page.addRow("SkillGrid", row -> {
            row.spacing(0);
            row.margin(new DocumentInsets(HEADING_TO_SKILLS, 0, 0, 0));
            DocumentRowColumn[] columns = new DocumentRowColumn[2 * count - 1];
            for (int index = 0; index < count; index++) {
                columns[2 * index] = DocumentRowColumn.weight(1.0);
                if (index < count - 1) {
                    columns[2 * index + 1] = DocumentRowColumn.fixed(SKILL_DIVIDER_THICKNESS);
                }
            }
            row.columns(columns);
            for (int index = 0; index < count; index++) {
                CvEntry entry = entries.get(index);
                int position = index;
                row.addSection("Skill_" + position,
                        column -> renderSkillColumn(column, entry, position, descriptionInset));
                if (index < count - 1) {
                    row.addSection("SkillDivider_" + position, VioletGridBody::renderDivider);
                }
            }
        });
    }

    /** One column: the mark, the label and the description, on its own axis. */
    private static void renderSkillColumn(SectionBuilder column, CvEntry entry, int index,
                                          double descriptionInset) {
        column.spacing(0);
        column.padding(0f, (float) SKILL_COLUMN_INSET, 0f, (float) SKILL_COLUMN_INSET);
        if (!entry.icon().isBlank()) {
            column.addSvgIcon(VioletGridIcons.icon(entry.icon()),
                    VioletGridIcons.size(entry.icon()), HorizontalAlign.CENTER);
        }
        column.addParagraph(p -> p
                .name("SkillLabel_" + index)
                .text(entry.title())
                .align(TextAlign.CENTER)
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, LABEL_SIZE, INK, true))
                .margin((float) ICON_TO_LABEL, 0f, (float) LABEL_TO_DESC, 0f));
        column.addParagraph(p -> p
                .name("SkillDescription_" + index)
                .text(entry.body())
                .align(TextAlign.CENTER)
                .lineSpacing(leading(SKILL_DESC_PITCH, SKILL_DESC_SIZE))
                .textStyle(style(BODY_FONT, SKILL_DESC_SIZE, MUTED, false))
                // The description wraps to a narrower measure than the label
                // above it, which is why the inset is on this paragraph and not
                // on the column.
                .padding(0f, (float) descriptionInset, 0f, (float) descriptionInset));
    }

    /**
     * The dotted rule between two columns.
     *
     * <p>A dashed line is the only primitive that produces a dotted rule. An
     * accent border would take its height from the column beside it, which is
     * what the height constant has to state by hand instead — the trade the
     * design's dotted rule costs.</p>
     */
    private static void renderDivider(SectionBuilder cell) {
        cell.spacing(0);
        cell.addLine(line -> line
                .name("SkillDividerRule")
                .vertical(SKILL_RAIL_HEIGHT)
                .thickness(SKILL_DIVIDER_THICKNESS)
                .color(RULE)
                .dashed(SKILL_DASH_ON, SKILL_DASH_OFF));
    }

    // -- the tools strip ---------------------------------------------------

    /**
     * The tools — one paragraph of names and inline discs.
     *
     * <p>The discs are inline shape runs rather than a bullet glyph, so they
     * cannot fall victim to whatever a fallback font ships, and the line is
     * running text rather than a grid — which is what the design's constant gap
     * either side of every disc says it is.</p>
     */
    static void renderTools(PageFlowBuilder page, SkillsSection tools) {
        if (tools == null || tools.groups().stream().allMatch(g -> g.entries().isEmpty())) {
            return;
        }
        page.add(headingRow("Tools", tools.title(), SKILLS_TO_TOOLS));
        DocumentTextStyle name = style(BODY_FONT, TOOL_SIZE, BODY, false);
        List<String> items = new ArrayList<>();
        for (SkillGroup group : tools.groups()) {
            for (CvSkill skill : group.entries()) {
                items.add(skill.name());
            }
        }
        page.addParagraph(p -> {
            p.name("ToolsLine");
            p.lineSpacing(0);
            p.textStyle(name);
            for (int index = 0; index < items.size(); index++) {
                if (index > 0) {
                    p.inlineText(TOOL_GAP, name);
                    p.dot(TOOL_DOT_DIAMETER, ACCENT);
                    p.inlineText(TOOL_GAP, name);
                }
                p.inlineText(items.get(index), name);
            }
            p.margin((float) HEADING_TO_TOOLS, 0f, 0f, 0f);
        });
    }

    // -- the timeline ------------------------------------------------------

    /**
     * The dated timeline.
     *
     * <p>Three columns, which is what the design has always been: the dates out at the page
     * margin, the discs on the rail, the copy inside it. Each is stated once for the whole
     * timeline, so the dates cannot drift the markers and a longer date cannot move the
     * copy. The rail is the timeline's own, drawn from where the markers landed.</p>
     */
    static void renderExperience(PageFlowBuilder page, EntriesSection experience) {
        if (experience == null || experience.entries().isEmpty()) {
            return;
        }
        page.add(headingRow("Experience", experience.title(), TOOLS_TO_EXPERIENCE));
        List<CvEntry> entries = experience.entries();
        page.addSection("ExperienceEntries", host -> {
            host.spacing(0);
            host.margin((float) HEADING_TO_ENTRIES, 0f, 0f, 0f);
            host.addTimeline(timeline -> {
                // Each gap is its own: the dates' column plus the air after it puts the
                // disc's centre on the rail, and the air after the disc puts the copy one
                // entry indent inside it. With a single gap for both, the dates' column
                // would be whatever was left over — 55.86pt, which breaks the longest date
                // across two lines.
                timeline.markerOnRail()
                        .rail(rail -> rail
                                .stroke(DocumentStroke.of(RULE, RAIL_THICKNESS))
                                .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                        .leadingColumn(DocumentRowColumn.fixed(DATE_COLUMN))
                        .leadingGap(DATE_TO_MARKER)
                        .axisWidth(MARKER_DIAMETER)
                        .markerGap(MARKER_TO_COPY)
                        .gutter(0)
                        .spacing(ENTRY_GAP)
                        .keepEntriesTogether();
                for (int i = 0; i < entries.size(); i++) {
                    CvEntry entry = entries.get(i);
                    int index = i;
                    timeline.entry(entryMarker(index), e -> e
                            .leading(column -> period(column, entry, index))
                            .content(column -> renderEntry(column, entry, index)));
                }
            });
        });
    }

    /**
     * The disc, in a box the height of the title line it belongs to.
     *
     * <p>Declaring the line rather than the disc is what keeps the mark where it was: a
     * timeline top-aligns a marker in its column, so a box the size of the disc would ride
     * the line's top edge instead of its middle. A box the size of the line, with the disc
     * centred in it, puts the disc where the layer stack used to hold it — and puts the
     * rail's anchor, that box's own centre, through the disc rather than above it.</p>
     */
    private static TimelineMarker entryMarker(int index) {
        return TimelineMarker.custom(MARKER_DIAMETER, ROLE_SIZE * LINE_FACTOR, column -> {
            column.spacing(0);
            column.padding((float) centred(MARKER_DIAMETER), 0f, 0f, 0f);
            column.add(markerDot("Marker_" + index, MARKER_DIAMETER,
                    MARKER_DIAMETER / LINE_FACTOR, ACCENT));
        });
    }

    /** The dates, in their own column, dropped onto the title line's setting. */
    private static void period(SectionBuilder column, CvEntry entry, int index) {
        column.spacing(0);
        column.padding((float) centred(PERIOD_SIZE * LINE_FACTOR), 0f, 0f, 0f);
        column.add(text("Period_" + index, entry.date(),
                style(BODY_FONT, PERIOD_SIZE, ACCENT, false)));
    }

    /**
     * One entry: the title line, then the bullets.
     *
     * <p>The title is a two-column table — a row here would be a row inside a row's
     * cell.</p>
     */
    private static void renderEntry(SectionBuilder body, CvEntry entry, int index) {
        body.spacing(0);
        body.add(titleLine(entry, index));
        List<String> highlights = lines(entry.body());
        if (highlights.isEmpty()) {
            return;
        }
        body.addTable(table -> {
            table.name("Highlights_" + index);
            table.margin(new DocumentInsets(TITLE_TO_BULLETS, 0, 0, 0));
            table.width(ENTRY_WIDTH);
            table.columns(
                    DocumentTableColumn.fixed(BULLET_COLUMN),
                    DocumentTableColumn.fixed(ENTRY_WIDTH - BULLET_COLUMN));
            table.defaultCellStyle(cellStyle(
                    new DocumentInsets(0, 0,
                            gap(BULLET_PITCH, HIGHLIGHT_SIZE * LINE_FACTOR), 0),
                    DocumentTableTextAnchor.TOP_LEFT));
            for (int i = 0; i < highlights.size(); i++) {
                table.rowCells(
                        DocumentTableCell.node(markerDot("HighlightDot_" + index + "_" + i,
                                BULLET_DOT_DIAMETER, HIGHLIGHT_SIZE, ACCENT)),
                        DocumentTableCell.node(text("Highlight_" + index + "_" + i,
                                highlights.get(i),
                                style(BODY_FONT, HIGHLIGHT_SIZE, BODY, false))));
            }
            // The pitch is a gap BETWEEN bullets; left on the last one it
            // becomes a gap the entry below already owns.
            table.rowStyle(highlights.size() - 1,
                    cellStyle(DocumentInsets.zero(), DocumentTableTextAnchor.TOP_LEFT));
        });
    }

    /** The role, the employer and the location, as one line of three runs. */
    private static DocumentNode titleLine(CvEntry entry, int index) {
        DocumentTextStyle role = style(BODY_FONT, ROLE_SIZE, INK, true);
        DocumentTextStyle employer = style(BODY_FONT, EMPLOYER_SIZE, MUTED, false);
        ParagraphBuilder title = new ParagraphBuilder()
                .name("Role_" + index)
                .lineSpacing(0)
                .textStyle(role)
                .inlineText(entry.title(), role);
        if (!entry.subtitle().isBlank()) {
            title.inlineText(PIPE, employer).inlineText(entry.subtitle(), employer);
        }
        if (!entry.link().isBlank()) {
            title.link(new DocumentLinkOptions(entry.link()));
        }
        return new TableBuilder()
                .name("TitleTable_" + index)
                .width(ENTRY_WIDTH)
                .columns(
                        DocumentTableColumn.fixed(ROLE_COLUMN),
                        DocumentTableColumn.fixed(LOCATION_COLUMN))
                .defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.TOP_LEFT))
                .rowCells(
                        DocumentTableCell.node(title.build()),
                        DocumentTableCell.node(new ParagraphBuilder()
                                .name("Location_" + index)
                                .text(entry.place())
                                .align(TextAlign.RIGHT)
                                .lineSpacing(0)
                                .textStyle(style(BODY_FONT, LOCATION_SIZE, MUTED, false))
                                .build()))
                .build();
    }
}
