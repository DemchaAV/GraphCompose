package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * v2 port of the legacy "Engineering Resume" CV preset.
 *
 * <p>Senior engineering CV with a full-width navy command header
 * (UPPERCASE name, subtitle line, right-aligned contact stack with
 * cyan-green underlined links), a dark navy skill rail
 * (<em>Core Stack</em> / <em>Learning</em> / <em>Details</em> with
 * green accent labels), and white evidence cards for Leadership
 * Experience plus Technical Evidence on the right.</p>
 *
 * <p>The preset stays a thin orchestrator. Theme tokens cover body
 * ink / muted / rule / profile-band fill; the navy header, brighter
 * green accent, navy-rail text variants and the cyan-green contact
 * link colour stay preset-local because no other v2 preset shares
 * them today (same pattern as NordicClean / EditorialBlue).</p>
 *
 * <p>Body rendering uses a preset-local dispatcher because the engine
 * bans nested horizontal rows and every body card sits inside the
 * page-level 2-column {@code flow.addRow}; entries are drawn as a
 * single "title / date" header paragraph instead of the standard
 * {@code EntryRenderer}'s 2-column Row.</p>
 */
public final class EngineeringResume {

    /** Stable template identifier. */
    public static final String ID = "engineering-resume";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Engineering Resume";

    /** Recommended page margin (in points) — matches V1 TechLead. */
    public static final double RECOMMENDED_MARGIN = 20.0;

    /** V1 TechLead deep navy used for the command header + rail fill. */
    private static final DocumentColor NAVY =
            DocumentColor.rgb(13, 32, 47);

    /** V1 TechLead lighter navy used for inside-rail rule lines. */
    private static final DocumentColor NAVY_SOFT =
            DocumentColor.rgb(35, 56, 72);

    /** V1 TechLead green accent for headings, strips and link colour. */
    private static final DocumentColor GREEN =
            DocumentColor.rgb(27, 145, 104);

    /** Body text colour for items rendered inside the navy rail. */
    private static final DocumentColor RAIL_TEXT =
            DocumentColor.rgb(220, 231, 236);

    /** Secondary text colour (e.g. dates) inside the navy rail. */
    private static final DocumentColor RAIL_DATE =
            DocumentColor.rgb(182, 201, 210);

    /** Subtitle colour used under the masthead name. */
    private static final DocumentColor SUBTITLE_COLOR =
            DocumentColor.rgb(190, 209, 219);

    /** Contact metadata colour (right-aligned over the navy header). */
    private static final DocumentColor CONTACT_META =
            DocumentColor.rgb(196, 211, 220);

    /** Contact-link colour (cyan-green underlined over the navy header). */
    private static final DocumentColor CONTACT_LINK =
            DocumentColor.rgb(78, 207, 161);

    /** Fallback masthead subtitle when {@code identity.jobTitle()} is blank. */
    private static final String SUBTITLE_FALLBACK =
            "SECURE BACKEND SYSTEMS / DELIVERY LEADERSHIP";

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

    private EngineeringResume() {
    }

