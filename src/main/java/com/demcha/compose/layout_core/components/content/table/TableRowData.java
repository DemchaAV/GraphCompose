package com.demcha.compose.layout_core.components.content.table;

import com.demcha.compose.layout_core.components.core.Component;

import java.util.List;
import java.util.Objects;

/**
 * Resolved row payload rendered atomically as a single leaf entity.
 */
public record TableRowData(List<TableResolvedCell> cells) implements Component {
    public TableRowData {
        Objects.requireNonNull(cells, "cells");
        cells = List.copyOf(cells);
    }
}
