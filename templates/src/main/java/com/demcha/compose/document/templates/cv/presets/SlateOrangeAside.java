package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
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

import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACHIEVEMENT_BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACHIEVEMENT_BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACHIEVEMENT_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACHIEVEMENT_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACHIEVEMENTS_TOP_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ADDITIONAL_ICON_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ADDITIONAL_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ADDITIONAL_TOP_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.COMPETENCY_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HALF_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ICON_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGE_LEVEL_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGE_NAME_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGE_RATING_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LANGUAGES_TOP_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LEVEL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RATING_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RATING_EMPTY;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_TO_SIDEBAR_BODY;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.SIDEBAR_TABLE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.heading;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.iconCell;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.iconTable;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.spacer;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.text;

/**
 * The narrow column: the marked competencies, the achievements, the rated
 * languages and the closing facts.
 */
final class SlateOrangeAside {

    /** The mark every achievement takes when its entry names none. */
    private static final String DEFAULT_ACHIEVEMENT_MARK = SlateOrangeIcons.ACHIEVEMENT;

    /** How many discs a rating is drawn out of. */
    private static final int RATING_SCALE = 5;

    private SlateOrangeAside() {
    }

    static void compose(SectionBuilder side, EntriesSection competencies,
                        EntriesSection achievements, SkillsSection languages,
                        EntriesSection facts) {
        side.spacing(0);
        side.padding((float) BODY_TOP, (float) HALF_GAP, (float) PAGE_MARGIN,
                (float) PAGE_MARGIN);
        if (hasEntries(competencies)) {
            renderCompetencies(side, competencies);
        }
        if (hasEntries(achievements)) {
            renderAchievements(side, achievements);
        }
        if (hasSkills(languages)) {
            renderLanguages(side, languages);
        }
        if (hasEntries(facts)) {
            renderFacts(side, facts);
        }
    }

    /** The competencies — one mark-and-label row each, on a fixed mark column. */
    private static void renderCompetencies(SectionBuilder side, EntriesSection section) {
        side.addSection("Competencies", block -> {
            block.spacing(0);
            heading(block, "Competencies", section.title());
            iconTable(block, "CompetencyTable", "Competency", section.entries(),
                    COMPETENCY_PITCH, ICON_COLUMN);
        });
    }

    /** The closing facts — the same mark column at a wider row pitch. */
    private static void renderFacts(SectionBuilder side, EntriesSection section) {
        side.addSection("Additional", block -> {
            block.spacing(0);
            block.margin((float) ADDITIONAL_TOP_GAP, 0f, 0f, 0f);
            heading(block, "Additional", section.title());
            iconTable(block, "AdditionalTable", "Additional", section.entries(),
                    ADDITIONAL_PITCH, ADDITIONAL_ICON_COLUMN);
        });
    }

    // -- achievements ------------------------------------------------------

