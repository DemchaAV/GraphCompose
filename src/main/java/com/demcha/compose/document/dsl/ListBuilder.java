package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.*;
import java.util.function.Consumer;

/**
 * Builder for semantic list nodes with marker and spacing controls.
 *
 * @since 1.0.0
 */
public final class ListBuilder {
    private final List<ListItem> items = new ArrayList<>();
    private final Map<Integer, ListMarker> markerOverrides = new LinkedHashMap<>();
    private String name = "";
    private boolean usedNestedAuthoring = false;
    private ListMarker marker = ListMarker.bullet();
    private DocumentTextStyle textStyle = DocumentTextStyle.DEFAULT;
    private TextAlign align = TextAlign.LEFT;
    private double lineSpacing = 0.0;
    private double itemSpacing = 0.0;
    private String continuationIndent = "";
    private boolean normalizeMarkers = true;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a list builder.
     */
    public ListBuilder() {
    }

    private static List<ListItem> applyMarkerOverrides(List<ListItem> items,
                                                       int depth,
                                                       Map<Integer, ListMarker> overrides) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<ListItem> out = new ArrayList<>(items.size());
        for (ListItem item : items) {
            ListMarker effective = item.marker() != null
                    ? item.marker()
                    : overrides.get(depth);
            List<ListItem> resolvedChildren = applyMarkerOverrides(item.children(), depth + 1, overrides);
            out.add(new ListItem(item.label(), effective, resolvedChildren));
        }
        return List.copyOf(out);
    }

    /**
     * Sets the list node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ListBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Replaces list items from varargs.
     *
     * @param items item texts
     * @return this builder
     */
    public ListBuilder items(String... items) {
        this.items.clear();
        if (items != null) {
            for (String item : items) {
                this.items.add(new ListItem(item, null, List.of()));
            }
        }
        return this;
    }

    /**
     * Replaces list items from a collection.
     *
     * @param items item texts
     * @return this builder
     */
    public ListBuilder items(List<String> items) {
        this.items.clear();
        if (items != null) {
            for (String item : items) {
                this.items.add(new ListItem(item, null, List.of()));
            }
        }
        return this;
    }

    /**
     * Appends one list item.
     *
     * @param item item text
     * @return this builder
     */
    public ListBuilder addItem(String item) {
        this.items.add(new ListItem(item, null, List.of()));
        return this;
    }

    /**
     * Appends one nested list item with the given label and a builder
     * callback that adds the item's children. The callback receives a
     * fresh {@link ListBuilder} scoped to the child level — every
     * {@code addItem(...)} call inside the callback adds a child of
     * this item, and nested {@code addItem(label, body)} calls extend
     * the tree to deeper levels.
     *
     * <p>Switching to nested authoring promotes the list to the nested
     * code path; markers, indentation, and pagination are resolved per
     * depth. Mixing {@link #addItem(String)} (flat) and this method on
     * the same builder is supported — the flat items become depth-0
     * leaves alongside the nested entries.</p>
     *
     * @param label visible label for this item
     * @param body  callback that adds children of this item
     * @return this builder
     */
    public ListBuilder addItem(String label, Consumer<ListBuilder> body) {
        Objects.requireNonNull(body, "body");
        this.usedNestedAuthoring = true;
        ListBuilder childScope = new ListBuilder();
        body.accept(childScope);
        this.items.add(new ListItem(label, null, childScope.snapshotItems()));
        return this;
    }

    /**
     * Overrides the marker used for items at the given depth when no
     * per-item marker is set. Depth 0 is the top-level marker; depth 1
     * is the first nested level, and so on.
     *
     * <p>When unset, depth-0 falls back to {@link #marker(ListMarker)}
     * (default {@code bullet()}), and deeper levels fall back to a
     * built-in cascade ({@code •} → {@code ◦} → {@code ▪} → {@code ·}).
     * This setter wins over the cascade but is overridden by an
     * explicit per-item marker.</p>
     *
     * @param depth  zero-based depth (0 = top-level)
     * @param marker marker to render at that depth, or {@code null} to clear an override
     * @return this builder
     * @throws IllegalArgumentException when {@code depth} is negative
     */
    public ListBuilder markerFor(int depth, ListMarker marker) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be non-negative: " + depth);
        }
        if (marker == null) {
            this.markerOverrides.remove(depth);
        } else {
            this.markerOverrides.put(depth, marker);
        }
        return this;
    }

    /**
     * Sets the list marker.
     *
     * @param marker list marker
     * @return this builder
     */
    public ListBuilder marker(ListMarker marker) {
        this.marker = marker == null ? ListMarker.bullet() : marker;
        return this;
    }

    /**
     * Sets a custom list marker.
     *
     * @param marker marker text
     * @return this builder
     */
    public ListBuilder marker(String marker) {
        return marker(ListMarker.custom(marker));
    }

    /**
     * Uses bullet markers.
     *
     * @return this builder
     */
    public ListBuilder bullet() {
        return marker(ListMarker.bullet());
    }

    /**
     * Uses dash markers.
     *
     * @return this builder
     */
    public ListBuilder dash() {
        return marker(ListMarker.dash());
    }

    /**
     * Uses markerless rows.
     *
     * @return this builder
     */
    public ListBuilder noMarker() {
        return marker(ListMarker.none());
    }

    /**
     * Sets list text style with the public canonical style value.
     *
     * @param textStyle list text style
     * @return this builder
     */
    public ListBuilder textStyle(DocumentTextStyle textStyle) {
        this.textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        return this;
    }

    /**
     * Sets list item alignment.
     *
     * @param align item text alignment
     * @return this builder
     */
    public ListBuilder align(TextAlign align) {
        this.align = align == null ? TextAlign.LEFT : align;
        return this;
    }

    /**
     * Sets spacing between wrapped lines within one item.
     *
     * @param lineSpacing line spacing in points
     * @return this builder
     */
    public ListBuilder lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    /**
     * Sets spacing between list items.
     *
     * @param itemSpacing item spacing in points
     * @return this builder
     */
    public ListBuilder itemSpacing(double itemSpacing) {
        this.itemSpacing = itemSpacing;
        return this;
    }

    /**
     * Sets the prefix used only for wrapped continuation lines when the list
     * has no visible marker.
     *
     * @param continuationIndent continuation-line prefix, often a few spaces
     * @return this builder
     */
    public ListBuilder continuationIndent(String continuationIndent) {
        this.continuationIndent = continuationIndent == null ? "" : continuationIndent;
        return this;
    }

    /**
     * Sets whether leading raw markers should be stripped from input items.
     *
     * @param normalizeMarkers whether input markers are normalized
     * @return this builder
     */
    public ListBuilder normalizeMarkers(boolean normalizeMarkers) {
        this.normalizeMarkers = normalizeMarkers;
        return this;
    }

    /**
     * Sets list padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ListBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets list padding from explicit side values.
     *
     * @param top    top padding
     * @param right  right padding
     * @param bottom bottom padding
     * @param left   left padding
     * @return this builder
     */
    public ListBuilder padding(float top, float right, float bottom, float left) {
        return padding(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Sets list margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ListBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets list margin from explicit side values.
     *
     * @param top    top margin
     * @param right  right margin
     * @param bottom bottom margin
     * @param left   left margin
     * @return this builder
     */
    public ListBuilder margin(float top, float right, float bottom, float left) {
        return margin(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Builds the semantic list node.
     *
     * <p>When only {@link #addItem(String)} was used the result is a
     * flat list (back-compat with v1.4 / v1.5). As soon as
     * {@link #addItem(String, Consumer)} is called at least once the
     * result is a nested list — flat items added before the first
     * nested call become depth-0 leaves alongside the nested entries,
     * preserving source order. Per-depth marker overrides set via
     * {@link #markerFor(int, ListMarker)} are baked into each item's
     * resolved marker before the node is sealed.</p>
     *
     * @return list node
     */
    public ListNode build() {
        if (!usedNestedAuthoring) {
            // Back-compat flat path. node.items() carries the labels
            // and node.nestedItems() is empty; rendering matches the
            // v1.4 / v1.5 flat-list behaviour exactly.
            List<String> flatLabels = new ArrayList<>(items.size());
            for (ListItem item : items) {
                flatLabels.add(item.label());
            }
            return new ListNode(
                    name,
                    List.copyOf(flatLabels),
                    List.of(),
                    marker,
                    textStyle,
                    align,
                    lineSpacing,
                    itemSpacing,
                    continuationIndent,
                    normalizeMarkers,
                    padding,
                    margin);
        }
        // Nested path. Source order across flat and nested entries is
        // preserved because both flow through the unified `items` list.
        List<ListItem> resolved = applyMarkerOverrides(items, 0, markerOverrides);
        return new ListNode(
                name,
                List.of(),
                resolved,
                marker,
                textStyle,
                align,
                lineSpacing,
                itemSpacing,
                continuationIndent,
                normalizeMarkers,
                padding,
                margin);
    }

    /**
     * Snapshots the items collected on this builder for use as a
     * child sub-tree in a parent {@link #addItem(String, Consumer)}
     * invocation.
     */
    private List<ListItem> snapshotItems() {
        return List.copyOf(items);
    }

    /**
     * Returns the per-depth marker overrides set on this builder, used
     * by the layout pipeline to resolve item markers when no per-item
     * override is set.
     *
     * @return map of depth → marker overrides
     */
    public Map<Integer, ListMarker> markerOverrides() {
        return Map.copyOf(markerOverrides);
    }
}

/**
 * Builder for semantic images.
 */
