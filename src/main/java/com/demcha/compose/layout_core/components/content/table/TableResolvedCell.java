package com.demcha.compose.layout_core.components.content.table;

import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fully materialized cell description rendered by {@link com.demcha.compose.layout_core.components.renderable.TableRow}.
 */
public record TableResolvedCell(
        String name,
        double x,
        double width,
        double height,
        List<String> lines,
        TableCellStyle style,
        Padding fillInsets,
        Set<Side> borderSides
) {
    public TableResolvedCell {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(fillInsets, "fillInsets");
        Objects.requireNonNull(borderSides, "borderSides");
        lines = List.copyOf(lines);
        borderSides = Set.copyOf(borderSides);
    }
}
