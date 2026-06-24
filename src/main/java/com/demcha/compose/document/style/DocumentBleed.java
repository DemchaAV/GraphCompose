package com.demcha.compose.document.style;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Declares the page edges on which a node bleeds: instead of being placed inside
 * the page content margin, a bled edge extends to the trimmed physical page edge.
 *
 * <p>This is the content-side twin of
 * {@link com.demcha.compose.document.api.PageBackgroundFill}: where a background
 * fill paints a coloured area to the page edge, a bled node draws live content
 * (a band, a rule, an image) to the same edge. It replaces the hand-computed
 * negative-margin idiom ({@code margin(new DocumentInsets(-pageMargin, ...))})
 * with an intent-revealing declaration the engine resolves against the active
 * page margin.</p>
 *
 * <p>Horizontal bleed ({@link DocumentEdge#LEFT}/{@link DocumentEdge#RIGHT})
 * widens the node to reach the side edges. Vertical bleed
 * ({@link DocumentEdge#TOP}/{@link DocumentEdge#BOTTOM}) extends the node toward
 * the top/bottom edge and is meaningful for a node already seated against that
 * edge (e.g. a masthead band at the top of the first page). Instances are
 * immutable and thread-safe.</p>
 *
 * @param edges the set of edges to bleed; never {@code null}
 * @author Artem Demchyshyn
 * @see DocumentEdge
 * @since 1.9.0
 */
public record DocumentBleed(Set<DocumentEdge> edges) {

    private static final DocumentBleed NONE = new DocumentBleed(Set.of());

    /**
     * Normalizes the edge set into an immutable copy.
     *
     * @param edges requested edges; {@code null} is treated as no edges
     */
    public DocumentBleed {
        edges = edges == null || edges.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(edges));
    }

    /**
     * Returns a bleed that touches no edge — i.e. normal in-margin placement.
     *
     * @return the empty bleed
     */
    public static DocumentBleed none() {
        return NONE;
    }

    /**
     * Returns a bleed on all four edges (full-bleed).
     *
     * @return a bleed covering every edge
     */
    public static DocumentBleed all() {
        return new DocumentBleed(EnumSet.allOf(DocumentEdge.class));
    }

    /**
     * Returns a bleed on the given edges.
     *
     * @param edges the edges to bleed; an empty argument list yields {@link #none()}
     * @return a bleed on the requested edges
     */
    public static DocumentBleed of(DocumentEdge... edges) {
        if (edges == null || edges.length == 0) {
            return NONE;
        }
        EnumSet<DocumentEdge> set = EnumSet.noneOf(DocumentEdge.class);
        for (DocumentEdge edge : edges) {
            if (edge != null) {
                set.add(edge);
            }
        }
        return set.isEmpty() ? NONE : new DocumentBleed(set);
    }

    /**
     * Returns whether this bleed touches the given edge.
     *
     * @param edge edge to test
     * @return {@code true} if the node bleeds on {@code edge}
     */
    public boolean bleeds(DocumentEdge edge) {
        return edges.contains(edge);
    }

    /**
     * Returns whether any edge bleeds. The compiler uses this as a fast path to
     * keep non-bleeding nodes byte-identical.
     *
     * @return {@code true} if at least one edge bleeds
     */
    public boolean any() {
        return !edges.isEmpty();
    }
}
