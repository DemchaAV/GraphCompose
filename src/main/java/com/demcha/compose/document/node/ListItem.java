package com.demcha.compose.document.node;

import java.util.List;

/**
 * One item in a nested {@link ListNode} tree.
 *
 * <p>A {@code ListItem} carries the visible label, an optional marker
 * override (when {@code null}, the marker is resolved per-depth from
 * the parent list's defaults), and an optional child list. Leaf items
 * have an empty children list.</p>
 *
 * <p>Construct items through {@link com.demcha.compose.document.dsl.ListBuilder}
 * — call {@code addItem(label)} for a leaf, or
 * {@code addItem(label, body -> body.addItem("child"))} for a nested
 * item. Direct {@code new ListItem(...)} construction is supported but
 * should be reserved for record-shaped fixtures and serialization.</p>
 *
 * @param label visible item text
 * @param marker per-item marker override, or {@code null} to inherit
 *               the per-depth default from the parent list
 * @param children nested child items, empty for leaves
 *
 * @author Artem Demchyshyn
 */
public record ListItem(String label, ListMarker marker, List<ListItem> children) {
    /**
     * Normalizes nullable inputs and copy-protects {@code children}.
     */
    public ListItem {
        label = label == null ? "" : label;
        children = children == null ? List.of() : List.copyOf(children);
    }

    /**
     * Creates a leaf item with the given label and inherited marker.
     *
     * @param label visible item text
     * @return leaf item
     */
    public static ListItem of(String label) {
        return new ListItem(label, null, List.of());
    }

    /**
     * Creates an item with the given label, inherited marker, and
     * child sub-tree.
     *
     * @param label visible item text
     * @param children nested child items
     * @return parent item
     */
    public static ListItem of(String label, List<ListItem> children) {
        return new ListItem(label, null, children);
    }

    /**
     * Returns whether this item has no children.
     *
     * @return {@code true} when the children list is empty
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
}
