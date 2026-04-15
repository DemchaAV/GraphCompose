package com.demcha.compose.v2;

import java.util.List;
import java.util.Objects;

/**
 * Semantic authoring tree for the v2 pipeline.
 */
public record DocumentGraph(List<DocumentNode> roots) {
    public DocumentGraph {
        Objects.requireNonNull(roots, "roots");
        roots = List.copyOf(roots);
    }
}
