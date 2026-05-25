package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
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
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Objects;

/**
 * v2 port of the "Nordic Clean" CV preset.
 *
 * <p>Visual signature: Barlow uppercase identity, a quiet right-aligned
 * contact stack, pale teal profile band with a left accent, then a
 * two-column body where the tinted left rail carries skills,
 * education, and additional information while the right column carries
 * experience and selected projects.</p>
 *
 * <p>The layout is intentionally preset-local because the side rail,
 * profile band, and compact project/entry treatments are a full page
 * composition rather than a generic section-body renderer. Reusable
 * pieces still go through the v2 widgets and semantic data model.</p>
 *
 * <h2>Customising the rail and colours</h2>
 *
 * <p>Use {@link #create(CvTheme, Options)} when you want the same
 * Nordic layout with a different accent colour, rail fill, profile
 * band fill, or rail side. The default {@link #create()} stays
 * byte-for-byte compatible with the shipped teal-left-rail look.</p>
 *
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = NordicClean.create(
 *         CvTheme.nordicClean(),
 *         NordicClean.Options.builder()
 *                 .railSide(NordicClean.RailSide.RIGHT)
 *                 .accentColor(DocumentColor.rgb(40, 110, 120))
 *                 .railFillColor(DocumentColor.rgb(244, 249, 249))
 *                 .profileFillColor(DocumentColor.rgb(226, 244, 245))
 *                 .build());
 * }</pre>
 */
public final class NordicClean {

    /** Stable template identifier. */
    public static final String ID = "nordic-clean";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Nordic Clean";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 18.0;

    private static final DocumentColor DEFAULT_ACCENT =
            DocumentColor.rgb(28, 128, 135);
    private static final DocumentColor DEFAULT_RAIL_FILL =
            DocumentColor.rgb(244, 249, 249);

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

    private NordicClean() {
    }

    /**
     * Builds the preset with its Nordic Clean theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.nordicClean(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and explicit
     * Nordic-specific layout/colour options.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme,
                                                      Options options) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(options, "options");
        return new Template(theme, options);
    }

    /**
     * Which side carries the tinted rail modules (skills, education,
     * additional information). The opposite column carries experience
     * and projects.
     */
    public enum RailSide {
        /** Skills rail on the left, experience/projects on the right. */
        LEFT,
        /** Skills rail on the right, experience/projects on the left. */
        RIGHT
    }

