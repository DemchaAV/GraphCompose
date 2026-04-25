package com.demcha.compose.document.snapshot;

/**
 * Snapshot-friendly inset model used for margins and padding.
 *
 * @param top top inset
 * @param right right inset
 * @param bottom bottom inset
 * @param left left inset
 */
public record LayoutInsetsSnapshot(double top, double right, double bottom, double left) {
}
