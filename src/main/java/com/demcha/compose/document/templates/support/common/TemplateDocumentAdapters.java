package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.VAnchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal adapter between existing template support specs and canonical
 * document facade values.
 *
 * <p>This class is intentionally package-private. Public authoring code should
 * never need engine value objects; templates still use some internal support
 * specs while the remaining package cleanup continues.</p>
 *
 * @author Artem Demchyshyn
 */
final class TemplateDocumentAdapters {
    private TemplateDocumentAdapters() {
    }

    static DocumentInsets insets(Padding padding) {
        if (padding == null) {
            return DocumentInsets.zero();
        }
        return new DocumentInsets(padding.top(), padding.right(), padding.bottom(), padding.left());
    }

    static DocumentInsets insets(Margin margin) {
        if (margin == null) {
            return DocumentInsets.zero();
        }
        return new DocumentInsets(margin.top(), margin.right(), margin.bottom(), margin.left());
    }

    static DocumentTextStyle textStyle(TextStyle textStyle) {
        if (textStyle == null) {
            return DocumentTextStyle.DEFAULT;
        }
        return new DocumentTextStyle(
                textStyle.fontName(),
                textStyle.size(),
                decoration(textStyle.decoration()),
                DocumentColor.of(textStyle.color()));
    }

    static DocumentTextIndent indent(TextIndentStrategy indentStrategy) {
        if (indentStrategy == null) {
            return DocumentTextIndent.NONE;
        }
        return switch (indentStrategy) {
            case FIRST_LINE -> DocumentTextIndent.FIRST_LINE;
            case FROM_SECOND_LINE -> DocumentTextIndent.FROM_SECOND_LINE;
            case ALL_LINES -> DocumentTextIndent.ALL_LINES;
            case NONE -> DocumentTextIndent.NONE;
        };
    }

    static DocumentTableColumn tableColumn(TableColumnLayout column) {
        Objects.requireNonNull(column, "column");
        return column.type() == TableColumnLayout.Type.FIXED
                ? DocumentTableColumn.fixed(column.fixedWidth())
                : DocumentTableColumn.auto();
    }

    static DocumentTableStyle tableStyle(TableCellLayoutStyle style) {
        if (style == null) {
            return DocumentTableStyle.empty();
        }
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder();
        if (style.padding() != null) {
            builder.padding(insets(style.padding()));
        }
        if (style.fillColor() != null) {
            builder.fillColor(DocumentColor.of(style.fillColor()));
        }
        if (style.stroke() != null) {
            builder.stroke(stroke(style.stroke()));
        }
        if (style.textStyle() != null) {
            builder.textStyle(textStyle(style.textStyle()));
        }
        if (style.textAnchor() != null) {
            builder.textAnchor(tableTextAnchor(style.textAnchor()));
        }
        if (style.lineSpacing() != null) {
            builder.lineSpacing(style.lineSpacing());
        }
        return builder.build();
    }

    static DocumentTableCell tableCell(TableCellContent cell) {
        Objects.requireNonNull(cell, "cell");
        DocumentTableStyle style = cell.styleOverride() == null ? null : tableStyle(cell.styleOverride());
        return new DocumentTableCell(cell.lines(), style, cell.colSpan());
    }

    static List<DocumentTableCell> tableRow(List<TableCellContent> row) {
        List<DocumentTableCell> cells = new ArrayList<>();
        if (row != null) {
            for (TableCellContent cell : row) {
                cells.add(tableCell(cell));
            }
        }
        return List.copyOf(cells);
    }

    private static DocumentTextDecoration decoration(TextDecoration decoration) {
        if (decoration == null) {
            return DocumentTextDecoration.DEFAULT;
        }
        return switch (decoration) {
            case BOLD -> DocumentTextDecoration.BOLD;
            case ITALIC -> DocumentTextDecoration.ITALIC;
            case BOLD_ITALIC -> DocumentTextDecoration.BOLD_ITALIC;
            case UNDERLINE -> DocumentTextDecoration.UNDERLINE;
            case STRIKETHROUGH -> DocumentTextDecoration.STRIKETHROUGH;
            case DEFAULT -> DocumentTextDecoration.DEFAULT;
        };
    }

    private static DocumentStroke stroke(Stroke stroke) {
        return stroke == null ? null : new DocumentStroke(DocumentColor.of(stroke.strokeColor().color()), stroke.width());
    }

    private static DocumentTableTextAnchor tableTextAnchor(Anchor anchor) {
        if (anchor == null) {
            return null;
        }
        return switch (anchor.h()) {
            case LEFT -> switch (anchor.v()) {
                case TOP -> DocumentTableTextAnchor.TOP_LEFT;
                case BOTTOM -> DocumentTableTextAnchor.BOTTOM_LEFT;
                case MIDDLE -> DocumentTableTextAnchor.CENTER_LEFT;
                case DEFAULT -> DocumentTableTextAnchor.DEFAULT;
            };
            case CENTER -> anchor.v() == VAnchor.MIDDLE
                    ? DocumentTableTextAnchor.CENTER
                    : DocumentTableTextAnchor.DEFAULT;
            case RIGHT -> switch (anchor.v()) {
                case TOP -> DocumentTableTextAnchor.TOP_RIGHT;
                case BOTTOM -> DocumentTableTextAnchor.BOTTOM_RIGHT;
                case MIDDLE -> DocumentTableTextAnchor.CENTER_RIGHT;
                case DEFAULT -> DocumentTableTextAnchor.DEFAULT;
            };
            case DEFAULT -> DocumentTableTextAnchor.DEFAULT;
        };
    }
}
