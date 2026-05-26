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
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
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
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;

import java.util.List;
import java.util.Objects;

/**
 * v2 port of the "Classic Serif" CV preset.
 *
 * <p>Visual signature: PT Serif throughout, centred spaced-caps name,
 * tan rules, a cream professional-profile band, a quiet core-skills
 * module, then detail modules for experience, projects,
 * education, and additional information.</p>
 *
 * <p>The preset reuses the shared headline, contact, and section-title
 * widgets. The cover/detail page composition stays preset-local
 * because no other v2 preset uses this editorial two-page structure
 * today.</p>
 */
public final class ClassicSerif {

    /** Stable template identifier. */
    public static final String ID = "classic-serif";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Classic Serif";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 20.0;

    private static final DocumentColor ACCENT =
            DocumentColor.rgb(126, 93, 52);

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private ClassicSerif() {
    }

    /**
     * Builds the preset with its Classic Serif theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.classicSerif());
    }

    /**
     * Builds the preset with a caller-supplied theme.
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
            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);

            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CvV2ClassicSerifRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc, width);
            addSummary(flow, SectionLookup.firstMatching(sections, SUMMARY_KEYS));
            addCoverSkillsModule(flow, SectionLookup.firstMatching(sections, SKILL_KEYS));
            addLinearModule(flow, "Experience",
                    SectionLookup.firstMatching(sections, EXPERIENCE_KEYS));
            addLinearModule(flow, "Projects",
                    SectionLookup.firstMatching(sections, PROJECT_KEYS));
            addLinearModule(flow, "Education",
                    SectionLookup.firstMatching(sections, EDUCATION_KEYS));
            addLinearModule(flow, "Additional",
                    SectionLookup.firstMatching(sections, ADDITIONAL_KEYS));
            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvDocument doc,
                               double width) {
            flow.addSection("CvV2ClassicSerifHeader", section -> {
                section.spacing(5);
                Headline.spacedCentered(section, doc.identity().name(), theme);
                section.addLine(line -> line
                        .name("CvV2ClassicSerifHeaderRule")
                        .horizontal(width)
                        .color(theme.palette().rule())
                        .thickness(theme.spacing().accentRuleWidth())
                        .margin(new DocumentInsets(1, 0, 0, 0)));
                ContactLine.centered(section, doc.identity(), theme,
                        contactMetaStyle(), contactLinkStyle(),
                        contactSeparatorStyle());
            });
        }

        private void addSummary(PageFlowBuilder flow, CvSection section) {
            if (!(section instanceof ParagraphSection summary)
                    || summary.body().isBlank()) {
                return;
            }

            flow.addSection("CvV2ClassicSerifSummary", host -> {
                host.spacing(5)
                        .padding(new DocumentInsets(12, 18, 13, 18))
                        .fillColor(theme.palette().banner())
                        .accentTop(ACCENT, 1.15)
                        .accentBottom(theme.palette().rule(), 0.45);
                addCenteredTitle(host, "Professional Profile");
                RichParagraphRenderer.render(host, summary.body(),
                        CvTextStyles.of(theme.typography().bodyFont(), 9.8,
                                DocumentTextDecoration.DEFAULT,
                                theme.palette().ink()),
                        1.55, DocumentInsets.zero(), TextAlign.CENTER);
            });
        }

        private void addCoverSkillsModule(PageFlowBuilder flow, CvSection section) {
            if (section == null || !SectionLookup.hasContent(section)) {
                return;
            }

            flow.addSection("CvV2ClassicSerifCoreSkills", host -> {
                host.spacing(theme.spacing().sectionBodySpacing())
                        .padding(new DocumentInsets(0, 0, 2, 0));
                SectionHeader.spacedCapsRule(host, "Core Skills", theme,
                        titleStyle(), ACCENT, 72, 1.0,
                        new DocumentInsets(0, 0, 2, 0));
                renderCoverSkillsBody(host, section);
            });
        }

        private void renderCoverSkillsBody(SectionBuilder host,
                                           CvSection section) {
            if (section instanceof SkillsSection skills) {
                for (SkillGroup group : skills.groups()) {
                    renderTightKeyValue(host,
                            new CvRow(group.category(), group.skillsInline()));
                }
                return;
            }
            renderDetailBody(host, section);
        }

        private void addLinearModule(PageFlowBuilder flow, String title,
                                     CvSection section) {
            if (section == null || !SectionLookup.hasContent(section)) {
                return;
            }

            flow.addSection("CvV2ClassicSerif" + SectionLookup.normalize(title), host -> {
                host.spacing(theme.spacing().sectionBodySpacing())
                        .padding(new DocumentInsets(0, 0, 2, 0));
                SectionHeader.spacedCapsRule(host, title, theme,
                        titleStyle(), ACCENT, 72, 1.0,
                        new DocumentInsets(0, 0, 2, 0));
                renderDetailBody(host, section);
            });
        }

        private void renderDetailBody(SectionBuilder host, CvSection section) {
            if (section instanceof ParagraphSection paragraph) {
                RichParagraphRenderer.render(host, paragraph.body(),
                        theme.bodyStyle(),
                        theme.typography().bodyLineSpacing(),
                        DocumentInsets.top(theme.spacing().paragraphMarginTop()));
            } else if (section instanceof EntriesSection entries) {
                renderEntries(host, entries);
            } else if (section instanceof RowsSection rows) {
                renderRows(host, rows);
            } else if (section instanceof SkillsSection skills) {
                for (SkillGroup group : skills.groups()) {
                    renderKeyValue(host,
                            new CvRow(group.category(), group.skillsInline()));
                }
            } else {
                throw new IllegalStateException(
                        "Unknown CvSection subtype: "
                                + section.getClass().getName());
            }
        }

        private void renderEntries(SectionBuilder host, EntriesSection entries) {
            for (int i = 0; i < entries.entries().size(); i++) {
                if (i > 0) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                renderEntry(host, entries.entries().get(i));
            }
        }

        private void renderEntry(SectionBuilder host, CvEntry entry) {
            EntryCompactRenderer.twoColumnTitleDateBody(host, entry,
                    "CvV2ClassicSerifEntryHeader",
                    theme.entryTitleStyle(), theme.entryDateStyle(),
                    theme.entrySubtitleStyle(),
                    CvTextStyles.of(theme.typography().bodyFont(), 8.8,
                            DocumentTextDecoration.DEFAULT,
                            theme.palette().ink()),
                    theme.spacing().entryHeaderRowSpacing(),
                    theme.spacing().entryTitleWeight(),
                    theme.spacing().entryDateWeight(),
                    DocumentInsets.zero(),
                    DocumentInsets.top(theme.spacing().paragraphMarginTop()),
                    theme.typography().bodyLineSpacing(),
                    false);
        }

        private void renderRows(SectionBuilder host, RowsSection rows) {
            boolean projects = SectionLookup.titleContains(rows.title(), "project");
            for (int i = 0; i < rows.rows().size(); i++) {
                if (i > 0) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                if (projects) {
                    renderProject(host, rows.rows().get(i));
                } else {
                    renderKeyValue(host, rows.rows().get(i));
                }
            }
        }

        private void renderProject(SectionBuilder host, CvRow row) {
            ProjectRenderer.titleThenBody(host, row, theme.entryTitleStyle(),
                    CvTextStyles.of(theme.typography().bodyFont(), 8.7,
                            DocumentTextDecoration.ITALIC,
                            theme.palette().muted()),
                    CvTextStyles.of(theme.typography().bodyFont(), 8.8,
                            DocumentTextDecoration.DEFAULT,
                            theme.palette().ink()),
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.top(theme.spacing().paragraphMarginTop()),
                    DocumentInsets.zero());
        }

        private void renderKeyValue(SectionBuilder host, CvRow row) {
            LabelValueRenderer.render(host, row.label(), row.body(),
                    theme.bodyBoldStyle(), theme.bodyStyle(),
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.top(theme.spacing().paragraphMarginTop()));
        }

        private void renderTightKeyValue(SectionBuilder host, CvRow row) {
            LabelValueRenderer.render(host, row.label(), row.body(),
                    theme.bodyBoldStyle(), theme.bodyStyle(),
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.zero());
        }

        private void addCenteredTitle(SectionBuilder host, String title) {
            host.addParagraph(paragraph -> paragraph
                    .text(TextOrnaments.spacedUpper(title))
                    .textStyle(titleStyle())
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero()));
        }

        private DocumentTextStyle titleStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    ACCENT);
        }

        private DocumentTextStyle contactMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle contactLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    ACCENT);
        }

        private DocumentTextStyle contactSeparatorStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().rule());
        }
    }
}
