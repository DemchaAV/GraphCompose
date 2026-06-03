package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.EntryCompactRenderer;
import com.demcha.compose.document.templates.cv.v2.components.LabelValueRenderer;
import com.demcha.compose.document.templates.cv.v2.components.ProjectRenderer;
import com.demcha.compose.document.templates.cv.v2.components.RichParagraphRenderer;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.cv.v2.components.SkillTableRenderer;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.FlowSectionHeader;
import com.demcha.compose.document.templates.cv.v2.widgets.Masthead;
import com.demcha.compose.document.templates.widgets.TableWidget;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * v2 port of the "Editorial Blue" CV preset.
 *
 * <p>Visual signature:</p>
 * <ul>
 *   <li>centred navy uppercase masthead with optional job-title
 *       subtitle;</li>
 *   <li>compact centred contact metadata plus blue underlined
 *       profile links;</li>
 *   <li>uppercase blue section labels with thin editorial rules;</li>
 *   <li>dense prose, inline editorial timeline entries, and a
 *       four-column skills table fed by {@link SkillsSection}.</li>
 * </ul>
 *
 * <p>The preset owns the custom entry/project/skills shapes locally
 * because they are editorial-specific. Shared widgets and renderers
 * are extended only for reusable concepts such as the uppercase
 * headline variant.</p>
 */
public final class EditorialBlue {

    /** Stable template identifier. */
    public static final String ID = "editorial-blue";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Editorial Blue";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final DocumentColor NAME_COLOR =
            DocumentColor.rgb(18, 31, 72);
    private static final int SKILL_COLUMNS = 4;

    private EditorialBlue() {
    }

