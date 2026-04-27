package com.demcha.compose.document.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Public table cell payload for the canonical DSL.
 *
 * <p>Use plain string rows for the common case and this type only when a cell
 * needs multiple lines, a per-cell style override, or a {@code colSpan}.</p>
 *
 * @param lines text lines rendered inside the cell
 * @param style optional style override
 * @param colSpan number of columns the cell occupies (must be {@code >= 1})
 * @author Artem Demchyshyn
 */
public record DocumentTableCell(List<String> lines, DocumentTableStyle style, int colSpan) {

    /**
     * Creates a normalized table cell payload.
     */
    public DocumentTableCell {
        if (colSpan < 1) {
            throw new IllegalArgumentException("colSpan must be >= 1: " + colSpan);
        }
        lines = normalizeLines(lines);
    }

    /**
     * Backward-compatible constructor for callers that do not need column spans.
     *
     * @param lines text lines rendered inside the cell
     * @param style optional style override
     */
    public DocumentTableCell(List<String> lines, DocumentTableStyle style) {
        this(lines, style, 1);
    }

    /**
     * Creates a one-line text cell.
     *
     * @param text cell text
     * @return table cell
     */
    public static DocumentTableCell text(String text) {
        return new DocumentTableCell(List.of(text == null ? "" : text), null, 1);
    }

    /**
     * Creates a multi-line text cell.
     *
     * @param lines text lines
     * @return table cell
     */
    public static DocumentTableCell lines(String... lines) {
        if (lines == null || lines.length == 0) {
            return text("");
        }
        return new DocumentTableCell(Arrays.asList(lines), null, 1);
    }

    /**
     * Creates a copy with a style override.
     *
     * @param style style override
     * @return styled table cell
     */
    public DocumentTableCell withStyle(DocumentTableStyle style) {
        return new DocumentTableCell(lines, style, colSpan);
    }

    /**
     * Creates a copy that spans the given number of adjacent columns.
     *
     * <p>The sum of {@code colSpan} values across a row must equal the table
     * column count. Use {@code 1} for a regular single-column cell.</p>
     *
     * @param span number of columns the cell occupies (must be {@code >= 1})
     * @return spanned table cell
     */
    public DocumentTableCell colSpan(int span) {
        return new DocumentTableCell(lines, style, span);
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
