package com.demcha.compose.document.templates.api;

import com.demcha.compose.document.node.DocumentNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable map from slot names to ordered lists of {@link DocumentNode}
 * children.
 *
 * <p>Templates v2 layouts expose named slots ({@code "main"},
 * {@code "sidebar"}, {@code "col-1"}, etc.) that the surrounding
 * preset / builder fills with composed module nodes. The
 * {@code SlotMap} is the carrier that travels between
 * {@code CvBuilder.build()} and {@code CvLayout.compose(...)}.</p>
 *
 * <p>Insertion order within a slot is preserved — children are
 * rendered in the order they were added.</p>
 *
 * <p>The map is intentionally permissive about unknown slot names:
 * adding a child to a slot the layout does not declare is a
 * caller-side bug, surfaced during composition by the layout
 * (typically by ignoring the unknown slot and / or logging a warning).
 * Validation against a layout's declared slot list is the layout's
 * job, not the map's.</p>
 */
public final class SlotMap {

    private final Map<String, List<DocumentNode>> bySlot = new LinkedHashMap<>();

    /**
     * Creates a new empty slot map.
     */
    public SlotMap() {
    }

    /**
     * Returns a new empty slot map. Equivalent to {@code new SlotMap()};
     * provided for fluent symmetry with builder-style call sites.
     *
     * @return new empty slot map
     */
    public static SlotMap empty() {
        return new SlotMap();
    }

    /**
     * Appends a child to the named slot.
     *
     * @param slot non-null slot name
     * @param node non-null child node
     * @return this slot map (for chaining)
     * @throws NullPointerException if either argument is null
     */
    public SlotMap add(String slot, DocumentNode node) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(node, "node");
        bySlot.computeIfAbsent(slot, key -> new ArrayList<>()).add(node);
        return this;
    }

    /**
     * Appends multiple children to the named slot in source order.
     *
     * @param slot  non-null slot name
     * @param nodes non-null list of children; may be empty
     * @return this slot map (for chaining)
     * @throws NullPointerException if any argument or any node is null
     */
    public SlotMap addAll(String slot, List<DocumentNode> nodes) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(nodes, "nodes");
        nodes.forEach(node -> Objects.requireNonNull(node, "node"));
        bySlot.computeIfAbsent(slot, key -> new ArrayList<>()).addAll(nodes);
        return this;
    }

    /**
     * Returns the children placed in the given slot, in insertion order.
     *
     * @param slot slot name; may be unknown
     * @return immutable list of children, or an empty list if the slot
     *         has no children (or is unknown)
     */
    public List<DocumentNode> get(String slot) {
        List<DocumentNode> children = bySlot.get(slot);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        return List.copyOf(children);
    }

    /**
     * Returns the names of slots that currently hold at least one
     * child, in insertion order.
     *
     * @return ordered list of slot names with children
     */
    public List<String> populatedSlots() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, List<DocumentNode>> entry : bySlot.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                names.add(entry.getKey());
            }
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Returns true if no child has been added to any slot.
     *
     * @return true when this map is empty
     */
    public boolean isEmpty() {
        return bySlot.values().stream().allMatch(List::isEmpty);
    }
}
