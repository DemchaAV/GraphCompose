package com.demcha.compose.document.node;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Semantic table node with row-atomic pagination.
 *
 * @param name table node name used in snapshots and layout graph paths
 * @param columns negotiated table columns
 * @param rows table rows in source order
 * @param defaultCellStyle default style applied to cells without overrides
 * @param rowStyles row-specific style overrides
 * @param columnStyles column-specific style overrides
 * @param width optional explicit table width
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding outer table padding
 * @param margin outer table margin
 * @param repeatedHeaderRowCount number of leading rows to repeat at the
 *                               top of every continuation page when the
 *                               table is split across pages; {@code 0}
 *                               disables the feature
 */
public record TableNode(
        String name,
        List<DocumentTableColumn> columns,
        List<List<DocumentTableCell>> rows,
        DocumentTableStyle defaultCellStyle,
        Map<Integer, DocumentTableStyle> rowStyles,
        Map<Integer, DocumentTableStyle> columnStyles,
        Double width,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        int repeatedHeaderRowCount
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
                     List<DocumentTableColumn> columns,
                     List<List<DocumentTableCell>> rows,
                     DocumentTableStyle defaultCellStyle,
                     Double width,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, columns, rows, defaultCellStyle, Map.of(), Map.of(), width, null, null, padding, margin, 0);
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
                     List<DocumentTableColumn> columns,
                     List<List<DocumentTableCell>> rows,
                     DocumentTableStyle defaultCellStyle,
                     Map<Integer, DocumentTableStyle> rowStyles,
                     Map<Integer, DocumentTableStyle> columnStyles,
                     Double width,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, columns, rows, defaultCellStyle, rowStyles, columnStyles, width, null, null, padding, margin, 0);
    }

    /**
     * Backward-compatible 11-arg constructor (pre-Phase D.3) that defaults
     * {@code repeatedHeaderRowCount} to {@code 0}.
     */
    public TableNode(String name,
                     List<DocumentTableColumn> columns,
                     List<List<DocumentTableCell>> rows,
                     DocumentTableStyle defaultCellStyle,
                     Map<Integer, DocumentTableStyle> rowStyles,
                     Map<Integer, DocumentTableStyle> columnStyles,
                     Double width,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, columns, rows, defaultCellStyle, rowStyles, columnStyles, width,
                linkOptions, bookmarkOptions, padding, margin, 0);
    }

    /**
     * Normalizes table rows, styles, spacing, and validates explicit width
     * and repeated-header row count.
     */
    public TableNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(rows, "rows");
        columns = List.copyOf(columns);
        rows = normalizeRows(rows);
        defaultCellStyle = defaultCellStyle == null ? DocumentTableStyle.empty() : defaultCellStyle;
        rowStyles = normalizeStyleMap(rowStyles, "row");
        columnStyles = normalizeStyleMap(columnStyles, "column");
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        if (width != null && (width <= 0 || Double.isNaN(width) || Double.isInfinite(width))) {
            throw new IllegalArgumentException("width must be finite and positive when set: " + width);
        }
        if (repeatedHeaderRowCount < 0) {
            throw new IllegalArgumentException(
                    "repeatedHeaderRowCount cannot be negative: " + repeatedHeaderRowCount);
        }
        if (repeatedHeaderRowCount > rows.size()) {
            throw new IllegalArgumentException(
                    "repeatedHeaderRowCount " + repeatedHeaderRowCount
                            + " exceeds row count " + rows.size() + ".");
        }
    }

    private static List<List<DocumentTableCell>> normalizeRows(List<List<DocumentTableCell>> rows) {
        List<List<DocumentTableCell>> normalized = new ArrayList<>(rows.size());
        for (List<DocumentTableCell> row : rows) {
            Objects.requireNonNull(row, "row");
            normalized.add(List.copyOf(row));
        }
        return List.copyOf(normalized);
    }

    private static Map<Integer, DocumentTableStyle> normalizeStyleMap(Map<Integer, DocumentTableStyle> styles, String label) {
        if (styles == null || styles.isEmpty()) {
            return Map.of();
        }

        for (Map.Entry<Integer, DocumentTableStyle> entry : styles.entrySet()) {
            Objects.requireNonNull(entry.getKey(), label + " index");
            if (entry.getKey() < 0) {
                throw new IllegalArgumentException(label + " style index cannot be negative: " + entry.getKey());
            }
            Objects.requireNonNull(entry.getValue(), label + " style");
        }
        return Map.copyOf(styles);
    }
}


