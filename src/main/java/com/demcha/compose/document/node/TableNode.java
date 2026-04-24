package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.engine.components.components_builders.TableCellSpec;
import com.demcha.compose.engine.components.components_builders.TableCellStyle;
import com.demcha.compose.engine.components.components_builders.TableColumnSpec;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.document.node.DocumentNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Semantic table node with row-atomic pagination.
 */
public record TableNode(
        String name,
        List<TableColumnSpec> columns,
        List<List<TableCellSpec>> rows,
        TableCellStyle defaultCellStyle,
        Map<Integer, TableCellStyle> rowStyles,
        Map<Integer, TableCellStyle> columnStyles,
        Double width,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        Padding padding,
        Margin margin
) implements DocumentNode {
    /**
     * Backward-compatible constructor that keeps the original advanced V2
     * authoring shape for callers that do not need row-level or column-level
     * style overrides.
     *
     * @param name table node name used in snapshots and layout graph paths
     * @param columns negotiated table columns
     * @param rows table rows in source order
     * @param defaultCellStyle default cell style applied to every cell
     * @param width optional explicit table width
     * @param padding outer table padding
     * @param margin outer table margin
     */
    public TableNode(String name,
                     List<TableColumnSpec> columns,
                     List<List<TableCellSpec>> rows,
                     TableCellStyle defaultCellStyle,
                     Double width,
                     Padding padding,
                     Margin margin) {
        this(name, columns, rows, defaultCellStyle, Map.of(), Map.of(), width, null, null, padding, margin);
    }

    /**
     * Backward-compatible constructor that retains row and column style
     * overrides without content-scoped PDF metadata.
     *
     * @param name table node name used in snapshots and layout graph paths
     * @param columns negotiated table columns
     * @param rows table rows in source order
     * @param defaultCellStyle default cell style applied to every cell
     * @param rowStyles row-specific style overrides
     * @param columnStyles column-specific style overrides
     * @param width optional explicit table width
     * @param padding outer table padding
     * @param margin outer table margin
     */
    public TableNode(String name,
                     List<TableColumnSpec> columns,
                     List<List<TableCellSpec>> rows,
                     TableCellStyle defaultCellStyle,
                     Map<Integer, TableCellStyle> rowStyles,
                     Map<Integer, TableCellStyle> columnStyles,
                     Double width,
                     Padding padding,
                     Margin margin) {
        this(name, columns, rows, defaultCellStyle, rowStyles, columnStyles, width, null, null, padding, margin);
    }

    public TableNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(rows, "rows");
        columns = List.copyOf(columns);
        rows = normalizeRows(rows);
        defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : defaultCellStyle;
        rowStyles = normalizeStyleMap(rowStyles, "row");
        columnStyles = normalizeStyleMap(columnStyles, "column");
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

    private static Map<Integer, TableCellStyle> normalizeStyleMap(Map<Integer, TableCellStyle> styles, String label) {
        if (styles == null || styles.isEmpty()) {
            return Map.of();
        }

        for (Map.Entry<Integer, TableCellStyle> entry : styles.entrySet()) {
            Objects.requireNonNull(entry.getKey(), label + " index");
            if (entry.getKey() < 0) {
                throw new IllegalArgumentException(label + " style index cannot be negative: " + entry.getKey());
            }
            Objects.requireNonNull(entry.getValue(), label + " style");
        }
        return Map.copyOf(styles);
    }
}