    /**
     * Nordic-specific configuration for layout side and colour
     * overrides.
     *
     * @param railSide         side for the skills/education rail
     * @param accentColor      teal accent used for rules, links, and
     *                         the identity underline
     * @param railFillColor    fill behind the rail column
     * @param profileFillColor profile band fill; {@code null} means
     *                         use {@code theme.palette().banner()}
     */
    public record Options(RailSide railSide,
                          DocumentColor accentColor,
                          DocumentColor railFillColor,
                          DocumentColor profileFillColor) {

        public Options {
            railSide = railSide == null ? RailSide.LEFT : railSide;
            accentColor = accentColor == null ? DEFAULT_ACCENT : accentColor;
            railFillColor = railFillColor == null
                    ? DEFAULT_RAIL_FILL
                    : railFillColor;
        }

        /**
         * Default Nordic look: left rail, teal accent, pale rail fill,
         * and profile fill read from {@code CvTheme.nordicClean()}.
         */
        public static Options defaults() {
            return new Options(RailSide.LEFT, null, null, null);
        }

        /**
         * Starts a mutable builder for ergonomic colour overrides.
         */
        public static Builder builder() {
            return new Builder();
        }

        private DocumentColor resolvedProfileFill(CvTheme theme) {
            return profileFillColor == null
                    ? theme.palette().banner()
                    : profileFillColor;
        }

        /**
         * Builder for {@link Options}.
         */
        public static final class Builder {
            private RailSide railSide = RailSide.LEFT;
            private DocumentColor accentColor;
            private DocumentColor railFillColor;
            private DocumentColor profileFillColor;

            private Builder() {
            }

            public Builder railSide(RailSide value) {
                this.railSide = value;
                return this;
            }

            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            public Builder railFillColor(DocumentColor value) {
                this.railFillColor = value;
                return this;
            }

            public Builder profileFillColor(DocumentColor value) {
                this.profileFillColor = value;
                return this;
            }

            public Options build() {
                return new Options(railSide, accentColor, railFillColor,
                        profileFillColor);
            }
        }
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;
        private final Options options;

        Template(CvTheme theme, Options options) {
            this.theme = theme;
            this.options = options;
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
                    .name("CvV2NordicCleanRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc);
            addProfile(flow, findSection(sections, SUMMARY_KEYS));
            addBody(flow, sections);
            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvDocument doc) {
            flow.addRow("CvV2NordicCleanHeader", row -> row
                    .spacing(14)
                    .weights(1.2, 0.8)
                    .addSection("Identity", identity -> {
                        identity.spacing(3)
                                .padding(new DocumentInsets(1, 0, 2, 0));
                        Headline.uppercaseLeftAligned(identity,
                                doc.identity().name(), theme,
                                headlineStyle());
                        identity.addShape(shape -> shape
                                .name("CvV2NordicCleanNameAccent")
                                .size(64, 2.6)
                                .fillColor(options.accentColor())
                                .cornerRadius(1.3)
                                .margin(DocumentInsets.zero()));
                        if (!doc.identity().jobTitle().isBlank()) {
                            identity.addParagraph(paragraph -> paragraph
                                    .text(stripBasicMarkdown(
                                            doc.identity().jobTitle())
                                            .toUpperCase(java.util.Locale.ROOT))
                                    .textStyle(style(theme.typography().bodyFont(),
                                            7.7,
                                            DocumentTextDecoration.BOLD,
                                            theme.palette().muted()))
                                    .margin(DocumentInsets.zero()));
                        }
                    })
                    .addSection("Contact", contact ->
                            ContactLine.rightAlignedStacked(contact,
                                    doc.identity(), theme,
                                    contactMetaStyle(), contactLinkStyle())));
        }

        private void addProfile(PageFlowBuilder flow, CvSection section) {
            if (!(section instanceof ParagraphSection profile)
                    || profile.body().isBlank()) {
                return;
            }

            flow.addSection("CvV2NordicCleanProfile", host -> host
                    .spacing(4)
                    .padding(new DocumentInsets(8, 10, 8, 10))
                    .fillColor(options.resolvedProfileFill(theme))
                    .accentLeft(options.accentColor(), 3.0)
                    .cornerRadius(DocumentCornerRadius.right(4))
                    .addParagraph(paragraph -> paragraph
                            .text("PROFILE")
                            .textStyle(sectionTitleStyle())
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle(7.85, theme.palette().ink()))
                            .lineSpacing(1.25)
                            .margin(DocumentInsets.zero())
                            .rich(rich -> appendMarkdown(rich,
                                    profile.body(),
                                    bodyStyle(7.85, theme.palette().ink())))));
        }

        private void addBody(PageFlowBuilder flow, List<CvSection> sections) {
            flow.addRow("CvV2NordicCleanBody", row -> {
                row.spacing(15);
                if (options.railSide() == RailSide.LEFT) {
                    row.weights(0.72, 1.28)
                            .addSection("Rail",
                                    rail -> addRail(rail, sections))
                            .addSection("Main",
                                    main -> addMain(main, sections));
                } else {
                    row.weights(1.28, 0.72)
                            .addSection("Main",
                                    main -> addMain(main, sections))
                            .addSection("Rail",
                                    rail -> addRail(rail, sections));
                }
            });
        }

        private void addRail(SectionBuilder rail, List<CvSection> sections) {
            rail.spacing(8)
                    .padding(new DocumentInsets(9, 10, 9, 10))
                    .fillColor(options.railFillColor())
                    .stroke(DocumentStroke.of(theme.palette().rule(), 0.35))
                    .cornerRadius(4);
            addSkills(rail, findSection(sections, SKILL_KEYS));
            addEducation(rail, findSection(sections, EDUCATION_KEYS));
            addAdditional(rail, findSection(sections, ADDITIONAL_KEYS));
        }

        private void addMain(SectionBuilder main, List<CvSection> sections) {
            main.spacing(9);
            addExperience(main, findSection(sections, EXPERIENCE_KEYS));
            addProjects(main, findSection(sections, PROJECT_KEYS));
        }

        private void addSkills(SectionBuilder parent, CvSection section) {
            if (!(section instanceof SkillsSection skills)
                    || skills.groups().isEmpty()) {
                return;
            }

            parent.addSection("CvV2NordicCleanSkills", host -> {
                addHeading(host, "Skills", 82);
                for (SkillGroup group : skills.groups()) {
                    addLabelValueLine(host, group.category(),
                            group.skillsInline(), 7.15, 1.05);
                }
            });
        }

        private void addEducation(SectionBuilder parent, CvSection section) {
            if (!(section instanceof EntriesSection education)
                    || education.entries().isEmpty()) {
                return;
            }

            parent.addSection("CvV2NordicCleanEducation", host -> {
                addHeading(host, "Education", 82);
                for (CvEntry entry : education.entries()) {
                    host.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle(7.05, theme.palette().ink()))
                            .lineSpacing(1.05)
                            .margin(DocumentInsets.bottom(2))
                            .rich(rich -> {
                                rich.style(stripBasicMarkdown(entry.title()),
                                        style(theme.typography().bodyFont(),
                                                7.05,
                                                DocumentTextDecoration.BOLD,
                                                theme.palette().ink()));
                                if (!entry.subtitle().isBlank()) {
                                    rich.style(" / "
                                                    + stripBasicMarkdown(
                                                            entry.subtitle()),
                                            bodyStyle(7.05,
                                                    theme.palette().muted()));
                                }
                                if (!entry.date().isBlank()) {
                                    rich.style(" / "
                                                    + stripBasicMarkdown(
                                                            entry.date()),
                                            bodyStyle(6.85,
                                                    theme.palette().muted()));
                                }
                            }));
                }
            });
        }

        private void addAdditional(SectionBuilder parent, CvSection section) {
            if (!(section instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }

            parent.addSection("CvV2NordicCleanAdditional", host -> {
                addHeading(host, "Additional", 82);
                for (CvRow row : rows.rows()) {
                    addLabelValueLine(host, row.label(), row.body(),
                            7.1, 1.05);
                }
            });
        }

        private void addExperience(SectionBuilder parent, CvSection section) {
            if (!(section instanceof EntriesSection entries)
                    || entries.entries().isEmpty()) {
                return;
            }

            parent.addSection("CvV2NordicCleanExperience", host -> {
                addHeading(host, "Experience", 130);
                for (CvEntry entry : entries.entries()) {
                    addWorkEntry(host, entry);
                }
            });
        }

        private void addProjects(SectionBuilder parent, CvSection section) {
            if (!(section instanceof RowsSection projects)
                    || projects.rows().isEmpty()) {
                return;
            }

            parent.addSection("CvV2NordicCleanProjects", host -> {
                addHeading(host, "Selected Projects", 130);
                for (CvRow row : projects.rows()) {
                    addProject(host, row);
                }
            });
        }

        private void addWorkEntry(SectionBuilder host, CvEntry entry) {
            host.addParagraph(paragraph -> paragraph
                    .textStyle(theme.entryTitleStyle())
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(entry.title()),
                                theme.entryTitleStyle());
                        if (!entry.date().isBlank()) {
                            rich.style(" / " + stripBasicMarkdown(entry.date()),
                                    style(theme.typography().bodyFont(),
                                            theme.typography().sizeEntryDate(),
                                            DocumentTextDecoration.BOLD,
                                            options.accentColor()));
                        }
                    }));
            if (!entry.subtitle().isBlank()) {
                host.addParagraph(paragraph -> paragraph
                        .text(stripBasicMarkdown(entry.subtitle()))
                        .textStyle(theme.entrySubtitleStyle())
                        .margin(DocumentInsets.zero()));
            }
            if (!entry.body().isBlank()) {
                host.addParagraph(paragraph -> paragraph
                        .textStyle(theme.bodyStyle())
                        .lineSpacing(theme.typography().bodyLineSpacing())
                        .margin(DocumentInsets.bottom(5))
                        .rich(rich -> appendMarkdown(rich, entry.body(),
                                theme.bodyStyle())));
            }
        }

        private void addProject(SectionBuilder host, CvRow row) {
            TitleAndStack title = splitTitleAndStack(row.label());
            DocumentTextStyle projectTitle = style(theme.typography().bodyFont(),
                    7.35, DocumentTextDecoration.BOLD, theme.palette().ink());
            DocumentTextStyle context = bodyStyle(6.95, theme.palette().muted());
            DocumentTextStyle body = bodyStyle(7.2, theme.palette().ink());

            host.addParagraph(paragraph -> paragraph
                    .textStyle(body)
                    .lineSpacing(1.08)
                    .margin(DocumentInsets.bottom(3))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(title.title()),
                                projectTitle);
                        if (!title.stack().isBlank()) {
                            rich.style(" (" + stripBasicMarkdown(title.stack())
                                    + ")", context);
                        }
                        if (!row.body().isBlank()) {
                            rich.style(" - ", body);
                            appendMarkdown(rich, row.body(), body);
                        }
                    }));
        }

        private void addHeading(SectionBuilder host, String title,
                                double ruleWidth) {
            host.spacing(3)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(java.util.Locale.ROOT))
                            .textStyle(sectionTitleStyle())
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(ruleWidth)
                            .color(options.accentColor())
                            .thickness(theme.spacing().accentRuleWidth())
                            .margin(DocumentInsets.bottom(2)));
        }

        private void addLabelValueLine(SectionBuilder host, String label,
                                       String value, double size,
                                       double lineSpacing) {
            DocumentTextStyle labelStyle = style(theme.typography().bodyFont(),
                    size, DocumentTextDecoration.BOLD, theme.palette().ink());
            DocumentTextStyle valueStyle = bodyStyle(size,
                    theme.palette().muted());

            host.addParagraph(paragraph -> paragraph
                    .textStyle(valueStyle)
                    .lineSpacing(lineSpacing)
                    .margin(DocumentInsets.bottom(1.5))
                    .rich(rich -> {
                        rich.style(stripBasicMarkdown(label) + ":",
                                labelStyle);
                        if (value != null && !value.isBlank()) {
                            rich.style(" ", valueStyle);
                            appendMarkdown(rich, value, valueStyle);
                        }
                    }));
        }

        private DocumentTextStyle headlineStyle() {
            return style(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sectionTitleStyle() {
            return style(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    options.accentColor());
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
                    options.accentColor());
        }

        private DocumentTextStyle bodyStyle(double size, DocumentColor color) {
            return style(theme.typography().bodyFont(), size,
                    DocumentTextDecoration.DEFAULT, color);
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
