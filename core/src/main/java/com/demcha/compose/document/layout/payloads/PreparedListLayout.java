package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.PreparedNodeLayout;

import java.util.List;

/**
 * Prepared layout payload attached to {@code ListNode} prepared nodes.
 * Aggregates the per-item prepared layouts and the resolved width/height
 * the list definition uses for the emit pass.
 *
 * @param items              per-item prepared layouts
 * @param maxLineWidth       widest measured line width across items
 * @param totalHeight        cumulative list height
 * @param resolvedWidth      resolved layout width
 * @param markerContentItems the normalized depth/marker/content view of the same
 *                           items, in the same order — populated only under
 *                           {@link com.demcha.compose.document.layout.ListItemLayout#MARKER_CONTENT},
 *                           and empty under the legacy prefix layout, which has
 *                           no marker left to keep apart from its text
 */
public record PreparedListLayout(
        List<PreparedListItemLayout> items,
        double maxLineWidth,
        double totalHeight,
        double resolvedWidth,
        List<ListItemSpec> markerContentItems
) implements PreparedNodeLayout {
    /**
     * Freezes both item lists to keep the prepared layout immutable.
     */
    public PreparedListLayout {
        items = List.copyOf(items);
        markerContentItems = markerContentItems == null ? List.of() : List.copyOf(markerContentItems);
    }

    /**
     * Creates a prepared list layout with no normalized item view — the legacy
     * prefix layout, where depth and marker are already inside each item's text.
     *
     * @param items         per-item prepared layouts
     * @param maxLineWidth  widest measured line width across items
     * @param totalHeight   cumulative list height
     * @param resolvedWidth resolved layout width
     */
    public PreparedListLayout(List<PreparedListItemLayout> items,
                              double maxLineWidth,
                              double totalHeight,
                              double resolvedWidth) {
        this(items, maxLineWidth, totalHeight, resolvedWidth, List.of());
    }
}
