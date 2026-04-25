package com.demcha.compose.document.layout;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
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
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
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

/**
 * Registers the first semantic built-ins for the v2 document graph.
 *
 * @author Artem Demchyshyn
 */
public final class BuiltInNodeDefinitions {
    private static final double EPS = 1e-6;

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
                .register(new ImageDefinition())
                .register(new BarcodeDefinition())
                .register(new PageBreakDefinition())
                .register(new ContainerDefinition())
                .register(new SectionDefinition())
                .register(new TableDefinition());
    }

    /**
     * One measured inline text span inside a paragraph line.
     *
     * @param text visible text for the span
     * @param textStyle resolved text style
     * @param width measured span width
     * @param linkOptions optional link metadata for the span
     */
    public record ParagraphSpan(String text, TextStyle textStyle, double width, PdfLinkOptions linkOptions) {
        /**
         * Creates a normalized measured paragraph span.
         */
        public ParagraphSpan {
            text = text == null ? "" : text;
            textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        }

        /**
         * Creates a span without link metadata.
         *
         * @param text visible span text
         * @param textStyle resolved text style
         * @param width measured span width
         */
        public ParagraphSpan(String text, TextStyle textStyle, double width) {
            this(text, textStyle, width, null);
        }
    }

    /**
     * One measured paragraph line emitted to the PDF backend.
     *
     * @param text line text used for diagnostics and simple rendering paths
     * @param width measured line width
     * @param spans measured styled spans in source order
     */
    public record ParagraphLine(String text, double width, List<ParagraphSpan> spans) {
        /**
         * Creates a normalized measured paragraph line.
         */
        public ParagraphLine {
            text = text == null ? "" : text;
            spans = List.copyOf(spans);
        }
    }

    /**
     * Marker interface for fragment payloads that carry canonical PDF link or
     * bookmark metadata through the resolved semantic graph.
     */
    public interface PdfSemanticFragmentPayload {
        /**
         * Returns link metadata for the resolved fragment, or {@code null} when
         * no PDF annotation should be emitted.
         *
         * @return fragment-level link options, or {@code null}
         */
        PdfLinkOptions linkOptions();

        /**
         * Returns bookmark metadata for the resolved fragment, or {@code null}
         * when no PDF outline entry should be emitted.
         *
         * @return fragment-level bookmark options, or {@code null}
         */
        PdfBookmarkOptions bookmarkOptions();
    }

    /**
     * PDF payload for a resolved paragraph fragment.
     *
     * @param textStyle base text style for the fragment
     * @param align horizontal text alignment
     * @param padding fragment padding
     * @param lineHeight resolved line height
     * @param lineGap extra spacing between lines
     * @param baselineOffset offset from line bottom to baseline
     * @param lines measured lines contained by the fragment
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record ParagraphFragmentPayload(
            TextStyle textStyle,
            TextAlign align,
            Padding padding,
            double lineHeight,
            double lineGap,
            double baselineOffset,
            List<ParagraphLine> lines,
            PdfLinkOptions linkOptions,
            PdfBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
        /**
         * Creates an immutable paragraph fragment payload.
         */
        public ParagraphFragmentPayload {
            lines = List.copyOf(lines);
        }
    }

    /**
     * PDF payload for a resolved shape fragment.
     *
     * @param fillColor optional shape fill color
     * @param stroke optional shape stroke
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record ShapeFragmentPayload(
            Color fillColor,
            Stroke stroke,
            PdfLinkOptions linkOptions,
            PdfBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
    }

    /**
     * PDF payload for a resolved image fragment.
     *
     * @param imageData image source data
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record ImageFragmentPayload(
            ImageData imageData,
            PdfLinkOptions linkOptions,
            PdfBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
    }

    /**
     * PDF payload for a resolved barcode or QR fragment.
     *
     * @param barcodeData barcode payload data
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record BarcodeFragmentPayload(
            BarcodeData barcodeData,
            PdfLinkOptions linkOptions,
            PdfBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
    }

    /**
     * PDF payload for one resolved table row fragment.
     *
     * @param cells resolved cells in column order
     * @param startsPageFragment whether this row starts a table page fragment
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record TableRowFragmentPayload(
            List<TableResolvedCell> cells,
            boolean startsPageFragment,
            PdfLinkOptions linkOptions,
            PdfBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
        /**
         * Creates an immutable table row fragment payload.
         */
        public TableRowFragmentPayload {
            cells = List.copyOf(cells);
        }
    }

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
            int maxLines = maxLinesThatFit(layout.visualLines().size(), layout.lineHeight(), layout.lineGap(), innerAvailableHeight);
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
                    itemLayout.visualLines().size(),
                    itemLayout.lineHeight(),
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

    private static final class ShapeDefinition implements NodeDefinition<ShapeNode> {
        @Override
        public Class<ShapeNode> nodeType() {
            return ShapeNode.class;
        }

        @Override
        public PreparedNode<ShapeNode> prepare(ShapeNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(
                    node.width() + node.padding().horizontal(),
                    node.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(ShapeNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ShapeNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            ShapeNode node = prepared.node();
            double width = Math.max(0.0, placement.width() - node.padding().horizontal());
            double height = Math.max(0.0, placement.height() - node.padding().vertical());
            if (width <= EPS || height <= EPS) {
                return List.of();
            }
            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    node.padding().left(),
                    node.padding().bottom(),
                    width,
                    height,
                    new ShapeFragmentPayload(
                            node.fillColor() == null ? null : node.fillColor().color(),
                            toStroke(node.stroke()),
                            node.linkOptions(),
                            node.bookmarkOptions())));
        }
    }

    private static final class ImageDefinition implements NodeDefinition<ImageNode> {
        @Override
        public Class<ImageNode> nodeType() {
            return ImageNode.class;
        }

        @Override
        public PreparedNode<ImageNode> prepare(ImageNode node, PrepareContext ctx, BoxConstraints constraints) {
            ImageDimensions dimensions = resolveImageDimensions(node, constraints.availableWidth());
            return PreparedNode.leaf(node, new MeasureResult(
                    dimensions.width() + node.padding().horizontal(),
                    dimensions.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(ImageNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ImageNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            ImageNode node = prepared.node();
            double width = Math.max(0.0, placement.width() - node.padding().horizontal());
            double height = Math.max(0.0, placement.height() - node.padding().vertical());
            if (width <= EPS || height <= EPS) {
                return List.of();
            }
            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    node.padding().left(),
                    node.padding().bottom(),
                    width,
                    height,
                    new ImageFragmentPayload(toImageData(node.imageData()), node.linkOptions(), node.bookmarkOptions())));
        }
    }

    private static final class BarcodeDefinition implements NodeDefinition<BarcodeNode> {
        @Override
        public Class<BarcodeNode> nodeType() {
            return BarcodeNode.class;
        }

        @Override
        public PreparedNode<BarcodeNode> prepare(BarcodeNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(
                    node.width() + node.padding().horizontal(),
                    node.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(BarcodeNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<BarcodeNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            BarcodeNode node = prepared.node();
            double width = Math.max(0.0, placement.width() - node.padding().horizontal());
            double height = Math.max(0.0, placement.height() - node.padding().vertical());
            if (width <= EPS || height <= EPS) {
                return List.of();
            }
            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    node.padding().left(),
                    node.padding().bottom(),
                    width,
                    height,
                    new BarcodeFragmentPayload(toBarcodeData(node.barcodeOptions()), node.linkOptions(), node.bookmarkOptions())));
        }
    }

    private static final class PageBreakDefinition implements NodeDefinition<PageBreakNode> {
        @Override
        public Class<PageBreakNode> nodeType() {
            return PageBreakNode.class;
        }

        @Override
        public PreparedNode<PageBreakNode> prepare(PageBreakNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(0.0, 0.0));
        }

        @Override
        public PaginationPolicy paginationPolicy(PageBreakNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<PageBreakNode> prepared,
                                                  FragmentContext ctx,
                                                  FragmentPlacement placement) {
            return List.of();
        }
    }

    private static final class ContainerDefinition implements NodeDefinition<ContainerNode> {
        @Override
        public Class<ContainerNode> nodeType() {
            return ContainerNode.class;
        }

        @Override
        public PreparedNode<ContainerNode> prepare(ContainerNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.composite(
                    node,
                    measureComposite(node.children(), node.spacing(), toPadding(node.padding()), ctx, constraints),
                    new CompositeLayoutSpec(node.spacing()));
        }

        @Override
        public PaginationPolicy paginationPolicy(ContainerNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<DocumentNode> children(ContainerNode node) {
            return node.children();
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ContainerNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            return List.of();
        }
    }

    private static final class SectionDefinition implements NodeDefinition<SectionNode> {
        @Override
        public Class<SectionNode> nodeType() {
            return SectionNode.class;
        }

        @Override
        public PreparedNode<SectionNode> prepare(SectionNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.composite(
                    node,
                    measureComposite(node.children(), node.spacing(), toPadding(node.padding()), ctx, constraints),
                    new CompositeLayoutSpec(node.spacing()));
        }

        @Override
        public PaginationPolicy paginationPolicy(SectionNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<DocumentNode> children(SectionNode node) {
            return node.children();
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<SectionNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            return List.of();
        }
    }

    private static final class TableDefinition implements NodeDefinition<TableNode> {
        @Override
        public Class<TableNode> nodeType() {
            return TableNode.class;
        }

        @Override
        public PreparedNode<TableNode> prepare(TableNode node, PrepareContext ctx, BoxConstraints constraints) {
            ResolvedTableLayout layout = resolveTableLayout(node, ctx.textMeasurement(), constraints.availableWidth());
            MeasureResult measure = new MeasureResult(
                    layout.finalWidth() + node.padding().horizontal(),
                    layout.totalHeight() + node.padding().vertical());
            return PreparedNode.leaf(node, measure, new PreparedTableLayout(layout, node.bookmarkOptions() != null));
        }

        @Override
        public PaginationPolicy paginationPolicy(TableNode node) {
            return PaginationPolicy.SPLITTABLE;
        }

        @Override
        public PreparedSplitResult<TableNode> split(PreparedNode<TableNode> prepared, SplitRequest request) {
            TableNode node = prepared.node();
            ResolvedTableLayout layout = prepared.requirePreparedLayout(PreparedTableLayout.class).resolvedLayout();
            double innerAvailableHeight = Math.max(0.0, request.remainingHeight() - node.padding().vertical());

            int rowCount = 0;
            double consumed = 0.0;
            for (double rowHeight : layout.rowHeights()) {
                if (consumed + rowHeight > innerAvailableHeight + EPS) {
                    break;
                }
                consumed += rowHeight;
                rowCount++;
            }

            if (rowCount <= 0) {
                return new PreparedSplitResult<>(null, prepared);
            }
            if (rowCount >= node.rows().size()) {
                return PreparedSplitResult.whole(prepared);
            }

            PreparedNode<TableNode> head = sliceTablePreparedNode(node, layout, 0, rowCount, true, false);
            PreparedNode<TableNode> tail = sliceTablePreparedNode(node, layout, rowCount, node.rows().size(), false, true);
            return new PreparedSplitResult<>(head, tail);
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<TableNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            TableNode node = prepared.node();
            PreparedTableLayout preparedLayout = prepared.requirePreparedLayout(PreparedTableLayout.class);
            ResolvedTableLayout layout = preparedLayout.resolvedLayout();

            List<LayoutFragment> fragments = new ArrayList<>(layout.rows().size());
            double innerHeight = layout.totalHeight();
            double rowTopOffset = 0.0;

            for (int rowIndex = 0; rowIndex < layout.rows().size(); rowIndex++) {
                double rowHeight = layout.rowHeights().get(rowIndex);
                double localY = node.padding().bottom() + (innerHeight - rowTopOffset - rowHeight);
                fragments.add(new LayoutFragment(
                        placement.path(),
                        rowIndex,
                        node.padding().left(),
                        localY,
                        layout.finalWidth(),
                        rowHeight,
                        new TableRowFragmentPayload(
                                layout.rows().get(rowIndex),
                                rowIndex == 0,
                                node.linkOptions(),
                                rowIndex == 0 && preparedLayout.emitBookmark()
                                        ? node.bookmarkOptions()
                                        : null)));
                rowTopOffset += rowHeight;
            }

            return List.copyOf(fragments);
        }
    }

    // HELPERS

    private static BarcodeData toBarcodeData(PdfBarcodeOptions options) {
        return BarcodeData.of(
                options.getContent(),
                switch (options.getType()) {
                    case CODE_128 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_128;
                    case CODE_39 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_39;
                    case EAN_13 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_13;
                    case EAN_8 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_8;
                    case UPC_A -> com.demcha.compose.engine.components.content.barcode.BarcodeType.UPC_A;
                    case PDF_417 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.PDF_417;
                    case DATA_MATRIX -> com.demcha.compose.engine.components.content.barcode.BarcodeType.DATA_MATRIX;
                    case QR_CODE -> com.demcha.compose.engine.components.content.barcode.BarcodeType.QR_CODE;
                },
                options.getForeground(),
                options.getBackground(),
                options.getQuietZoneMargin());
    }

    private static ImageDimensions resolveImageDimensions(ImageNode node, double availableWidth) {
        ImageData imageData = toImageData(node.imageData());
        int intrinsicWidth = Math.max(1, imageData.getMetadata().width());
        int intrinsicHeight = Math.max(1, imageData.getMetadata().height());
        double ratio = intrinsicWidth / (double) intrinsicHeight;

        double requestedWidth = node.width() == null ? Double.NaN : node.width();
        double requestedHeight = node.height() == null ? Double.NaN : node.height();

        double width;
        double height;

        if (!Double.isNaN(requestedWidth) && !Double.isNaN(requestedHeight)) {
            width = requestedWidth;
            height = requestedHeight;
        } else if (!Double.isNaN(requestedWidth)) {
            width = requestedWidth;
            height = requestedWidth / ratio;
        } else if (!Double.isNaN(requestedHeight)) {
            height = requestedHeight;
            width = requestedHeight * ratio;
        } else {
            width = intrinsicWidth;
            height = intrinsicHeight;
        }

        double maxWidth = Math.max(0.0, availableWidth - node.padding().horizontal());
        if (maxWidth > EPS && width > maxWidth) {
            double scale = maxWidth / width;
            width *= scale;
            height *= scale;
        }

        return new ImageDimensions(width, height);
    }

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

    private static PreparedParagraphLayout prepareParagraphLayout(ParagraphNode node,
                                                                 double innerWidth,
                                                                 TextMeasurementSystem measurement,
                                                                 boolean markdownEnabled) {
        List<String> logicalLines = sanitizeLogicalLines(node.text());
        boolean useMarkdownLayout = markdownEnabled && logicalLines.stream().anyMatch(BuiltInNodeDefinitions::containsMarkdownSyntax);
        TextStyle textStyle = toTextStyle(node.textStyle());
        TextIndentStrategy indentStrategy = toIndentStrategy(node.indentStrategy());
        List<ParagraphLine> visualLines = !node.inlineTextRuns().isEmpty()
                ? wrapInlineParagraph(
                        node.inlineTextRuns(),
                        textStyle,
                        Math.max(0.0, innerWidth),
                        node.bulletOffset(),
                        indentStrategy,
                        measurement)
                : useMarkdownLayout
                ? wrapMarkdownParagraph(
                        logicalLines,
                        textStyle,
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
                        measurement);
        if (visualLines.isEmpty()) {
            visualLines = List.of(new ParagraphLine("", 0.0, List.of()));
        }

        TextMeasurementSystem.LineMetrics lineMetrics = measurement.lineMetrics(textStyle);
        double lineHeight = lineMetrics.lineHeight();
        double gap = node.lineSpacing();
        double totalHeight = lineHeight * visualLines.size() + Math.max(0, visualLines.size() - 1) * gap;
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
        double totalHeight = layout.lineHeight() * slice.size() + Math.max(0, slice.size() - 1) * layout.lineGap();

        ParagraphNode fragmentNode = new ParagraphNode(
                source.name(),
                String.join("\n", sliceLogicalLines),
                source.inlineTextRuns(),
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
                                                        TextMeasurementSystem measurement) {
        List<ParagraphLine> result = new ArrayList<>(wrappedLines.size());
        for (String line : wrappedLines) {
            String safeLine = line == null ? "" : line;
            double width = measurement.textWidth(style, safeLine);
            result.add(new ParagraphLine(
                    safeLine,
                    width,
                    List.of(new ParagraphSpan(safeLine, style, width))));
        }
        return List.copyOf(result);
    }

    private static List<ParagraphLine> wrapInlineParagraph(List<InlineTextRun> runs,
                                                           TextStyle defaultStyle,
                                                           double maxWidth,
                                                           String bulletOffset,
                                                           TextIndentStrategy indentStrategy,
                                                           TextMeasurementSystem measurement) {
        List<ParagraphLine> result = new ArrayList<>();
        ParagraphIndentSpec indentSpec = ParagraphIndentSpec.from(bulletOffset, defaultStyle, measurement);
        List<List<InlineLayoutToken>> logicalLines = tokenizeInlineRuns(runs, defaultStyle);

        for (int logicalLineIndex = 0; logicalLineIndex < logicalLines.size(); logicalLineIndex++) {
            List<InlineLayoutToken> logicalLine = logicalLines.get(logicalLineIndex);
            if (logicalLine.isEmpty() || maxWidth <= EPS) {
                result.add(new ParagraphLine("", 0.0, List.of()));
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
                currentLine.add(new InlineLayoutToken(initialPrefix, defaultStyle, null));
            }
            double currentWidth = inlineLineWidth(currentLine, measurement);

            for (InlineLayoutToken token : logicalLine) {
                InlineLayoutToken sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine);
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
                    result.add(toInlineParagraphLine(currentLine, measurement));
                }
                currentLine = new ArrayList<>();
                if (!continuationPrefix.isEmpty()) {
                    currentLine.add(new InlineLayoutToken(continuationPrefix, defaultStyle, null));
                }
                currentWidth = inlineLineWidth(currentLine, measurement);

                sanitizedToken = trimLeadingIfInlineLineStart(token, currentLine);
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
                for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                    String chunk = chunks.get(chunkIndex);
                    if (chunk.isEmpty()) {
                        continue;
                    }
                    InlineLayoutToken chunkToken = new InlineLayoutToken(
                            chunk,
                            sanitizedToken.textStyle(),
                            sanitizedToken.linkOptions());
                    currentLine.add(chunkToken);
                    currentWidth += measurement.textWidth(chunkToken.textStyle(), chunkToken.text());

                    if (chunkIndex < chunks.size() - 1) {
                        result.add(toInlineParagraphLine(currentLine, measurement));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(new InlineLayoutToken(continuationPrefix, defaultStyle, null));
                        }
                        currentWidth = inlineLineWidth(currentLine, measurement);
                    }
                }
            }

            result.add(toInlineParagraphLine(currentLine, measurement));
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
                result.add(new ParagraphLine("", 0.0, List.of()));
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
                    result.add(toParagraphLine(currentLine, measurement));
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
                        result.add(toParagraphLine(currentLine, measurement));
                        currentLine = new ArrayList<>();
                        if (!continuationPrefix.isEmpty()) {
                            currentLine.add(new TextDataBody(continuationPrefix, style));
                        }
                        currentWidth = lineWidth(currentLine, measurement);
                    }
                }
            }

            result.add(toParagraphLine(currentLine, measurement));
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
                                                 TextMeasurementSystem measurement) {
        List<TextDataBody> trimmedBodies = trimTrailingWhitespaceBodies(bodies);
        if (trimmedBodies.isEmpty()) {
            return new ParagraphLine("", 0.0, List.of());
        }

        List<ParagraphSpan> spans = new ArrayList<>(trimmedBodies.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        for (TextDataBody body : trimmedBodies) {
            TextStyle style = body.textStyle() == null ? TextStyle.DEFAULT_STYLE : body.textStyle();
            double bodyWidth = measurement.textWidth(style, body.text());
            spans.add(new ParagraphSpan(body.text(), style, bodyWidth));
            text.append(body.text());
            width += bodyWidth;
        }

        return new ParagraphLine(text.toString(), width, spans);
    }

    private static List<List<InlineLayoutToken>> tokenizeInlineRuns(List<InlineTextRun> runs, TextStyle defaultStyle) {
        List<List<InlineLayoutToken>> lines = new ArrayList<>();
        List<InlineLayoutToken> currentLine = new ArrayList<>();

        for (InlineTextRun run : runs) {
            if (run == null || run.text().isEmpty()) {
                continue;
            }
            TextStyle style = run.textStyle() == null ? defaultStyle : toTextStyle(run.textStyle());
            String normalized = BlockText.sanitizeText(run.text().replace("\r\n", "\n").replace('\r', '\n'));
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
                    currentLine.add(new InlineLayoutToken(token, style, run.linkOptions()));
                }
            }
        }

        lines.add(List.copyOf(currentLine));
        return List.copyOf(lines);
    }

    private static ParagraphLine toInlineParagraphLine(List<InlineLayoutToken> tokens,
                                                       TextMeasurementSystem measurement) {
        List<InlineLayoutToken> trimmedTokens = trimTrailingWhitespaceTokens(tokens);
        if (trimmedTokens.isEmpty()) {
            return new ParagraphLine("", 0.0, List.of());
        }

        List<ParagraphSpan> spans = new ArrayList<>(trimmedTokens.size());
        StringBuilder text = new StringBuilder();
        double width = 0.0;
        for (InlineLayoutToken token : trimmedTokens) {
            double tokenWidth = measurement.textWidth(token.textStyle(), token.text());
            spans.add(new ParagraphSpan(token.text(), token.textStyle(), tokenWidth, token.linkOptions()));
            text.append(token.text());
            width += tokenWidth;
        }

        return new ParagraphLine(text.toString(), width, spans);
    }

    private static double inlineLineWidth(List<InlineLayoutToken> tokens,
                                          TextMeasurementSystem measurement) {
        double width = 0.0;
        for (InlineLayoutToken token : tokens) {
            width += measurement.textWidth(token.textStyle(), token.text());
        }
        return width;
    }

    private static List<InlineLayoutToken> trimTrailingWhitespaceTokens(List<InlineLayoutToken> tokens) {
        int end = tokens.size();
        while (end > 0) {
            InlineLayoutToken candidate = tokens.get(end - 1);
            if (candidate == null || candidate.text() == null || candidate.text().isBlank()) {
                end--;
                continue;
            }
            break;
        }
        return end <= 0 ? List.of() : List.copyOf(tokens.subList(0, end));
    }

    private static InlineLayoutToken trimLeadingIfInlineLineStart(InlineLayoutToken token,
                                                                  List<InlineLayoutToken> currentLine) {
        if (token == null) {
            return null;
        }
        if (!inlineLineHasVisibleContent(currentLine)) {
            String trimmed = token.text() == null ? "" : token.text().stripLeading();
            if (trimmed.isEmpty()) {
                return null;
            }
            return new InlineLayoutToken(trimmed, token.textStyle(), token.linkOptions());
        }
        return token;
    }

    private static boolean inlineLineHasVisibleContent(List<InlineLayoutToken> tokens) {
        for (InlineLayoutToken token : tokens) {
            if (token != null && token.text() != null && !token.text().isBlank()) {
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

    private static int maxLinesThatFit(int lineCount, double lineHeight, double lineGap, double availableHeight) {
        if (availableHeight + EPS < lineHeight) {
            return 0;
        }

        int count = 0;
        double used = 0.0;
        while (count < lineCount) {
            double addition = count == 0 ? lineHeight : lineGap + lineHeight;
            if (used + addition > availableHeight + EPS) {
                break;
            }
            used += addition;
            count++;
        }
        return count;
    }

    private static MeasureResult measureComposite(List<DocumentNode> children,
                                                  double spacing,
                                                  Padding padding,
                                                  PrepareContext ctx,
                                                  BoxConstraints constraints) {
        double innerWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        double totalHeight = padding.vertical();
        double maxWidth = 0.0;

        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(
                    child,
                    BoxConstraints.unboundedHeight(Math.max(0.0, innerWidth - child.margin().horizontal())));
            totalHeight += child.margin().vertical() + childPrepared.measureResult().height();
            maxWidth = Math.max(maxWidth, child.margin().horizontal() + childPrepared.measureResult().width());
            if (index < children.size() - 1) {
                totalHeight += spacing;
            }
        }

        return new MeasureResult(
                Math.min(constraints.availableWidth(), padding.horizontal() + maxWidth),
                totalHeight);
    }

    private static ResolvedTableLayout resolveTableLayout(TableNode node,
                                                          TextMeasurementSystem measurement,
                                                          double availableWidth) {
        validateRowsExist(node);
        int columnCount = resolveColumnCount(node);
        validateRowLengths(node, columnCount);

        List<TableColumnLayout> normalizedSpecs = normalizeSpecs(node, columnCount);
        List<List<TableCellLayoutStyle>> stylesByRow = resolveCellStyles(node, columnCount);
        List<List<TableCellContent>> tableRows = toTableRows(node.rows());
        double[] naturalWidths = resolveNaturalColumnWidths(node, normalizedSpecs, stylesByRow, columnCount, measurement);
        double naturalWidth = sum(naturalWidths);
        double[] finalWidths = resolveFinalColumnWidths(node, normalizedSpecs, naturalWidths, naturalWidth);
        double finalWidth = sum(finalWidths);

        double innerAvailableWidth = Math.max(0.0, availableWidth - node.padding().horizontal());
        if (finalWidth > innerAvailableWidth + EPS) {
            throw new IllegalStateException("Table '" + displayName(node) + "' width " + finalWidth
                    + " exceeds available width " + innerAvailableWidth + ".");
        }

        List<List<TableResolvedCell>> rows = new ArrayList<>(node.rows().size());
        List<Double> rowHeights = new ArrayList<>(node.rows().size());

        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            List<TableCellContent> rowValues = tableRows.get(rowIndex);
            List<TableCellLayoutStyle> resolvedStyles = stylesByRow.get(rowIndex);
            double rowHeight = 0.0;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                rowHeight = Math.max(rowHeight, cellNaturalHeight(rowValues.get(columnIndex), resolvedStyles.get(columnIndex), measurement));
            }
            rowHeights.add(rowHeight);

            List<TableResolvedCell> cells = new ArrayList<>(columnCount);
            double x = 0.0;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                cells.add(new TableResolvedCell(
                        cellName(node, rowIndex, columnIndex),
                        x,
                        finalWidths[columnIndex],
                        rowHeight,
                        sanitizeCellLines(rowValues.get(columnIndex)),
                        resolvedStyles.get(columnIndex),
                        fillInsets(stylesByRow, rowIndex, columnIndex),
                        borderSides(stylesByRow, rowIndex, columnIndex)));
                x += finalWidths[columnIndex];
            }
            rows.add(List.copyOf(cells));
        }

        return new ResolvedTableLayout(
                toList(finalWidths),
                List.copyOf(rowHeights),
                List.copyOf(rows),
                naturalWidth,
                finalWidth,
                rowHeights.stream().mapToDouble(Double::doubleValue).sum());
    }

        private static PreparedNode<TableNode> sliceTablePreparedNode(TableNode source,
                                                                      ResolvedTableLayout layout,
                                                                      int fromInclusive,
                                                                      int toExclusive,
                                                                      boolean keepTopInsets,
                                                                      boolean keepBottomInsets) {
        List<List<TableResolvedCell>> rows = List.copyOf(layout.rows().subList(fromInclusive, toExclusive));
        List<Double> rowHeights = List.copyOf(layout.rowHeights().subList(fromInclusive, toExclusive));
        double totalHeight = rowHeights.stream().mapToDouble(Double::doubleValue).sum();
        var fixedColumns = layout.columnWidths().stream()
                .map(com.demcha.compose.document.table.DocumentTableColumn::fixed)
                .toList();

            TableNode fragmentNode = new TableNode(
                    source.name(),
                    fixedColumns,
                    source.rows().subList(fromInclusive, toExclusive),
                    source.defaultCellStyle(),
                    source.rowStyles(),
                    source.columnStyles(),
                    layout.finalWidth(),
                    source.linkOptions(),
                    keepTopInsets ? source.bookmarkOptions() : null,
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

        ResolvedTableLayout fragmentLayout = new ResolvedTableLayout(
                layout.columnWidths(),
                rowHeights,
                rows,
                layout.naturalWidth(),
                layout.finalWidth(),
                totalHeight);

        MeasureResult measure = new MeasureResult(
                layout.finalWidth() + fragmentNode.padding().horizontal(),
                totalHeight + fragmentNode.padding().vertical());
        return PreparedNode.leaf(fragmentNode, measure, new PreparedTableLayout(fragmentLayout, keepTopInsets));
    }

    private static List<List<TableCellLayoutStyle>> resolveCellStyles(TableNode node, int columnCount) {
        TableCellLayoutStyle tableDefault = TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, toTableStyle(node.defaultCellStyle()));
        Map<Integer, TableCellLayoutStyle> rowStyleOverrides = toTableStyles(node.rowStyles());
        Map<Integer, TableCellLayoutStyle> columnStyleOverrides = toTableStyles(node.columnStyles());
        List<List<TableCellLayoutStyle>> result = new ArrayList<>(node.rows().size());

        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            List<TableCellLayoutStyle> rowResult = new ArrayList<>(columnCount);
            TableCellLayoutStyle rowOverride = rowStyleOverrides.get(rowIndex);
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                TableCellLayoutStyle resolved = TableCellLayoutStyle.merge(tableDefault, columnStyleOverrides.get(columnIndex));
                resolved = TableCellLayoutStyle.merge(resolved, rowOverride);
                resolved = TableCellLayoutStyle.merge(resolved, toTableStyle(node.rows().get(rowIndex).get(columnIndex).style()));
                rowResult.add(resolved);
            }
            result.add(List.copyOf(rowResult));
        }

        return List.copyOf(result);
    }

    private static double[] resolveNaturalColumnWidths(TableNode node,
                                                       List<TableColumnLayout> normalizedSpecs,
                                                       List<List<TableCellLayoutStyle>> stylesByRow,
                                                       int columnCount,
                                                       TextMeasurementSystem measurement) {
        double[] widths = new double[columnCount];

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            double requiredNaturalWidth = 0.0;
            for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
                requiredNaturalWidth = Math.max(
                        requiredNaturalWidth,
                        cellNaturalWidth(toTableCell(node.rows().get(rowIndex).get(columnIndex)), stylesByRow.get(rowIndex).get(columnIndex), measurement));
            }

            TableColumnLayout spec = normalizedSpecs.get(columnIndex);
            if (spec.isFixed()) {
                if (spec.requiredFixedWidth() + EPS < requiredNaturalWidth) {
                    throw new IllegalStateException("Fixed column " + columnIndex + " width " + spec.requiredFixedWidth()
                            + " is smaller than required natural width " + requiredNaturalWidth + ".");
                }
                widths[columnIndex] = spec.requiredFixedWidth();
            } else {
                widths[columnIndex] = requiredNaturalWidth;
            }
        }

        return widths;
    }

    private static double[] resolveFinalColumnWidths(TableNode node,
                                                     List<TableColumnLayout> normalizedSpecs,
                                                     double[] naturalWidths,
                                                     double naturalWidth) {
        double[] finalWidths = naturalWidths.clone();
        if (node.width() == null) {
            return finalWidths;
        }
        if (node.width() + EPS < naturalWidth) {
            throw new IllegalStateException("Requested table width " + node.width()
                    + " is smaller than natural width " + naturalWidth + ".");
        }

        double extra = node.width() - naturalWidth;
        if (extra <= EPS) {
            return finalWidths;
        }

        List<Integer> autoColumns = new ArrayList<>();
        for (int index = 0; index < normalizedSpecs.size(); index++) {
            if (normalizedSpecs.get(index).isAuto()) {
                autoColumns.add(index);
            }
        }

        if (autoColumns.isEmpty()) {
            finalWidths[finalWidths.length - 1] += extra;
            return finalWidths;
        }

        double share = extra / autoColumns.size();
        for (Integer autoColumn : autoColumns) {
            finalWidths[autoColumn] += share;
        }
        return finalWidths;
    }

    private static double cellNaturalWidth(TableCellContent cell,
                                           TableCellLayoutStyle style,
                                           TextMeasurementSystem measurement) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        double maxWidth = 0.0;
        for (String line : sanitizeCellLines(cell)) {
            maxWidth = Math.max(maxWidth, measurement.textWidth(style.textStyle(), line));
        }
        return maxWidth + padding.horizontal();
    }

    private static double cellNaturalHeight(TableCellContent cell,
                                            TableCellLayoutStyle style,
                                            TextMeasurementSystem measurement) {
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        int lineCount = Math.max(1, sanitizeCellLines(cell).size());
        return (lineCount * measurement.lineHeight(style.textStyle()))
                + ((lineCount - 1) * tableCellLineSpacing(style))
                + padding.vertical();
    }

    private static double tableCellLineSpacing(TableCellLayoutStyle style) {
        return style.lineSpacing() == null ? 0.0 : style.lineSpacing();
    }

    private static List<TableColumnLayout> normalizeSpecs(TableNode node, int columnCount) {
        List<TableColumnLayout> normalized = new ArrayList<>(columnCount);
        List<TableColumnLayout> columns = toTableColumns(node.columns());
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            normalized.add(columnIndex < columns.size() ? columns.get(columnIndex) : TableColumnLayout.auto());
        }
        return List.copyOf(normalized);
    }

    private static int resolveColumnCount(TableNode node) {
        int maxRowColumns = node.rows().stream().mapToInt(List::size).max().orElse(0);
        return Math.max(node.columns().size(), maxRowColumns);
    }

    private static void validateRowsExist(TableNode node) {
        if (node.rows().isEmpty()) {
            throw new IllegalStateException("Table '" + displayName(node) + "' must contain at least one row.");
        }
    }

    private static void validateRowLengths(TableNode node, int columnCount) {
        for (int rowIndex = 0; rowIndex < node.rows().size(); rowIndex++) {
            int actual = node.rows().get(rowIndex).size();
            if (actual != columnCount) {
                throw new IllegalStateException("Row " + rowIndex + " has " + actual
                        + " cells but table requires " + columnCount + " columns.");
            }
        }
    }

    private static EnumSet<Side> borderSides(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        EnumSet<Side> sides = EnumSet.of(Side.BOTTOM, Side.RIGHT);
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.TOP);
        }
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            sides.add(Side.LEFT);
        }
        return sides;
    }

    private static Padding fillInsets(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        double topInset = topBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        double rightInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double bottomInset = strokeWidth(stylesByRow.get(rowIndex).get(columnIndex)) / 2.0;
        double leftInset = leftBoundaryStrokeWidth(stylesByRow, rowIndex, columnIndex) / 2.0;
        return new Padding(topInset, rightInset, bottomInset, leftInset);
    }

    private static double topBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsTopBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private static double leftBoundaryStrokeWidth(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        if (ownsLeftBoundary(stylesByRow, rowIndex, columnIndex)) {
            return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex));
        }
        return strokeWidth(stylesByRow.get(rowIndex).get(columnIndex - 1));
    }

    private static boolean ownsTopBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return rowIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex - 1).get(columnIndex));
    }

    private static boolean ownsLeftBoundary(List<List<TableCellLayoutStyle>> stylesByRow, int rowIndex, int columnIndex) {
        return columnIndex == 0 || !hasVisibleStroke(stylesByRow.get(rowIndex).get(columnIndex - 1));
    }

    private static double strokeWidth(TableCellLayoutStyle style) {
        if (style == null || style.stroke() == null) {
            return 0.0;
        }
        return style.stroke().width();
    }

    private static boolean hasVisibleStroke(TableCellLayoutStyle style) {
        return strokeWidth(style) > EPS;
    }

    private static List<String> sanitizeCellLines(TableCellContent cell) {
        List<String> sanitized = new ArrayList<>(cell.lines().size());
        for (String line : cell.lines()) {
            sanitized.add(line == null ? "" : line.replace('\r', ' ').replace('\n', ' '));
        }
        return List.copyOf(sanitized);
    }

    private static List<Double> toList(double[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (double value : values) {
            result.add(value);
        }
        return List.copyOf(result);
    }

    private static double sum(double[] values) {
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static String cellName(TableNode node, int rowIndex, int columnIndex) {
        return displayName(node) + "__row_" + rowIndex + "__cell_" + columnIndex;
    }

    private static String displayName(DocumentNode node) {
        if (node.name() == null || node.name().isBlank()) {
            return node.nodeKind();
        }
        return node.name();
    }

    private record ImageDimensions(double width, double height) {
    }

    private record PreparedParagraphLayout(
            List<String> logicalLines,
            List<ParagraphLine> visualLines,
            TextMeasurementSystem.LineMetrics lineMetrics,
            double baselineOffset,
            double lineHeight,
            double lineGap,
            double maxLineWidth,
            double totalHeight,
            boolean emitBookmark
    ) implements PreparedNodeLayout {
    }

    private record PreparedListItemLayout(
            String text,
            PreparedParagraphLayout paragraphLayout
    ) {
        private PreparedListItemLayout {
            text = text == null ? "" : text;
            paragraphLayout = Objects.requireNonNull(paragraphLayout, "paragraphLayout");
        }
    }

    private record PreparedListLayout(
            List<PreparedListItemLayout> items,
            double maxLineWidth,
            double totalHeight,
            double resolvedWidth
    ) implements PreparedNodeLayout {
        private PreparedListLayout {
            items = List.copyOf(items);
        }
    }

    private record PreparedTableLayout(
            ResolvedTableLayout resolvedLayout,
            boolean emitBookmark
    ) implements PreparedNodeLayout {
    }

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

    private record ResolvedTableLayout(
            List<Double> columnWidths,
            List<Double> rowHeights,
            List<List<TableResolvedCell>> rows,
            double naturalWidth,
            double finalWidth,
            double totalHeight
    ) {
    }

    private record InlineLayoutToken(
            String text,
            TextStyle textStyle,
            PdfLinkOptions linkOptions
    ) {
        private InlineLayoutToken {
            text = text == null ? "" : text;
            textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        }
    }
}



