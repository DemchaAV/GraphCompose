package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable list instruction used by shared template scene composers.
 *
 * @param name semantic list name used in snapshots and layout graph paths
 * @param items list item texts in source order
 * @param marker marker rendered before each item
 * @param style shared item text style
 * @param align horizontal item alignment
 * @param lineSpacing extra spacing between wrapped lines in one item
 * @param itemSpacing extra spacing between list items
 * @param normalizeMarkers whether input items may include pre-existing markers
 * @param padding list padding
 * @param margin list margin
 * @author Artem Demchyshyn
 */
public record TemplateListSpec(
        String name,
        List<String> items,
        ListMarker marker,
        TextStyle style,
        TextAlign align,
        double lineSpacing,
        double itemSpacing,
        boolean normalizeMarkers,
        Padding padding,
        Margin margin
) {
    /**
     * Creates a normalized template list instruction.
     */
    public TemplateListSpec {
        name = name == null ? "" : name;
        items = normalizeItems(items);
        marker = marker == null ? ListMarker.bullet() : marker;
        style = style == null ? TextStyle.DEFAULT_STYLE : style;
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
