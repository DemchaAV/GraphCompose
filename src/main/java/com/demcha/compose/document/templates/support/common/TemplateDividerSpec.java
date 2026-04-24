package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.engine.components.style.Margin;

import java.awt.Color;

/**
 * Immutable divider instruction used by shared template scene composers.
 *
 * @param name semantic divider name used in snapshots
 * @param width divider width in points
 * @param thickness divider stroke thickness in points
 * @param color divider color
 * @param margin outer divider margin
 */
public record TemplateDividerSpec(
        String name,
        double width,
        double thickness,
        Color color,
        Margin margin
) {
    /**
     * Normalizes divider name and margin defaults.
     */
    public TemplateDividerSpec {
        name = name == null ? "" : name;
        margin = margin == null ? Margin.zero() : margin;
    }
}
