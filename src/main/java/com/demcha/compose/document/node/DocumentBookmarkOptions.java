package com.demcha.compose.document.node;

import java.util.Objects;

/**
 * Backend-neutral outline/bookmark metadata attached to semantic content.
 *
 * @param title display label shown by renderers that support outlines
 * @param level nesting level where {@code 0} is a root bookmark
 * @author Artem Demchyshyn
 */
public record DocumentBookmarkOptions(String title, int level) {
    /**
     * Creates a root-level bookmark entry.
     *
     * @param title display label shown in the outline panel
     */
    public DocumentBookmarkOptions(String title) {
        this(title, 0);
    }

    /**
     * Validates and normalizes bookmark metadata.
     *
     * @param title display label
     * @param level non-negative nesting level
     */
    public DocumentBookmarkOptions {
        title = Objects.requireNonNullElse(title, "").trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Bookmark title must not be blank.");
        }
        level = Math.max(0, level);
    }
}
