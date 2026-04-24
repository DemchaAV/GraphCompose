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
}

