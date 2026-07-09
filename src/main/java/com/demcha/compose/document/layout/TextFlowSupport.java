package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.*;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.renderable.BlockText;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.text.markdown.MarkDownParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        boolean useMarkdownLayout = markdownEnabled && logicalLines.stream().anyMatch(TextFlowSupport::containsMarkdownSyntax);
        TextStyle textStyle = node.autoSize() != null
                ? toTextStyle(resolveAutoSizeTextStyle(node, innerWidth, measurement))
                : toTextStyle(node.textStyle());
        TextIndentStrategy indentStrategy = toIndentStrategy(node.indentStrategy());
        TextMeasurementSystem.LineMetrics lineMetrics = measurement.lineMetrics(textStyle);
        List<ParagraphLine> visualLines = !node.inlineRuns().isEmpty()
                ? wrapInlineParagraph(
                node.inlineRuns(),
                textStyle,
                lineMetrics,
                Math.max(0.0, innerWidth),
                node.bulletOffset(),
                indentStrategy,
                measurement)
                : useMarkdownLayout
                  ? wrapMarkdownParagraph(
                logicalLines,
                textStyle,
                lineMetrics,
                Math.max(0.0, innerWidth),
                node.bulletOffset(),
                indentStrategy,
                measurement)
                  : toParagraphLines(
                wrapParagraph(
                        logicalLines,
                        textStyle,
                        Math.max(0.0, innerWidth),
                        node.bulletOffset(),
                        indentStrategy,
                        measurement),
                textStyle,
                lineMetrics,
                measurement);
        if (visualLines.isEmpty()) {
            visualLines = List.of(emptyParagraphLine(lineMetrics));
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

    private static ParagraphLine emptyParagraphLine(TextMeasurementSystem.LineMetrics metrics) {
        return new ParagraphLine(
                "",
                0.0,
                metrics.lineHeight(),
                metrics.lineHeight(),
                metrics.ascent(),
                metrics.baselineOffsetFromBottom(),
                List.of());
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
                keepTopInsets ? source.anchor() : null);

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

    private static List<String> sanitizeLogicalLines(String rawText) {
        String safeText = rawText == null ? "" : rawText.replace("\r\n", "\n").replace('\r', '\n');
        String[] logicalLines = safeText.split("\n", -1);
        List<String> sanitized = new ArrayList<>(logicalLines.length);
        for (String logicalLine : logicalLines) {
            sanitized.add(BlockText.sanitizeText(logicalLine));
        }
        return List.copyOf(sanitized);
    }

    // ------------------------------------------------------------------
    // Wrapping primitives
    // ------------------------------------------------------------------

    private static List<String> wrapParagraph(List<String> logicalLines,
                                              TextStyle style,
                                              double maxWidth,
                                              String bulletOffset,
                                              TextIndentStrategy indentStrategy,
                                              TextMeasurementSystem measurement) {
        List<String> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, style, measurement);

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            String logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty()) {
                result.add("");
                continue;
            }
            if (maxWidth <= EPS) {
                result.add("");
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<String> tokens = tokenize(logicalLine);
            String currentPrefix = initialPrefix;
            // currentLine is assembled in a reused StringBuilder: appending a
            // token is amortised O(1), whereas concatenating Strings re-copied
            // the whole growing line on every token (O(chars^2) char copies plus
            // a fresh throwaway String each step). The character sequence is
            // identical to the old `+` assembly, so wrapping stays byte-for-byte
            // the same; we only materialise a String via toString() when a line
            // is emitted (which the result list needs anyway).
            StringBuilder currentLine = new StringBuilder(initialPrefix);
            // Running width of currentLine. The greedy fit only needs the width
            // of the line built so far plus the next token, not a fresh
            // measurement of the whole growing prefix on every token (which made
            // wrapping O(chars per line x tokens) measured characters). PDFBox
            // glyph advances are additive here (no kerning), so accumulating
            // per-token widths matches measuring the full string to well within
            // the EPS the fit test already tolerates; each new line re-measures
            // its (short) start to pin any floating-point drift.
            double currentWidth = measurement.textWidth(style, initialPrefix);
            boolean hasContent = false;

            for (String token : tokens) {
                String nextToken = hasContent ? token : token.stripLeading();
                if (nextToken.isEmpty()) {
                    continue;
                }

                double nextTokenWidth = measurement.textWidth(style, nextToken);
                if (currentWidth + nextTokenWidth <= maxWidth + EPS) {
                    currentLine.append(nextToken);
                    currentWidth += nextTokenWidth;
                    hasContent = true;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                String strippedToken = nextToken.stripLeading();
                double strippedTokenWidth = measurement.textWidth(style, strippedToken);
                if (hasContent) {
                    result.add(trimTrailingSpaces(currentLine.toString()));
                    currentPrefix = continuationPrefix;
                    currentLine.setLength(0);
                    currentLine.append(continuationPrefix);
                    currentWidth = measurement.textWidth(style, continuationPrefix);
                    hasContent = false;

                    if (currentWidth + strippedTokenWidth <= maxWidth + EPS) {
                        currentLine.setLength(0);
                        currentLine.append(currentPrefix).append(strippedToken);
                        currentWidth += strippedTokenWidth;
                        hasContent = true;
                        continue;
                    }
                }

                // Over-wide on a fresh (or already empty) line: break it at soft seams,
                // char-splitting as a last resort.
                double availableWidth = availableWidthForPrefix(maxWidth, currentPrefix, style, measurement);
                List<String> chunks = TokenBreaking.breakLongToken(strippedToken, style, availableWidth, measurement);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int index = 0; index < chunks.size() - 1; index++) {
                    result.add(currentPrefix + chunks.get(index));
                    currentPrefix = continuationPrefix;
                }
                currentLine.setLength(0);
                currentLine.append(currentPrefix).append(chunks.get(chunks.size() - 1));
                currentWidth = measurement.textWidth(style, currentLine.toString());
                hasContent = true;
            }

            result.add(trimTrailingSpaces(currentLine.toString()));
        }

        return List.copyOf(result);
    }

    private static List<ParagraphLine> toParagraphLines(List<String> wrappedLines,
                                                        TextStyle style,
                                                        TextMeasurementSystem.LineMetrics metrics,
                                                        TextMeasurementSystem measurement) {
        List<ParagraphLine> result = new ArrayList<>(wrappedLines.size());
        double textLineHeight = metrics.lineHeight();
        for (String line : wrappedLines) {
            String safeLine = line == null ? "" : line;
            double width = measurement.textWidth(style, safeLine);
            result.add(new ParagraphLine(
                    safeLine,
                    width,
                    textLineHeight,
                    textLineHeight,
                    metrics.ascent(),
                    metrics.baselineOffsetFromBottom(),
                    List.of(new ParagraphTextSpan(safeLine, style, width, textLineHeight))));
        }
        return List.copyOf(result);
    }

    private static List<ParagraphLine> wrapInlineParagraph(List<InlineRun> runs,
                                                           TextStyle defaultStyle,
                                                           TextMeasurementSystem.LineMetrics defaultMetrics,
                                                           double maxWidth,
                                                           String bulletOffset,
                                                           TextIndentStrategy indentStrategy,
                                                           TextMeasurementSystem measurement) {
        List<ParagraphLine> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, defaultStyle, measurement);
        List<List<InlineLayoutToken>> logicalLines = tokenizeInlineRuns(runs, defaultStyle, measurement);

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            List<InlineLayoutToken> logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty() || maxWidth <= EPS) {
                result.add(emptyParagraphLine(defaultMetrics));
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<InlineLayoutToken> currentLine = new ArrayList<>();
            if (!initialPrefix.isEmpty()) {
                currentLine.add(InlineTextToken.of(initialPrefix, defaultStyle, null, measurement));
            }
            double currentWidth = inlineLineWidth(currentLine);

            for (InlineLayoutToken token : logicalLine) {
                InlineLayoutToken sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine, measurement);
                if (sanitizedToken == null) {
                    continue;
                }

                double tokenWidth = sanitizedToken.wrapWidth();
                if (currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                if (!currentLine.isEmpty()) {
                    result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement));
                    currentLine = new ArrayList<>();
                    if (!continuationPrefix.isEmpty()) {
                        currentLine.add(InlineTextToken.of(continuationPrefix, defaultStyle, null, measurement));
                    }
                    currentWidth = inlineLineWidth(currentLine);

                    sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine, measurement);
                    if (sanitizedToken == null) {
                        continue;
                    }
                    tokenWidth = sanitizedToken.wrapWidth();
                    if (currentWidth + tokenWidth <= maxWidth + EPS) {
                        currentLine.add(sanitizedToken);
                        currentWidth += tokenWidth;
                        continue;
                    }
                }

                // Still over-wide on a fresh (or already empty) line. Text tokens —
                // plain OR highlight chip — are broken within the column; a graphic
                // (image / shape / SVG) has no break point and is emitted as-is.
                if (!(sanitizedToken instanceof InlineTextToken textToken)) {
                    currentLine.add(sanitizedToken);
                    currentWidth += sanitizedToken.wrapWidth();
                    continue;
                }

                boolean chip = textToken.highlightGroup() != null;
                // A chip fragment paints leadPad + glyphs + trailPad, but the break
                // budgets glyphs only, so reserve the run's padding here to keep the
                // coalesced fill inside the column. Interior fragments under-fill by
                // at most that padding — cosmetic, and only on an over-wide chip.
                double reserve = chip ? textToken.leadPad() + textToken.trailPad() : 0.0;
                List<String> pieces = TokenBreaking.breakLongToken(
                        textToken.text(),
                        textToken.textStyle(),
                        Math.max(1.0, maxWidth - currentWidth - reserve),
                        measurement);
                for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
                    String piece = pieces.get(pieceIndex);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    // Chip pieces keep the run's group + background so the coalescer
                    // paints one fill per fragment; the run's outer pad sits on the
                    // first/last piece only, leaving the break seams open.
                    InlineTextToken chunkToken = chip
                            ? InlineTextToken.ofHighlight(
                                    piece,
                                    textToken.textStyle(),
                                    textToken.linkTarget(),
                                    textToken.background(),
                                    textToken.highlightGroup(),
                                    pieceIndex == 0 ? textToken.leadPad() : 0.0,
                                    pieceIndex == pieces.size() - 1 ? textToken.trailPad() : 0.0,
                                    measurement)
                            : InlineTextToken.of(
                                    piece,
                                    textToken.textStyle(),
                                    textToken.linkTarget(),
                                    measurement);
                    currentLine.add(chunkToken);
                    currentWidth += chunkToken.wrapWidth();

                    if (pieceIndex < pieces.size() - 1) {
                        result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(InlineTextToken.of(continuationPrefix, defaultStyle, null, measurement));
                        }
                        currentWidth = inlineLineWidth(currentLine);
                    }
                }
            }

            result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement));
        }

        return List.copyOf(result);
    }

    private static boolean containsMarkdownSyntax(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.indexOf('*') >= 0
               || value.indexOf('_') >= 0
               || value.indexOf('`') >= 0;
    }

    private static List<ParagraphLine> wrapMarkdownParagraph(List<String> logicalLines,
                                                             TextStyle style,
                                                             TextMeasurementSystem.LineMetrics metrics,
                                                             double maxWidth,
                                                             String bulletOffset,
                                                             TextIndentStrategy indentStrategy,
                                                             TextMeasurementSystem measurement) {
        List<ParagraphLine> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, style, measurement);
        MarkDownParser parser = new MarkDownParser();

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            String logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty() || maxWidth <= EPS) {
                result.add(emptyParagraphLine(metrics));
                continue;
            }

            String initialPrefix = "";
            if (logicalLineIndex == 0) {
                if (indentStrategy.indentFirstLine()) {
                    initialPrefix = indentSpec.firstLinePrefix();
                }
            } else if (indentStrategy.indentWrappedLines()) {
                initialPrefix = indentSpec.continuationPrefix();
            }

            String continuationPrefix = indentStrategy.indentWrappedLines()
                    ? indentSpec.continuationPrefix()
                    : "";

            List<TextDataBody> tokens = tokenizeMarkdownLine(logicalLine, style, parser);
            List<TextDataBody> currentLine = new ArrayList<>();
            if (!initialPrefix.isEmpty()) {
                currentLine.add(new TextDataBody(initialPrefix, style));
            }
            double currentWidth = lineWidth(currentLine, measurement);

            for (TextDataBody token : tokens) {
                TextDataBody sanitizedToken = trimLeadingIfLineStart(token, currentLine);
                if (sanitizedToken == null || sanitizedToken.text().isEmpty()) {
                    continue;
                }

                double tokenWidth = measurement.textWidth(sanitizedToken.textStyle(), sanitizedToken.text());
                if (currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                // Does not fit. If the line already has content, flush it and retry
                // the token on a fresh line before resorting to a break.
                if (!currentLine.isEmpty()) {
                    result.add(toParagraphLine(currentLine, metrics, measurement));
                    currentLine = new ArrayList<>();
                    if (!continuationPrefix.isEmpty()) {
                        currentLine.add(new TextDataBody(continuationPrefix, style));
                    }
                    currentWidth = lineWidth(currentLine, measurement);

                    sanitizedToken = trimLeadingIfLineStart(token, currentLine);
                    if (sanitizedToken == null || sanitizedToken.text().isEmpty()) {
                        continue;
                    }
                    tokenWidth = measurement.textWidth(sanitizedToken.textStyle(), sanitizedToken.text());
                    if (currentWidth + tokenWidth <= maxWidth + EPS) {
                        currentLine.add(sanitizedToken);
                        currentWidth += tokenWidth;
                        continue;
                    }
                }

                List<String> chunks = TokenBreaking.breakLongToken(
                        sanitizedToken.text(),
                        sanitizedToken.textStyle(),
                        Math.max(1.0, maxWidth - currentWidth),
                        measurement);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    String chunk = chunks.get(chunkIndex);
                    if (chunk.isEmpty()) {
                        continue;
                    }
                    TextDataBody chunkBody = new TextDataBody(chunk, sanitizedToken.textStyle());
                    currentLine.add(chunkBody);
                    currentWidth += measurement.textWidth(chunkBody.textStyle(), chunkBody.text());

                    if (chunkIndex < chunks.size() - 1) {
                        result.add(toParagraphLine(currentLine, metrics, measurement));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(new TextDataBody(continuationPrefix, style));
                        }
                        currentWidth = lineWidth(currentLine, measurement);
                    }
                }
            }

            result.add(toParagraphLine(currentLine, metrics, measurement));
        }

        return List.copyOf(result);
    }

    // ------------------------------------------------------------------
    // Tokenisation + measurement utilities
    // ------------------------------------------------------------------

    private static double availableWidthForPrefix(double maxWidth,
                                                  String prefix,
                                                  TextStyle style,
                                                  TextMeasurementSystem measurement) {
        return Math.max(1.0, maxWidth - measurement.textWidth(style, prefix == null ? "" : prefix));
    }

    private static String normalizeBulletPrefix(String bulletOffset) {
        if (bulletOffset == null || bulletOffset.isEmpty()) {
            return "";
        }
        char last = bulletOffset.charAt(bulletOffset.length() - 1);
        return Character.isWhitespace(last) ? bulletOffset : bulletOffset + " ";
    }

    private static String computeIndentFromPrefix(TextMeasurementSystem measurement,
                                                  TextStyle style,
                                                  String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        double targetWidth = measurement.textWidth(style, prefix);
        double spaceWidth = measurement.textWidth(style, " ");
        if (spaceWidth <= EPS) {
            return "";
        }
        int spaces = (int) Math.ceil(targetWidth / spaceWidth);
        return " ".repeat(Math.max(0, spaces));
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean whitespace = Character.isWhitespace(text.charAt(0));

        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            boolean currentWhitespace = Character.isWhitespace(ch);
            if (currentWhitespace != whitespace && !current.isEmpty()) {
                tokens.add(current.toString());
                current.setLength(0);
            }
            current.append(ch);
            whitespace = currentWhitespace;
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private static List<TextDataBody> tokenizeMarkdownLine(String text,
                                                           TextStyle style,
                                                           MarkDownParser parser) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < text.length() && Character.isWhitespace(text.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        if (firstNonWhitespace + 1 < text.length()) {
            char marker = text.charAt(firstNonWhitespace);
            boolean listMarker = marker == '-' || marker == '*' || marker == '+';
            boolean hasSpaceAfter = Character.isWhitespace(text.charAt(firstNonWhitespace + 1));
            if (listMarker && hasSpaceAfter) {
                List<TextDataBody> bodies = new ArrayList<>();
                if (firstNonWhitespace > 0) {
                    bodies.add(new TextDataBody(text.substring(0, firstNonWhitespace), style));
                }
                bodies.add(new TextDataBody(String.valueOf(marker), style));
                bodies.add(new TextDataBody(" ", style));
                bodies.addAll(parser.getBody(text.substring(firstNonWhitespace + 2), style));
                return List.copyOf(bodies);
            }
        }

        return List.copyOf(parser.getBody(text, style));
    }

    private static ParagraphLine toParagraphLine(List<TextDataBody> bodies,
                                                 TextMeasurementSystem.LineMetrics metrics,
                                                 TextMeasurementSystem measurement) {
        List<TextDataBody> trimmedBodies = trimTrailingWhitespaceBodies(bodies);
        if (trimmedBodies.isEmpty()) {
            return emptyParagraphLine(metrics);
        }

        double textLineHeight = metrics.lineHeight();
        List<ParagraphSpan> spans = new ArrayList<>(trimmedBodies.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        for (TextDataBody body : trimmedBodies) {
            TextStyle style = body.textStyle() == null ? TextStyle.DEFAULT_STYLE : body.textStyle();
            double bodyWidth = measurement.textWidth(style, body.text());
            spans.add(new ParagraphTextSpan(body.text(), style, bodyWidth, textLineHeight));
            text.append(body.text());
            width += bodyWidth;
        }

        return new ParagraphLine(
                text.toString(),
                width,
                textLineHeight,
                textLineHeight,
                metrics.ascent(),
                metrics.baselineOffsetFromBottom(),
                spans);
    }

    private static List<List<InlineLayoutToken>> tokenizeInlineRuns(List<InlineRun> runs,
                                                                    TextStyle defaultStyle,
                                                                    TextMeasurementSystem measurement) {
        List<List<InlineLayoutToken>> lines = new ArrayList<>();
        List<InlineLayoutToken> currentLine = new ArrayList<>();

        for (InlineRun run : runs) {
            if (run == null) {
                continue;
            }
            if (run instanceof InlineTextRun textRun) {
                if (textRun.text().isEmpty()) {
                    continue;
                }
                TextStyle style = textRun.textStyle() == null ? defaultStyle : toTextStyle(textRun.textStyle());
                String normalized = BlockText.sanitizeText(textRun.text().replace("\r\n", "\n").replace('\r', '\n'));
                String[] parts = normalized.split("\n", -1);
                for (int partIndex = 0; partIndex < parts.length; partIndex++) {
                    if (partIndex > 0) {
                        lines.add(List.copyOf(currentLine));
                        currentLine = new ArrayList<>();
                    }
                    if (parts[partIndex].isEmpty()) {
                        continue;
                    }
                    for (String token : tokenize(parts[partIndex])) {
                        currentLine.add(InlineTextToken.of(token, style, textRun.linkTarget(), measurement));
                    }
                }
            } else if (run instanceof InlineImageRun imageRun) {
                currentLine.add(InlineImageToken.of(imageRun));
            } else if (run instanceof InlineShapeRun shapeRun) {
                currentLine.add(InlineShapeToken.of(shapeRun));
            } else if (run instanceof InlineSvgRun svgRun) {
                currentLine.add(InlineSvgToken.of(svgRun));
            } else if (run instanceof InlineHighlightRun highlight) {
                if (highlight.text().isEmpty()) {
                    continue;
                }
                TextStyle style = highlight.textStyle() == null
                        ? defaultStyle : toTextStyle(highlight.textStyle());
                // A chip stays on one logical line (newlines collapse to spaces) but
                // its text tokenizes into words, all tagged with the same group, so
                // it wraps with the surrounding line. Horizontal padding sits on the
                // run's outer edges — lead pad on the first word, trail pad on the
                // last — and toInlineParagraphLine coalesces the same-group tokens on
                // each visual line back into one rounded fill.
                String normalized = BlockText.sanitizeText(
                        highlight.text().replace("\r\n", " ").replace('\r', ' ').replace('\n', ' '));
                if (normalized.isEmpty()) {
                    continue;
                }
                List<String> words = tokenize(normalized);
                DocumentInsets pad = highlight.background().padding();
                for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
                    currentLine.add(InlineTextToken.ofHighlight(
                            words.get(wordIndex), style, highlight.linkTarget(),
                            highlight.background(), highlight,
                            wordIndex == 0 ? pad.left() : 0.0,
                            wordIndex == words.size() - 1 ? pad.right() : 0.0,
                            measurement));
                }
            }
        }

        lines.add(List.copyOf(currentLine));
        return List.copyOf(lines);
    }

    private static ParagraphLine toInlineParagraphLine(List<InlineLayoutToken> tokens,
                                                       TextMeasurementSystem.LineMetrics defaultMetrics,
                                                       TextMeasurementSystem measurement) {
        List<InlineLayoutToken> trimmedTokens = trimTrailingWhitespaceTokens(tokens);
        if (trimmedTokens.isEmpty()) {
            return emptyParagraphLine(defaultMetrics);
        }

        double dominantTextLineHeight = 0.0;
        double dominantAscent = 0.0;
        double dominantBaselineFromBottom = defaultMetrics.baselineOffsetFromBottom();
        boolean sawText = false;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineTextToken textToken) {
                TextMeasurementSystem.LineMetrics metrics = measurement.lineMetrics(textToken.textStyle());
                double textLineHeight = metrics.lineHeight();
                if (textLineHeight > dominantTextLineHeight) {
                    dominantTextLineHeight = textLineHeight;
                    dominantAscent = metrics.ascent();
                    dominantBaselineFromBottom = metrics.baselineOffsetFromBottom();
                    sawText = true;
                }
            }
        }
        if (!sawText) {
            dominantTextLineHeight = defaultMetrics.lineHeight();
            dominantAscent = defaultMetrics.ascent();
            dominantBaselineFromBottom = defaultMetrics.baselineOffsetFromBottom();
        }

        double maxInlineGraphicHeight = 0.0;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineImageToken imageToken) {
                if (imageToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = imageToken.height();
                }
            } else if (token instanceof InlineShapeToken shapeToken) {
                if (shapeToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = shapeToken.height();
                }
            } else if (token instanceof InlineSvgToken svgToken) {
                if (svgToken.height() > maxInlineGraphicHeight) {
                    maxInlineGraphicHeight = svgToken.height();
                }
            }
        }
        double resolvedLineHeight = Math.max(dominantTextLineHeight, maxInlineGraphicHeight);

        List<ParagraphSpan> spans = new ArrayList<>(trimmedTokens.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        int tokenIndex = 0;
        while (tokenIndex < trimmedTokens.size()) {
            InlineLayoutToken token = trimmedTokens.get(tokenIndex);
            if (token instanceof InlineTextToken chipStart && chipStart.highlightGroup() != null) {
                // Coalesce every consecutive token of the same chip run on this
                // visual line into ONE span, so a multi-word (or wrapped) chip paints
                // a single rounded fill per line-fragment. Padding sits on the
                // fragment's outer edges — the lead pad of the first token consumed
                // and the trail pad of the last — so a wrapped fragment is open on
                // the inner break edge.
                Object group = chipStart.highlightGroup();
                InlineBackground source = chipStart.background();
                List<InlineTextToken> parts = new ArrayList<>();
                while (tokenIndex < trimmedTokens.size()
                        && trimmedTokens.get(tokenIndex) instanceof InlineTextToken part
                        && part.highlightGroup() == group) {
                    parts.add(part);
                    tokenIndex++;
                }
                // Collapse a soft-wrap space at a wrap seam: a continuation fragment
                // can begin or end with an inter-word space token (which carries no
                // lead/trail pad). Drop those so the fill hugs the visible glyphs and
                // the seam space stays out of line width. The run's AUTHORED outer
                // spaces keep their pad (leadPad/trailPad > 0) and are preserved.
                // tokenize() coalesces consecutive whitespace, so at most one token is
                // trimmed per side; the guard keeps at least one token regardless.
                int start = 0;
                int end = parts.size();
                while (end - start > 1 && parts.get(end - 1).text().isBlank() && parts.get(end - 1).trailPad() == 0.0) {
                    end--;
                }
                while (end - start > 1 && parts.get(start).text().isBlank() && parts.get(start).leadPad() == 0.0) {
                    start++;
                }
                double leftPad = parts.get(start).leadPad();
                double trailPad = parts.get(end - 1).trailPad();
                double glyphs = 0.0;
                StringBuilder chip = new StringBuilder();
                for (int partIndex = start; partIndex < end; partIndex++) {
                    chip.append(parts.get(partIndex).text());
                    glyphs += parts.get(partIndex).width();
                }
                double spanWidth = leftPad + glyphs + trailPad;
                DocumentInsets basePad = source.padding();
                InlineBackground fragment = new InlineBackground(source.fill(), source.cornerRadius(),
                        new DocumentInsets(basePad.top(), trailPad, basePad.bottom(), leftPad));
                spans.add(new ParagraphTextSpan(
                        chip.toString(),
                        chipStart.textStyle(),
                        spanWidth,
                        measurement.lineMetrics(chipStart.textStyle()).lineHeight(),
                        chipStart.linkTarget(),
                        fragment));
                text.append(chip);
                width += spanWidth;
            } else if (token instanceof InlineTextToken textToken) {
                // wrapWidth folds in the chip's horizontal padding (zero for plain
                // text), so the span width and the line width both account for it.
                double spanWidth = textToken.wrapWidth();
                spans.add(new ParagraphTextSpan(
                        textToken.text(),
                        textToken.textStyle(),
                        spanWidth,
                        measurement.lineMetrics(textToken.textStyle()).lineHeight(),
                        textToken.linkTarget(),
                        textToken.background()));
                text.append(textToken.text());
                width += spanWidth;
                tokenIndex++;
            } else if (token instanceof InlineImageToken imageToken) {
                spans.add(new ParagraphImageSpan(
                        imageToken.imageData(),
                        imageToken.width(),
                        imageToken.height(),
                        imageToken.alignment(),
                        imageToken.baselineOffset(),
                        imageToken.linkTarget()));
                width += imageToken.width();
                tokenIndex++;
            } else if (token instanceof InlineShapeToken shapeToken) {
                spans.add(new ParagraphShapeSpan(
                        shapeToken.layers(),
                        shapeToken.width(),
                        shapeToken.height(),
                        shapeToken.alignment(),
                        shapeToken.baselineOffset(),
                        shapeToken.linkTarget()));
                width += shapeToken.width();
                tokenIndex++;
            } else if (token instanceof InlineSvgToken svgToken) {
                spans.add(new ParagraphSvgSpan(
                        svgToken.layers(),
                        svgToken.width(),
                        svgToken.height(),
                        svgToken.alignment(),
                        svgToken.baselineOffset(),
                        svgToken.linkTarget()));
                width += svgToken.width();
                tokenIndex++;
            } else {
                tokenIndex++;
            }
        }

        return new ParagraphLine(
                text.toString(),
                width,
                resolvedLineHeight,
                dominantTextLineHeight,
                dominantAscent,
                dominantBaselineFromBottom,
                spans);
    }

    private static double inlineLineWidth(List<InlineLayoutToken> tokens) {
        double width = 0.0;
        for (InlineLayoutToken token : tokens) {
            width += token.wrapWidth();
        }
        return width;
    }

    private static List<InlineLayoutToken> trimTrailingWhitespaceTokens(List<InlineLayoutToken> tokens) {
        int end = tokens.size();
        while (end > 0) {
            InlineLayoutToken candidate = tokens.get(end - 1);
            if (candidate == null) {
                end--;
                continue;
            }
            if (candidate instanceof InlineTextToken textToken
                && textToken.highlightGroup() == null
                && (textToken.text() == null || textToken.text().isBlank())) {
                end--;
                continue;
            }
            break;
        }
        return end <= 0 ? List.of() : List.copyOf(tokens.subList(0, end));
    }

    private static InlineLayoutToken trimLeadingIfInlineLineStart(InlineLayoutToken token,
                                                                  List<InlineLayoutToken> currentLine,
                                                                  TextMeasurementSystem measurement) {
        if (token == null) {
            return null;
        }
        if (!(token instanceof InlineTextToken textToken)) {
            return token;
        }
        if (textToken.highlightGroup() != null) {
            // A chip token carries the run's background/group/padding; never strip
            // its leading whitespace or rebuild it via the plain factory (that would
            // drop the fill). The chip's words reassemble in toInlineParagraphLine,
            // which collapses the soft-wrap space at a wrap seam.
            return textToken;
        }
        if (!inlineLineHasVisibleContent(currentLine)) {
            String trimmed = textToken.text() == null ? "" : textToken.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            if (trimmed.equals(textToken.text())) {
                return textToken;
            }
            return InlineTextToken.of(trimmed, textToken.textStyle(), textToken.linkTarget(), measurement);
        }
        return textToken;
    }

    private static boolean inlineLineHasVisibleContent(List<InlineLayoutToken> tokens) {
        for (InlineLayoutToken token : tokens) {
            if (token == null) {
                continue;
            }
            if (token instanceof InlineTextToken textToken) {
                if (textToken.highlightGroup() != null) {
                    // A chip is visible content (it carries a fill) even when its
                    // text is blank — e.g. a colour-swatch badge.
                    return true;
                }
                if (textToken.text() != null && !textToken.text().isBlank()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private static List<TextDataBody> trimTrailingWhitespaceBodies(List<TextDataBody> bodies) {
        int end = bodies.size();
        while (end > 0) {
            TextDataBody candidate = bodies.get(end - 1);
            if (candidate == null || candidate.text() == null || candidate.text().isBlank()) {
                end--;
                continue;
            }
            break;
        }
        return end <= 0 ? List.of() : List.copyOf(bodies.subList(0, end));
    }

    private static TextDataBody trimLeadingIfLineStart(TextDataBody body,
                                                       List<TextDataBody> currentLine) {
        if (body == null) {
            return null;
        }
        if (!lineHasVisibleContent(currentLine)) {
            String trimmed = body.text() == null ? "" : body.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            return new TextDataBody(trimmed, body.textStyle());
        }
        return body;
    }

    private static boolean lineHasVisibleContent(List<TextDataBody> bodies) {
        for (TextDataBody body : bodies) {
            if (body != null && body.text() != null && !body.text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static double lineWidth(List<TextDataBody> bodies,
                                    TextMeasurementSystem measurement) {
        double width = 0.0;
        for (TextDataBody body : bodies) {
            if (body == null || body.text() == null || body.text().isEmpty()) {
                continue;
            }
            width += measurement.textWidth(
                    body.textStyle() == null ? TextStyle.DEFAULT_STYLE : body.textStyle(),
                    body.text());
        }
        return width;
    }

    private static String trimTrailingSpaces(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
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

    // ------------------------------------------------------------------
    // Inline tokens + indent spec
    // ------------------------------------------------------------------

    private record ParagraphIndentSpec(String firstLinePrefix, String continuationPrefix) {
        private static ParagraphIndentSpec from(String bulletOffset,
                                                TextStyle style,
                                                TextMeasurementSystem measurement) {
            String raw = bulletOffset == null ? "" : bulletOffset;
            boolean hasVisibleChars = raw.chars().anyMatch(ch -> !Character.isWhitespace(ch));
            if (hasVisibleChars) {
                String normalizedPrefix = normalizeBulletPrefix(raw);
                return new ParagraphIndentSpec(
                        normalizedPrefix,
                        computeIndentFromPrefix(measurement, style, normalizedPrefix));
            }
            return new ParagraphIndentSpec(raw, raw);
        }
    }
}
