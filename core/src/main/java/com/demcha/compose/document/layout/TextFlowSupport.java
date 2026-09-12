package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.text.TextControlSanitizer;
import com.demcha.compose.engine.text.bidi.ArabicShaper;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.*;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;

/**
 * Shared text-flow helpers backing {@link com.demcha.compose.document.layout.definitions.ParagraphDefinition}
 * and {@link com.demcha.compose.document.layout.definitions.ListDefinition}.
 *
 * <p>Holds paragraph wrapping, markdown tokenisation, inline-run layout, and
 * the slicing logic used during pagination. Definitions in
 * {@code document.layout.definitions} delegate here so they remain thin.</p>
 *
 * @author Artem Demchyshyn
 */
public final class TextFlowSupport {
    /**
     * Indent unit added per nesting depth in a nested list. Two
     * non-breaking spaces visually match two regular spaces but are
     * preserved by paragraph wrapping (which strips leading
     * {@link Character#isWhitespace whitespace} from the first token
     * of each line). Switching to NBSP keeps depth indentation intact
     * without rewriting the wrap pipeline.
     */
    private static final String NESTED_LIST_INDENT_UNIT = "  ";

    /**
     * The width a drawn list marker is measured at — wide enough that nothing a
     * marker could reasonably be will wrap, so its measured width is the width of
     * what it draws rather than of the column it happens to sit in.
     */
    private static final double MARKER_MEASURE_WIDTH = 100_000.0;

    // ------------------------------------------------------------------
    // Paragraph entry points
    // ------------------------------------------------------------------

    private TextFlowSupport() {
    }

    /**
     * Measures the glyph width of a single text string in a document text style —
     * the content width, not a full-block paragraph width. Used by leaves that
     * size themselves to a short string (e.g. a page-reference number).
     *
     * @param style       document text style
     * @param text        text to measure
     * @param measurement text measurement service
     * @return the measured glyph width in points
     */
    public static double measureTextWidth(DocumentTextStyle style, String text, TextMeasurementSystem measurement) {
        return measurement.textWidth(toTextStyle(style), text);
    }

    /**
     * Measures a paragraph node and wraps it into a prepared leaf carrying its
     * visual line layout.
     *
     * @param node        paragraph node to prepare
     * @param ctx         prepare-phase context
     * @param constraints box constraints for measurement
     * @return prepared paragraph node with its line layout
     */
    public static PreparedNode<ParagraphNode> prepareParagraph(ParagraphNode node,
                                                               PrepareContext ctx,
                                                               BoxConstraints constraints) {
        double innerWidth = Math.max(0.0, constraints.availableWidth() - node.padding().horizontal());
        PreparedParagraphLayout layout = prepareParagraphLayout(node, innerWidth, ctx.textMeasurement(), ctx.markdownEnabled());
        double measuredWidth = Math.min(constraints.availableWidth(), layout.maxLineWidth() + node.padding().horizontal());
        double resolvedWidth = node.align() == TextAlign.LEFT
                ? measuredWidth
                : constraints.availableWidth();
        MeasureResult measure = new MeasureResult(
                resolvedWidth,
                layout.totalHeight() + node.padding().vertical());
        return PreparedNode.leaf(node, measure, layout);
    }

    /**
     * Splits a prepared paragraph at the largest line count that fits the
     * remaining height.
     *
     * @param prepared prepared paragraph node
     * @param request  split request carrying the remaining height
     * @return the head/tail split result
     */
    public static PreparedSplitResult<ParagraphNode> splitParagraph(PreparedNode<ParagraphNode> prepared,
                                                                    SplitRequest request) {
        ParagraphNode node = prepared.node();
        PreparedParagraphLayout layout = prepared.requirePreparedLayout(PreparedParagraphLayout.class);

        // The head fragment keeps the current top padding, but its bottom
        // padding moves to the eventual last fragment. Reserving the full
        // vertical padding here makes the split path overly conservative
        // and shifts one extra line to the next page.
        double innerAvailableHeight = Math.max(0.0, request.remainingHeight() - node.padding().top());
        int maxLines = maxLinesThatFit(layout.visualLines(), layout.lineGap(), innerAvailableHeight);
        if (maxLines <= 0) {
            return new PreparedSplitResult<>(null, prepared);
        }
        if (maxLines >= layout.visualLines().size()) {
            return PreparedSplitResult.whole(prepared);
        }

        PreparedNode<ParagraphNode> head = sliceParagraphPreparedNode(node, layout, 0, maxLines, true, false);
        PreparedNode<ParagraphNode> tail = sliceParagraphPreparedNode(node, layout, maxLines, layout.visualLines().size(), false, true);
        return new PreparedSplitResult<>(head, tail);
    }

    /**
     * First-slice content height of a prepared paragraph: the height of its
     * first visual line, which is the smallest unit {@link #splitParagraph}
     * places on a page. Backs
     * {@link com.demcha.compose.document.layout.definitions.ParagraphDefinition#firstSliceHeight}.
     *
     * @param prepared prepared paragraph node
     * @return the first visual line's height, or the whole content height when
     * the paragraph has no visual lines
     */
    public static double paragraphFirstSliceHeight(PreparedNode<ParagraphNode> prepared) {
        PreparedParagraphLayout layout = prepared.requirePreparedLayout(PreparedParagraphLayout.class);
        List<ParagraphLine> lines = layout.visualLines();
        if (lines.isEmpty()) {
            return prepared.measureResult().height();
        }
        return lines.get(0).lineHeight();
    }

    // ------------------------------------------------------------------
    // List entry points
    // ------------------------------------------------------------------

