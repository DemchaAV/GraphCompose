package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT_RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BADGE_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BADGE_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CREDENTIALS_TO_QUOTE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.CREDENTIAL_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DEGREE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DEGREE_TO_INSTITUTION;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EDUCATION_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EDUCATION_RULE_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EDUCATION_TEXT_TOP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EDU_DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.EXPERIENCE_TO_PROJECTS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_RULE_OFFSET;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_EDUCATION;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_PROJECTS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TRACKING;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.INSTITUTION_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.INSTITUTION_TO_DETAIL;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGES_BODY_OFFSET;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGES_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGES_RULE_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGE_LEVEL_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGE_NAME_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGE_RATING_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LANGUAGE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LEVEL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PIPE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECTS_TO_CREDENTIALS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_BODY_PITCH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_COPY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_COPY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_SUB_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_TITLE_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_YEAR_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PROJECT_YEAR_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_LINE_BOX;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_PAD_H;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_PAD_V;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_RADIUS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.QUOTE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RATING_DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RATING_EMPTY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RATING_GAP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RATING_SCALE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TABLE_SLACK;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.THUMB_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.THUMB_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.THUMB_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.THUMB_RADIUS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.THUMB_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TINT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.TITLE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.style;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.headingRow;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.inlineIcon;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.text;
import static com.demcha.compose.document.templates.cv.presets.VioletGridWidgets.tracked;

/**
 * What closes the sheet: the projects, the credentials band and the quote.
 */
final class VioletGridClosing {

    private VioletGridClosing() {
    }

    // -- the projects ------------------------------------------------------

    /** One top-level row per project: the tile, then the copy beside it. */
    static void renderProjects(PageFlowBuilder page, EntriesSection projects) {
        if (projects == null || projects.entries().isEmpty()) {
            return;
        }
        page.add(headingRow("Projects", projects.title(), EXPERIENCE_TO_PROJECTS));
        List<CvEntry> entries = projects.entries();
        for (int i = 0; i < entries.size(); i++) {
            CvEntry entry = entries.get(i);
            int index = i;
            double top = i == 0 ? HEADING_TO_PROJECTS : PROJECT_GAP;
            page.addRow("Project_" + index, row -> {
                row.spacing(0);
                row.margin(new DocumentInsets(top, 0, 0, 0));
                row.columns(
                        DocumentRowColumn.fixed(THUMB_COLUMN),
                        DocumentRowColumn.weight(1.0));
                row.addSection("ProjectThumb_" + index,
                        cell -> renderThumb(cell, entry, index));
                row.addSection("ProjectCopy_" + index,
                        cell -> renderProjectCopy(cell, entry, index));
            });
        }
    }

    /**
     * The tinted tile and the glyph it owns — a shape container rather than a
     * fill with a mark beside it: the tile IS the thumbnail, and centring the
     * glyph in the container is what keeps the two together when either size
     * changes.
     */
    private static void renderThumb(SectionBuilder cell, CvEntry entry, int index) {
        cell.spacing(0);
        cell.padding(0f, 0f, 0f, (float) THUMB_PAD_LEFT);
        if (entry.icon().isBlank()) {
            return;
        }
        SectionBuilder glyph = new SectionBuilder();
        glyph.name("ProjectGlyph_" + index);
        glyph.addSvgIcon(VioletGridIcons.icon(entry.icon()), VioletGridIcons.size(entry.icon()));
        cell.addContainer(tile -> tile
                .name("ProjectTile_" + index)
                .roundedRect(THUMB_WIDTH, THUMB_HEIGHT, THUMB_RADIUS)
                .fillColor(TINT)
                .center(glyph.build()));
    }

