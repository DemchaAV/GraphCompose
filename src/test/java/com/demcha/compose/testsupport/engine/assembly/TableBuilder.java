package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.testsupport.engine.assembly.container.ContainerBuilder;
import com.demcha.compose.engine.components.layout.StackAxis;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.table.TableLayoutData;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.content.table.TableRowData;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.core.EntityName;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.HAnchor;
import com.demcha.compose.engine.components.layout.VAnchor;
import com.demcha.compose.engine.components.renderable.TableRow;
import com.demcha.compose.engine.components.renderable.VContainer;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Engine-level builder for v1 tables.
 * <p>
 * The table implementation is intentionally hybrid. The table root is a
 * breakable vertical container, while each row is materialized as an atomic
 * leaf entity carrying precomputed cell payload. That design lets the engine
 * negotiate widths once at the table level while keeping pagination row-safe.
 * </p>
 *
 * <p>Use this builder when you need column width negotiation, scoped row/column
 * styling, and table pagination that behaves consistently with the rest of the
 * layout engine.</p>
 */
public class TableBuilder extends ContainerBuilder<TableBuilder> {
    private static final double EPS = 1e-6;

    private final List<TableColumnLayout> columnSpecs = new ArrayList<>();
    private final List<List<TableCellContent>> rows = new ArrayList<>();
    private final Map<Integer, TableCellLayoutStyle> rowStyles = new HashMap<>();
    private final Map<Integer, TableCellLayoutStyle> columnStyles = new HashMap<>();

    private TableCellLayoutStyle defaultCellStyle;
    private Double requestedWidth;
    private boolean materialized;

    TableBuilder(EntityManager entityManager) {
        super(entityManager, Align.defaultAlign(0));
    }

    @Override
    public void initialize() {
        entity.addComponent(new VContainer());
        entity.addComponent(StackAxis.VERTICAL);
    }

    public TableBuilder columns(TableColumnLayout... specs) {
        ensureMutable();
        Objects.requireNonNull(specs, "specs");
        columnSpecs.clear();
        columnSpecs.addAll(List.of(specs));
        return self();
    }

    public TableBuilder width(double width) {
        ensureMutable();
        if (width <= 0) {
            throw new IllegalArgumentException("Table width must be greater than 0.");
        }
        this.requestedWidth = width;
        return self();
    }

    public TableBuilder defaultCellStyle(TableCellLayoutStyle style) {
        ensureMutable();
        this.defaultCellStyle = Objects.requireNonNull(style, "style");
        return self();
    }

    public TableBuilder rowStyle(int rowIndex, TableCellLayoutStyle style) {
        ensureMutable();
        validateNonNegativeIndex(rowIndex, "Row");
        rowStyles.put(rowIndex, Objects.requireNonNull(style, "style"));
        return self();
    }

    public TableBuilder columnStyle(int columnIndex, TableCellLayoutStyle style) {
        ensureMutable();
        validateNonNegativeIndex(columnIndex, "Column");
        columnStyles.put(columnIndex, Objects.requireNonNull(style, "style"));
        return self();
    }

    public TableBuilder row(TableCellContent... cells) {
        ensureMutable();
        Objects.requireNonNull(cells, "cells");

        List<TableCellContent> values = new ArrayList<>(cells.length);
        for (TableCellContent cell : cells) {
            values.add(cell == null ? TableCellContent.text("") : cell);
        }
        rows.add(List.copyOf(values));
        return self();
    }

    public TableBuilder row(String... cells) {
        Objects.requireNonNull(cells, "cells");

        TableCellContent[] values = new TableCellContent[cells.length];
        for (int i = 0; i < cells.length; i++) {
            values[i] = TableCellContent.text(cells[i]);
        }
        return row(values);
    }

    @Override
    public Entity build() {
        if (!materialized) {
            materializeRows();
            materialized = true;
        }
        return super.build();
    }

    private void materializeRows() {
        validateRowsExist();

        int columnCount = resolveColumnCount();
        validateRowSpans(columnCount);

        List<List<LogicalCell>> logicalRows = buildLogicalRows();
        List<TableColumnLayout> normalizedSpecs = normalizeSpecs(columnCount);
        TableCellLayoutStyle[][] stylesGrid = buildStylesGrid(logicalRows, columnCount);
        double[] naturalWidths = resolveNaturalColumnWidths(normalizedSpecs, logicalRows, stylesGrid, columnCount);
        double naturalWidth = sum(naturalWidths);
        double[] finalWidths = resolveFinalColumnWidths(normalizedSpecs, naturalWidths, naturalWidth);
        double finalWidth = sum(finalWidths);

        List<Entity> rowEntities = new ArrayList<>(logicalRows.size());

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            Entity rowEntity = buildRowEntity(rowIndex, logicalRows.get(rowIndex), stylesGrid, finalWidths);
            addChild(rowEntity);
            entityManager.putEntity(rowEntity);
            rowEntities.add(rowEntity);
        }

