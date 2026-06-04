package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.Objects;

/**
 * One inline shape run inside a {@link ParagraphNode} — a geometric figure
 * (circle / ellipse, rectangle, rounded rectangle, diamond, triangle, star, or
 * any {@link ShapeOutline}) measured and rendered on the surrounding text
 * baseline.
 *
 * <p>Inline shapes are measured as part of paragraph wrapping exactly like
 * {@link InlineImageRun}: the outline's width and height contribute to span
 * placement, line breaking and per-line height, and the figure shares the text
 * baseline. The shape is drawn directly from geometry — no raster payload and
 * no font glyph — so skill rating dots ({@code Java ●●●●○}), custom bullets and
 * inline status markers render regardless of font coverage. The kind is the
 * existing {@code ShapeOutline} taxonomy, so any new outline kind is usable
 * inline automatically.</p>
 *
 * <p>At least one of {@code fill} or {@code stroke} must be present, otherwise
 * the run would be invisible: a filled figure uses {@code fill} with a
 * {@code null} stroke; an outlined figure uses a {@code null} fill with a
 * {@code stroke}; the two combined paint a filled-and-outlined figure.</p>
 *
 * @param outline shape geometry; its {@link ShapeOutline#width()} and
 *                {@link ShapeOutline#height()} are the run's measured size
 * @param fill optional fill color; {@code null} leaves the interior empty
 * @param stroke optional outline stroke; {@code null} leaves the figure
 *               without a border
 * @param alignment vertical alignment relative to the surrounding text;
 *                  defaults to {@link InlineImageAlignment#CENTER}
 * @param baselineOffset extra vertical offset in points applied after
 *                       {@code alignment} resolution; positive values move the
 *                       figure up
 * @param linkOptions optional per-run link metadata
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public record InlineShapeRun(
        ShapeOutline outline,
        DocumentColor fill,
        DocumentStroke stroke,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements InlineRun {
    /**
     * Validates the outline, requires at least one visible paint, and
     * normalizes alignment defaults.
     */
    public InlineShapeRun {
        Objects.requireNonNull(outline, "outline");
        if (Double.isNaN(baselineOffset) || Double.isInfinite(baselineOffset)) {
            throw new IllegalArgumentException("inline shape baselineOffset must be finite: " + baselineOffset);
        }
        if (fill == null && stroke == null) {
            throw new IllegalArgumentException("inline shape must have a fill, a stroke, or both");
        }
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    /**
     * Convenience constructor for a filled shape with default
     * {@link InlineImageAlignment#CENTER} alignment and zero offset.
     *
     * @param outline shape geometry
     * @param fill fill color; must not be {@code null}
     */
    public InlineShapeRun(ShapeOutline outline, DocumentColor fill) {
        this(outline, Objects.requireNonNull(fill, "fill"), null, InlineImageAlignment.CENTER, 0.0, null);
    }
}