    /**
     * Emits the render fragment for a prepared paragraph.
     *
     * @param prepared  prepared paragraph node
     * @param placement resolved fragment placement
     * @return renderer-facing paragraph fragments
     */
    public static List<LayoutFragment> emitParagraphFragments(PreparedNode<ParagraphNode> prepared,
                                                              FragmentPlacement placement) {
        ParagraphNode node = prepared.node();
        PreparedParagraphLayout layout = prepared.requirePreparedLayout(PreparedParagraphLayout.class);
        ParagraphFragmentPayload payload = new ParagraphFragmentPayload(
                toTextStyle(node.textStyle()),
                node.align(),
                toPadding(node.padding()),
                layout.lineHeight(),
                layout.lineGap(),
                layout.baselineOffset(),
                layout.visualLines(),
                node.linkTarget(),
                layout.emitBookmark() ? node.bookmarkOptions() : null,
                node.verticalAlign());

        LayoutFragment paragraph = new LayoutFragment(
                placement.path(),
                0,
                0.0,
                0.0,
                placement.width(),
                placement.height(),
                payload);
        return NodeDefinitionSupport.withAnchorMarker(
                List.of(paragraph),
                layout.emitAnchor() ? node.anchor() : null,
                placement);
    }

    /**
     * Measures a list node and wraps it into a prepared leaf carrying its
     * per-item layout.
     *
     * <p>This is the one place the public {@code hangingIndent} flag becomes a
     * decision. Below this method each strategy owns its own preparation, and
     * neither re-reads the flag — so the legacy path cannot acquire a branch it
     * has to be re-proven against.</p>
     *
     * @param node        list node to prepare
     * @param ctx         prepare-phase context
     * @param constraints box constraints for measurement
     * @return prepared list node with its item layout
     */
    public static PreparedNode<ListNode> prepareList(ListNode node,
                                                     PrepareContext ctx,
                                                     BoxConstraints constraints) {
        return switch (ListItemLayout.of(node)) {
            case LEGACY_PREFIX -> prepareLegacyPrefixList(node, ctx, constraints);
            case MARKER_CONTENT -> prepareMarkerContentList(node, ctx, constraints);
        };
    }

    /**
     * The v1.4-through-2.3 preparation, unchanged: nested items are flattened
     * into indent-and-marker-prefixed labels, and every item becomes a paragraph
     * whose marker is a text prefix.
     */
    private static PreparedNode<ListNode> prepareLegacyPrefixList(ListNode node,
                                                                  PrepareContext ctx,
                                                                  BoxConstraints constraints) {
        // The list's own marker reaches a flat list without going through the
        // nested walk, so it is checked here as well as there.
        refuseDrawnMarker(node.marker());
        ListNode effective = node.nestedItems().isEmpty()
                ? node
                : flattenNestedListNode(node);
        double innerWidth = Math.max(0.0, constraints.availableWidth() - effective.padding().horizontal());
        PreparedListLayout layout = prepareListLayout(effective, innerWidth, constraints.availableWidth(), ctx.textMeasurement(), ctx.markdownEnabled());
        return PreparedNode.leaf(
                effective,
                new MeasureResult(layout.resolvedWidth(), layout.totalHeight() + effective.padding().vertical()),
                layout);
    }

    /**
     * Marker/content preparation. The normalized depth/marker/content view of
     * the list is built here and attached to the prepared layout.
     *
     * <p>Each item's text is wrapped inside its own {@code contentWidth}, and
     * the marker takes no part in that: it is not a prefix, not a token, and
     * never decides where a line breaks. The node keeps its authored shape —
     * nested items are not flattened into labels here — because depth is
     * geometry in this layout, not characters.</p>
     */
    private static PreparedNode<ListNode> prepareMarkerContentList(ListNode node,
                                                                   PrepareContext ctx,
                                                                   BoxConstraints constraints) {
        double availableItemWidth = Math.max(0.0, constraints.availableWidth() - node.padding().horizontal());
        List<ListItemSpec> specs = ListItemNormalizer.normalize(node);
        List<MarkerContentItem> geometry = ListMarkerGeometry.resolve(
                specs, node, availableItemWidth, ctx.textMeasurement(), prepareDrawnMarkers(specs, node, ctx));

        List<PreparedListItemLayout> items = new ArrayList<>(geometry.size());
        for (MarkerContentItem item : geometry) {
            // A rich item's content is its runs, wrapped by the same inline
            // algorithm a paragraph uses. That is the point of expressing it as
            // runs rather than as a second rich-text model: the marker column,
            // the gap and contentX resolve for it exactly as for a plain item,
            // and the wrapping inside that width is the paragraph's own.
            ParagraphNode content = item.spec().isRich()
                    ? new ParagraphNode(
                            "",
                            item.content(),
                            item.spec().runs(),
                            node.textStyle(),
                            node.align(),
                            node.lineSpacing(),
                            "",
                            DocumentTextIndent.NONE,
                            null,
                            null,
                            DocumentInsets.zero(),
                            DocumentInsets.zero())
                    : new ParagraphNode(
                    "",
                    item.content(),
                    node.textStyle(),
                    node.align(),
                    node.lineSpacing(),
                    "",
                    DocumentTextIndent.NONE,
                    DocumentInsets.zero(),
                    DocumentInsets.zero());
            items.add(new PreparedListItemLayout(
                    item.content(),
                    prepareParagraphLayout(content, item.contentWidth(), ctx.textMeasurement(), ctx.markdownEnabled()),
                    item,
                    true));
        }

        double totalHeight = listItemsHeight(items, node.itemSpacing());
        double maxLineWidth = markerContentMaxLineWidth(items);
        double measuredWidth = Math.min(constraints.availableWidth(), maxLineWidth + node.padding().horizontal());
        double resolvedWidth = node.align() == TextAlign.LEFT
                ? measuredWidth
                : constraints.availableWidth();

        return PreparedNode.leaf(
                node,
                new MeasureResult(resolvedWidth, totalHeight + node.padding().vertical()),
                new PreparedListLayout(items, maxLineWidth, totalHeight, resolvedWidth, geometry));
    }

