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
        validateRowLengths(columnCount);

        List<TableColumnLayout> normalizedSpecs = normalizeSpecs(columnCount);
        List<List<TableCellLayoutStyle>> stylesByRow = resolveCellStyles(columnCount);
        double[] naturalWidths = resolveNaturalColumnWidths(normalizedSpecs, stylesByRow, columnCount);
        double naturalWidth = sum(naturalWidths);
        double[] finalWidths = resolveFinalColumnWidths(normalizedSpecs, naturalWidths, naturalWidth);
        double finalWidth = sum(finalWidths);

        List<Entity> rowEntities = new ArrayList<>(rows.size());

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Entity rowEntity = buildRowEntity(rowIndex, rows.get(rowIndex), stylesByRow, finalWidths);
            addChild(rowEntity);
            entityManager.putEntity(rowEntity);
            rowEntities.add(rowEntity);
        }

        entity.addComponent(new TableLayoutData(
                toList(finalWidths),
                naturalWidth,
                finalWidth,
                rows.size(),
                columnCount,
                List.copyOf(rowEntities)));
    }

    private Entity buildRowEntity(int rowIndex,
                                  List<TableCellContent> rowValues,
                                  List<List<TableCellLayoutStyle>> stylesByRow,
                                  double[] columnWidths) {
        List<TableCellLayoutStyle> resolvedStyles = stylesByRow.get(rowIndex);
        double rowHeight = 0.0;
        for (int columnIndex = 0; columnIndex < columnWidths.length; columnIndex++) {
            rowHeight = Math.max(rowHeight, cellNaturalHeight(rowValues.get(columnIndex), resolvedStyles.get(columnIndex)));
        }

        List<TableResolvedCell> cells = new ArrayList<>(columnWidths.length);
        double x = 0.0;
        for (int columnIndex = 0; columnIndex < columnWidths.length; columnIndex++) {
            cells.add(new TableResolvedCell(
                    cellName(rowIndex, columnIndex),
                    x,
                    columnWidths[columnIndex],
                    rowHeight,
                    sanitizeCellLines(rowValues.get(columnIndex)),
                    resolvedStyles.get(columnIndex),
                    fillInsets(stylesByRow, rowIndex, columnIndex),
                    borderSides(stylesByRow, rowIndex, columnIndex)
            ));
            x += columnWidths[columnIndex];
        }

        Entity rowEntity = new Entity();
        rowEntity.addComponent(new EntityName(rowName(rowIndex)));
        rowEntity.addComponent(new Anchor(HAnchor.LEFT, VAnchor.DEFAULT));
        rowEntity.addComponent(new ContentSize(x, rowHeight));
        rowEntity.addComponent(new TableRowData(cells));
        rowEntity.addComponent(new TableRow());
        return rowEntity;
    }

    private List<List<TableCellLayoutStyle>> resolveCellStyles(int columnCount) {
        TableCellLayoutStyle tableDefault = TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, defaultCellStyle);
        List<List<TableCellLayoutStyle>> result = new ArrayList<>(rows.size());

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<TableCellLayoutStyle> rowResult = new ArrayList<>(columnCount);
            TableCellLayoutStyle rowOverride = rowStyles.get(rowIndex);

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                TableCellLayoutStyle resolved = TableCellLayoutStyle.merge(tableDefault, columnStyles.get(columnIndex));
                resolved = TableCellLayoutStyle.merge(resolved, rowOverride);
                resolved = TableCellLayoutStyle.merge(resolved, rows.get(rowIndex).get(columnIndex).styleOverride());
                rowResult.add(resolved);
            }

            result.add(List.copyOf(rowResult));
        }

        return result;
    }

    private double[] resolveNaturalColumnWidths(List<TableColumnLayout> normalizedSpecs,
                                                List<List<TableCellLayoutStyle>> stylesByRow,
                                                int columnCount) {
        double[] widths = new double[columnCount];

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            double requiredNaturalWidth = 0.0;
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                requiredNaturalWidth = Math.max(
                        requiredNaturalWidth,
                        cellNaturalWidth(rows.get(rowIndex).get(columnIndex), stylesByRow.get(rowIndex).get(columnIndex))
                );
            }

            TableColumnLayout spec = normalizedSpecs.get(columnIndex);
            if (spec.isFixed()) {
                if (spec.requiredFixedWidth() + EPS < requiredNaturalWidth) {
                    throw new IllegalStateException("Fixed column %d width %.2f is smaller than required natural width %.2f."
                            .formatted(columnIndex, spec.requiredFixedWidth(), requiredNaturalWidth));
                }
                widths[columnIndex] = spec.requiredFixedWidth();
            } else {
                widths[columnIndex] = requiredNaturalWidth;
            }
        }

        return widths;
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
        int maxRowColumns = rows.stream().mapToInt(List::size).max().orElse(0);
        return Math.max(columnSpecs.size(), maxRowColumns);
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

    private EnumSet<Side> borderSides(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.TOP);
        }
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private Padding fillInsets(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        double topInset = topBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        double rightInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double bottomInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private double topBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private double leftBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex - 1));
    }

    private boolean ownsTopBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return rowIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private boolean ownsLeftBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return columnIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex).get(columnIndex - 1));
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

    private void validateRowLengths(int columnCount) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int actual = rows.get(rowIndex).size();
            if (actual != columnCount) {
                throw new IllegalStateException("Row %d has %d cells but table requires %d columns."
                        .formatted(rowIndex, actual, columnCount));
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
}
