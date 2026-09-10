package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.ListItemSpec;
import com.demcha.compose.document.layout.payloads.MarkerContentItem;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTextStyle;

/**
 * Resolves the marker/content geometry of a list from its normalized items.
 *
 * <p>The marker's width is <b>measured</b>, in the list's own text style. It is
 * never counted in characters and never approximated by spaces — that
 * approximation is precisely what the legacy layout does and what this replaces.
 * The caller never computes any of these numbers; it asks for them.</p>
 *
 * <h2>Depth</h2>
 * <p>Nesting is an outline, not a fixed step: a child's marker starts where its
 * parent's <em>text</em> starts. Because {@link ListItemNormalizer} emits items
 * depth-first in source order, the most recent item one level up is exactly the
 * parent, so one array of content offsets resolves the whole tree in a single
 * pass — no tree walk, no per-item search, and nothing that grows with the
 * square of the item count.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class ListMarkerGeometry {

    private ListMarkerGeometry() {
    }

    /**
     * Resolves geometry for every normalized item of a list.
     *
     * @param specs              normalized items, depth-first in source order
     * @param node               the list, for its marker gap and text style
     * @param availableItemWidth width one row may occupy, inside the list padding
     * @param measurement        text measurement service
     * @return one resolved item per spec, in the same order
     */
    static List<MarkerContentItem> resolve(List<ListItemSpec> specs,
                                           ListNode node,
                                           double availableItemWidth,
                                           TextMeasurementSystem measurement) {
        if (specs.isEmpty()) {
            return List.of();
        }
        TextStyle style = toTextStyle(node.textStyle());
        List<MarkerContentItem> out = new ArrayList<>(specs.size());
        // contentX of the most recent item at each depth, which is where a child
        // of that item hangs its own marker.
        double[] contentXByDepth = new double[8];

        for (ListItemSpec spec : specs) {
            int depth = spec.depth();
            contentXByDepth = ensureCapacity(contentXByDepth, depth + 1);

            // A depth with no item above it resolves to 0 rather than throwing:
            // the builder cannot produce that, but a hand-built node can, and a
            // list is not worth failing a render over.
            double markerX = depth == 0 ? 0.0 : contentXByDepth[depth - 1];

            boolean hasMarker = spec.hasMarker();
            double markerWidth = hasMarker ? measurement.textWidth(style, spec.markerText()) : 0.0;
            double gap = hasMarker ? node.markerGap() : 0.0;
            double contentX = markerX + markerWidth + gap;
            // Floored at the width the text pipeline already treats as its
            // minimum, so a marker column wider than its container overflows the
            // way an over-long word does — rather than dropping to the engine's
            // zero-width behaviour, which renders an empty line and loses the
            // text. The marker itself is placed at markerX and drawn at its
            // measured width either way; nothing here clips it.
            double contentWidth = Math.max(
                    ParagraphWrapping.MIN_TEXT_WIDTH, availableItemWidth - contentX);

            contentXByDepth[depth] = contentX;
            out.add(new MarkerContentItem(spec, markerX, markerWidth, gap, contentX, contentWidth));
        }
        return List.copyOf(out);
    }

    private static double[] ensureCapacity(double[] values, int needed) {
        if (needed <= values.length) {
            return values;
        }
        int grown = values.length;
        while (grown < needed) {
            grown *= 2;
        }
        return Arrays.copyOf(values, grown);
    }
}
