package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentColor;

import java.util.List;
import java.util.Objects;

/**
 * A fill specification: a flat colour or a gradient. Lives in the chart package
 * for now, but is intended to graduate into
 * {@code com.demcha.compose.document.style} and replace bare {@link DocumentColor}
 * in every fillable surface (shape containers, panel backgrounds, page
 * backgrounds) once gradients land engine-wide. PDFBox renders gradients via
 * axial / radial shadings (PDShadingType2/3); until that work lands every
 * backend (and the v1 chart resolver) renders {@link #primaryColor()}.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public sealed interface DocumentPaint permits DocumentPaint.Solid, DocumentPaint.Linear, DocumentPaint.Radial {

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
     * @param to end colour
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
        /** Validates the colour. */
        public Solid {
            Objects.requireNonNull(color, "color");
        }

        @Override public DocumentColor primaryColor() {
            return color;
        }
    }

    /**
     * Linear gradient.
     *
     * @param stops ordered colour stops, offsets in [0,1]; at least two
     * @param angleDegrees gradient direction, 0 = left→right, 90 = bottom→top
     */
    record Linear(List<Stop> stops, double angleDegrees) implements DocumentPaint {
        /** Copy-protects and validates stops. */
        public Linear {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("linear gradient needs at least two stops");
            }
        }

        @Override public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    /**
     * Radial gradient from a normalized centre outward.
     *
     * @param stops ordered colour stops, offsets in [0,1]; at least two
     * @param cx normalized centre x in [0,1]
     * @param cy normalized centre y in [0,1]
     */
    record Radial(List<Stop> stops, double cx, double cy) implements DocumentPaint {
        /** Copy-protects and validates stops. */
        public Radial {
            Objects.requireNonNull(stops, "stops");
            stops = List.copyOf(stops);
            if (stops.size() < 2) {
                throw new IllegalArgumentException("radial gradient needs at least two stops");
            }
        }

        @Override public DocumentColor primaryColor() {
            return stops.get(0).color();
        }
    }

    /**
     * One gradient colour stop.
     *
     * @param offset position along the gradient axis in [0,1]
     * @param color colour at this offset
     */
    record Stop(double offset, DocumentColor color) {
        /** Validates the offset and colour. */
        public Stop {
            if (offset < 0 || offset > 1 || Double.isNaN(offset)) {
                throw new IllegalArgumentException("stop offset must be in [0,1]: " + offset);
            }
            Objects.requireNonNull(color, "color");
        }
    }
}
