package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.ListItemSpec;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens an authored list into {@link ListItemSpec}s for
 * {@link ListItemLayout#MARKER_CONTENT}.
 *
 * <p>This is the structural counterpart to the legacy flatten in
 * {@code TextFlowSupport}. Both walk the same tree in the same depth-first
 * order and produce one entry per rendered row; the difference is what they
 * produce. The legacy walk concatenates — depth becomes non-breaking spaces,
 * the marker becomes a text prefix, and the row is a single string. This one
 * keeps the three apart, because a marker that has become characters at the
 * front of a string can no longer be measured as a marker.</p>
 *
 * <p>Marker resolution is identical to the legacy walk and deliberately shares
 * {@link ListMarker#defaultForDepth(int)} with it, so a list does not change
 * which glyph it shows when it opts in — only where that glyph sits.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class ListItemNormalizer {

    private ListItemNormalizer() {
    }

    /**
     * Normalizes every rendered row of a list, flat or nested, in source order.
     *
     * @param node authored list node
     * @return one spec per rendered row; empty when the list renders nothing
     */
    static List<ListItemSpec> normalize(ListNode node) {
        List<ListItemSpec> out = new ArrayList<>();
        if (node.nestedItems().isEmpty()) {
            for (String item : node.items()) {
                String content = ListMarker.normalizeItemText(item, node.normalizeMarkers());
                if (content.isBlank()) {
                    // Same rule as the flat legacy path: an item with no
                    // renderable content contributes no row, marker or not.
                    continue;
                }
                out.add(new ListItemSpec(0, node.marker(), content));
            }
            return List.copyOf(out);
        }
        normalizeNested(node, node.nestedItems(), 0, out);
        return List.copyOf(out);
    }

    private static void normalizeNested(ListNode node,
                                        List<ListItem> items,
                                        int depth,
                                        List<ListItemSpec> out) {
        for (ListItem item : items) {
            ListMarker marker = item.marker() != null
                    ? item.marker()
                    : ListMarker.defaultForDepth(depth);
            String content = ListMarker.normalizeItemText(item.label(), node.normalizeMarkers());
            if (!content.isBlank()) {
                out.add(new ListItemSpec(depth, marker, content));
            }
            // Children are walked either way: an empty label is a reason to skip
            // that one row, never a reason to lose the sub-tree hanging off it.
            if (!item.children().isEmpty()) {
                normalizeNested(node, item.children(), depth + 1, out);
            }
        }
    }
}
