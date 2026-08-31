package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_TOP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_LEFT_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_LINE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_RIGHT_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_RULE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_TABLE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.DEGREE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.EDUCATION_LINE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.EDUCATION_LINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.EMPLOYER_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.EMPLOYER_TO_HIGHLIGHTS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ENTRY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ENTRY_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.EXPERIENCE_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HALF_GAP;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HIGHLIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HIGHLIGHT_PITCH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HIGHLIGHT_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.NO_BORDER;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE_MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PERIOD_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PERIOD_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PROFILE_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PROFILE_TO_EXPERIENCE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ROLE_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ROLE_TO_EMPLOYER;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_TO_CREDENTIALS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_TO_MAIN_BODY;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.TABLE_SLACK;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.italic;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.heading;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.spacer;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeWidgets.text;

/**
 * The wide column: the profile, the roles on a rail, and the credentials
 * footer.
 */
final class SlateOrangeMain {

    private SlateOrangeMain() {
    }

    static void compose(SectionBuilder main, ParagraphSection profile,
                        EntriesSection experience, EntriesSection education,
                        EntriesSection certifications) {
        main.spacing(0);
        main.padding((float) BODY_TOP, (float) PAGE_MARGIN, (float) PAGE_MARGIN,
                (float) HALF_GAP);
        if (profile != null && !profile.body().isBlank()) {
            renderProfile(main, profile);
        }
        if (hasEntries(experience)) {
            renderExperience(main, experience);
        }
        if (hasEntries(education) || hasEntries(certifications)) {
            renderCredentials(main, education, certifications);
        }
    }

    /** The profile — one paragraph at the design's measured leading. */
    private static void renderProfile(SectionBuilder main, ParagraphSection profile) {
        main.addSection("Profile", block -> {
            block.spacing(0);
            heading(block, "Profile", profile.title());
            block.addParagraph(p -> p
                    .name("ProfileBody")
                    .text(profile.body().replace(String.valueOf((char) 10), " "))
                    .lineSpacing(leading(PROFILE_LEADING, BODY_SIZE))
                    .textStyle(style(BODY_FONT, BODY_SIZE, INK, false))
                    .margin((float) RULE_TO_MAIN_BODY, 0f, 0f, 0f));
        });
    }

    // -- experience --------------------------------------------------------

    /**
     * The roles held, on one rail.
     *
     * <p>The rail is the left border of each entry's own section, so it
     * stretches to whatever height that entry turns out to have and
     * consecutive entries butt together into one unbroken line. The last entry
     * does not carry it: the design's rail stops at the final marker rather
     * than running past the final bullet, and the entry list is what decides
     * which entry that is.</p>
     *
     * <p>The marker is a disc positioned on the rail by a layer stack, nudged
     * left by the entry's own indent plus half the disc so its centre lands on
     * the border rather than beside it. Both terms are geometry the sheet
     * already holds, not offsets tuned by eye.</p>
     */
    private static void renderExperience(SectionBuilder main, EntriesSection experience) {
        main.addSection("Experience", block -> {
            block.spacing(0);
            block.margin((float) PROFILE_TO_EXPERIENCE, 0f, 0f, 0f);
            block.keepTogether();
            heading(block, "Experience", experience.title());
            block.addSection("ExperienceEntries", host -> {
                host.spacing(0);
                host.margin((float) RULE_TO_MAIN_BODY, 0f, 0f, 0f);
                List<CvEntry> entries = experience.entries();
                for (int i = 0; i < entries.size(); i++) {
                    CvEntry entry = entries.get(i);
                    boolean last = i == entries.size() - 1;
                    int index = i;
                    host.addSection("ExperienceEntry_" + index, body -> {
                        body.spacing(0);
                        if (!last) {
                            body.accentLeft(RULE, RULE_THICKNESS);
                        }
                        body.padding(0f, 0f, last ? 0f : (float) ENTRY_GAP,
                                (float) ENTRY_INDENT);
                        renderEntry(body, entry, index);
                    });
                }
            });
        });
    }

