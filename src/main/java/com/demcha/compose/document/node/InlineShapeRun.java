package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One inline shape run inside a {@link ParagraphNode} — a stack of geometric
 * {@link ShapeLayer}s (circle / ellipse, rectangle, rounded rectangle, diamond,
 * triangle, star, arrow, chevron, checkmark, plus, polygon, …) measured and
 * rendered on the surrounding text baseline.
 *
 * <p>Inline shapes are measured as part of paragraph wrapping exactly like
 * {@link InlineImageRun}: the run's bounding {@link #width()} / {@link #height()}
 * (the widest / tallest layer) contribute to span placement, line breaking and
 * per-line height, and the figure shares the text baseline. Each layer is drawn
 * directly from geometry — no raster payload and no font glyph — so skill rating
 * dots, custom bullets, arrows between text and checkboxes render regardless of
 * font coverage.</p>
 *
 * <p>Most figures are a single layer (a dot, an arrow). Composite figures stack
 * layers overlaid and centred: a checkbox is a box layer plus an optional
 * checkmark layer, each with its own colour.</p>
 *
 * @param layers         one or more paint layers, drawn back-to-front and centred in the
 *                       run's bounding box
 * @param alignment      vertical alignment relative to the surrounding text;
 *                       defaults to {@link InlineImageAlignment#CENTER}
 * @param baselineOffset extra vertical offset in points applied after
 *                       {@code alignment} resolution; positive values move the
 *                       figure up
 * @param linkOptions    optional per-run link metadata
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public record InlineShapeRun(
        List<ShapeLayer> layers,
        InlineImageAlignment alignment,
        double baselineOffset,
        DocumentLinkOptions linkOptions
) implements InlineRun {
    /**
     * Copies the layer stack defensively, requires at least one layer, and
     * normalizes alignment defaults.
     */
    public InlineShapeRun {
        Objects.requireNonNull(layers, "layers");
        layers = List.copyOf(layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("inline shape needs at least one layer");
        }
        if (Double.isNaN(baselineOffset) || Double.isInfinite(baselineOffset)) {
            throw new IllegalArgumentException("inline shape baselineOffset must be finite: " + baselineOffset);
        }
        alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
    }

    /**
     * Single-layer convenience constructor.
     *
     * @param outline        figure geometry
     * @param fill           optional fill color
     * @param stroke         optional outline stroke
     * @param alignment      vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions    optional inline link metadata
     */
    public InlineShapeRun(ShapeOutline outline,
                          DocumentColor fill,
                          DocumentStroke stroke,
                          InlineImageAlignment alignment,
                          double baselineOffset,
                          DocumentLinkOptions linkOptions) {
        this(List.of(new ShapeLayer(outline, fill, stroke)), alignment, baselineOffset, linkOptions);
    }

    /**
     * Single filled-layer convenience constructor with default
     * {@link InlineImageAlignment#CENTER} alignment and zero offset.
     *
     * @param outline figure geometry
     * @param fill    fill color; must not be {@code null}
     */
    public InlineShapeRun(ShapeOutline outline, DocumentColor fill) {
        this(outline, Objects.requireNonNull(fill, "fill"), null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Returns the bounding width of the run — the widest layer.
     *
     * @return bounding width in points
     */
    public double width() {
        double max = 0.0;
        for (ShapeLayer layer : layers) {
            max = Math.max(max, layer.outline().width());
        }
        return max;
    }

    /**
     * Returns the bounding height of the run — the tallest layer.
     *
     * @return bounding height in points
     */
    public double height() {
        double max = 0.0;
        for (ShapeLayer layer : layers) {
            max = Math.max(max, layer.outline().height());
        }
        return max;
    }

    /**
     * Creates an inline checkbox with the default
     * {@link ShapeOutline.CheckmarkStyle#CLASSIC} tick — a rounded square frame
     * with an optional centred checkmark inside (the checked state), each in its
     * own colour. The frame is stroke-only; the checkmark, when present, is a
     * smaller filled figure centred inside the frame.
     *
     * @param size       box width and height in points
     * @param checked    whether the checkmark is shown
     * @param boxColor   frame stroke color
     * @param checkColor checkmark fill color
     * @return checkbox shape run
     */
    public static InlineShapeRun checkbox(double size,
                                          boolean checked,
                                          DocumentColor boxColor,
                                          DocumentColor checkColor) {
        return checkbox(size, checked, ShapeOutline.CheckmarkStyle.CLASSIC, boxColor, checkColor);
    }

    /**
     * Creates an inline checkbox whose checked-state tick uses the given
     * {@link ShapeOutline.CheckmarkStyle} — the "pick your tick" overload. The
     * mark is sized to fit the frame automatically; an unchecked box ignores the
     * style and renders the frame alone.
     *
     * @param size       box width and height in points
     * @param checked    whether the checkmark is shown
     * @param markStyle  design of the checked-state tick
     * @param boxColor   frame stroke color
     * @param checkColor checkmark fill color
     * @return checkbox shape run
     * @since 1.7.0
     */
    public static InlineShapeRun checkbox(double size,
                                          boolean checked,
                                          ShapeOutline.CheckmarkStyle markStyle,
                                          DocumentColor boxColor,
                                          DocumentColor checkColor) {
        Objects.requireNonNull(markStyle, "markStyle");
        double inner = size * 0.62;
        ShapeOutline mark = checked ? ShapeOutline.checkmark(inner, inner, markStyle) : null;
        return checkbox(size, checked, mark, boxColor, checkColor);
    }

    /**
     * Creates an inline checkbox whose checked-state mark is an arbitrary
     * {@link ShapeOutline} — the power-user overload for any glyph (a custom
     * tick, a dash, a cross, …). The mark is drawn centred in the frame at its
     * own size, so size it to fit (≈ {@code 0.6 × size}); an unchecked box
     * renders the frame alone and the {@code mark} is ignored.
     *
     * @param size       box width and height in points
     * @param checked    whether the mark is shown
     * @param mark       checked-state mark geometry, already sized; must be non-null
     *                   when {@code checked} is {@code true}
     * @param boxColor   frame stroke color
     * @param checkColor mark fill color
     * @return checkbox shape run
     * @since 1.7.0
     */
    public static InlineShapeRun checkbox(double size,
                                          boolean checked,
                                          ShapeOutline mark,
                                          DocumentColor boxColor,
                                          DocumentColor checkColor) {
        DocumentStroke frame = DocumentStroke.of(boxColor, Math.max(0.5, size * 0.09));
        List<ShapeLayer> layers = new ArrayList<>(2);
        layers.add(new ShapeLayer(new ShapeOutline.RoundedRectangle(size, size, size * 0.18), null, frame));
        if (checked) {
            Objects.requireNonNull(mark, "mark");
            layers.add(new ShapeLayer(mark, checkColor));
        }
        return new InlineShapeRun(layers, InlineImageAlignment.CENTER, 0.0, null);
    }
}
