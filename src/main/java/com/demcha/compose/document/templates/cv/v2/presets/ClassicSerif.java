package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
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
import com.demcha.compose.font.FontName;

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
            addSummary(flow, findSection(sections, SUMMARY_KEYS));
            addCoverSkillsModule(flow, findSection(sections, SKILL_KEYS));
            addLinearModule(flow, "Experience",
                    findSection(sections, EXPERIENCE_KEYS));
            addLinearModule(flow, "Projects",
                    findSection(sections, PROJECT_KEYS));
            addLinearModule(flow, "Education",
                    findSection(sections, EDUCATION_KEYS));
            addLinearModule(flow, "Additional",
                    findSection(sections, ADDITIONAL_KEYS));
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
                host.addParagraph(paragraph -> paragraph
                        .textStyle(style(theme.typography().bodyFont(), 9.8,
                                DocumentTextDecoration.DEFAULT,
                                theme.palette().ink()))
                        .lineSpacing(1.55)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> appendMarkdown(rich, summary.body(),
                                style(theme.typography().bodyFont(), 9.8,
                                        DocumentTextDecoration.DEFAULT,
                                        theme.palette().ink()))));
            });
        }

        private void addCoverSkillsModule(PageFlowBuilder flow, CvSection section) {
            if (section == null || !hasContent(section)) {
                return;
            }

            flow.addSection("CvV2ClassicSerifCoreSkills", host -> {
                host.spacing(theme.spacing().sectionBodySpacing())
                        .padding(new DocumentInsets(0, 0, 2, 0));
                SectionHeader.flatSpacedCaps(host, "Core Skills", ACCENT,
                        theme, titleStyle());
                host.addLine(line -> line
                        .name("CvV2ClassicSerifCoreSkillsRule")
                        .horizontal(72)
                        .color(ACCENT)
                        .thickness(1.0)
                        .margin(new DocumentInsets(0, 0, 2, 0)));
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
            if (section == null || !hasContent(section)) {
                return;
            }

            flow.addSection("CvV2ClassicSerif" + normalize(title), host -> {
                host.spacing(theme.spacing().sectionBodySpacing())
                        .padding(new DocumentInsets(0, 0, 2, 0));
                SectionHeader.flatSpacedCaps(host, title, ACCENT, theme,
                        titleStyle());
                host.addLine(line -> line
                        .name("CvV2ClassicSerif" + normalize(title) + "Rule")
                        .horizontal(72)
                        .color(ACCENT)
                        .thickness(1.0)
                        .margin(new DocumentInsets(0, 0, 2, 0)));
                renderDetailBody(host, section);
            });
        }

        private void renderDetailBody(SectionBuilder host, CvSection section) {
            if (section instanceof ParagraphSection paragraph) {
                renderBodyParagraph(host, paragraph.body(), theme.bodyStyle(),
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
            host.addRow("CvV2ClassicSerifEntryHeader", row -> row
                    .spacing(theme.spacing().entryHeaderRowSpacing())
                    .weights(theme.spacing().entryTitleWeight(),
                            theme.spacing().entryDateWeight())
                    .addSection("Title", titleColumn -> titleColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripBasicMarkdown(entry.title()))
                                    .textStyle(theme.entryTitleStyle())
                                    .align(TextAlign.LEFT)
                                    .margin(DocumentInsets.zero())))
                    .addSection("Date", dateColumn -> dateColumn
                            .padding(DocumentInsets.zero())
                            .addParagraph(paragraph -> paragraph
                                    .text(stripBasicMarkdown(entry.date()))
                                    .textStyle(theme.entryDateStyle())
                                    .align(TextAlign.RIGHT)
                                    .margin(DocumentInsets.zero()))));

            if (!entry.subtitle().isBlank()) {
                host.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.subtitle()))
                        .textStyle(theme.entrySubtitleStyle())
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
            renderBodyParagraph(host, entry.body(),
                    style(theme.typography().bodyFont(), 8.8,
                            DocumentTextDecoration.DEFAULT,
                            theme.palette().ink()),
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.top(theme.spacing().paragraphMarginTop()));
        }

        private void renderRows(SectionBuilder host, RowsSection rows) {
            boolean projects = normalize(rows.title()).contains("project");
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
            TitleAndStack title = splitTitleAndStack(row.label());
            DocumentTextStyle stackStyle = style(theme.typography().bodyFont(),
                    8.7, DocumentTextDecoration.ITALIC, theme.palette().muted());

            host.addParagraph(paragraph -> paragraph
                    .textStyle(theme.entryTitleStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(theme.spacing().paragraphMarginTop()))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(title.title()),
                                theme.entryTitleStyle());
                        if (!title.stack().isBlank()) {
                            rich.style(" (" + stripBasicMarkdown(title.stack())
                                    + ")", stackStyle);
                        }
                    }));
            renderBodyParagraph(host, row.body(),
                    style(theme.typography().bodyFont(), 8.8,
                            DocumentTextDecoration.DEFAULT,
                            theme.palette().ink()),
                    theme.typography().bodyLineSpacing(),
                    DocumentInsets.zero());
        }

        private void renderKeyValue(SectionBuilder host, CvRow row) {
            host.addParagraph(paragraph -> paragraph
                    .textStyle(theme.bodyStyle())
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(theme.spacing().paragraphMarginTop()))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(row.label()) + ":",
                                theme.bodyBoldStyle());
                        if (!row.body().isBlank()) {
                            rich.style(" ", theme.bodyStyle());
                            appendMarkdown(rich, row.body(), theme.bodyStyle());
                        }
                    }));
        }

        private void renderTightKeyValue(SectionBuilder host, CvRow row) {
            host.addParagraph(paragraph -> paragraph
                    .textStyle(theme.bodyStyle())
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(row.label()) + ":",
                                theme.bodyBoldStyle());
                        if (!row.body().isBlank()) {
                            rich.style(" ", theme.bodyStyle());
                            appendMarkdown(rich, row.body(), theme.bodyStyle());
                        }
                    }));
        }

        private void addCenteredTitle(SectionBuilder host, String title) {
            host.addParagraph(paragraph -> paragraph
                    .text(TextOrnaments.spacedUpper(title))
                    .textStyle(titleStyle())
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero()));
        }

        private void renderBodyParagraph(SectionBuilder host, String text,
                                         DocumentTextStyle bodyStyle,
                                         double lineSpacing,
                                         DocumentInsets margin) {
            if (text == null || text.isBlank()) {
                return;
            }
            host.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle)
                    .lineSpacing(lineSpacing)
                    .align(TextAlign.LEFT)
                    .margin(margin)
                    .rich(rich -> appendMarkdown(rich, text.trim(), bodyStyle)));
        }

        private DocumentTextStyle titleStyle() {
            return style(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    ACCENT);
        }

        private DocumentTextStyle contactMetaStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle contactLinkStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    ACCENT);
        }

        private DocumentTextStyle contactSeparatorStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().rule());
        }
    }

    private static void appendMarkdown(RichText rich, String text,
                                       DocumentTextStyle baseStyle) {
        MarkdownInline.append(rich, text, baseStyle);
    }

    private static CvSection findSection(List<CvSection> sections,
                                         List<String> keys) {
        for (CvSection section : sections) {
            String normalizedTitle = normalize(section.title());
            for (String key : keys) {
                if (normalizedTitle.contains(normalize(key))) {
                    return section;
                }
            }
        }
        return null;
    }

    private static boolean hasContent(CvSection section) {
        if (section instanceof ParagraphSection p) {
            return !p.body().isBlank();
        }
        if (section instanceof EntriesSection e) {
            return !e.entries().isEmpty();
        }
        if (section instanceof RowsSection r) {
            return !r.rows().isEmpty();
        }
        if (section instanceof SkillsSection s) {
            return !s.groups().isEmpty();
        }
        return false;
    }

    private static DocumentTextStyle style(FontName font, double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }

    private static TitleAndStack splitTitleAndStack(String value) {
        String title = value == null ? "" : value.trim();
        String stack = "";
        int open = title.indexOf('(');
        int close = title.lastIndexOf(')');
        if (open > 0 && close > open) {
            stack = title.substring(open + 1, close).trim();
            title = title.substring(0, open).trim();
        }
        return new TitleAndStack(title, stack);
    }

    private static String stripBasicMarkdown(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(safe.length());
        for (int i = 0; i < safe.length(); i++) {
            char current = Character.toLowerCase(safe.charAt(i));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private record TitleAndStack(String title, String stack) {
    }
}
