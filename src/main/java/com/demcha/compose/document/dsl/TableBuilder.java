package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.dsl.internal.SemanticNameNormalizer;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for semantic table nodes.
 */
public final class TableBuilder {
    private String name = "";
    private final List<DocumentTableColumn> columns = new ArrayList<>();
    private final List<List<DocumentTableCell>> rows = new ArrayList<>();
    private final Map<Integer, DocumentTableStyle> rowStyles = new LinkedHashMap<>();
    private final Map<Integer, DocumentTableStyle> columnStyles = new LinkedHashMap<>();
    private DocumentTableStyle defaultCellStyle = DocumentTableStyle.empty();
    private Double width;
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a table builder.
     */
    public TableBuilder() {
    }

    /**
     * Sets the table node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public TableBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets columns with public canonical table column values.
     *
     * @param columns column specifications
     * @return this builder
     */
    public TableBuilder columns(DocumentTableColumn... columns) {
        this.columns.clear();
        if (columns != null) {
            for (DocumentTableColumn column : columns) {
                this.columns.add(Objects.requireNonNull(column, "column"));
            }
        }
        return this;
    }

    /**
     * Replaces table columns with auto-width columns.
     *
     * @param count number of auto columns
     * @return this builder
     */
    public TableBuilder autoColumns(int count) {
        this.columns.clear();
        for (int index = 0; index < count; index++) {
            this.columns.add(DocumentTableColumn.auto());
        }
        return this;
    }

    /**
     * Adds a public canonical table column value.
     *
     * @param column column specification
     * @return this builder
     */
    public TableBuilder addColumn(DocumentTableColumn column) {
        this.columns.add(Objects.requireNonNull(column, "column"));
        return this;
    }

    /**
     * Adds a plain-text table row.
     *
     * @param values cell text values
     * @return this builder
     */
    public TableBuilder row(String... values) {
        List<DocumentTableCell> row = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                row.add(DocumentTableCell.text(value));
            }
        }
        return rowCells(row);
    }

    /**
     * Adds a row made of public canonical table cells.
     *
     * @param row cells in column order
     * @return this builder
     */
    public TableBuilder rowCells(List<DocumentTableCell> row) {
        this.rows.add(List.copyOf(Objects.requireNonNull(row, "row")));
        return this;
    }

    /**
     * Adds a row made of public canonical table cells.
     *
     * @param row table cells
     * @return this builder
     */
    public TableBuilder rowCells(DocumentTableCell... row) {
        if (row == null) {
            return rowCells(List.of());
        }
        List<DocumentTableCell> cells = new ArrayList<>(row.length);
        for (DocumentTableCell cell : row) {
            cells.add(Objects.requireNonNull(cell, "cell"));
        }
        return rowCells(cells);
    }

    /**
     * Adds a semantic header row as the first logical table row.
     *
     * <p>This is a naming convenience over {@link #row(String...)} for the
     * common "header then data rows" authoring pattern.</p>
     *
     * @param values header cell text values
     * @return this builder
     */
    public TableBuilder header(String... values) {
        return row(values);
    }

    /**
     * Adds a semantic header row from public canonical table cells.
     *
     * @param row header cells
     * @return this builder
     */
    public TableBuilder headerCells(DocumentTableCell... row) {
        return rowCells(row);
    }

    /**
     * Adds a semantic header row from public canonical table cells.
     *
     * @param row header cells
     * @return this builder
     */
    public TableBuilder headerCells(List<DocumentTableCell> row) {
        return rowCells(row);
    }

    /**
     * Adds multiple plain-text rows in source order.
     *
     * @param rows row values, one string array per row
     * @return this builder
     */
    public TableBuilder rows(String[]... rows) {
        if (rows != null) {
            for (String[] row : rows) {
                row(row);
            }
        }
        return this;
    }

    /**
     * Sets the default cell style with a public canonical table style.
     *
     * @param defaultCellStyle default cell style
     * @return this builder
     */
    public TableBuilder defaultCellStyle(DocumentTableStyle defaultCellStyle) {
        this.defaultCellStyle = defaultCellStyle == null ? DocumentTableStyle.empty() : defaultCellStyle;
        return this;
    }

    /**
     * Applies a public canonical style override to the first row.
     *
     * @param style header row style override
     * @return this builder
     */
    public TableBuilder headerStyle(DocumentTableStyle style) {
        return rowStyle(0, style);
    }

    /**
     * Applies a public canonical style override to a row.
     *
     * @param rowIndex zero-based row index
     * @param style row style override
     * @return this builder
     */
    public TableBuilder rowStyle(int rowIndex, DocumentTableStyle style) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex cannot be negative: " + rowIndex);
        }
        rowStyles.put(rowIndex, Objects.requireNonNull(style, "style"));
        return this;
    }

    /**
     * Applies a public canonical style override to a column.
     *
     * @param columnIndex zero-based column index
     * @param style column style override
     * @return this builder
     */
    public TableBuilder columnStyle(int columnIndex, DocumentTableStyle style) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex cannot be negative: " + columnIndex);
        }
        columnStyles.put(columnIndex, Objects.requireNonNull(style, "style"));
        return this;
    }

    /**
     * Sets explicit table width.
     *
     * @param width width in points
     * @return this builder
     */
    public TableBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Attaches table-level link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public TableBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
        return this;
    }

    /**
     * Attaches table-level bookmark metadata.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public TableBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets table padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public TableBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets table margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public TableBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the semantic table node.
     *
     * @return table node
     */
    public TableNode build() {
        return new TableNode(
                name,
                List.copyOf(columns),
                List.copyOf(rows),
                defaultCellStyle,
                Map.copyOf(rowStyles),
                Map.copyOf(columnStyles),
                width,
                linkOptions,
                bookmarkOptions,
                padding,
                margin);
    }
}

/**
 * Builder for explicit page-break control nodes.
 */
