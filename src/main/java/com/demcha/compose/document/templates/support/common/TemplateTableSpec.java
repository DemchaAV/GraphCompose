package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.List;
import java.util.Map;

/**
 * Immutable table instruction used by shared template scene composers.
 */
public record TemplateTableSpec(
        String name,
        List<TableColumnSpec> columns,
        List<List<TableCellSpec>> rows,
        TableCellStyle defaultCellStyle,
        Map<Integer, TableCellStyle> rowStyles,
        Map<Integer, TableCellStyle> columnStyles,
        double width,
        Padding padding,
        Margin margin
) {
    public TemplateTableSpec {
        name = name == null ? "" : name;
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : defaultCellStyle;
        rowStyles = rowStyles == null ? Map.of() : Map.copyOf(rowStyles);
        columnStyles = columnStyles == null ? Map.of() : Map.copyOf(columnStyles);
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }
}