    /**
     * Measures each distinct drawn marker in the list as one line of inline
     * runs, through the pipeline that measures the items' own content.
     *
     * <p>A marker's width has to be known before any content origin can be, so
     * this runs ahead of {@link ListMarkerGeometry}. Measuring it as a paragraph
     * line rather than as a special case is what makes a disc, an icon and a
     * coloured glyph all resolve to the width of the thing they draw, and what
     * lets the emit phase place the marker's pieces with spans the renderers
     * already know how to draw.</p>
     *
     * <p>Measured unbounded, because a marker is one line by definition and a
     * marker wider than its column overflows — the same answer a too-wide text
     * marker already gets, rather than wrapping into a second line the row has
     * no space for.</p>
     */
    private static Map<ListMarker, ParagraphLine> prepareDrawnMarkers(List<ListItemSpec> specs,
                                                                      ListNode node,
                                                                      PrepareContext ctx) {
        Map<ListMarker, ParagraphLine> lines = null;
        for (ListItemSpec spec : specs) {
            ListMarker marker = spec.marker();
            if (!marker.isRich()) {
                continue;
            }
            if (lines == null) {
                lines = new LinkedHashMap<>();
            } else if (lines.containsKey(marker)) {
                // A list shows the same handful of markers over and over, one per
                // depth, so each distinct one is measured once for the whole list.
                continue;
            }
            ParagraphNode asParagraph = new ParagraphNode(
                    "", "", marker.runs(), node.textStyle(), TextAlign.LEFT, 0.0, "",
                    DocumentTextIndent.NONE, null, null,
                    DocumentInsets.zero(), DocumentInsets.zero());
            PreparedParagraphLayout layout = prepareParagraphLayout(
                    asParagraph, MARKER_MEASURE_WIDTH, ctx.textMeasurement(), false);
            if (layout.visualLines().size() != 1) {
                throw new IllegalStateException(
                        "a list marker is one line: \"" + marker.value() + "\" measured "
                        + layout.visualLines().size() + " lines. A marker sits on the item's "
                        + "first line and shares its baseline, so it has nowhere to put a "
                        + "second one");
            }
            lines.put(marker, layout.visualLines().get(0));
        }
        return lines == null ? Map.of() : lines;
    }

    /**
     * Widest point any row reaches — the marker column and the content column
     * are both candidates, and a marker-only row is measured by its marker.
     */
    private static double markerContentMaxLineWidth(List<PreparedListItemLayout> items) {
        double widest = 0.0;
        for (PreparedListItemLayout item : items) {
            MarkerContentItem geometry = item.geometry();
            widest = Math.max(widest, geometry.markerX() + geometry.measuredMarkerWidth());
            widest = Math.max(widest, geometry.contentX() + item.paragraphLayout().maxLineWidth());
        }
        return widest;
    }

    /**
     * Synthesizes a flat {@link ListNode} from a nested one by walking
     * the tree depth-first and prefixing each label with
     * {@code [indent][marker] }. The synthesized node carries
     * {@code marker = ListMarker.none()} (markers are now baked into
     * each item's prefix) and {@code normalizeMarkers = false} so the
     * baked marker characters are not stripped during paragraph
     * normalization. The existing flat-list rendering pipeline then
     * paginates and emits fragments unchanged.
     *
     * <p>The result is a legacy-shaped node by construction — its markers are
     * characters inside its labels — so it reports {@code hangingIndent = false}
     * whatever the authored node said. The marker/content strategy keeps its own
     * structural view of the same tree in
     * {@link ListItemNormalizer}; it does not read this one.</p>
     */
    private static ListNode flattenNestedListNode(ListNode node) {
        List<String> flatItems = new ArrayList<>();
        flattenNestedItems(node.nestedItems(), 0, flatItems);
        return new ListNode(
                node.name(),
                flatItems,
                List.of(),
                ListMarker.none(),
                node.textStyle(),
                node.align(),
                node.lineSpacing(),
                node.itemSpacing(),
                node.continuationIndent(),
                false,
                node.padding(),
                node.margin(),
                false,
                node.markerGap());
    }

    /**
     * Refuses a drawn marker on the legacy layout, where a marker is characters
     * at the front of the item's text and a drawing has nowhere to go. Its plain
     * reading is empty for a marker that draws only a disc or an icon, so
     * rendering that would leave the list markerless with no signal at all.
     */
    private static void refuseDrawnMarker(ListMarker marker) {
        if (marker != null && marker.isRich()) {
            throw new IllegalStateException(
                    "a drawn list marker needs marker/content geometry: call "
                    + "hangingIndent(true) on the list. The legacy layout puts the marker "
                    + "inside the item's text, which can hold characters but not a drawing");
        }
    }

