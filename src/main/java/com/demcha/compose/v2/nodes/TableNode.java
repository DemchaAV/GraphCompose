package com.demcha.compose.v2.nodes;

import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.v2.DocumentNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Semantic table node with row-atomic pagination.
 */
public record TableNode(
        String name,
        List<TableColumnSpec> columns,
        List<List<TableCellSpec>> rows,
        TableCellStyle defaultCellStyle,
        Double width,
        Padding padding,
        Margin margin
) implements DocumentNode {
    public TableNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(rows, "rows");
        columns = List.copyOf(columns);
        rows = normalizeRows(rows);
        defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : defaultCellStyle;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (width != null && (width <= 0 || Double.isNaN(width) || Double.isInfinite(width))) {
            throw new IllegalArgumentException("width must be finite and positive when set: " + width);
        }
    }

    private static List<List<TableCellSpec>> normalizeRows(List<List<TableCellSpec>> rows) {
        List<List<TableCellSpec>> normalized = new ArrayList<>(rows.size());
        for (List<TableCellSpec> row : rows) {
            Objects.requireNonNull(row, "row");
            normalized.add(List.copyOf(row));
        }
        return List.copyOf(normalized);
    }
}
