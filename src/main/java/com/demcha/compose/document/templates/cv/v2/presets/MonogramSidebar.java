package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.*;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.*;
import com.demcha.compose.document.templates.cv.v2.data.*;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.font.FontName;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v2 port of the legacy "Monogram Sidebar" CV preset.
 *
 * <p>Two-column resume with a pale teal-grey sidebar carrying a
 * monogram badge (initials inside a dark ring), centered contact
 * icons, education and expertise blocks; the right column carries a
 * two-line letter-spaced headline plus the main career narrative.
 * Visual signature ported from the v1
 * {@code MonogramSidebarCvTemplateComposer}: Crimson Text headline,
 * PT Serif monogram, muted gold accent.</p>
 *
 * <p>The page uses a zero margin so the pale sidebar fill bleeds to
 * the page edge. Two-column page chrome is painted via
 * {@link com.demcha.compose.document.api.DocumentSession#pageBackgrounds},
 * which repeats automatically on every page — when content overflows
 * onto page 2 the sidebar tint and white main area continue without
 * any preset-side filler logic. The preset draws its visual ornaments
 * (monogram ring, section rules) inline because none of these visuals
 * are shared with another v2 preset today.</p>
 */
public final class MonogramSidebar {

    /**
     * Stable template identifier.
     */
    public static final String ID = "monogram-sidebar";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Monogram Sidebar";

    /**
     * Recommended page margin (in points) — 0 so the sidebar bleeds to the edge.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    /**
     * V1 default muted-gold accent — used for the subtitle, dates.
     */
    private static final DocumentColor DEFAULT_ACCENT =
            DocumentColor.rgb(158, 146, 104);

    /**
     * V1 default dark monogram ring + initials colour.
     */
    private static final DocumentColor DEFAULT_MONOGRAM_RING =
            DocumentColor.rgb(54, 62, 74);

    /**
     * V1 dark main-column rule colour (theme rule is sidebar-only).
     */
    private static final DocumentColor MAIN_RULE =
            DocumentColor.rgb(72, 79, 84);

    /**
     * PT Serif used only for the monogram initials inside the ring badge.
     */
    private static final FontName MONOGRAM_FONT = FontName.PT_SERIF;

    private static final double MONOGRAM_DIAMETER = 122;
    private static final double SIDEBAR_RULE_WIDTH = 118;
    private static final double CONTACT_ICON_SIZE = 22;

    /**
     * Sidebar column width as a fraction of the page width.
     */
    private static final double SIDEBAR_WIDTH_RATIO = 0.33;

    private static final double MAIN_SECTION_RULE_WIDTH = 355.0;

    private static final int EDUCATION_LIMIT = 2;
    private static final int SKILL_LIMIT = 7;
    private static final int EXPERIENCE_LIMIT = 2;
    private static final int PROJECT_LIMIT = 3;
    private static final int ADDITIONAL_LIMIT = 3;

    private static final String CONTACT_ICON_ROOT =
            "/templates/cv/monogram-sidebar/icons/";
    private static final Map<String, byte[]> CONTACT_ICON_CACHE =
            new ConcurrentHashMap<>();

    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "technical skills", "expertise");
    private static final List<String> SUMMARY_KEYS =
            List.of("profile", "professional profile", "summary",
                    "professional summary");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "professional experience",
                    "work");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional information", "additional");

    private MonogramSidebar() {
    }

    /**
     * Builds the preset with its Monogram Sidebar theme and default
     * options (theme's banner fill for the sidebar, muted-gold accent,
     * dark slate monogram ring).
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.monogramSidebar(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and default
     * options.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with explicit colour options. Use this to
     * override the sidebar fill, accent colour, or monogram ring
     * without forking the theme.
     *
     * @param theme   active theme
     * @param options sidebar colour options
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme,
                                                      Options options) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(options, "options");
        return new Template(theme, options);
    }

    /**
     * Monogram Sidebar customisation knobs. {@code null} fields fall
     * back to the theme palette / V1 defaults documented on each
     * accessor.
     *
     * @param sidebarFillColor  sidebar (left column) background fill;
     *                          {@code null} → {@code theme.palette().banner()}
     * @param mainFillColor     main (right column) background fill;
     *                          {@code null} → {@code theme.palette().mainFill()}
     *                          (defaults to {@link DocumentColor#WHITE})
     * @param accentColor       muted-gold accent for subtitle,
     *                          education date, experience date;
     *                          {@code null} → V1 rgb(158,146,104)
     * @param monogramRingColor ring stroke + initials colour;
     *                          {@code null} → V1 rgb(54,62,74)
     */
    public record Options(DocumentColor sidebarFillColor,
                          DocumentColor mainFillColor,
                          DocumentColor accentColor,
                          DocumentColor monogramRingColor) {

        /**
         * Default options: every field unset, falling back to the theme
         * palette / V1 defaults.
         *
         * @return options that leave the committed look unchanged
         */
        public static Options defaults() {
            return new Options(null, null, null, null);
        }

        /**
         * Starts a mutable builder for the sidebar colour knobs.
         *
         * @return new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link Options}.
         */
        public static final class Builder {
            private DocumentColor sidebarFillColor;
            private DocumentColor mainFillColor;
            private DocumentColor accentColor;
            private DocumentColor monogramRingColor;

            private Builder() {
            }

            /**
             * Sets the sidebar (left column) background fill.
             *
             * @param value sidebar fill colour
             * @return this builder
             */
            public Builder sidebarFillColor(DocumentColor value) {
                this.sidebarFillColor = value;
                return this;
            }

            /**
             * Sets the main (right column) background fill.
             *
             * @param value main fill colour
             * @return this builder
             */
            public Builder mainFillColor(DocumentColor value) {
                this.mainFillColor = value;
                return this;
            }

            /**
             * Sets the muted-gold accent for the subtitle and dates.
             *
             * @param value accent colour
             * @return this builder
             */
            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            /**
             * Sets the monogram ring stroke and initials colour.
             *
             * @param value monogram ring colour
             * @return this builder
             */
            public Builder monogramRingColor(DocumentColor value) {
                this.monogramRingColor = value;
                return this;
            }

            /**
             * Builds the configured options.
             *
             * @return a new {@link Options} with the configured colours
             */
            public Options build() {
                return new Options(sidebarFillColor, mainFillColor,
                        accentColor, monogramRingColor);
            }
        }
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;
        private final DocumentColor sidebarFill;
        private final DocumentColor mainFill;
        private final DocumentColor accent;
        private final DocumentColor monogramRing;

        Template(CvTheme theme, Options options) {
            this.theme = theme;
            this.sidebarFill = options.sidebarFillColor() != null
                    ? options.sidebarFillColor()
                    : theme.palette().banner();
            this.mainFill = options.mainFillColor() != null
                    ? options.mainFillColor()
                    : theme.palette().mainFill();
            this.accent = options.accentColor() != null
                    ? options.accentColor()
                    : DEFAULT_ACCENT;
            this.monogramRing = options.monogramRingColor() != null
                    ? options.monogramRingColor()
                    : DEFAULT_MONOGRAM_RING;
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

            double pageInnerWidth = document.canvas().innerWidth();
            double sidebarOuterWidth = pageInnerWidth * 0.33;
            double sidebarHorizontalPadding = 13.0 * 2.0;
            double sidebarInnerWidth = Math.max(0.0,
                    sidebarOuterWidth - sidebarHorizontalPadding);
            double mainOuterWidth = pageInnerWidth - sidebarOuterWidth;
            // Main section has 20pt left + 18pt right padding (see addMain).
            // Spacer width must be the content-area width so the inner
            // content fills exactly the section's allocated outer width
            // (= mainOuterWidth) — passing the outer width directly
            // would overflow because outer = content + padding.
            double mainContentWidth = Math.max(0.0,
                    mainOuterWidth - (20.0 + 18.0));
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
                    .name("CvV2MonogramSidebarRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .padding(DocumentInsets.zero())
                    .addRow("CvV2MonogramSidebarFrame", row -> row
                            .spacing(0)
                            .weights(SIDEBAR_WIDTH_RATIO,
                                    1.0 - SIDEBAR_WIDTH_RATIO)
                            .addSection("CvV2MonogramSidebarSidebar",
                                    section -> addSidebar(section, doc, sections,
                                            sidebarInnerWidth))
                            .addSection("CvV2MonogramSidebarMain",
                                    section -> addMain(section, doc.identity(),
                                            sections, mainContentWidth)))
                    .build();
        }

        // -- Sidebar -------------------------------------------------------

        private void addSidebar(SectionBuilder section, CvDocument doc,
                                List<CvSection> sections, double innerWidth) {
            // Sidebar section deliberately has no fillColor — the
            // pageBackgrounds emitted in compose() paint the pale fill
            // edge-to-edge on every page, including continuation
            // pages. Top padding establishes the breathing room above
            // the monogram badge.
            section.spacing(8)
                    .padding(new DocumentInsets(36, 13, 0, 13));

            addMonogramBlock(section, initials(doc.identity().name()),
                    innerWidth);

            addSidebarHeader(section, "CONTACT", innerWidth);
            addContactBlock(section, doc.identity());

            CvSection education = SectionLookup.firstMatching(sections,
                    EDUCATION_KEYS);
            if (hasContent(education)) {
                addSidebarHeader(section, education.title(), innerWidth);
                addEducationEntries(section, education);
            }

            CvSection skills = SectionLookup.firstMatching(sections, SKILL_KEYS);
            if (hasContent(skills)) {
                addSidebarHeader(section, "EXPERTISE", innerWidth);
                addSkillsList(section, skills);
            }
        }

        private void addMonogramBlock(SectionBuilder section,
                                      String initialsText, double innerWidth) {
            LayerStackNode badge = new LayerStackBuilder()
                    .name("CvV2MonogramSidebarBadge")
                    .back(new EllipseBuilder()
                            .name("CvV2MonogramSidebarRing")
                            .size(MONOGRAM_DIAMETER, MONOGRAM_DIAMETER)
                            .stroke(DocumentStroke.of(monogramRing, 1.25))
                            .build())
                    .layer(new ParagraphBuilder()
                            .name("CvV2MonogramSidebarInitials")
                            .text(initialsText)
                            .textStyle(CvTextStyles.of(MONOGRAM_FONT, 44.0,
                                    DocumentTextDecoration.BOLD, monogramRing))
                            .align(TextAlign.LEFT)
                            .build(), LayerAlign.CENTER)
                    .build();

            section.addLayerStack(outer -> outer
                    .name("CvV2MonogramSidebarFrame")
                    .margin(DocumentInsets.bottom(42))
                    .back(new SpacerNode(
                            "CvV2MonogramSidebarSpace",
                            Math.max(MONOGRAM_DIAMETER, innerWidth),
                            MONOGRAM_DIAMETER,
                            DocumentInsets.zero(),
                            DocumentInsets.zero()))
                    .layer(badge, LayerAlign.TOP_CENTER));
        }

        private void addSidebarHeader(SectionBuilder section, String title,
                                      double innerWidth) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(TextOrnaments.spacedUpper(title))
                    .textStyle(sidebarHeaderStyle())
                    .align(TextAlign.CENTER)
                    .lineSpacing(1.2)
                    .margin(DocumentInsets.top(6)));
            double ruleWidth = Math.min(innerWidth, SIDEBAR_RULE_WIDTH);
            double sideInset = Math.max(0.0, (innerWidth - ruleWidth) / 2.0);
            section.addLine(line -> line
                    .horizontal(ruleWidth)
                    .color(theme.palette().rule())
                    .thickness(0.45)
                    .margin(new DocumentInsets(1, sideInset, 2, sideInset)));
        }

        private void addContactBlock(SectionBuilder section, CvIdentity identity) {
            List<ContactItem> items = contactItems(identity);
            if (items.isEmpty()) {
                return;
            }
            DocumentTextStyle textStyle = sidebarBodyStyle();
            for (ContactItem item : items) {
                if (item.iconFile() != null) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(textStyle)
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.top(4))
                            .rich(rich -> rich.image(
                                    contactIcon(item.iconFile()),
                                    CONTACT_ICON_SIZE,
                                    CONTACT_ICON_SIZE,
                                    InlineImageAlignment.CENTER,
                                    0.0,
                                    item.linkOptions())));
                }
                section.addParagraph(paragraph -> paragraph
                        .textStyle(textStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.zero())
                        .link(item.linkOptions())
                        .rich(rich -> {
                            if (item.linkOptions() != null) {
                                rich.link(item.text(), item.linkOptions());
                            } else {
                                rich.style(item.text(), textStyle);
                            }
                        }));
            }
        }

        private void addEducationEntries(SectionBuilder section,
                                         CvSection eduSection) {
            if (!(eduSection instanceof EntriesSection entries)) {
                return;
            }
            DocumentTextStyle headingStyle = sidebarEntryTitleStyle();
            DocumentTextStyle subStyle = sidebarEntrySubtitleStyle();
            DocumentTextStyle metaStyle = sidebarEntryDateStyle();

            List<CvEntry> list = entries.entries();
            for (int i = 0; i < Math.min(list.size(), EDUCATION_LIMIT); i++) {
                CvEntry entry = list.get(i);
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(entry.title())
                                .toUpperCase(Locale.ROOT))
                        .textStyle(headingStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(6)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(MarkdownInline.plainText(entry.subtitle()))
                            .textStyle(subStyle)
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(MarkdownInline.plainText(entry.date()))
                            .textStyle(metaStyle)
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero()));
                }
            }
        }

        private void addSkillsList(SectionBuilder section, CvSection skillSection) {
            if (!(skillSection instanceof SkillsSection skills)) {
                return;
            }
            DocumentTextStyle skillStyle = sidebarSkillStyle();
            List<String> tokens = skillTokens(skills);
            for (String token : tokens.stream().limit(SKILL_LIMIT).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(token))
                        .textStyle(skillStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.25)
                        .margin(DocumentInsets.top(1)));
            }
        }

        // -- Main column ---------------------------------------------------

        private void addMain(SectionBuilder section, CvIdentity identity,
                             List<CvSection> sections, double anchorWidth) {
            // No fillColor — the page background defaults to white, so
            // the right column is naturally white from top to bottom.
            section.spacing(5)
                    .padding(new DocumentInsets(38, 20, 24, 18));

            addNameBlock(section, identity);

            CvSection profile = SectionLookup.firstMatching(sections,
                    SUMMARY_KEYS);
            if (hasContent(profile)) {
                addMainSectionHeader(section,
                        profile.title().isBlank()
                                ? "Professional Profile"
                                : profile.title());
                addProfileBody(section, profile);
            }

            CvSection experience = SectionLookup.firstMatching(sections,
                    EXPERIENCE_KEYS);
            if (hasContent(experience)) {
                addMainSectionHeader(section,
                        experience.title().isBlank()
                                ? "Experience"
                                : experience.title());
                addExperienceEntries(section, experience);
            }

            CvSection projects = SectionLookup.firstMatching(sections,
                    PROJECT_KEYS);
            if (hasContent(projects)) {
                addMainSectionHeader(section,
                        projects.title().isBlank()
                                ? "Projects"
                                : projects.title());
                addProjectsList(section, projects);
            }

            CvSection additional = SectionLookup.firstMatching(sections,
                    ADDITIONAL_KEYS);
            if (hasContent(additional)) {
                addMainSectionHeader(section,
                        additional.title().isBlank()
                                ? "Additional Information"
                                : additional.title());
                addAdditionalList(section, additional);
            }
        }

        private void addProjectsList(SectionBuilder section,
                                     CvSection projectsSection) {
            if (!(projectsSection instanceof RowsSection rows)) {
                return;
            }
            DocumentTextStyle titleStyle = mainEntryTitleStyle();
            DocumentTextStyle stackStyle = mainEntryDateStyle();
            DocumentTextStyle bodyStyle = mainBodyStyle();
            List<CvRow> list = rows.rows();
            for (int i = 0; i < Math.min(list.size(), PROJECT_LIMIT); i++) {
                ProjectRenderer.inline(section, list.get(i),
                        titleStyle, stackStyle, bodyStyle,
                        theme.typography().bodyLineSpacing(),
                        DocumentInsets.top(4));
            }
        }

        private void addAdditionalList(SectionBuilder section,
                                       CvSection addSection) {
            if (!(addSection instanceof RowsSection rows)) {
                return;
            }
            DocumentTextStyle labelStyle = mainEntryTitleStyle();
            DocumentTextStyle valueStyle = mainBodyStyle();
            List<CvRow> list = rows.rows();
            for (int i = 0; i < Math.min(list.size(), ADDITIONAL_LIMIT); i++) {
                CvRow row = list.get(i);
                LabelValueRenderer.render(section, row.label(), row.body(),
                        labelStyle, valueStyle,
                        theme.typography().bodyLineSpacing(),
                        DocumentInsets.top(3));
            }
        }

        private void addNameBlock(SectionBuilder section, CvIdentity identity) {
            CvName name = identity == null ? CvName.of("", "") : identity.name();
            List<String> parts = new ArrayList<>();
            if (!name.first().isBlank()) {
                parts.add(name.first());
            }
            if (!name.last().isBlank()) {
                parts.add(name.last());
            }
            if (parts.isEmpty()) {
                parts.add("");
            }
            String jobTitle = identity == null ? "" : identity.jobTitle();
            String subline = jobTitle == null || jobTitle.isBlank()
                    ? "Your Professional Title"
                    : jobTitle;
            DocumentTextStyle nameStyle = nameStyle();
            DocumentTextStyle titleStyle = subtitleStyle();

            for (int index = 0; index < parts.size(); index++) {
                String part = parts.get(index);
                DocumentInsets margin = index == parts.size() - 1
                        ? DocumentInsets.zero()
                        : DocumentInsets.bottom(6);
                section.addParagraph(paragraph -> paragraph
                        .text(TextOrnaments.spacedUpper(part))
                        .textStyle(nameStyle)
                        .align(TextAlign.CENTER)
                        .lineSpacing(1.0)
                        .margin(margin));
            }
            section.addParagraph(paragraph -> paragraph
                    .text(TextOrnaments.spacedUpper(subline))
                    .textStyle(titleStyle)
                    .align(TextAlign.CENTER)
                    .margin(new DocumentInsets(12, 0, 22, 0)));
        }

        private void addMainSectionHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addParagraph(paragraph -> paragraph
                    .text(TextOrnaments.spacedUpper(title))
                    .textStyle(mainHeaderStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(6)));
            section.addLine(line -> line
                    .horizontal(MAIN_SECTION_RULE_WIDTH)
                    .color(MAIN_RULE)
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(1, 0, 4, 0)));
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
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .align(TextAlign.LEFT)
                    .margin(new DocumentInsets(4, 0, 12, 0))
                    .rich(rich -> MarkdownInline.appendTrimmed(rich, body, base)));
        }

        private void addExperienceEntries(SectionBuilder section,
                                          CvSection expSection) {
            if (!(expSection instanceof EntriesSection entries)) {
                return;
            }
            DocumentTextStyle positionStyle = mainEntryTitleStyle();
            DocumentTextStyle dateStyle = mainEntryDateStyle();
            DocumentTextStyle bodyStyle = mainBodyStyle();

            List<CvEntry> list = entries.entries();
            for (int i = 0; i < Math.min(list.size(), EXPERIENCE_LIMIT); i++) {
                CvEntry entry = list.get(i);
                section.addParagraph(paragraph -> paragraph
                        .text(MarkdownInline.plainText(entry.title())
                                .toUpperCase(Locale.ROOT))
                        .textStyle(positionStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.15)
                        .margin(DocumentInsets.top(5)));
                if (!entry.date().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(TextOrnaments.spacedUpper(
                                    MarkdownInline.plainText(entry.date())))
                            .textStyle(dateStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
                }
                if (!entry.body().isBlank()) {
                    String description = entry.body();
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(bodyStyle)
                            .lineSpacing(theme.typography().bodyLineSpacing())
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(1))
                            .rich(rich -> MarkdownInline.appendTrimmed(rich,
                                    description, bodyStyle)));
                }
            }
        }

        // -- Style factories ----------------------------------------------

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle subtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.BOLD,
                    accent);
        }

        private DocumentTextStyle sidebarHeaderStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    8.0,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle sidebarEntryTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.6,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntrySubtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntryDateStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.2,
                    DocumentTextDecoration.DEFAULT,
                    accent);
        }

        private DocumentTextStyle sidebarSkillStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
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

        private DocumentTextStyle mainEntryDateStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.BOLD,
                    accent);
        }
    }

    // -- Static helpers ----------------------------------------------------

    private static boolean hasContent(CvSection section) {
        return SectionLookup.hasContent(section);
    }

    private static String initials(CvName name) {
        if (name == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendInitial(builder, name.first());
        appendInitial(builder, name.last());
        return builder.toString();
    }

    private static void appendInitial(StringBuilder builder, String value) {
        if (builder.length() >= 2 || value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && Character.isLetter(trimmed.charAt(0))) {
            builder.append(Character.toUpperCase(trimmed.charAt(0)));
        }
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
        if (normalized.contains("github")) {
            return "github.png";
        }
        if (normalized.contains("linkedin")) {
            return "linkedin.png";
        }
        return "linkedin.png";
    }

    private static DocumentImageData contactIcon(String iconFile) {
        return DocumentImageData.fromBytes(
                CONTACT_ICON_CACHE.computeIfAbsent(CONTACT_ICON_ROOT + iconFile,
                        MonogramSidebar::readIconBytes));
    }

    private static byte[] readIconBytes(String resourcePath) {
        try (InputStream input = MonogramSidebar.class
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing monogram sidebar icon: " + resourcePath);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read monogram sidebar icon: " + resourcePath, e);
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

    private record ContactItem(String iconFile, String text,
                               DocumentLinkOptions linkOptions) {
    }
}