    private static void flattenNestedItems(List<ListItem> items, int depth, List<String> output) {
        for (ListItem item : items) {
            // A rich item's content is a sequence of independently styled runs and
            // a label is a single string: there is no flattening of one into the
            // other that keeps what the author asked for. Rendering the plain
            // reading instead would silently drop every style, icon and chip,
            // which is a worse answer than saying so. This is the only walk a rich
            // item can reach the legacy layout through, so the check belongs here
            // rather than in a second pass over the tree.
            if (item.isRich()) {
                throw new IllegalStateException(
                        "a list item made of inline runs needs marker/content geometry: call "
                        + "hangingIndent(true) on the list. The legacy layout makes an item's "
                        + "marker part of its text and so carries one style for the whole item, "
                        + "which cannot hold the runs of \"" + item.label() + "\"");
            }
            ListMarker marker = item.marker() != null ? item.marker() : ListMarker.defaultForDepth(depth);
            refuseDrawnMarker(marker);
            StringBuilder prefix = new StringBuilder(NESTED_LIST_INDENT_UNIT.repeat(depth));
            if (marker.isVisible()) {
                // ListMarker.normalize already appends a trailing space
                // when the marker doesn't end in whitespace, so prefix()
                // is "<marker> " and we don't append another space.
                prefix.append(marker.prefix());
            }
            output.add(prefix.append(item.label()).toString());
            if (!item.children().isEmpty()) {
                flattenNestedItems(item.children(), depth + 1, output);
            }
        }
    }

    /**
     * Splits a prepared list at whole-item boundaries, falling back to
     * splitting the first item's lines when no whole item fits.
     *
     * @param prepared prepared list node
     * @param request  split request carrying the remaining height
     * @return the head/tail split result
     */
    public static PreparedSplitResult<ListNode> splitList(PreparedNode<ListNode> prepared,
                                                          SplitRequest request) {
        ListNode node = prepared.node();
        PreparedListLayout layout = prepared.requirePreparedLayout(PreparedListLayout.class);
        if (layout.items().isEmpty()) {
            return PreparedSplitResult.whole(prepared);
        }

        double innerAvailableHeight = Math.max(0.0, request.remainingHeight() - node.padding().top());
        int wholeItemsThatFit = wholeListItemsThatFit(layout.items(), node.itemSpacing(), innerAvailableHeight);
        if (wholeItemsThatFit >= layout.items().size()) {
            return PreparedSplitResult.whole(prepared);
        }
        if (wholeItemsThatFit > 0) {
            PreparedNode<ListNode> head = sliceListPreparedNode(
                    node,
                    layout,
                    layout.items().subList(0, wholeItemsThatFit),
                    true,
                    false);
            PreparedNode<ListNode> tail = sliceListPreparedNode(
                    node,
                    layout,
                    layout.items().subList(wholeItemsThatFit, layout.items().size()),
                    false,
                    true);
            return new PreparedSplitResult<>(head, tail);
        }

        PreparedListItemLayout firstItem = layout.items().get(0);
        PreparedParagraphLayout itemLayout = firstItem.paragraphLayout();
        int maxLines = maxLinesThatFit(
                itemLayout.visualLines(),
                itemLayout.lineGap(),
                innerAvailableHeight);
        if (maxLines <= 0) {
            return new PreparedSplitResult<>(null, prepared);
        }
        if (maxLines >= itemLayout.visualLines().size()) {
            return PreparedSplitResult.whole(prepared);
        }

        PreparedListItemLayout headItem = sliceListItem(firstItem, 0, maxLines);
        PreparedListItemLayout tailItem = sliceListItem(firstItem, maxLines, itemLayout.visualLines().size());
        List<PreparedListItemLayout> tailItems = new ArrayList<>();
        if (tailItem != null) {
            tailItems.add(tailItem);
        }
        tailItems.addAll(layout.items().subList(1, layout.items().size()));

        PreparedNode<ListNode> head = sliceListPreparedNode(node, layout, List.of(headItem), true, false);
        PreparedNode<ListNode> tail = tailItems.isEmpty()
                ? null
                : sliceListPreparedNode(node, layout, tailItems, false, true);
        return new PreparedSplitResult<>(head, tail);
    }

    /**
     * First-slice content height of a prepared list: the height of its first
     * item. Backs
     * {@link com.demcha.compose.document.layout.definitions.ListDefinition#firstSliceHeight}
     * &mdash; the leading unit that anchors a keep-with-next heading to a
     * page-spanning list is its first item.
     *
     * @param prepared prepared list node
     * @return the first item's height, or the whole content height for an empty
     * list
     */
    public static double listFirstSliceHeight(PreparedNode<ListNode> prepared) {
        PreparedListLayout layout = prepared.requirePreparedLayout(PreparedListLayout.class);
        List<PreparedListItemLayout> items = layout.items();
        if (items.isEmpty()) {
            return prepared.measureResult().height();
        }
        return items.get(0).paragraphLayout().totalHeight();
    }

