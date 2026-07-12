package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.layout.PreparedNodeLayout;

import java.util.List;

/**
 * Prepared layout payload attached to {@code ListNode} prepared nodes.
 * Aggregates the per-item prepared layouts and the resolved width/height
 * the list definition uses for the emit pass.
 *
 * @param items         per-item prepared layouts
 * @param maxLineWidth  widest measured line width across items
 * @param totalHeight   cumulative list height
 * @param resolvedWidth resolved layout width
 */
public record PreparedListLayout(
        List<PreparedListItemLayout> items,
        double maxLineWidth,
        double totalHeight,
        double resolvedWidth
) implements PreparedNodeLayout {
    /**
     * Freezes the items list to keep the prepared layout immutable.
     */
    public PreparedListLayout {
        items = List.copyOf(items);
    }
}
