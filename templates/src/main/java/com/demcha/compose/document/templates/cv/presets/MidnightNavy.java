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

import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_RATIO;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_RATIO;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.PAGE;

/**
 * Midnight Navy — a one-page CV on a full-height navy plate: an outlined
 * monogram over the name and a tracked role, then contact, education, metered
 * skills and dotted languages down the plate, beside a paper column carrying
 * the summary, the roles held on a rail, three achievement discs and the
 * certifications in divided columns.
 *
 * <h2>The plate is page chrome, not a fill</h2>
 *
 * <p>The navy reaches the top, left and bottom paper edges, and a section's
 * fill stops at its own box — so the plate is a page background sized by the
 * same ratio the body row splits on, and the two cannot drift apart.</p>
 *
 * <h2>Every horizontal pair goes through one wrapper</h2>
 *
 * <p>A row nested directly in a row cell is refused, and both columns are row
 * cells. Each of the sheet's horizontal pairs — a skill and its meter, a
 * language and its dots, a title and its dates, a disc and its line, the
 * certification columns — is therefore a row wrapped in a single layer of a
 * stack.</p>
 *
 * <h2>One page, strictly</h2>
 *
 * <p>The body is a single row and a row is atomic, so this sheet does not
 * paginate: a CV with more content than the design holds is refused with an
 * {@code AtomicNodeTooLargeException} naming the node that could not fit,
 * rather than being cut or silently clipped. That is the honest failure for a
 * design whose two columns have to reach the same foot — splitting the row
 * would leave the navy plate on one page and half the aside on the next.
 * {@link TimelineMinimal} is the preset in this package built to flow.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by title
 * rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has seven:</p>
 *
 * <ul>
 *   <li>education, an {@link EntriesSection} of a qualification, its
 *       institution and its years;</li>
 *   <li>skills and languages, {@link SkillsSection}s whose groups are
 *       flattened — the design draws no group names. A skill's level fills its
 *       meter; a language's fills the nearest of five dots;</li>
 *   <li>the summary, a {@link ParagraphSection};</li>
 *   <li>experience, an {@link EntriesSection} whose bodies are bullets;</li>
 *   <li>achievements, an {@link EntriesSection} whose entries carry a mark and
 *       one line of text — the design gives a card no heading over its line, so
 *       the entry's title <em>is</em> that line;</li>
 *   <li>certifications, an {@link EntriesSection} of a title, its issuer and
 *       its year, one to a column.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, and a berth with no
 * section takes its heading and its rule with it. The contact block's heading
 * is the preset's own, because a document has no section to carry it — the
 * channels come from {@code CvIdentity}.</p>
 *
 * <h2>The marks and the links</h2>
 *
 * <p>An achievement takes the mark its entry names in {@code CvEntry.icon()},
 * from this preset's own vocabulary, and an unknown token is reported as a data
 * error naming the set.</p>
 *
 * <p>The email, the phone and each link are reachable from the PDF, with the
 * {@code mailto:} and {@code tel:} targets built from the values. A link is
 * drawn as its own label with the address behind it, so the plate's width does
 * not depend on how long an address happens to be, and it takes the mark of the
 * network it points at or the globe. A degree, a job title and a certification
 * become links when their entry carries {@code CvEntry.link()}.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Barlow throughout. The templates artifact carries no
 * fonts — register the family on the session, or the engine substitutes and the
 * measured geometry no longer matches the type sitting in it. The role line's
 * tracking is real spaces, so its width depends on the face's space advance
 * specifically.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = MidnightNavy.create();
 * template.compose(session, cv);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class MidnightNavy {

    /** Stable identifier of this preset. */
    public static final String ID = "midnight-navy";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Midnight Navy";

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "awards", "highlights");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certificates", "licences", "licenses");

    private MidnightNavy() {
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

            ParagraphSection summary = berth(sections, ParagraphSection.class, SUMMARY_KEYS);
            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            SkillsSection languages =
                    berth(sections, SkillsSection.class, LANGUAGE_KEYS, skills);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            EntriesSection experience =
                    berth(sections, EntriesSection.class, EXPERIENCE_KEYS, education);
            EntriesSection achievements = berth(sections, EntriesSection.class,
                    ACHIEVEMENT_KEYS, education, experience);
            EntriesSection certifications = berth(sections, EntriesSection.class,
                    CERTIFICATION_KEYS, education, experience, achievements);

            // Every length on the sheet is a share of the design's own grid, so
            // the preset sets the page and its margins rather than following
            // what the caller configured.
            document.pageSize(PAGE);
            document.margin(DocumentInsets.zero());
            document.pageBackgrounds(List.of(PageBackgroundFill.leftColumn(ASIDE_RATIO, NAVY)));

            document.pageFlow(page -> page
                    .name("MidnightNavyCv")
                    .padding(DocumentInsets.zero())
                    .spacing(0)
                    .addRow("PageGrid", row -> {
                        row.spacing(0);
                        row.weights(ASIDE_RATIO, MAIN_RATIO);
                        row.addSection("Aside", column -> MidnightNavyAside.compose(
                                column, doc.identity(), education, skills, languages));
                        row.addSection("Main", column -> MidnightNavyMain.compose(
                                column, summary, experience, achievements, certifications));
                    }));
        }

        /**
         * The first section of the wanted shape whose title names this berth,
         * skipping any another berth has already claimed, or {@code null} when
         * the document fills no such berth.
         */
        @SafeVarargs
        private static <T extends CvSection> T berth(List<CvSection> sections, Class<T> type,
                                                     List<String> keys, T... taken) {
            for (CvSection section : sections) {
                if (!type.isInstance(section) || isTaken(section, taken)) {
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

        private static boolean isTaken(CvSection section, CvSection[] taken) {
            for (CvSection claimed : taken) {
                if (section == claimed) {
                    return true;
                }
            }
            return false;
        }
    }
}
