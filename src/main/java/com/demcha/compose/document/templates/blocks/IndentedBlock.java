package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that renders as a list of indented title-plus-body
 * entries.
 *
 * <p>Use this for Education ({@code "MSc Computer Science"} title +
 * {@code "University of Manchester | 2021"} body) or Projects
 * ({@code "TaskFlow Studio"} title + project description body) modules.
 * The title sits flush left in a bold weight; the body is indented and
 * follows in the regular body style.</p>
 *
 * @param items entries in source order (must not be null; may be empty;
 *              individual entries must not be null)
 */
public record IndentedBlock(List<Item> items) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no item reference is null.
     *
     * @throws NullPointerException if {@code items} or any element is null
     */
    public IndentedBlock {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    /**
     * One title-plus-body entry inside an {@link IndentedBlock}.
     *
     * @param title bold leading line such as a degree, project name, or
     *              role (must not be null; may be empty)
     * @param body  indented body text such as institution, dates, or
     *              project description (must not be null; may be empty)
     */
    public record Item(String title, String body) {

        /**
         * Compact constructor that rejects null references for either
         * field.
         *
         * @throws NullPointerException if {@code title} or {@code body}
         *                              is null
         */
        public Item {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
        }
    }
}
