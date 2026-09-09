package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.LayoutAnchorId;

import java.util.Objects;

/**
 * Non-visual marker fragment payload that reports where an anchored subtree landed.
 *
 * <p>One of these is emitted per page a {@code LayoutAnchorNode} occupies, and it draws
 * nothing — the backends register a no-op handler for it. A post-layout pass reads the
 * resolved fragments, keeps the ones carrying this payload, and gets the anchor's page and
 * position without knowing anything about what the anchored subtree drew.</p>
 *
 * <p><strong>The size is the anchored child's, not its container's.</strong> An 8×8 marker
 * inside a table cell is placed in a 16pt-wide cell, and an anchor reporting the cell would
 * be no use to anything looking for the marker. A marker drawn as three stacked shapes
 * still gets one box, which is what makes the anchor a <em>logical owner</em> rather than a
 * particular draw fragment.</p>
 *
 * <p>The box is the child's <b>border box</b> — its margin excluded, its padding included —
 * and, when the child spans pages, the <b>slice of that box on this page</b> rather than
 * the whole subtree. {@code LayoutAnchorDefinition} holds the details, including which
 * margin edge comes off which slice.</p>
 *
 * @param id     the anchor's identity, shared by every page's slice
 * @param width  the anchored child's border-box width
 * @param height the height of this page's slice of that box
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record LayoutAnchorPayload(LayoutAnchorId id, double width, double height) {

    /**
     * Validates the payload.
     *
     * @throws NullPointerException     if {@code id} is null
     * @throws IllegalArgumentException if a dimension is negative or non-finite
     */
    public LayoutAnchorPayload {
        Objects.requireNonNull(id, "id");
        requireSize(width, "width");
        requireSize(height, "height");
    }

    private static void requireSize(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    "Anchor " + name + " must be finite and non-negative, was " + value + ".");
        }
    }
}
