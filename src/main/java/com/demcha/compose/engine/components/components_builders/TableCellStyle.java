package com.demcha.compose.engine.components.components_builders;

import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Padding;
import lombok.Builder;

import java.awt.*;

@Builder(toBuilder = true)
/**
 * Shared cell styling model for {@link TableBuilder}.
 *
 * <p>All fields are optional in override styles. The builder applies
 * property-wise merge in the order: built-in defaults, table default, column
 * override, row override, then cell override.</p>
 *
 * @author Artem Demchyshyn
 */
public record TableCellStyle(
        Padding padding,
        Color fillColor,
        Stroke stroke,
        TextStyle textStyle,
        Anchor textAnchor,
        Double lineSpacing
) {
    public static final TableCellStyle DEFAULT = TableCellStyle.builder()
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

    public TableCellStyle {
        validatePadding(padding);
        validateStroke(stroke);
        validateTextStyle(textStyle);
        validateLineSpacing(lineSpacing);
    }

    /**
     * Backward-compatible constructor for callers that do not need custom
     * spacing between multiple text lines inside a cell.
     */
    public TableCellStyle(Padding padding,
                          Color fillColor,
                          Stroke stroke,
                          TextStyle textStyle,
                          Anchor textAnchor) {
        this(padding, fillColor, stroke, textStyle, textAnchor, null);
    }

    public static TableCellStyle empty() {
        return TableCellStyle.builder().build();
    }

    public static TableCellStyle merge(TableCellStyle base, TableCellStyle override) {
        if (base == null) {
            throw new IllegalArgumentException("Base style cannot be null.");
        }
        if (override == null) {
            return base;
        }
        return new TableCellStyle(
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
