package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.EntryCompactRenderer;
import com.demcha.compose.document.templates.cv.v2.components.ParagraphRenderer;
import com.demcha.compose.document.templates.cv.v2.components.RowRenderer;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.cv.v2.components.SkillsRenderer;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.widgets.CardWidget;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * v2 port of the legacy "Panel" CV preset.
 *
 * <p>Panel-led CV. The page is composed of four full-width cards of
 * equal width, all sharing the same shell (rounded corner, thin teal
 * stroke):</p>
 * <ol>
 *   <li><strong>Header card</strong> — pale-teal fill, centred Poppins
 *       UPPERCASE name, optional job title, centred meta + link
 *       line.</li>
 *   <li><strong>Profile card</strong> — full-width white card with
 *       UPPERCASE teal title, accent strip, and the summary
 *       paragraph.</li>
 *   <li><strong>Two-column row</strong> — left card stacks
 *       <em>Skills + Education</em>, right card stacks
 *       <em>Experience + Projects</em>. Each side is one card with
 *       internal sub-modules separated by a small vertical gap, so the
 *       page reads as four panels of consistent width.</li>
 *   <li><strong>Additional card</strong> — full-width closer with the
 *       same shell as Profile.</li>
 * </ol>
 *
 * <p>The preset stays a thin orchestrator. Every visual shell goes
 * through {@link CardWidget}; the module title + accent strip pair is
 * preset-local because no other v2 preset places the tick
 * <em>below</em> the title. Body rendering uses a preset-local
 * dispatcher (functionally equivalent to
 * {@code SectionDispatcher.renderBody}) that draws
 * {@link EntriesSection} headers as a single "title - date" paragraph
 * via {@link EntryCompactRenderer#titleDateBody} instead of the
 * standard {@code EntryRenderer}'s 2-column Row — the engine bans
 * nested horizontal rows, and the side cards sit inside the
 * {@code flow.addRow}.</p>
 */
public final class Panel {

    /** Stable template identifier. */
    public static final String ID = "panel";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Panel";

    /** Recommended page margin (in points) — matches V1 ProductLeader. */
    public static final double RECOMMENDED_MARGIN = 18.0;

    /** V1 ProductLeader deep navy used for the masthead text. */
    private static final DocumentColor HEADER_TEXT =
            DocumentColor.rgb(20, 44, 66);

    /** V1 ProductLeader teal accent used for module titles + links. */
    private static final DocumentColor ACCENT =
            DocumentColor.rgb(0, 128, 128);

    /** V1 ProductLeader white panel fill. */
    private static final DocumentColor PANEL_FILL = DocumentColor.WHITE;

    /** Width of the accent strip drawn under each module title. */
    private static final double ACCENT_STRIP_WIDTH = 54.0;

    /**
     * Stroke thickness shared by every Panel card (header, profile,
     * side modules, additional). Keeping a single value here is the
     * only knob that makes all panels render with visually identical
     * borders — diverging this between the header and the modules
     * leaks straight into the visible card outline width.
     */
    private static final double PANEL_STROKE_THICKNESS = 0.45;

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

    private Panel() {
    }

    /**
     * Builds the preset with its Panel theme.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.panel());
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

            // Pre-compute card widths so every panel renders at an
            // identical outer width regardless of content. Sections in
            // the v2 engine fit content min-width by default, so the
            // longest line in (say) Skills would otherwise push that
            // card wider than Education or Additional. The widthAnchor
            // spacer below pins each card's content area to the
            // pre-computed target.
            double innerWidth = document.canvas().innerWidth();
            double gap = theme.spacing().pageFlowSpacing();
            double cardPadding = theme.spacing().bannerInnerPadding();
            double fullCardContentWidth = innerWidth - 2 * cardPadding;
            double sideCardContentWidth =
                    (innerWidth - gap) / 2 - 2 * cardPadding;

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CvV2PanelRoot")
                    .spacing(gap);

            CvSection summary = SectionLookup.firstMatching(sections, SUMMARY_KEYS);
            CvSection skills = SectionLookup.firstMatching(sections, SKILL_KEYS);
            CvSection education = SectionLookup.firstMatching(sections, EDUCATION_KEYS);
            CvSection experience = SectionLookup.firstMatching(sections, EXPERIENCE_KEYS);
            CvSection projects = SectionLookup.firstMatching(sections, PROJECT_KEYS);
            CvSection additional = SectionLookup.firstMatching(sections, ADDITIONAL_KEYS);

            addHeader(flow, doc.identity(), fullCardContentWidth);
            addFullWidthPanel(flow, "Profile", "Profile", summary,
                    fullCardContentWidth);

            // Left column = three separate cards (Skills, Education,
            // Additional). Right column = two separate cards (Experience,
            // Projects). Every card is anchored to sideCardContentWidth
            // so all panels in each column are visually identical width.
            boolean leftHasContent = hasContent(skills) || hasContent(education)
                    || hasContent(additional);
            boolean rightHasContent = hasContent(experience) || hasContent(projects);
            if (leftHasContent || rightHasContent) {
                flow.addRow("CvV2PanelStacked", row -> row
                        .spacing(gap)
                        .weights(1.0, 1.0)
                        .addSection("CvV2PanelStackedLeft", left -> {
                            left.spacing(gap);
                            addSidePanel(left, "Skills", "Skills",
                                    skills, sideCardContentWidth);
                            addSidePanel(left, "Education", "Education",
                                    education, sideCardContentWidth);
                            addSidePanel(left, "Additional", "Additional",
                                    additional, sideCardContentWidth);
                        })
                        .addSection("CvV2PanelStackedRight", right -> {
                            right.spacing(gap);
                            addSidePanel(right, "Experience", "Experience",
                                    experience, sideCardContentWidth);
                            addSidePanel(right, "Projects", "Projects",
                                    projects, sideCardContentWidth);
                        }));
            }

            flow.build();
        }

        private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                               double anchorWidth) {
            CardWidget.render(flow, "CvV2PanelHeader",
                    headerStyle(),
                    card -> {
                        widthAnchor(card, anchorWidth);
                        // Inline the name paragraph instead of going through
                        // Headline.uppercaseCentered — that widget calls
                        // host.padding(theme.spacing.headlinePadding()) which
                        // overrides the card's padding set by CardWidget,
                        // making the header outline visibly wider than the
                        // panel cards below it.
                        card.addParagraph(paragraph -> paragraph
                                .text(identity.name().full()
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(nameStyle())
                                .align(TextAlign.CENTER)
                                .margin(DocumentInsets.zero()));
                        if (!identity.jobTitle().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(identity.jobTitle())
                                    .textStyle(headerBodyStyle())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero()));
                        }
                        String contact = joinPipe(identity.contact().address(),
                                identity.contact().phone());
                        if (!contact.isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(contact)
                                    .textStyle(headerMetaStyle())
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero()));
                        }
                        addLinkRow(card, identity);
                    });
        }

        private void addLinkRow(SectionBuilder section, CvIdentity identity) {
            boolean hasEmail = !identity.contact().email().isBlank();
            boolean hasLinks = !identity.links().isEmpty();
            if (!hasEmail && !hasLinks) {
                return;
            }
            DocumentTextStyle bodyStyle = headerMetaStyle();
            DocumentTextStyle linkStyle = headerLinkStyle();
            section.addParagraph(paragraph -> paragraph
                    .textStyle(bodyStyle)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        boolean first = true;
                        String email = identity.contact().email();
                        if (!email.isBlank()) {
                            rich.with(email, linkStyle,
                                    new DocumentLinkOptions("mailto:" + email));
                            first = false;
                        }
                        for (CvLink link : identity.links()) {
                            if (link.label().isBlank()) {
                                continue;
                            }
                            if (!first) {
                                rich.style(" | ", bodyStyle);
                            }
                            first = false;
                            if (link.url().isBlank()) {
                                rich.style(link.label(), bodyStyle);
                            } else {
                                rich.with(link.label(), linkStyle,
                                        new DocumentLinkOptions(link.url()));
                            }
                        }
                    }));
        }

        /**
         * Renders a full-width top-level card (Profile). Shares the
         * exact same shell as {@link #addSidePanel} and
         * {@link #headerStyle} so every panel on the page draws with
         * the same outline width and corner radius.
         */
        private void addFullWidthPanel(PageFlowBuilder flow, String name,
                                       String title, CvSection section,
                                       double anchorWidth) {
            if (!hasContent(section)) {
                return;
            }
            CardWidget.render(flow, "CvV2Panel" + name + "Card",
                    panelStyle(),
                    card -> {
                        widthAnchor(card, anchorWidth);
                        renderModuleBody(card, title, section);
                    });
        }

        /**
         * Renders one panel card inside a left/right column section.
         * Each section in a side column gets its own card so the
         * column reads as a stack of separately-bordered panels of
         * identical width but varying height.
         */
        private void addSidePanel(SectionBuilder column, String name,
                                  String title, CvSection section,
                                  double anchorWidth) {
            if (!hasContent(section)) {
                return;
            }
            CardWidget.render(column, "CvV2Panel" + name + "Card",
                    panelStyle(),
                    card -> {
                        widthAnchor(card, anchorWidth);
                        renderModuleBody(card, title, section);
                    });
        }

        /**
         * Anchors a card's content min-width to the given target. Sections
         * in the v2 engine default to fit-content widths inside columns;
         * adding a zero-height spacer of the exact target width forces the
         * card's content area to that width so every panel renders at the
         * same outer width regardless of how long its longest paragraph is.
         */
        private void widthAnchor(SectionBuilder card, double width) {
            card.spacer(width, 0.0);
        }

        private static boolean hasContent(CvSection section) {
            return section != null && SectionLookup.hasContent(section);
        }

        private void renderModuleBody(SectionBuilder card, String title,
                                      CvSection section) {
            card.addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(moduleTitleStyle())
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()))
                    .addShape(shape -> shape
                            .name("CvV2PanelAccent_"
                                    + SectionLookup.normalize(title))
                            .size(ACCENT_STRIP_WIDTH,
                                    theme.spacing().accentRuleWidth())
                            .fillColor(ACCENT)
                            .cornerRadius(
                                    theme.spacing().accentRuleWidth() / 2.0)
                            .margin(DocumentInsets.zero()));
            renderCardBody(card, section);
        }

        /**
         * Preset-local body dispatcher. Functionally equivalent to
         * {@link com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher}
         * except that {@link EntriesSection} entries are drawn through
         * {@link EntryCompactRenderer#titleDateBody} (single-paragraph
         * "title - date" header) instead of the standard
         * {@code EntryRenderer}'s two-column Row header. The engine
         * bans nested horizontal rows; since every Panel module card
         * may sit inside the page-level 2-column {@code flow.addRow},
         * the entry header must stay row-free here.
         */
        private void renderCardBody(SectionBuilder card, CvSection section) {
            if (section instanceof ParagraphSection paragraph) {
                ParagraphRenderer.render(card, paragraph.body(), theme);
            } else if (section instanceof SkillsSection skills) {
                SkillsRenderer.render(card, skills, theme);
            } else if (section instanceof RowsSection rows) {
                boolean stackedNeedsSeparator =
                        rows.style() == RowStyle.BULLETED_STACKED;
                for (int i = 0; i < rows.rows().size(); i++) {
                    if (i > 0 && stackedNeedsSeparator) {
                        card.spacer(0, theme.spacing().entrySeparation());
                    }
                    RowRenderer.render(card, rows.rows().get(i),
                            rows.style(), theme);
                }
            } else if (section instanceof EntriesSection entries) {
                for (int i = 0; i < entries.entries().size(); i++) {
                    if (i > 0) {
                        card.spacer(0, theme.spacing().entrySeparation());
                    }
                    CvEntry entry = entries.entries().get(i);
                    EntryCompactRenderer.titleDateBody(card, entry,
                            theme.entryTitleStyle(),
                            theme.entryDateStyle(),
                            theme.entrySubtitleStyle(),
                            theme.bodyStyle(),
                            " - ",
                            1.0,
                            DocumentInsets.zero(),
                            DocumentInsets.zero(),
                            DocumentInsets.top(theme.spacing().paragraphMarginTop()),
                            theme.typography().bodyLineSpacing(),
                            false);
                }
            }
        }

        /**
         * Shared shell for every module card (Profile, Skills,
         * Education, Additional, Experience, Projects). White fill,
         * the same stroke/corner as the header, and the
         * {@code bannerInnerPadding} the theme exposes.
         */
        private CardWidget.Style panelStyle() {
            return CardWidget.Style.builder()
                    .spacing(theme.spacing().sectionBodySpacing())
                    .padding(DocumentInsets.of(
                            theme.spacing().bannerInnerPadding()))
                    .fillColor(PANEL_FILL)
                    .stroke(DocumentStroke.of(theme.palette().rule(),
                            PANEL_STROKE_THICKNESS))
                    .cornerRadius(theme.spacing().bannerCornerRadius())
                    .build();
        }

        /**
         * Header card shell. Same outline (stroke, padding, corner) as
         * {@link #panelStyle()} — only the fill is tinted teal so the
         * masthead reads as a distinct band while still being visually
         * the same width as every other panel below it.
         */
        private CardWidget.Style headerStyle() {
            return CardWidget.Style.builder()
                    .spacing(4)
                    .padding(DocumentInsets.of(
                            theme.spacing().bannerInnerPadding()))
                    .fillColor(theme.palette().banner())
                    .stroke(DocumentStroke.of(theme.palette().rule(),
                            PANEL_STROKE_THICKNESS))
                    .cornerRadius(theme.spacing().bannerCornerRadius())
                    .build();
        }

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(FontName.POPPINS,
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    HEADER_TEXT);
        }

        private DocumentTextStyle headerBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle headerMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle headerLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    ACCENT);
        }

        private DocumentTextStyle moduleTitleStyle() {
            return CvTextStyles.of(FontName.POPPINS,
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    ACCENT);
        }

        private static String joinPipe(String... parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(part.trim());
            }
            return sb.toString();
        }
    }
}
