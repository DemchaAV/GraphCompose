package com.demcha.compose.document.layout;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Internal adapter seam between canonical document values and engine values.
 *
 * <p>Public authoring packages should stay on {@code document.*} types. Layout,
 * pagination, measurement, and rendering code use this adapter at runtime
 * boundaries before talking to the ECS-based engine.</p>
 */
final class DocumentNodeAdapters {
    private DocumentNodeAdapters() {
    }

    static Padding toPadding(DocumentInsets insets) {
        if (insets == null) {
            return Padding.zero();
        }
        return new Padding(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    static Margin toMargin(DocumentInsets insets) {
        if (insets == null) {
            return Margin.zero();
        }
        return new Margin(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    static TextStyle toTextStyle(DocumentTextStyle textStyle) {
        if (textStyle == null) {
            return TextStyle.DEFAULT_STYLE;
        }
        return new TextStyle(
                textStyle.fontName(),
                textStyle.size(),
                toDecoration(textStyle.decoration()),
                textStyle.color().color());
    }

    static TextIndentStrategy toIndentStrategy(DocumentTextIndent indent) {
        if (indent == null) {
            return TextIndentStrategy.NONE;
        }
        return switch (indent) {
            case FIRST_LINE -> TextIndentStrategy.FIRST_LINE;
            case FROM_SECOND_LINE -> TextIndentStrategy.FROM_SECOND_LINE;
            case ALL_LINES -> TextIndentStrategy.ALL_LINES;
            case NONE -> TextIndentStrategy.NONE;
        };
    }

    static Stroke toStroke(DocumentStroke stroke) {
        return stroke == null ? null : new Stroke(stroke.color().color(), stroke.width());
    }

    static ImageData toImageData(DocumentImageData imageData) {
        Objects.requireNonNull(imageData, "imageData");
        return imageData.path()
                .map(ImageData::create)
                .orElseGet(() -> ImageData.create(imageData.bytes().orElseThrow()));
    }

    static TableColumnLayout toTableColumn(DocumentTableColumn column) {
        Objects.requireNonNull(column, "column");
        return column.type() == DocumentTableColumn.Type.FIXED
                ? TableColumnLayout.fixed(column.fixedWidth())
                : TableColumnLayout.auto();
    }

    static List<TableColumnLayout> toTableColumns(List<DocumentTableColumn> columns) {
        return columns.stream()
                .map(DocumentNodeAdapters::toTableColumn)
                .toList();
    }

    static TableCellLayoutStyle toTableStyle(DocumentTableStyle style) {
        if (style == null) {
            return TableCellLayoutStyle.empty();
        }
        return TableCellLayoutStyle.builder()
                .padding(style.padding() == null ? null : toPadding(style.padding()))
                .fillColor(style.fillColor() == null ? null : style.fillColor().color())
                .stroke(style.stroke() == null ? null : toStroke(style.stroke()))
                .textStyle(style.textStyle() == null ? null : toTextStyle(style.textStyle()))
                .textAnchor(style.textAnchor() == null ? null : toAnchor(style.textAnchor()))
                .lineSpacing(style.lineSpacing())
                .build();
    }

    static Map<Integer, TableCellLayoutStyle> toTableStyles(Map<Integer, DocumentTableStyle> styles) {
        return styles.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> toTableStyle(entry.getValue())));
    }

    static TableCellContent toTableCell(DocumentTableCell cell) {
        Objects.requireNonNull(cell, "cell");
        TableCellContent spec = TableCellContent.of(cell.lines());
        return cell.style() == null ? spec : spec.withStyle(toTableStyle(cell.style()));
    }

    static List<List<TableCellContent>> toTableRows(List<List<DocumentTableCell>> rows) {
        return rows.stream()
                .map(row -> row.stream()
                        .map(DocumentNodeAdapters::toTableCell)
                        .toList())
                .toList();
    }

    private static TextDecoration toDecoration(DocumentTextDecoration decoration) {
        if (decoration == null) {
            return TextDecoration.DEFAULT;
        }
        return switch (decoration) {
            case BOLD -> TextDecoration.BOLD;
            case ITALIC -> TextDecoration.ITALIC;
            case BOLD_ITALIC -> TextDecoration.BOLD_ITALIC;
            case UNDERLINE -> TextDecoration.UNDERLINE;
            case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
            case DEFAULT -> TextDecoration.DEFAULT;
        };
    }

    private static Anchor toAnchor(DocumentTableTextAnchor anchor) {
        if (anchor == null) {
            return null;
        }
        return switch (anchor) {
            case CENTER_LEFT -> Anchor.centerLeft();
            case CENTER -> Anchor.center();
            case CENTER_RIGHT -> Anchor.centerRight();
            case TOP_LEFT -> Anchor.topLeft();
            case TOP_RIGHT -> Anchor.topRight();
            case BOTTOM_LEFT -> Anchor.bottomLeft();
            case BOTTOM_RIGHT -> Anchor.bottomRight();
            case DEFAULT -> Anchor.defaultAnchor();
        };
    }
}
