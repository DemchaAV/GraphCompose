package com.demcha.compose.document.style;

/**
 * Geometric outline of a shape container. Sealed so layout, render, and
 * snapshot code can pattern-match exhaustively against the supported kinds.
 *
 * <p>Outlines are always axis-aligned and described in their own local
 * coordinate space. Their {@link #width()} / {@link #height()} are the
 * intrinsic outer size; container layout adds {@link com.demcha.compose.document.style.DocumentInsets}
 * around them for padding / margin.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface ShapeOutline permits
        ShapeOutline.Rectangle,
        ShapeOutline.RoundedRectangle,
        ShapeOutline.Ellipse {

    /**
     * @return outline outer width in points
     */
    double width();

    /**
     * @return outline outer height in points
     */
    double height();

    /**
     * Plain axis-aligned rectangle outline.
     *
     * @param width outer width in points
     * @param height outer height in points
     */
    record Rectangle(double width, double height) implements ShapeOutline {
        /**
         * Validates that both dimensions are finite and positive.
         */
        public Rectangle {
            requirePositive("width", width);
            requirePositive("height", height);
        }
    }

    /**
     * Rectangle with a uniform corner radius. Render code may further clamp
     * {@code cornerRadius} to half of the smaller side at draw time.
     *
     * @param width outer width in points
     * @param height outer height in points
     * @param cornerRadius corner radius in points (0 means square corners)
     */
    record RoundedRectangle(double width, double height, double cornerRadius) implements ShapeOutline {
        /**
         * Validates dimensions and that {@code cornerRadius} is non-negative.
         */
        public RoundedRectangle {
            requirePositive("width", width);
            requirePositive("height", height);
            if (cornerRadius < 0 || Double.isNaN(cornerRadius) || Double.isInfinite(cornerRadius)) {
                throw new IllegalArgumentException("cornerRadius must be finite and non-negative: " + cornerRadius);
            }
        }
    }

    /**
     * Ellipse outline. A circle is just an ellipse with {@code width == height}.
     *
     * @param width outer width in points
     * @param height outer height in points
     */
    record Ellipse(double width, double height) implements ShapeOutline {
        /**
         * Validates that both dimensions are finite and positive.
         */
        public Ellipse {
            requirePositive("width", width);
            requirePositive("height", height);
        }
    }

    /**
     * Convenience factory for a circular {@link Ellipse}.
     *
     * @param diameter circle diameter in points
     * @return ellipse with {@code width == height == diameter}
     */
    static Ellipse circle(double diameter) {
        return new Ellipse(diameter, diameter);
    }

    private static void requirePositive(String label, double value) {
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite and positive: " + value);
        }
    }
}
