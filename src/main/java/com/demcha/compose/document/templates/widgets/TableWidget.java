package com.demcha.compose.document.templates.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared table widget for template presets.
 *
 * <p>The low-level DSL already knows how to lay out tables; this
 * widget captures the common template-facing knobs so presets can
 * swap border colour, cell fill, zebra rows, padding, typography,
 * and column count without rewriting table plumbing.</p>
 *
 * <p>Use {@link #fixed} when you already have rows. Use
 * {@link #grid} when you have a flat list of items that should flow
 * left-to-right into a fixed number of columns.</p>
 */
public final class TableWidget {

    private TableWidget() {
    }

    /**
     * Renders a fixed-column table from pre-grouped rows.
     *
     * @param host  section receiving the table
     * @param rows  row data; short rows are padded with blank cells
     * @param width available table width in points
     * @param style visual table options
     */
    public static void fixed(SectionBuilder host,
                             List<List<String>> rows,
                             double width,
                             Style style) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(rows, "rows");
        Style safeStyle = style == null ? Style.builder().build() : style;
        List<List<String>> normalized = normalizeRows(rows, safeStyle.columns());
        if (normalized.isEmpty()) {
            return;
        }

        host.addTable(table -> {
            table.name(safeStyle.name())
                    .columns(fixedColumns(width, safeStyle))
                    .defaultCellStyle(cellStyle(safeStyle));
            if (safeStyle.zebraFillColor() != null) {
                table.zebra(fillStyle(safeStyle.cellFillColor()),
                        fillStyle(safeStyle.zebraFillColor()));
            }
            for (List<String> row : normalized) {
                table.row(row.toArray(String[]::new));
            }
        });
    }

    /**
     * Renders a flat list as a fixed-column grid.
     *
     * @param host  section receiving the table
     * @param cells flat cell values, filled row-major
     * @param width available table width in points
     * @param style visual table options
     */
    public static void grid(SectionBuilder host,
                            List<String> cells,
                            double width,
                            Style style) {
        Objects.requireNonNull(cells, "cells");
        Style safeStyle = style == null ? Style.builder().build() : style;
        List<String> cleaned = new ArrayList<>();
        for (String cell : cells) {
            if (cell != null && !cell.isBlank()) {
                cleaned.add(cell.trim());
            }
        }
        if (cleaned.isEmpty()) {
            return;
        }

        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i += safeStyle.columns()) {
            List<String> row = new ArrayList<>(safeStyle.columns());
            for (int c = 0; c < safeStyle.columns(); c++) {
                int index = i + c;
                row.add(index < cleaned.size() ? cleaned.get(index) : "");
            }
            rows.add(row);
        }
        fixed(host, rows, width, safeStyle);
    }

    private static DocumentTableColumn[] fixedColumns(double width, Style style) {
        double columnWidth = (width - style.widthAdjustment()) / style.columns();
        DocumentTableColumn[] columns = new DocumentTableColumn[style.columns()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = DocumentTableColumn.fixed(columnWidth);
        }
        return columns;
    }

    private static DocumentTableStyle cellStyle(Style style) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .padding(style.cellPadding());
        if (style.cellFillColor() != null) {
            builder.fillColor(style.cellFillColor());
        }
        if (style.cellStroke() != null) {
            builder.stroke(style.cellStroke());
        }
        if (style.textStyle() != null) {
            builder.textStyle(style.textStyle());
        }
        if (style.lineSpacing() != null) {
            builder.lineSpacing(style.lineSpacing());
        }
        return builder.build();
    }

    private static DocumentTableStyle fillStyle(DocumentColor color) {
        return color == null
                ? null
                : DocumentTableStyle.builder().fillColor(color).build();
    }

    private static List<List<String>> normalizeRows(List<List<String>> rows,
                                                    int columns) {
        List<List<String>> out = new ArrayList<>();
        for (List<String> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (row.size() > columns) {
                throw new IllegalArgumentException(
                        "Row has " + row.size() + " cells, but table has "
                                + columns + " columns");
            }
            List<String> normalized = new ArrayList<>(columns);
            for (int i = 0; i < columns; i++) {
                String value = i < row.size() ? row.get(i) : "";
                normalized.add(value == null ? "" : value);
            }
            out.add(normalized);
        }
        return List.copyOf(out);
    }

    /**
     * Visual options for the shared table widget.
     *
     * @param name            semantic table node name
     * @param columns         fixed column count
     * @param cellPadding     padding inside every cell
     * @param cellStroke      optional border stroke; null means no
     *                        explicit border override
     * @param cellFillColor   optional default fill for every cell
     * @param zebraFillColor  optional fill for alternating rows
     * @param textStyle       optional text style override
     * @param lineSpacing     optional line spacing override
     * @param widthAdjustment value subtracted before splitting width
     *                        into fixed columns, useful when borders
     *                        would otherwise nudge the table over
     */
    public record Style(String name,
                        int columns,
                        DocumentInsets cellPadding,
                        DocumentStroke cellStroke,
                        DocumentColor cellFillColor,
                        DocumentColor zebraFillColor,
                        DocumentTextStyle textStyle,
                        Double lineSpacing,
                        double widthAdjustment) {

        /**
         * Normalises a blank name to {@code "TemplateTable"} and a null
         * padding to zero insets, and validates the column count,
         * line spacing, and width adjustment.
         */
        public Style {
            name = (name == null || name.isBlank()) ? "TemplateTable" : name;
            if (columns < 1) {
                throw new IllegalArgumentException(
                        "columns must be >= 1: " + columns);
            }
            cellPadding = cellPadding == null
                    ? DocumentInsets.zero()
                    : cellPadding;
            if (lineSpacing != null
                    && (lineSpacing < 0
                    || Double.isNaN(lineSpacing)
                    || Double.isInfinite(lineSpacing))) {
                throw new IllegalArgumentException(
                        "lineSpacing must be finite and non-negative");
            }
            if (widthAdjustment < 0
                    || Double.isNaN(widthAdjustment)
                    || Double.isInfinite(widthAdjustment)) {
                throw new IllegalArgumentException(
                        "widthAdjustment must be finite and non-negative");
            }
        }

        /**
         * Creates a new style builder.
         *
         * @return mutable builder seeded with conservative defaults
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Fluent builder for {@link Style}.
         */
        public static final class Builder {
            private String name = "TemplateTable";
            private int columns = 2;
            private DocumentInsets cellPadding = DocumentInsets.zero();
            private DocumentStroke cellStroke;
            private DocumentColor cellFillColor;
            private DocumentColor zebraFillColor;
            private DocumentTextStyle textStyle;
            private Double lineSpacing;
            private double widthAdjustment = 0.0;

            private Builder() {
            }

            /**
             * Sets the semantic table node name.
             *
             * @param value semantic table node name
             * @return this builder for chaining
             */
            public Builder name(String value) {
                this.name = value;
                return this;
            }

            /**
             * Sets the fixed column count.
             *
             * @param value fixed column count
             * @return this builder for chaining
             */
            public Builder columns(int value) {
                this.columns = value;
                return this;
            }

            /**
             * Sets the padding inside every cell.
             *
             * @param value padding inside every cell
             * @return this builder for chaining
             */
            public Builder cellPadding(DocumentInsets value) {
                this.cellPadding = value;
                return this;
            }

            /**
             * Sets the cell border stroke from a colour and width.
             *
             * @param color border colour
             * @param width border width in points
             * @return this builder for chaining
             */
            public Builder border(DocumentColor color, double width) {
                this.cellStroke = DocumentStroke.of(color, width);
                return this;
            }

            /**
             * Sets the optional cell border stroke.
             *
             * @param value optional border stroke
             * @return this builder for chaining
             */
            public Builder cellStroke(DocumentStroke value) {
                this.cellStroke = value;
                return this;
            }

            /**
             * Sets the optional default fill for every cell.
             *
             * @param value optional default fill for every cell
             * @return this builder for chaining
             */
            public Builder cellFillColor(DocumentColor value) {
                this.cellFillColor = value;
                return this;
            }

            /**
             * Sets the optional fill for alternating rows.
             *
             * @param value optional fill for alternating rows
             * @return this builder for chaining
             */
            public Builder zebraFillColor(DocumentColor value) {
                this.zebraFillColor = value;
                return this;
            }

            /**
             * Sets the optional text style override.
             *
             * @param value optional text style override
             * @return this builder for chaining
             */
            public Builder textStyle(DocumentTextStyle value) {
                this.textStyle = value;
                return this;
            }

            /**
             * Sets the optional line spacing override.
             *
             * @param value optional line spacing override
             * @return this builder for chaining
             */
            public Builder lineSpacing(Double value) {
                this.lineSpacing = value;
                return this;
            }

            /**
             * Sets the value subtracted before splitting width into columns.
             *
             * @param value value subtracted before splitting width into columns
             * @return this builder for chaining
             */
            public Builder widthAdjustment(double value) {
                this.widthAdjustment = value;
                return this;
            }

            /**
             * Builds the configured {@link Style}.
             *
             * @return a new {@code Style} carrying the configured table options
             */
            public Style build() {
                return new Style(name, columns, cellPadding, cellStroke,
                        cellFillColor, zebraFillColor, textStyle,
                        lineSpacing, widthAdjustment);
            }
        }
    }
}
