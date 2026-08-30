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
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.PAGE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_WEIGHT;

/**
 * Navy Sidebar — a one-page A4 CV in two columns: a navy plate carrying a
 * ringed portrait, the contact channels, the degrees, the skills and the
 * languages, beside a white column carrying the name, the summary, the roles
 * held on a timeline rail, the achievements and the certifications.
 *
 * <p>The preset owns its page: it sets A4 with no margin and paints the navy
 * plate as a page background, so the column reaches the foot of the sheet
 * whatever the sidebar holds.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>This sheet holds one page of content. The two columns are a single row,
 * and a row is atomic — it cannot be split — so a CV longer than the sheet
 * does not flow onto a second page: composing it raises
 * {@code AtomicNodeTooLargeException}, naming the node and the height it
 * needed. It draws no cap of its own, because a CV that quietly loses a job
 * is worse than one that refuses to compose. {@link TimelineMinimal} is the
 * preset in this package that splits its own columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by
 * title rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}: summary, experience,
 * achievements and certifications to the main column; education, skills and
 * languages to the sidebar. A section whose title matches no berth is not
 * drawn, and a berth with no section takes its leading hairline with it.</p>
 *
 * <p>Each berth wants a particular shape. The summary, the achievements and
 * the certifications are {@link ParagraphSection}s, and the two lists are
 * drawn one bullet per line of the body. Experience and education are
 * {@link EntriesSection}s. Languages are a {@link RowsSection}, because this
 * design writes the proficiency out — "Native", "Advanced" — which a
 * levelled skill could not carry back; its {@code RowStyle} is not read,
 * since the rows are drawn to this design rather than to a family style.</p>
 *
 * <p>Three things are drawn in capitals whatever the document writes: the
 * name, the role, and each degree. Headings are upper-cased too, and set
 * with the tracking this design uses throughout.</p>
 *
 * <p>Content this sheet has no place for is not drawn: the summary block's
 * own title, since it is the one block with no heading; a skill's level,
 * since there are no meters here; and a {@code SkillGroup}'s category. A
 * berth is filled by the first section that names it, so a second section
 * naming the same berth is not drawn either. An education entry uses all
 * four of its fields — the degree, the institution, the body as the campus
 * line, and the years.</p>
 *
 * <h2>The portrait</h2>
 *
 * <p>The photograph comes from {@code CvIdentity.portrait()}. An identity
 * without one draws the ring around an empty navy disc rather than leaving a
 * hole in the column.</p>
 *
 * <p>The phone, the email and each link are reachable from the PDF, with the
 * {@code tel:} and {@code mailto:} targets built from the values. The
 * packaged set has one network mark, so every link the identity carries is
 * drawn behind it whatever the network is; a document that lists more than
 * one profile should say which is which in the label.</p>
 *
 * <p>A channel is one paragraph, the mark and the value sharing a line, so a
 * value wider than the sidebar's text column wraps under the mark and leaves
 * it alone on the first line. The column is about 124pt, which at this type
 * size is a little over twenty characters — long addresses want shortening
 * rather than a wider column.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato. The templates artifact does not carry it —
 * register it on the session, or the engine substitutes and the measured
 * geometry here no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = NavySidebar.create();
 * template.compose(session, cv);
 * }</pre>
 */
public final class NavySidebar {

    /** Stable identifier of this preset. */
    public static final String ID = "navy-sidebar";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Navy Sidebar";

    /**
     * The margin this preset expects: none. It sets the page margin itself,
     * because the navy plate runs to the paper edge.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "achievement", "awards");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "licences", "licenses");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");

    private NavySidebar() {
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

            document.pageSize(PAGE)
                    .margin(DocumentInsets.zero())
                    // The plate is a page background rather than a section
                    // fill: a section is only as tall as what it holds, and
                    // the navy column has to reach the foot of the sheet
                    // whatever the sidebar carries.
                    .pageBackgrounds(List.of(
                            PageBackgroundFill.leftColumn(SIDEBAR_WEIGHT, NAVY)));

            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            RowsSection languages = berth(sections, RowsSection.class, LANGUAGE_KEYS);
            ParagraphSection summary = berth(sections, ParagraphSection.class, SUMMARY_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            ParagraphSection achievements =
                    berth(sections, ParagraphSection.class, ACHIEVEMENT_KEYS);
            ParagraphSection certifications =
                    berth(sections, ParagraphSection.class, CERTIFICATION_KEYS);

            document.pageFlow(page -> page
                    .name("NavySidebarCv")
                    .padding(DocumentInsets.zero())
                    // spacing(0): every gap in this design is authored on the
                    // node that owns it, so a flow gap would add to all of
                    // them.
                    .spacing(0)
                    .addRow("PageGrid", row -> {
                        row.spacing(0);
                        row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("Sidebar", aside ->
                                NavySidebarAside.compose(aside, doc.identity(),
                                        education, skills, languages));
                        row.addSection("Main", main ->
                                NavySidebarMain.compose(main, doc.identity(),
                                        summary, experience, achievements, certifications));
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