    /** One entry: the role and its dates, the employer line, then the bullets. */
    private static void renderEntry(SectionBuilder body, CvEntry entry, int index) {
        body.addLayerStack(stack -> stack
                .name("RoleLine_" + index)
                .layer(roleLine(entry, index), LayerAlign.TOP_LEFT, 0)
                .position(marker(index),
                        -(ENTRY_INDENT + MARKER_DIAMETER / 2.0), 0.0,
                        LayerAlign.TOP_LEFT, 1));
        body.addParagraph(p -> p
                .name("Employer_" + index)
                .text(entry.subtitle())
                .lineSpacing(0)
                .textStyle(italic(BODY_FONT, EMPLOYER_SIZE, INK))
                .margin((float) ROLE_TO_EMPLOYER, 0f, (float) EMPLOYER_TO_HIGHLIGHTS, 0f));
        List<String> highlights = lines(entry.body());
        if (highlights.isEmpty()) {
            return;
        }
        body.addList(list -> list
                .name("Highlights_" + index)
                .items(highlights)
                .marker(ListMarker.bullet())
                .textStyle(style(BODY_FONT, HIGHLIGHT_SIZE, INK, false))
                .itemSpacing(gap(HIGHLIGHT_PITCH, HIGHLIGHT_SIZE * LINE_FACTOR))
                .lineSpacing(leading(HIGHLIGHT_LEADING, HIGHLIGHT_SIZE)));
    }

