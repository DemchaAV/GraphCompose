package com.demcha.compose.document.style;

import com.demcha.compose.document.api.Beta;

import java.util.List;
import java.util.Objects;

/**
 * A fill specification: a flat colour or a multi-stop gradient. This is the
 * single paint vocabulary every fillable surface shares — chart palettes
 * today, shape and panel fills as they adopt the {@code fillPaint} component.
 *
 * <p>Backend contract: the PDF backend renders all gradient forms as native
 * axial / radial shadings; a backend (or surface) that cannot paint a gradient
 * degrades to {@link #primaryColor()} — the first stop — so authoring code
 * never branches per backend.</p>
 *
 * <p>Two ways to spell each gradient coexist by design, not by redundancy.
 * {@link Linear} / {@link Radial} take an ergonomic angle / corner-reaching
 * geometry and are the chart- and shape-authoring forms; {@link LinearAxis} /
 * {@link RadialCircle} take exact endpoints / a radius in the unit box and are
 * the forms the SVG reader emits to reproduce a source gradient verbatim. All
 * four render through the same shading machinery and degrade to the same
 * {@link #primaryColor()}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public sealed interface DocumentPaint
        permits DocumentPaint.Solid, DocumentPaint.Linear, DocumentPaint.Radial,
                DocumentPaint.LinearAxis, DocumentPaint.RadialCircle {

    /**
     * Representative colour for backends that cannot render gradients.
     *
     * @return the primary (first-stop) colour
     */
    DocumentColor primaryColor();

    /**
     * Flat fill.
     *
     * @param color fill colour
     * @return solid paint
     */
    static DocumentPaint solid(DocumentColor color) {
        return new Solid(color);
    }

    /**
     * Two-stop linear gradient along a normalized angle (0 = left→right).
     *
     * @param from start colour
     * @param to   end colour
     * @return linear paint
     */
    static DocumentPaint linear(DocumentColor from, DocumentColor to) {
        return new Linear(List.of(new Stop(0.0, from), new Stop(1.0, to)), 0.0);
    }

    /**
     * Flat fill.
     *
     * @param color fill colour
     */
    record Solid(DocumentColor color) implements DocumentPaint {
        /**
         * Validates the colour.
         */
        public Solid {
            Objects.requireNonNull(color, "color");
        }

        @Override
        public DocumentColor primaryColor() {
            return color;
        }
    }

    /**
     * Linear gradient.
     *
     * @param stops        ordered colour stops, offsets in [0,1]; at least two
     * @param angleDegrees gradient direction, 0 = left→right, 90 = bottom→top
     */
    record Linear(List<Stop> stops, double angleDegrees) implements DocumentPaint {
        /**
         * Copy-protects and validates stops.
         */
        public Linear {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("linear gradient needs at least two stops");
            }
        }

        @Override
        public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    /**
     * Radial gradient from a normalized centre outward.
     *
     * @param stops ordered colour stops, offsets in [0,1]; at least two
     * @param cx    normalized centre x in [0,1]
     * @param cy    normalized centre y in [0,1]
     */
    record Radial(List<Stop> stops, double cx, double cy) implements DocumentPaint {
        /**
         * Copy-protects and validates stops.
         */
        public Radial {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("radial gradient needs at least two stops");
            }
        }

        @Override
        public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    /**
     * Linear gradient along an explicit axis. Endpoints are normalized to the
     * painted box ({@code 0,0} = bottom-left, {@code 1,1} = top-right, y up)
     * and may lie outside {@code [0,1]} — an SVG gradient whose axis starts
     * beyond a small shape's bounds maps here verbatim. The axis extent is
     * exact: colour runs from the first stop at {@code (x0, y0)} to the last
     * stop at {@code (x1, y1)} and clamps beyond (pad spread).
     *
     * <p>Marked {@link Beta} — emitted by the beta SVG gradient reader; the
     * endpoint normalization contract may shift while that reader hardens.</p>
     *
     * @param stops ordered colour stops, offsets in [0,1]; at least two
     * @param x0    axis start x, normalized to the box width
     * @param y0    axis start y, normalized to the box height
     * @param x1    axis end x, normalized to the box width
     * @param y1    axis end y, normalized to the box height
     * @since 1.8.0
     */
    @Beta
    record LinearAxis(List<Stop> stops, double x0, double y0, double x1, double y1)
            implements DocumentPaint {
        /**
         * Copy-protects stops and validates the axis.
         */
        public LinearAxis {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("linear gradient needs at least two stops");
            }
            requireFinite(x0, "x0");
            requireFinite(y0, "y0");
            requireFinite(x1, "x1");
            requireFinite(y1, "y1");
            if (x0 == x1 && y0 == y1) {
                throw new IllegalArgumentException(
                        "gradient axis is degenerate: both endpoints are (" + x0 + ", " + y0 + ")");
            }
        }

        @Override
        public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    /**
     * Radial gradient with an explicit radius. Centre coordinates are
     * normalized to the painted box (y up, may lie outside {@code [0,1]});
     * the radius is a fraction of the box <em>width</em>, so a circle stays
     * a circle when the box preserves the source's aspect ratio (the SVG
     * icon frame contract). Colour clamps beyond the last stop (pad spread).
     *
     * <p>Marked {@link Beta} — emitted by the beta SVG gradient reader; the
     * centre/radius normalization contract may shift while that reader hardens.</p>
     *
     * @param stops ordered colour stops, offsets in [0,1]; at least two
     * @param cx    centre x, normalized to the box width
     * @param cy    centre y, normalized to the box height
     * @param r     radius as a fraction of the box width; positive
     * @since 1.8.0
     */
    @Beta
    record RadialCircle(List<Stop> stops, double cx, double cy, double r)
            implements DocumentPaint {
        /**
         * Copy-protects stops and validates the circle.
         */
        public RadialCircle {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("radial gradient needs at least two stops");
            }
            requireFinite(cx, "cx");
            requireFinite(cy, "cy");
            requireFinite(r, "r");
            if (r <= 0) {
                throw new IllegalArgumentException("radial gradient radius must be positive: " + r);
            }
        }

        @Override
        public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    private static void requireFinite(double value, String what) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(what + " must be finite: " + value);
        }
    }

    /**
     * One gradient colour stop.
     *
     * <p>The colour must be fully opaque. Gradients render through PDF axial /
     * radial shadings, which carry no alpha channel, so a translucent stop
     * would silently render opaque. Rather than render wrong, a stop with
     * alpha below 255 is rejected at construction — flatten transparency into
     * the stop colour, or apply opacity to the whole shape instead. This
     * mirrors the SVG reader, which already refuses {@code stop-opacity}.
     *
     * @param offset position along the gradient axis in [0,1]
     * @param color  fully-opaque colour at this offset
     */
    record Stop(double offset, DocumentColor color) {
        /**
         * Validates the offset and the opaque-colour requirement.
         *
         * @throws IllegalArgumentException if {@code offset} is outside [0,1]
         *                                  or {@code color} is translucent
         */
        public Stop {
            if (offset < 0 || offset > 1 || Double.isNaN(offset)) {
                throw new IllegalArgumentException("stop offset must be in [0,1]: " + offset);
            }
            Objects.requireNonNull(color, "color");
            int alpha = color.color().getAlpha();
            if (alpha != 255) {
                throw new IllegalArgumentException(
                        "gradient stop colour must be opaque (alpha=" + alpha + "); shadings "
                                + "carry no alpha, so a translucent stop would render opaque — "
                                + "flatten transparency into the colour or apply opacity to the shape");
            }
        }
    }
}
