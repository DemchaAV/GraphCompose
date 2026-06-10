package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;

/**
 * Semantic authoring node in the GraphCompose document graph.
 *
 * <p>Implementations are immutable semantic values that describe author intent
 * independently from layout or backend concerns. Measurement, splitting, and
 * fragment emission are delegated to registered layout definitions rather than
 * encoded in the node itself.</p>
 */
public interface DocumentNode {

    /**
     * Optional semantic name used for snapshots and diagnostics.
     *
     * @return human-readable semantic name, or an empty string when unnamed
     */
    String name();

    /**
     * Outer spacing around the node.
     *
     * @return outer margin contribution
     */
    default DocumentInsets margin() {
        return DocumentInsets.zero();
    }

    /**
     * Inner spacing inside the node box.
     *
     * @return inner padding contribution
     */
    default DocumentInsets padding() {
        return DocumentInsets.zero();
    }

    /**
     * Child semantic nodes. Leaf nodes return an empty list.
     *
     * @return immutable child node list
     */
    default List<DocumentNode> children() {
        return List.of();
    }

    /**
     * Stable logical kind for diagnostics and snapshots.
     *
     * @return logical node kind
     */
    default String nodeKind() {
        return getClass().getSimpleName();
    }

    /**
     * Whether this node must paginate as a single unit — when it does not fit in
     * the remaining page space but would fit on a fresh page, the compiler
     * relocates it whole to the next page instead of flowing its children across
     * the boundary. Default {@code false} (normal flow). Nodes taller than a full
     * page always flow regardless of this flag.
     *
     * @return true to keep the node together on one page when possible
     * @since 1.8.0
     */
    default boolean keepTogether() {
        return false;
    }
}