    /**
     * Emits one paragraph fragment per list item so items paginate
     * independently.
     *
     * @param prepared  prepared list node
     * @param placement resolved fragment placement
     * @return renderer-facing per-item fragments
     */
    public static List<LayoutFragment> emitListFragments(PreparedNode<ListNode> prepared,
                                                         FragmentPlacement placement) {
        ListNode node = prepared.node();
        PreparedListLayout layout = prepared.requirePreparedLayout(PreparedListLayout.class);
        if (layout.items().isEmpty()) {
            return List.of();
        }

        List<LayoutFragment> fragments = new ArrayList<>(layout.items().size());
        double boxHeight = layout.totalHeight() + node.padding().vertical();
        double itemTopOffset = 0.0;
        int fragmentIndex = 0;

        for (int itemIndex = 0; itemIndex < layout.items().size(); itemIndex++) {
            PreparedListItemLayout item = layout.items().get(itemIndex);
            PreparedParagraphLayout itemLayout = item.paragraphLayout();
            double itemHeight = itemLayout.totalHeight();
            Padding itemPadding = itemPadding(node, itemIndex, layout.items().size());
            double fragmentHeight = itemHeight + itemPadding.vertical();
            double localY = boxHeight - itemTopOffset - fragmentHeight;
            MarkerContentItem geometry = item.geometry();

            if (geometry == null) {
                // Legacy: one fragment spanning the row, marker inside the text.
                fragments.add(new LayoutFragment(
                        placement.path(), fragmentIndex++, 0.0, localY,
                        placement.width(), fragmentHeight,
                        paragraphPayload(node, node.align(), itemPadding, itemLayout, itemLayout.visualLines())));
            } else {
                // The marker sits in its own column and the text in another, so
                // the two are separate fragments at separate x. They share one
                // box: same localY, same height, same vertical padding — which is
                // what puts the marker on the first content line's baseline
                // instead of starting a second flow beside it.
                Padding sides = new Padding(itemPadding.top(), 0.0, itemPadding.bottom(), 0.0);
                if (item.drawsMarker() && !itemLayout.visualLines().isEmpty()) {
                    fragments.add(new LayoutFragment(
                            placement.path(), fragmentIndex++,
                            node.padding().left() + geometry.markerX(), localY,
                            geometry.measuredMarkerWidth(), fragmentHeight,
                            // Always LEFT: the marker's column is geometry, and a
                            // centred or right-aligned list aligns its text inside
                            // the content column without moving the marker.
                            paragraphPayload(node, TextAlign.LEFT, sides, itemLayout,
                                    List.of(markerLine(node, itemLayout, geometry)))));
                }
                fragments.add(new LayoutFragment(
                        placement.path(), fragmentIndex++,
                        node.padding().left() + geometry.contentX(), localY,
                        geometry.contentWidth(), fragmentHeight,
                        paragraphPayload(node, node.align(), sides, itemLayout, itemLayout.visualLines())));
            }
            itemTopOffset += fragmentHeight + node.itemSpacing();
        }

        return List.copyOf(fragments);
    }

    /**
     * The marker as a single measured line, built from the content's own first
     * line rather than measured again.
     *
     * <p>Its width is the one already resolved in the item's geometry, and its
     * line metrics are copied from the first content line — so the marker does
     * not merely land near that line's baseline, it is placed by the same
     * numbers and shares it by construction. Nothing here re-measures text, and
     * the marker never becomes a second block of flow: it has one line, in a box
     * that is the content's box.</p>
     */
    private static ParagraphLine markerLine(ListNode node,
                                            PreparedParagraphLayout itemLayout,
                                            MarkerContentItem geometry) {
        ParagraphLine first = itemLayout.visualLines().get(0);
        String text = geometry.markerText();
        // A drawn marker's pieces were measured already, as inline runs. They are
        // placed in a line whose metrics are still the content's first line, so a
        // disc or an icon rides the item's own baseline exactly as a glyph does
        // and does not make the row taller; one drawn larger than the line
        // overflows it, the answer a marker wider than its column already gets.
        List<ParagraphSpan> spans = geometry.hasDrawnMarker()
                ? geometry.markerSpans()
                : List.of(new ParagraphTextSpan(
                        text,
                        toTextStyle(node.textStyle()),
                        geometry.measuredMarkerWidth(),
                        first.textLineHeight(),
                        null,
                        null,
                        false));
        return new ParagraphLine(
                text,
                geometry.measuredMarkerWidth(),
                first.lineHeight(),
                first.textLineHeight(),
                first.textAscent(),
                first.baselineOffsetFromBottom(),
                spans);
    }

    private static ParagraphFragmentPayload paragraphPayload(ListNode node,
                                                             TextAlign align,
                                                             Padding padding,
                                                             PreparedParagraphLayout metrics,
                                                             List<ParagraphLine> lines) {
        return new ParagraphFragmentPayload(
                toTextStyle(node.textStyle()),
                align,
                padding,
                metrics.lineHeight(),
                metrics.lineGap(),
                metrics.baselineOffset(),
                lines,
                null,
                null,
                TextVerticalAlign.DEFAULT);
    }

    // ------------------------------------------------------------------
    // List helpers
    // ------------------------------------------------------------------

    private static PreparedListLayout prepareListLayout(ListNode node,
                                                        double innerWidth,
                                                        double availableWidth,
                                                        TextMeasurementSystem measurement,
                                                        boolean markdownEnabled) {
        List<PreparedListItemLayout> items = new ArrayList<>();
        for (String item : node.items()) {
            String normalizedItem = ListMarker.normalizeItemText(item, node.normalizeMarkers());
            if (normalizedItem.isBlank()) {
                continue;
            }
            ParagraphNode paragraph = new ParagraphNode(
                    "",
                    normalizedItem,
                    node.textStyle(),
                    node.align(),
                    node.lineSpacing(),
                    listParagraphPrefix(node),
                    listParagraphIndentStrategy(node),
                    DocumentInsets.zero(),
                    DocumentInsets.zero());
            items.add(new PreparedListItemLayout(
                    normalizedItem,
                    prepareParagraphLayout(paragraph, innerWidth, measurement, markdownEnabled)));
        }

        double maxLineWidth = maxListLineWidth(items);
        double totalHeight = listItemsHeight(items, node.itemSpacing());
        double measuredWidth = Math.min(availableWidth, maxLineWidth + node.padding().horizontal());
        double resolvedWidth = node.align() == TextAlign.LEFT
                ? measuredWidth
                : availableWidth;
        return new PreparedListLayout(List.copyOf(items), maxLineWidth, totalHeight, resolvedWidth);
    }

