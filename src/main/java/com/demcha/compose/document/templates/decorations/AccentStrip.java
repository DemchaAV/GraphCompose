package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;

import java.util.Objects;

/**
 * Templates v2 accent-strip decoration.
 *
 * <p>Produces a flat coloured {@link ShapeNode} suitable for use as a
 * left/right vertical accent (typically inside a row alongside content)
 * or a top/bottom horizontal accent (typically below a header or above
 * a footer). The strip is a plain filled rectangle — no rounded corners,
 * no stroke — so a preset can keep its visual chrome consistent across
 * sections.</p>
 *
 * <p>This class is a stateless utility — all factories are static and
 * the produced {@link ShapeNode} is immutable.</p>
 */
public final class AccentStrip {

    private AccentStrip() {
        // utility class — not instantiable
    }

    /**
     * Returns a vertical strip suited for placement on the left edge
     * of a content area (typical accent-left pattern in editorial
     * layouts).
     *
     * @param color  non-null fill colour
     * @param width  positive finite strip width in points
     * @param height positive finite strip height in points (usually
     *               the height of the row it accents)
     * @return shape node representing the strip
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if a dimension is non-positive
     */
    public static DocumentNode left(DocumentColor color, double width, double height) {
        return strip("AccentStrip.left", color, width, height);
    }

    /**
     * Returns a vertical strip suited for placement on the right edge
     * of a content area.
     *
     * @param color  non-null fill colour
     * @param width  positive finite strip width in points
     * @param height positive finite strip height in points
     * @return shape node representing the strip
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if a dimension is non-positive
     */
    public static DocumentNode right(DocumentColor color, double width, double height) {
        return strip("AccentStrip.right", color, width, height);
    }

    /**
     * Returns a horizontal strip suited for placement above content
     * (typical above-header accent pattern).
     *
     * @param color  non-null fill colour
     * @param width  positive finite strip width in points
     * @param height positive finite strip height in points
     * @return shape node representing the strip
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if a dimension is non-positive
     */
    public static DocumentNode top(DocumentColor color, double width, double height) {
        return strip("AccentStrip.top", color, width, height);
    }

    /**
     * Returns a horizontal strip suited for placement below content
     * (typical below-header accent pattern).
     *
     * @param color  non-null fill colour
     * @param width  positive finite strip width in points
     * @param height positive finite strip height in points
     * @return shape node representing the strip
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if a dimension is non-positive
     */
    public static DocumentNode bottom(DocumentColor color, double width, double height) {
        return strip("AccentStrip.bottom", color, width, height);
    }

    /**
     * Returns a freely-sized accent rectangle.
     *
     * @param color  non-null fill colour
     * @param width  positive finite rectangle width in points
     * @param height positive finite rectangle height in points
     * @return shape node representing the rectangle
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if a dimension is non-positive
     */
    public static DocumentNode rect(DocumentColor color, double width, double height) {
        return strip("AccentStrip.rect", color, width, height);
    }

    private static DocumentNode strip(String name, DocumentColor color, double width, double height) {
        Objects.requireNonNull(color, "color");
        return new ShapeNode(
                name,
                width,
                height,
                /* fillColor    */ color,
                /* stroke       */ null,
                /* cornerRadius */ DocumentCornerRadius.ZERO,
                /* link         */ null,
                /* bookmark     */ null,
                /* padding      */ DocumentInsets.zero(),
                /* margin       */ DocumentInsets.zero(),
                /* transform    */ DocumentTransform.NONE);
    }
}
