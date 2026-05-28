package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.ProjectLabel;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v2 port of the legacy "Sidebar Portrait" CV preset.
 *
 * <p>Two-column resume with a pale-beige portrait sidebar on the
 * left (circular photo, contact stack with inline icons, education /
 * key skills / languages summary) and the main career narrative on
 * the right (large serif name, professional profile, experience
 * timeline of bold position + subtitle + description). Visual
 * signature ported from the v1
 * {@code SidebarPortraitCvTemplateComposer}: Crimson Text serif for
 * the hero name, Lato body, restrained grey palette.</p>
 *
 * <p>The two-column page chrome is painted by
 * {@link com.demcha.compose.document.api.DocumentSession#pageBackgrounds},
 * so the sidebar fill stretches edge-to-edge on every page (including
 * continuation pages of multi-page CVs) without any preset-side
 * filler logic. Use {@link Options} to override the sidebar fill,
 * main fill or accent colour without forking the theme.</p>
 */
public final class SidebarPortrait {

    /** Stable template identifier. */
    public static final String ID = "sidebar-portrait";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Sidebar Portrait";

    /** Recommended page margin (in points) — 0 so the sidebar bleeds to the edge. */
    public static final double RECOMMENDED_MARGIN = 0.0;

    /** Ratio of the page width allocated to the left sidebar column. */
    private static final double SIDEBAR_WIDTH_RATIO = 0.34;

    /** V1 default mid-grey accent used for the divider rule under sidebar headings. */
    private static final DocumentColor DEFAULT_ACCENT =
            DocumentColor.rgb(106, 106, 106);

    private static final double SIDEBAR_INNER_WIDTH = 156.4;
    private static final double PHOTO_DIAMETER = 98.0;

    /**
     * Vertical offset of the hero strip from the top of the main
     * column. Tuned so the hero strip's vertical centre lines up with
     * the photo's centre, producing the half-on-sidebar / half-on-hero
     * portrait effect.
     */
    private static final double HERO_TOP_OFFSET = 70.0;

    /** Width of the divider rule above each sidebar heading. */
    private static final double SIDEBAR_HEADER_RULE_WIDTH = 50.0;
    private static final double MAIN_SECTION_RULE_WIDTH = 346.0;

    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 5;
    private static final int LANGUAGE_LIMIT = 3;
    private static final int EXPERIENCE_LIMIT = 2;

    private static final String TEMPLATE_ASSET_ROOT =
            "/templates/cv/sidebar-portrait/";
    private static final String CONTACT_ICON_ROOT =
            TEMPLATE_ASSET_ROOT + "icons/";
    private static final String PORTRAIT_FILE = "portrait.png";
    private static final Map<String, byte[]> ASSET_CACHE =
            new ConcurrentHashMap<>();

    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "technical skills");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "additional information", "additional");
    private static final List<String> SUMMARY_KEYS =
            List.of("profile", "professional profile", "summary",
                    "professional summary");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "professional experience",
                    "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "selected projects");

    /**
     * Maximum number of project rows rendered in the main column.
     *
     * <p>The side-by-side body is wrapped in a {@code flow.addRow},
     * which is atomic by engine contract (see {@code RowBuilder}'s
     * error message: <em>"tables are splittable and would conflict
     * with the row's atomic pagination"</em>). That means the whole
     * sidebar + main row has to fit on a single page — content
     * overflow raises {@code AtomicNodeTooLargeException} instead of
     * page-breaking. Capping projects keeps the dense canonical
     * sample data inside the page bound; richer CVs that genuinely
     * need page-breaking sidebar layouts will need a separate
     * preset wired against a future splittable-row engine primitive.</p>
     */
    private static final int PROJECT_LIMIT = 2;

    private SidebarPortrait() {
    }

    /**
     * Builds the preset with its Sidebar Portrait theme and default
     * options (theme's banner fill for the sidebar, white for the
     * main column, mid-grey accent rule).
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.sidebarPortrait(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and default
     * options.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with explicit colour options. Use this to
     * override the sidebar fill, main fill or accent colour without
     * forking the theme.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme,
                                                      Options options) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(options, "options");
        return new Template(theme, options);
    }

    /**
     * Sidebar Portrait customisation knobs. {@code null} fields fall
     * back to the V1 defaults documented on each accessor.
     *
     * @param sidebarFillColor sidebar column fill; {@code null} →
     *                         {@code theme.palette().banner()}
     * @param mainFillColor    main column fill; {@code null} →
     *                         {@link DocumentColor#WHITE}
     * @param accentColor      divider rule colour above each sidebar
     *                         heading; {@code null} → V1 rgb(106,106,106)
     */
    public record Options(DocumentColor sidebarFillColor,
                          DocumentColor mainFillColor,
                          DocumentColor accentColor) {

        public static Options defaults() {
            return new Options(null, null, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private DocumentColor sidebarFillColor;
            private DocumentColor mainFillColor;
            private DocumentColor accentColor;

            private Builder() {
            }

            public Builder sidebarFillColor(DocumentColor value) {
                this.sidebarFillColor = value;
                return this;
            }

            public Builder mainFillColor(DocumentColor value) {
                this.mainFillColor = value;
                return this;
            }

            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            public Options build() {
                return new Options(sidebarFillColor, mainFillColor,
                        accentColor);
            }
        }
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;
        private final DocumentColor sidebarFill;
        private final DocumentColor mainFill;
        private final DocumentColor accent;

        Template(CvTheme theme, Options options) {
            this.theme = theme;
            this.sidebarFill = options.sidebarFillColor() != null
                    ? options.sidebarFillColor()
                    : theme.palette().banner();
            this.mainFill = options.mainFillColor() != null
                    ? options.mainFillColor()
                    : DocumentColor.WHITE;
            this.accent = options.accentColor() != null
                    ? options.accentColor()
                    : DEFAULT_ACCENT;
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

            // Paint the two-column chrome via pageBackgrounds — the
            // engine emits both fills on every page automatically, so
            // overflow content on page 2+ keeps the same visual
            // structure without any preset-side filler logic.
            document.pageBackgrounds(List.of(
                    PageBackgroundFill.leftColumn(SIDEBAR_WIDTH_RATIO,
                            sidebarFill),
                    PageBackgroundFill.rightColumn(1.0 - SIDEBAR_WIDTH_RATIO,
                            mainFill)));

            document.dsl()
                    .pageFlow()
                    .name("CvV2SidebarPortraitRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .padding(DocumentInsets.zero())
                    .addRow("CvV2SidebarPortraitBodyRow", row -> row
                            .spacing(0)
                            .weights(SIDEBAR_WIDTH_RATIO,
                                    1.0 - SIDEBAR_WIDTH_RATIO)
                            .addSection("CvV2SidebarPortraitSidebar",
                                    section -> addSidebar(section, doc,
                                            sections))
                            .addSection("CvV2SidebarPortraitMain",
                                    section -> {
                                        section.spacing(0)
                                                .padding(DocumentInsets.zero());
                                        addNameBlock(section, doc.identity());
                                        addMain(section, sections);
                                    }))
                    .build();
        }

        // -- Sidebar -------------------------------------------------------

        private void addSidebar(SectionBuilder section, CvDocument doc,
                                List<CvSection> sections) {
            // Sidebar section deliberately has no fillColor — the
            // pageBackgrounds emitted in compose() paint the pale fill
            // edge-to-edge on every page.
            section.spacing(9)
                    .padding(new DocumentInsets(54, 20, 45.45, 26));

            addPhotoBlock(section);
            addContactBlock(section, doc.identity());

            CvSection education = SectionLookup.firstMatching(sections,
                    EDUCATION_KEYS);
            if (hasContent(education)) {
                addSidebarHeader(section, "Education");
                addEducationEntries(section, education);
            }

            CvSection skills = SectionLookup.firstMatching(sections, SKILL_KEYS);
            if (hasContent(skills)) {
                addSidebarHeader(section, "Key Skills");
                addSkillsList(section, skills);
            }

            CvSection languages = SectionLookup.firstMatching(sections,
                    LANGUAGE_KEYS);
            if (hasContent(languages)) {
                addSidebarHeader(section, "Languages");
                addLanguageList(section, languages);
            }
        }

        private void addPhotoBlock(SectionBuilder section) {
            double sideInset = Math.max(0.0,
                    (SIDEBAR_INNER_WIDTH - PHOTO_DIAMETER) / 2.0);
            section.addImage(image -> image
                    .name("CvV2SidebarPortraitPhoto")
                    .source(portraitImage())
                    .size(PHOTO_DIAMETER, PHOTO_DIAMETER)
                    .margin(new DocumentInsets(0, sideInset, 17, sideInset)));
        }

        private void addContactBlock(SectionBuilder section, CvIdentity identity) {
            List<ContactItem> items = contactItems(identity);
            if (items.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = contactStyle();
            for (ContactItem item : items) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.35)
                        .margin(DocumentInsets.top(3))
                        .link(item.linkOptions())
                        .rich(rich -> {
                            if (item.iconFile() != null) {
                                rich.image(contactIcon(item.iconFile()),
                                        10.0, 10.0,
                                        InlineImageAlignment.CENTER,
                                        0.0, item.linkOptions());
                                rich.style("  ", textStyle);
                            }
                            if (item.linkOptions() != null) {
                                rich.link(item.text(), item.linkOptions());
                            } else {
                                rich.style(item.text(), textStyle);
                            }
                        }));
            }
        }

        private void addSidebarHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addLine(line -> line
                    .horizontal(SIDEBAR_HEADER_RULE_WIDTH)
                    .color(accent)
                    .thickness(0.75)
                    .margin(new DocumentInsets(12, 0, 7, 0)));
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(sidebarHeaderStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.zero()));
        }

        private void addEducationEntries(SectionBuilder section,
                                         CvSection eduSection) {
            if (!(eduSection instanceof EntriesSection entries)) {
                return;
            }
            DocumentTextStyle headingStyle = sidebarEntryTitleStyle();
            DocumentTextStyle metaStyle = sidebarEntryMetaStyle();

            List<CvEntry> list = entries.entries();
            for (int i = 0; i < Math.min(list.size(), EDUCATION_LIMIT); i++) {
                CvEntry entry = list.get(i);
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(entry.title())
                                .toUpperCase(Locale.ROOT))
                        .textStyle(headingStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(6)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(MarkdownInline.plainText(entry.subtitle()))
                            .textStyle(metaStyle)
                            .align(TextAlign.LEFT)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(MarkdownInline.plainText(entry.date()))
                            .textStyle(metaStyle)
                            .align(TextAlign.LEFT)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
            }
        }

        private void addSkillsList(SectionBuilder section,
                                   CvSection skillSection) {
            if (!(skillSection instanceof SkillsSection skills)) {
                return;
            }
            DocumentTextStyle skillStyle = sidebarSkillStyle();
            List<String> tokens = skillTokens(skills);
            for (String token : tokens.stream().limit(SKILL_LIMIT).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(token))
                        .textStyle(skillStyle)
                        .lineSpacing(1.35)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(3)));
            }
        }

        private void addLanguageList(SectionBuilder section,
                                     CvSection langSection) {
            DocumentTextStyle nameStyle = sidebarLanguageNameStyle();
            DocumentTextStyle metaStyle = sidebarLanguageMetaStyle();
            List<String> items = languageItems(langSection);
            for (String item : items.stream().limit(LANGUAGE_LIMIT).toList()) {
                String text = MarkdownInline.plainText(item);
                int paren = text.indexOf('(');
                String langName = paren > 0
                        ? text.substring(0, paren).trim()
                        : text.trim();
                String level = paren > 0
                        ? text.substring(paren).trim()
                        : "";
                if (langName.isBlank()) {
                    continue;
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(nameStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(4))
                        .rich(rich -> {
                            rich.style(langName.toUpperCase(Locale.ROOT),
                                    nameStyle);
                            if (!level.isBlank()) {
                                rich.style("  " + level, metaStyle);
                            }
                        }));
            }
        }

        // -- Main column ---------------------------------------------------

        private void addNameBlock(SectionBuilder section, CvIdentity identity) {
            String displayName = identity == null
                    ? ""
                    : identity.name().full();
            String jobTitle = identity == null ? "" : identity.jobTitle();
            String subline = jobTitle == null || jobTitle.isBlank()
                    ? "Your Professional Title Goes Here"
                    : jobTitle;
            section.addSection("CvV2SidebarPortraitHero", hero -> hero
                    .fillColor(sidebarFill)
                    .padding(new DocumentInsets(8, 34, 6, 34))
                    .spacing(3)
                    .margin(DocumentInsets.top(HERO_TOP_OFFSET))
                    .addParagraph(paragraph -> paragraph
                            .text(displayName)
                            .textStyle(nameStyle())
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(subline))
                            .textStyle(subtitleStyle())
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero())));
        }

        private void addMain(SectionBuilder section, List<CvSection> sections) {
            section.addSection("CvV2SidebarPortraitContent", content -> {
                content.spacing(10)
                        .padding(new DocumentInsets(24, 34, 24, 34));

                CvSection profile = SectionLookup.firstMatching(sections,
                        SUMMARY_KEYS);
                if (hasContent(profile)) {
                    addMainSectionHeader(content, "Professional Profile");
                    addProfileBody(content, profile);
                }

                CvSection experience = SectionLookup.firstMatching(sections,
                        EXPERIENCE_KEYS);
                if (hasContent(experience)) {
                    addMainSectionHeader(content, "Experience");
                    addExperienceEntries(content, experience);
                }

                CvSection projects = SectionLookup.firstMatching(sections,
                        PROJECT_KEYS);
                if (hasContent(projects)) {
                    addMainSectionHeader(content, "Projects");
                    addProjectsList(content, projects);
                }
            });
        }

        private void addMainSectionHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(spacedUpper(title))
                    .textStyle(mainHeaderStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(8)));
            section.addLine(line -> line
                    .horizontal(MAIN_SECTION_RULE_WIDTH)
                    .color(theme.palette().rule())
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(2, 0, 7, 0)));
        }

        private void addProfileBody(SectionBuilder section,
                                    CvSection profileSection) {
            if (!(profileSection instanceof ParagraphSection paragraphSection)) {
                return;
            }
            DocumentTextStyle base = mainBodyStyle();
            String body = paragraphSection.body();
            if (body == null || body.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .textStyle(base)
                    .lineSpacing(1.35)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(2))
                    .rich(rich -> MarkdownInline.appendTrimmed(rich, body, base)));
        }

        private void addExperienceEntries(SectionBuilder section,
                                          CvSection expSection) {
            if (!(expSection instanceof EntriesSection entries)) {
                return;
            }
            DocumentTextStyle positionStyle = mainEntryTitleStyle();
            DocumentTextStyle subtitleStyle = mainEntrySubtitleStyle();
            DocumentTextStyle bodyStyle = mainBodyStyle();

            List<CvEntry> list = entries.entries();
            for (int i = 0; i < Math.min(list.size(), EXPERIENCE_LIMIT); i++) {
                CvEntry entry = list.get(i);
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(entry.title())
                                .toUpperCase(Locale.ROOT))
                        .textStyle(positionStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(8)));

                String subtitle = composeSubtitle(entry);
                if (!subtitle.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(subtitle)
                            .textStyle(subtitleStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.body().isBlank()) {
                    String description = entry.body();
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle)
                            .lineSpacing(1.35)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2))
                            .rich(rich -> MarkdownInline.appendTrimmed(rich,
                                    description, bodyStyle)));
                }
            }
        }

        private static String composeSubtitle(CvEntry entry) {
            String sub = MarkdownInline.plainText(entry.subtitle());
            String date = MarkdownInline.plainText(entry.date());
            if (sub.isBlank()) {
                return date;
            }
            if (date.isBlank()) {
                return sub;
            }
            return sub + " | " + date;
        }

        /**
         * Renders the Projects section in the main column. Same visual
         * grammar as Profile / Experience — section heading + rule
         * via {@link #addMainSectionHeader}, then a stacked row per
         * project where each row carries a bold title, optional
         * italic stack context parsed by {@link ProjectLabel}, and a
         * body paragraph. Each project lives as separate paragraphs
         * inside the same flow, so the engine page-breaks naturally
         * between projects on multi-page CVs and the pageBackgrounds
         * keep the sidebar fill repeating on every continuation page.
         */
        private void addProjectsList(SectionBuilder section,
                                     CvSection projectSection) {
            if (!(projectSection instanceof RowsSection rows)) {
                return;
            }
            DocumentTextStyle titleStyle = mainProjectTitleStyle();
            DocumentTextStyle contextStyle = mainProjectContextStyle();
            DocumentTextStyle bodyStyle = mainBodyStyle();

            List<CvRow> list = rows.rows();
            for (int i = 0; i < Math.min(list.size(), PROJECT_LIMIT); i++) {
                CvRow row = list.get(i);
                ProjectLabel label = ProjectLabel.parse(row.label());
                String body = MarkdownInline.plainText(row.body());
                double topMargin = i == 0 ? 4.0 : 8.0;

                section.addParagraph(paragraph -> paragraph
                        .textStyle(titleStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(topMargin))
                        .rich(rich -> {
                            rich.style(label.title(), titleStyle);
                            if (!label.stack().isBlank()) {
                                rich.style(" (" + label.stack() + ")",
                                        contextStyle);
                            }
                        }));
                if (!body.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle)
                            .lineSpacing(1.35)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(2))
                            .rich(rich -> MarkdownInline.appendTrimmed(rich,
                                    body, bodyStyle)));
                }
            }
        }

        // -- Style factories ------------------------------------------------

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle subtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle contactStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarHeaderStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    10.8,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntryTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    8.4,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntryMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.8,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle sidebarSkillStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntrySubtitle(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarLanguageNameStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    8.1,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarLanguageMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.9,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle mainHeaderStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainEntryTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainEntrySubtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    9.2,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainProjectTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainProjectContextStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.ITALIC,
                    theme.palette().muted());
        }
    }

    // -- Static helpers ----------------------------------------------------

    private static boolean hasContent(CvSection section) {
        return section != null && SectionLookup.hasContent(section);
    }

    private static List<ContactItem> contactItems(CvIdentity identity) {
        if (identity == null) {
            return List.of();
        }
        List<ContactItem> items = new ArrayList<>();
        addContactItem(items, "phone.png", identity.contact().phone(), null);
        String email = identity.contact().email();
        if (!email.isBlank()) {
            addContactItem(items, "email.png", email,
                    new DocumentLinkOptions("mailto:" + email));
        }
        addContactItem(items, "location.png", identity.contact().address(),
                null);
        for (CvLink link : identity.links()) {
            String label = link.label();
            if (label.isBlank()) {
                continue;
            }
            String url = link.url();
            addContactItem(items, pickIconFile(label), label,
                    url.isBlank()
                            ? null
                            : new DocumentLinkOptions(url.trim()));
        }
        return List.copyOf(items);
    }

    private static void addContactItem(List<ContactItem> items,
                                       String iconFile, String text,
                                       DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            items.add(new ContactItem(iconFile, text.trim(), linkOptions));
        }
    }

    private static String pickIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "linkedin.png";
        }
        if (normalized.contains("github")) {
            return "github.png";
        }
        if (normalized.contains("dribbble")) {
            return "dribbble.png";
        }
        if (normalized.contains("google")) {
            return "google.png";
        }
        return "linkedin.png";
    }

    private static DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(
                ASSET_CACHE.computeIfAbsent(CONTACT_ICON_ROOT + iconFile,
                        SidebarPortrait::readAssetBytes));
    }

    private static DocumentImageData portraitImage() {
        return DocumentImageData.fromBytes(
                ASSET_CACHE.computeIfAbsent(TEMPLATE_ASSET_ROOT + PORTRAIT_FILE,
                        SidebarPortrait::readAssetBytes));
    }

    private static byte[] readAssetBytes(String resourcePath) {
        try (InputStream input = SidebarPortrait.class
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing sidebar portrait asset: " + resourcePath);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read sidebar portrait asset: " + resourcePath,
                    e);
        }
    }

    private static List<String> skillTokens(SkillsSection skills) {
        List<String> tokens = new ArrayList<>();
        for (SkillGroup group : skills.groups()) {
            String inline = MarkdownInline.plainText(group.skillsInline());
            for (String token : inline.split(",")) {
                String clean = token.trim();
                if (!clean.isBlank()) {
                    tokens.add(clean);
                }
            }
        }
        return tokens;
    }

    /**
     * Extracts language strings out of a section. Accepts either an
     * explicit {@code RowsSection} with a "Languages: ..." row or any
     * row whose body looks like an inline list, plus a fallback that
     * parses {@code SkillsSection.groups()} when languages are stored
     * as a single group inside the additional-information slot.
     */
    private static List<String> languageItems(CvSection section) {
        if (section == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (section instanceof RowsSection rows) {
            for (CvRow row : rows.rows()) {
                String label = MarkdownInline.plainText(row.label()).trim();
                String body = MarkdownInline.plainText(row.body()).trim();
                String lower = label.toLowerCase(Locale.ROOT);
                if (lower.contains("language") && !body.isBlank()) {
                    for (String part : body.split(",")) {
                        String p = part.trim();
                        if (!p.isBlank()) {
                            result.add(p);
                        }
                    }
                } else if (!label.isBlank()
                        && (body.contains("(") || body.contains("|"))) {
                    result.add(label + " " + body);
                }
            }
        } else if (section instanceof SkillsSection skills) {
            for (SkillGroup group : skills.groups()) {
                String inline = MarkdownInline.plainText(group.skillsInline());
                for (String part : inline.split(",")) {
                    String p = part.trim();
                    if (!p.isBlank()) {
                        result.add(p);
                    }
                }
            }
        }
        return result;
    }

    private static String spacedUpper(String value) {
        String upper = (value == null ? "" : value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            builder.append(current);
            if (Character.isLetterOrDigit(current)
                    && i + 1 < upper.length()
                    && Character.isLetterOrDigit(upper.charAt(i + 1))) {
                builder.append(' ');
            } else if (Character.isWhitespace(current)) {
                builder.append("  ");
            }
        }
        return builder.toString();
    }

    private record ContactItem(String iconFile, String text,
                               DocumentLinkOptions linkOptions) {
    }
}