    /**
     * Builds the preset with its Engineering Resume theme.
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.engineeringResume());
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

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            PageFlowBuilder flow = document.dsl()
                    .pageFlow()
                    .name("CvV2EngineeringResumeRoot")
                    .spacing(theme.spacing().pageFlowSpacing());

            addHeader(flow, doc.identity());
            addBody(flow, sections);

            flow.build();
        }

        // -- Header --------------------------------------------------------

        private void addHeader(PageFlowBuilder flow, CvIdentity identity) {
            flow.addSection("CvV2EngineeringResumeHeader", section -> section
                    .spacing(5)
                    .padding(new DocumentInsets(13, 15, 13, 15))
                    .fillColor(NAVY)
                    .cornerRadius(DocumentCornerRadius.top(
                            theme.spacing().bannerCornerRadius()))
                    .accentBottom(GREEN, theme.spacing().accentRuleWidth())
                    .addRow("CvV2EngineeringResumeHeaderRow", row -> row
                            .spacing(12)
                            .weights(1.15, 0.85)
                            .addSection("CvV2EngineeringResumeIdentity",
                                    identityBlock -> addIdentityBlock(
                                            identityBlock, identity))
                            .addSection("CvV2EngineeringResumeContact",
                                    contact -> addContactStack(contact,
                                            identity))));
        }

        private void addIdentityBlock(SectionBuilder block, CvIdentity identity) {
            block.padding(DocumentInsets.zero())
                    .spacing(3)
                    .addParagraph(paragraph -> paragraph
                            .text(identity.name().full()
                                    .toUpperCase(Locale.ROOT))
                            .textStyle(nameStyle())
                            .autoSize(theme.typography().sizeHeadline(), 19.0)
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(headerSubtitleText(identity))
                            .textStyle(subtitleStyle())
                            .margin(DocumentInsets.zero()));
        }

        private void addContactStack(SectionBuilder section, CvIdentity identity) {
            section.spacing(2).padding(DocumentInsets.zero());
            DocumentTextStyle meta = contactMetaStyle();
            DocumentTextStyle link = contactLinkStyle();
            for (ContactPart part : contactParts(identity)) {
                section.addParagraph(paragraph -> paragraph
                        .text(part.text())
                        .textStyle(part.linkOptions() == null ? meta : link)
                        .link(part.linkOptions())
                        .align(TextAlign.RIGHT)
                        .margin(DocumentInsets.zero()));
            }
        }

        private static List<ContactPart> contactParts(CvIdentity identity) {
            if (identity == null) {
                return List.of();
            }
            java.util.List<ContactPart> parts = new java.util.ArrayList<>();
            addPart(parts, identity.contact().address(), null);
            addPart(parts, identity.contact().phone(), null);
            String email = identity.contact().email();
            if (!email.isBlank()) {
                addPart(parts, email,
                        new DocumentLinkOptions("mailto:" + email));
            }
            for (CvLink link : identity.links()) {
                addPart(parts, link.label(), link.url().isBlank()
                        ? null
                        : new DocumentLinkOptions(link.url().trim()));
            }
            return List.copyOf(parts);
        }

        private static void addPart(java.util.List<ContactPart> parts,
                                    String text,
                                    DocumentLinkOptions linkOptions) {
            if (text != null && !text.isBlank()) {
                parts.add(new ContactPart(text.trim(), linkOptions));
            }
        }

        private static String headerSubtitleText(CvIdentity identity) {
            if (identity == null) {
                return SUBTITLE_FALLBACK;
            }
            String jobTitle = identity.jobTitle();
            if (jobTitle == null || jobTitle.isBlank()) {
                return SUBTITLE_FALLBACK;
            }
            return MarkdownInline.plainText(jobTitle)
                    .toUpperCase(Locale.ROOT);
        }

        // -- Body 2-column -------------------------------------------------

        private void addBody(PageFlowBuilder flow, List<CvSection> sections) {
            CvSection skills = SectionLookup.firstMatching(sections, SKILL_KEYS);
            CvSection education = SectionLookup.firstMatching(sections,
                    EDUCATION_KEYS);
            CvSection additional = SectionLookup.firstMatching(sections,
                    ADDITIONAL_KEYS);
            CvSection summary = SectionLookup.firstMatching(sections,
                    SUMMARY_KEYS);
            CvSection experience = SectionLookup.firstMatching(sections,
                    EXPERIENCE_KEYS);
            CvSection projects = SectionLookup.firstMatching(sections,
                    PROJECT_KEYS);

            flow.addRow("CvV2EngineeringResumeBody", row -> row
                    .spacing(14)
                    .weights(0.76, 1.64)
                    .addSection("CvV2EngineeringResumeRail", rail -> {
                        rail.spacing(8)
                                .padding(new DocumentInsets(10, 10, 11, 10))
                                .fillColor(NAVY)
                                .cornerRadius(DocumentCornerRadius.bottom(
                                        theme.spacing().bannerCornerRadius()))
                                .accentTop(GREEN, 2.0);
                        addRailSkills(rail, skills);
                        addRailEducation(rail, education);
                        addRailAdditional(rail, additional);
                    })
                    .addSection("CvV2EngineeringResumeMain", main -> {
                        main.spacing(8);
                        addProfile(main, summary);
                        addExperience(main, experience);
                        addProjects(main, projects);
                    }));
        }

        // -- Rail modules ---------------------------------------------------

        private void addRailSkills(SectionBuilder parent, CvSection section) {
            if (!hasContent(section) || !(section instanceof SkillsSection skills)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeSkills", block -> {
                addRailHeading(block, "Core Stack");
                List<SkillGroup> groups = skills.groups();
                for (int i = 0; i < Math.min(groups.size(), 7); i++) {
                    SkillGroup group = groups.get(i);
                    block.addParagraph(paragraph -> paragraph
                            .textStyle(railBodyStyle())
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(1.8))
                            .rich(rich -> {
                                String category = group.category();
                                if (!category.isBlank()) {
                                    rich.style(category + ":",
                                            railLabelStyle());
                                    rich.style(" "
                                                    + compactValues(
                                                    group.skillsInline(), 5),
                                            railBodyStyle());
                                } else {
                                    rich.style(
                                            compactValues(group.skillsInline(),
                                                    6),
                                            railBodyStyle());
                                }
                            }));
                }
            });
        }

        private void addRailEducation(SectionBuilder parent, CvSection section) {
            if (!hasContent(section)
                    || !(section instanceof EntriesSection entries)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeEducation", block -> {
                addRailHeading(block, "Learning");
                List<CvEntry> list = entries.entries();
                for (int i = 0; i < Math.min(list.size(), 4); i++) {
                    CvEntry entry = list.get(i);
                    block.addParagraph(paragraph -> paragraph
                            .textStyle(railBodyStyle())
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(2.3))
                            .rich(rich -> {
                                rich.style(entry.title(), railTitleStyle());
                                if (!entry.date().isBlank()) {
                                    rich.style(" / " + entry.date(),
                                            railDateStyle());
                                }
                            }));
                }
            });
        }

        private void addRailAdditional(SectionBuilder parent, CvSection section) {
            if (!hasContent(section)
                    || !(section instanceof RowsSection rows)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeAdditional", block -> {
                addRailHeading(block, "Details");
                List<CvRow> list = rows.rows();
                for (int i = 0; i < Math.min(list.size(), 2); i++) {
                    CvRow row = list.get(i);
                    String text = row.label().isBlank()
                            ? row.body()
                            : (row.body().isBlank()
                                    ? row.label()
                                    : row.label() + ": " + row.body());
                    String clean = MarkdownInline.plainText(text);
                    block.addParagraph(paragraph -> paragraph
                            .text(clean)
                            .textStyle(railBodyStyle())
                            .lineSpacing(1.0)
                            .margin(DocumentInsets.bottom(1.8)));
                }
            });
        }

        // -- Main modules ---------------------------------------------------

        private void addProfile(SectionBuilder parent, CvSection section) {
            if (!hasContent(section)
                    || !(section instanceof ParagraphSection summary)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeProfile", card -> card
                    .spacing(4)
                    .padding(new DocumentInsets(8, 10, 8, 10))
                    .fillColor(theme.palette().banner())
                    .accentLeft(GREEN, 3.0)
                    .cornerRadius(DocumentCornerRadius.right(
                            theme.spacing().bannerCornerRadius()))
                    .addParagraph(paragraph -> paragraph
                            .text("ENGINEERING PROFILE")
                            .textStyle(profileHeadingStyle())
                            .margin(DocumentInsets.zero()))
                    .addParagraph(paragraph -> paragraph
                            .text(MarkdownInline.plainText(summary.body()))
                            .textStyle(profileBodyStyle())
                            .lineSpacing(1.2)
                            .margin(DocumentInsets.zero())));
        }

        private void addExperience(SectionBuilder parent, CvSection section) {
            if (!hasContent(section)
                    || !(section instanceof EntriesSection entries)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeExperience", block -> {
                addMainHeading(block, "Leadership Experience");
                List<CvEntry> list = entries.entries();
                for (int i = 0; i < Math.min(list.size(), 2); i++) {
                    CvEntry entry = list.get(i);
                    block.addSection("CvV2EngineeringResumeRoleCard", card -> {
                        card.spacing(3)
                                .padding(new DocumentInsets(6, 8, 6, 8))
                                .fillColor(DocumentColor.WHITE)
                                .stroke(DocumentStroke.of(theme.palette().rule(),
                                        0.35))
                                .cornerRadius(DocumentCornerRadius.right(3))
                                .accentLeft(GREEN, 2.0);
                        addRoleHeader(card, entry);
                        if (!entry.subtitle().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(MarkdownInline.plainText(
                                            entry.subtitle()))
                                    .textStyle(subtitleBodyStyle())
                                    .margin(DocumentInsets.zero()));
                        }
                        if (!entry.body().isBlank()) {
                            card.addParagraph(paragraph -> paragraph
                                    .text(MarkdownInline.plainText(entry.body()))
                                    .textStyle(roleBodyStyle())
                                    .lineSpacing(1.08)
                                    .margin(DocumentInsets.zero()));
                        }
                    });
                }
            });
        }

        private void addProjects(SectionBuilder parent, CvSection section) {
            if (!hasContent(section)
                    || !(section instanceof RowsSection rows)) {
                return;
            }
            parent.addSection("CvV2EngineeringResumeProjects", block -> {
                addMainHeading(block, "Technical Evidence");
                List<CvRow> list = rows.rows();
                for (int i = 0; i < Math.min(list.size(), 4); i++) {
                    CvRow row = list.get(i);
                    ProjectLabel label = ProjectLabel.parse(row.label());
                    String body = MarkdownInline.plainText(row.body());
                    block.addSection("CvV2EngineeringResumeProjectCard", card -> card
                            .spacing(3)
                            .padding(new DocumentInsets(5, 8, 5, 8))
                            .fillColor(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(theme.palette().rule(), 0.3))
                            .cornerRadius(3)
                            .addParagraph(paragraph -> paragraph
                                    .textStyle(projectBodyStyle())
                                    .lineSpacing(1.06)
                                    .margin(DocumentInsets.zero())
                                    .rich(rich -> {
                                        rich.style(label.title(),
                                                projectTitleStyle());
                                        if (!label.stack().isBlank()) {
                                            rich.style(" (" + label.stack()
                                                            + ")",
                                                    projectContextStyle());
                                        }
                                        if (!body.isBlank()) {
                                            rich.style(" - " + body,
                                                    projectBodyStyle());
                                        }
                                    })));
                }
            });
        }

        // -- Headings -------------------------------------------------------

        private void addRailHeading(SectionBuilder section, String title) {
            section.spacing(3)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(railHeadingStyle())
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(82)
                            .color(NAVY_SOFT)
                            .thickness(0.8)
                            .margin(DocumentInsets.bottom(2)));
        }

        private void addMainHeading(SectionBuilder section, String title) {
            section.spacing(5)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(mainHeadingStyle())
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .horizontal(176)
                            .color(GREEN)
                            .thickness(1.0)
                            .margin(DocumentInsets.bottom(1)));
        }

        private void addRoleHeader(SectionBuilder card, CvEntry entry) {
            card.addParagraph(paragraph -> paragraph
                    .textStyle(roleTitleStyle())
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        rich.style(entry.title(), roleTitleStyle());
                        if (!entry.date().isBlank()) {
                            rich.style(" / " + entry.date(),
                                    roleDateStyle());
                        }
                    }));
        }

        // -- Style factories ------------------------------------------------

        private DocumentTextStyle nameStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeHeadline(),
                    DocumentTextDecoration.BOLD,
                    DocumentColor.WHITE);
        }

        private DocumentTextStyle subtitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.6,
                    DocumentTextDecoration.BOLD,
                    SUBTITLE_COLOR);
        }

        private DocumentTextStyle contactMetaStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT,
                    CONTACT_META);
        }

        private DocumentTextStyle contactLinkStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.UNDERLINE,
                    CONTACT_LINK);
        }

        private DocumentTextStyle railHeadingStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    7.4,
                    DocumentTextDecoration.BOLD,
                    GREEN);
        }

        private DocumentTextStyle railBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    6.95,
                    DocumentTextDecoration.DEFAULT,
                    RAIL_TEXT);
        }

        private DocumentTextStyle railLabelStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    6.95,
                    DocumentTextDecoration.BOLD,
                    GREEN);
        }

        private DocumentTextStyle railTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    6.95,
                    DocumentTextDecoration.BOLD,
                    DocumentColor.WHITE);
        }

        private DocumentTextStyle railDateStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    6.7,
                    DocumentTextDecoration.DEFAULT,
                    RAIL_DATE);
        }

        private DocumentTextStyle mainHeadingStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD,
                    GREEN);
        }

        private DocumentTextStyle profileHeadingStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    8.0,
                    DocumentTextDecoration.BOLD,
                    GREEN);
        }

        private DocumentTextStyle profileBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.75,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle roleTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle roleDateStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.BOLD,
                    GREEN);
        }

        private DocumentTextStyle subtitleBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntrySubtitle(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().muted());
        }

        private DocumentTextStyle roleBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }

        private DocumentTextStyle projectTitleStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.35,
                    DocumentTextDecoration.BOLD,
                    theme.palette().ink());
        }

        private DocumentTextStyle projectContextStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    6.85,
                    DocumentTextDecoration.DEFAULT,
                    GREEN);
        }

        private DocumentTextStyle projectBodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    7.1,
                    DocumentTextDecoration.DEFAULT,
                    theme.palette().ink());
        }
    }

    // -- Static helpers ----------------------------------------------------

    private static boolean hasContent(CvSection section) {
        return section != null && SectionLookup.hasContent(section);
    }

    private static String compactValues(String value, int maxItems) {
        String clean = MarkdownInline.plainText(value);
        String[] tokens = clean.split(",");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(trimmed);
            count++;
            if (count == maxItems) {
                break;
            }
        }
        return builder.toString();
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }
}
