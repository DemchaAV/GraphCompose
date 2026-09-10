package com.demcha.compose.document.dsl;

/**
 * The box a marker declares for itself, whatever it draws inside it.
 *
 * <p>One box per marker, and it is a declaration rather than a measurement. A marker drawn
 * as three stacked shapes has one box; a marker drawn as one ellipse has one box; the rail
 * that later anchors on it cannot tell the two apart, which is the point. Reading the box
 * back off whatever the marker happened to draw would make the rail's position depend on
 * the marker's construction.</p>
 *
 * <p>Width and height separately, not one {@code size}: the four built-in markers are all
 * square, but a custom one — a chevron, a date pill, an icon — need not be.</p>
 *
 * @param width  declared width in points
 * @param height declared height in points
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineMarkerBounds(double width, double height) {

    /**
     * Validates the box.
     *
     * @throws IllegalArgumentException if either dimension is not positive and finite
     */
    TimelineMarkerBounds {
        require(width, "width");
        require(height, "height");
    }

    /** A square box, which is what every built-in marker declares. */
    static TimelineMarkerBounds square(double size) {
        return new TimelineMarkerBounds(size, size);
    }

    private static void require(double value, String name) {
        if (!(value > 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    "A timeline marker's " + name + " must be a positive finite number of points, got: " + value);
        }
    }
}
