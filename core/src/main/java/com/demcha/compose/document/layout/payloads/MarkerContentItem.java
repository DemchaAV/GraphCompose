package com.demcha.compose.document.layout.payloads;

import java.util.Objects;

/**
 * One list item under
 * {@link com.demcha.compose.document.layout.ListItemLayout#MARKER_CONTENT},
 * with its geometry resolved.
 *
 * <p>All four x values are relative to the <b>item's own start</b> — the left
 * edge of the box the list gives each row, inside the list's padding — not to
 * the page. Placement adds the item origin exactly once, so the same numbers
 * hold whether the list sits at the page margin, inside a card, or on the second
 * page of a split.</p>
 *
 * <pre>
 *   |&lt;- markerX ->|&lt;- measuredMarkerWidth ->|&lt;- markerGap ->|&lt;- contentWidth ->|
 *   |             •                                          Item text that
 *   |                                                        wraps to here
 *   ^ item start                                             ^ contentX
 * </pre>
 *
 * <p>{@code contentX} is what every visual line of the item shares — the first
 * line, the lines it wraps onto, and the lines that continue on the next page.
 * That is the whole point of the layout: in the legacy one each of those was
 * approximated separately by a run of spaces.</p>
 *
 * @param spec                 the item's authored depth, marker and content
 * @param markerX              where the marker starts; {@code 0} at depth 0, and
 *                             at deeper levels the {@code contentX} of the
 *                             parent row, so a marker lines up with the text of
 *                             the item it hangs under
 * @param measuredMarkerWidth  the marker's measured glyph width — measured, never
 *                             assumed and never derived from character count;
 *                             {@code 0} for a markerless item
 * @param markerGap            the gap actually applied between marker and
 *                             content; the list's {@code markerGap} for an item
 *                             with a marker, and {@code 0} for one without, so a
 *                             markerless row starts flush instead of at an
 *                             unexplained inset
 * @param contentX             {@code markerX + measuredMarkerWidth + markerGap}
 * @param contentWidth         the width the item's text wraps within
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record MarkerContentItem(
        ListItemSpec spec,
        double markerX,
        double measuredMarkerWidth,
        double markerGap,
        double contentX,
        double contentWidth
) {

    /**
     * Validates that the resolved geometry is finite and self-consistent.
     *
     * @throws IllegalArgumentException when any value is not finite, when a
     *                                  width or offset is negative, or when
     *                                  {@code contentX} does not equal
     *                                  {@code markerX + measuredMarkerWidth + markerGap}
     */
    public MarkerContentItem {
        Objects.requireNonNull(spec, "spec");
        requireFinite(markerX, "markerX");
        requireFinite(measuredMarkerWidth, "measuredMarkerWidth");
        requireFinite(markerGap, "markerGap");
        requireFinite(contentX, "contentX");
        requireFinite(contentWidth, "contentWidth");
        // Checked rather than trusted: contentX is the number every line of the
        // item is placed at, so a resolver that computed it a different way from
        // its own parts would misalign the item in a way no width assertion sees.
        double expected = markerX + measuredMarkerWidth + markerGap;
        if (Math.abs(contentX - expected) > 1e-9) {
            throw new IllegalArgumentException(
                    "contentX must be markerX + measuredMarkerWidth + markerGap: "
                    + contentX + " != " + expected);
        }
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }

    /**
     * Returns the item's nesting depth.
     *
     * @return zero-based depth
     */
    public int depth() {
        return spec.depth();
    }

    /**
     * Returns the marker text to draw, without the separator the legacy prefix
     * path appends.
     *
     * @return marker text, empty for a markerless item
     */
    public String markerText() {
        return spec.markerText();
    }

    /**
     * Returns the item's text.
     *
     * @return item content
     */
    public String content() {
        return spec.content();
    }
}
