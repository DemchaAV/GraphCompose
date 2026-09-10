package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic list block with one marker and style shared by all items.
 *
 * <p>This node is the canonical high-level API for unordered lists.
 * It supports both flat and nested authoring:</p>
 *
 * <ul>
 *   <li><b>Flat</b> — supply {@code items} (a list of plain strings)
 *       and a single {@code marker}. Pagination splits on item
 *       boundaries; this is the v1.4 / v1.5 surface and continues to
 *       work unchanged.</li>
 *   <li><b>Nested</b> — supply {@code nestedItems} (a tree of
 *       {@link ListItem}). Each level renders with depth-appropriate
 *       indentation and a marker resolved either from
 *       {@code item.marker()} or, when {@code null}, from per-depth
 *       defaults set on {@link com.demcha.compose.document.dsl.ListBuilder}.</li>
 * </ul>
 *
 * <p>When {@code nestedItems} is non-empty, the layout pipeline
 * flattens the tree depth-first into indent-prefixed paragraph
 * fragments and the top-level {@code marker} / {@code items} fields
 * are ignored. When {@code nestedItems} is empty, the node behaves
 * exactly like the v1.5 flat list.</p>
 *
 * @param name               optional semantic name used in snapshots and diagnostics
 * @param items              item texts in source order — used when {@code nestedItems} is empty
 * @param nestedItems        nested item tree, empty for flat lists
 * @param marker             top-level marker rendered before each flat item
 * @param textStyle          shared item text style
 * @param align              horizontal alignment for item text
 * @param lineSpacing        extra space between wrapped lines within one item
 * @param itemSpacing        extra space between list items
 * @param continuationIndent prefix used only for wrapped continuation lines when the marker is hidden
 * @param normalizeMarkers   whether leading user-supplied bullets or dashes are stripped
 * @param padding            inner list padding
 * @param margin             outer list margin
 * @param hangingIndent      whether items get marker/content geometry instead of the
 *                           legacy text prefix; {@code false} — the default — keeps the
 *                           v1.4-through-2.3 rendering exactly
 * @param markerGap          space between marker and content in points, observed only
 *                           when {@code hangingIndent} is set
 * @author Artem Demchyshyn
 */
public record ListNode(
        String name,
        List<String> items,
        List<ListItem> nestedItems,
        ListMarker marker,
        DocumentTextStyle textStyle,
        TextAlign align,
        double lineSpacing,
        double itemSpacing,
        String continuationIndent,
        boolean normalizeMarkers,
        DocumentInsets padding,
        DocumentInsets margin,
        boolean hangingIndent,
        double markerGap
) implements DocumentNode {

    /**
     * Space between marker and content used when {@code hangingIndent} is set
     * and the author did not choose a gap, in points.
     *
     * @since 2.4.0
     */
    public static final double DEFAULT_MARKER_GAP = 4.0;

    /**
     * Creates a normalized list node.
     */
    public ListNode {
        name = name == null ? "" : name;
        items = normalizeItems(items);
        nestedItems = nestedItems == null ? List.of() : List.copyOf(nestedItems);
        marker = marker == null ? ListMarker.bullet() : marker;
        textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        align = align == null ? TextAlign.LEFT : align;
        continuationIndent = continuationIndent == null ? "" : continuationIndent;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        if (lineSpacing < 0 || Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing)) {
            throw new IllegalArgumentException("lineSpacing must be finite and non-negative: " + lineSpacing);
        }
        if (itemSpacing < 0 || Double.isNaN(itemSpacing) || Double.isInfinite(itemSpacing)) {
            throw new IllegalArgumentException("itemSpacing must be finite and non-negative: " + itemSpacing);
        }
        // Validated even when hangingIndent is false: a node that carries a
        // nonsense gap and only reveals it the day someone opts in is worse
        // than one that refuses to be built.
        if (markerGap < 0 || Double.isNaN(markerGap) || Double.isInfinite(markerGap)) {
            throw new IllegalArgumentException("markerGap must be finite and non-negative: " + markerGap);
        }
    }

    /**
     * Back-compat constructor matching the v1.4 / v1.5 11-component
     * signature. Treats the list as flat (no nested items).
     *
     * @param name               optional semantic name used in snapshots and diagnostics
     * @param items              item texts in source order — used when {@code nestedItems} is empty
     * @param marker             top-level marker rendered before each flat item
     * @param textStyle          shared item text style
     * @param align              horizontal alignment for item text
     * @param lineSpacing        extra space between wrapped lines within one item
     * @param itemSpacing        extra space between list items
     * @param continuationIndent prefix used only for wrapped continuation lines when the marker is hidden
     * @param normalizeMarkers   whether leading user-supplied bullets or dashes are stripped
     * @param padding            inner list padding
     * @param margin             outer list margin
     */
    public ListNode(String name,
                    List<String> items,
                    ListMarker marker,
                    DocumentTextStyle textStyle,
                    TextAlign align,
                    double lineSpacing,
                    double itemSpacing,
                    String continuationIndent,
                    boolean normalizeMarkers,
                    DocumentInsets padding,
                    DocumentInsets margin) {
        this(name, items, List.of(), marker, textStyle, align, lineSpacing, itemSpacing,
                continuationIndent, normalizeMarkers, padding, margin);
    }

    /**
     * Back-compat constructor matching the v1.8-through-2.3 12-component
     * signature. Keeps the legacy prefix layout — {@code hangingIndent} is
     * {@code false} and {@code markerGap} takes its default, which that layout
     * does not observe.
     *
     * @param name               optional semantic name used in snapshots and diagnostics
     * @param items              item texts in source order — used when {@code nestedItems} is empty
     * @param nestedItems        nested item tree, empty for flat lists
     * @param marker             top-level marker rendered before each flat item
     * @param textStyle          shared item text style
     * @param align              horizontal alignment for item text
     * @param lineSpacing        extra space between wrapped lines within one item
     * @param itemSpacing        extra space between list items
     * @param continuationIndent prefix used only for wrapped continuation lines when the marker is hidden
     * @param normalizeMarkers   whether leading user-supplied bullets or dashes are stripped
     * @param padding            inner list padding
     * @param margin             outer list margin
     */
    public ListNode(String name,
                    List<String> items,
                    List<ListItem> nestedItems,
                    ListMarker marker,
                    DocumentTextStyle textStyle,
                    TextAlign align,
                    double lineSpacing,
                    double itemSpacing,
                    String continuationIndent,
                    boolean normalizeMarkers,
                    DocumentInsets padding,
                    DocumentInsets margin) {
        this(name, items, nestedItems, marker, textStyle, align, lineSpacing, itemSpacing,
                continuationIndent, normalizeMarkers, padding, margin, false, DEFAULT_MARKER_GAP);
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
