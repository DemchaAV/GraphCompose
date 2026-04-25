package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

/**
 * Page geometry used by the v2 compiler and backends.
 *
 * @param width full page width
 * @param height full page height
 * @param innerWidth page width after horizontal margins
 * @param innerHeight page height after vertical margins
 * @param margin page margin
 */
public record LayoutCanvas(double width, double height, double innerWidth, double innerHeight, Margin margin) {

    /**
     * Creates canvas geometry from backend-neutral page dimensions and margin.
     *
     * @param width physical page width
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
}


