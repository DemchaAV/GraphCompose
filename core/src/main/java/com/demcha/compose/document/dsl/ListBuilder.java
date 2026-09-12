package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.InlineRun;
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
    /**
     * Whether the items still fit the flat {@code List<String>} shape a list had
     * before it could nest. Nesting is one reason they do not; a rich item, whose
     * content is runs rather than a label, is the other — it has to reach the
     * layout as a {@link ListItem}, and the flat path carries only labels.
     */
    private boolean needsItemTree = false;
    /**
     * Whether the author declared depth. Kept apart from {@link #needsItemTree}
     * because the two answer different questions, and only this one decides
     * whose marker depth 0 takes: a nested list resolves every level from the
     * per-depth cascade and {@link #markerFor(int, ListMarker)}, while a flat
     * list's marker is its own — and a list of rich items is still flat.
     */
    private boolean declaredDepth = false;
    private ListMarker marker = ListMarker.bullet();
    private DocumentTextStyle textStyle = DocumentTextStyle.DEFAULT;
    private TextAlign align = TextAlign.LEFT;
    private double lineSpacing = 0.0;
    private double itemSpacing = 0.0;
    private String continuationIndent = "";
    private boolean normalizeMarkers = true;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private boolean hangingIndent = false;
    private double markerGap = ListNode.DEFAULT_MARKER_GAP;

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
            out.add(new ListItem(item.label(), item.runs(), effective, resolvedChildren));
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
        this.needsItemTree = true;
        this.declaredDepth = true;
        ListBuilder childScope = new ListBuilder();
        body.accept(childScope);
        this.items.add(new ListItem(label, null, childScope.snapshotItems()));
        return this;
    }

    /**
     * Appends one list item whose content is styled in pieces.
     *
     * <p>{@code "Bold label: normal description"} becomes one item rather than a
     * row of two columns pretending to be one. The content is a {@link RichText}
     * — the same inline runs a paragraph is made of, taken by the same builder
     * {@link ParagraphBuilder#rich(Consumer)} takes — so the library has one
     * rich-text model and not a second one for lists.</p>
     *
     * <p>The item lays out on the measured marker geometry:
     * {@link #hangingIndent(boolean)} is required, and the layout says so if it
     * is missing. The marker is measured, {@link #markerGap(double)} applies, and
     * every visual line of the content — the first, the ones it wraps onto, the
     * ones that continue on the next page — starts at one x. A rich item is
     * content whatever its runs draw, so runs of an icon and no text are still a
     * row.</p>
     *
     * <p>Seed the supplied builder with {@link RichText#plain(String)} — not
     * {@code t.text(...)}: {@link RichText#text(String)} is a static factory, so
     * that call compiles but builds a separate, discarded {@code RichText} and
     * leaves this item empty.</p>
     *
     * @param content callback that appends this item's inline runs
     * @return this builder
     * @throws NullPointerException if {@code content} is null
     * @since 2.4.0
     */
    public ListBuilder addItem(Consumer<RichText> content) {
        Objects.requireNonNull(content, "content");
        RichText rich = RichText.empty();
        content.accept(rich);
        this.needsItemTree = true;
        this.items.add(ListItem.ofRuns(rich.runs()));
        return this;
    }

    /**
     * Appends one nested list item whose own content is styled in pieces.
     *
     * <p>{@link #addItem(Consumer)} with children, so a styled label can head a
     * sub-tree.</p>
     *
     * @param content callback that appends this item's inline runs
     * @param body    callback that adds children of this item
     * @return this builder
     * @throws NullPointerException if either callback is null
     * @since 2.4.0
     */
    public ListBuilder addItem(Consumer<RichText> content, Consumer<ListBuilder> body) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(body, "body");
        this.needsItemTree = true;
        this.declaredDepth = true;
        RichText rich = RichText.empty();
        content.accept(rich);
        ListBuilder childScope = new ListBuilder();
        body.accept(childScope);
        List<InlineRun> runs = rich.runs();
        this.items.add(new ListItem(InlineRun.plainText(runs), runs, null,
                childScope.snapshotItems()));
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
     * Lays the list out as a marker column and a content column, so every
     * visual line of an item — the lines it wraps onto, and the lines that
     * continue on the next page — starts at the same horizontal position, one
     * marker width plus {@link #markerGap(double)} in from the item's own start.
     *
     * <p>Off by default, and this is not a step towards making it the default.
     * Unset, a list renders exactly as it did in v1.4 through 2.3: the marker is
     * a text prefix on the first line and wrapped lines carry a run of spaces
     * measured to clear it, which lands them a fraction of a space width off the
     * first line's text. Setting this replaces that approximation with
     * geometry.</p>
     *
     * <p>Applies to nested lists too — depth, marker and content stay apart
     * instead of being concatenated into one label, so each level resolves its
     * own content origin. Two consequences of that are worth knowing. Because a
     * nested label is no longer carrying a baked-in marker that must survive,
     * {@link #normalizeMarkers(boolean)} applies to it the way it already
     * applies to a flat item, so an author-typed {@code "- "} is stripped from a
     * child label as well. And an item that draws nothing at all — no text and
     * no marker — contributes no row, so its children hang at the level it would
     * have occupied rather than one deeper.</p>
     *
     * <p><b>Fixed-layout only.</b> This is geometry, and it applies to the
     * backends that do their own layout — PDF and PPTX. The semantic DOCX
     * export writes a Word paragraph per item and lets Word lay it out, so it
     * keeps the marker in the item's text and is unchanged by this setting: the
     * same paragraphs, the same text, the same nesting. Word positions content
     * at absolute indents and has no way to be told "start the text one marker
     * width plus a gap from here", so reproducing this geometry there would mean
     * measuring the marker — which the semantic backend deliberately cannot do,
     * since it depends on neither a font runtime nor a layout pass. A document
     * exported both ways is therefore identical in content and nesting, and
     * differs in how its wrapped lines line up.</p>
     *
     * @param hangingIndent whether items use marker/content geometry
     * @return this builder
     * @since 2.4.0
     */
    public ListBuilder hangingIndent(boolean hangingIndent) {
        this.hangingIndent = hangingIndent;
        return this;
    }

    /**
     * Sets the space between an item's marker and its content, in points.
     *
     * <p>Observed only when {@link #hangingIndent(boolean)} is set. The legacy
     * layout's gap is whatever the marker's own trailing separator measures, and
     * this value does not change it.</p>
     *
     * <p>Real geometry, never spaces. A markerless item takes no gap at all,
     * rather than an unexplained inset.</p>
     *
     * <p><b>Fixed-layout only</b>, for the reason given on
     * {@link #hangingIndent(boolean)}: the semantic DOCX export does not lay text
     * out and cannot place content a measured distance after a marker, so it
     * ignores this value rather than approximating it with something that would
     * render as a different number than the one asked for.</p>
     *
     * @param markerGap gap in points; {@code 0} is allowed
     * @return this builder
     * @throws IllegalArgumentException when {@code markerGap} is negative, NaN or infinite
     * @since 2.4.0
     */
    public ListBuilder markerGap(double markerGap) {
        if (markerGap < 0 || Double.isNaN(markerGap) || Double.isInfinite(markerGap)) {
            throw new IllegalArgumentException("markerGap must be finite and non-negative: " + markerGap);
        }
        this.markerGap = markerGap;
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
     * flat list (back-compat with v1.4 / v1.5). As soon as something is
     * added that a list of labels cannot hold — a nested item via
     * {@link #addItem(String, Consumer)}, or a rich item via
     * {@link #addItem(Consumer)}, either of them once — the result is an
     * item tree; flat items added before that become depth-0 leaves
     * alongside the rest, preserving source order. Per-depth marker
     * overrides set via {@link #markerFor(int, ListMarker)} are baked
     * into each item's resolved marker before the node is sealed.</p>
     *
     * @return list node
     */
    public ListNode build() {
        if (!needsItemTree) {
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
                    margin,
                    hangingIndent,
                    markerGap);
        }
        // Nested path. Source order across flat and nested entries is
        // preserved because both flow through the unified `items` list.
        // A flat list's marker is its own, whether its items are strings or
        // runs. Only a list whose author declared depth hands depth 0 to the
        // per-depth cascade — that is the nested contract, and markerFor(0, …)
        // is how it is overridden. Without this a dashed list would render one
        // bullet the moment one of its items needed styling.
        Map<Integer, ListMarker> effectiveOverrides = markerOverrides;
        if (!declaredDepth) {
            effectiveOverrides = new LinkedHashMap<>(markerOverrides);
            effectiveOverrides.putIfAbsent(0, marker);
        }
        List<ListItem> resolved = applyMarkerOverrides(items, 0, effectiveOverrides);
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
                margin,
                hangingIndent,
                markerGap);
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
