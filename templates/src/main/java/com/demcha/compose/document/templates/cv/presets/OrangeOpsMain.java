package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ADDITIONAL_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ADDITIONAL_RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BULLET_ITEM_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BULLET_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.COMPANY_TO_BULLETS;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.DATE_COLUMN_RATIO;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EXPERIENCE_RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.JOB_GAP;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.JOB_META_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.JOB_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.JOB_TITLE_TO_COMPANY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.LATO;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MAIN_JOIN_INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MAIN_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_CAPTION_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_ICON_TO_VALUE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_SEPARATOR;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_VALUE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.METRIC_VALUE_TO_CAPTION;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.NO_STROKE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.PROFILE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.PROFILE_RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.style;

/**
 * The wide column: the profile, the roles held, the metric strip, and the
 * closing lines — each under a fixed-length accent rule and separated from the
 * next by a hairline.
 *
 * <h2>A parenthetical in a heading is set smaller</h2>
 *
 * <p>The design sets {@code (Recent 12 Months)} after the metric heading in a
 * smaller body face, so a berth's title is split at its first opening bracket:
 * what comes before is the heading, and the bracket and everything after it is
 * the parenthetical. A title with no bracket is a heading and nothing else.</p>
 *
 * <h2>Why every horizontal pair here is a table</h2>
 *
 * <p>The main column is a row cell and a row cannot nest inside one, so the
 * title/date line and the metric strip are tables rather than rows. A cell here
 * is only ever given a paragraph or a spacer: a section with composite children
 * in a cell reserves its box and draws nothing.</p>
 */
final class OrangeOpsMain {

    private OrangeOpsMain() {
    }

    /**
     * Draws the column.
     *
     * @param main       the main cell
     * @param profile    the opening prose, or {@code null}
     * @param experience the roles held, or {@code null}
     * @param metrics    the metric strip, or {@code null}
     * @param additional the closing lines, or {@code null}
     */
    static void compose(SectionBuilder main, ParagraphSection profile, EntriesSection experience,
                        EntriesSection metrics, EntriesSection additional) {
        List<OrangeOpsWidgets.Block> blocks = new ArrayList<>();
        if (SectionLookup.hasContent(profile)) {
            blocks.add(new OrangeOpsWidgets.Block("Profile",
                    column -> renderProfile(column, profile), LATO, BODY_SIZE, false));
        }
        if (SectionLookup.hasContent(experience)) {
            // A bullet list ends on a full sentence, which descends.
            blocks.add(new OrangeOpsWidgets.Block("Experience",
                    column -> renderExperience(column, experience), LATO, BODY_SIZE, true));
        }
        if (SectionLookup.hasContent(metrics)) {
            blocks.add(new OrangeOpsWidgets.Block("Metrics",
                    column -> renderMetrics(column, metrics), LATO, METRIC_CAPTION_SIZE, false));
        }
        if (SectionLookup.hasContent(additional)) {
            blocks.add(new OrangeOpsWidgets.Block("Additional",
                    column -> renderAdditional(column, additional), LATO, BODY_SIZE, false));
        }
        OrangeOpsWidgets.stack(main, blocks, MAIN_JOIN_INK);
    }

    /**
     * The profile — one paragraph.
     *
     * <p>The design sets it flush on both edges; the alignments available here
     * are left, centre and right, so it is set flush left and the line breaks
     * are the engine's.</p>
     */
    private static void renderProfile(SectionBuilder main, ParagraphSection profile) {
        main.addSection("Profile", block -> {
            block.spacing(0);
            heading(block, "Profile", profile.title());
            block.addParagraph(p -> p
                    .name("ProfileBody")
                    .text(profile.body())
                    .align(TextAlign.LEFT)
                    .lineSpacing(leading(PROFILE_PITCH, BODY_SIZE))
                    .textStyle(style(BODY_FONT, BODY_SIZE, BODY, false))
                    .margin((float) PROFILE_RULE_TO_BODY, 0f, 0f, 0f));
        });
    }

