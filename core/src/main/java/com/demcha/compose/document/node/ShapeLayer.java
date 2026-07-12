package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.Objects;

/**
 * One paint layer of an {@link InlineShapeRun}: a {@link ShapeOutline} figure
 * with its own fill and/or stroke.
 *
 * <p>Layers are drawn overlaid, each centred within the run's bounding box, so
 * composite inline figures are expressed as a stack — a checkbox is a box layer
 * plus an optional checkmark layer, each with its own colour; a single dot or
 * arrow is just one layer.</p>
 *
 * @param outline figure geometry; its {@link ShapeOutline#width()} /
 *                {@link ShapeOutline#height()} size this layer
 * @param fill    optional fill color; {@code null} leaves the interior empty
 * @param stroke  optional outline stroke; {@code null} leaves no border
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public record ShapeLayer(ShapeOutline outline, DocumentColor fill, DocumentStroke stroke) {
    /**
     * Validates the outline and requires at least one visible paint.
     */
    public ShapeLayer {
        Objects.requireNonNull(outline, "outline");
        if (fill == null && stroke == null) {
            throw new IllegalArgumentException("shape layer must have a fill, a stroke, or both");
        }
    }

    /**
     * Creates a filled layer with no stroke.
     *
     * @param outline figure geometry
     * @param fill    fill color; must not be {@code null}
     */
    public ShapeLayer(ShapeOutline outline, DocumentColor fill) {
        this(outline, Objects.requireNonNull(fill, "fill"), null);
    }
}