    private static PreparedNode<ListNode> sliceListPreparedNode(ListNode source,
                                                                PreparedListLayout sourceLayout,
                                                                List<PreparedListItemLayout> items,
                                                                boolean keepTopInsets,
                                                                boolean keepBottomInsets) {
        List<PreparedListItemLayout> safeItems = List.copyOf(items);
        // A slice is the same list with fewer rows, so it measures the way the
        // whole list did. Under marker/content that means counting the marker
        // column the rows are placed into — measuring only their text would give
        // the slice a box narrower than what it draws, and the text would hang
        // past its own right edge as soon as a list paginated.
        double maxLineWidth = safeItems.isEmpty() || safeItems.get(0).geometry() == null
                ? maxListLineWidth(safeItems)
                : markerContentMaxLineWidth(safeItems);
        double totalHeight = listItemsHeight(safeItems, source.itemSpacing());
        DocumentInsets padding = new DocumentInsets(
                keepTopInsets ? source.padding().top() : 0.0,
                source.padding().right(),
                keepBottomInsets ? source.padding().bottom() : 0.0,
                source.padding().left());
        DocumentInsets margin = new DocumentInsets(
                keepTopInsets ? source.margin().top() : 0.0,
                source.margin().right(),
                keepBottomInsets ? source.margin().bottom() : 0.0,
                source.margin().left());
        double resolvedWidth = source.align() == TextAlign.LEFT
                ? maxLineWidth + padding.horizontal()
                : sourceLayout.resolvedWidth();

        // A slice is the same list with fewer rows, so it keeps the authored
        // layout intent. Dropping it here would leave a paginated list's tail
        // disagreeing with its head about which strategy it is.
        ListNode fragmentNode = new ListNode(
                source.name(),
                safeItems.stream().map(PreparedListItemLayout::text).toList(),
                List.of(),
                source.marker(),
                source.textStyle(),
                source.align(),
                source.lineSpacing(),
                source.itemSpacing(),
                source.continuationIndent(),
                false,
                padding,
                margin,
                source.hangingIndent(),
                source.markerGap());
        PreparedListLayout fragmentLayout = new PreparedListLayout(
                safeItems,
                maxLineWidth,
                totalHeight,
                resolvedWidth);
        return PreparedNode.leaf(
                fragmentNode,
                new MeasureResult(resolvedWidth, totalHeight + padding.vertical()),
                fragmentLayout);
    }

    private static Padding itemPadding(ListNode node, int itemIndex, int itemCount) {
        return new Padding(
                itemIndex == 0 ? node.padding().top() : 0.0,
                node.padding().right(),
                itemIndex == itemCount - 1 ? node.padding().bottom() : 0.0,
                node.padding().left());
    }

    private static String listParagraphPrefix(ListNode node) {
        return node.marker().isVisible()
                ? node.marker().prefix()
                : node.continuationIndent();
    }

    private static DocumentTextIndent listParagraphIndentStrategy(ListNode node) {
        if (node.marker().isVisible()) {
            return DocumentTextIndent.ALL_LINES;
        }
        return node.continuationIndent().isEmpty()
                ? DocumentTextIndent.NONE
                : DocumentTextIndent.FROM_SECOND_LINE;
    }

    private static int wholeListItemsThatFit(List<PreparedListItemLayout> items,
                                             double itemSpacing,
                                             double availableHeight) {
        int count = 0;
        double used = 0.0;
        for (PreparedListItemLayout item : items) {
            double addition = item.paragraphLayout().totalHeight();
            if (count > 0) {
                addition += itemSpacing;
            }
            if (used + addition > availableHeight + EPS) {
                break;
            }
            used += addition;
            count++;
        }
        return count;
    }

    private static PreparedListItemLayout sliceListItem(PreparedListItemLayout item,
                                                        int fromInclusive,
                                                        int toExclusive) {
        PreparedParagraphLayout source = item.paragraphLayout();
        if (fromInclusive >= toExclusive) {
            return null;
        }
        List<ParagraphLine> lines = List.copyOf(source.visualLines().subList(fromInclusive, toExclusive));
        List<String> logicalLines = lines.stream()
                .map(ParagraphLine::text)
                .toList();
        double maxLineWidth = lines.stream()
                .mapToDouble(ParagraphLine::width)
                .max()
                .orElse(0.0);
        double totalHeight = source.lineHeight() * lines.size()
                             + Math.max(0, lines.size() - 1) * source.lineGap();
        PreparedParagraphLayout layout = new PreparedParagraphLayout(
                logicalLines,
                lines,
                source.lineMetrics(),
                source.baselineOffset(),
                source.lineHeight(),
                source.lineGap(),
                maxLineWidth,
                totalHeight,
                false,
                false);
        String slicedText = String.join("\n", logicalLines);
        // Only a slice that begins at line 0 is still the start of the authored
        // item; anything past it is a continuation and must not repeat a marker.
        return fromInclusive == 0
                ? item.startingAs(slicedText, layout)
                : item.continuedAs(slicedText, layout);
    }

    private static double maxListLineWidth(List<PreparedListItemLayout> items) {
        return items.stream()
                .map(PreparedListItemLayout::paragraphLayout)
                .mapToDouble(PreparedParagraphLayout::maxLineWidth)
                .max()
                .orElse(0.0);
    }

