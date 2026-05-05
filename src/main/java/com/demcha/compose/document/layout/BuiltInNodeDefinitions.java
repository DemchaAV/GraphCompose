package com.demcha.compose.document.layout;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.layout.definitions.BarcodeDefinition;
import com.demcha.compose.document.layout.definitions.ContainerDefinition;
import com.demcha.compose.document.layout.definitions.EllipseDefinition;
import com.demcha.compose.document.layout.definitions.ImageDefinition;
import com.demcha.compose.document.layout.definitions.LayerStackDefinition;
import com.demcha.compose.document.layout.definitions.LineDefinition;
import com.demcha.compose.document.layout.definitions.PageBreakDefinition;
import com.demcha.compose.document.layout.definitions.RowDefinition;
import com.demcha.compose.document.layout.definitions.SectionDefinition;
import com.demcha.compose.document.layout.definitions.ShapeContainerDefinition;
import com.demcha.compose.document.layout.definitions.ShapeDefinition;
import com.demcha.compose.document.layout.definitions.SpacerDefinition;
import com.demcha.compose.document.layout.definitions.TableDefinition;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphImageSpan;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.layout.payloads.PdfSemanticFragmentPayload;
import com.demcha.compose.document.layout.payloads.PreparedListItemLayout;
import com.demcha.compose.document.layout.payloads.PreparedListLayout;
import com.demcha.compose.document.layout.payloads.PreparedParagraphLayout;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.renderable.BlockText;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.text.markdown.MarkDownParser;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toImageData;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toIndentStrategy;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toMargin;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toPadding;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toStroke;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableCell;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableColumns;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableRows;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableStyle;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTableStyles;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTextStyle;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;

/**
 * Registers the first semantic built-ins for the v2 document graph.
 *
 * @author Artem Demchyshyn
 */
public final class BuiltInNodeDefinitions {
    private BuiltInNodeDefinitions() {
    }

    /**
     * Registers every built-in canonical node definition with the supplied registry.
     *
     * @param registry mutable registry to populate
     * @return the same registry after registration
     */
    public static NodeRegistry registerDefaults(NodeRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return registry
                .register(new ParagraphDefinition())
                .register(new ListDefinition())
                .register(new ShapeDefinition())
                .register(new SpacerDefinition())
                .register(new LineDefinition())
                .register(new EllipseDefinition())
                .register(new ImageDefinition())
                .register(new BarcodeDefinition())
                .register(new PageBreakDefinition())
                .register(new ContainerDefinition())
                .register(new SectionDefinition())
                .register(new RowDefinition())
                .register(new LayerStackDefinition())
                .register(new ShapeContainerDefinition())
                .register(new TableDefinition());
    }

    // ParagraphSpan / ParagraphTextSpan / ParagraphImageSpan / ParagraphLine
    // moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // PdfSemanticFragmentPayload interface moved to
    // com.demcha.compose.document.layout.payloads in Phase E.2.

    // ParagraphFragmentPayload moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // ShapeFragmentPayload moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // SideBorders moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // LineFragmentPayload and EllipseFragmentPayload moved to
    // com.demcha.compose.document.layout.payloads in Phase E.2.

    // ShapeClipBeginPayload and ShapeClipEndPayload moved to
    // com.demcha.compose.document.layout.payloads in Phase E.2.

    // TransformBeginPayload and TransformEndPayload moved to
    // com.demcha.compose.document.layout.payloads in Phase E.2.

    // ImageFragmentPayload and BarcodeFragmentPayload moved to
    // com.demcha.compose.document.layout.payloads in Phase E.2.

    // TableRowFragmentPayload moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // DEFINITIONS

    private static final class ParagraphDefinition implements NodeDefinition<ParagraphNode> {
        @Override
        public Class<ParagraphNode> nodeType() {
            return ParagraphNode.class;
        }

