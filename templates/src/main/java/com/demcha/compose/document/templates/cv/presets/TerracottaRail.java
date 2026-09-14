package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.COLUMN_PAD_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MAIN_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_WEIGHT;

/**
 * Terracotta Rail — a CV in two columns drawn as one page: a narrow column
 * carrying a serif monogram over a terracotta rule, the contact channels
 * behind their marks, two bulleted lists and a block of closing facts, beside
 * a wide column carrying a letter-spaced masthead, the summary, the roles held
 * on a ringed rail, a projects grid and the degrees.
 *
 * <p>The columns are divided by a hairline the sidebar carries as its own
 * right border, so it is as tall as the sidebar rather than as tall as the
 * page. The preset paints nothing: the sheet is drawn on white paper and
 * leaves the page size and the margin to the caller — the geometry follows
 * the page's own width, so another page rescales rather than breaks.</p>
 *
 * <h2>Longer than one page</h2>
 *
 * <p>A CV that fits the page is composed as the one row the sheet is: the two
 * columns side by side. A longer one continues onto as many pages as it needs,
 * each page a row of its own, both columns keeping their padding on every
 * page. The hairline runs as far as the sidebar does on each page, and a page
 * whose sidebar has nothing left to hold draws none. The pages are planned
 * for the session's own page size and margin, so a per-page margin rule that
 * changes a later page is not taken into account.</p>
 *
 * <p>What moves to the next page is a whole block: the masthead, the summary,
 * a role, a project, the degrees, or one of the sidebar's lists. A heading
 * stays with the first entry it heads, the roles a page carries share one
 * rail, and the hairline between two blocks is left out when the second one
 * opens a page. A single block taller than a page cannot be split, and
 * composing it raises {@code AtomicNodeTooLargeException}, naming the block.
 * The preset draws no cap of its own, because a CV that quietly loses a job is
 * worse than one that runs to a second page.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by
 * title rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has eight:</p>
 *
 * <ul>
 *   <li>competencies and software, {@link SkillsSection}s; certifications,
 *       an {@link EntriesSection} of one-line entries; and the closing
 *       facts, an {@link EntriesSection} whose bodies are one line per
 *       value — in the sidebar;</li>
 *   <li>the summary, a {@link ParagraphSection} of one paragraph per line;
 *       experience, projects and education, {@link EntriesSection}s — in the
 *       main column.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, a berth with no
 * section takes its heading and the divider above it with it, and a berth is
 * filled by the first section that names it. The two bulleted lists are
 * skills without levels: this design writes them as plain lines, so a rating
 * would have nowhere to go — one takes a terracotta square and no dash under
 * its heading, the other a disc and a dash, which is how the sheet tells two
 * lists of one-liners apart.</p>
 *
 * <h2>The monogram, the marks and the links</h2>
 *
 * <p>The monogram is drawn from the name's own initials rather than a field
 * of its own: a document states its name once, and a monogram that could
 * disagree with it would be a second place to keep true.</p>
 *
 * <p>Each fact and each project takes the mark its entry names in
 * {@code CvEntry.icon()}, from this preset's own vocabulary —
 * {@code globe}, {@code badge}, {@code clock}, {@code building},
 * {@code hotel}, {@code house} — and an unknown token is reported as a data
 * error naming the set. An entry with no token is drawn without a mark.</p>
 *
 * <p>Every title the preset draws — a role, a project, a degree, a
 * credential — becomes a link when its entry carries {@code CvEntry.link()}.
 * It costs the layout nothing, because a link is an annotation rather than
 * ink, so a linked title and a plain one are the same line. The closing facts
 * are the exception: their bold line is a label for the values under it —
 * "Languages:", "Availability:" — rather than the name of something a reader
 * could open.</p>
 *
 * <p>The email, the phone and each link are reachable from the PDF, with the
 * {@code mailto:} and {@code tel:} targets built from the values. A link is
 * drawn as its own label with the address behind it — {@code Link("LinkedIn",
 * "https://…")} sets the word and links the URL — so the four contact rows sit
 * on one axis at one size whatever a profile happens to be called. It takes
 * the mark of the network it points at, or a globe.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato, with PT Serif for the monogram. The templates
 * artifact carries neither — register them on the session, or the engine
 * substitutes and the measured geometry here no longer matches the type
 * sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = TerracottaRail.create();
 * template.compose(session, cv);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class TerracottaRail {

    /** Stable identifier of this preset. */
    public static final String ID = "terracotta-rail";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Terracotta Rail";

    /**
     * The margin this preset expects: none. Both columns run to the paper
     * edge and each carries its own padding.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final List<String> COMPETENCY_KEYS =
            List.of("competencies", "competency", "skills", "expertise");
    private static final List<String> SOFTWARE_KEYS =
            List.of("software", "tools", "stack");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "licences", "licenses");
    private static final List<String> FACT_KEYS =
            List.of("additional", "details", "other");
    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "portfolio", "selected work");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");

    private TerracottaRail() {
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

            // Software first, and the competency berth then skips whatever it
            // claimed: a section titled for both — "Software Skills" — would
            // otherwise fill the two berths and be drawn twice.
            SkillsSection software = berth(sections, SkillsSection.class, SOFTWARE_KEYS);
            SkillsSection competencies =
                    berthExcept(sections, SkillsSection.class, COMPETENCY_KEYS, software);
            EntriesSection certifications =
                    berth(sections, EntriesSection.class, CERTIFICATION_KEYS);
            EntriesSection facts = berth(sections, EntriesSection.class, FACT_KEYS);
            ParagraphSection summary = berth(sections, ParagraphSection.class, SUMMARY_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection projects = berth(sections, EntriesSection.class, PROJECT_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);

            List<ColumnPages.Block> asideBlocks = TerracottaRailAside.blocks(doc.identity(),
                    competencies, software, certifications, facts);
            List<TerracottaRailMain.Piece> mainPieces = TerracottaRailMain.pieces(doc.identity(),
                    summary, experience, projects, education);
            // The column widths follow the page, as the row's weights do.
            double pageWidth = document.canvas().innerWidth();
            double pageHeight = document.availableHeight();
            ColumnPages.Plan plan = ColumnPages.plan(document, List.of(
                    new ColumnPages.Column(0, pageWidth * MAIN_WEIGHT,
                            SIDEBAR_PAD_LEFT, SIDEBAR_PAD_RIGHT,
                            pageHeight - SIDEBAR_PAD_TOP - COLUMN_PAD_BOTTOM,
                            pageHeight - SIDEBAR_PAD_TOP - COLUMN_PAD_BOTTOM, asideBlocks),
                    new ColumnPages.Column(pageWidth * SIDEBAR_WEIGHT, 0,
                            MAIN_PAD_LEFT, MAIN_PAD_RIGHT,
                            pageHeight - MAIN_PAD_TOP - COLUMN_PAD_BOTTOM,
                            pageHeight - MAIN_PAD_TOP - COLUMN_PAD_BOTTOM,
                            TerracottaRailMain.blocks(mainPieces))));

            document.pageFlow(page -> {
                page.name("TerracottaRailCv").spacing(0);
                if (plan.pages() == 1) {
                    // The sheet as drawn: one row, the two columns side by side.
                    page.addRow("Body", row -> {
                        row.name("Body");
                        row.spacing(0);
                        row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("Sidebar", side ->
                                TerracottaRailAside.compose(side, doc.identity(), competencies,
                                        software, certifications, facts));
                        row.addSection("MainColumn", main ->
                                TerracottaRailMain.compose(main, doc.identity(), summary,
                                        experience, projects, education));
                    });
                    return;
                }
                // Longer than the page: a row per page, each on a page of its
                // own and holding the blocks the plan put there. See ColumnPages.
                ColumnPages.addRows(page, plan, "Body", (row, pageIndex) -> {
                    row.spacing(0);
                    row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                    row.addSection("Sidebar", side ->
                            TerracottaRailAside.composePage(side, asideBlocks,
                                    plan.blocks(0, pageIndex)));
                    row.addSection("MainColumn", main ->
                            TerracottaRailMain.composePage(main, mainPieces, experience,
                                    plan.blocks(1, pageIndex)));
                });
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