    private static double listItemsHeight(List<PreparedListItemLayout> items, double itemSpacing) {
        if (items.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (int index = 0; index < items.size(); index++) {
            total += items.get(index).paragraphLayout().totalHeight();
            if (index < items.size() - 1) {
                total += itemSpacing;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Paragraph layout core
    // ------------------------------------------------------------------

    private static DocumentTextStyle resolveAutoSizeTextStyle(ParagraphNode node,
                                                              double innerWidth,
                                                              TextMeasurementSystem measurement) {
        DocumentTextAutoSize autoSize = node.autoSize();
        if (autoSize == null) {
            return node.textStyle();
        }
        DocumentTextStyle baseStyle = node.textStyle();
        double maxSize = autoSize.maxSize();
        double minSize = autoSize.minSize();
        double step = Math.max(0.1, autoSize.step());

        // Single-line text: pick the largest grid size (maxSize, maxSize-step, …,
        // down to >= minSize) whose longest logical line measures inside the
        // available inner width, otherwise fall back to the smallest configured
        // size. The fit predicate is monotonic in size (line width is linear in
        // size), so binary-search the grid for the boundary instead of measuring
        // at every step — the same size the linear scan returned, in ~log2(n)
        // measurements rather than n.
        int maxStepCount = (int) Math.floor((maxSize - minSize + 1e-6) / step);
        int lo = 0;
        int hi = maxStepCount;
        int fitStep = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            DocumentTextStyle candidate = baseStyle.withSize(maxSize - mid * step);
            if (paragraphFitsSingleLine(node, candidate, innerWidth, measurement)) {
                fitStep = mid;      // fits — try a larger size (fewer steps down)
                hi = mid - 1;
            } else {
                lo = mid + 1;       // too wide — need a smaller size (more steps)
            }
        }
        if (fitStep >= 0) {
            return baseStyle.withSize(maxSize - fitStep * step);
        }
        return baseStyle.withSize(minSize);
    }

    private static boolean paragraphFitsSingleLine(ParagraphNode node,
                                                   DocumentTextStyle candidate,
                                                   double innerWidth,
                                                   TextMeasurementSystem measurement) {
        TextStyle engineStyle = toTextStyle(candidate);
        if (!node.inlineRuns().isEmpty()) {
            double width = 0.0;
            for (InlineRun run : node.inlineRuns()) {
                if (run instanceof InlineTextRun textRun) {
                    width += measurement.textWidth(engineStyle, ArabicShaper.shape(textRun.text()));
                } else if (run instanceof InlineImageRun imageRun) {
                    width += imageRun.width();
                } else if (run instanceof InlineShapeRun shapeRun) {
                    width += shapeRun.width();
                } else if (run instanceof InlineSvgRun svgRun) {
                    width += svgRun.width();
                } else if (run instanceof InlineHighlightRun highlight) {
                    // Shaped, like the token this run becomes. An unshaped measurement
                    // sizes the text off one string and lays it out from another: the
                    // presentation forms carry their own advance widths, so the probe
                    // was answering about a string the layout never sees. Not currently
                    // observable end to end — auto-size does not shrink a paragraph
                    // holding a chip at all, for any script — but the two paths have to
                    // agree regardless of which of them is reached first.
                    width += measurement.textWidth(engineStyle, ArabicShaper.shape(highlight.text()))
                            + highlight.background().padding().horizontal();
                }
            }
            return width <= innerWidth;
        }
        List<String> lines = shapeAll(sanitizeLogicalLines(node.text()));
        if (lines.size() != 1) {
            return false;
        }
        // Auto-size measurement is intentionally approximate when markdown is
        // enabled: the raw source includes formatting markers that add a few
        // characters of width, which keeps the search slightly conservative.
        return measurement.textWidth(engineStyle, lines.get(0)) <= innerWidth;
    }

    private static PreparedParagraphLayout prepareParagraphLayout(ParagraphNode node,
                                                                  double innerWidth,
                                                                  TextMeasurementSystem measurement,
                                                                  boolean markdownEnabled) {
        // Shaped before wrapping, because the contextual forms have their own advance
        // widths and everything downstream — the fit tests, the spans, the page — must
        // measure the text that will be drawn. Text without an Arabic letter passes
        // through as the same instances.
        List<String> logicalLines = shapeAll(sanitizeLogicalLines(node.text()));
        boolean useMarkdownLayout = markdownEnabled && logicalLines.stream().anyMatch(ParagraphWrapping::containsMarkdownSyntax);
        TextStyle textStyle = node.autoSize() != null
                ? toTextStyle(resolveAutoSizeTextStyle(node, innerWidth, measurement))
                : toTextStyle(node.textStyle());
        TextIndentStrategy indentStrategy = toIndentStrategy(node.indentStrategy());
        TextMeasurementSystem.LineMetrics lineMetrics = measurement.lineMetrics(textStyle);
        List<ParagraphLine> visualLines = !node.inlineRuns().isEmpty()
                ? ParagraphWrapping.wrapInlineParagraph(
                node.inlineRuns(),
                textStyle,
                lineMetrics,
                Math.max(0.0, innerWidth),
                node.bulletOffset(),
                indentStrategy,
                measurement,
                resolveBaseDirection(node))
                : useMarkdownLayout
                  ? ParagraphWrapping.wrapMarkdownParagraph(
                logicalLines,
                textStyle,
                lineMetrics,
                Math.max(0.0, innerWidth),
                node.bulletOffset(),
                indentStrategy,
                measurement,
                resolveBaseDirection(node))
                  : ParagraphWrapping.toParagraphLines(
                ParagraphWrapping.wrapParagraph(
                        logicalLines,
                        textStyle,
                        Math.max(0.0, innerWidth),
                        node.bulletOffset(),
                        indentStrategy,
                        measurement),
                textStyle,
                lineMetrics,
                measurement,
                resolveBaseDirection(node));
        if (visualLines.isEmpty()) {
            visualLines = List.of(ParagraphWrapping.emptyParagraphLine(lineMetrics));
        }

        double lineHeight = lineMetrics.lineHeight();
        double gap = node.lineSpacing();
        int lineCount = visualLines.size();
        double totalHeight = 0.0;
        for (ParagraphLine line : visualLines) {
            totalHeight += line.lineHeight();
        }
        if (lineCount > 1) {
            totalHeight += (lineCount - 1) * gap;
        }
        double maxLineWidth = visualLines.stream()
                .mapToDouble(ParagraphLine::width)
                .max()
                .orElse(0.0);

        return new PreparedParagraphLayout(
                List.copyOf(logicalLines),
                List.copyOf(visualLines),
                lineMetrics,
                lineMetrics.baselineOffsetFromBottom(),
                lineHeight,
                gap,
                maxLineWidth,
                totalHeight,
                node.bookmarkOptions() != null,
                node.anchor() != null);
    }

    private static PreparedNode<ParagraphNode> sliceParagraphPreparedNode(ParagraphNode source,
                                                                          PreparedParagraphLayout layout,
                                                                          int fromInclusive,
                                                                          int toExclusive,
                                                                          boolean keepTopInsets,
                                                                          boolean keepBottomInsets) {
        List<ParagraphLine> slice = List.copyOf(layout.visualLines().subList(fromInclusive, toExclusive));
        List<String> sliceLogicalLines = slice.stream()
                .map(ParagraphLine::text)
                .toList();
        double maxLineWidth = slice.stream()
                .mapToDouble(ParagraphLine::width)
                .max()
                .orElse(0.0);
        double totalHeight = 0.0;
        for (ParagraphLine line : slice) {
            totalHeight += line.lineHeight();
        }
        if (slice.size() > 1) {
            totalHeight += (slice.size() - 1) * layout.lineGap();
        }

        ParagraphNode fragmentNode = new ParagraphNode(
                source.name(),
                String.join("\n", sliceLogicalLines),
                source.inlineRuns(),
                source.textStyle(),
                source.align(),
                source.lineSpacing(),
                "",
                DocumentTextIndent.NONE,
                source.linkTarget(),
                keepTopInsets && layout.emitBookmark() ? source.bookmarkOptions() : null,
                new DocumentInsets(
                        keepTopInsets ? source.padding().top() : 0.0,
                        source.padding().right(),
                        keepBottomInsets ? source.padding().bottom() : 0.0,
                        source.padding().left()),
                new DocumentInsets(
                        keepTopInsets ? source.margin().top() : 0.0,
                        source.margin().right(),
                        keepBottomInsets ? source.margin().bottom() : 0.0,
                        source.margin().left()),
                null,
                source.verticalAlign(),
                keepTopInsets ? source.anchor() : null,
                source.direction());

        PreparedParagraphLayout fragmentLayout = new PreparedParagraphLayout(
                List.copyOf(sliceLogicalLines),
                slice,
                layout.lineMetrics(),
                layout.baselineOffset(),
                layout.lineHeight(),
                layout.lineGap(),
                maxLineWidth,
                totalHeight,
                keepTopInsets && layout.emitBookmark(),
                keepTopInsets && layout.emitAnchor());

        MeasureResult measure = new MeasureResult(
                maxLineWidth + fragmentNode.padding().horizontal(),
                totalHeight + fragmentNode.padding().vertical());
        return PreparedNode.leaf(fragmentNode, measure, fragmentLayout);
    }

    /**
     * Resolves the paragraph's base direction, once, from the whole paragraph.
     *
     * <p>UAX #9 fixes the base direction per paragraph (rules P2–P3); only the
     * line-level reset (L1) is per line. {@link TextDirection#AUTO} is therefore
     * decided here, from the paragraph's full text, and every wrapped line receives
     * the same base. Resolving it per line instead would let a continuation line that
     * happens to begin with Latin flip its base mid-paragraph — the same Hebrew prose
     * laid out right-to-left on one line and left-to-right on the next.</p>
     *
     * <p>This is also the rule {@code ParagraphBuilder} applies when it derives the
     * default alignment for {@code AUTO}, so what the page does agrees with where the
     * builder put it.</p>
     */
    private static BidiParagraphResolver.BaseDirection resolveBaseDirection(ParagraphNode node) {
        return ParagraphDirection.baseDirection(node);
    }

    private static List<String> shapeAll(List<String> logicalLines) {
        List<String> shaped = null;
        for (int index = 0; index < logicalLines.size(); index++) {
            String line = logicalLines.get(index);
            String shapedLine = ArabicShaper.shape(line);
            if (shapedLine != line && shaped == null) {
                shaped = new ArrayList<>(logicalLines);
            }
            if (shaped != null) {
                shaped.set(index, shapedLine);
            }
        }
        return shaped == null ? logicalLines : List.copyOf(shaped);
    }

    private static List<String> sanitizeLogicalLines(String rawText) {
        String safeText = rawText == null ? "" : rawText.replace("\r\n", "\n").replace('\r', '\n');
        String[] logicalLines = safeText.split("\n", -1);
        List<String> sanitized = new ArrayList<>(logicalLines.length);
        for (String logicalLine : logicalLines) {
            // The bidirectional formatting characters survive this pass: they are the
            // author's instruction to the layout, and they are dropped once it has
            // been read — at the span, and at the glyph seam that measures and draws.
            sanitized.add(TextControlSanitizer.removeExceptFormattingControls(logicalLine));
        }
        return List.copyOf(sanitized);
    }

    private static int maxLinesThatFit(List<ParagraphLine> lines, double lineGap, double availableHeight) {
        if (lines.isEmpty()) {
            return 0;
        }
        if (availableHeight + EPS < lines.get(0).lineHeight()) {
            return 0;
        }

        int count = 0;
        double used = 0.0;
        for (ParagraphLine line : lines) {
            double addition = count == 0 ? line.lineHeight() : lineGap + line.lineHeight();
            if (used + addition > availableHeight + EPS) {
                break;
            }
            used += addition;
            count++;
        }
        return count;
    }

}
