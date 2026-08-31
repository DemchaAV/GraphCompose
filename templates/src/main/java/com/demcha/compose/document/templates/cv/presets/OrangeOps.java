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
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ASIDE_CELL_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.COLUMN_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.GUTTER;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MAIN_CELL_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MARGIN_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MARGIN_TOP;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MARGIN_X;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.PAGE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE;

/**
 * Orange Ops — a one-page operations CV: a two-tone name over a dark role bar
 * with accent slashes, a contact strip across the page, and a body split into a
 * narrow aside of skills, achievement discs, a degree and certifications
 * against a wide column of profile, roles, a metric strip and closing lines.
 *
 * <h2>The caller registers the display family</h2>
 *
 * <p>The sheet is set in Lato, with Oswald for the name, the headings and the
 * achievement titles. Oswald is not one of the families
 * {@code graph-compose-fonts} carries and the templates artifact carries no
 * fonts at all, so the family is <em>named</em> here and registered by whoever
 * composes the document:</p>
 *
 * <pre>{@code
 * document.registerFontFamily(
 *         FontFamilyDefinition.classpath(OrangeOps.DISPLAY_FONT, "/fonts/oswald/Oswald-Regular.ttf")
 *                 .wordFamily("Oswald")
 *                 .boldResource("/fonts/oswald/Oswald-SemiBold.ttf")
 *                 .build());
 * DocumentTemplate<CvDocument> template = OrangeOps.create();
 * template.compose(document, cv);
 * }</pre>
 *
 * <p>Without that registration the engine substitutes, and every length on the
 * sheet was measured against a condensed face — the name alone is set at the
 * size whose cap band matches the design's, so a wider face runs it off the
 * page.</p>
 *
 * <h2>Why the page margins are zero</h2>
 *
 * <p>The role bar bleeds past the left margin to the paper's edge, and a
 * section's bleed extends its own fill rather than a shape inside it — so it
 * cannot carry the trapezoid out to the edge. The session therefore takes
 * {@link DocumentInsets} of zero on both sides and each band applies the
 * horizontal margin as its own padding: one constant in four places rather than
 * a special case for one band.</p>
 *
 * <h2>Every horizontal pair inside a column is a table or a layer stack</h2>
 *
 * <p>The aside and the main column are row cells, and a row cannot nest inside
 * one. So every horizontal arrangement inside them is a table, a layer stack,
 * or an inline run — never a nested row. A table cell here is only ever given a
 * paragraph or a spacer: a section with composite children in a cell reserves
 * its box and draws nothing.</p>
 *
 * <h2>One page, and what happens past it</h2>
 *
 * <p>Each column is a stack of blocks, so a CV with more content than the
 * design holds runs onto a second page rather than losing anything — but the
 * sheet is drawn for one page, and a longer one will look like a one-page
 * design that ran over. {@link TimelineMinimal} is the preset in this package
 * built to paginate.</p>
 *
 * <h2>How a document reaches its berth</h2>
 *
 * <p>The columns are fixed, so the preset assigns each section a berth by title
 * rather than reading {@link
 * com.demcha.compose.document.templates.cv.data.Slot}. It has eight — four in
 * the aside and four in the main column, in the order the design stacks
 * them:</p>
 *
 * <ul>
 *   <li>skills, a {@link SkillsSection} whose groups are flattened into one
 *       dotted list — the design draws no group names;</li>
 *   <li>achievements, an {@link EntriesSection} whose entries carry a mark, a
 *       title and a body — the discs;</li>
 *   <li>education, an {@link EntriesSection} whose first entry is drawn, its
 *       body one line per line;</li>
 *   <li>certifications, an {@link EntriesSection} of a title over its issuer;</li>
 *   <li>the profile, a {@link ParagraphSection};</li>
 *   <li>experience, an {@link EntriesSection} whose bodies are bullets;</li>
 *   <li>the metric strip, an {@link EntriesSection} whose entries carry the
 *       number as the title and the caption below it as the body;</li>
 *   <li>the closing lines, an {@link EntriesSection} of a label and its value.</li>
 * </ul>
 *
 * <p>A section whose title matches no berth is not drawn, and a berth with no
 * section takes its heading, its rule and its join hairline with it.</p>
 *
 * <p>A main-column berth's title is split at its first opening bracket: what
 * comes before is the heading, and the bracket and everything after it is set
 * smaller beside it, which is how the design writes {@code KEY KPI SNAPSHOT
 * (Recent 12 Months)}. An aside heading is drawn whole.</p>
 *
 * <h2>The marks and the links</h2>
 *
 * <p>An achievement, the degree, a metric and a closing line each take the mark
 * its entry names in {@code CvEntry.icon()}, from this preset's own vocabulary,
 * and an unknown token is reported as a data error naming the set. An entry
 * with no token is drawn without a mark.</p>
 *
 * <p>The email, the phone and each link are reachable from the PDF, with the
 * {@code mailto:} and {@code tel:} targets built from the values. A link is
 * drawn as its own label with the address behind it, so the contact strip's
 * width does not depend on how long an address happens to be, and it takes the
 * mark of the network it points at or the globe. An achievement title, a job
 * title, the degree and a certification become links when their entry carries
 * {@code CvEntry.link()}.</p>
 *
 * @since 2.2.3
 */