    /**
     * The copy beside a thumbnail. The hairline is this section's own left
     * border, so it runs the copy's height — which is what the design shows,
     * and what a line with a measured height would get wrong the first time
     * the copy ran to three lines.
     */
    private static void renderProjectCopy(SectionBuilder cell, CvEntry entry, int index) {
        DocumentTextStyle title = style(BODY_FONT, PROJECT_TITLE_SIZE, INK, true);
        DocumentTextStyle subtitle = style(BODY_FONT, PROJECT_SUB_SIZE, MUTED, false);
        cell.spacing(0);
        cell.accentLeft(RULE, PROJECT_RULE_THICKNESS);
        cell.padding(0f, 0f, 0f, (float) PROJECT_COPY_INDENT);
        ParagraphBuilder name = new ParagraphBuilder()
                .name("ProjectName_" + index)
                .lineSpacing(0)
                .textStyle(title)
                .inlineText(entry.title(), title);
        if (!entry.subtitle().isBlank()) {
            name.inlineText(PIPE, subtitle).inlineText(entry.subtitle(), subtitle);
        }
        if (!entry.link().isBlank()) {
            name.link(new DocumentLinkOptions(entry.link()));
        }
        cell.addTable(table -> table
                .name("ProjectTitle_" + index)
                .width(PROJECT_COPY_WIDTH)
                .columns(
                        DocumentTableColumn.fixed(PROJECT_TITLE_COLUMN),
                        DocumentTableColumn.fixed(PROJECT_YEAR_COLUMN))
                .defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.TOP_LEFT))
                .rowCells(
                        DocumentTableCell.node(name.build()),
                        DocumentTableCell.node(new ParagraphBuilder()
                                .name("ProjectYear_" + index)
                                .text(entry.date())
                                .align(TextAlign.RIGHT)
                                .lineSpacing(0)
                                .textStyle(style(BODY_FONT, PROJECT_YEAR_SIZE, MUTED, false))
                                .build())));
        cell.addParagraph(p -> p
                .name("ProjectBody_" + index)
                .text(entry.body().replace(String.valueOf((char) 10), " "))
                .lineSpacing(leading(PROJECT_BODY_PITCH, PROJECT_BODY_SIZE))
                .textStyle(style(BODY_FONT, PROJECT_BODY_SIZE, BODY, false))
                .margin((float) TITLE_TO_BODY, 0f, 0f, 0f));
    }

    // -- the credentials band ----------------------------------------------

    /**
     * The credentials band: education and languages side by side.
     *
     * <p>TWO top-level rows rather than one, and that is not a stylistic
     * choice. The obvious shape — one row whose two cells each hold a heading
     * and a body — cannot be built: a heading is a text and a rule side by
     * side, so a cell holding one would need a nested row, which the layout
     * compiler refuses. So the band is flattened: one row carries both headings
     * and both rules as direct children, and a second carries the two bodies.
     * Nothing is nested, and the two rows share the same column arithmetic, so
     * the halves cannot drift apart.</p>
     */
    static void renderCredentials(PageFlowBuilder page, EntriesSection education,
                                  SkillsSection languages) {
        boolean hasEducation = education != null && !education.entries().isEmpty();
        boolean hasLanguages = languages != null
                && languages.groups().stream().anyMatch(group -> !group.entries().isEmpty());
        if (!hasEducation && !hasLanguages) {
            return;
        }
        renderCredentialHeadings(page,
                hasEducation ? education.title() : "",
                hasLanguages ? languages.title() : "");
        page.addRow("CredentialBodies", row -> {
            row.spacing(0);
            row.margin(new DocumentInsets(HEADING_TO_EDUCATION, 0, 0, 0));
            row.columns(
                    DocumentRowColumn.fixed(EDUCATION_COLUMN),
                    DocumentRowColumn.fixed(CREDENTIAL_GUTTER),
                    DocumentRowColumn.weight(1.0));
            row.addSection("Education", cell -> renderEducation(cell, education));
            row.addSection("CredentialGutter", cell -> cell.spacer(CREDENTIAL_GUTTER, 1.0));
            row.addSection("Languages", cell -> renderLanguages(cell, languages));
        });
    }

    /**
     * Both headings and both rules, as children of one row. The two text
     * columns take what their words need and the two weighted ones split what
     * is left in the ratio the design's rules measure, so each rule still
     * begins where its own heading ends.
     */
    private static void renderCredentialHeadings(PageFlowBuilder page, String educationTitle,
                                                 String languagesTitle) {
        page.addRow("CredentialHeadings", row -> {
            row.spacing(0);
            row.margin(new DocumentInsets(PROJECTS_TO_CREDENTIALS, 0, 0, 0));
            row.columns(
                    DocumentRowColumn.auto(),
                    DocumentRowColumn.weight(EDUCATION_RULE_WEIGHT),
                    DocumentRowColumn.fixed(CREDENTIAL_GUTTER),
                    DocumentRowColumn.auto(),
                    DocumentRowColumn.weight(LANGUAGES_RULE_WEIGHT));
            row.add(tracked("EducationHeadingLabel", educationTitle, DISPLAY_FONT,
                    HEADING_SIZE, ACCENT, true, HEADING_TRACKING).build());
            row.addLine(line -> line
                    .name("EducationHeadingRule")
                    .fill()
                    .thickness(HEADING_RULE_THICKNESS)
                    .color(ACCENT_RULE)
                    .margin(new DocumentInsets(HEADING_RULE_OFFSET, 0, 0, HEADING_TO_RULE)));
            row.addSpacer(CREDENTIAL_GUTTER);
            row.add(tracked("LanguagesHeadingLabel", languagesTitle, DISPLAY_FONT,
                    HEADING_SIZE, ACCENT, true, HEADING_TRACKING).build());
            row.addLine(line -> line
                    .name("LanguagesHeadingRule")
                    .fill()
                    .thickness(HEADING_RULE_THICKNESS)
                    .color(ACCENT_RULE)
                    .margin(new DocumentInsets(HEADING_RULE_OFFSET, 0, 0, HEADING_TO_RULE)));
        });
    }

    /**
     * The badge beside the degree, the institution and the detail line.
     *
     * <p>A layer stack rather than a table: the badge is a shape container and
     * the three lines are a stack, and neither survives a table cell here. The
     * lines carry a left margin the width of the badge column, so the two
     * layers do not overlap and the badge needs no offset.</p>
     */
    private static void renderEducation(SectionBuilder cell, EntriesSection education) {
        cell.spacing(0);
        if (education == null || education.entries().isEmpty()) {
            return;
        }
        CvEntry entry = education.entries().get(0);
        cell.addLayerStack(stack -> {
            stack.name("EducationBody");
            stack.layer(educationLines(entry), LayerAlign.TOP_LEFT, 0);
            if (!entry.icon().isBlank()) {
                stack.position(badge(entry), 0.0, 0.0, LayerAlign.TOP_LEFT, 1);
            }
        });
    }

    /** The tinted circle and the glyph it owns. */
    private static DocumentNode badge(CvEntry entry) {
        SectionBuilder glyph = new SectionBuilder();
        glyph.name("EducationGlyph");
        glyph.addSvgIcon(VioletGridIcons.icon(entry.icon()), VioletGridIcons.size(entry.icon()));
        return new ShapeContainerBuilder()
                .name("EducationBadge")
                .circle(BADGE_DIAMETER)
                .fillColor(TINT)
                .center(glyph.build())
                .build();
    }

    /** The degree, the institution and the place-and-years line, as one column. */
    private static DocumentNode educationLines(CvEntry entry) {
        DocumentTextStyle place = style(BODY_FONT, EDU_DETAIL_SIZE, MUTED, false);
        DocumentTextStyle years = style(BODY_FONT, EDU_DETAIL_SIZE, ACCENT, false);
        double width = EDUCATION_COLUMN - BADGE_COLUMN - TABLE_SLACK;
        ParagraphBuilder degree = new ParagraphBuilder()
                .name("Degree")
                .text(entry.title())
                .lineSpacing(0)
                .textStyle(style(BODY_FONT, DEGREE_SIZE, INK, true));
        if (!entry.link().isBlank()) {
            degree.link(new DocumentLinkOptions(entry.link()));
        }
        ParagraphBuilder detail = new ParagraphBuilder()
                .name("EducationDetail")
                .lineSpacing(0)
                .textStyle(place)
                .inlineText(entry.place(), place);
        if (!entry.date().isBlank()) {
            detail.inlineText(PIPE, place).inlineText(entry.date(), years);
        }
        return new TableBuilder()
                .name("EducationLines")
                .margin(new DocumentInsets(EDUCATION_TEXT_TOP, 0, 0, BADGE_COLUMN))
                .width(width)
                .columns(DocumentTableColumn.fixed(width))
                .defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.TOP_LEFT))
                .rowCells(DocumentTableCell.node(degree.build())
                        .withStyle(cellStyle(
                                new DocumentInsets(0, 0, DEGREE_TO_INSTITUTION, 0),
                                DocumentTableTextAnchor.TOP_LEFT)))
                .rowCells(DocumentTableCell.node(text("Institution", entry.subtitle(),
                                style(BODY_FONT, INSTITUTION_SIZE, BODY, false)))
                        .withStyle(cellStyle(
                                new DocumentInsets(0, 0, INSTITUTION_TO_DETAIL, 0),
                                DocumentTableTextAnchor.TOP_LEFT)))
                .rowCells(DocumentTableCell.node(detail.build()))
                .build();
    }

    /**
     * The languages — a name, the level as the document words it, and a
     * five-disc rating per row.
     *
     * <p>The rating paragraph is styled although it carries no words: a
     * paragraph takes its line box from its type whether or not a glyph uses
     * it, and the default box would set the whole row's pitch.</p>
     */
    private static void renderLanguages(SectionBuilder block, SkillsSection languages) {
        block.spacing(0);
        if (languages == null) {
            return;
        }
        List<CvSkill> entries = new ArrayList<>();
        for (SkillGroup group : languages.groups()) {
            entries.addAll(group.entries());
        }
        if (entries.isEmpty()) {
            return;
        }
        block.addTable(table -> {
            table.name("LanguageTable");
            table.margin(new DocumentInsets(LANGUAGES_BODY_OFFSET, 0, 0, 0));
            table.width(LANGUAGES_COLUMN - TABLE_SLACK);
            table.columns(
                    DocumentTableColumn.fixed(LANGUAGE_NAME_COLUMN),
                    DocumentTableColumn.fixed(LANGUAGE_LEVEL_COLUMN),
                    DocumentTableColumn.fixed(LANGUAGE_RATING_COLUMN - TABLE_SLACK));
            table.defaultCellStyle(cellStyle(
                    new DocumentInsets(0, 0,
                            gap(LANGUAGE_PITCH, LANGUAGE_SIZE * LINE_FACTOR), 0),
                    DocumentTableTextAnchor.CENTER_LEFT));
            for (int index = 0; index < entries.size(); index++) {
                CvSkill skill = entries.get(index);
                table.rowCells(
                        DocumentTableCell.node(text("LanguageName_" + index, skill.name(),
                                style(BODY_FONT, LANGUAGE_SIZE, BODY, false))),
                        DocumentTableCell.node(text("LanguageLevel_" + index, skill.note(),
                                style(BODY_FONT, LEVEL_SIZE, MUTED, false))),
                        DocumentTableCell.node(rating("LanguageRating_" + index, skill)));
            }
            table.rowStyle(entries.size() - 1,
                    cellStyle(DocumentInsets.zero(), DocumentTableTextAnchor.CENTER_LEFT));
        });
    }

    /** One language's discs, filled up to its rating and grey after it. */
    private static DocumentNode rating(String name, CvSkill skill) {
        DocumentTextStyle box = style(BODY_FONT, LANGUAGE_SIZE, BODY, false);
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name(name);
        paragraph.lineSpacing(0);
        paragraph.textStyle(box);
        if (skill.level().isEmpty()) {
            return paragraph.build();
        }
        int filled = (int) Math.round(skill.level().getAsDouble() * RATING_SCALE);
        for (int index = 0; index < RATING_SCALE; index++) {
            paragraph.dot(RATING_DOT_DIAMETER, index < filled ? ACCENT : RATING_EMPTY);
            if (index < RATING_SCALE - 1) {
                paragraph.inlineText(RATING_GAP, box);
            }
        }
        return paragraph.build();
    }

    // -- the closing band --------------------------------------------------

    /**
     * The closing band.
     *
     * <p>A fill with a corner radius and its own padding rather than a soft
     * panel, whose padding is a single uniform value: this band's side padding
     * is several times its vertical padding.</p>
     *
     * <p>The quotation mark is a packaged glyph rather than a typographic one.
     * At the size the design draws it a real quotation mark would need about
     * forty points of type, and its line box alone would be nearly twice the
     * band's height.</p>
     */
    static void renderQuote(PageFlowBuilder page, ParagraphSection quote) {
        if (quote == null || quote.body().isBlank()) {
            return;
        }
        DocumentTextStyle line = style(BODY_FONT, QUOTE_SIZE, BODY, false);
        DocumentTextStyle box = style(BODY_FONT, QUOTE_LINE_BOX / LINE_FACTOR, BODY, false);
        page.addSection("QuoteBand", band -> {
            band.spacing(0);
            band.margin((float) CREDENTIALS_TO_QUOTE, 0f, 0f, 0f);
            band.fillColor(TINT);
            band.cornerRadius(QUOTE_RADIUS);
            band.padding((float) QUOTE_PAD_V, (float) QUOTE_PAD_H,
                    (float) QUOTE_PAD_V, (float) QUOTE_PAD_H);
            band.addParagraph(p -> {
                p.name("QuoteLine");
                p.lineSpacing(0);
                p.textStyle(box);
                inlineIcon(p, VioletGridIcons.QUOTE);
                p.inlineText(QUOTE_GAP, line);
                p.inlineText(quote.body().replace(String.valueOf((char) 10), " "), line);
            });
        });
    }
}
