package com.demcha.compose.document.style;


import java.util.List;
import java.util.Objects;

/**
 * A fill specification: a flat colour or a multi-stop gradient. This is the
 * single paint vocabulary every fillable surface shares — chart palettes
 * today, shape and panel fills as they adopt the {@code fillPaint} component.
 *
 * <p>Backend contract: the PDF backend renders {@link Linear} and
 * {@link Radial} as native axial / radial shadings; a backend (or surface)
 * that cannot paint a gradient degrades to {@link #primaryColor()} — the
 * first stop — so authoring code never branches per backend.</p>
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
     * One gradient colour stop.
     *
     * @param offset position along the gradient axis in [0,1]
     * @param color  colour at this offset
     */
    record Stop(double offset, DocumentColor color) {
        /**
         * Validates the offset and colour.
         */
        public Stop {
            if (offset < 0 || offset > 1 || Double.isNaN(offset)) {
                throw new IllegalArgumentException("stop offset must be in [0,1]: " + offset);
            }
            Objects.requireNonNull(color, "color");
        }
    }
}
