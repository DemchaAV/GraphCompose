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

import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.MAIN_CELL_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.PAGE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.SIDEBAR_CELL_WEIGHT;

/**
 * Slate Orange — a one-page CV built as a full-bleed slate band over a
 * two-column body: an orange monogram tile beside the name, the role line and
 * the specialism strip, with the contact lines across an orange hairline;
 * then a narrow column of marked competencies, trophy-marked achievements,
 * rated languages and closing facts, beside a wide column carrying the
 * profile, the roles held on a rail, and a credentials footer.
 *
 * <h2>Why the fills are page backgrounds</h2>
 *
 * <p>The slate band, the orange tile, the hairline between the two body
 * columns and the fainter one between the credential columns are page
 * backgrounds rather than section fills or line nodes. A fill on a section is
 * bounded by its content and would stop short of the paper edge; a page
 * background takes its geometry from the canvas and reaches all four edges
 * however much content happens to sit on it.</p>
 *
 * <h2>Why every horizontal pair inside a column is a table</h2>
 *
 * <p>A row cannot nest inside a row cell, and both body columns are cells of
 * the body row — so every pair inside one that a row would otherwise carry is
 * a table with fixed column widths: mark and label, language and rating, role
 * and dates, the two credential columns. Where a column needs several stacked
 * lines inside one cell, that cell holds a single-column table of its own,
 * because a cell holds one node.</p>
 *
 * <p>The experience rail is the left border of each entry rather than a
 * drawn line, so it stretches to whatever height the entry turns out to have
 * instead of being a length computed against today's text.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>The experience block is held together, and the body's two columns are a
 * single row — a row is atomic, so a CV taller than the sheet raises
 * {@code AtomicNodeTooLargeException}, naming the node and the height it
 * needed. The preset draws no cap of its own, because a CV that quietly loses
 * a role is worse than one that refuses to compose. {@link TimelineMinimal}
 * is the preset in this package that splits its own columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The bands are fixed, so the preset assigns each section a berth by title
 * rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has seven:</p>
 *
 * <ul>
 *   <li>the specialisms, a {@link ParagraphSection} of one specialism per
 *       line, drawn as the strip under the role. Only its body is drawn: the
 *       title is how the document names the berth;</li>
 *   <li>competencies, achievements and the closing facts,
 *       {@link EntriesSection}s, and languages, a {@link SkillsSection} — the
 *       narrow column;</li>
 *   <li>the profile, a {@link ParagraphSection}, and experience, an
 *       {@link EntriesSection} whose bodies are one highlight per line — the
 *       wide column;</li>
 *   <li>education and certifications, {@link EntriesSection}s — the
 *       credentials footer.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, and a berth with no
 * section takes its heading and rule with it.</p>
 *
 * <h2>The marks, the ratings and the links</h2>
 *
 * <p>A competency, an achievement and a closing fact each take the mark its
 * entry names in {@code CvEntry.icon()}, from this preset's own vocabulary,
 * and an unknown token is reported as a data error naming the set. An entry
 * with no token is drawn without a mark — except an achievement, which falls
 * back to the trophy the design gives them all.</p>
 *
 * <p>A language draws both channels of its skill: the discs from
 * {@code CvSkill.level()} and the words beside them from
 * {@code CvSkill.note()}. A skill with no level draws no discs, and one with
 * no note leaves that column empty rather than inventing a wording.</p>
 *
 * <p>The phone, the email and each link are reachable from the PDF, with the
 * {@code tel:} and {@code mailto:} targets built from the values — so a
 * number written without a country code gets a national dial target, which is
 * what the document said. A link is
 * drawn as its own label with the address behind it, so the contact column's
 * width does not depend on how long a profile happens to be called. A role
 * and a degree become links when their entry carries
 * {@code CvEntry.link()}.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Fira Sans Condensed, with Asap Condensed for the
 * name and the headings. The templates artifact carries neither — register
 * them on the session, or the engine substitutes and the measured geometry
 * here no longer matches the type sitting in it. Both faces are condensed on
 * purpose: the design's measure is narrow enough that a normal-width face
 * turns single-line bullets into two.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = SlateOrange.create();
 * template.compose(session, cv);
 * }</pre>
 *
 * @since 2.2.3
 */
public final class SlateOrange {

    /** Stable identifier of this preset. */
    public static final String ID = "slate-orange";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Slate Orange";

    private static final List<String> SPECIALISM_KEYS =
            List.of("specialisms", "specialities", "specialties", "focus");
    private static final List<String> COMPETENCY_KEYS =
            List.of("competencies", "competency", "skills", "expertise");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "achievement", "awards", "highlights");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");
    private static final List<String> FACT_KEYS =
            List.of("additional", "details", "other");
    private static final List<String> PROFILE_KEYS =
            List.of("profile", "summary", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "training", "licences", "licenses");

    private SlateOrange() {
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

            // The specialisms are claimed before the profile: their own keys
            // are narrow, but the profile's are broad enough to take a strip
            // titled loosely.
            ParagraphSection specialisms =
                    berth(sections, ParagraphSection.class, SPECIALISM_KEYS);
            ParagraphSection profile =
                    berthExcept(sections, ParagraphSection.class, PROFILE_KEYS, specialisms);
            EntriesSection competencies =
                    berth(sections, EntriesSection.class, COMPETENCY_KEYS);
            EntriesSection achievements =
                    berthExcept(sections, EntriesSection.class, ACHIEVEMENT_KEYS, competencies);
            SkillsSection languages = berth(sections, SkillsSection.class, LANGUAGE_KEYS);
            EntriesSection facts = berth(sections, EntriesSection.class, FACT_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            EntriesSection certifications =
                    berth(sections, EntriesSection.class, CERTIFICATION_KEYS);

            // The sheet is a fixed composition drawn to the paper edge, so the
            // preset sets the page and takes the margin off: every column pads
            // itself, and a session margin would move the fills off the fills.
            document.pageSize(PAGE).margin(DocumentInsets.zero());
            SlateOrangeMasthead.renderChrome(document);

            document.pageFlow(page -> {
                page.name("SlateOrangeCv");
                page.spacing(0);
                SlateOrangeMasthead.render(page, doc.identity(), specialisms);
                page.addRow("Body", row -> {
                    row.spacing(0);
                    row.weights(SIDEBAR_CELL_WEIGHT, MAIN_CELL_WEIGHT);
                    row.addSection("Sidebar", side -> SlateOrangeAside.compose(side,
                            competencies, achievements, languages, facts));
                    row.addSection("MainColumn", main -> SlateOrangeMain.compose(main,
                            profile, experience, education, certifications));
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
