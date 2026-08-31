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

import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MARGIN_X;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PAD_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PAGE;

/**
 * Teal Pulse — a clinical one-page CV in five stacked bands: a heart crossed
 * by a pulse beside the name, a contact strip divided by short rules, a
 * two-column body carrying the competencies beside the summary and the roles,
 * a three-column closing band, and a tracked line under a rule that ends in a
 * small heart.
 *
 * <p>Exactly one of those bands is two-column, and both vertical rules on the
 * sheet are the left border of the column to their right rather than lines
 * placed beside it — so they are the grid: change a weight and the rules
 * follow.</p>
 *
 * <h2>The page is the design's, not the caller's</h2>
 *
 * <p>The design was drawn on a raster whose proportion is not A4's, and every
 * length on the sheet is a share of that grid, so the preset sets its own page
 * size and margin. A session configured by the caller is overwritten. This is
 * the opposite choice from {@link CharcoalGold}, which follows the page it is
 * given, and it is made here because the sheet is a fixed composition rather
 * than a layout that rescales.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>The sheet is drawn for one page but does not insist on it, which sets it
 * apart from the other ported presets in this package. The bands are stacked
 * in the page flow, so a CV with more roles than the design holds carries the
 * closing band onto a second page rather than losing anything — it will look
 * like a one-page design that ran over, because that is what it is.</p>
 *
 * <p>What cannot flow is the body itself: its two columns are a single row,
 * and a row is atomic. A body taller than one page — the competencies beside
 * the summary and every role — raises {@code AtomicNodeTooLargeException},
 * naming the node and the height it needed. The preset draws no cap of its
 * own, because a CV that quietly loses a role is worse than one that refuses
 * to compose. {@link TimelineMinimal} is the preset in this package that
 * splits its own columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The bands are fixed, so the preset assigns each section a berth by title
 * rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has six:</p>
 *
 * <ul>
 *   <li>competencies, a {@link SkillsSection} — the narrow column;</li>
 *   <li>the summary, a {@link ParagraphSection}, and experience, an
 *       {@link EntriesSection} whose bodies are one highlight per line — the
 *       wide column;</li>
 *   <li>education, certifications and the closing facts,
 *       {@link EntriesSection}s — the three columns of the closing band;</li>
 *   <li>the tagline, a {@link ParagraphSection} whose body is the line under
 *       the closing rule. Only its body is drawn: the title is how the
 *       document names the berth, not something the sheet prints.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, and a berth with no
 * section takes its badge and heading with it. The competencies are skills
 * without levels: this design writes them as dotted lines, so a rating would
 * have nowhere to go.</p>
 *
 * <h2>The marks, the tracking and the links</h2>
 *
 * <p>Every mark is chrome — the brand's two halves, the four contact glyphs,
 * the five badges and the closing heart — so no document names one, and the
 * badge each band carries is fixed by the berth it heads.</p>
 *
 * <p>Headings are letter-spaced by writing each character as its own run with
 * a small space between: a text style carries no letter-spacing, and the gap
 * is split across several spacers because a run's size sets its line's height
 * as well as its advance.</p>
 *
 * <p>The email, the phone and each link are reachable from the PDF, with the
 * {@code mailto:} and {@code tel:} targets built from the values. A link is
 * drawn as its own label with the address behind it — {@code Link("LinkedIn",
 * "https://…")} sets the word and links the URL — so the strip's gaps do not
 * depend on how long a profile's address happens to be. A role and a degree
 * become links when their entry carries {@code CvEntry.link()}.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato, with Poppins for the name and Barlow Condensed
 * for the headings. The templates artifact carries none of them — register
 * them on the session, or the engine substitutes and the measured geometry
 * here no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = TealPulse.create();
 * template.compose(session, cv);
 * }</pre>
 *
 * @since 2.2.3
 */
public final class TealPulse {

    /** Stable identifier of this preset. */
    public static final String ID = "teal-pulse";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Teal Pulse";

    private static final List<String> COMPETENCY_KEYS =
            List.of("competencies", "competency", "skills", "expertise");
    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> TAGLINE_KEYS =
            List.of("tagline", "motto", "closing");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "licences", "licenses");
    private static final List<String> FACT_KEYS =
            List.of("additional", "details", "other");

    private TealPulse() {
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

            SkillsSection competencies = berth(sections, SkillsSection.class, COMPETENCY_KEYS);
            // The tagline is claimed first: its own keys are narrow, but the
            // summary's are broad enough to take a closing line titled loosely.
            ParagraphSection tagline = berth(sections, ParagraphSection.class, TAGLINE_KEYS);
            ParagraphSection summary =
                    berthExcept(sections, ParagraphSection.class, SUMMARY_KEYS, tagline);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            EntriesSection certifications =
                    berth(sections, EntriesSection.class, CERTIFICATION_KEYS);
            EntriesSection facts = berth(sections, EntriesSection.class, FACT_KEYS);

            // The sheet is a fixed composition on the design's own page, so the
            // preset sets both rather than following what the caller configured.
            document.pageSize(PAGE).margin(DocumentInsets.zero());
            document.pageFlow(page -> {
                page.name("TealPulseCv");
                page.spacing(0);
                page.padding(new DocumentInsets(PAD_TOP, MARGIN_X, PAD_BOTTOM, MARGIN_X));
                TealPulseMasthead.render(page, doc.identity());
                TealPulseBody.render(page, competencies, summary, experience);
                TealPulseClosing.render(page, education, certifications, facts, tagline);
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
