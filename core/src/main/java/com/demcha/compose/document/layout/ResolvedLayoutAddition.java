package com.demcha.compose.document.layout;

import java.util.Objects;

/**
 * One fragment a pass wants added, and where it belongs relative to the body.
 *
 * @param depth    behind or in front of the document body
 * @param fragment the fragment to add, already in page coordinates
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record ResolvedLayoutAddition(LayoutDepth depth, PlacedFragment fragment) {

    /**
     * Validates the addition.
     *
     * @throws NullPointerException if either component is null
     */
    public ResolvedLayoutAddition {
        Objects.requireNonNull(depth, "depth");
        Objects.requireNonNull(fragment, "fragment");
    }
}
