package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.core.identity.Link;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.page.ContinuationSafeArea;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.core.text.MarkdownInline;
import com.demcha.compose.document.templates.cv.components.ProjectLabel;
import com.demcha.compose.document.templates.cv.components.SectionAllocation;
import com.demcha.compose.document.templates.cv.components.SectionRouter;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.*;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
 *
 * <p>Both session-wide settings are set by {@code compose(...)} and replace
 * whatever the caller had set: the page backgrounds above, and a page-margin rule
 * that reserves {@link #CONTINUATION_TOP_SAFE_AREA} at the top of every page after
 * the first. The columns' padding is an edge of the columns rather than of each
 * page, so without that rule a body running past page one resumed at the trimmed
 * edge — the safe area a zero {@link #RECOMMENDED_MARGIN} gives up along the top
 * of the sheet as well as the sides.</p>
 *
 * <p>The body is a column flow, so each column continues on the next page and
 * the preset draws every entry of the sections it recognises. It used to be a
 * row — one atomic band that had to fit the page it started on — and carried
 * per-section caps (two jobs, two degrees, five skills, three languages, two
 * projects) to stay under that bound; the rest was dropped without a word. Both
 * are gone. A section this preset has no slot for is still dropped, as in every
 * preset that composes a fixed layout.</p>
 *
 * <p>The portrait geometry is sized for A4. A narrower page cannot hold it: the
 * photo is clipped to the sidebar column — at 420pt roughly a third of it
 * survives — and below about 310pt the column runs out of room altogether and
 * the layout fails rather than drawing. The clipping predates the column flow;
 * the failure is new, and replaces a page that came out with the portrait laid
 * over the main column.</p>
 */
public final class SidebarPortrait {

    /**
     * Stable template identifier.
     */
    public static final String ID = "sidebar-portrait";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Sidebar Portrait";

    /**
     * Recommended page margin (in points) — 0 so the sidebar bleeds to the edge.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    /**
     * Top inset (in points) the body reserves on the pages it continues onto.
     *
     * <p>Half an inch: the safe area every common printer's non-printable band
     * fits inside. {@link #RECOMMENDED_MARGIN} is zero so the sidebar fill reaches
     * the paper edge, and a zero margin gives up the safe area at the top of the
     * sheet along with the one at the sides — which only matters now that the body
     * paginates and there is a page whose top edge the columns' own padding does
     * not cover. Page one is unaffected: it opens where its own design puts it —
     * 54pt into the sidebar column, and 59pt into the main one, where the hero
     * strip's top margin starts.</p>
     *
     * @since 2.3.0
     */
    public static final double CONTINUATION_TOP_SAFE_AREA = 36.0;

    /**
     * Ratio of the page width allocated to the left sidebar column.
     */
    private static final double SIDEBAR_WIDTH_RATIO = 0.34;

    /**
     * V1 default mid-grey accent used for the divider rule under sidebar headings.
     */
    private static final DocumentColor DEFAULT_ACCENT =
            DocumentColor.rgb(106, 106, 106);

    /**
     * Contact glyph fill — a dark slate that reads clearly on the pale-beige
     * portrait sidebar fill. Recolours the shared {@link SvgGlyph} silhouettes
     * via {@code rich.shape(...)}.
     */
    private static final DocumentColor ICON_COLOR = DocumentColor.rgb(58, 58, 58);

    /**
     * Inner content width of the sidebar column. Derived from the V1
     * SidebarPortrait token set (sidebar outer width minus 13pt left +
     * 13pt right padding); preserves the half-on-sidebar /
     * half-on-hero portrait geometry against the canonical A4 page.
     */
    private static final double SIDEBAR_INNER_WIDTH = 156.4;

    /**
     * Diameter of the circular portrait photo. V1 SidebarPortrait
     * token — chosen so the photo's horizontal extent fits inside
     * {@link #SIDEBAR_INNER_WIDTH} with breathing room either side.
     */
    private static final double PHOTO_DIAMETER = 98.0;

    /**
     * Vertical offset of the hero strip from the top of the main
     * column. Tuned so the hero strip's vertical centre lines up with
     * the photo's centre, producing the half-on-sidebar / half-on-hero
     * portrait effect.
     *
     * <p>Paired with {@link #HERO_PADDING_TOP} / {@link #HERO_PADDING_BOTTOM}
     * — the offset is adjusted whenever the padding changes so the
     * strip's vertical centre stays on the same axis as the photo.</p>
     */
    private static final double HERO_TOP_OFFSET = 59.0;

    /**
     * Top / bottom padding inside the hero strip. The original V1
     * design used 8 / 6; the strip now renders 1.4× taller while
     * keeping the same on-page centre line via the adjusted
     * {@link #HERO_TOP_OFFSET}.
     */
    private static final double HERO_PADDING_TOP = 19.0;
    private static final double HERO_PADDING_BOTTOM = 17.0;

    /**
     * Width of the accent divider rule drawn above each sidebar
     * heading. V1 SidebarPortrait token — kept short so the rule
     * reads as a tick mark, not a separator line.
     */
    private static final double SIDEBAR_HEADER_RULE_WIDTH = 50.0;

    /**
     * Vertical spacing inside the sidebar column and inside the main column's
     * content section. The heading groups repeat these so wrapping a heading in
     * its own section leaves the rhythm exactly as it was.
     */
    private static final double SIDEBAR_SPACING = 9.0;
    private static final double MAIN_CONTENT_SPACING = 10.0;

    private static final String TEMPLATE_ASSET_ROOT =
            "/templates/cv/sidebar-portrait/";
    private static final String CONTACT_ICON_ROOT =
            TEMPLATE_ASSET_ROOT + "icons/";
    private static final String PORTRAIT_FILE = "portrait.svg";
    private static final Map<String, SvgIcon> PORTRAIT_CACHE =
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

    private SidebarPortrait() {
    }

    /**
     * Builds the preset with its Sidebar Portrait theme and default
     * options (theme's banner fill for the sidebar, white for the
     * main column, mid-grey accent rule).
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(BrandTheme.sidebarPortrait(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and default
     * options.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(BrandTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with explicit colour options. Use this to
     * override the sidebar fill, main fill or accent colour without
     * forking the theme.
     *
     * @param theme   active theme
     * @param options sidebar colour options
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(BrandTheme theme,
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

        /**
         * Default options: every field unset, falling back to the V1
         * defaults.
         *
         * @return options that leave the committed look unchanged
         */
        public static Options defaults() {
            return new Options(null, null, null);
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

            private Builder() {
            }

            /**
             * Sets the sidebar column fill.
             *
             * @param value sidebar fill colour
             * @return this builder
             */
            public Builder sidebarFillColor(DocumentColor value) {
                this.sidebarFillColor = value;
                return this;
            }

            /**
             * Sets the main column fill.
             *
             * @param value main fill colour
             * @return this builder
             */
            public Builder mainFillColor(DocumentColor value) {
                this.mainFillColor = value;
                return this;
            }

            /**
             * Sets the divider rule colour above each sidebar heading.
             *
             * @param value accent colour
             * @return this builder
             */
            public Builder accentColor(DocumentColor value) {
                this.accentColor = value;
                return this;
            }

            /**
             * Builds the configured options.
             *
             * @return a new {@link Options} with the configured colours
             */
            public Options build() {
                return new Options(sidebarFillColor, mainFillColor,
                        accentColor);
            }
        }
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final BrandTheme theme;
        private final DocumentColor sidebarFill;
        private final DocumentColor mainFill;
        private final DocumentColor accent;

        Template(BrandTheme theme, Options options) {
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

            SectionAllocation allocation =
                    SectionAllocation.of(doc.sectionsIn(Slot.MAIN));

            // Paint the two-column chrome via pageBackgrounds — the
            // engine emits both fills on every page automatically, so
            // overflow content on page 2+ keeps the same visual
            // structure without any preset-side filler logic.
            document.pageBackgrounds(List.of(
                    PageBackgroundFill.leftColumn(SIDEBAR_WIDTH_RATIO,
                            sidebarFill),
                    PageBackgroundFill.rightColumn(1.0 - SIDEBAR_WIDTH_RATIO,
                            mainFill)));

            // The columns' padding is an edge of the columns, not of each page, so
            // it holds the first page's content off the trimmed edge and says
            // nothing about the second's. Reserve the safe area the zero page
            // margin gave up, on the pages the body continues onto only. Derived
            // from the caller's own margin, so a caller who chose a margin of their
            // own keeps it and a caller who already clears 36pt gets no rule at all.
            ContinuationSafeArea.applyTo(document, 2, CONTINUATION_TOP_SAFE_AREA);

            document.dsl()
                    .pageFlow()
                    .name("CvV2SidebarPortraitRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .padding(DocumentInsets.zero())
                    // A column flow rather than a row: both columns keep
                    // flowing onto the next page instead of having to fit the
                    // first one. The page backgrounds above already repeat the
                    // two fills per page, so a continuation page looks like the
                    // page it continues.
                    .addColumnFlow("CvV2SidebarPortraitBody", body -> body
                            .gap(0)
                            .weights(SIDEBAR_WIDTH_RATIO,
                                    1.0 - SIDEBAR_WIDTH_RATIO)
                            .addColumn("CvV2SidebarPortraitSidebar",
                                    section -> addSidebar(section, doc,
                                            allocation))
                            .addColumn("CvV2SidebarPortraitMain",
                                    section -> {
                                        section.spacing(0)
                                                .padding(DocumentInsets.zero());
                                        addNameBlock(section, doc.identity());
                                        addMain(section, allocation);
                                    }))
                    .build();
        }

        // -- Sidebar -------------------------------------------------------

        private void addSidebar(SectionBuilder section, CvDocument doc,
                                SectionAllocation allocation) {
            // Sidebar section deliberately has no fillColor — the
            // pageBackgrounds emitted in compose() paint the pale fill
            // edge-to-edge on every page.
            section.spacing(SIDEBAR_SPACING)
                    .padding(new DocumentInsets(54, 20, 45.45, 26));

            addPhotoBlock(section);
            addContactBlock(section, doc.identity());

            CvSection education = allocation.entries(SectionRole.EDUCATION, EDUCATION_KEYS);
            if (hasContent(education)) {
                addSidebarHeader(section, "Education");
                addEducationEntries(section, education);
            }

            CvSection skills = allocation.skills(SectionRole.SKILLS, SKILL_KEYS);
            if (hasContent(skills)) {
                addSidebarHeader(section, "Key Skills");
                addSkillsList(section, skills);
            }

            CvSection languages = allocation.rows(SectionRole.LANGUAGES, LANGUAGE_KEYS, RowStyle.PLAIN);
            if (hasContent(languages)) {
                addSidebarHeader(section, "Languages");
                addLanguageList(section, languages);
            }
        }

        private void addPhotoBlock(SectionBuilder section) {
            double sideInset = Math.max(0.0,
                    (SIDEBAR_INNER_WIDTH - PHOTO_DIAMETER) / 2.0);
            // Default avatar is the bundled portrait.svg, whose outermost
            // layer is a full-frame filled circle, so the illustration is
            // already round at PHOTO_DIAMETER — no extra circular clip is
            // needed. Wrapped in a layer stack so the photo keeps its
            // centred side insets + 17pt bottom margin (addSvgIcon has no
            // margin overload). A user-supplied override is a follow-up.
            section.addLayerStack(photo -> photo
                    .name("CvV2SidebarPortraitPhoto")
                    .margin(new DocumentInsets(0, sideInset, 17, sideInset))
                    .layer(portraitIcon().node(PHOTO_DIAMETER)));
        }

        /**
         * Renders the icon + label contact stack in the sidebar.
         *
         * <p>Inlined instead of delegating to a shared
         * {@code ContactLine} variant because none of the existing
         * widgets carry an inline PNG icon followed by the text label
         * on the same baseline — every shared variant assumes either
         * pipe-separated text or a stacked link list with no glyph.
         * If a second preset ever needs the same icon-driven contact
         * stack, extract this into
         * {@code cv/widgets/IconContactLine}.</p>
         */
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
                                rich.shape(glyph(item.iconFile()).outline(10.0),
                                        ICON_COLOR, null,
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

        /**
         * Rule + heading as one keep-with-next group, so the pair travels to the
         * next page rather than closing this one with a heading whose block is
         * overleaf. A row could not break at all; a column can, which is what
         * makes the stranding reachable.
         */
        private void addSidebarHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addSection("CvV2SidebarPortraitSidebarHeading", heading -> heading
                    .spacing(SIDEBAR_SPACING)
                    .keepWithNext()
                    .addLine(line -> line
                            .horizontal(SIDEBAR_HEADER_RULE_WIDTH)
                            .color(accent)
                            .thickness(0.75)
                            .margin(new DocumentInsets(12, 0, 7, 0)))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(title))
                            .textStyle(sidebarHeaderStyle())
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero())));
        }

        private void addEducationEntries(SectionBuilder section,
                                         CvSection eduSection) {
            if (!(eduSection instanceof EntriesSection entries)) {
                return;
            }
            DocumentTextStyle headingStyle = sidebarEntryTitleStyle();
            DocumentTextStyle metaStyle = sidebarEntryMetaStyle();

            for (CvEntry entry : entries.entries()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(headingStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.top(6))
                        .rich(rich -> MarkdownInline.appendUpperCased(rich, entry.title(), headingStyle)));
                if (!entry.subtitle().isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .textStyle(metaStyle)
                            .align(TextAlign.LEFT)
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero())
                            .rich(rich -> MarkdownInline.append(rich, entry.subtitle(), metaStyle)));
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
            for (String token : skillTokens(skills)) {
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
            for (String item : languageItems(langSection)) {
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
                    .padding(new DocumentInsets(HERO_PADDING_TOP, 34,
                            HERO_PADDING_BOTTOM, 34))
                    .spacing(3)
                    .margin(DocumentInsets.top(HERO_TOP_OFFSET))
                    // Name is rendered inline rather than via
                    // Headline.uppercaseCentered because that widget
                    // calls host.padding(theme.spacing().headlinePadding())
                    // which would overwrite the hero strip's
                    // carefully-tuned HERO_PADDING_TOP / BOTTOM and
                    // break the on-axis alignment with the photo.
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

        private void addMain(SectionBuilder section, SectionAllocation allocation) {
            section.addSection("CvV2SidebarPortraitContent", content -> {
                content.spacing(MAIN_CONTENT_SPACING)
                        .padding(new DocumentInsets(24, 34, 24, 34));

                CvSection profile = allocation.paragraph(SectionRole.SUMMARY, SUMMARY_KEYS);
                if (hasContent(profile)) {
                    addMainSectionHeader(content, "Professional Profile");
                    addProfileBody(content, profile);
                }

                CvSection experience = allocation.entries(SectionRole.EXPERIENCE, EXPERIENCE_KEYS);
                if (hasContent(experience)) {
                    addMainSectionHeader(content, "Experience");
                    addExperienceEntries(content, experience);
                }

                CvSection projects = allocation.rows(SectionRole.PROJECTS, PROJECT_KEYS, RowStyle.BULLETED_STACKED);
                if (hasContent(projects)) {
                    addMainSectionHeader(content, "Projects");
                    addProjectsList(content, projects);
                }

                // Whatever no slot claimed — an "Awards", a "Publications", a
                // module the catalogue has no role for — under the title its
                // author wrote. This preset has a place for six categories and a
                // CV is not obliged to have exactly those; before the body
                // paginated there was nowhere to put the rest, and dropping it
                // looked like a finished page.
                for (CvSection leftover : allocation.remaining()) {
                    CvSection shaped = SectionRouter.naturalShape(leftover);
                    // A module with items but nothing in them lowers to an empty
                    // shape; a heading over nothing is worse than the drop.
                    if (!hasContent(shaped)) {
                        continue;
                    }
                    addMainSectionHeader(content, leftover.title());
                    addLeftoverBody(content, shaped);
                }
            });
        }

        /**
         * Draws a section this preset has no slot for, in the shape its author
         * chose, using the main column's own renderers.
         */
        private void addLeftoverBody(SectionBuilder content, CvSection shaped) {
            if (shaped instanceof ParagraphSection) {
                addProfileBody(content, shaped);
            } else if (shaped instanceof EntriesSection) {
                addExperienceEntries(content, shaped);
            } else if (shaped instanceof RowsSection) {
                addProjectsList(content, shaped);
            } else if (shaped instanceof SkillsSection skills) {
                // Grouped skills read as one labelled row per category, which is
                // what the projects renderer draws.
                List<CvRow> rows = new ArrayList<>();
                for (SkillGroup group : skills.groups()) {
                    rows.add(new CvRow(group.category(), group.skillsInline()));
                }
                addProjectsList(content, new RowsSection(skills.title(), rows,
                        RowStyle.BULLETED_STACKED));
            }
        }

        /**
         * Title + rule as one keep-with-next group, so the pair moves to the next
         * page rather than closing this one with a heading whose body is
         * overleaf — reachable only now that the column breaks at all.
         */
        private void addMainSectionHeader(SectionBuilder section, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            section.addSection("CvV2SidebarPortraitMainHeading", heading -> heading
                    .spacing(MAIN_CONTENT_SPACING)
                    .keepWithNext()
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(title))
                            .textStyle(mainHeaderStyle())
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.top(8)))
                    .addLine(line -> line
                            // Fills the main column rather than carrying a width of
                            // its own: the fixed 346pt this used to draw was 21pt
                            // wider than the column's content box on A4 and wider
                            // still on a narrower page, and a row slot never said so.
                            // horizontal(0) first so thickness() still reserves the
                            // stroke's height — fill() alone leaves the box at 1pt
                            // and a thicker themed rule would bleed into the text.
                            .horizontal(0)
                            .fill()
                            .color(theme.palette().rule())
                            .thickness(theme.spacing().accentRuleWidth())
                            .margin(new DocumentInsets(2, 0, 7, 0))));
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

            for (CvEntry entry : entries.entries()) {
                section.addParagraph(paragraph -> paragraph
                        .textStyle(positionStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.top(8))
                        .rich(rich -> MarkdownInline.appendUpperCased(rich, entry.title(), positionStyle)));

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
            // Deliberately flattened: this experience subtitle shares one fused meta
            // line with the date, so an inline [label](url) is reduced to its label
            // text here (the entry title and the education subtitle still link).
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
            for (int i = 0; i < list.size(); i++) {
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
                            MarkdownInline.append(rich, label.title(), titleStyle);
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
            return TextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle subtitleStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle contactStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarHeaderStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    10.8,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntryTitleStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    8.4,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarEntryMetaStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    7.8,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle sidebarSkillStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntrySubtitle(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarLanguageNameStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    8.1,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle sidebarLanguageMetaStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    7.9,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle mainHeaderStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainBodyStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainEntryTitleStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainEntrySubtitleStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    9.2,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainProjectTitleStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle mainProjectContextStyle() {
            return TextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.ITALIC,
                    theme.palette().muted());
        }
    }

    // -- Static helpers ----------------------------------------------------

    private static boolean hasContent(CvSection section) {
        return SectionLookup.hasContent(section);
    }

    private static List<ContactItem> contactItems(CvIdentity identity) {
        if (identity == null) {
            return List.of();
        }
        List<ContactItem> items = new ArrayList<>();
        addContactItem(items, "phone.svg", identity.contact().phone(), null);
        String email = identity.contact().email();
        if (!email.isBlank()) {
            addContactItem(items, "email.svg", email,
                    new DocumentLinkOptions("mailto:" + email));
        }
        addContactItem(items, "location.svg", identity.contact().address(),
                null);
        for (Link link : identity.links()) {
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
            return "github.svg";
        }
        if (normalized.contains("dribbble")) {
            return "dribbble.svg";
        }
        if (normalized.contains("google")) {
            return "google.svg";
        }
        // LinkedIn and any other link → the LinkedIn glyph (V1 fallback).
        return "linkedin.svg";
    }

    private static SvgGlyph glyph(String iconFile) {
        return SvgGlyph.fromResource(CONTACT_ICON_ROOT + iconFile);
    }

    private static SvgIcon portraitIcon() {
        return PORTRAIT_CACHE.computeIfAbsent(TEMPLATE_ASSET_ROOT + PORTRAIT_FILE,
                SidebarPortrait::readSvgIcon);
    }

    private static SvgIcon readSvgIcon(String resourcePath) {
        try (InputStream input = SidebarPortrait.class
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing sidebar portrait asset: " + resourcePath);
            }
            return SvgIcon.parse(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8));
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
            if (result.isEmpty()) {
                // The sniffing above exists because this slot also accepts an
                // "Additional Information" section and has to pick the language
                // rows out of it. A section routed here by its role is entirely
                // languages, and nothing in it needs to look like one — without
                // this the block draws its heading over nothing, which is worse
                // than the drop it replaced.
                for (CvRow row : rows.rows()) {
                    String label = MarkdownInline.plainText(row.label()).trim();
                    String body = MarkdownInline.plainText(row.body()).trim();
                    if (label.isBlank() && body.isBlank()) {
                        continue;
                    }
                    result.add(body.isBlank() ? label : label + " " + body);
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
