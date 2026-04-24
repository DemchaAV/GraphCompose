package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

/**
 * Explicit page-break control node for the semantic document graph.
 *
 * <p>This node does not emit visible fragments. Instead, the layout compiler
 * uses it as a control signal that forces subsequent content onto the next
 * page.</p>
 *
 * @param name semantic name used in snapshots and diagnostics
 * @param margin optional surrounding spacing; {@code null} resolves to zero
 */
public record PageBreakNode(String name, DocumentInsets margin) implements DocumentNode {

    /**
     * Creates a page-break node.
     *
     * @param name semantic name used in snapshots and diagnostics
     * @param margin optional surrounding spacing; {@code null} resolves to zero
     */
    public PageBreakNode {
        name = name == null ? "" : name;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }
}