    /**
     * The achievements — a trophy in its own column beside a bold title over
     * its body.
     *
     * <p>Two table rows per achievement rather than one: the title and the
     * body are separate lines that must both start at the label column, and a
     * cell holds one node. The body's row leaves the mark column empty, which
     * is what makes the body hang on the title's left edge rather than wrap
     * back under the trophy.</p>
     */
    private static void renderAchievements(SectionBuilder side, EntriesSection section) {
        side.addSection("Achievements", block -> {
            block.spacing(0);
            block.margin((float) ACHIEVEMENTS_TOP_GAP, 0f, 0f, 0f);
            heading(block, "Achievements", section.title());
            List<CvEntry> entries = section.entries();
            block.addTable(table -> {
                table.name("AchievementTable");
                table.margin(new DocumentInsets(RULE_TO_SIDEBAR_BODY, 0, 0, 0));
                table.width(SIDEBAR_TABLE_WIDTH);
                table.columns(
                        DocumentTableColumn.fixed(ICON_COLUMN),
                        DocumentTableColumn.fixed(SIDEBAR_TABLE_WIDTH - ICON_COLUMN));
                table.defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.TOP_LEFT));
                for (int index = 0; index < entries.size(); index++) {
                    CvEntry entry = entries.get(index);
                    boolean last = index == entries.size() - 1;
                    String mark = entry.icon().isBlank()
                            ? DEFAULT_ACHIEVEMENT_MARK
                            : entry.icon();
                    table.rowCells(
                            DocumentTableCell.node(iconCell("AchievementIcon_" + index, mark,
                                    ACHIEVEMENT_TITLE_SIZE)),
                            DocumentTableCell.node(text("AchievementTitle_" + index,
                                    entry.title(),
                                    style(BODY_FONT, ACHIEVEMENT_TITLE_SIZE, INK, true))));
                    table.rowCells(
                            DocumentTableCell.node(spacer("AchievementGutter_" + index)),
                            DocumentTableCell.node(new ParagraphBuilder()
                                    .name("AchievementBody_" + index)
                                    .text(entry.body())
                                    .lineSpacing(leading(ACHIEVEMENT_BODY_LEADING,
                                            ACHIEVEMENT_BODY_SIZE))
                                    .textStyle(style(BODY_FONT, ACHIEVEMENT_BODY_SIZE, INK, false))
                                    .build()));
                    if (!last) {
                        // The gap belongs to the row, not to the paragraph in
                        // it: a margin on a node inside a table cell is the
                        // cell's business, and the cell ignores it.
                        table.rowStyle(2 * index + 1, cellStyle(
                                new DocumentInsets(0, 0, ACHIEVEMENT_GAP, 0),
                                DocumentTableTextAnchor.TOP_LEFT));
                    }
                }
            });
        });
    }

    // -- languages ---------------------------------------------------------

    /**
     * The languages — the name, the level as the document words it, and the
     * rating as five discs, on one line.
     *
     * <p>This is the block that wants both channels of a rated skill: the
     * discs come from {@code CvSkill.level()} and the words beside them from
     * {@code CvSkill.note()}. A skill with no level draws no discs, and one
     * with no note leaves that column empty rather than inventing a
     * wording.</p>
     */
    private static void renderLanguages(SectionBuilder side, SkillsSection languages) {
        List<CvSkill> entries = new ArrayList<>();
        for (SkillGroup group : languages.groups()) {
            entries.addAll(group.entries());
        }
        side.addSection("Languages", block -> {
            block.spacing(0);
            block.margin((float) LANGUAGES_TOP_GAP, 0f, 0f, 0f);
            heading(block, "Languages", languages.title());
            block.addTable(table -> {
                table.name("LanguageTable");
                table.margin(new DocumentInsets(RULE_TO_SIDEBAR_BODY, 0, 0, 0));
                table.width(SIDEBAR_TABLE_WIDTH);
                table.columns(
                        DocumentTableColumn.fixed(LANGUAGE_NAME_COLUMN),
                        DocumentTableColumn.fixed(LANGUAGE_LEVEL_COLUMN),
                        DocumentTableColumn.fixed(LANGUAGE_RATING_COLUMN));
                table.defaultCellStyle(cellStyle(
                        new DocumentInsets(0, 0,
                                gap(LANGUAGE_PITCH, LANGUAGE_SIZE * LINE_FACTOR), 0),
                        DocumentTableTextAnchor.CENTER_LEFT));
                for (int index = 0; index < entries.size(); index++) {
                    CvSkill skill = entries.get(index);
                    table.rowCells(
                            DocumentTableCell.node(text("LanguageName_" + index, skill.name(),
                                    style(BODY_FONT, LANGUAGE_SIZE, INK, false))),
                            DocumentTableCell.node(skill.note().isBlank()
                                    ? spacer("LanguageLevel_" + index)
                                    : text("LanguageLevel_" + index, skill.note(),
                                            style(BODY_FONT, LEVEL_SIZE, MUTED, false))),
                            DocumentTableCell.node(rating("LanguageRating_" + index, skill)));
                }
                table.rowStyle(entries.size() - 1,
                        cellStyle(DocumentInsets.zero(), DocumentTableTextAnchor.CENTER_LEFT));
            });
        });
    }

    /**
     * One language's rating, as inline shape runs rather than a bullet glyph,
     * so it cannot fall victim to whatever a fallback font does or does not
     * ship.
     *
     * <p>The paragraph is styled although it carries no words: a paragraph
     * takes its line box from its type whether or not a glyph uses it, and the
     * default box would set the whole row's pitch.</p>
     */
    private static DocumentNode rating(String name, CvSkill skill) {
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name(name);
        paragraph.lineSpacing(0);
        DocumentTextStyle gapStyle = style(BODY_FONT, LANGUAGE_SIZE, INK, false);
        paragraph.textStyle(gapStyle);
        if (skill.level().isEmpty()) {
            return paragraph.build();
        }
        int filled = (int) Math.round(skill.level().getAsDouble() * RATING_SCALE);
        for (int disc = 0; disc < RATING_SCALE; disc++) {
            paragraph.dot(RATING_DOT_DIAMETER, disc < filled ? ACCENT : RATING_EMPTY);
            if (disc < RATING_SCALE - 1) {
                // Two spaces, not one: the design sets the discs nearly twice
                // their own diameter apart, and one space at this size is half
                // of that.
                paragraph.inlineText("  ", gapStyle);
            }
        }
        return paragraph.build();
    }

    // -- presence ----------------------------------------------------------

    private static boolean hasEntries(EntriesSection section) {
        return section != null && !section.entries().isEmpty();
    }

    private static boolean hasSkills(SkillsSection section) {
        return section != null
                && section.groups().stream().anyMatch(group -> !group.entries().isEmpty());
    }
}
