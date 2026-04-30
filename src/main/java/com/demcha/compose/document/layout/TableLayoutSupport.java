package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableCell;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableColumns;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableStyle;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableStyles;

/**
 * Package-private support helpers for {@code TableNode} layout.
 *
 * <p>Extracted from {@code BuiltInNodeDefinitions} to keep the registry file
 * focused on definition wiring instead of cell-resolution arithmetic. Only
 * {@link TableLayoutSupport} and {@code BuiltInNodeDefinitions} are expected to
 * reference these helpers.</p>
 */
final class TableLayoutSupport {
    private static final double EPS = 1e-6;

    private TableLayoutSupport() {
    }

    /**
     * Prepared layout payload attached to {@code TableNode} prepared nodes.
     */
    record PreparedTableLayout(
            ResolvedTableLayout resolvedLayout,
            boolean emitBookmark
    ) implements PreparedNodeLayout {
    }

    /**
     * Resolved table layout (column widths, row heights, resolved cells).
     */
    record ResolvedTableLayout(
            List<Double> columnWidths,
            List<Double> rowHeights,
            List<List<TableResolvedCell>> rows,
            double naturalWidth,
            double finalWidth,
            double totalHeight
    ) {
    }

    /**
     * Logical cell: a single user-authored cell after the cell-grid pre-pass
     * has resolved its starting position and colSpan/rowSpan extent. A
     * spanning cell appears once at its starting (row, column); the
     * positions it occupies in subsequent rows are tracked by the
     * occupancy grid built in {@link #buildLogicalRows(TableNode, int)} and
     * are skipped when iterating later source rows.
     */
    private record LogicalCell(int startRow, int startColumn, int colSpan, int rowSpan, TableCellContent content) {
    }

