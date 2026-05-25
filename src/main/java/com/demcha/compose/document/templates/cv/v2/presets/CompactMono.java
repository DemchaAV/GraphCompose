package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
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
import java.util.Locale;
import java.util.Objects;

/**
 * v2 port of the "Compact Mono" CV preset.
 *
 * <p>Visual signature: a dark command-bar header, IBM Plex Mono
 * identity/section labels, a pale left rail for compact skills,
 * education, and additional information, plus same-width white cards
 * in the right column for profile, experience, and selected
 * projects.</p>
 *
 * <p>The rail/card composition is preset-local: it decides which
 * semantic v2 sections belong in each column and how aggressively to
 * compact them. Reusable pieces still go through v2 widgets:
 * {@link Headline}, {@link ContactLine}, and
 * {@link SectionHeader#tickLabel}.</p>
 */
public final class CompactMono {

    /** Stable template identifier. */
    public static final String ID = "compact-mono";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Compact Mono";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 20.0;

    private static final DocumentColor PAPER =
            DocumentColor.rgb(248, 250, 252);
    private static final DocumentColor HEADER =
            DocumentColor.rgb(18, 24, 32);
    private static final DocumentColor HEADER_SOFT =
            DocumentColor.rgb(192, 207, 219);
    private static final DocumentColor LINK_CYAN =
            DocumentColor.rgb(108, 213, 222);
    private static final DocumentColor SEPARATOR_GRAY =
            DocumentColor.rgb(102, 117, 132);
    private static final DocumentColor ACCENT =
            DocumentColor.rgb(0, 126, 151);
    private static final DocumentColor CARD_FILL =
            DocumentColor.rgb(255, 255, 255);

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "professional summary", "profile");
    private static final List<String> SKILL_KEYS =
            List.of("technical skills", "skills", "core skills");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "professional experience", "employment", "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "selected projects");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private CompactMono() {
    }

    /**
     * Builds the preset with its Compact Mono theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.compactMono());
    }

    /**
     * Builds the preset with a caller-supplied theme. Use this for
     * controlled typography/spacing/palette variants without changing
     * the shipped preset.
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

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CvV2CompactMonoRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .fillColor(PAPER);

            addHeader(flow, doc, document.canvas().innerWidth());
            flow.addRow("CvV2CompactMonoBody", row -> row
                    .spacing(14)
                    .weights(0.78, 1.62)
                    .addSection("Rail", rail -> addRail(rail, sections))
                    .addSection("Main", main -> addMain(main, sections)));
            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvDocument doc,
                               double width) {
            flow.addSection("CvV2CompactMonoHeader", section -> {
                section.spacing(4)
                        .padding(new DocumentInsets(13, 16, 14, 16))
                        .fillColor(HEADER)
                        .cornerRadius(3);
                section.addSection("Name", name ->
                        Headline.uppercaseLeftAligned(name,
                                doc.identity().name(), theme,
                                headerNameStyle()));
                section.addSection("Contact", contact ->
                        ContactLine.leftAligned(contact, doc.identity(), theme,
                                headerMetaStyle(), headerLinkStyle(),
                                headerSeparatorStyle()));
                section.addLine(line -> line
                        .name("CvV2CompactMonoHeaderWidthRule")
                        .horizontal(Math.max(0, width - 32))
                        .color(HEADER)
                        .thickness(0.1)
                        .margin(DocumentInsets.zero()));
            });
        }

        private void addRail(SectionBuilder rail, List<CvSection> sections) {
            rail.spacing(8)
                    .padding(new DocumentInsets(11, 11, 13, 11))
                    .fillColor(theme.palette().banner())
                    .accentLeft(ACCENT, 3.0);
            addRailSkills(rail, findSection(sections, SKILL_KEYS));
            addRailEducation(rail, findSection(sections, EDUCATION_KEYS));
            addRailAdditional(rail, findSection(sections, ADDITIONAL_KEYS));
        }

        private void addMain(SectionBuilder main, List<CvSection> sections) {
            main.spacing(8);
            addProfile(main, findSection(sections, SUMMARY_KEYS));
            addExperience(main, findSection(sections, EXPERIENCE_KEYS));
            addProjects(main, findSection(sections, PROJECT_KEYS));
        }

        private void addRailSkills(SectionBuilder parent, CvSection section) {
            if (!(section instanceof SkillsSection skills)
                    || skills.groups().isEmpty()) {
                return;
            }

            parent.addSection("CvV2CompactMonoSkills", host -> {
                moduleLabel(host, "Skills", 22);
                for (SkillGroup group : skills.groups()) {
                    addSkillLine(host, group);
                }
            });
        }

        private void addRailEducation(SectionBuilder parent, CvSection section) {
            if (!(section instanceof EntriesSection education)
                    || education.entries().isEmpty()) {
                return;
            }

            parent.addSection("CvV2CompactMonoEducation", host -> {
                moduleLabel(host, "Education", 22);
                for (CvEntry entry : education.entries()) {
                    host.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle(7.25, theme.palette().ink()))
                            .lineSpacing(1.02)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.bottom(2.0))
                            .rich(rich -> {
                                rich.style(stripBasicMarkdown(entry.title()),
                                        bodyBoldStyle(7.25));
                                appendIfPresent(rich, " | ", entry.subtitle(),
                                        bodyStyle(7.15, theme.palette().ink()));
                                appendIfPresent(rich, " | ", entry.date(),
                                        bodyStyle(7.0, theme.palette().muted()));
                            }));
                }
            });
        }

        private void addRailAdditional(SectionBuilder parent, CvSection section) {
            if (!(section instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }

            parent.addSection("CvV2CompactMonoAdditional", host -> {
                moduleLabel(host, "Additional", 22);
                for (CvRow row : rows.rows()) {
                    addRailLabelValue(host, row.label(), row.body());
                }
            });
        }

        private void addProfile(SectionBuilder parent, CvSection section) {
            if (!(section instanceof ParagraphSection profile)
                    || profile.body().isBlank()) {
                return;
            }
            addCard(parent, "CvV2CompactMonoProfile", "Profile", card ->
                    card.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle(8.05, theme.palette().ink()))
                            .lineSpacing(1.18)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero())
                            .rich(rich -> appendMarkdown(rich, profile.body(),
                                    bodyStyle(8.05, theme.palette().ink())))));
        }

        private void addExperience(SectionBuilder parent, CvSection section) {
            if (!(section instanceof EntriesSection entries)
                    || entries.entries().isEmpty()) {
                return;
            }
            addCard(parent, "CvV2CompactMonoExperience", "Experience", card -> {
                for (CvEntry entry : entries.entries()) {
                    addExperienceEntry(card, entry);
                }
            });
        }

        private void addProjects(SectionBuilder parent, CvSection section) {
            if (!(section instanceof RowsSection projects)
                    || projects.rows().isEmpty()) {
                return;
            }
            addCard(parent, "CvV2CompactMonoProjects", "Selected Projects", card -> {
                for (CvRow row : projects.rows()) {
                    addProject(card, row);
                }
            });
        }

        private void addCard(SectionBuilder parent, String name, String title,
                             SectionAction content) {
            parent.addSection(name, card -> {
                card.spacing(4)
                        .padding(new DocumentInsets(9, 10, 10, 11))
                        .fillColor(CARD_FILL)
                        .stroke(DocumentStroke.of(theme.palette().rule(), 0.35))
                        .cornerRadius(3);
                moduleLabel(card, title, 24);
                content.run(card);
            });
        }

        private void moduleLabel(SectionBuilder host, String title, double tickWidth) {
            SectionHeader.tickLabel(host, title, theme, ACCENT, tickWidth,
                    moduleLabelStyle());
        }

        private void addRailLabelValue(SectionBuilder host, String label,
                                       String value) {
            host.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle(7.2, theme.palette().ink()))
                    .lineSpacing(1.05)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(2.0))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(label) + ":",
                                bodyBoldStyle(7.2));
                        if (value != null && !value.isBlank()) {
                            rich.style(" ", bodyStyle(7.2,
                                    theme.palette().muted()));
                            appendMarkdown(rich, value,
                                    bodyStyle(7.2, theme.palette().muted()));
                        }
                    }));
        }

        private void addSkillLine(SectionBuilder host, SkillGroup group) {
            List<String> picked = group.skills().stream().limit(4).toList();
            if (picked.isEmpty()) {
                return;
            }
            DocumentTextStyle labelStyle = bodyBoldStyle(7.55);
            DocumentTextStyle valueStyle = bodyStyle(7.45,
                    theme.palette().ink());
            host.addParagraph(paragraph -> paragraph
                    .textStyle(valueStyle)
                    .lineSpacing(0.98)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(2.0))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(group.category()) + " ",
                                labelStyle);
                        rich.style(String.join(", ", picked), valueStyle);
                    }));
        }

        private void addExperienceEntry(SectionBuilder host, CvEntry entry) {
            host.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle(8.0, theme.palette().ink()))
                    .lineSpacing(1.08)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(1.2))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(entry.title()),
                                bodyBoldStyle(8.0));
                        if (!entry.subtitle().isBlank()) {
                            rich.style(", " + stripBasicMarkdown(
                                    entry.subtitle()),
                                    bodyStyle(8.0, theme.palette().ink()));
                        }
                        if (!entry.date().isBlank()) {
                            rich.style(" | " + stripBasicMarkdown(
                                    entry.date()),
                                    bodyStyle(7.75, theme.palette().muted()));
                        }
                    }));
            if (!entry.body().isBlank()) {
                host.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle(7.95, theme.palette().ink()))
                        .lineSpacing(1.14)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.bottom(
                                theme.spacing().entrySeparation()))
                        .rich(rich -> appendMarkdown(rich, entry.body(),
                                bodyStyle(7.95, theme.palette().ink()))));
            }
        }

        private void addProject(SectionBuilder host, CvRow row) {
            TitleAndStack title = splitTitleAndStack(row.label());
            DocumentTextStyle projectTitle = bodyBoldStyle(8.0);
            DocumentTextStyle stackStyle = italicStyle(7.75,
                    theme.palette().muted());
            DocumentTextStyle body = bodyStyle(7.95, theme.palette().ink());

            host.addParagraph(paragraph -> paragraph
                    .textStyle(body)
                    .lineSpacing(1.12)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(3.0))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(title.title()),
                                projectTitle);
                        if (!title.stack().isBlank()) {
                            rich.style(" (" + stripBasicMarkdown(title.stack())
                                    + ")", stackStyle);
                        }
                        if (!row.body().isBlank()) {
                            rich.style(" - ", body);
                            appendMarkdown(rich, row.body(), body);
                        }
                    }));
        }

        private DocumentTextStyle headerNameStyle() {
            return style(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    DocumentColor.rgb(255, 255, 255));
        }

        private DocumentTextStyle headerMetaStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    HEADER_SOFT);
        }

        private DocumentTextStyle headerLinkStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    LINK_CYAN);
        }

        private DocumentTextStyle headerSeparatorStyle() {
            return style(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    SEPARATOR_GRAY);
        }

        private DocumentTextStyle moduleLabelStyle() {
            return style(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    ACCENT);
        }

        private DocumentTextStyle bodyStyle(double size, DocumentColor color) {
            return style(theme.typography().bodyFont(), size,
                    DocumentTextDecoration.DEFAULT, color);
        }

        private DocumentTextStyle bodyBoldStyle(double size) {
            return style(theme.typography().bodyFont(), size,
                    DocumentTextDecoration.BOLD, theme.palette().ink());
        }

        private DocumentTextStyle italicStyle(double size, DocumentColor color) {
            return style(theme.typography().bodyFont(), size,
                    DocumentTextDecoration.ITALIC, color);
        }
    }

    private static void appendIfPresent(RichText rich, String prefix,
                                        String value,
                                        DocumentTextStyle style) {
        String clean = stripBasicMarkdown(value);
        if (!clean.isBlank()) {
            rich.style(prefix + clean, style);
        }
    }

    private static void appendMarkdown(RichText rich, String text,
                                       DocumentTextStyle baseStyle) {
        MarkdownInline.append(rich, text == null ? "" : text.trim(), baseStyle);
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

    private static TitleAndStack splitTitleAndStack(String value) {
        String clean = stripBasicMarkdown(value).trim();
        int open = clean.lastIndexOf('(');
        if (open > 0 && clean.endsWith(")")) {
            return new TitleAndStack(clean.substring(0, open).trim(),
                    clean.substring(open + 1, clean.length() - 1).trim());
        }
        return new TitleAndStack(clean, "");
    }

    private static String stripBasicMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        String safeValue = safe(value);
        for (int index = 0; index < safeValue.length(); index++) {
            char current = Character.toLowerCase(safeValue.charAt(index));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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

    private record TitleAndStack(String title, String stack) {
    }

    @FunctionalInterface
    private interface SectionAction {
        void run(SectionBuilder section);
    }
}
