package com.demcha.compose.document.layout;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.*;
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
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Internal adapter seam between canonical document values and engine values.
 *
 * <p>Public authoring packages should stay on {@code document.*} types. Layout,
 * pagination, measurement, and rendering code use this adapter at runtime
 * boundaries before talking to the engine's own value types.</p>
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
                textStyle.color().color(),
                // The public value keeps its unit; the engine gets points. This
                // is the only place that knows the font size and the unit at
                // the same time, so it is the only place that can resolve one
                // against the other.
                toFixedLayoutTracking(textStyle.letterSpacing().resolve(textStyle.size())));
    }

    /**
     * The largest tracking a fixed-layout document can carry, in points.
     *
     * <p>Set by DrawingML, the least capacious of the fixed backends: {@code spc}
     * is {@code ST_TextPoint}, whose numeric member is bounded at
     * &plusmn;400000 hundredths. Measured, not read &mdash; the schema validates
     * {@code 400000} and rejects {@code 400001}.</p>
     */
    static final double MAX_FIXED_LAYOUT_TRACKING_POINTS = 4000.0;

    /**
     * Tracking as fixed layout can actually express it: quantised to hundredths
     * of a point.
     *
     * <p>This exists because the engine's measurement and the file's declared
     * spacing have to be the <em>same number</em>, and PPTX can only declare
     * hundredths. Left unquantised, a {@code points(1.0/3.0)} style measured at
     * {@code 0.33333…} per code point while the deck said {@code spc="33"} —
     * {@code 0.33} — so the width the layout reserved, wrapped against, aligned
     * to and sized its frames from was a width the deck would never draw. The
     * residue is small per code point and accumulates with the string: a third
     * of a point is {@code 0.0033} out per code point, {@code 0.13pt} over a
     * forty-character line. Quantising here makes the engine measure the value
     * every fixed backend will actually use, so PDF's {@code Tc} and PPTX's
     * {@code spc} are two spellings of one number.</p>
     *
     * <p>It is done once, here, rather than in each backend: this is the single
     * seam where the public value becomes engine points, so it is the only place
     * that can make the measurement and every renderer agree by construction.
     * The public {@link DocumentTextStyle} is untouched &mdash; it still carries
     * exactly what the author wrote, and {@code DocumentLetterSpacing} still
     * resolves to exactly what the author asked for. The quantisation is a
     * property of fixed layout, not of the value.</p>
     *
     * <p>The semantic DOCX export does not come through here. It resolves the
     * public value itself and rounds to Word's twentieths, which is a coarser
     * grid again &mdash; and correctly so, because Word owns that layout and
     * owes the PDF no coordinate.</p>
     *
     * <p>Out of range is refused rather than clamped or wrapped. {@code spc} is
     * written as an {@code int} of hundredths, and a large enough value silently
     * changes sign on the cast &mdash; {@code 2.2e7} points becomes
     * {@code -2094967296}, turning wide tracking into tight. A document asking
     * for more than the format can hold is a mistake worth hearing about.</p>
     *
     * @param points resolved tracking in points
     * @return the same tracking on the grid fixed layout can express
     * @throws IllegalArgumentException if the tracking exceeds
     *         {@link #MAX_FIXED_LAYOUT_TRACKING_POINTS}
     */
    private static double toFixedLayoutTracking(double points) {
        if (points == 0.0) {
            // Short-circuited so an untracked style keeps the identical double,
            // and never depends on the rounding below behaving at zero.
            return 0.0;
        }
        if (Math.abs(points) > MAX_FIXED_LAYOUT_TRACKING_POINTS) {
            throw new IllegalArgumentException(
                    "Letter spacing resolves to " + points + "pt, beyond the "
                            + MAX_FIXED_LAYOUT_TRACKING_POINTS
                            + "pt a fixed-layout document can express (DrawingML spc is "
                            + "hundredths of a point, bounded at +/-400000).");
        }
        return Math.round(points * 100.0) / 100.0;
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
                .direction(toBaseDirection(style.direction()))
                .build();
    }

    /**
     * Carries the declared direction across as a question, not an answer.
     *
     * <p>{@link TextDirection#AUTO} maps to
     * {@link BidiParagraphResolver.BaseDirection#FIRST_STRONG_CHARACTER} rather than being
     * resolved here, because the text it has to be read off is the cell's, and a style
     * override is merged before anyone knows which cell it landed on. The layout answers
     * it once the two have met.</p>
     */
    private static BidiParagraphResolver.BaseDirection toBaseDirection(TextDirection direction) {
        if (direction == null) {
            return null;
        }
        return switch (direction) {
            case LTR -> BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT;
            case RTL -> BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT;
            case AUTO -> BidiParagraphResolver.BaseDirection.FIRST_STRONG_CHARACTER;
        };
    }

    static Map<Integer, TableCellLayoutStyle> toTableStyles(Map<Integer, DocumentTableStyle> styles) {
        return styles.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> toTableStyle(entry.getValue())));
    }

    static TableCellContent toTableCell(DocumentTableCell cell) {
        Objects.requireNonNull(cell, "cell");
        TableCellLayoutStyle style = cell.style() == null ? null : toTableStyle(cell.style());
        return new TableCellContent(cell.lines(), style, cell.colSpan());
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