    static ResolvedTableLayout resolveTableLayout(TableNode node,
                                                  TextMeasurementSystem measurement,
                                                  double availableWidth) {
        validateRowsExist(node);
        int columnCount = resolveColumnCount(node);
        List<List<LogicalCell>> logicalRows = buildLogicalRows(node, columnCount);
        List<TableColumnLayout> normalizedSpecs = normalizeSpecs(node, columnCount);
        TableCellLayoutStyle[][] stylesGrid = buildStylesGrid(node, logicalRows, columnCount);
        double[] naturalWidths = resolveNaturalColumnWidths(node, normalizedSpecs, logicalRows, stylesGrid, columnCount, measurement);
        double naturalWidth = sum(naturalWidths);
        double[] finalWidths = resolveFinalColumnWidths(node, normalizedSpecs, naturalWidths, naturalWidth);
        double finalWidth = sum(finalWidths);

        double innerAvailableWidth = Math.max(0.0, availableWidth - node.padding().horizontal());
        if (finalWidth > innerAvailableWidth + EPS) {
            throw new IllegalStateException("Table '" + displayName(node) + "' width " + finalWidth
                    + " exceeds available width " + innerAvailableWidth + ".");
        }

        // Two-pass row height resolution. First pass: row height = max of
        // natural heights of the single-row cells starting in that row.
        // Second pass: for every spanning cell, ensure the SUM of its
        // covered row heights is at least its natural height; if short,
        // distribute the deficit equally across the spanned rows. This
        // keeps single-row content predictable while still satisfying
        // tall spanned content.
        double[] rowHeightsArr = new double[node.rows().size()];
        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.rowSpan() != 1) {
                    continue;
                }
                TableCellLayoutStyle style = stylesGrid[rowIndex][logical.startColumn()];
                rowHeightsArr[rowIndex] = Math.max(rowHeightsArr[rowIndex],
                        cellNaturalHeight(logical.content(), style, measurement));
            }
        }
        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.rowSpan() == 1) {
                    continue;
                }
                TableCellLayoutStyle style = stylesGrid[rowIndex][logical.startColumn()];
                double need = cellNaturalHeight(logical.content(), style, measurement);
                double have = sumRange(rowHeightsArr, logical.startRow(),
                        logical.startRow() + logical.rowSpan());
                if (need <= have + EPS) {
                    continue;
                }
                double share = (need - have) / logical.rowSpan();
                for (int r = logical.startRow(); r < logical.startRow() + logical.rowSpan(); r++) {
                    rowHeightsArr[r] += share;
                }
            }
        }

        List<List<TableResolvedCell>> rows = new ArrayList<>(logicalRows.size());
        List<Double> rowHeights = new ArrayList<>(logicalRows.size());

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            List<LogicalCell> rowCells = logicalRows.get(rowIndex);
            rowHeights.add(rowHeightsArr[rowIndex]);

            List<TableResolvedCell> resolved = new ArrayList<>(rowCells.size());
            for (LogicalCell logical : rowCells) {
                TableCellLayoutStyle style = stylesGrid[rowIndex][logical.startColumn()];
                double x = sumRange(finalWidths, 0, logical.startColumn());
                double width = sumRange(finalWidths, logical.startColumn(), logical.startColumn() + logical.colSpan());
                // Spanning cells take the SUM of their covered row heights,
                // so they visually merge across rows when rendered.
                double height = sumRange(rowHeightsArr, logical.startRow(),
                        logical.startRow() + logical.rowSpan());
                resolved.add(new TableResolvedCell(
                        cellName(node, rowIndex, logical.startColumn()),
                        x,
                        width,
                        height,
                        sanitizeCellLines(logical.content()),
                        style,
                        fillInsets(stylesGrid, rowIndex, logical.startColumn(), logical.colSpan()),
                        borderSides(stylesGrid, rowIndex, logical.startColumn(), logical.colSpan())));
            }
            rows.add(List.copyOf(resolved));
        }

        return new ResolvedTableLayout(
                toList(finalWidths),
                List.copyOf(rowHeights),
                List.copyOf(rows),
                naturalWidth,
                finalWidth,
                rowHeights.stream().mapToDouble(Double::doubleValue).sum());
    }

    static PreparedNode<TableNode> sliceTablePreparedNode(TableNode source,
                                                          ResolvedTableLayout layout,
                                                          int fromInclusive,
                                                          int toExclusive,
                                                          boolean keepTopInsets,
                                                          boolean keepBottomInsets) {
        List<List<TableResolvedCell>> rows = List.copyOf(layout.rows().subList(fromInclusive, toExclusive));
        List<Double> rowHeights = List.copyOf(layout.rowHeights().subList(fromInclusive, toExclusive));
        double totalHeight = rowHeights.stream().mapToDouble(Double::doubleValue).sum();
        var fixedColumns = layout.columnWidths().stream()
                .map(com.demcha.compose.document.table.DocumentTableColumn::fixed)
                .toList();

        TableNode fragmentNode = new TableNode(
                source.name(),
                fixedColumns,
                source.rows().subList(fromInclusive, toExclusive),
                source.defaultCellStyle(),
                source.rowStyles(),
                source.columnStyles(),
                layout.finalWidth(),
                source.linkOptions(),
                keepTopInsets ? source.bookmarkOptions() : null,
                new DocumentInsets(
                        keepTopInsets ? source.padding().top() : 0.0,
                        source.padding().right(),
                        keepBottomInsets ? source.padding().bottom() : 0.0,
                        source.padding().left()),
                new DocumentInsets(
                        keepTopInsets ? source.margin().top() : 0.0,
                        source.margin().right(),
                        keepBottomInsets ? source.margin().bottom() : 0.0,
                        source.margin().left()));

        ResolvedTableLayout fragmentLayout = new ResolvedTableLayout(
                layout.columnWidths(),
                rowHeights,
                rows,
                layout.naturalWidth(),
                layout.finalWidth(),
                totalHeight);

        MeasureResult measure = new MeasureResult(
                layout.finalWidth() + fragmentNode.padding().horizontal(),
                totalHeight + fragmentNode.padding().vertical());
        return PreparedNode.leaf(fragmentNode, measure, new PreparedTableLayout(fragmentLayout, keepTopInsets));
    }

    /**
     * Builds the logical-cell grid using an occupancy mask to reconcile
     * source-order author input with multi-cell colSpan / rowSpan extents.
     *
     * <p>For each source row the algorithm walks columns left-to-right.
     * When a column is already covered by a prior row's spanning cell the
     * algorithm skips it (the author should not — and must not — provide
     * a source cell there). Otherwise the algorithm consumes the next
     * source cell, validates that its colSpan/rowSpan fit inside the
     * remaining grid, and marks every {@code (r, c)} position it occupies.
     * Misalignments raise a precise diagnostic instead of producing a
     * silently corrupted grid.</p>
     */
    private static List<List<LogicalCell>> buildLogicalRows(TableNode node, int columnCount) {
        int rowCount = node.rows().size();
        boolean[][] occupied = new boolean[rowCount][columnCount];
        List<List<LogicalCell>> result = new ArrayList<>(rowCount);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<DocumentTableCell> source = node.rows().get(rowIndex);
            List<LogicalCell> logical = new ArrayList<>(source.size());
            int sourceIdx = 0;
            int col = 0;
            while (col < columnCount) {
                if (occupied[rowIndex][col]) {
                    col++;
                    continue;
                }
                if (sourceIdx >= source.size()) {
                    throw new IllegalStateException("Row " + rowIndex
                            + " is missing a cell for column " + col
                            + " (table has " + columnCount + " columns; source row provides "
                            + source.size() + " cells, prior rowSpan covers some columns).");
                }
                DocumentTableCell cell = source.get(sourceIdx++);
                if (col + cell.colSpan() > columnCount) {
                    throw new IllegalStateException("Cell at row " + rowIndex
                            + " column " + col + " has colSpan " + cell.colSpan()
                            + " but only " + (columnCount - col) + " columns remain.");
                }
                if (rowIndex + cell.rowSpan() > rowCount) {
                    throw new IllegalStateException("Cell at row " + rowIndex
                            + " column " + col + " has rowSpan " + cell.rowSpan()
                            + " but only " + (rowCount - rowIndex) + " rows remain.");
                }
                for (int r = rowIndex; r < rowIndex + cell.rowSpan(); r++) {
                    for (int c = col; c < col + cell.colSpan(); c++) {
                        if (occupied[r][c]) {
                            throw new IllegalStateException("Cell at row " + rowIndex
                                    + " column " + col + " (colSpan=" + cell.colSpan()
                                    + ", rowSpan=" + cell.rowSpan()
                                    + ") overlaps an already-spanned position (" + r + ", " + c + ").");
                        }
                        occupied[r][c] = true;
                    }
                }
                logical.add(new LogicalCell(rowIndex, col, cell.colSpan(), cell.rowSpan(), toTableCell(cell)));
                col += cell.colSpan();
            }
            if (sourceIdx < source.size()) {
                throw new IllegalStateException("Row " + rowIndex
                        + " has " + (source.size() - sourceIdx) + " extra source cell(s) "
                        + "after the grid was already filled — column slots are accounted for "
                        + "by colSpan plus rowSpan from earlier rows.");
            }
            result.add(List.copyOf(logical));
        }
        return List.copyOf(result);
    }

    private static TableCellLayoutStyle[][] buildStylesGrid(TableNode node,
                                                            List<List<LogicalCell>> logicalRows,
                                                            int columnCount) {
        TableCellLayoutStyle tableDefault = TableCellLayoutStyle.merge(
                TableCellLayoutStyle.DEFAULT, toTableStyle(node.defaultCellStyle()));
        Map<Integer, TableCellLayoutStyle> rowStyleOverrides = toTableStyles(node.rowStyles());
        Map<Integer, TableCellLayoutStyle> columnStyleOverrides = toTableStyles(node.columnStyles());
        TableCellLayoutStyle[][] grid = new TableCellLayoutStyle[logicalRows.size()][columnCount];

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            TableCellLayoutStyle rowOverride = rowStyleOverrides.get(rowIndex);
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                TableCellLayoutStyle resolved = TableCellLayoutStyle.merge(
                        tableDefault, columnStyleOverrides.get(logical.startColumn()));
                resolved = TableCellLayoutStyle.merge(resolved, rowOverride);
                resolved = TableCellLayoutStyle.merge(resolved, logical.content().styleOverride());
                // Propagate the spanning cell's resolved style to every
                // grid position it occupies (across rowSpan rows AND
                // colSpan columns) so neighbour-style lookups for borders
                // and fill insets correctly detect a single shared cell.
                for (int r = logical.startRow(); r < logical.startRow() + logical.rowSpan(); r++) {
                    for (int c = logical.startColumn(); c < logical.startColumn() + logical.colSpan(); c++) {
                        grid[r][c] = resolved;
                    }
                }
            }
        }

        return grid;
    }

    private static double[] resolveNaturalColumnWidths(TableNode node,
                                                       List<TableColumnLayout> normalizedSpecs,
                                                       List<List<LogicalCell>> logicalRows,
                                                       TableCellLayoutStyle[][] stylesGrid,
                                                       int columnCount,
                                                       TextMeasurementSystem measurement) {
        double[] widths = new double[columnCount];
        double[] singleCellRequired = new double[columnCount];

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.colSpan() != 1) {
                    continue;
                }
                int col = logical.startColumn();
                singleCellRequired[col] = Math.max(singleCellRequired[col],
                        cellNaturalWidth(logical.content(), stylesGrid[rowIndex][col], measurement));
            }
        }

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            TableColumnLayout spec = normalizedSpecs.get(columnIndex);
            if (spec.isFixed()) {
                if (spec.requiredFixedWidth() + EPS < singleCellRequired[columnIndex]) {
                    throw new IllegalStateException("Fixed column " + columnIndex + " width " + spec.requiredFixedWidth()
                            + " is smaller than required natural width " + singleCellRequired[columnIndex] + ".");
                }
                widths[columnIndex] = spec.requiredFixedWidth();
            } else {
                widths[columnIndex] = singleCellRequired[columnIndex];
            }
        }

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.colSpan() == 1) {
                    continue;
                }
                int startCol = logical.startColumn();
                int endCol = startCol + logical.colSpan();
                double need = cellNaturalWidth(logical.content(), stylesGrid[rowIndex][startCol], measurement);
                double have = sumRange(widths, startCol, endCol);
                if (need <= have + EPS) {
                    continue;
                }
                distributeDeficit(widths, normalizedSpecs, startCol, endCol, need - have, rowIndex, logical.colSpan());
            }
        }

        return widths;
    }

    private static void distributeDeficit(double[] widths,
                                          List<TableColumnLayout> normalizedSpecs,
                                          int startCol,
                                          int endCol,
                                          double deficit,
                                          int rowIndex,
                                          int colSpan) {
        List<Integer> autoColumns = new ArrayList<>();
        for (int col = startCol; col < endCol; col++) {
            if (normalizedSpecs.get(col).isAuto()) {
                autoColumns.add(col);
            }
        }
        if (autoColumns.isEmpty()) {
            throw new IllegalStateException("Spanned cell at row " + rowIndex + " over " + colSpan
                    + " fixed columns starting at " + startCol + " requires extra width " + deficit
                    + " but all spanned columns are fixed.");
        }
        double share = deficit / autoColumns.size();
        for (int col : autoColumns) {
            widths[col] += share;
        }
    }

    private static double[] resolveFinalColumnWidths(TableNode node,
                                                     List<TableColumnLayout> normalizedSpecs,
                                                     double[] naturalWidths,
                                                     double naturalWidth) {
        double[] finalWidths = naturalWidths.clone();
        if (node.width() == null) {
            return finalWidths;
        }
        if (node.width() + EPS < naturalWidth) {
            throw new IllegalStateException("Requested table width " + node.width()
                    + " is smaller than natural width " + naturalWidth + ".");
        }

        double extra = node.width() - naturalWidth;
        if (extra <= EPS) {
            return finalWidths;
        }

        List<Integer> autoColumns = new ArrayList<>();
        for (int index = 0; index < normalizedSpecs.size(); index++) {
            if (normalizedSpecs.get(index).isAuto()) {
                autoColumns.add(index);
            }
        }

        if (autoColumns.isEmpty()) {
            finalWidths[finalWidths.length - 1] += extra;
            return finalWidths;
        }

        double share = extra / autoColumns.size();
        for (Integer autoColumn : autoColumns) {
            finalWidths[autoColumn] += share;
        }
        return finalWidths;
    }

    private static double cellNaturalWidth(TableCellContent cell,
                                           TableCellLayoutStyle style,
                                           TextMeasurementSystem measurement) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        double maxWidth = 0.0;
        for (String line : sanitizeCellLines(cell)) {
            maxWidth = Math.max(maxWidth, measurement.textWidth(style.textStyle(), line));
        }
        return maxWidth + padding.horizontal();
    }

    private static double cellNaturalHeight(TableCellContent cell,
                                            TableCellLayoutStyle style,
                                            TextMeasurementSystem measurement) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        int lineCount = Math.max(1, sanitizeCellLines(cell).size());
        return (lineCount * measurement.lineHeight(style.textStyle()))
                + ((lineCount - 1) * tableCellLineSpacing(style))
                + padding.vertical();
    }

    private static double tableCellLineSpacing(TableCellLayoutStyle style) {
        return style.lineSpacing() == null ? 0.0 : style.lineSpacing();
    }

    private static List<TableColumnLayout> normalizeSpecs(TableNode node, int columnCount) {
        List<TableColumnLayout> normalized = new ArrayList<>(columnCount);
        List<TableColumnLayout> columns = toTableColumns(node.columns());
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            normalized.add(columnIndex < columns.size() ? columns.get(columnIndex) : TableColumnLayout.auto());
        }
        return List.copyOf(normalized);
    }

    private static int resolveColumnCount(TableNode node) {
        // The first row by definition has no rowSpan-occupied slots from
        // earlier rows, so its colSpan sum equals the table's column count.
        // Subsequent rows may have fewer source cells when prior rowSpan
        // covers some of their columns, so they must not be used to derive
        // the column count.
        int firstRowColSpanSum = 0;
        if (!node.rows().isEmpty()) {
            for (DocumentTableCell cell : node.rows().get(0)) {
                firstRowColSpanSum += cell.colSpan();
            }
        }
        return Math.max(node.columns().size(), firstRowColSpanSum);
    }

    private static void validateRowsExist(TableNode node) {
        if (node.rows().isEmpty()) {
            throw new IllegalStateException("Table '" + displayName(node) + "' must contain at least one row.");
        }
    }

    // Note: row colSpan-sum validation is no longer a separate pass — the
    // occupancy-grid algorithm in buildLogicalRows enforces both
    // colSpan/rowSpan bounds and per-row coverage with precise diagnostics
    // (missing cell, extra cell, overlap, out-of-bounds span).

    private static EnumSet<Side> borderSides(TableCellLayoutStyle[][] stylesGrid,
                                             int rowIndex,
                                             int startCol,
                                             int colSpan) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (ownsTopBoundary(stylesGrid, rowIndex, startCol, colSpan)) {
            sides.add(Side.TOP);
        }
        if (ownsLeftBoundary(stylesGrid, rowIndex, startCol)) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private static Padding fillInsets(TableCellLayoutStyle[][] stylesGrid,
                                      int rowIndex,
                                      int startCol,
                                      int colSpan) {
        TableCellLayoutStyle ownStyle = stylesGrid[rowIndex][startCol];
        double ownStroke = strokeWidth(ownStyle);
        double topInset = topBoundaryStrokeWidth(stylesGrid, rowIndex, startCol, colSpan) / 2.0;
        double rightInset = ownStroke / 2.0;
        double bottomInset = ownStroke / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesGrid, rowIndex, startCol) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private static double topBoundaryStrokeWidth(TableCellLayoutStyle[][] stylesGrid,
                                                 int rowIndex,
                                                 int startCol,
                                                 int colSpan) {
        if (ownsTopBoundary(stylesGrid, rowIndex, startCol, colSpan)) {
            return strokeWidth(stylesGrid[rowIndex][startCol]);
        }
        double maxAbove = 0.0;
        for (int col = startCol; col < startCol + colSpan; col++) {
            maxAbove = Math.max(maxAbove, strokeWidth(stylesGrid[rowIndex - 1][col]));
        }
        return maxAbove;
    }

    private static double leftBoundaryStrokeWidth(TableCellLayoutStyle[][] stylesGrid,
                                                  int rowIndex,
                                                  int startCol) {
        if (ownsLeftBoundary(stylesGrid, rowIndex, startCol)) {
            return strokeWidth(stylesGrid[rowIndex][startCol]);
        }
        return strokeWidth(stylesGrid[rowIndex][startCol - 1]);
    }

    private static boolean ownsTopBoundary(TableCellLayoutStyle[][] stylesGrid,
                                           int rowIndex,
                                           int startCol,
                                           int colSpan) {
        if (rowIndex == 0) {
            return true;
        }
        for (int col = startCol; col < startCol + colSpan; col++) {
            if (hasVisibleStroke(stylesGrid[rowIndex - 1][col])) {
                return false;
            }
        }
        return true;
    }

    private static boolean ownsLeftBoundary(TableCellLayoutStyle[][] stylesGrid,
                                            int rowIndex,
                                            int startCol) {
        return startCol == 0 || !hasVisibleStroke(stylesGrid[rowIndex][startCol - 1]);
    }

    private static double strokeWidth(TableCellLayoutStyle style) {
        if (style == null || style.stroke() == null) {
            return 0.0;
        }
        return style.stroke().width();
    }

    private static boolean hasVisibleStroke(TableCellLayoutStyle style) {
        return strokeWidth(style) > EPS;
    }

    private static List<String> sanitizeCellLines(TableCellContent cell) {
        List<String> sanitized = new ArrayList<>(cell.lines().size());
        for (String line : cell.lines()) {
            sanitized.add(line == null ? "" : line.replace('\r', ' ').replace('\n', ' '));
        }
        return List.copyOf(sanitized);
    }

    private static List<Double> toList(double[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (double value : values) {
            result.add(value);
        }
        return List.copyOf(result);
    }

    private static double sum(double[] values) {
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static double sumRange(double[] values, int fromInclusive, int toExclusive) {
        double total = 0.0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            total += values[i];
        }
        return total;
    }

    private static String cellName(TableNode node, int rowIndex, int columnIndex) {
        return displayName(node) + "__row_" + rowIndex + "__cell_" + columnIndex;
    }

    private static String displayName(DocumentNode node) {
        if (node.name() == null || node.name().isBlank()) {
            return node.nodeKind();
        }
        return node.name();
    }
}