public final class OrangeOps {

    /** Stable identifier of this preset. */
    public static final String ID = "orange-ops";

    /** Human-readable name of this preset. */
    public static final String DISPLAY_NAME = "Orange Ops";

    /**
     * The display family this preset sets its name and headings in.
     *
     * <p>Named, not carried — register a family under this name on the session
     * before composing. See the class documentation.</p>
     */
    public static final FontName DISPLAY_FONT = OrangeOpsStyles.DISPLAY_FONT;

    private static final List<String> PROFILE_KEYS =
            List.of("profile", "summary", "about");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "expertise", "competencies");
    private static final List<String> ACHIEVEMENT_KEYS =
            List.of("achievements", "awards", "highlights");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "qualifications");
    private static final List<String> CERTIFICATION_KEYS =
            List.of("certifications", "certificates", "licences", "licenses");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "work history", "career");
    private static final List<String> METRIC_KEYS =
            List.of("kpi", "metrics", "snapshot", "results");
    private static final List<String> ADDITIONAL_KEYS =
            List.of("additional", "other", "extras", "interests");

    private OrangeOps() {
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

            SkillsSection skills = berth(sections, SkillsSection.class, SKILL_KEYS);
            ParagraphSection profile = berth(sections, ParagraphSection.class, PROFILE_KEYS);

            // Six berths take the same shape, so each is claimed against what
            // the ones before it took: the metric keys are broad enough to
            // catch an achievements block titled "Results", and the closing
            // keys broad enough to catch anything left over.
            EntriesSection achievements =
                    berth(sections, EntriesSection.class, ACHIEVEMENT_KEYS);
            EntriesSection education =
                    berth(sections, EntriesSection.class, EDUCATION_KEYS, achievements);
            EntriesSection certifications = berth(sections, EntriesSection.class,
                    CERTIFICATION_KEYS, achievements, education);
            EntriesSection experience = berth(sections, EntriesSection.class,
                    EXPERIENCE_KEYS, achievements, education, certifications);
            EntriesSection metrics = berth(sections, EntriesSection.class,
                    METRIC_KEYS, achievements, education, certifications, experience);
            EntriesSection additional = berth(sections, EntriesSection.class, ADDITIONAL_KEYS,
                    achievements, education, certifications, experience, metrics);

            // Every length on the sheet is a share of the design's own grid, so
            // the preset sets the page and its margins rather than following
            // what the caller configured.
            document.pageSize(PAGE).margin(new DocumentInsets(MARGIN_TOP, 0, MARGIN_BOTTOM, 0));
            document.pageFlow(page -> {
                page.name("OrangeOpsCv");
                page.spacing(0);
                OrangeOpsMasthead.render(page, doc.identity());
                page.addRow("Body", row -> {
                    row.spacing(0);
                    row.weights(ASIDE_CELL_WEIGHT, MAIN_CELL_WEIGHT);
                    row.addSection("Aside", side -> {
                        side.spacing(0);
                        side.padding(0f, (float) (GUTTER / 2), 0f, (float) MARGIN_X);
                        OrangeOpsAside.compose(side, skills, achievements, education,
                                certifications);
                    });
                    row.addSection("Main", main -> {
                        main.spacing(0);
                        main.padding(0f, (float) MARGIN_X, 0f, (float) (GUTTER / 2));
                        // The divider is the main cell's own left accent: its x
                        // is that cell's left edge, so it cannot drift from the
                        // split, and its height is the column's. A page
                        // background would fix both by ratio and drift the
                        // moment the content changed.
                        main.accentLeft(RULE, COLUMN_RULE_THICKNESS);
                        OrangeOpsMain.compose(main, profile, experience, metrics, additional);
                    });
                });
            });
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
