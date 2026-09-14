package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_DESCRIPTION_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_DIVIDER_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_LINE_SPACING;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_META_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_META_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PROJECT_WEIGHTS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.italic;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.text;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.headingWithDash;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.inlineIcon;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.layeredRow;

/**
 * The wide column's projects grid: a sketch, the project's own three lines,
 * and its description across a hairline.
 *
 * <p>The mark is an inline run inside a paragraph rather than a block icon
 * node: a block icon in a row cell makes the whole row lay its cells out
 * vertically.</p>
 */
final class TerracottaRailProjects {

    private TerracottaRailProjects() {
    }

    /** Every project under the heading, a hairline under each one another follows. */
    static void renderProjects(SectionBuilder main, EntriesSection projects) {
        main.addSection("SelectedProjects", block -> {
            block.spacing(0);
            headingWithDash(block, projects.title(), MAIN_HEADING_TRACKING_EM, MAIN_DASH_WIDTH);
            List<CvEntry> entries = projects.entries();
            for (int i = 0; i < entries.size(); i++) {
                projectRow(block, entries.get(i), i);
                if (i < entries.size() - 1) {
                    projectDivider(block, i);
                }
            }
        });
    }

    /** One project on a page of its own choosing, under the heading when it is the first. */
    static void renderProject(SectionBuilder main, EntriesSection projects, int index) {
        main.addSection(index == 0 ? "SelectedProjects" : "SelectedProjects_" + index, block -> {
            block.spacing(0);
            if (index == 0) {
                headingWithDash(block, projects.title(), MAIN_HEADING_TRACKING_EM,
                        MAIN_DASH_WIDTH);
            }
            projectRow(block, projects.entries().get(index), index);
        });
    }

    /** The hairline under a project that another follows. */
    static void projectDivider(SectionBuilder block, int index) {
        block.addLine(line -> line
                .name("ProjDiv_" + index)
                .fill()
                .thickness(RULE_THICKNESS)
                .color(RULE)
                .margin(new DocumentInsets(
                        PROJECT_DIVIDER_GAP, 0, PROJECT_DIVIDER_GAP, 0)));
    }

    private static void projectRow(SectionBuilder block, CvEntry entry, int index) {
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
    }
}
