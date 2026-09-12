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
 * <p><b>One of these per page the node occupies</b>, each the slice of that box lying on
 * its own page. A node that fits on one page has one, and it is the whole box. A node that
 * spans pages has one per page: the first runs from the node's top to the bottom of that
 * page's content band, a middle one is the whole band, and the last runs from the band's
 * top to the node's bottom. They share an id, so a consumer asking for an owner gets the
 * slices in page order and can draw each on its own page without arithmetic of its own.
 * The bands come from the geometry a spanning node's border already uses, per-page margins
 * included.</p>
 *
 * <p>The margin comes off the edges a slice actually contains — the top margin only on the
 * node's first page, the bottom margin only on its last, neither in the middle, both when
 * there is only one. Horizontally every slice is the full box, so the left and right
 * margins always come off.</p>
 *
 * @param id        the anchor's identity, shared by every page's slice
 * @param pageIndex zero-based page this slice lies on
 * @param x         left edge, page coordinates
 * @param y         bottom edge of this page's slice, page coordinates
 * @param width     the wrapped node's border-box width
 * @param height    the height of this page's slice, not of the whole node
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
