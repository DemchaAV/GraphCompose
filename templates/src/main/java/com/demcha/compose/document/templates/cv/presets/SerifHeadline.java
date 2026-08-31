package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.RowVerticalAlign;
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

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ASIDE_CELL_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.BODY_BAND_TO_CERTIFICATIONS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MAIN_CELL_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PAGE;

/**
 * Serif Headline — a one-page A4 CV under a Volkhov masthead: the name in
 * the display serif over its role and a short gold rule, the contact
 * channels stacked opposite, then a two-column body — the roles held on a
 * timeline rail and the projects as marked cards on the left, the degrees
 * and the skill meters across a hairline divider on the right — closing with
 * full-width bands of certifications and achievements.
 *
 * <h2>Geometry</h2>
 *
 * <p>This sheet is not a set of round numbers. It was drawn on a 1024-pixel
 * grid, so the preset carries that grid's numbers and scales them onto A4 —
 * heights by an extra factor, because the drawing is proportionally taller
 * than the page. Vertical gaps are stated as the white the drawing shows
 * between letters, with the blank a line box carries above and below its own
 * type subtracted; setting the pixel numbers directly would push every block
 * a few points too far apart.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>This sheet holds one page of content. The body is a single row, and a
 * row is atomic — it cannot be split — so a CV longer than the sheet does
 * not flow onto a second page: composing it raises
 * {@code AtomicNodeTooLargeException}, naming the node and the height it
 * needed. It draws no cap of its own, because a CV that quietly loses a job
 * is worse than one that refuses to compose. {@link TimelineMinimal} is the
 * preset in this package that splits its own columns across pages.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by
 * title rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has eight:</p>
 *
 * <ul>
 *   <li>the summary, a {@link ParagraphSection} set under the masthead;</li>
 *   <li>experience and projects, {@link EntriesSection}s in the wide
 *       column;</li>
 *   <li>education, an {@link EntriesSection}, and skills, a
 *       {@link SkillsSection}, in the narrow one;</li>
 *   <li>soft skills, a {@link ParagraphSection} of one line each, under the
 *       skills;</li>
 *   <li>certifications, a {@link RowsSection} of qualification and issuer,
 *       and achievements, an {@link EntriesSection}, as the closing
 *       bands.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, a berth with no
 * section takes its heading with it, and a berth is filled by the first
 * section that names it. The certifications' {@code RowStyle} is not read —
 * the rows are drawn to this design rather than to a family style — and the
 * skills heading covers the soft skills too, so a document with only soft
 * skills is headed "Skills".</p>
 *
 * <h2>Marks</h2>
 *
 * <p>Each project and each achievement is opened by the mark its entry names
 * in {@code CvEntry.icon()}. The vocabulary is this preset's own —
 * {@code cart}, {@code api}, {@code trophy}, {@code chart}, {@code rocket} —
 * and an unknown token is reported as a data error naming the set. An entry
 * with no token is drawn without a mark. The certification medal is chrome
 * rather than data: every plate takes the same one.</p>
 *
 * <p>The employer's city and the campus come from {@code CvEntry.place()},
 * because this design sets them beside the employer and the years in their
 * own colour rather than inside them.</p>
 *
 * <p>The phone, the email and each link are reachable from the PDF, with the
 * {@code tel:} and {@code mailto:} targets built from the values. The set
 * packages two network marks: a link naming GitHub takes that one, anything
 * else takes LinkedIn's.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The name is set in Volkhov and everything else in Lato. The templates
 * artifact carries neither — register them on the session, or the engine
 * substitutes and the measured geometry here no longer matches the type
 * sitting in it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<CvDocument> template = SerifHeadline.create();
 * template.compose(session, cv);
 * }</pre>
 */
public final class SerifHeadline {

    /** Stable identifier of this preset. */
    public static final String ID = "serif-headline";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Serif Headline";

    /**
     * The margin this preset expects: none of the caller's. It sets the page
     * margin itself, because the grid the sheet is drawn on defines it.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private static final List<String> SUMMARY_KEYS =
            List.of("summary", "profile", "about");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> PROJECT_KEYS =
            List.of("projects", "project", "portfolio");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> SOFT_SKILL_KEYS =
            List.of("soft skills", "strengths");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certification", "licences", "licenses");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "achievement", "awards");

    private SerifHeadline() {
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

            document.pageSize(PAGE).margin(DocumentInsets.of(MARGIN));

            ParagraphSection summary = berth(sections, ParagraphSection.class, SUMMARY_KEYS);
            EntriesSection experience = berth(sections, EntriesSection.class, EXPERIENCE_KEYS);
            EntriesSection projects = berth(sections, EntriesSection.class, PROJECT_KEYS);
            EntriesSection education = berth(sections, EntriesSection.class, EDUCATION_KEYS);
            // Soft skills first: "Soft Skills" names the skills berth too, and
            // the block that has a shape of its own should win it.
            ParagraphSection softSkills =
                    berth(sections, ParagraphSection.class, SOFT_SKILL_KEYS);
            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            RowsSection certifications =
                    berth(sections, RowsSection.class, CERTIFICATION_KEYS);
            EntriesSection achievements =
                    berth(sections, EntriesSection.class, ACHIEVEMENT_KEYS);

            boolean hasBody = SectionLookup.hasContent(experience)
                    || SectionLookup.hasContent(projects)
                    || SectionLookup.hasContent(education)
                    || SectionLookup.hasContent(skills)
                    || SectionLookup.hasContent(softSkills);
            boolean hasCertifications = SectionLookup.hasContent(certifications);
            boolean hasAchievements = SectionLookup.hasContent(achievements);

            document.pageFlow(page -> {
                page.name("SerifHeadlineCv").spacing(0).padding(DocumentInsets.zero());
                SerifHeadlineMasthead.compose(page, doc.identity(), summary);
                if (hasBody) {
                    page.addRow("BodyColumns", row -> {
                        row.spacing(0);
                        row.gap(0);
                        row.weights(MAIN_CELL_WEIGHT, ASIDE_CELL_WEIGHT);
                        // The row splits on the gutter's midline, so the
                        // divider the right column hangs off lands in the
                        // middle of the gutter rather than on its edge.
                        row.verticalAlign(RowVerticalAlign.TOP);
                        row.margin(new DocumentInsets(
                                0, 0, BODY_BAND_TO_CERTIFICATIONS, 0));
                        row.addSection("MainColumn", section ->
                                SerifHeadlineMain.compose(section, experience, projects));
                        row.addSection("AsideColumn", section ->
                                SerifHeadlineAside.compose(section, education, skills,
                                        softSkills));
                    });
                }
                if (hasCertifications) {
                    SerifHeadlineClosing.renderCertifications(page, certifications,
                            hasAchievements);
                }
                if (hasAchievements) {
                    SerifHeadlineClosing.renderAchievements(page, achievements);
                }
            });
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