        @Override
        public PreparedNode<ParagraphNode> prepare(ParagraphNode node, PrepareContext ctx, BoxConstraints constraints) {
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

        @Override
        public PaginationPolicy paginationPolicy(ParagraphNode node) {
            return PaginationPolicy.SPLITTABLE;
        }

        @Override
        public PreparedSplitResult<ParagraphNode> split(PreparedNode<ParagraphNode> prepared, SplitRequest request) {
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

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ParagraphNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
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
                    node.linkOptions(),
                    layout.emitBookmark() ? node.bookmarkOptions() : null);

            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    0.0,
                    0.0,
                    placement.width(),
                    placement.height(),
                    payload));
        }
    }

    private static final class ListDefinition implements NodeDefinition<ListNode> {
        @Override
        public Class<ListNode> nodeType() {
            return ListNode.class;
        }

        @Override
        public PreparedNode<ListNode> prepare(ListNode node, PrepareContext ctx, BoxConstraints constraints) {
            double innerWidth = Math.max(0.0, constraints.availableWidth() - node.padding().horizontal());
            PreparedListLayout layout = prepareListLayout(node, innerWidth, constraints.availableWidth(), ctx.textMeasurement(), ctx.markdownEnabled());
            return PreparedNode.leaf(
                    node,
                    new MeasureResult(layout.resolvedWidth(), layout.totalHeight() + node.padding().vertical()),
                    layout);
        }

        @Override
        public PaginationPolicy paginationPolicy(ListNode node) {
            return PaginationPolicy.SPLITTABLE;
        }

        @Override
        public PreparedSplitResult<ListNode> split(PreparedNode<ListNode> prepared, SplitRequest request) {
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

            PreparedListItemLayout firstItem = layout.items().getFirst();
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

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ListNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
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
                                null)));
                itemTopOffset += fragmentHeight + node.itemSpacing();
            }

            return List.copyOf(fragments);
        }
    }

    // PreparedStackLayout moved to com.demcha.compose.document.layout.payloads in Phase E.2.

    // HELPERS

    private static PreparedListLayout prepareListLayout(ListNode node,
                                                        double innerWidth,
                                                        double availableWidth,
                                                        TextMeasurementSystem measurement,
                                                        boolean markdownEnabled) {
        List<PreparedListItemLayout> items = new ArrayList<>();
        for (String item : node.items()) {
            String normalizedItem = normalizeListItem(item, node.normalizeMarkers());
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

    private static String normalizeListItem(String value, boolean normalizeMarkers) {
        String normalized = value == null ? "" : value.trim();
        if (!normalizeMarkers || normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.startsWith("\u2022")) {
            return normalized.substring(1).trim();
        }
        if (normalized.startsWith("- ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("+ ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("* ") && !normalized.startsWith("**")) {
            return normalized.substring(2).trim();
        }
        return normalized;
    }

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

        // Single-line text: shrink the font size until the longest logical line
        // measures inside the available inner width, otherwise fall back to the
        // smallest configured size.
        for (double size = maxSize; size >= minSize - 1e-6; size -= step) {
            DocumentTextStyle candidate = baseStyle.withSize(size);
            if (paragraphFitsSingleLine(node, candidate, innerWidth, measurement)) {
                return candidate;
            }
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
        boolean useMarkdownLayout = markdownEnabled && logicalLines.stream().anyMatch(BuiltInNodeDefinitions::containsMarkdownSyntax);
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
                node.bookmarkOptions() != null);
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
                source.linkOptions(),
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
                        source.margin().left()));

        PreparedParagraphLayout fragmentLayout = new PreparedParagraphLayout(
                List.copyOf(sliceLogicalLines),
                slice,
                layout.lineMetrics(),
                layout.baselineOffset(),
                layout.lineHeight(),
                layout.lineGap(),
                maxLineWidth,
                totalHeight,
                keepTopInsets && layout.emitBookmark());

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
            String currentLine = initialPrefix;
            boolean hasContent = false;

            for (String token : tokens) {
                String nextToken = hasContent ? token : token.stripLeading();
                if (nextToken.isEmpty()) {
                    continue;
                }

                String candidate = currentLine + nextToken;
                if (!hasContent || measurement.textWidth(style, candidate) <= maxWidth + EPS) {
                    currentLine = candidate;
                    hasContent = true;
                    continue;
                }

                result.add(trimTrailingSpaces(currentLine));
                currentPrefix = continuationPrefix;
                currentLine = continuationPrefix;
                hasContent = false;

                double availableWidth = availableWidthForPrefix(maxWidth, currentPrefix, style, measurement);
                String strippedToken = nextToken.stripLeading();
                if (measurement.textWidth(style, currentPrefix + strippedToken) <= maxWidth + EPS) {
                    currentLine = currentPrefix + strippedToken;
                    hasContent = true;
                    continue;
                }

                List<String> chunks = splitLongToken(strippedToken, style, availableWidth, measurement);
                if (chunks.isEmpty()) {
                    continue;
                }

                for (int index = 0; index < chunks.size() - 1; index++) {
                    result.add(currentPrefix + chunks.get(index));
                    currentPrefix = continuationPrefix;
                }
                currentLine = currentPrefix + chunks.getLast();
                hasContent = true;
            }

            result.add(trimTrailingSpaces(currentLine));
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

                double tokenWidth = sanitizedToken.width();
                if (currentLine.isEmpty() || currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                if (!currentLine.isEmpty()) {
                    result.add(toInlineParagraphLine(currentLine, defaultMetrics, measurement));
                }
                currentLine = new ArrayList<>();
                if (!continuationPrefix.isEmpty()) {
                    currentLine.add(InlineTextToken.of(continuationPrefix, defaultStyle, null, measurement));
                }
                currentWidth = inlineLineWidth(currentLine);

                sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine, measurement);
                if (sanitizedToken == null) {
                    continue;
                }
                tokenWidth = sanitizedToken.width();
                if (currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                if (!(sanitizedToken instanceof InlineTextToken textToken)) {
                    // Atomic image runs that exceed the available width are
                    // emitted on their own line; further breaking is not
                    // possible.
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                List<String> chunks = splitLongToken(
                        textToken.text(),
                        textToken.textStyle(),
                        Math.max(1.0, maxWidth - currentWidth),
                        measurement);
                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    String chunk = chunks.get(chunkIndex);
                    if (chunk.isEmpty()) {
                        continue;
                    }
                    InlineTextToken chunkToken = InlineTextToken.of(
                            chunk,
                            textToken.textStyle(),
                            textToken.linkOptions(),
                            measurement);
                    currentLine.add(chunkToken);
                    currentWidth += chunkToken.width();

                    if (chunkIndex < chunks.size() - 1) {
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
                if (currentLine.isEmpty() || currentWidth + tokenWidth <= maxWidth + EPS) {
                    currentLine.add(sanitizedToken);
                    currentWidth += tokenWidth;
                    continue;
                }

                if (!currentLine.isEmpty()) {
                    result.add(toParagraphLine(currentLine, metrics, measurement));
                }
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

                List<String> chunks = splitLongToken(
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
                        currentLine.add(InlineTextToken.of(token, style, textRun.linkOptions(), measurement));
                    }
                }
            } else if (run instanceof InlineImageRun imageRun) {
                currentLine.add(InlineImageToken.of(imageRun));
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

        double maxImageHeight = 0.0;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineImageToken imageToken) {
                if (imageToken.height() > maxImageHeight) {
                    maxImageHeight = imageToken.height();
                }
            }
        }
        double resolvedLineHeight = Math.max(dominantTextLineHeight, maxImageHeight);

        List<ParagraphSpan> spans = new ArrayList<>(trimmedTokens.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        for (InlineLayoutToken token : trimmedTokens) {
            if (token instanceof InlineTextToken textToken) {
                spans.add(new ParagraphTextSpan(
                        textToken.text(),
                        textToken.textStyle(),
                        textToken.width(),
                        measurement.lineMetrics(textToken.textStyle()).lineHeight(),
                        textToken.linkOptions()));
                text.append(textToken.text());
                width += textToken.width();
            } else if (token instanceof InlineImageToken imageToken) {
                spans.add(new ParagraphImageSpan(
                        imageToken.imageData(),
                        imageToken.width(),
                        imageToken.height(),
                        imageToken.alignment(),
                        imageToken.baselineOffset(),
                        imageToken.linkOptions()));
                width += imageToken.width();
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
            width += token.width();
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
        if (!inlineLineHasVisibleContent(currentLine)) {
            String trimmed = textToken.text() == null ? "" : textToken.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            if (trimmed.equals(textToken.text())) {
                return textToken;
            }
            return InlineTextToken.of(trimmed, textToken.textStyle(), textToken.linkOptions(), measurement);
        }
        return textToken;
    }

    private static boolean inlineLineHasVisibleContent(List<InlineLayoutToken> tokens) {
        for (InlineLayoutToken token : tokens) {
            if (token == null) {
                continue;
            }
            if (token instanceof InlineTextToken textToken) {
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

    private static List<String> splitLongToken(String token,
                                               TextStyle style,
                                               double maxWidth,
                                               TextMeasurementSystem measurement) {
        if (token == null || token.isEmpty()) {
            return List.of();
        }

        List<String> pieces = new ArrayList<>();
        String remaining = token;
        while (!remaining.isEmpty()) {
            int splitIndex = fitCharacters(remaining, style, maxWidth, measurement);
            if (splitIndex <= 0 || splitIndex >= remaining.length()) {
                pieces.add(remaining);
                break;
            }
            pieces.add(remaining.substring(0, splitIndex));
            remaining = remaining.substring(splitIndex).stripLeading();
        }
        return List.copyOf(pieces);
    }

    private static int fitCharacters(String text,
                                     TextStyle style,
                                     double maxWidth,
                                     TextMeasurementSystem measurement) {
        int lastFitting = 0;
        for (int index = 1; index <= text.length(); index++) {
            String candidate = text.substring(0, index);
            if (measurement.textWidth(style, candidate) <= maxWidth + EPS) {
                lastFitting = index;
            } else {
                break;
            }
        }
        return lastFitting == 0 ? Math.min(1, text.length()) : lastFitting;
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
        if (availableHeight + EPS < lines.getFirst().lineHeight()) {
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

    // PreparedParagraphLayout / PreparedListItemLayout / PreparedListLayout
    // moved to com.demcha.compose.document.layout.payloads in Phase E.1.

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

    private sealed interface InlineLayoutToken permits InlineTextToken, InlineImageToken {
        double width();
    }

    private record InlineTextToken(
            String text,
            TextStyle textStyle,
            DocumentLinkOptions linkOptions,
            double width
    ) implements InlineLayoutToken {
        private InlineTextToken {
            text = text == null ? "" : text;
            textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        }

        private static InlineTextToken of(String text,
                                          TextStyle style,
                                          DocumentLinkOptions linkOptions,
                                          TextMeasurementSystem measurement) {
            String safeText = text == null ? "" : text;
            TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
            double width = safeText.isEmpty() ? 0.0 : measurement.textWidth(safeStyle, safeText);
            return new InlineTextToken(safeText, safeStyle, linkOptions, width);
        }
    }

    private record InlineImageToken(
            ImageData imageData,
            double width,
            double height,
            InlineImageAlignment alignment,
            double baselineOffset,
            DocumentLinkOptions linkOptions
    ) implements InlineLayoutToken {
        private InlineImageToken {
            Objects.requireNonNull(imageData, "imageData");
            alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
        }

        private static InlineImageToken of(InlineImageRun run) {
            return new InlineImageToken(
                    toImageData(run.imageData()),
                    run.width(),
                    run.height(),
                    run.alignment(),
                    run.baselineOffset(),
                    run.linkOptions());
        }
    }
}
