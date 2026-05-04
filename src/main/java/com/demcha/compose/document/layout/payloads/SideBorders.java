package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.engine.components.content.shape.Stroke;

/**
 * Resolved engine-level per-side border strokes attached to a shape payload.
 *
 * <p>Each side stroke is independent. {@code null} disables the corresponding
 * side. The renderer treats this record as a request to draw the four lines
 * separately rather than rely on a single uniform rectangle stroke.</p>
 *
 * @param top top side stroke, or {@code null} for no border on that side
 * @param right right side stroke, or {@code null} for no border on that side
 * @param bottom bottom side stroke, or {@code null} for no border on that side
 * @param left left side stroke, or {@code null} for no border on that side
 */
public record SideBorders(Stroke top, Stroke right, Stroke bottom, Stroke left) {
    /**
     * Indicates whether at least one side carries a stroke.
     *
     * @return {@code true} when any side is set
     */
    public boolean hasAny() {
        return top != null || right != null || bottom != null || left != null;
    }
}
