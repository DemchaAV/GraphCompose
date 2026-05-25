package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.widgets.TableWidget;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
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
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.editorialBlue());
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
            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2EditorialBlueRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(pageFlow, doc.identity());

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN).stream()
                    .filter(this::hasContent)
                    .toList();
            for (int i = 0; i < sections.size(); i++) {
                CvSection section = sections.get(i);
                String name = "CvV2EditorialBlue_" + i;
                sectionHeader(pageFlow, name + "_Title",
                        displayTitle(section.title()), width, true);
                pageFlow.addSection(name + "_Body",
                        host -> renderSectionBody(host, section, width));
            }

            addFooter(pageFlow, width);
            pageFlow.build();
        }

        private void addHeader(PageFlowBuilder pageFlow, CvIdentity identity) {
            pageFlow.addSection("CvV2EditorialBlueHeader", section -> {
                DocumentTextStyle nameStyle = style(FontName.HELVETICA_BOLD,
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.BOLD, NAME_COLOR);
                Headline.uppercaseCentered(section, identity.name(),
                        theme, nameStyle);

                if (!identity.jobTitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(identity.jobTitle())
                            .textStyle(style(FontName.HELVETICA,
                                    10.0, DocumentTextDecoration.DEFAULT,
                                    theme.palette().ink()))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(1)));
                }

                String meta = joinDash(identity.contact().phone(),
                        identity.contact().address());
                if (!meta.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(meta)
                            .textStyle(theme.contactStyle())
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(1)));
                }

                addLinkRow(section, identity);
            });
        }

        private void addLinkRow(SectionBuilder section, CvIdentity identity) {
            List<ContactPart> parts = new ArrayList<>();
            String email = identity.contact().email();
            if (!email.isBlank()) {
                parts.add(new ContactPart(email,
                        new DocumentLinkOptions("mailto:" + email)));
            }
            for (CvLink link : identity.links()) {
                if (!link.label().isBlank()) {
                    parts.add(new ContactPart(link.label(),
                            link.url().isBlank()
                                    ? null
                                    : new DocumentLinkOptions(link.url())));
                }
            }
            if (parts.isEmpty()) {
                return;
            }

            DocumentTextStyle base = theme.contactStyle();
            DocumentTextStyle linkStyle = style(FontName.HELVETICA,
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    theme.palette().rule());
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        for (int i = 0; i < parts.size(); i++) {
                            ContactPart part = parts.get(i);
                            if (part.link() == null) {
                                rich.style(part.text(), base);
                            } else {
                                rich.with(part.text(), linkStyle, part.link());
                            }
                            if (i < parts.size() - 1) {
                                rich.style(theme.decoration().contactSeparator(), base);
                            }
                        }
                    }));
        }

        private void sectionHeader(PageFlowBuilder pageFlow, String name,
                                   String title, double width,
                                   boolean withTopRule) {
            if (withTopRule) {
                pageFlow.addLine(line -> line
                        .name(name + "RuleTop")
                        .horizontal(width)
                        .color(theme.palette().rule())
                        .thickness(theme.spacing().accentRuleWidth())
                        .margin(new DocumentInsets(8, 0, 0, 0)));
            }
            pageFlow.addSection(name, section -> section
                    .spacing(0)
                    .padding(new DocumentInsets(7, 0, 5, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(title)
                            .textStyle(style(FontName.HELVETICA_BOLD,
                                    theme.typography().sizeBanner(),
                                    DocumentTextDecoration.BOLD,
                                    theme.palette().rule()))
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero())));
            pageFlow.addLine(line -> line
                    .name(name + "RuleBottom")
                    .horizontal(width)
                    .color(theme.palette().rule())
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(DocumentInsets.zero()));
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
            boolean education = isEducation(entries.title());
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
            DocumentTextStyle titleStyle = style(FontName.HELVETICA_BOLD,
                    11.0, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle dateStyle = style(FontName.HELVETICA_BOLD,
                    11.0, DocumentTextDecoration.BOLD, theme.palette().rule());
            DocumentTextStyle subtitleStyle = style(FontName.HELVETICA,
                    9.4, DocumentTextDecoration.ITALIC, theme.palette().ink());

            section.addParagraph(paragraph -> paragraph
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(entry.title(), titleStyle);
                        if (!entry.date().isBlank()) {
                            rich.style(" ", titleStyle);
                            rich.style(entry.date(), dateStyle);
                        }
                    }));
            if (!entry.subtitle().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(entry.subtitle())
                        .textStyle(subtitleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
            if (!entry.body().isBlank()) {
                renderParagraph(section, entry.body(), 1.5);
            }
        }

        private void renderEducationEntry(SectionBuilder section, CvEntry entry) {
            DocumentTextStyle titleStyle = style(FontName.HELVETICA_BOLD,
                    10.6, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle dateStyle = style(FontName.HELVETICA_BOLD,
                    10.0, DocumentTextDecoration.BOLD, theme.palette().rule());
            DocumentTextStyle subtitleStyle = style(FontName.HELVETICA,
                    9.2, DocumentTextDecoration.ITALIC, theme.palette().ink());

            section.addParagraph(paragraph -> paragraph
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(entry.title(), titleStyle);
                        if (!entry.date().isBlank()) {
                            rich.style(" ", titleStyle);
                            rich.style(entry.date(), dateStyle);
                        }
                    }));
            if (!entry.subtitle().isBlank()) {
                section.addParagraph(paragraph -> paragraph
                        .text(entry.subtitle())
                        .textStyle(subtitleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
            if (!entry.body().isBlank()) {
                renderParagraph(section, entry.body(), 1.4);
            }
        }

        private void renderRows(SectionBuilder section, RowsSection rows) {
            if (isProjects(rows.title())) {
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
            TitleAndStack title = splitTitleAndStack(row.label());
            DocumentTextStyle titleStyle = style(FontName.HELVETICA_BOLD,
                    10.6, DocumentTextDecoration.BOLD, NAME_COLOR);
            DocumentTextStyle stackStyle = style(FontName.HELVETICA,
                    9.3, DocumentTextDecoration.ITALIC, theme.palette().rule());

            section.addParagraph(paragraph -> paragraph
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(title.title(), titleStyle);
                        if (!title.stack().isBlank()) {
                            rich.style(" (" + title.stack() + ")", stackStyle);
                        }
                    }));
            if (!row.body().isBlank()) {
                renderParagraph(section, row.body(), 1.45);
            }
        }

        private void renderKeyValue(SectionBuilder section, CvRow row) {
            DocumentTextStyle keyStyle = style(FontName.HELVETICA_BOLD,
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.BOLD, NAME_COLOR);
            section.addParagraph(paragraph -> paragraph
                    .textStyle(theme.bodyStyle())
                    .lineSpacing(1.4)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> {
                        rich.style(row.label() + ":", keyStyle);
                        if (!row.body().isBlank()) {
                            rich.style(" ", theme.bodyStyle());
                            appendMarkdown(rich, row.body(), theme.bodyStyle());
                        }
                    }));
        }

        private void renderParagraph(SectionBuilder section, String text,
                                     double lineSpacing) {
            String value = text == null ? "" : text.trim();
            if (value.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .textStyle(theme.bodyStyle())
                    .lineSpacing(lineSpacing)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1))
                    .rich(rich -> appendMarkdown(rich, value, theme.bodyStyle())));
        }

        private void renderSkills(SectionBuilder section, List<SkillGroup> groups,
                                  double width) {
            if (groups.isEmpty()) {
                return;
            }
            DocumentTextStyle cellStyle = style(FontName.HELVETICA,
                    8.6, DocumentTextDecoration.DEFAULT, theme.palette().ink());
            TableWidget.Style tableStyle = TableWidget.Style.builder()
                    .name("CvV2EditorialBlueSkillsTable")
                    .columns(SKILL_COLUMNS)
                    .cellPadding(new DocumentInsets(4, 6, 4, 6))
                    .border(theme.palette().banner(), 0.5)
                    .textStyle(cellStyle)
                    .widthAdjustment(1.0)
                    .build();

            TableWidget.grid(section, flattenSkills(groups), width, tableStyle);
        }

        private List<String> flattenSkills(List<SkillGroup> groups) {
            List<String> out = new ArrayList<>();
            for (SkillGroup group : groups) {
                for (String skill : group.skills()) {
                    out.add("• " + skill);
                }
            }
            return out;
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
                            .textStyle(style(FontName.HELVETICA,
                                    8.4, DocumentTextDecoration.ITALIC,
                                    theme.palette().muted()))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(2))));
        }

        private boolean hasContent(CvSection section) {
            if (section instanceof ParagraphSection p) {
                return !p.body().isBlank();
            }
            if (section instanceof SkillsSection s) {
                return !s.groups().isEmpty();
            }
            if (section instanceof EntriesSection e) {
                return !e.entries().isEmpty();
            }
            if (section instanceof RowsSection r) {
                return !r.rows().isEmpty();
            }
            return false;
        }

        private String displayTitle(String title) {
            String normalized = normalize(title);
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

        private boolean isEducation(String title) {
            String normalized = normalize(title);
            return normalized.contains("education")
                    || normalized.contains("certification");
        }

        private boolean isProjects(String title) {
            return normalize(title).contains("project");
        }
    }

    private static void appendMarkdown(RichText rich, String text,
                                       DocumentTextStyle baseStyle) {
        MarkdownInline.append(rich, text, baseStyle);
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

    private static String joinDash(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
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

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder(safe.length());
        for (int i = 0; i < safe.length(); i++) {
            char current = Character.toLowerCase(safe.charAt(i));
            if (Character.isLetterOrDigit(current)) {
                out.append(current);
            }
        }
        return out.toString();
    }

    private record ContactPart(String text, DocumentLinkOptions link) {
    }

    private record TitleAndStack(String title, String stack) {
    }
}
