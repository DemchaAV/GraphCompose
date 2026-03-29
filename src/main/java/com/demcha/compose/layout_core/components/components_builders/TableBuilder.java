package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.layout_core.components.containers.abstract_builders.StackAxis;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.table.TableLayoutData;
import com.demcha.compose.layout_core.components.content.table.TableResolvedCell;
import com.demcha.compose.layout_core.components.content.table.TableRowData;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.core.EntityName;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.HAnchor;
import com.demcha.compose.layout_core.components.layout.VAnchor;
import com.demcha.compose.layout_core.components.renderable.TableRow;
import com.demcha.compose.layout_core.components.renderable.VContainer;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.interfaces.Font;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Public builder for v1 tables with negotiated column widths, scoped styling, and row-atomic pagination.
 */
public class TableBuilder extends ContainerBuilder<TableBuilder> {
    private static final double EPS = 1e-6;

    private final List<TableColumnSpec> columnSpecs = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();
    private final Map<Integer, TableCellStyle> rowStyles = new HashMap<>();
    private final Map<Integer, TableCellStyle> columnStyles = new HashMap<>();

    private TableCellStyle defaultCellStyle;
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

    public TableBuilder columns(TableColumnSpec... specs) {
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

    public TableBuilder defaultCellStyle(TableCellStyle style) {
        ensureMutable();
        this.defaultCellStyle = Objects.requireNonNull(style, "style");
        return self();
    }

    public TableBuilder rowStyle(int rowIndex, TableCellStyle style) {
        ensureMutable();
        validateNonNegativeIndex(rowIndex, "Row");
        rowStyles.put(rowIndex, Objects.requireNonNull(style, "style"));
        return self();
    }

    public TableBuilder columnStyle(int columnIndex, TableCellStyle style) {
        ensureMutable();
        validateNonNegativeIndex(columnIndex, "Column");
        columnStyles.put(columnIndex, Objects.requireNonNull(style, "style"));
        return self();
    }

    public TableBuilder row(String... cells) {
        ensureMutable();
        Objects.requireNonNull(cells, "cells");
        List<String> values = new ArrayList<>(cells.length);
        for (String cell : cells) {
            values.add(cell == null ? "" : cell);
        }
        rows.add(List.copyOf(values));
        return self();
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

        List<TableColumnSpec> normalizedSpecs = normalizeSpecs(columnCount);
        List<List<TableCellStyle>> stylesByRow = resolveCellStyles(columnCount);
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
                                  List<String> rowValues,
                                  List<List<TableCellStyle>> stylesByRow,
                                  double[] columnWidths) {
        List<TableCellStyle> resolvedStyles = stylesByRow.get(rowIndex);
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
                    sanitizeCellText(rowValues.get(columnIndex)),
                    resolvedStyles.get(columnIndex),
                    fillInsets(stylesByRow, rowIndex, columnIndex),
                    borderSides(rowIndex, columnIndex)
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

    private List<List<TableCellStyle>> resolveCellStyles(int columnCount) {
        TableCellStyle tableDefault = TableCellStyle.merge(TableCellStyle.DEFAULT, defaultCellStyle);
        List<List<TableCellStyle>> result = new ArrayList<>(rows.size());

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<TableCellStyle> rowResult = new ArrayList<>(columnCount);
            TableCellStyle rowOverride = rowStyles.get(rowIndex);

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                TableCellStyle resolved = TableCellStyle.merge(tableDefault, columnStyles.get(columnIndex));
                resolved = TableCellStyle.merge(resolved, rowOverride);
                rowResult.add(resolved);
            }

            result.add(List.copyOf(rowResult));
        }

        return result;
    }

    private double[] resolveNaturalColumnWidths(List<TableColumnSpec> normalizedSpecs,
                                                List<List<TableCellStyle>> stylesByRow,
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

            TableColumnSpec spec = normalizedSpecs.get(columnIndex);
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

    private double[] resolveFinalColumnWidths(List<TableColumnSpec> normalizedSpecs,
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

    private List<TableColumnSpec> normalizeSpecs(int columnCount) {
        List<TableColumnSpec> normalized = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            normalized.add(columnIndex < columnSpecs.size() ? columnSpecs.get(columnIndex) : TableColumnSpec.auto());
        }
        return normalized;
    }

    private int resolveColumnCount() {
        int maxRowColumns = rows.stream().mapToInt(List::size).max().orElse(0);
        return Math.max(columnSpecs.size(), maxRowColumns);
    }

    private double cellNaturalWidth(String text, TableCellStyle style) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        return measureText(sanitizeCellText(text), style).width() + padding.horizontal();
    }

    private double cellNaturalHeight(String text, TableCellStyle style) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        return measureText(sanitizeCellText(text), style).height() + padding.vertical();
    }

    private ContentSize measureText(String text, TableCellStyle style) {
        Class<? extends Font<?>> fontType = entityManager.getSystems()
                .getSystem(LayoutSystem.class)
                .orElseThrow(() -> new IllegalStateException("LayoutSystem is required to measure table text."))
                .getRenderingSystem()
                .fontClazz();

        Font<?> font = entityManager.getFonts()
                .getFont(style.textStyle().fontName(), fontType)
                .orElseThrow(() -> new IllegalStateException("Font not found for table cell style: " + style.textStyle().fontName()));

        double width = font.getTextWidth(style.textStyle(), text);
        double height = font.getLineHeight(style.textStyle());
        return new ContentSize(width, height);
    }

    private EnumSet<Side> borderSides(int rowIndex, int columnIndex) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (rowIndex == 0) {
            sides.add(Side.TOP);
        }
        if (columnIndex == 0) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private Padding fillInsets(List<List<TableCellStyle>> stylesByRow, int rowIndex, int columnIndex) {
        double topInset = topBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        double rightInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double bottomInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private double topBoundaryStrokeWidth(List<List<TableCellStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (rowIndex == 0) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private double leftBoundaryStrokeWidth(List<List<TableCellStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex - 1));
    }

    private double strokeWidth(TableCellStyle style) {
        if (style == null || style.stroke() == null) {
            return 0.0;
        }
        return style.stroke().width();
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

    private String sanitizeCellText(String value) {
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
