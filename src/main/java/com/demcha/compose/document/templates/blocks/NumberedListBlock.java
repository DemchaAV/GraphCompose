package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that renders as a numbered list of items.
 *
 * <p>Use this for ordered procedures, ranked items, or step-by-step
 * descriptions where the position carries meaning. The Module composer
 * uses an arabic-numeral marker by default; presets can override the
 * marker style during composition.</p>
 *
 * @param items list items in source order (must not be null; may be
 *              empty; individual items must not be null)
 */
public record NumberedListBlock(List<String> items) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no item is null.
     *
     * @throws NullPointerException if {@code items} or any item is null
     */
    public NumberedListBlock {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }
}
