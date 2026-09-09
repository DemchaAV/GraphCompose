package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.List;
import java.util.Objects;

/**
 * Wraps one node so the finished layout reports where it landed.
 *
 * <p>The wrapper is transparent: it measures to exactly its child's size and adds no
 * spacing, so inserting one changes no geometry. What it adds is a single non-visual
 * fragment carrying {@link com.demcha.compose.document.layout.payloads.LayoutAnchorPayload},
 * which a post-layout pass can find by identity.</p>
 *
 * <p>Measuring to the child rather than to the available width is not a detail. It is what
 * makes the anchor's box the child's box, so a caller reading the anchor learns where the
 * <em>marker</em> is, not where its container is.</p>
 *
 * <p>Lives in this {@code @Internal} package on purpose. Anchoring is engine plumbing that
 * a built-in feature uses to reach its own resolved geometry; whether authors should ever
 * declare an anchor themselves is a separate question, and answering it by accident here
 * would stabilise a public API on the strength of one built-in use case.</p>
 *
 * @param name  semantic name, may be empty
 * @param id    the anchor's identity
 * @param child the node whose resolved geometry is being reported
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record LayoutAnchorNode(String name, LayoutAnchorId id, DocumentNode child) implements DocumentNode {

    /**
     * Normalizes the name and validates the rest.
     *
     * @throws NullPointerException if {@code id} or {@code child} is null
     */
    public LayoutAnchorNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(child, "child");
    }

    /**
     * The single anchored child.
     *
     * @return one child
     */
    @Override
    public List<DocumentNode> children() {
        return List.of(child);
    }
}
