package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.EntryCompactRenderer;
import com.demcha.compose.document.templates.cv.v2.components.LabelValueRenderer;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.ProjectRenderer;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
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
import com.demcha.compose.document.templates.cv.v2.widgets.ProfileBand;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionModule;

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
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.nordicClean(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and explicit
     * Nordic-specific layout/colour options.
     *
     * @param theme   active theme
     * @param options Nordic-specific layout and colour options
     * @return ready-to-use template
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

        /** Normalises null rail side, accent, and rail fill to their defaults. */
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
         *
         * @return the default Nordic options
         */
        public static Options defaults() {
            return new Options(RailSide.LEFT, null, null, null);
        }

        /**
         * Starts a mutable builder for ergonomic colour overrides.
         *
         * @return new builder
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

            /**
             * Sets the side carrying the skills/education rail.
             *
             * @param value rail side
             * @return this builder
             */
            public Builder railSide(RailSide value) {
                this.railSide = value;
                return this;
            }

            /**
             * Sets the teal accent for rules, links, and the underline.
             *
             * @param value accent colour
             * @return this builder
             */
            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            /**
             * Sets the fill behind the rail column.
             *
             * @param value rail fill colour
             * @return this builder
             */
            public Builder railFillColor(DocumentColor value) {
                this.railFillColor = value;
                return this;
            }

            /**
             * Sets the profile band fill.
             *
             * @param value profile band fill colour
             * @return this builder
             */
            public Builder profileFillColor(DocumentColor value) {
                this.profileFillColor = value;
                return this;
            }

            /**
             * Builds the configured options.
             *
             * @return a new {@link Options} with the configured values
             */
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
            addProfile(flow, SectionLookup.firstMatching(sections, SUMMARY_KEYS));
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
                                    .text(MarkdownInline.plainText(
                                            doc.identity().jobTitle())
                                            .toUpperCase(java.util.Locale.ROOT))
                                    .textStyle(CvTextStyles.of(theme.typography().bodyFont(),
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

            ProfileBand.render(flow, "CvV2NordicCleanProfile", "PROFILE",
                    profile.body(), ProfileBand.Style.builder()
                            .spacing(4)
                            .padding(new DocumentInsets(8, 10, 8, 10))
                            .fillColor(options.resolvedProfileFill(theme))
                            .accentLeft(options.accentColor(), 3.0)
                            .cornerRadius(DocumentCornerRadius.right(4))
                            .titleStyle(sectionTitleStyle())
                            .bodyStyle(bodyStyle(7.85, theme.palette().ink()))
                            .bodyLineSpacing(1.25)
                            .build());
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
            addSkills(rail, SectionLookup.firstMatching(sections, SKILL_KEYS));
            addEducation(rail, SectionLookup.firstMatching(sections, EDUCATION_KEYS));
            addAdditional(rail, SectionLookup.firstMatching(sections, ADDITIONAL_KEYS));
        }

        private void addMain(SectionBuilder main, List<CvSection> sections) {
            main.spacing(9);
            addExperience(main, SectionLookup.firstMatching(sections, EXPERIENCE_KEYS));
            addProjects(main, SectionLookup.firstMatching(sections, PROJECT_KEYS));
        }

        private void addSkills(SectionBuilder parent, CvSection section) {
            if (!(section instanceof SkillsSection skills)
                    || skills.groups().isEmpty()) {
                return;
            }

            SectionModule.upperRule(parent, "CvV2NordicCleanSkills", "Skills",
                    theme, sectionTitleStyle(), options.accentColor(), 82,
                    host -> {
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

            SectionModule.upperRule(parent, "CvV2NordicCleanEducation",
                    "Education", theme, sectionTitleStyle(),
                    options.accentColor(), 82, host -> {
                for (CvEntry entry : education.entries()) {
                    EntryCompactRenderer.slashSubtitleDate(host, entry,
                            CvTextStyles.of(theme.typography().bodyFont(), 7.05,
                                    DocumentTextDecoration.BOLD,
                                    theme.palette().ink()),
                            bodyStyle(7.05, theme.palette().muted()),
                            bodyStyle(6.85, theme.palette().muted()),
                            1.05, DocumentInsets.bottom(2));
                }
            });
        }

        private void addAdditional(SectionBuilder parent, CvSection section) {
            if (!(section instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }

            SectionModule.upperRule(parent, "CvV2NordicCleanAdditional",
                    "Additional", theme, sectionTitleStyle(),
                    options.accentColor(), 82, host -> {
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

            SectionModule.upperRule(parent, "CvV2NordicCleanExperience",
                    "Experience", theme, sectionTitleStyle(),
                    options.accentColor(), 130, host -> {
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

            SectionModule.upperRule(parent, "CvV2NordicCleanProjects",
                    "Selected Projects", theme, sectionTitleStyle(),
                    options.accentColor(), 130, host -> {
                for (CvRow row : projects.rows()) {
                    addProject(host, row);
                }
            });
        }

        private void addWorkEntry(SectionBuilder host, CvEntry entry) {
            EntryCompactRenderer.titleDateBody(host, entry,
                    theme.entryTitleStyle(),
                    CvTextStyles.of(theme.typography().bodyFont(),
                            theme.typography().sizeEntryDate(),
                            DocumentTextDecoration.BOLD,
                            options.accentColor()),
                    theme.entrySubtitleStyle(),
                    theme.bodyStyle(),
                    " / ",
                    1.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    DocumentInsets.bottom(5),
                    theme.typography().bodyLineSpacing(),
                    false);
        }

        private void addProject(SectionBuilder host, CvRow row) {
            ProjectRenderer.inline(host, row,
                    CvTextStyles.of(theme.typography().bodyFont(), 7.35,
                            DocumentTextDecoration.BOLD, theme.palette().ink()),
                    bodyStyle(6.95, theme.palette().muted()),
                    bodyStyle(7.2, theme.palette().ink()),
                    1.08, DocumentInsets.bottom(3));
        }

        private void addLabelValueLine(SectionBuilder host, String label,
                                       String value, double size,
                                       double lineSpacing) {
            DocumentTextStyle labelStyle = CvTextStyles.of(theme.typography().bodyFont(),
                    size, DocumentTextDecoration.BOLD, theme.palette().ink());
            DocumentTextStyle valueStyle = bodyStyle(size,
                    theme.palette().muted());

            LabelValueRenderer.render(host, label, value, labelStyle,
                    valueStyle, lineSpacing, DocumentInsets.bottom(1.5));
        }

        private DocumentTextStyle headlineStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sectionTitleStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    options.accentColor());
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
                    options.accentColor());
        }

        private DocumentTextStyle bodyStyle(double size, DocumentColor color) {
            return CvTextStyles.of(theme.typography().bodyFont(), size,
                    DocumentTextDecoration.DEFAULT, color);
        }
    }
}
