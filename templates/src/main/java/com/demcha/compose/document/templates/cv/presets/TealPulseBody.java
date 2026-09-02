package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT_DEEP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_TO_CLOSING_RULE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BULLET_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BULLET_LEADING;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BULLET_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.COMPETENCY_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.COMPETENCY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DASH_TO_HEADING;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DIVIDER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ENTRY_HEADLINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ENTRY_TO_BULLETS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADER_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADER_TO_ENTRY;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADING_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADING_TO_ITEMS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ITEMS_TO_CLOSING_RULE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MAIN_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_PALE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_CLOSING_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_CLOSING_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_DASH_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SIDEBAR_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SUMMARY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SUMMARY_RIGHT_INSET;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SUMMARY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SUMMARY_TO_EXPERIENCE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.style;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.dottedLine;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.sectionHeader;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.tracked;

/**
 * The body row: the competencies in the narrow column, the summary and the
 * roles in the wide one.
 */
final class TealPulseBody {

    private TealPulseBody() {
    }

    /**
     * The one two-column band on the sheet.
     *
     * <p>The rule between the columns is the main column's own left accent,
     * not a drawn line. That is what makes its height follow the taller of the
     * two columns, which is what the design shows: it runs well past the end
     * of the competency list.</p>
     */
    static void render(PageFlowBuilder page, SkillsSection competencies,
                       ParagraphSection summary, EntriesSection experience) {
        page.addRow("BodyGrid", row -> {
            row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
            row.verticalAlign(RowVerticalAlign.TOP);
            row.spacing(0);
            row.addSection("Sidebar", sidebar -> renderCompetencies(sidebar, competencies));
            row.addSection("Main", main -> {
                main.spacing(0);
                main.accentLeft(RULE_SOFT, DIVIDER_WIDTH);
                main.padding(new DocumentInsets(0, 0, 0, MAIN_PAD_LEFT));
                renderSummary(main, summary);
                renderExperience(main, experience);
            });
            row.margin(new DocumentInsets(0, 0, BODY_TO_CLOSING_RULE, 0));
        });
    }

    // -- the narrow column -------------------------------------------------

