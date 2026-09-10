package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.List;
import java.util.Objects;

/**
 * Lays its child out inside a column another node already resolved.
 *
 * <p>The child is measured and paginated at the band's width, at the band's x, exactly as it
 * would be in the column itself — but it stays where it is in the vertical flow, so it
 * splits across pages the way any other block does. That is the whole point: a row cannot
 * cross a page, so content that has to line up with a column and also has to be long cannot
 * live in the row.</p>
 *
 * <p>The band has to have been published before this node is reached, by a
 * {@link HorizontalBandsNode} carrying the same key by identity — in practice an earlier
 * sibling. Everything else fails closed: an unknown key, a slot the row does not have, or a
 * slot resolved to nothing throws rather than falling back to the parent's width, because a
 * silent fallback is a layout that looks deliberate and is not.</p>
 *
 * <p>Transparent otherwise: it measures to its child and adds no spacing of its own.</p>
 *
 * @param name  semantic name, may be empty
 * @param key   the identity the band was published under
 * @param slot  which of that row's columns, counting from zero
 * @param child the content to lay out inside the band
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record HorizontalBandContentNode(String name, Object key, int slot, DocumentNode child)
        implements DocumentNode {

    /**
     * Normalizes the name and validates the rest.
     *
     * @throws NullPointerException     if {@code key} or {@code child} is null
     * @throws IllegalArgumentException if {@code slot} is negative
     */
    public HorizontalBandContentNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(child, "child");
        if (slot < 0) {
            throw new IllegalArgumentException("A band slot is counted from zero: " + slot);
        }
    }

    /**
     * The single child laid out in the band.
     *
     * @return one child
     */
    @Override
    public List<DocumentNode> children() {
        return List.of(child);
    }
}
