package com.demcha.compose.layout_core.components.content.table;

import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.core.Entity;

import java.util.List;
import java.util.Objects;

/**
 * Resolved table layout metadata stored on the table root entity for tests and debugging.
 */
public record TableLayoutData(
        List<Double> columnWidths,
        double naturalWidth,
        double finalWidth,
        int rowCount,
        int columnCount,
        List<Entity> rowEntities
) implements Component {
    public TableLayoutData {
        Objects.requireNonNull(columnWidths, "columnWidths");
        Objects.requireNonNull(rowEntities, "rowEntities");
        columnWidths = List.copyOf(columnWidths);
        rowEntities = List.copyOf(rowEntities);
    }
}
