package com.demcha.compose.document.templates.cv.data;

/**
 * How a {@link ModuleSection}'s items are laid out — the presentation
 * shape the author picks at runtime, independent of what the section
 * means.
 *
 * <p>This is the axis that lets one {@link CvItem} record serve every
 * module: the kind decides which of the item's optional fields are
 * <strong>read</strong> and which are ignored. An item carrying a
 * {@code period} rendered under {@link #ENTRIES} simply does not show
 * a date column — the same data under {@link #ENTRIES_DATED} does.
 * Each constant below names exactly what it reads, so "ignored" is a
 * documented contract rather than a surprise.</p>
 *
 * <p>Every kind lowers onto the renderers this package already ships
 * (see {@code components.ModuleRenderer}); none of them draws
 * anything a hand-built {@link RowsSection}, {@link EntriesSection} or
 * {@link ParagraphSection} could not.</p>
 *
 * <p>The orthogonal axes are {@link SectionRole} — what the section
 * <em>means</em>, which is what a multi-column preset places on — and
 * {@link BodyStyle}, which decides how one item's description lines
 * render. Keeping them apart is what lets a "Volunteering" module be
 * shaped exactly like Education without a new type.</p>
 *
 * @since 2.3.0
 */
public enum CvKind {

    /**
     * Prose — a summary, an objective, a statement. Each item renders
     * as its description, one paragraph per body line.
     *
     * <p>Reads {@code body} only. The {@code title} is ignored here on
     * purpose: the section already carries a heading, and a prose block
     * that repeated it would print the same words twice. For a labelled
     * one-liner ({@code Languages: English, German}) reach for
     * {@link #INLINE_LIST}, which is what that shape is.</p>
     */
    PARAGRAPH,

    /**
     * A bullet per item, description on the same line —
     * {@code • Throughput: doubled it}. The shape of a short list where
     * each entry is a label and a value ({@link RowStyle#BULLETED}).
     *
     * <p>Reads {@code title}, {@code link}, {@code body}. Ignores
     * {@code subtitle}, {@code period}, {@code location}. A body of
     * several lines is joined with spaces; if the lines are meant to
     * stand apart, the module wants {@link #BULLETS_STACKED}.</p>
     */
    BULLETS,

    /**
     * A bullet per item, description stacked underneath and indented to
     * the title — the shape a Projects section takes when the
     * description is a sentence rather than a value
     * ({@link RowStyle#BULLETED_STACKED}).
     *
     * <p>Reads {@code title}, {@code link}, {@code body}. Ignores
     * {@code subtitle}, {@code period}, {@code location}.</p>
     *
     * <p>Inline or stacked is the module's choice, not something
     * inferred from how long a description happens to be: the same
     * section reads one way throughout, and an author who picked
     * "bulleted list with descriptions underneath" gets it whether the
     * first entry is one line or five.</p>
     */
    BULLETS_STACKED,

    /**
     * One line per item, the description collapsed into a
     * comma-separated run after a bold label —
     * {@code Languages: Java 21, Kotlin, SQL}. The shape skills and
     * languages take in a narrow column.
     *
     * <p>Reads {@code title} and {@code body}. Ignores {@code link},
     * {@code subtitle}, {@code period}, {@code location}.</p>
     */
    INLINE_LIST,

    /**
     * Timeline entries without the date column: bold title, italic
     * subtitle line, description beneath.
     *
     * <p>Reads {@code title}, {@code link}, {@code subtitle},
     * {@code location}, {@code body}. Ignores {@code period} — this
     * is the kind to pick when the dates exist in the data but should
     * not show.</p>
     */
    ENTRIES,

    /**
     * Timeline entries with the date column right-aligned against the
     * title — Education, Experience, and anything shaped like them.
     *
     * <p>Reads every field: {@code title}, {@code link},
     * {@code subtitle}, {@code period}, {@code location},
     * {@code body}.</p>
     */
    ENTRIES_DATED
}
