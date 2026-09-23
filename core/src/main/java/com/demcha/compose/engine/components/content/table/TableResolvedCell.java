package com.demcha.compose.engine.components.content.table;

import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fully materialized cell description consumed by the table row renderer.
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
 * @param lineHeight the height of one line of the cell's text, as the layout measured it
 *                   when it sized the row; {@code NaN} when whoever built the cell did not
 *                   measure it, in which case a renderer measures it itself
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
        Set<Side> borderSides,
        double lineHeight
) {
    public TableResolvedCell {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(fillInsets, "fillInsets");
        Objects.requireNonNull(borderSides, "borderSides");
        if (!Double.isNaN(lineHeight) && (Double.isInfinite(lineHeight) || lineHeight < 0)) {
            throw new IllegalArgumentException(
                    "lineHeight must be NaN (not measured) or finite and >= 0: " + lineHeight);
        }
        lines = List.copyOf(lines);
        borderSides = Set.copyOf(borderSides);
    }

    /**
     * The constructor this record had before it carried a line height, kept so a cell built
     * against it still links. It states nothing about the text's line height, and a renderer
     * reading the cell measures it itself — which is what every renderer did before.
     *
     * @param name        diagnostic cell name
     * @param x           cell left edge, relative to the row fragment's left edge
     * @param width       cell outer width
     * @param height      cell outer height
     * @param yOffset     PDF-y offset of the cell bottom relative to the row fragment's bottom
     * @param lines       text lines rendered inside the cell
     * @param style       resolved cell style
     * @param fillInsets  cell padding for the fill rectangle
     * @param borderSides cell border sides to draw
     */
    public TableResolvedCell(
            String name,
            double x,
            double width,
            double height,
            double yOffset,
            List<String> lines,
            TableCellLayoutStyle style,
            Padding fillInsets,
            Set<Side> borderSides) {
        this(name, x, width, height, yOffset, lines, style, fillInsets, borderSides, Double.NaN);
    }

    /**
     * Whether the layout stated the height of one line of this cell's text.
     *
     * @return {@code true} when {@link #lineHeight()} is a measurement rather than {@code NaN}
     */
    public boolean hasMeasuredLineHeight() {
        return !Double.isNaN(lineHeight);
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
