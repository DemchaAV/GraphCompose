package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.LayoutAnchorId;

import java.util.Objects;

/**
 * Non-visual marker fragment payload that reports where an anchored subtree landed.
 *
 * <p>One of these is emitted per {@code LayoutAnchorNode}, and it draws nothing — the
 * backends register a no-op handler for it. A post-layout pass reads the resolved
 * fragments, keeps the ones carrying this payload, and gets the anchor's page and
 * position without knowing anything about what the anchored subtree drew.</p>
 *
 * <p><strong>The size is declared, not observed.</strong> It comes from the anchored
 * child's own measurement, never from the placement the anchor was handed. That
 * distinction is the whole point: an 8×8 marker inside a table cell is placed in a
 * 16pt-wide cell, and a payload built from {@code placement.width()} would report the
 * cell. A caller anchoring a marker drawn as five SVG fragments still gets one anchor with
 * one box, which is what makes the anchor a <em>logical owner</em> rather than a
 * particular draw fragment.</p>
 *
 * @param id     the anchor's identity
 * @param width  the anchored child's own measured width
 * @param height the anchored child's own measured height
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
