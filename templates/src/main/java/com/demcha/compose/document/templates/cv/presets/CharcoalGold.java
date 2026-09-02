package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
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

import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MAIN_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.PAPER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_WEIGHT;

/**
 * Charcoal Gold — a one-page CV in two columns: a charcoal sidebar carrying
 * a ringed photograph, the contact channels, rated skills, languages and
 * degrees, beside a paper column carrying a two-tone name, the summary, the
 * roles held on a dated rail, a pair of credential columns and a closing
 * strip of tools.
 *
 * <p>The preset paints both columns as page backgrounds, so each reaches the
 * foot of the sheet whatever it holds. It leaves the page size to the
 * caller, unlike its ported siblings: the design is drawn on A4 and the
 * geometry follows the page's own width, so a different page rescales rather
 * than breaks.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>The two columns are a single row, and a row is atomic — it cannot be
 * split — so a CV longer than the sheet does not flow onto a second page:
 * composing it raises {@code AtomicNodeTooLargeException}, naming the node
 * and the height it needed. It draws no cap of its own, because a CV that
 * quietly loses a job is worse than one that refuses to compose.
 * {@link TimelineMinimal} is the preset in this package that splits its own
 * columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by
 * title rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has seven:</p>
 *
 * <ul>
 *   <li>skills, a {@link SkillsSection}; languages, a {@link RowsSection};
 *       and education, an {@link EntriesSection} — in the sidebar;</li>
 *   <li>the summary, a {@link ParagraphSection} of one paragraph per line,
 *       and experience, an {@link EntriesSection} — in the main column;</li>
 *   <li>certifications and achievements, {@link EntriesSection}s, as the
 *       credential pair;</li>
 *   <li>the tools, a {@link ParagraphSection} of one tool per line, as the
 *       closing strip.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, a berth with no
 * section takes its heading and its divider with it, and a berth is filled
 * by the first section that names it. Languages are rows rather than
 * levelled skills because this design writes the level out — "Native",
 * "B2 – Upper Intermediate" — which a number could not carry back; their
 * {@code RowStyle} is not read.</p>
 *
 * <h2>The name, the photograph and the marks</h2>
 *
 * <p>The masthead changes colour halfway through the name: the given name in
 * ink, the family name larger and in gold. It reads them from
 * {@code CvName}, which is why this preset wants a structured name — nothing
 * else could tell it where to change.</p>
 *
 * <p>The photograph comes from {@code CvIdentity.portrait()} and is drawn to
 * fill its disc, so a picture that is not square is cut rather than
 * squeezed. An identity without one simply starts at the contact block.</p>
 *
 * <p>Each credential takes the mark its entry names in
 * {@code CvEntry.icon()}, from this preset's own vocabulary —
 * {@code certificate}, {@code trophy}, {@code growth}, {@code star} — and an
 * unknown token is reported as a data error naming the set. An entry with no
 * token is drawn without a mark. Every title the preset draws — a role, a
 * credential — becomes a link when its entry carries
 * {@code CvEntry.link()}.</p>
 *
 * <p>A skill's rating is drawn as five dots, of which its level fills the
 * nearest whole number; a skill the document leaves unlevelled draws no
 * rating rather than five empty dots. The phone, the email and each link are
 * reachable from the PDF, with the {@code tel:} and {@code mailto:} targets
 * built from the values.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Lato. The templates artifact does not carry it —
 * register it on the session, or the engine substitutes and the measured
 * geometry here no longer matches the type sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = CharcoalGold.create();
 * template.compose(session, cv);
 * }</pre>
 */
public final class CharcoalGold {

    /** Stable identifier of this preset. */
    public static final String ID = "charcoal-gold";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Charcoal Gold";

    /**
     * The margin this preset expects: none. Both columns run to the paper
     * edge and each carries its own padding.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> TOOL_KEYS =
            List.of("tools", "stack", "software");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> LANGUAGE_KEYS =
            List.of("languages", "language");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "licences", "licenses");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "achievement", "awards");

    private CharcoalGold() {
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

            // Both fills are page backgrounds: a section is only as tall as
            // what it holds, and each column has to reach the foot of the
            // sheet whatever the other one carries.
            document.pageBackgrounds(List.of(
                    PageBackgroundFill.leftColumn(SIDEBAR_WEIGHT, SIDEBAR),
                    PageBackgroundFill.rightColumn(MAIN_WEIGHT, PAPER)));

            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            RowsSection languages = berth(sections, RowsSection.class, LANGUAGE_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            // Tools first: "Technical Tools" names no other berth, but the
            // summary keys are broad and a document is free to title its
            // closing strip loosely.
            ParagraphSection tools = berth(sections, ParagraphSection.class, TOOL_KEYS);
            ParagraphSection summary = berth(sections, ParagraphSection.class, SUMMARY_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection certifications =
                    berth(sections, EntriesSection.class, CERTIFICATION_KEYS);
            EntriesSection achievements =
                    berth(sections, EntriesSection.class, ACHIEVEMENT_KEYS);

            boolean hasCredentials = SectionLookup.hasContent(certifications)
                    || SectionLookup.hasContent(achievements);
            boolean hasTools = SectionLookup.hasContent(tools);

            document.pageFlow(page -> page
                    .name("CharcoalGoldCv")
                    .spacing(0)
                    .addRow("Body", row -> {
                        row.name("Body");
                        row.spacing(0);
                        row.weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("Sidebar", side ->
                                CharcoalGoldAside.compose(side, doc.identity(),
                                        skills, languages, education));
                        row.addSection("MainColumn", main -> {
                            CharcoalGoldMain.compose(main, doc.identity(), summary, experience);
                            if (hasCredentials) {
                                CharcoalGoldCredentials.renderCredentials(main,
                                        certifications, achievements);
                            }
                            if (hasTools) {
                                CharcoalGoldCredentials.renderTools(main, tools);
                            }
                        });
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
