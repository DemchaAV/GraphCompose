package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Map;

/**
 * Immutable table instruction used by shared template scene composers.
 */
public record TemplateTableSpec(
        String name,
        List<TableColumnLayout> columns,
        List<List<TableCellContent>> rows,
        TableCellLayoutStyle defaultCellStyle,
        Map<Integer, TableCellLayoutStyle> rowStyles,
        Map<Integer, TableCellLayoutStyle> columnStyles,
        double width,
        Padding padding,
        Margin margin
) {
    public TemplateTableSpec {
        name = name == null ? "" : name;
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        defaultCellStyle = defaultCellStyle == null ? TableCellLayoutStyle.DEFAULT : defaultCellStyle;
        rowStyles = rowStyles == null ? Map.of() : Map.copyOf(rowStyles);
        columnStyles = columnStyles == null ? Map.of() : Map.copyOf(columnStyles);
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }
}
