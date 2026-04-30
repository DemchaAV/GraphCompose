package com.demcha.compose.engine.components.content.table;

import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fully materialized cell description rendered by {@link com.demcha.compose.engine.components.renderable.TableRow}.
 *
 * <p>{@code yOffset} shifts the cell's bottom edge relative to the row
 * fragment's bottom (in PDF coordinates where y grows up). Single-row
 * cells use {@code 0}. Multi-row spanning cells use a NEGATIVE offset
 * equal to the cumulative height of the rows below the spanning cell's
 * starting row, so the cell's rectangle extends visually downward
 * through the rows it merges instead of upward beyond the row above.</p>
 *
 * @param name diagnostic cell name (semantic-path-style)
 * @param x cell left edge, relative to the row fragment's left edge
 * @param width cell outer width
 * @param height cell outer height (sum of spanned row heights for
 *               multi-row cells)
 * @param yOffset PDF-y offset of the cell bottom relative to the row
 *                fragment's bottom; non-positive (zero for single-row,
 *                negative for spanning cells)
 * @param lines text lines rendered inside the cell
 * @param style resolved cell style
 * @param fillInsets cell padding for the fill rectangle
 * @param borderSides cell border sides to draw
 */
public record TableResolvedCell(
        String name,
        double x,
        double width,
        double height,
        double yOffset,
        List<String> lines,
        TableCellLayoutStyle style,
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

    /**
     * Backward-compatible 8-arg constructor for callers that don't span
     * rows. Defaults {@code yOffset} to {@code 0}.
     */
    public TableResolvedCell(
            String name,
            double x,
            double width,
            double height,
            List<String> lines,
            TableCellLayoutStyle style,
            Padding fillInsets,
            Set<Side> borderSides) {
        this(name, x, width, height, 0.0, lines, style, fillInsets, borderSides);
    }
}
