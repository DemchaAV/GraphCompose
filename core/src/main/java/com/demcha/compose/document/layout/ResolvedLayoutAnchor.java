package com.demcha.compose.document.layout;

import java.util.Objects;

/**
 * Where an anchored subtree ended up, in resolved page coordinates.
 *
 * <p>Coordinates are the same space every {@code PlacedFragment} uses: origin at the
 * bottom-left of the page, y growing upwards. A pass reading these needs no conversion to
 * emit a fragment beside them.</p>
 *
 * <p><b>The box is the wrapped node's border box</b>: the box that node was laid out into,
 * <em>its own margin excluded</em> and its padding included. Not its container's — that is
 * what lets a caller treat this as the position of a <em>marker</em> rather than of
 * whatever cell or column happened to hold it. Not its ink either: a stroke may paint
 * outside it and a glyph need not fill it, so a consumer that wants the ink of one
 * particular shape has to ask that shape. And it is the box of the node that was
 * <em>wrapped</em> — anchor a container and the container is what comes back.</p>
 *
 * @param id        the anchor's identity
 * @param pageIndex zero-based page the anchor landed on
 * @param x         left edge, page coordinates
 * @param y         bottom edge, page coordinates
 * @param width     the wrapped node's border-box width
 * @param height    the wrapped node's border-box height
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record ResolvedLayoutAnchor(LayoutAnchorId id, int pageIndex,
                                   double x, double y, double width, double height) {

    /**
     * Validates the resolved anchor.
     *
     * @throws NullPointerException     if {@code id} is null
     * @throws IllegalArgumentException if the page index is negative
     */
    public ResolvedLayoutAnchor {
        Objects.requireNonNull(id, "id");
        if (pageIndex < 0) {
            throw new IllegalArgumentException("Anchor page index must not be negative, was " + pageIndex + ".");
        }
    }

    /**
     * A point inside the anchor's box, given as fractions of its size.
     *
     * <p>{@code (0.5, 0.5)} is the centre; {@code (0.0, 0.5)} the middle of the left edge.
     * A consumer that wants "the rail passes through the marker" asks for the centre; one
     * reproducing an older layout that put its line at a container's edge asks for a
     * fraction plus its own offset. Both are then the same arithmetic, which is what keeps
     * a compatibility case from becoming a second code path.</p>
     *
     * @param relativeX fraction of the width, left to right
     * @return the x coordinate of that point
     */
    public double pointX(double relativeX) {
        return x + width * relativeX;
    }

    /**
     * The y coordinate of {@link #pointX(double)}'s companion point.
     *
     * @param relativeY fraction of the height, bottom to top
     * @return the y coordinate of that point
     */
    public double pointY(double relativeY) {
        return y + height * relativeY;
    }
}