    /** A dash, a tracked heading, the dotted lines, and a pale closing rule. */
    private static void renderCompetencies(SectionBuilder section, SkillsSection competencies) {
        section.spacing(0);
        section.padding(new DocumentInsets(SIDEBAR_PAD_TOP, 0, 0, 0));
        if (competencies == null) {
            return;
        }
        section.addLine(line -> line
                .name("CompetenciesDash")
                .horizontal(SIDEBAR_DASH_WIDTH)
                .thickness(SIDEBAR_DASH_THICKNESS)
                .color(ACCENT)
                .margin(new DocumentInsets(0, 0, DASH_TO_HEADING, 0)));

        DocumentTextStyle headingStyle = style(
                HEADING_FONT, SIDEBAR_HEADING_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
        ParagraphBuilder heading = new ParagraphBuilder()
                .name("CompetenciesHeading")
                .textStyle(headingStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0);
        tracked(heading, competencies.title(), headingStyle, SIDEBAR_HEADING_TRACKING_EM);
        section.add(heading.margin(new DocumentInsets(0, 0, HEADING_TO_ITEMS, 0)).build());

        section.addSection("CompetencyItems", items -> {
            items.spacing(COMPETENCY_GAP);
            for (SkillGroup group : competencies.groups()) {
                for (CvSkill skill : group.entries()) {
                    items.add(dottedLine("Competency_" + compact(skill.name()), skill.name(),
                            COMPETENCY_SIZE, 1.0));
                }
            }
        });
        section.addLine(line -> line
                .name("CompetenciesClosingRule")
                .horizontal(SIDEBAR_CLOSING_RULE_WIDTH)
                .thickness(SIDEBAR_CLOSING_RULE_THICKNESS)
                .color(RULE_PALE)
                .margin(new DocumentInsets(ITEMS_TO_CLOSING_RULE, 0, 0, 0)));
    }

    // -- the wide column ---------------------------------------------------

    /**
     * The summary under its badge.
     *
     * <p>The paragraph carries a right inset because the design breaks its
     * first line with a finger's width of the column still free: reproducing
     * the line breaks means reproducing the measure, not only the type
     * size.</p>
     *
     * <p>The summary is one flowing block here, so a body written as several
     * lines is joined into one and left to wrap. This is the one berth that
     * does that — everywhere else a line the document wrote is a line the
     * sheet draws.</p>
     */
    private static void renderSummary(SectionBuilder main, ParagraphSection summary) {
        if (summary == null || summary.body().isBlank()) {
            return;
        }
        main.add(sectionHeader(summary.title(), TealPulseIcons.SUMMARY, HEADER_TO_BODY));
        main.addParagraph(paragraph -> paragraph
                .name("SummaryText")
                .text(summary.body().replace(String.valueOf((char) 10), " "))
                .textStyle(style(BODY_FONT, SUMMARY_SIZE, BODY_TEXT,
                        DocumentTextDecoration.DEFAULT))
                .align(TextAlign.LEFT)
                .lineSpacing(SUMMARY_LEADING)
                .margin(new DocumentInsets(0, SUMMARY_RIGHT_INSET, SUMMARY_TO_EXPERIENCE, 0)));
    }

    /** The roles, each a two-voice headline over its dotted highlights. */
    private static void renderExperience(SectionBuilder main, EntriesSection experience) {
        if (experience == null || experience.entries().isEmpty()) {
            return;
        }
        main.add(sectionHeader(experience.title(), TealPulseIcons.EXPERIENCE, HEADER_TO_ENTRY));
        List<CvEntry> entries = experience.entries();
        for (int i = 0; i < entries.size(); i++) {
            CvEntry entry = entries.get(i);
            boolean last = i == entries.size() - 1;
            int index = i;
            DocumentTextStyle roleStyle = style(
                    BODY_FONT, ENTRY_HEADLINE_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
            DocumentTextStyle metaStyle = style(
                    BODY_FONT, ENTRY_HEADLINE_SIZE, BODY_TEXT, DocumentTextDecoration.BOLD);
            main.addParagraph(paragraph -> {
                paragraph.name("ExperienceHeadline_" + index)
                        .textStyle(metaStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.0);
                if (entry.link().isBlank()) {
                    paragraph.inlineText(entry.title(), roleStyle);
                } else {
                    paragraph.inlineText(entry.title(), roleStyle,
                            new DocumentLinkOptions(entry.link()));
                }
                paragraph.inlineText(headlineTail(entry), metaStyle)
                        .margin(new DocumentInsets(0, 0, ENTRY_TO_BULLETS, 0));
            });
            main.addSection("ExperienceHighlights_" + index, highlights -> {
                highlights.spacing(BULLET_GAP);
                for (String item : lines(entry.body())) {
                    highlights.add(dottedLine(
                            "Highlight_" + index + "_" + compact(item), item,
                            BULLET_SIZE, BULLET_LEADING));
                }
                highlights.margin(new DocumentInsets(0, 0, last ? 0 : ENTRY_GAP, 0));
            });
        }
    }

    /**
     * What follows the role in its headline: the employer, then the dates,
     * joined the way the design joins them. A dash and a pipe are typography
     * rather than content, so they live here and not in the document.
     */
    private static String headlineTail(CvEntry entry) {
        StringBuilder tail = new StringBuilder();
        if (!entry.subtitle().isBlank()) {
            tail.append(' ').append((char) 0x2014).append(' ').append(entry.subtitle());
        }
        if (!entry.date().isBlank()) {
            tail.append(tail.isEmpty() ? " " : " | ").append(entry.date());
        }
        return tail.toString();
    }

    /** A body, one entry per line the document wrote. */
    static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }
}
