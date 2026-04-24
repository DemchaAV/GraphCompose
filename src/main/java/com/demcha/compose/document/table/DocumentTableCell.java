package com.demcha.compose.document.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Public table cell payload for the canonical DSL.
 *
 * <p>Use plain string rows for the common case and this type only when a cell
 * needs multiple lines or a per-cell style override.</p>
 *
 * @param lines text lines rendered inside the cell
 * @param style optional style override
 * @author Artem Demchyshyn
 */
public record DocumentTableCell(List<String> lines, DocumentTableStyle style) {

    /**
     * Creates a normalized table cell payload.
     */
    public DocumentTableCell {
        lines = normalizeLines(lines);
    }

    /**
     * Creates a one-line text cell.
     *
     * @param text cell text
     * @return table cell
     */
    public static DocumentTableCell text(String text) {
        return new DocumentTableCell(List.of(text == null ? "" : text), null);
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
        return new DocumentTableCell(Arrays.asList(lines), null);
    }

    /**
     * Creates a copy with a style override.
     *
     * @param style style override
     * @return styled table cell
     */
    public DocumentTableCell withStyle(DocumentTableStyle style) {
        return new DocumentTableCell(lines, style);
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
