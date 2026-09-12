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
 * <p>An item's content is either its {@code label} or, when it needs more than
 * one style, a sequence of {@code runs} — the same {@link InlineRun} sequence a
 * paragraph is made of, so {@code "Label: description"} with the label bold is
 * one list item rather than a hand-built row of two columns. When {@code runs}
 * is non-empty it is the content, and {@code label} is its plain-text reading,
 * kept so that anything reading an item as text still can.</p>
 *
 * @param label    visible item text; the plain-text reading when {@code runs} is
 *                 set
 * @param runs     inline runs making up the content, empty when the item is
 *                 just its label
 * @param marker   per-item marker override, or {@code null} to inherit
 *                 the per-depth default from the parent list
 * @param children nested child items, empty for leaves
 * @author Artem Demchyshyn
 */
public record ListItem(String label, List<InlineRun> runs, ListMarker marker,
                       List<ListItem> children) {
    /**
     * Normalizes nullable inputs and copy-protects {@code runs} and {@code children}.
     */
    public ListItem {
        label = label == null ? "" : label;
        runs = runs == null ? List.of() : List.copyOf(runs);
        children = children == null ? List.of() : List.copyOf(children);
    }

    /**
     * Creates an item whose content is its label, which is all an item could
     * carry before it could carry runs.
     *
     * <p>Kept as its own constructor rather than folded into the canonical one,
     * so code written against the three-argument shape still compiles and still
     * links.</p>
     *
     * @param label    visible item text
     * @param marker   per-item marker override, or {@code null} to inherit
     * @param children nested child items, empty for leaves
     */
    public ListItem(String label, ListMarker marker, List<ListItem> children) {
        this(label, List.of(), marker, children);
    }

    /**
     * Creates a leaf item whose content is a sequence of inline runs, reading as
     * whatever those runs say.
     *
     * @param runs the runs making up the content
     * @return leaf item
     * @since 2.4.0
     */
    public static ListItem ofRuns(List<InlineRun> runs) {
        return new ListItem(InlineRun.plainText(runs), runs, null, List.of());
    }

    /**
     * Creates a leaf item whose content is a sequence of inline runs, reading as
     * the given label.
     *
     * <p>Use this over {@link #ofRuns(List)} when the runs draw something that
     * does not read as itself — a row that is an icon and a chip has no text to
     * derive a label from, and {@code label} is then what anything reading the
     * item as text gets.</p>
     *
     * @param label plain-text reading of the runs
     * @param runs  the runs making up the content
     * @return leaf item
     * @since 2.4.0
     */
    public static ListItem ofRuns(String label, List<InlineRun> runs) {
        return new ListItem(label, runs, null, List.of());
    }

    /**
     * Returns whether this item's content is runs rather than its label alone.
     *
     * @return {@code true} when the item carries inline runs
     * @since 2.4.0
     */
    public boolean isRich() {
        return !runs.isEmpty();
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
     * @param label    visible item text
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
