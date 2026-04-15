package com.demcha.compose.document.templates.support;

import com.demcha.compose.layout_core.components.style.Margin;

import java.awt.Color;

/**
 * Immutable divider instruction used by shared template scene composers.
 */
public record TemplateDividerSpec(
        String name,
        double width,
        double thickness,
        Color color,
        Margin margin
) {
    public TemplateDividerSpec {
        name = name == null ? "" : name;
        margin = margin == null ? Margin.zero() : margin;
    }
}
