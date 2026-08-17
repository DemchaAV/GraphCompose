package com.demcha.compose.document.templates.cv.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A section assembled at runtime: a heading, what it means
 * ({@link SectionRole}), how it draws ({@link CvKind}), and the
 * {@link CvItem}s it holds.
 *
 * <p>The other {@link CvSection} implementations each fix one shape at
 * compile time — {@link ParagraphSection} is prose, {@link RowsSection}
 * is rows, {@link EntriesSection} is a timeline. That is the right
 * model for a CV written in Java, where the author picks the record and
 * the compiler checks it. It is the wrong one for a CV assembled from
 * data at runtime: a user who has just chosen "Volunteering, shaped
 * like Education, with dates" cannot instantiate a different record per
 * choice, and every new shape would mean a new type.</p>
 *
 * <p>So this record moves the choice into a value. One item type carries
 * every optional field; the kind decides which are read and which are
 * ignored; the role says where the section belongs without a preset
 * having to recognise its heading. The result is that a module nobody
 * anticipated needs no new code — only a different
 * {@code (role, kind)} pair.</p>
 *
 * <p>It renders through the same components as everything else. Every
 * kind lowers onto {@link ParagraphSection}-, {@link RowsSection}- or
 * {@link EntriesSection}-shaped output, so a module drawn as
 * {@link CvKind#ENTRIES_DATED} is laid out exactly like the
 * {@code EntriesSection} carrying the same content — which the parity
 * suite holds to, layout node for layout node.</p>
 *
 * <pre>{@code
 * ModuleSection.builder("Volunteering", SectionRole.OTHER, CvKind.ENTRIES_DATED)
 *     .item(CvItem.of("Mentor, Rails Girls")
 *                 .at("Rails Girls Berlin")
 *                 .period("2019 - 2021")
 *                 .bullets("Ran three weekend workshops"))
 *     .build();
 * }</pre>
 *
 * @param title non-blank banner heading, in the author's own words
 * @param role  what the section means; {@link SectionRole#OTHER} when
 *              the catalogue has no name for it
 * @param kind  how the items draw
 * @param items ordered items; null entries are dropped
 * @since 2.3.0
 */
public record ModuleSection(String title, SectionRole role, CvKind kind, List<CvItem> items)
        implements CvSection {

    /**
     * Validates that every field is non-null and {@code title} is
     * non-blank, drops null items, and defensively copies the list.
     */
    public ModuleSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(items, "items");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        List<CvItem> cleaned = new ArrayList<>(items.size());
        for (CvItem item : items) {
            if (item != null) {
                cleaned.add(item);
            }
        }
        items = List.copyOf(cleaned);
    }

    /**
     * Fluent builder seeded with the three choices that define the
     * module.
     *
     * @param title non-blank banner heading
     * @param role  what the section means
     * @param kind  how its items draw
     * @return new builder
     */
    public static Builder builder(String title, SectionRole role, CvKind kind) {
        return new Builder(title, role, kind);
    }

    /**
     * Module assembled from a fixed set of items.
     *
     * @param title non-blank banner heading
     * @param role  what the section means
     * @param kind  how its items draw
     * @param items items in source order; null becomes empty
     * @return a {@code ModuleSection} carrying the supplied items
     */
    public static ModuleSection of(String title, SectionRole role, CvKind kind, CvItem... items) {
        return new ModuleSection(title, role, kind,
                items == null ? List.of() : Arrays.asList(items));
    }

    /**
     * Prose module — the common case of a summary or objective, where
     * the section is one block of text and naming a kind and a role
     * adds nothing.
     *
     * @param title non-blank banner heading
     * @param text  the prose; each argument is its own paragraph
     * @return a {@link CvKind#PARAGRAPH} module under
     * {@link SectionRole#SUMMARY}
     */
    public static ModuleSection summary(String title, String... text) {
        // The item's title is the section's: PARAGRAPH reads only the body, so
        // it names the item without reaching the page.
        return of(title, SectionRole.SUMMARY, CvKind.PARAGRAPH,
                CvItem.of(title).paragraphs(text));
    }

    /**
     * Mutable builder.
     */
    public static final class Builder {
        private final String title;
        private final SectionRole role;
        private final CvKind kind;
        private final List<CvItem> items = new ArrayList<>();

        private Builder(String title, SectionRole role, CvKind kind) {
            this.title = title;
            this.role = role;
            this.kind = kind;
        }

        /**
         * Appends one pre-built item.
         *
         * @param item the item to append (non-null)
         * @return this builder for chaining
         */
        public Builder item(CvItem item) {
            this.items.add(Objects.requireNonNull(item, "item"));
            return this;
        }

        /**
         * Appends a title-only item — the shape a bulleted list or an
         * inline list of one-liners takes.
         *
         * @param title what the entry is called; required, non-blank
         * @return this builder for chaining
         */
        public Builder item(String title) {
            return item(CvItem.of(title));
        }

        /**
         * Appends a labelled item whose description is the given
         * lines, read as prose.
         *
         * @param title what the entry is called; required, non-blank
         * @param body  description lines; null or blank lines dropped
         * @return this builder for chaining
         */
        public Builder item(String title, String... body) {
            return item(CvItem.of(title).paragraphs(body));
        }

        /**
         * Builds the immutable {@link ModuleSection}.
         *
         * @return the assembled section
         */
        public ModuleSection build() {
            return new ModuleSection(title, role, kind, items);
        }
    }
}
