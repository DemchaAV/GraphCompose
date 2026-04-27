package com.demcha.compose.engine.components.content.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Internal table cell content model with optional multiline text, style
 * override and column span.
 *
 * <p>This class belongs to the V2 runtime table model. Public authoring should
 * prefer {@code com.demcha.compose.document.table.DocumentTableCell}.</p>
 *
 * @author Artem Demchyshyn
 */
public record TableCellContent(List<String> lines, TableCellLayoutStyle styleOverride, int colSpan) {

    public TableCellContent {
        if (colSpan < 1) {
            throw new IllegalArgumentException("colSpan must be >= 1: " + colSpan);
        }
        lines = normalizeLines(lines);
    }

    /**
     * Backward-compatible constructor for callers that do not need column spans.
     *
     * @param lines cell lines
     * @param styleOverride optional style override
     */
    public TableCellContent(List<String> lines, TableCellLayoutStyle styleOverride) {
        this(lines, styleOverride, 1);
    }

    /**
     * Creates a single-line text cell.
     *
     * @param text cell text, nullable
     * @return normalized cell content
     */
    public static TableCellContent text(String text) {
        return new TableCellContent(List.of(text == null ? "" : text), null, 1);
    }

    /**
     * Creates a multiline cell from varargs.
     *
     * @param lines cell lines
     * @return normalized cell content
     */
    public static TableCellContent lines(String... lines) {
        if (lines == null || lines.length == 0) {
            return new TableCellContent(List.of(""), null, 1);
        }
        return new TableCellContent(Arrays.asList(lines), null, 1);
    }

    /**
     * Creates a multiline cell.
     *
     * @param lines cell lines
     * @return normalized cell content
     */
    public static TableCellContent of(List<String> lines) {
        return new TableCellContent(lines, null, 1);
    }

    /**
     * Creates a multiline cell with a style override.
     *
     * @param lines cell lines
     * @param styleOverride optional cell style override
     * @return normalized cell content
     */
    public static TableCellContent of(List<String> lines, TableCellLayoutStyle styleOverride) {
        return new TableCellContent(lines, styleOverride, 1);
    }

    /**
     * Returns a copy with the given style override.
     *
     * @param override style override
     * @return styled cell content
     */
    public TableCellContent withStyle(TableCellLayoutStyle override) {
        return new TableCellContent(lines, override, colSpan);
    }

    /**
     * Returns a copy that spans the given number of adjacent columns.
     *
     * <p>The sum of {@code colSpan} values across a row must equal the table
     * column count.</p>
     *
     * @param span column span (must be {@code >= 1})
     * @return spanned cell content
     */
    public TableCellContent withColSpan(int span) {
        return new TableCellContent(lines, styleOverride, span);
    }

    private static List<String> normalizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of("");
        }

        List<String> normalized = new ArrayList<>(lines.size());
        for (String line : lines) {
            normalized.add(Objects.requireNonNullElse(line, ""));
        }
        return List.copyOf(normalized);
    }
}
