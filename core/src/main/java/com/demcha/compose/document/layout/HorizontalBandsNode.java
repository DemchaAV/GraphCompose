package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;

import java.util.List;
import java.util.Objects;

/**
 * Wraps a row so the columns it resolves can be read by content laid out after it.
 *
 * <p>The wrapper is transparent: it measures to its child and adds no spacing, so putting
 * one around a row changes no geometry. What it adds is a publication — when the row is
 * compiled, each of its slots is recorded under {@link #key} as a
 * {@link ResolvedHorizontalBand}, and a {@link HorizontalBandContentNode} naming the same
 * key can lay itself out inside one of them.</p>
 *
 * <p>The key is compared by <b>identity</b>, never by equality. Two features that happen to
 * describe their columns alike are still two features, and a band belongs to whichever
 * object published it; there is no name, path or index that a second feature could collide
 * with by accident.</p>
 *
 * <p>The child has to be a row, because a row is what has columns. Wrapping anything else
 * would publish nothing and leave the consumer to fail later with a puzzle instead of a
 * mistake, so it is rejected here.</p>
 *
 * <p>Lives in this {@code @Internal} package on purpose: this is engine plumbing that a
 * built-in feature uses to reach geometry the engine already resolved, and one built-in use
 * case is not enough to stabilise an authoring API.</p>
 *
 * @param name  semantic name, may be empty
 * @param key   the identity the bands are published under
 * @param child the row whose columns are published
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record HorizontalBandsNode(String name, Object key, DocumentNode child) implements DocumentNode {

    /**
     * Normalizes the name and validates the rest.
     *
     * @throws NullPointerException     if {@code key} or {@code child} is null
     * @throws IllegalArgumentException if {@code child} is not a row
     */
    public HorizontalBandsNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(child, "child");
        if (!(child instanceof RowNode)) {
            throw new IllegalArgumentException(
                    "A horizontal-bands wrapper publishes a row's columns, and " + child.nodeKind()
                    + " has none. Wrap the row itself.");
        }
    }

    /**
     * The single wrapped row.
     *
     * @return one child
     */
    @Override
    public List<DocumentNode> children() {
        return List.of(child);
    }
}
