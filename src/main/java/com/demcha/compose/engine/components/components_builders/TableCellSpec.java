package com.demcha.compose.engine.components.components_builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builder-facing table cell payload with optional multiline content and style override.
 */
public record TableCellSpec(List<String> lines, TableCellStyle styleOverride) {

    public TableCellSpec {
        lines = normalizeLines(lines);
    }

    public static TableCellSpec text(String text) {
        return new TableCellSpec(List.of(text == null ? "" : text), null);
    }

    public static TableCellSpec lines(String... lines) {
        if (lines == null || lines.length == 0) {
            return new TableCellSpec(List.of(""), null);
        }
        return new TableCellSpec(Arrays.asList(lines), null);
    }

    public static TableCellSpec of(List<String> lines) {
        return new TableCellSpec(lines, null);
    }

    public static TableCellSpec of(List<String> lines, TableCellStyle styleOverride) {
        return new TableCellSpec(lines, styleOverride);
    }

    public TableCellSpec withStyle(TableCellStyle override) {
        return new TableCellSpec(lines, override);
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
