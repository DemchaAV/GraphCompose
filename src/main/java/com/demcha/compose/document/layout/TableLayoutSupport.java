package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
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
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableRows;
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

    static ResolvedTableLayout resolveTableLayout(TableNode node,
                                                  TextMeasurementSystem measurement,
                                                  double availableWidth) {
        validateRowsExist(node);
        int columnCount = resolveColumnCount(node);
        validateRowLengths(node, columnCount);

        List<TableColumnLayout> normalizedSpecs = normalizeSpecs(node, columnCount);
        List<List<TableCellLayoutStyle>> stylesByRow = resolveCellStyles(node, columnCount);
        List<List<TableCellContent>> tableRows = toTableRows(node.rows());
        double[] naturalWidths = resolveNaturalColumnWidths(node, normalizedSpecs, stylesByRow, columnCount, measurement);
        double naturalWidth = sum(naturalWidths);
        double[] finalWidths = resolveFinalColumnWidths(node, normalizedSpecs, naturalWidths, naturalWidth);
        double finalWidth = sum(finalWidths);

        double innerAvailableWidth = Math.max(0.0, availableWidth - node.padding().horizontal());
        if (finalWidth > innerAvailableWidth + EPS) {
            throw new IllegalStateException("Table '" + displayName(node) + "' width " + finalWidth
                    + " exceeds available width " + innerAvailableWidth + ".");
        }

        List<List<TableResolvedCell>> rows = new ArrayList<>(node.rows().size());
        List<Double> rowHeights = new ArrayList<>(node.rows().size());

        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            List<TableCellContent> rowValues = tableRows.get(rowIndex);
            List<TableCellLayoutStyle> resolvedStyles = stylesByRow.get(rowIndex);
            double rowHeight = 0.0;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                rowHeight = Math.max(rowHeight, cellNaturalHeight(rowValues.get(columnIndex), resolvedStyles.get(columnIndex), measurement));
            }
            rowHeights.add(rowHeight);

            List<TableResolvedCell> cells = new ArrayList<>(columnCount);
            double x = 0.0;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                cells.add(new TableResolvedCell(
                        cellName(node, rowIndex, columnIndex),
                        x,
                        finalWidths[columnIndex],
                        rowHeight,
                        sanitizeCellLines(rowValues.get(columnIndex)),
                        resolvedStyles.get(columnIndex),
                        fillInsets(stylesByRow, rowIndex, columnIndex),
                        borderSides(stylesByRow, rowIndex, columnIndex)));
                x += finalWidths[columnIndex];
            }
            rows.add(List.copyOf(cells));
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

    private static List<List<TableCellLayoutStyle>> resolveCellStyles(TableNode node, int columnCount) {
        TableCellLayoutStyle tableDefault = TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, toTableStyle(node.defaultCellStyle()));
        Map<Integer, TableCellLayoutStyle> rowStyleOverrides = toTableStyles(node.rowStyles());
        Map<Integer, TableCellLayoutStyle> columnStyleOverrides = toTableStyles(node.columnStyles());
        List<List<TableCellLayoutStyle>> result = new ArrayList<>(node.rows().size());

        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            List<TableCellLayoutStyle> rowResult = new ArrayList<>(columnCount);
            TableCellLayoutStyle rowOverride = rowStyleOverrides.get(rowIndex);
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                TableCellLayoutStyle resolved = TableCellLayoutStyle.merge(tableDefault, columnStyleOverrides.get(columnIndex));
                resolved = TableCellLayoutStyle.merge(resolved, rowOverride);
                resolved = TableCellLayoutStyle.merge(resolved, toTableStyle(node.rows().get(rowIndex).get(columnIndex).style()));
                rowResult.add(resolved);
            }
            result.add(List.copyOf(rowResult));
        }

        return List.copyOf(result);
    }

    private static double[] resolveNaturalColumnWidths(TableNode node,
                                                       List<TableColumnLayout> normalizedSpecs,
                                                       List<List<TableCellLayoutStyle>> stylesByRow,
                                                       int columnCount,
                                                       TextMeasurementSystem measurement) {
        double[] widths = new double[columnCount];

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            double requiredNaturalWidth = 0.0;
            for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
                requiredNaturalWidth = Math.max(
                        requiredNaturalWidth,
                        cellNaturalWidth(toTableCell(node.rows().get(rowIndex).get(columnIndex)),
                                stylesByRow.get(rowIndex).get(columnIndex), measurement));
            }

            TableColumnLayout spec = normalizedSpecs.get(columnIndex);
            if (spec.isFixed()) {
                if (spec.requiredFixedWidth() + EPS < requiredNaturalWidth) {
                    throw new IllegalStateException("Fixed column " + columnIndex + " width " + spec.requiredFixedWidth()
                            + " is smaller than required natural width " + requiredNaturalWidth + ".");
                }
                widths[columnIndex] = spec.requiredFixedWidth();
            } else {
                widths[columnIndex] = requiredNaturalWidth;
            }
        }

        return widths;
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
        int maxRowColumns = node.rows().stream().mapToInt(List::size).max().orElse(0);
        return Math.max(node.columns().size(), maxRowColumns);
    }

    private static void validateRowsExist(TableNode node) {
        if (node.rows().isEmpty()) {
            throw new IllegalStateException("Table '" + displayName(node) + "' must contain at least one row.");
        }
    }

    private static void validateRowLengths(TableNode node, int columnCount) {
        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            int actual = node.rows().get(rowIndex).size();
            if (actual != columnCount) {
                throw new IllegalStateException("Row " + rowIndex + " has " + actual
                        + " cells but table requires " + columnCount + " columns.");
            }
        }
    }

    private static EnumSet<Side> borderSides(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.TOP);
        }
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private static Padding fillInsets(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        double topInset = topBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        double rightInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double bottomInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private static double topBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private static double leftBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex - 1));
    }

    private static boolean ownsTopBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return rowIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private static boolean ownsLeftBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return columnIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex).get(columnIndex - 1));
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
