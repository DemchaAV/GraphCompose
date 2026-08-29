package com.demcha.compose.document.layout;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;

/**
 * Page geometry used by the v2 compiler and backends.
 *
 * @param width       full page width
 * @param height      full page height
 * @param innerWidth  page width after horizontal margins
 * @param innerHeight page height after vertical margins
 * @param margin      page margin
 */
public record LayoutCanvas(double width, double height, double innerWidth, double innerHeight, Margin margin) {

    /**
     * The left margin as a primitive.
     *
     * <p>Exists so a canonical caller can place something against the content
     * area's left edge without calling a method on the engine's {@code Margin}:
     * the canonical surface reaches the engine through this package or not at
     * all, and {@code ModuleBoundaryArchTest} enforces it.</p>
     *
     * @return the left margin in points
     * @since 2.2.3
     */
    public double marginLeft() {
        return margin == null ? 0.0 : margin.left();
    }

    /**
     * Creates canvas geometry from backend-neutral page dimensions and margin.
     *
     * @param width  physical page width
     * @param height physical page height
     * @param margin page margin; null becomes zero margin
     * @return normalized canvas geometry
     */
    public static LayoutCanvas from(double width, double height, Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        return new LayoutCanvas(
                width,
                height,
                Math.max(0.0, width - safeMargin.horizontal()),
                Math.max(0.0, height - safeMargin.vertical()),
                safeMargin);
    }

    /**
     * Creates canvas geometry from backend-neutral page dimensions and canonical
     * page insets, converting them to an engine margin at this layout boundary so
     * the caller never has to. Equivalent to {@link #from(double, double, Margin)}
     * with {@code LayoutInsets.toMargin(margin)}.
     *
     * @param width  physical page width
     * @param height physical page height
     * @param margin canonical page insets; null becomes zero margin
     * @return normalized canvas geometry
     * @since 2.0.0
     */
    public static LayoutCanvas from(double width, double height, DocumentInsets margin) {
        return from(width, height, LayoutInsets.toMargin(margin));
    }
}


