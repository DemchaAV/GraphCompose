package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that captures a stack of work-history entries with
 * each field (title, organisation, date, description) supplied
 * separately so presets can place them precisely without re-parsing a
 * concatenated source string.
 *
 * <p>This is the <strong>preferred shape</strong> for "Professional
 * Experience", "Work History", or any module whose body is a list of
 * job entries. The {@code BoxedSections} preset renders each
 * {@link Item} as a row with the title bold on the left and the date
 * right-aligned, the organisation italic on the next line under the
 * title, and the description as a full-width paragraph beneath. Other
 * presets fall back to a single inline paragraph per item.</p>
 *
 * <p><strong>Legacy alternative.</strong> Authors may still pass work
 * history as a {@link MultiParagraphBlock} of pipe-separated strings —
 * e.g. {@code "**Title**, Organisation | *Date* — Description"} — and
 * {@code BoxedSections} parses that shape for backward compatibility.
 * Prefer {@code WorkHistoryBlock} in new code: the structured fields
 * are explicit, do not depend on the parser's separator heuristics,
 * and survive copy-paste from spreadsheets without quoting concerns.</p>
 *
 * @param items work-history entries in source order, oldest-last by
 *              convention (must not be null; may be empty;
 *              individual items must not be null)
 */
public record WorkHistoryBlock(List<Item> items) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no item reference is null.
     *
     * @throws NullPointerException if {@code items} or any element is
     *                              null
     */
    public WorkHistoryBlock {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    /**
     * One row in a work-history stack. All four fields are required
     * non-null strings but may be blank — a blank {@code organisation}
     * collapses the subtitle line, a blank {@code description}
     * collapses the body paragraph, and a blank {@code date} renders
     * the title row without a right-aligned date column.
     *
     * @param title        role / position, e.g. {@code "Senior Platform Engineer"}
     * @param organisation employer or organisation, e.g. {@code "Northwind Systems"}
     * @param date         date range or label, e.g. {@code "2024-Present"}, {@code "Jan 2023 – Mar 2024"}
     * @param description  what the role delivered, free prose; may
     *                     contain inline markdown ({@code **bold**},
     *                     {@code *italic*})
     */
    public record Item(String title, String organisation, String date, String description) {

        /**
         * Compact constructor: rejects null fields. Use empty strings
         * for absent values rather than null.
         */
        public Item {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(organisation, "organisation");
            Objects.requireNonNull(date, "date");
            Objects.requireNonNull(description, "description");
        }
    }
}
