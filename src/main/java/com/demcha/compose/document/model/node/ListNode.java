package com.demcha.compose.document.model.node;

import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic list block with one marker and style shared by all items.
 *
 * <p>This node is the canonical high-level API for simple unordered lists. It
 * keeps list authoring explicit without requiring callers to manually compose
 * many paragraph nodes just to get predictable bullets and hanging indents.</p>
 *
 * @param name optional semantic name used in snapshots and diagnostics
 * @param items item texts in source order
 * @param marker marker rendered before each item
 * @param textStyle shared item text style
 * @param align horizontal alignment for item text
 * @param lineSpacing extra space between wrapped lines within one item
 * @param itemSpacing extra space between list items
 * @param normalizeMarkers whether leading user-supplied bullets or dashes are stripped
 * @param padding inner list padding
 * @param margin outer list margin
 * @author Artem Demchyshyn
 */
public record ListNode(
        String name,
        List<String> items,
        ListMarker marker,
        TextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        double itemSpacing,
        boolean normalizeMarkers,
        Padding padding,
        Margin margin
) implements DocumentNode {
    /**
     * Creates a normalized list node.
     */
    public ListNode {
        name = name == null ? "" : name;
        items = normalizeItems(items);
        marker = marker == null ? ListMarker.bullet() : marker;
        textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
        if (itemSpacing < 0 || Double.isNaN(itemSpacing) || Double.isInfinite(itemSpacing)) {
            throw new IllegalArgumentException("itemSpacing must be finite and non-negative: " + itemSpacing);
        }
    }

    private static List<String> normalizeItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(items.size());
        for (String item : items) {
            normalized.add(item == null ? "" : item);
        }
        return List.copyOf(normalized);
    }
}
