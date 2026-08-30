package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PAGE_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.PAGE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_BACKGROUND;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_WEIGHT;

/**
 * Professional Sidebar — a one-page CV in two columns: a pale sidebar under
 * a navy monogram plate carrying the contact channels, the skill meters, the
 * education rail and the language ratings, and a white main column carrying
 * the name, the profile, the roles held, the projects and the references
 * note.
 *
 * <p>The preset owns its page: a 491.6 x 737.28pt sheet with no margin, the
 * page fill and the pale sidebar painted as page backgrounds so the column
 * reaches the foot of the sheet whatever the sidebar holds.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>This sheet holds one page of content. The two columns are a single row,
 * and a row is atomic — it cannot be split — so a CV longer than the sheet
 * does not flow onto a second page: composing it raises
 * {@code AtomicNodeTooLargeException}, naming the node and the height it
 * needed. Feed it a CV of about the length the design was drawn around: five
 * or six roles with three or four highlights each, alongside the sidebar
 * blocks.</p>
 *
 * <p>The preset draws no cap of its own, which is the deliberate half of
 * that: its siblings {@link MonogramSidebar}, {@link SidebarPortrait} and
 * {@link MintEditorial} cap each block and silently drop what does not fit,
 * and a CV that quietly loses a job is worse than one that refuses to
 * compose. {@link TimelineMinimal} is the preset in this package that splits
 * its own columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by
 * title rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}: profile, experience,
 * projects and references to the main column; skills, education and
 * languages to the sidebar. A section whose title matches no berth is not
 * drawn, and a berth with no section takes its leading hairline with it.</p>
 *
 * <p>A berth is filled by the first section that names it, so a second
 * section naming the same berth is not drawn. Neither is anything the design
 * has no place for: a project's {@code subtitle}, an education entry's
 * {@code body}, and a {@code SkillGroup}'s {@code category} — this sheet
 * heads its projects with a title and dates, its degrees with an institution
 * and dates, and sets its skills as one ungrouped list.</p>
 *
 * <p>Both the skills and the languages are {@link SkillsSection}s — the same
 * shape, drawn differently: a skill's level fills a bar, a language's fills
 * the nearest whole number of five dots. A level the document omits draws
 * the name alone.</p>
 *
 * <p>The sidebar's rows are one line each: a skill name, a language name and
 * an education degree are anchored inside a fixed-height band, so text
 * longer than the narrow column measures overflows the band instead of
 * wrapping inside it. That is the design — the sheet is drawn around short
 * labels — and it is the one length constraint a document has to respect.</p>
 *
 * <p>The contact channels come off the identity rather than a section: the
 * phone, the email and the address take the packaged marks, and each link
 * takes the network mark when it names one and the globe otherwise. The
 * phone and email rows carry {@code tel:} and {@code mailto:} targets built
 * from the value.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Barlow Condensed and Lato. The templates artifact
 * does not carry either face — register them on the session, or the engine
 * substitutes and the measured geometry here no longer matches the type
 * sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = ProfessionalSidebar.create();
 * template.compose(session, cv);
 * }</pre>
 */
public final class ProfessionalSidebar {

    /** Stable identifier of this preset. */
    public static final String ID = "professional-sidebar";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Professional Sidebar";

    /**
     * The margin this preset expects: none. It sets the page margin itself,
     * because both columns run to the paper edge.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final List<String> PROFILE_KEYS =
            List.of("profile", "summary", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "portfolio");
    private static final List<String> REFERENCES_KEYS =
            List.of("references", "reference");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications", "certifications");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");

    private ProfessionalSidebar() {
    }

    /**
     * Creates the template.
     *
     * @return a template composing a {@link CvDocument} onto its own page
     */
    public static DocumentTemplate<CvDocument> create() {
        return new Template();
    }

    private record Template() implements DocumentTemplate<CvDocument> {

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
            List<CvSection> sections = doc.sections();

            document.pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                    .margin(DocumentInsets.zero())
                    // The sidebar fill is a page background rather than a
                    // section fill: a section is only as tall as what it
                    // holds, and the pale column has to reach the foot of
                    // the sheet whatever the sidebar carries.
                    .pageBackgrounds(List.of(
                            PageBackgroundFill.fullPage(PAGE_BACKGROUND),
                            PageBackgroundFill.leftColumn(SIDEBAR_WEIGHT,
                                    SIDEBAR_BACKGROUND)));

            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            // A title can name both berths — "Skills and Languages" does —
            // and one section cannot fill two, so the second berth stays
            // empty rather than drawing the same list twice in two hands.
            SkillsSection named = berth(sections, SkillsSection.class, LANGUAGE_KEYS);
            SkillsSection languages = named == skills ? null : named;
            ParagraphSection profile = berth(sections, ParagraphSection.class, PROFILE_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection projects = berth(sections, EntriesSection.class, PROJECT_KEYS);
            ParagraphSection references =
                    berth(sections, ParagraphSection.class, REFERENCES_KEYS);

            document.pageFlow(page -> page
                    .name("ProfessionalSidebarCv")
                    .padding(DocumentInsets.zero())
                    // spacing(0): every gap in this design is authored on the
                    // node that owns it, so a flow gap would add to all of
                    // them.
                    .spacing(0)
                    .addRow("PageGrid", row -> {
                        row.spacing(0);
                        row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("Sidebar", aside ->
                                ProfessionalSidebarAside.compose(aside, doc.identity(),
                                        skills, education, languages));
                        row.addSection("Main", main ->
                                ProfessionalSidebarMain.compose(main, doc.identity(),
                                        profile, experience, projects, references));
                    }));
        }

        /**
         * The first section of the wanted shape whose title names this berth,
         * or {@code null} when the document fills no such berth.
         */
        private static <T extends CvSection> T berth(List<CvSection> sections,
                                                     Class<T> type,
                                                     List<String> keys) {
            for (CvSection section : sections) {
                if (!type.isInstance(section)) {
                    continue;
                }
                for (String key : keys) {
                    if (SectionLookup.titleContains(section.title(), key)) {
                        return type.cast(section);
                    }
                }
            }
            return null;
        }
    }
}