    /**
     * The roles held — a title/date line, an accent company line, and bullets.
     *
     * <p>The title line is a two-column table with the date anchored top right,
     * so the date lands on the column's right edge whatever its length. The
     * bullets are a list: unlike the aside's, these markers are the same
     * charcoal as their text, so the marker colour is not fighting the
     * style.</p>
     */
    private static void renderExperience(SectionBuilder main, EntriesSection experience) {
        main.addSection("Experience", block -> {
            block.spacing(0);
            heading(block, "Experience", experience.title());
            List<CvEntry> entries = experience.entries();
            double dateWidth = MAIN_WIDTH * DATE_COLUMN_RATIO;
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                int index = i;
                boolean first = i == 0;
                block.addTable(table -> {
                    table.name("JobHead" + index);
                    table.width(MAIN_WIDTH);
                    table.margin(new DocumentInsets(first ? EXPERIENCE_RULE_TO_BODY : JOB_GAP, 0,
                            JOB_TITLE_TO_COMPANY, 0));
                    table.columns(
                            DocumentTableColumn.fixed(MAIN_WIDTH - dateWidth),
                            DocumentTableColumn.fixed(dateWidth));
                    table.defaultCellStyle(cellStyle(DocumentInsets.zero(),
                            DocumentTableTextAnchor.TOP_LEFT));
                    ParagraphBuilder title = new ParagraphBuilder()
                            .name("JobTitle" + index)
                            .lineSpacing(0)
                            .textStyle(style(BODY_FONT, JOB_TITLE_SIZE, INK, true));
                    if (entry.link().isBlank()) {
                        title.inlineText(entry.title(),
                                style(BODY_FONT, JOB_TITLE_SIZE, INK, true));
                    } else {
                        title.inlineText(entry.title(),
                                style(BODY_FONT, JOB_TITLE_SIZE, INK, true),
                                new DocumentLinkOptions(entry.link()));
                    }
                    table.rowCells(
                            DocumentTableCell.node(title.build()),
                            DocumentTableCell.node(new ParagraphBuilder()
                                            .name("JobDates" + index)
                                            .text(entry.date())
                                            .align(TextAlign.RIGHT)
                                            .lineSpacing(0)
                                            .textStyle(style(BODY_FONT, JOB_META_SIZE, MUTED,
                                                    false))
                                            .build())
                                    .withStyle(cellStyle(DocumentInsets.zero(),
                                            DocumentTableTextAnchor.TOP_RIGHT)));
                });
                block.addParagraph(p -> p
                        .name("JobCompany" + index)
                        .text(companyLine(entry))
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, JOB_META_SIZE, ACCENT, false))
                        .margin(0f, 0f, (float) COMPANY_TO_BULLETS, 0f));
                block.addList(list -> {
                    list.name("JobBullets" + index);
                    list.items(OrangeOpsWidgets.lines(entry.body()));
                    list.marker(ListMarker.bullet());
                    list.textStyle(style(BODY_FONT, BODY_SIZE, BODY, false));
                    list.lineSpacing(leading(BULLET_PITCH, BODY_SIZE));
                    list.itemSpacing(gap(BULLET_ITEM_PITCH, BULLET_PITCH));
                });
            }
        });
    }

    /** The employer, and where the role was held when the entry says so. */
    private static String companyLine(CvEntry entry) {
        if (entry.place().isBlank()) {
            return entry.subtitle();
        }
        return entry.subtitle().isBlank()
                ? entry.place()
                : entry.subtitle() + " | " + entry.place();
    }

    /**
     * The metric strip — a mark, a number and its caption lines, per metric,
     * with hairlines between them.
     *
     * <p>One table of {@code 2n-1} columns: the metric columns interleaved with
     * hairline-wide columns whose cell style is filled with the rule colour.
     * That is how the separators run the block's full height — a table's rules
     * are drawn per cell with no per-edge control, so a stroke would box all
     * four edges of a metric cell instead.</p>
     *
     * <p>A row per line — mark, number, then one row per caption line — rather
     * than one composite node per metric, so the metrics sit on a shared
     * baseline grid however long their captions are. A metric with fewer
     * caption lines than its neighbours leaves its lower rows empty.</p>
     */
    private static void renderMetrics(SectionBuilder main, EntriesSection metrics) {
        main.addSection("Metrics", block -> {
            block.spacing(0);
            heading(block, "Metrics", metrics.title());
            List<CvEntry> entries = metrics.entries();
            List<List<String>> captions = new ArrayList<>();
            int captionRows = 0;
            for (CvEntry entry : entries) {
                List<String> lines = OrangeOpsWidgets.lines(entry.body());
                captions.add(lines);
                captionRows = Math.max(captionRows, lines.size());
            }
            int rows = captionRows;
            int separators = entries.size() - 1;
            double metricWidth = (MAIN_WIDTH - separators * METRIC_SEPARATOR) / entries.size();
            block.addTable(table -> {
                table.name("MetricTable");
                table.width(MAIN_WIDTH);
                table.margin(new DocumentInsets(METRIC_RULE_TO_BODY, 0, 0, 0));
                DocumentTableColumn[] columns = new DocumentTableColumn[2 * entries.size() - 1];
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = (i % 2 == 0)
                            ? DocumentTableColumn.fixed(metricWidth)
                            : DocumentTableColumn.fixed(METRIC_SEPARATOR);
                }
                table.columns(columns);
                table.defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.CENTER));

                table.rowCells(metricRow(entries.size(),
                        i -> markCell(entries.get(i).icon(), i)));
                table.rowStyle(0, cellStyle(new DocumentInsets(0, 0, METRIC_ICON_TO_VALUE, 0),
                        DocumentTableTextAnchor.CENTER));
                table.rowCells(metricRow(entries.size(), i -> centredCell("MetricValue" + i,
                        entries.get(i).title(), METRIC_VALUE_SIZE, ACCENT, true)));
                table.rowStyle(1, cellStyle(new DocumentInsets(0, 0, METRIC_VALUE_TO_CAPTION, 0),
                        DocumentTableTextAnchor.CENTER));
                for (int row = 0; row < rows; row++) {
                    int line = row;
                    table.rowCells(metricRow(entries.size(), i -> {
                        List<String> lines = captions.get(i);
                        String text = line < lines.size() ? lines.get(line) : "";
                        return centredCell("MetricCaption" + line + "_" + i, text,
                                METRIC_CAPTION_SIZE, BODY, false);
                    }));
                }
            });
        });
    }

    /** One metric row: metric cells interleaved with the hairline cells. */
    private static DocumentTableCell[] metricRow(int metrics,
                                                 IntFunction<DocumentTableCell> cell) {
        DocumentTableCell[] cells = new DocumentTableCell[2 * metrics - 1];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = (i % 2 == 0)
                    ? cell.apply(i / 2)
                    : DocumentTableCell.node(new SpacerBuilder()
                                    .name("MetricSeparator" + i)
                                    .width(METRIC_SEPARATOR)
                                    .build())
                            .withStyle(DocumentTableStyle.builder()
                                    .padding(DocumentInsets.zero())
                                    .fillColor(RULE)
                                    .textAnchor(DocumentTableTextAnchor.CENTER)
                                    .stroke(NO_STROKE)
                                    .build());
        }
        return cells;
    }

    private static DocumentTableCell markCell(String token, int index) {
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name("MetricMark" + index);
        paragraph.align(TextAlign.CENTER);
        paragraph.lineSpacing(0);
        if (token.isBlank()) {
            paragraph.textStyle(style(BODY_FONT, METRIC_CAPTION_SIZE, BODY, false));
        } else {
            paragraph.textStyle(style(BODY_FONT,
                    OrangeOpsWidgets.markLineSize(token, METRIC_CAPTION_SIZE), BODY, false));
            OrangeOpsWidgets.mark(paragraph, token);
        }
        return DocumentTableCell.node(paragraph.build());
    }

    private static DocumentTableCell centredCell(String name, String text, double size,
                                                 DocumentColor color, boolean bold) {
        return DocumentTableCell.node(new ParagraphBuilder()
                .name(name)
                .text(text)
                .align(TextAlign.CENTER)
                .lineSpacing(0)
                .textStyle(style(BODY_FONT, size, color, bold))
                .build());
    }

    /** The closing lines — an inline mark, a bold label and its value, per line. */
    private static void renderAdditional(SectionBuilder main, EntriesSection additional) {
        main.addSection("Additional", block -> {
            // spacing(0), not the item pitch: a main heading is two nodes — the
            // label and its accent rule — and a section spacing would open the
            // gap between them as well.
            double itemGap = gap(ADDITIONAL_PITCH, LATO.lineBox(BODY_SIZE));
            block.spacing(0);
            heading(block, "Additional", additional.title());
            List<CvEntry> entries = additional.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                int index = i;
                boolean first = i == 0;
                block.addParagraph(p -> {
                    p.name("Additional" + index);
                    p.lineSpacing(0);
                    if (entry.icon().isBlank()) {
                        p.textStyle(style(BODY_FONT, BODY_SIZE, BODY, false));
                    } else {
                        p.textStyle(style(BODY_FONT,
                                OrangeOpsWidgets.markLineSize(entry.icon(), BODY_SIZE),
                                BODY, false));
                        OrangeOpsWidgets.mark(p, entry.icon());
                        p.inlineText("   ", style(BODY_FONT, BODY_SIZE, BODY, false));
                    }
                    p.inlineText(entry.title(), style(BODY_FONT, BODY_SIZE, INK, true));
                    if (!entry.body().isBlank()) {
                        p.inlineText(" ", style(BODY_FONT, BODY_SIZE, BODY, false));
                        p.inlineText(entry.body(), style(BODY_FONT, BODY_SIZE, BODY, false));
                    }
                    p.margin((float) (first ? ADDITIONAL_RULE_TO_BODY : itemGap), 0f, 0f, 0f);
                });
            }
        });
    }

    /** A berth's title, with any parenthetical set as the smaller suffix. */
    private static void heading(SectionBuilder block, String name, String title) {
        int bracket = title.indexOf('(');
        if (bracket < 0) {
            OrangeOpsWidgets.mainHeading(block, name, title, null);
            return;
        }
        OrangeOpsWidgets.mainHeading(block, name, title.substring(0, bracket).stripTrailing(),
                title.substring(bracket));
    }
}
