package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Block} that renders as a bullet-pointed list of items.
 *
 * <p>Use this for skill lists, project bullet points, or any flat
 * enumeration where each item is one line of text.</p>
 *
 * @param items list items in source order (must not be null; may be
 *              empty; individual items must not be null)
 */
public record BulletListBlock(List<String> items) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no item is null.
     *
     * @throws NullPointerException if {@code items} or any item is null
     */
    public BulletListBlock {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }
}
