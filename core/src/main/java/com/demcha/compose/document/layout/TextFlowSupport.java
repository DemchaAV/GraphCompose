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
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.List;

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
     * per-item paragraph layout, flattening nested items first when present.
     *
     * @param node        list node to prepare
     * @param ctx         prepare-phase context
     * @param constraints box constraints for measurement
     * @return prepared list node with its item layout
     */
    public static PreparedNode<ListNode> prepareList(ListNode node,
                                                     PrepareContext ctx,
                                                     BoxConstraints constraints) {
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
     * Synthesizes a flat {@link ListNode} from a nested one by walking
     * the tree depth-first and prefixing each label with
     * {@code [indent][marker] }. The synthesized node carries
     * {@code marker = ListMarker.none()} (markers are now baked into
     * each item's prefix) and {@code normalizeMarkers = false} so the
     * baked marker characters are not stripped during paragraph
     * normalization. The existing flat-list rendering pipeline then
     * paginates and emits fragments unchanged.
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
                node.margin());
    }

    private static void flattenNestedItems(List<ListItem> items, int depth, List<String> output) {
        for (ListItem item : items) {
            ListMarker marker = item.marker() != null ? item.marker() : ListMarker.defaultForDepth(depth);
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

        for (int itemIndex = 0; itemIndex < layout.items().size(); itemIndex++) {
            PreparedParagraphLayout itemLayout = layout.items().get(itemIndex).paragraphLayout();
            double itemHeight = itemLayout.totalHeight();
            Padding itemPadding = itemPadding(node, itemIndex, layout.items().size());
            double fragmentHeight = itemHeight + itemPadding.vertical();
            double localY = boxHeight - itemTopOffset - fragmentHeight;
            fragments.add(new LayoutFragment(
                    placement.path(),
                    itemIndex,
                    0.0,
                    localY,
                    placement.width(),
                    fragmentHeight,
                    new ParagraphFragmentPayload(
                            toTextStyle(node.textStyle()),
                            node.align(),
                            itemPadding,
                            itemLayout.lineHeight(),
                            itemLayout.lineGap(),
                            itemLayout.baselineOffset(),
                            itemLayout.visualLines(),
                            null,
                            null,
                            TextVerticalAlign.DEFAULT)));
            itemTopOffset += fragmentHeight + node.itemSpacing();
        }

        return List.copyOf(fragments);
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
        double maxLineWidth = maxListLineWidth(safeItems);
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

        ListNode fragmentNode = new ListNode(
                source.name(),
                safeItems.stream().map(PreparedListItemLayout::text).toList(),
                source.marker(),
                source.textStyle(),
                source.align(),
                source.lineSpacing(),
                source.itemSpacing(),
                source.continuationIndent(),
                false,
                padding,
                margin);
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
        return new PreparedListItemLayout(String.join("\n", logicalLines), layout);
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
                    width += measurement.textWidth(engineStyle, textRun.text());
                } else if (run instanceof InlineImageRun imageRun) {
                    width += imageRun.width();
                } else if (run instanceof InlineShapeRun shapeRun) {
                    width += shapeRun.width();
                } else if (run instanceof InlineSvgRun svgRun) {
                    width += svgRun.width();
                } else if (run instanceof InlineHighlightRun highlight) {
                    width += measurement.textWidth(engineStyle, highlight.text())
                            + highlight.background().padding().horizontal();
                }
            }
            return width <= innerWidth;
        }
        List<String> lines = sanitizeLogicalLines(node.text());
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
        List<String> logicalLines = sanitizeLogicalLines(node.text());
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
        TextDirection direction = node.direction();
        if (direction == TextDirection.RTL) {
            return BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT;
        }
        if (direction == TextDirection.AUTO) {
            int baseLevel = BidiParagraphResolver.baseLevel(
                    node.text(), BidiParagraphResolver.BaseDirection.FIRST_STRONG_CHARACTER);
            return BidiParagraphResolver.isRightToLeftLevel(baseLevel)
                    ? BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT
                    : BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT;
        }
        return BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT;
    }

    private static List<String> sanitizeLogicalLines(String rawText) {
        String safeText = rawText == null ? "" : rawText.replace("\r\n", "\n").replace('\r', '\n');
        String[] logicalLines = safeText.split("\n", -1);
        List<String> sanitized = new ArrayList<>(logicalLines.length);
        for (String logicalLine : logicalLines) {
            // The bidirectional formatting characters survive this pass: they are the
            // author's instruction to the layout, and they are dropped once it has
            // been read — at the span, and at the glyph seam that measures and draws.
            sanitized.add(TextControlSanitizer.removeExceptDirectionMarks(logicalLine));
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
