package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that captures a stack of education / certification
 * entries with each field (degree, institution, year, details)
 * supplied separately so presets can place them precisely without
 * re-parsing a concatenated source string.
 *
 * <p>This is the <strong>preferred shape</strong> for "Education",
 * "Education &amp; Certifications", "Qualifications" or any module
 * whose body is a list of degree / course entries. The
 * {@code BoxedSections} preset renders each {@link Item} with the
 * same structured layout as {@code WorkHistoryBlock}: degree bold on
 * the left, year right-aligned on the same row, institution italic
 * on the next line under the degree, and details as a full-width
 * paragraph beneath. Other presets fall back to a single inline
 * paragraph per item.</p>
 *
 * <p><strong>Legacy alternative.</strong> Authors may still pass
 * education as a {@link MultiParagraphBlock} of pipe-separated
 * strings — e.g.
 * {@code "**Degree** - Institution | Year. Details..."} — and the
 * legacy parser tries to interpret them. Prefer
 * {@code EducationBlock} in new code: the structured fields are
 * explicit, do not depend on the parser's separator and date
 * heuristics (which over-trigger on prose containing stray hyphens
 * like "First-class"), and survive copy-paste from spreadsheets
 * without quoting concerns.</p>
 *
 * @param items education entries in source order, most-recent-first
 *              by convention (must not be null; may be empty;
 *              individual items must not be null)
 */
public record EducationBlock(List<Item> items) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no item reference is null.
     *
     * @throws NullPointerException if {@code items} or any element is
     *                              null
     */
    public EducationBlock {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    /**
     * One row in an education stack. All four fields are required
     * non-null strings but may be blank — a blank {@code institution}
     * collapses the subtitle line, a blank {@code details} collapses
     * the body paragraph, and a blank {@code year} renders the degree
     * row without a right-aligned year column.
     *
     * @param degree      degree / qualification name, e.g.
     *                    {@code "MSc Computer Science"} or
     *                    {@code "Oracle Java Certification"}
     * @param institution awarding institution, e.g.
     *                    {@code "University of Manchester"} or
     *                    {@code "Professional track"}
     * @param year        year or year range, e.g. {@code "2021"},
     *                    {@code "2018-2021"}
     * @param details     additional details (honours, thesis,
     *                    specialisation, course content); free prose,
     *                    may contain inline markdown
     *                    ({@code **bold**}, {@code *italic*})
     */
    public record Item(String degree, String institution, String year, String details) {

        /**
         * Compact constructor: rejects null fields. Use empty strings
         * for absent values rather than null.
         */
        public Item {
            Objects.requireNonNull(degree, "degree");
            Objects.requireNonNull(institution, "institution");
            Objects.requireNonNull(year, "year");
            Objects.requireNonNull(details, "details");
        }
    }
}
