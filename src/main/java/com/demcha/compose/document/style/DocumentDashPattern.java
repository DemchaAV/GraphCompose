package com.demcha.compose.document.style;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable on/off dash pattern for stroked lines, expressed in points.
 *
 * <p>The segments alternate paint-on and paint-off lengths starting with an
 * on-segment: {@code DocumentDashPattern.of(3, 2)} paints 3pt, skips 2pt, and
 * repeats. {@link #NONE} is the solid (un-dashed) default.</p>
 *
 * <p>Carried by {@link com.demcha.compose.document.node.LineNode} independently
 * of {@link DocumentStroke}, keeping the stroke value a stable two-component
 * record. Honoured by the PDF backend; other backends fall back to a solid
 * stroke.</p>
 *
 * @param segments alternating on/off lengths in points; an empty list means a
 *                 solid stroke
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public record DocumentDashPattern(List<Double> segments) {

    /**
     * Solid (un-dashed) stroke — the default for every line.
     */
    public static final DocumentDashPattern NONE = new DocumentDashPattern(List.of());

    /**
     * Normalizes the segment list to an immutable copy and validates that every
     * segment is finite and strictly positive. A zero on-length is intentionally
     * disallowed: an all-zero dash array throws at PDF render time, and the
     * dotted look is expressed instead as a short on-segment (for example
     * {@code of(1, 4)}).
     */
    public DocumentDashPattern {
        segments = segments == null ? List.of() : List.copyOf(segments);
        for (double segment : segments) {
            if (segment <= 0 || Double.isNaN(segment) || Double.isInfinite(segment)) {
                throw new IllegalArgumentException(
                        "Dash segments must be finite and strictly positive: " + segments);
            }
        }
    }

    /**
     * Creates a dash pattern from alternating on/off lengths in points.
     *
     * @param segments at least one strictly-positive length; the first paints,
     *                 the next is skipped, alternating thereafter
     * @return the dash pattern
     * @throws IllegalArgumentException if no segments are supplied or any is not
     *                                  finite and strictly positive
     */
    public static DocumentDashPattern of(double... segments) {
        if (segments == null || segments.length == 0) {
            throw new IllegalArgumentException("Dash pattern requires at least one segment");
        }
        return new DocumentDashPattern(Arrays.stream(segments).boxed().toList());
    }

    /**
     * Returns whether this pattern is solid (carries no dash segments).
     *
     * @return {@code true} when there are no segments
     */
    public boolean isSolid() {
        return segments.isEmpty();
    }
}