    /**
     * Builds the preset with its Editorial Blue theme.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.editorialBlue());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;

        Template(CvTheme theme) {
            this.theme = theme;
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double width = document.canvas().innerWidth();
            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2EditorialBlueRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            pageFlow.addSection("CvV2EditorialBlueHeader", section ->
                    Masthead.centered(section, doc.identity(), theme,
                            mastheadStyle()));

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN).stream()
                    .filter(SectionLookup::hasContent)
                    .toList();
            for (int i = 0; i < sections.size(); i++) {
                CvSection section = sections.get(i);
                String name = "CvV2EditorialBlue_" + i;
                FlowSectionHeader.label(pageFlow, name + "_Title",
                        displayTitle(section.title()), width, theme,
                        sectionTitleStyle(), new DocumentInsets(8, 0, 0, 0),
                        new DocumentInsets(7, 0, 5, 0),
                        DocumentInsets.zero(), true);
                pageFlow.addSection(name + "_Body",
                        host -> renderSectionBody(host, section, width));
            }

            addFooter(pageFlow, width);
            pageFlow.build();
        }

        private void renderSectionBody(SectionBuilder section, CvSection cvSection,
                                       double width) {
            section.spacing(theme.spacing().sectionBodySpacing())
                    .padding(theme.spacing().sectionBodyPadding());

            if (cvSection instanceof ParagraphSection paragraph) {
                renderParagraph(section, paragraph.body(), 1.6);
            } else if (cvSection instanceof SkillsSection skills) {
                renderSkills(section, skills.groups(), width);
            } else if (cvSection instanceof EntriesSection entries) {
                renderEntries(section, entries);
            } else if (cvSection instanceof RowsSection rows) {
                renderRows(section, rows);
            }
        }

        private void renderEntries(SectionBuilder section, EntriesSection entries) {
            String normalized = SectionLookup.normalize(entries.title());
            boolean education = normalized.contains("education")
                    || normalized.contains("certification");
            for (int i = 0; i < entries.entries().size(); i++) {
                if (i > 0) {
                    section.spacer(0, theme.spacing().entrySeparation());
                }
                if (education) {
                    renderEducationEntry(section, entries.entries().get(i));
                } else {
                    renderExperienceEntry(section, entries.entries().get(i));
                }
            }
        }

        private void renderExperienceEntry(SectionBuilder section, CvEntry entry) {
            DocumentTextStyle titleStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    11.0, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle dateStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    11.0, DocumentTextDecoration.BOLD, theme.palette().rule());
            DocumentTextStyle subtitleStyle = CvTextStyles.of(FontName.HELVETICA,
                    9.4, DocumentTextDecoration.ITALIC, theme.palette().ink());

            EntryCompactRenderer.titleDateBody(section, entry, titleStyle,
                    dateStyle, subtitleStyle, theme.bodyStyle(), " ",
                    1.0, DocumentInsets.top(1), DocumentInsets.zero(),
                    DocumentInsets.top(1), 1.5, false);
        }

        private void renderEducationEntry(SectionBuilder section, CvEntry entry) {
            DocumentTextStyle titleStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    10.6, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle dateStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    10.0, DocumentTextDecoration.BOLD, theme.palette().rule());
            DocumentTextStyle subtitleStyle = CvTextStyles.of(FontName.HELVETICA,
                    9.2, DocumentTextDecoration.ITALIC, theme.palette().ink());

            EntryCompactRenderer.titleDateBody(section, entry, titleStyle,
                    dateStyle, subtitleStyle, theme.bodyStyle(), " ",
                    1.0, DocumentInsets.top(1), DocumentInsets.zero(),
                    DocumentInsets.top(1), 1.4, false);
        }

        private void renderRows(SectionBuilder section, RowsSection rows) {
            if (SectionLookup.titleContains(rows.title(), "project")) {
                for (int i = 0; i < rows.rows().size(); i++) {
                    if (i > 0) {
                        section.spacer(0, theme.spacing().entrySeparation());
                    }
                    renderProject(section, rows.rows().get(i));
                }
                return;
            }

            for (CvRow row : rows.rows()) {
                renderKeyValue(section, row);
            }
        }

        private void renderProject(SectionBuilder section, CvRow row) {
            DocumentTextStyle titleStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    10.6, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle stackStyle = CvTextStyles.of(FontName.HELVETICA,
                    9.3, DocumentTextDecoration.ITALIC, theme.palette().rule());
            ProjectRenderer.titleThenBody(section, row, titleStyle, stackStyle,
                    theme.bodyStyle(), 1.45, DocumentInsets.top(1),
                    DocumentInsets.top(1));
        }

        private void renderKeyValue(SectionBuilder section, CvRow row) {
            DocumentTextStyle keyStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.BOLD, NAME_COLOR);
            LabelValueRenderer.render(section, row.label(), row.body(),
                    keyStyle, theme.bodyStyle(), 1.4, DocumentInsets.top(1));
        }

        private void renderParagraph(SectionBuilder section, String text,
                                     double lineSpacing) {
            String value = text == null ? "" : text.trim();
            if (value.isBlank()) {
                return;
            }
            RichParagraphRenderer.render(section, value, theme.bodyStyle(),
                    lineSpacing, DocumentInsets.top(1));
        }

        private void renderSkills(SectionBuilder section, List<SkillGroup> groups,
                                  double width) {
            if (groups.isEmpty()) {
                return;
            }
            DocumentTextStyle cellStyle = CvTextStyles.of(FontName.HELVETICA,
                    8.6, DocumentTextDecoration.DEFAULT, theme.palette().ink());
            TableWidget.Style tableStyle = TableWidget.Style.builder()
                    .name("CvV2EditorialBlueSkillsTable")
                    .columns(SKILL_COLUMNS)
                    .cellPadding(new DocumentInsets(4, 6, 4, 6))
                    .border(theme.palette().banner(), 0.5)
                    .textStyle(cellStyle)
                    .widthAdjustment(1.0)
                    .build();

            SkillTableRenderer.grid(section, groups, width, tableStyle, "• ");
        }

        private void addFooter(PageFlowBuilder pageFlow, double width) {
            pageFlow.addLine(line -> line
                    .name("CvV2EditorialBlueFooterRule")
                    .horizontal(width)
                    .color(theme.palette().banner())
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(6, 0, 0, 0)));
            pageFlow.addSection("CvV2EditorialBlueFooter", section -> section
                    .padding(new DocumentInsets(2, 0, 0, 0))
                    .addParagraph(paragraph -> paragraph
                            .text("References available upon request.")
                            .textStyle(CvTextStyles.of(FontName.HELVETICA,
                                    8.4, DocumentTextDecoration.ITALIC,
                                    theme.palette().muted()))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(2))));
        }

        private String displayTitle(String title) {
            String normalized = SectionLookup.normalize(title);
            if (normalized.contains("summary") || normalized.contains("profile")) {
                return "PROFESSIONAL PROFILE";
            }
            if (normalized.contains("experience")
                    || normalized.contains("employment")) {
                return "EMPLOYMENT HISTORY";
            }
            if (normalized.contains("project")) {
                return "PROJECTS";
            }
            if (normalized.contains("education")
                    || normalized.contains("certification")) {
                return "EDUCATION";
            }
            if (normalized.contains("skill")) {
                return "KEY SKILLS";
            }
            if (normalized.contains("additional")) {
                return "ADDITIONAL";
            }
            return title.toUpperCase(Locale.ROOT);
        }

        private DocumentTextStyle sectionTitleStyle() {
            return CvTextStyles.of(FontName.HELVETICA_BOLD,
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().rule());
        }

        private Masthead.Style mastheadStyle() {
            DocumentTextStyle nameStyle = CvTextStyles.of(FontName.HELVETICA_BOLD,
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle titleStyle = CvTextStyles.of(FontName.HELVETICA,
                    10.0, DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
            DocumentTextStyle linkStyle = CvTextStyles.of(FontName.HELVETICA,
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    theme.palette().rule());
            return Masthead.Style.builder()
                    .nameStyle(nameStyle)
                    .titleStyle(titleStyle)
                    .metaStyle(theme.contactStyle())
                    .linkStyle(linkStyle)
                    .separatorStyle(theme.contactStyle())
                    .lineMargin(DocumentInsets.top(1))
                    .build();
        }

    }
}
