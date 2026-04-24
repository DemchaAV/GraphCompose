package com.demcha.compose.engine.components.content.table;

import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Padding;
import lombok.Builder;

import java.awt.Color;

/**
 * Internal table cell layout style for V2 table measurement and rendering.
 *
 * <p>All fields are optional in override styles. The engine applies
 * property-wise merge in the order: built-in defaults, table default, column
 * override, row override, then cell override.</p>
 *
 * @author Artem Demchyshyn
 */
@Builder(toBuilder = true)
public record TableCellLayoutStyle(
        Padding padding,
        Color fillColor,
        Stroke stroke,
        TextStyle textStyle,
        Anchor textAnchor,
        Double lineSpacing
) {
    public static final TableCellLayoutStyle DEFAULT = TableCellLayoutStyle.builder()
            .padding(Padding.of(4))
            .fillColor(ComponentColor.WHITE)
            .stroke(new Stroke(ComponentColor.BLACK, 1.0))
            .textStyle(TextStyle.builder()
                    .fontName(TextStyle.DEFAULT_STYLE.fontName())
                    .size(TextStyle.DEFAULT_STYLE.size())
                    .decoration(TextStyle.DEFAULT_STYLE.decoration())
                    .color(TextStyle.DEFAULT_STYLE.color())
                    .build())
            .textAnchor(Anchor.centerLeft())
            .lineSpacing(0.0)
            .build();

    public TableCellLayoutStyle {
        validatePadding(padding);
        validateStroke(stroke);
        validateTextStyle(textStyle);
        validateLineSpacing(lineSpacing);
    }

    /**
     * Constructor for callers that do not need custom spacing between multiple
     * text lines inside a cell.
     */
    public TableCellLayoutStyle(Padding padding,
                                Color fillColor,
                                Stroke stroke,
                                TextStyle textStyle,
                                Anchor textAnchor) {
        this(padding, fillColor, stroke, textStyle, textAnchor, null);
    }

    /**
     * Creates an empty override style.
     *
     * @return empty style override
     */
    public static TableCellLayoutStyle empty() {
        return TableCellLayoutStyle.builder().build();
    }

    /**
     * Merges an override into a fully resolved base style.
     *
     * @param base fully resolved base style
     * @param override optional override
     * @return merged table cell style
     */
    public static TableCellLayoutStyle merge(TableCellLayoutStyle base, TableCellLayoutStyle override) {
        if (base == null) {
            throw new IllegalArgumentException("Base style cannot be null.");
        }
        if (override == null) {
            return base;
        }
        return new TableCellLayoutStyle(
                override.padding != null ? override.padding : base.padding,
                override.fillColor != null ? override.fillColor : base.fillColor,
                override.stroke != null ? override.stroke : base.stroke,
                override.textStyle != null ? override.textStyle : base.textStyle,
                override.textAnchor != null ? override.textAnchor : base.textAnchor,
                override.lineSpacing != null ? override.lineSpacing : base.lineSpacing
        );
    }

    private static void validatePadding(Padding padding) {
        if (padding == null) {
            return;
        }
        if (padding.top() < 0 || padding.right() < 0 || padding.bottom() < 0 || padding.left() < 0) {
            throw new IllegalArgumentException("Cell padding cannot be negative.");
        }
    }

    private static void validateStroke(Stroke stroke) {
        if (stroke == null) {
            return;
        }
        if (stroke.width() < 0) {
            throw new IllegalArgumentException("Cell stroke width cannot be negative.");
        }
    }

    private static void validateTextStyle(TextStyle textStyle) {
        if (textStyle == null) {
            return;
        }
        if (textStyle.size() <= 0) {
            throw new IllegalArgumentException("Cell text size must be greater than 0.");
        }
    }

    private static void validateLineSpacing(Double lineSpacing) {
        if (lineSpacing == null) {
            return;
        }
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("Cell line spacing must be finite and non-negative.");
        }
    }
}
