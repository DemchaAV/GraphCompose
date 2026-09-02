package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
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

import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PAGE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PAGE_MARGIN_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PAGE_MARGIN_TOP;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.PAGE_MARGIN_X;

/**
 * Violet Grid — a one-page CV in a single column, banded rather than split: a
 * two-tone name beside the contact list, three opening lines, a six-up grid of
 * marked skills divided by dotted rules, a strip of tools on inline discs, the
 * roles held on a dated timeline, the projects behind tinted tiles, education
 * and languages side by side, and a tinted band closing on a quotation.
 *
 * <h2>Every split is local to one band</h2>
 *
 * <p>Unlike a sidebar CV this page has no page-level grid. The masthead splits
 * where the contact marks begin, the timeline into dates / rail / content, a
 * project into tile / copy, and the credentials into two halves — and none of
 * those four splits knows about any of the others. That is why every
 * horizontal arrangement here is either a top-level row or a table, and why
 * none of them nests: a row cannot sit inside a row's cell.</p>
 *
 * <h2>Three marks that are relationships, not lengths</h2>
 *
 * <p>A section rule starts where its heading ends, so it is a weighted column
 * taking what an auto column leaves. The timeline rail is the left border of
 * every entry but the last, so consecutive entries butt into one unbroken line
 * whose length follows from how many entries there are. A project's hairline
 * is the left border of its copy, so it is exactly as tall as the copy. None
 * of the three is a measured height, and none has to be re-measured when the
 * content changes.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>The page is a stack of bands, so a CV with more content than the design
 * holds runs onto a second page rather than losing anything — each experience
 * entry is held together, so a role is never cut in half, but the sheet is
 * drawn for one page and a longer one will look like a one-page design that
 * ran over. {@link TimelineMinimal} is the preset in this package built to
 * paginate.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The bands are fixed, so the preset assigns each section a berth by title
 * rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has seven:</p>
 *
 * <ul>
 *   <li>the summary, a {@link ParagraphSection} of one line per line — set as
 *       written rather than wrapped, because the design's own breaks leave
 *       room no greedy wrap would;</li>
 *   <li>skills, an {@link EntriesSection} whose entries carry a mark, a label
 *       and a description — the six-up grid;</li>
 *   <li>tools, a {@link SkillsSection} of unlevelled names — the strip;</li>
 *   <li>experience and projects, {@link EntriesSection}s;</li>
 *   <li>education, an {@link EntriesSection} whose first entry is drawn, and
 *       languages, a {@link SkillsSection} — the closing band;</li>
 *   <li>the quotation, a {@link ParagraphSection} whose body is the line in
 *       the tinted band. Only its body is drawn: the title is how the document
 *       names the berth.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, and a berth with no
 * section takes its heading and rule with it.</p>
 *
 * <h2>The marks, the ratings and the links</h2>
 *
 * <p>A skill, a project and the degree each take the mark its entry names in
 * {@code CvEntry.icon()}, from this preset's own vocabulary, and an unknown
 * token is reported as a data error naming the set. An entry with no token is
 * drawn without a mark.</p>
 *
 * <p>A language draws both channels of its skill: the discs from
 * {@code CvSkill.level()} and the words beside them from
 * {@code CvSkill.note()}.</p>
 *
 * <p>The email, the phone and each link are reachable from the PDF, with the
 * {@code mailto:} and {@code tel:} targets built from the values. A link is
 * drawn as its own label with the address behind it, so the contact column's
 * width does not depend on how long an address happens to be, and it takes the
 * mark of the network it points at or the globe. A role, a project title and
 * the degree become links when their entry carries {@code CvEntry.link()}.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato, with Asap Condensed for the name, the headings
 * and the skill labels. The templates artifact carries neither — register them
 * on the session, or the engine substitutes and the measured geometry here no
 * longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = VioletGrid.create();
 * template.compose(session, cv);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class VioletGrid {

    /** Stable identifier of this preset. */
    public static final String ID = "violet-grid";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Violet Grid";

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> QUOTE_KEYS =
            List.of("quote", "motto", "tagline", "closing");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> TOOL_KEYS =
            List.of("tools", "stack", "software");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "portfolio", "selected work");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");

    private VioletGrid() {
    }

    /**
     * Creates the template.
     *
     * @return a template composing a {@link CvDocument}
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

            // The quotation is claimed before the summary: its own keys are
            // narrow, but the summary's are broad enough to take a closing line
            // titled loosely.
            ParagraphSection quote = berth(sections, ParagraphSection.class, QUOTE_KEYS);
            ParagraphSection summary =
                    berthExcept(sections, ParagraphSection.class, SUMMARY_KEYS, quote);
            EntriesSection skills = berth(sections, EntriesSection.class, SKILL_KEYS);
            SkillsSection tools = berth(sections, SkillsSection.class, TOOL_KEYS);
            SkillsSection languages =
                    berthExcept(sections, SkillsSection.class, LANGUAGE_KEYS, tools);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection projects =
                    berthExcept(sections, EntriesSection.class, PROJECT_KEYS, skills);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);

            // Every length on the sheet is a share of the design's own grid, so
            // the preset sets the page and its margins rather than following
            // what the caller configured.
            document.pageSize(PAGE).margin(new DocumentInsets(
                    PAGE_MARGIN_TOP, PAGE_MARGIN_X, PAGE_MARGIN_BOTTOM, PAGE_MARGIN_X));
            document.pageFlow(page -> {
                page.name("VioletGridCv");
                page.spacing(0);
                VioletGridMasthead.render(page, doc.identity(), summary);
                VioletGridBody.renderSkills(page, skills);
                VioletGridBody.renderTools(page, tools);
                VioletGridBody.renderExperience(page, experience);
                VioletGridClosing.renderProjects(page, projects);
                VioletGridClosing.renderCredentials(page, education, languages);
                VioletGridClosing.renderQuote(page, quote);
            });
        }

        /**
         * The first section of the wanted shape whose title names this berth,
         * or {@code null} when the document fills no such berth.
         */
        private static <T extends CvSection> T berth(List<CvSection> sections,
                                                     Class<T> type,
                                                     List<String> keys) {
            return berthExcept(sections, type, keys, null);
        }

        /** The same, skipping a section another berth has already claimed. */
        private static <T extends CvSection> T berthExcept(List<CvSection> sections,
                                                           Class<T> type,
                                                           List<String> keys,
                                                           CvSection taken) {
            for (CvSection section : sections) {
                if (!type.isInstance(section) || section == taken) {
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