    /** The role and its dates, the dates flush right on the same line. */
    private static DocumentNode roleLine(CvEntry entry, int index) {
        ParagraphBuilder role = new ParagraphBuilder()
                .name("Role_" + index)
                .text(entry.title())
                .lineSpacing(0)
                .textStyle(style(BODY_FONT, ROLE_SIZE, INK, true));
        if (!entry.link().isBlank()) {
            role.link(new DocumentLinkOptions(entry.link()));
        }
        return new TableBuilder()
                .name("RoleTable_" + index)
                .width(ENTRY_WIDTH)
                .columns(
                        DocumentTableColumn.fixed(ROLE_COLUMN),
                        DocumentTableColumn.fixed(PERIOD_COLUMN))
                .defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.BOTTOM_LEFT))
                .rowCells(
                        DocumentTableCell.node(role.build()),
                        DocumentTableCell.node(new ParagraphBuilder()
                                .name("Period_" + index)
                                .text(entry.date())
                                .align(TextAlign.RIGHT)
                                .lineSpacing(0)
                                .textStyle(style(BODY_FONT, PERIOD_SIZE, INK, false))
                                .build()))
                .build();
    }

    /** The disc that caps the rail beside a role line. */
    private static DocumentNode marker(int index) {
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name("Marker_" + index);
        paragraph.lineSpacing(0);
        paragraph.textStyle(style(BODY_FONT, MARKER_DIAMETER, ACCENT, false));
        paragraph.dot(MARKER_DIAMETER, ACCENT);
        return paragraph.build();
    }

    // -- credentials -------------------------------------------------------

    /**
     * The credentials footer: a rule across the column, then education and
     * certifications side by side.
     *
     * <p>One table, three rows. The first is the two headings; the second is
     * their orange rules, drawn as cells filled to half a point of height,
     * because a cell's stroke is a box with no per-edge control and a line
     * node in a cell is not drawn at all. The third holds each column's
     * content: a single-column table on the left, because a cell holds one
     * node and a degree is three stacked lines, and the certifications list on
     * the right, which is one list node and needs no wrapper.</p>
     */
    private static void renderCredentials(SectionBuilder main, EntriesSection education,
                                          EntriesSection certifications) {
        main.addSection("Credentials", block -> {
            block.spacing(0);
            block.margin((float) EXPERIENCE_TO_RULE, 0f, 0f, 0f);
            block.addLine(line -> line
                    .name("CredentialsRule")
                    .fill()
                    .thickness(RULE_THICKNESS)
                    .color(RULE));
            block.addTable(table -> {
                table.name("CredentialTable");
                table.margin(new DocumentInsets(RULE_TO_CREDENTIALS, 0, 0, 0));
                table.width(CREDENTIAL_TABLE_WIDTH);
                table.columns(
                        DocumentTableColumn.fixed(CREDENTIAL_LEFT_COLUMN),
                        DocumentTableColumn.fixed(CREDENTIAL_GUTTER),
                        DocumentTableColumn.fixed(CREDENTIAL_RIGHT_COLUMN));
                table.defaultCellStyle(cellStyle(DocumentInsets.zero(),
                        DocumentTableTextAnchor.TOP_LEFT));
                table.rowCells(
                        DocumentTableCell.node(text("EducationHeading",
                                education == null ? "" : education.title(),
                                style(DISPLAY_FONT, CREDENTIAL_HEADING_SIZE, INK, true))),
                        DocumentTableCell.node(spacer("CredentialGutterHeading")),
                        DocumentTableCell.node(text("CertificationsHeading",
                                certifications == null ? "" : certifications.title(),
                                style(DISPLAY_FONT, CREDENTIAL_HEADING_SIZE, INK, true))));
                table.rowCells(
                        credentialRule("EducationHeadingRule"),
                        DocumentTableCell.node(spacer("CredentialGutterRule")),
                        credentialRule("CertificationsHeadingRule"));
                table.rowCells(
                        DocumentTableCell.node(educationLines(education)),
                        DocumentTableCell.node(spacer("CredentialGutterBody")),
                        DocumentTableCell.node(certificationList(certifications)));
                table.rowStyle(0, cellStyle(
                        new DocumentInsets(0, 0, CREDENTIAL_HEADING_TO_RULE, 0),
                        DocumentTableTextAnchor.TOP_LEFT));
                table.rowStyle(2, cellStyle(
                        new DocumentInsets(CREDENTIAL_RULE_TO_BODY, 0, 0, 0),
                        DocumentTableTextAnchor.TOP_LEFT));
            });
        });
    }

    /** A heading rule, as a cell filled to a hairline of height. */
    private static DocumentTableCell credentialRule(String name) {
        return DocumentTableCell
                .node(new ParagraphBuilder()
                        .name(name)
                        .text(" ")
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, CREDENTIAL_RULE_SIZE, ACCENT, false))
                        .build())
                .withStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.zero())
                        .fillColor(ACCENT)
                        .stroke(NO_BORDER)
                        .build());
    }

    /**
     * Each degree as three stacked lines: the award, the institution and
     * whatever the entry says about when.
     */
    private static DocumentNode educationLines(EntriesSection education) {
        double width = CREDENTIAL_LEFT_COLUMN - TABLE_SLACK;
        TableBuilder table = new TableBuilder()
                .name("EducationLines")
                .width(width)
                .columns(DocumentTableColumn.fixed(width))
                .defaultCellStyle(cellStyle(
                        new DocumentInsets(0, 0,
                                gap(EDUCATION_LINE_PITCH, EDUCATION_LINE_SIZE * LINE_FACTOR), 0),
                        DocumentTableTextAnchor.TOP_LEFT));
        if (education == null) {
            return table.rowCells(DocumentTableCell.node(spacer("EducationEmpty"))).build();
        }
        List<CvEntry> entries = education.entries();
        for (int index = 0; index < entries.size(); index++) {
            CvEntry entry = entries.get(index);
            ParagraphBuilder degree = new ParagraphBuilder()
                    .name("Degree_" + index)
                    .text(entry.title())
                    .lineSpacing(0)
                    .textStyle(style(BODY_FONT, DEGREE_SIZE, INK, true));
            if (!entry.link().isBlank()) {
                degree.link(new DocumentLinkOptions(entry.link()));
            }
            table.rowCells(DocumentTableCell.node(degree.build()));
            table.rowCells(DocumentTableCell.node(new ParagraphBuilder()
                    .name("Institution_" + index)
                    .text(entry.subtitle())
                    .lineSpacing(0)
                    .textStyle(italic(BODY_FONT, EDUCATION_LINE_SIZE, INK))
                    .build()));
            table.rowCells(DocumentTableCell.node(text("EducationDetail_" + index,
                    detail(entry), style(BODY_FONT, EDUCATION_LINE_SIZE, INK, false))));
        }
        return table.build();
    }

    /**
     * The line under an institution: what the entry says about where and when,
     * joined the way the design joins them.
     */
    private static String detail(CvEntry entry) {
        StringBuilder detail = new StringBuilder();
        if (!entry.place().isBlank()) {
            detail.append(entry.place());
        }
        if (!entry.date().isBlank()) {
            detail.append(detail.isEmpty() ? "" : "  " + (char) 0x2022 + "  ").append(entry.date());
        }
        return detail.toString();
    }

    /** The certifications, as one bulleted list. */
    private static DocumentNode certificationList(EntriesSection certifications) {
        List<String> items = new ArrayList<>();
        if (certifications != null) {
            for (CvEntry entry : certifications.entries()) {
                items.add(entry.title());
            }
        }
        if (items.isEmpty()) {
            return spacer("CertificationsEmpty");
        }
        return new ListBuilder()
                .name("CertificationItems")
                .items(items)
                .marker(ListMarker.bullet())
                .textStyle(style(BODY_FONT, CREDENTIAL_SIZE, INK, false))
                .itemSpacing(gap(CREDENTIAL_LINE_PITCH, CREDENTIAL_SIZE * LINE_FACTOR))
                .build();
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

    private static boolean hasEntries(EntriesSection section) {
        return section != null && !section.entries().isEmpty();
    }
}