        entity.addComponent(new TableLayoutData(
                toList(finalWidths),
                naturalWidth,
                finalWidth,
                logicalRows.size(),
                columnCount,
                List.copyOf(rowEntities)));
    }

    private Entity buildRowEntity(int rowIndex,
                                  List<LogicalCell> rowCells,
                                  TableCellLayoutStyle[][] stylesGrid,
                                  double[] columnWidths) {
        double rowHeight = 0.0;
        for (LogicalCell logical : rowCells) {
            TableCellLayoutStyle style = stylesGrid[rowIndex][logical.startColumn];
            rowHeight = Math.max(rowHeight, cellNaturalHeight(logical.content, style));
        }

        List<TableResolvedCell> cells = new ArrayList<>(rowCells.size());
        double rowWidth = 0.0;
        for (LogicalCell logical : rowCells) {
            TableCellLayoutStyle style = stylesGrid[rowIndex][logical.startColumn];
            double x = sumRange(columnWidths, 0, logical.startColumn);
            double width = sumRange(columnWidths, logical.startColumn, logical.startColumn + logical.colSpan);
            cells.add(new TableResolvedCell(
                    cellName(rowIndex, logical.startColumn),
                    x,
                    width,
                    rowHeight,
                    sanitizeCellLines(logical.content),
                    style,
                    fillInsets(stylesGrid, rowIndex, logical.startColumn, logical.colSpan),
                    borderSides(stylesGrid, rowIndex, logical.startColumn, logical.colSpan)
            ));
            rowWidth = Math.max(rowWidth, x + width);
        }

        Entity rowEntity = new Entity();
        rowEntity.addComponent(new EntityName(rowName(rowIndex)));
        rowEntity.addComponent(new Anchor(HAnchor.LEFT, VAnchor.DEFAULT));
        rowEntity.addComponent(new ContentSize(rowWidth, rowHeight));
        rowEntity.addComponent(new TableRowData(cells));
        rowEntity.addComponent(new TableRow());
        return rowEntity;
    }

    private List<List<LogicalCell>> buildLogicalRows() {
        List<List<LogicalCell>> result = new ArrayList<>(rows.size());
        for (List<TableCellContent> row : rows) {
            List<LogicalCell> logical = new ArrayList<>(row.size());
            int col = 0;
            for (TableCellContent cell : row) {
                logical.add(new LogicalCell(col, cell.colSpan(), cell));
                col += cell.colSpan();
            }
            result.add(List.copyOf(logical));
        }
        return List.copyOf(result);
    }

    private TableCellLayoutStyle[][] buildStylesGrid(List<List<LogicalCell>> logicalRows, int columnCount) {
        TableCellLayoutStyle tableDefault = TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, defaultCellStyle);
        TableCellLayoutStyle[][] grid = new TableCellLayoutStyle[logicalRows.size()][columnCount];

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            TableCellLayoutStyle rowOverride = rowStyles.get(rowIndex);
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                TableCellLayoutStyle resolved = TableCellLayoutStyle.merge(tableDefault, columnStyles.get(logical.startColumn));
                resolved = TableCellLayoutStyle.merge(resolved, rowOverride);
                resolved = TableCellLayoutStyle.merge(resolved, logical.content.styleOverride());
                for (int col = logical.startColumn; col < logical.startColumn + logical.colSpan; col++) {
                    grid[rowIndex][col] = resolved;
                }
            }
        }

        return grid;
    }

    private double[] resolveNaturalColumnWidths(List<TableColumnLayout> normalizedSpecs,
                                                List<List<LogicalCell>> logicalRows,
                                                TableCellLayoutStyle[][] stylesGrid,
                                                int columnCount) {
        double[] widths = new double[columnCount];
        double[] singleCellRequired = new double[columnCount];

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.colSpan != 1) {
                    continue;
                }
                int col = logical.startColumn;
                singleCellRequired[col] = Math.max(singleCellRequired[col],
                        cellNaturalWidth(logical.content, stylesGrid[rowIndex][col]));
            }
        }

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            TableColumnLayout spec = normalizedSpecs.get(columnIndex);
            if (spec.isFixed()) {
                if (spec.requiredFixedWidth() + EPS < singleCellRequired[columnIndex]) {
                    throw new IllegalStateException("Fixed column %d width %.2f is smaller than required natural width %.2f."
                            .formatted(columnIndex, spec.requiredFixedWidth(), singleCellRequired[columnIndex]));
                }
                widths[columnIndex] = spec.requiredFixedWidth();
            } else {
                widths[columnIndex] = singleCellRequired[columnIndex];
            }
        }

        for (int rowIndex = 0; rowIndex < logicalRows.size(); rowIndex++) {
            for (LogicalCell logical : logicalRows.get(rowIndex)) {
                if (logical.colSpan == 1) {
                    continue;
                }
                int startCol = logical.startColumn;
                int endCol = startCol + logical.colSpan;
                double need = cellNaturalWidth(logical.content, stylesGrid[rowIndex][startCol]);
                double have = sumRange(widths, startCol, endCol);
                if (need <= have + EPS) {
                    continue;
                }
                distributeDeficit(widths, normalizedSpecs, startCol, endCol, need - have, rowIndex, logical.colSpan);
            }
        }

        return widths;
    }

    private void distributeDeficit(double[] widths,
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
            throw new IllegalStateException("Spanned cell at row %d over %d fixed columns starting at %d requires extra width %.2f but all spanned columns are fixed."
                    .formatted(rowIndex, colSpan, startCol, deficit));
        }
        double share = deficit / autoColumns.size();
        for (int col : autoColumns) {
            widths[col] += share;
        }
    }

    private double[] resolveFinalColumnWidths(List<TableColumnLayout> normalizedSpecs,
                                              double[] naturalWidths,
                                              double naturalWidth) {
        double[] finalWidths = naturalWidths.clone();

        if (requestedWidth == null) {
            return finalWidths;
        }

        if (requestedWidth + EPS < naturalWidth) {
            throw new IllegalStateException("Requested table width %.2f is smaller than natural width %.2f."
                    .formatted(requestedWidth, naturalWidth));
        }

        double extra = requestedWidth - naturalWidth;
        if (extra <= EPS) {
            return finalWidths;
        }

        List<Integer> autoColumns = new ArrayList<>();
        for (int i = 0; i < normalizedSpecs.size(); i++) {
            if (normalizedSpecs.get(i).isAuto()) {
                autoColumns.add(i);
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

    private List<TableColumnLayout> normalizeSpecs(int columnCount) {
        List<TableColumnLayout> normalized = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            normalized.add(columnIndex < columnSpecs.size() ? columnSpecs.get(columnIndex) : TableColumnLayout.auto());
        }
        return normalized;
    }

    private int resolveColumnCount() {
        int maxRowSpanSum = 0;
        for (List<TableCellContent> row : rows) {
            int spanSum = 0;
            for (TableCellContent cell : row) {
                spanSum += cell.colSpan();
            }
            maxRowSpanSum = Math.max(maxRowSpanSum, spanSum);
        }
        return Math.max(columnSpecs.size(), maxRowSpanSum);
    }

    private double cellNaturalWidth(TableCellContent cell, TableCellLayoutStyle style) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        double maxWidth = 0.0;
        for (String line : sanitizeCellLines(cell)) {
            maxWidth = Math.max(maxWidth, measureText(line, style).width());
        }
        return maxWidth + padding.horizontal();
    }

    private double cellNaturalHeight(TableCellContent cell, TableCellLayoutStyle style) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        int lineCount = Math.max(1, sanitizeCellLines(cell).size());
        return (lineCount * measureLineHeight(style))
                + ((lineCount - 1) * lineSpacing(style))
                + padding.vertical();
    }

    private double measureLineHeight(TableCellLayoutStyle style) {
        return textMeasurementSystem().lineHeight(style.textStyle());
    }

    private double lineSpacing(TableCellLayoutStyle style) {
        return style.lineSpacing() == null ? 0.0 : style.lineSpacing();
    }

    private ContentSize measureText(String text, TableCellLayoutStyle style) {
        return textMeasurementSystem().measure(style.textStyle(), text);
    }

    private TextMeasurementSystem textMeasurementSystem() {
        return entityManager.getSystems()
                .getSystem(TextMeasurementSystem.class)
                .orElseThrow(() -> new IllegalStateException("TextMeasurementSystem is required to measure table text."));
    }

    private EnumSet<Side> borderSides(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol, int colSpan) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (ownsTopBoundary(stylesGrid, rowIndex, startCol, colSpan)) {
            sides.add(Side.TOP);
        }
        if (ownsLeftBoundary(stylesGrid, rowIndex, startCol)) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private Padding fillInsets(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol, int colSpan) {
        TableCellLayoutStyle ownStyle = stylesGrid[rowIndex][startCol];
        double ownStroke = strokeWidth(ownStyle);
        double topInset = topBoundaryStrokeWidth(stylesGrid, rowIndex, startCol, colSpan) / 2.0;
        double rightInset = ownStroke / 2.0;
        double bottomInset = ownStroke / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesGrid, rowIndex, startCol) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private double topBoundaryStrokeWidth(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol, int colSpan) {
        if (ownsTopBoundary(stylesGrid, rowIndex, startCol, colSpan)) {
            return strokeWidth(stylesGrid[rowIndex][startCol]);
        }
        double maxAbove = 0.0;
        for (int col = startCol; col < startCol + colSpan; col++) {
            maxAbove = Math.max(maxAbove, strokeWidth(stylesGrid[rowIndex - 1][col]));
        }
        return maxAbove;
    }

    private double leftBoundaryStrokeWidth(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol) {
        if (ownsLeftBoundary(stylesGrid, rowIndex, startCol)) {
            return strokeWidth(stylesGrid[rowIndex][startCol]);
        }
        return strokeWidth(stylesGrid[rowIndex][startCol - 1]);
    }

    private boolean ownsTopBoundary(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol, int colSpan) {
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

    private boolean ownsLeftBoundary(TableCellLayoutStyle[][] stylesGrid, int rowIndex, int startCol) {
        return startCol == 0 || !hasVisibleStroke(stylesGrid[rowIndex][startCol - 1]);
    }

    private double strokeWidth(TableCellLayoutStyle style) {
        if (style == null || style.stroke() == null) {
            return 0.0;
        }
        return style.stroke().width();
    }

    private boolean hasVisibleStroke(TableCellLayoutStyle style) {
        return strokeWidth(style) > EPS;
    }

    private List<Double> toList(double[] widths) {
        List<Double> result = new ArrayList<>(widths.length);
        for (double width : widths) {
            result.add(width);
        }
        return List.copyOf(result);
    }

    private double sum(double[] widths) {
        double total = 0.0;
        for (double width : widths) {
            total += width;
        }
        return total;
    }

    private double sumRange(double[] values, int fromInclusive, int toExclusive) {
        double total = 0.0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            total += values[i];
        }
        return total;
    }

    private List<String> sanitizeCellLines(TableCellContent cell) {
        List<String> sanitized = new ArrayList<>(cell.lines().size());
        for (String line : cell.lines()) {
            sanitized.add(sanitizeCellLine(line));
        }
        return List.copyOf(sanitized);
    }

    private String sanitizeCellLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private String rowName(int rowIndex) {
        return tableName() + "__row_" + rowIndex;
    }

    private String cellName(int rowIndex, int columnIndex) {
        return rowName(rowIndex) + "__cell_" + columnIndex;
    }

    private String tableName() {
        return entity.getComponent(EntityName.class)
                .map(EntityName::value)
                .orElse("Table");
    }

    private void validateRowsExist() {
        if (rows.isEmpty()) {
            throw new IllegalStateException("Table must contain at least one row.");
        }
    }

    private void validateRowSpans(int columnCount) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int spanSum = 0;
            for (TableCellContent cell : rows.get(rowIndex)) {
                spanSum += cell.colSpan();
            }
            if (spanSum != columnCount) {
                throw new IllegalStateException("Row %d has cells with colSpan sum %d but table requires %d columns."
                        .formatted(rowIndex, spanSum, columnCount));
            }
        }
    }

    private void validateNonNegativeIndex(int index, String label) {
        if (index < 0) {
            throw new IllegalArgumentException(label + " index cannot be negative.");
        }
    }

    private void ensureMutable() {
        if (materialized) {
            throw new IllegalStateException("Table has already been built and cannot be modified.");
        }
    }

    private record LogicalCell(int startColumn, int colSpan, TableCellContent content) {
    }
}
