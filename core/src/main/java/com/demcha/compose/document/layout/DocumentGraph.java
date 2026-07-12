package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.List;
import java.util.Objects;

/**
 * Semantic authoring tree for the v2 pipeline.
 *
 * @param roots root semantic nodes in authoring order
 */
public record DocumentGraph(List<DocumentNode> roots) {
    /**
     * Creates an immutable semantic document graph.
     */
    public DocumentGraph {
        Objects.requireNonNull(roots, "roots");
        roots = List.copyOf(roots);
    }
}



