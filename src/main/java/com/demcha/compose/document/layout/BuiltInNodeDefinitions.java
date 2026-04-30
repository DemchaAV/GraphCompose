package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentBorders;
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
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
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

    /**
     * One measured span inside a paragraph line. Sealed because the wrapping
     * algorithm can produce either text spans or image spans for the same
     * line — both contribute to wrapping width and per-line height.
     */
    public sealed interface ParagraphSpan permits ParagraphTextSpan, ParagraphImageSpan {
        /**
         * @return measured span width in points
         */
        double width();

        /**
         * @return optional link metadata anchored to this span
         */
        DocumentLinkOptions linkOptions();

        /**
         * @return effective height contribution for line metrics (font line
         *         height for text spans, image height for image spans)
         */
        double height();
    }

    /**
     * Measured text span inside a paragraph line.
     *
     * @param text visible text for the span
     * @param textStyle resolved text style
     * @param width measured span width
     * @param height font line height contribution
     * @param linkOptions optional link metadata for the span
     */
    public record ParagraphTextSpan(
            String text,
            TextStyle textStyle,
            double width,
            double height,
            DocumentLinkOptions linkOptions
    ) implements ParagraphSpan {
        /**
         * Creates a normalized measured paragraph text span.
         */
        public ParagraphTextSpan {
            text = text == null ? "" : text;
            textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
        }

        /**
         * Convenience constructor without link metadata.
         */
        public ParagraphTextSpan(String text, TextStyle textStyle, double width, double height) {
            this(text, textStyle, width, height, null);
        }
    }

    /**
     * Measured inline image span inside a paragraph line.
     *
     * @param imageData engine image payload, ready for the PDF backend
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment relative to the surrounding text
     * @param baselineOffset extra vertical offset in points; positive moves up
     * @param linkOptions optional link metadata
     */
    public record ParagraphImageSpan(
            ImageData imageData,
            double width,
            double height,
            InlineImageAlignment alignment,
            double baselineOffset,
            DocumentLinkOptions linkOptions
    ) implements ParagraphSpan {
        /**
         * Validates and normalizes inline image span fields.
         */
        public ParagraphImageSpan {
            Objects.requireNonNull(imageData, "imageData");
            alignment = alignment == null ? InlineImageAlignment.CENTER : alignment;
        }
    }

    /**
     * One measured paragraph line emitted to the PDF backend.
     *
     * @param text line text used for diagnostics and simple rendering paths
     * @param width measured line width
     * @param lineHeight resolved line height (max of text and image heights)
     * @param textLineHeight font-line-height for the dominant text style on
     *                       this line; equals {@code lineHeight} when no
     *                       inline image enlarges the line
     * @param textAscent ascent of the dominant text style on this line; used
     *                   to position image spans relative to the baseline
     * @param baselineOffsetFromBottom distance from line bottom to the text
     *                                 baseline
     * @param spans measured styled spans in source order
     */
    public record ParagraphLine(
            String text,
            double width,
            double lineHeight,
            double textLineHeight,
            double textAscent,
            double baselineOffsetFromBottom,
            List<ParagraphSpan> spans
    ) {
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
        DocumentLinkOptions linkOptions();

        /**
         * Returns bookmark metadata for the resolved fragment, or {@code null}
         * when no PDF outline entry should be emitted.
         *
         * @return fragment-level bookmark options, or {@code null}
         */
        DocumentBookmarkOptions bookmarkOptions();
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
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
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
     * @param cornerRadius rectangle corner radius in points
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record ShapeFragmentPayload(
            Color fillColor,
            Stroke stroke,
            double cornerRadius,
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions,
            SideBorders sideBorders
    ) implements PdfSemanticFragmentPayload {
        /**
         * Normalizes the render-only corner radius.
         */
        public ShapeFragmentPayload {
            if (cornerRadius < 0 || Double.isNaN(cornerRadius) || Double.isInfinite(cornerRadius)) {
                cornerRadius = 0.0;
            }
        }

        /**
         * Backwards-compatible constructor without per-side borders.
         */
        public ShapeFragmentPayload(Color fillColor,
                                     Stroke stroke,
                                     double cornerRadius,
                                     DocumentLinkOptions linkOptions,
                                     DocumentBookmarkOptions bookmarkOptions) {
            this(fillColor, stroke, cornerRadius, linkOptions, bookmarkOptions, null);
        }
    }

    /**
     * Resolved engine-level per-side border strokes attached to a shape payload.
     *
     * <p>Each side stroke is independent. {@code null} disables the corresponding
     * side. The renderer treats this record as a request to draw the four lines
     * separately rather than rely on a single uniform rectangle stroke.</p>
     *
     * @param top top side stroke, or {@code null} for no border on that side
     * @param right right side stroke, or {@code null} for no border on that side
     * @param bottom bottom side stroke, or {@code null} for no border on that side
     * @param left left side stroke, or {@code null} for no border on that side
     */
    public record SideBorders(Stroke top, Stroke right, Stroke bottom, Stroke left) {
        /**
         * Indicates whether at least one side carries a stroke.
         *
         * @return {@code true} when any side is set
         */
        public boolean hasAny() {
            return top != null || right != null || bottom != null || left != null;
        }
    }

    /**
     * PDF payload for a resolved line fragment.
     *
     * @param stroke line stroke
     * @param startX line start x offset inside the fragment
     * @param startY line start y offset inside the fragment
     * @param endX line end x offset inside the fragment
     * @param endY line end y offset inside the fragment
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record LineFragmentPayload(
            Stroke stroke,
            double startX,
            double startY,
            double endX,
            double endY,
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
    }

    /**
     * PDF payload for a resolved ellipse or circle fragment.
     *
     * @param fillColor optional fill color
     * @param stroke optional stroke
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record EllipseFragmentPayload(
            Color fillColor,
            Stroke stroke,
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
    }

    /**
     * Marker payload that opens a graphics-state clip region for the layers
     * inside a {@link ShapeContainerNode}. The fragment carries the absolute
     * outline rectangle (its {@code x}/{@code y}/{@code width}/{@code height}
     * already match the outline once placed) plus the policy that decided
     * whether the clip applies to the bounding box, the outline path, or is
     * skipped.
     *
     * <p>The PDF backend uses this payload to emit
     * {@code saveGraphicsState() + add path + clip()}; a matching
     * {@link ShapeClipEndPayload} fragment that arrives after every layer
     * fragment of the same container then emits {@code restoreGraphicsState()}.
     * Backends that cannot express path clipping (e.g. DOCX/POI) skip both
     * fragments and emit a capability warning instead.</p>
     *
     * @param outline outline geometry to clip against
     * @param policy clip policy chosen for the container; renderers may
     *               degrade {@link com.demcha.compose.document.style.ClipPolicy#CLIP_PATH}
     *               to {@link com.demcha.compose.document.style.ClipPolicy#CLIP_BOUNDS}
     *               when path clipping is unavailable
     * @param ownerPath semantic path of the owning container — used by
     *                  architecture-guard tests to verify the begin/end
     *                  pair balance
     */
    public record ShapeClipBeginPayload(
            com.demcha.compose.document.style.ShapeOutline outline,
            com.demcha.compose.document.style.ClipPolicy policy,
            String ownerPath
    ) {
        /**
         * Validates the outline / policy and normalizes the owner path.
         */
        public ShapeClipBeginPayload {
            Objects.requireNonNull(outline, "outline");
            Objects.requireNonNull(policy, "policy");
            ownerPath = ownerPath == null ? "" : ownerPath;
        }
    }

    /**
     * Marker payload that closes the graphics-state clip region opened by a
     * matching {@link ShapeClipBeginPayload}. The {@code ownerPath} is
     * carried so balance can be verified — every begin must have an end with
     * the same path, and they must arrive on the same page.
     *
     * @param ownerPath semantic path of the owning container; matches the
     *                  begin payload that opened the clip region
     */
    public record ShapeClipEndPayload(String ownerPath) {
        /**
         * Normalizes the owner path.
         */
        public ShapeClipEndPayload {
            ownerPath = ownerPath == null ? "" : ownerPath;
        }
    }

    /**
     * Marker payload that opens a graphics-state transform region for the
     * outline + layers of a {@link ShapeContainerNode} that carries a
     * non-identity {@link com.demcha.compose.document.style.DocumentTransform}.
     * The fragment uses the outline's placement rectangle so the renderer
     * can compute the rotation/scale centre as {@code (x + width/2,
     * y + height/2)}.
     *
     * <p>The PDF backend turns this into
     * {@code saveGraphicsState() + cm(matrix)}; a matching
     * {@link TransformEndPayload} fragment that arrives after every
     * other fragment of the same container then emits
     * {@code restoreGraphicsState()}. The transform brackets the entire
     * shape composite — outline, optional clip path, and every child
     * layer — so the whole unit rotates / scales together.</p>
     *
     * @param transform render-time transform to apply
     * @param ownerPath semantic path of the owning container — used by
     *                  architecture-guard tests to verify the begin/end
     *                  pair balance
     */
    public record TransformBeginPayload(
            com.demcha.compose.document.style.DocumentTransform transform,
            String ownerPath
    ) {
        /**
         * Validates the transform and normalizes the owner path.
         */
        public TransformBeginPayload {
            Objects.requireNonNull(transform, "transform");
            ownerPath = ownerPath == null ? "" : ownerPath;
        }
    }

    /**
     * Marker payload that closes a graphics-state transform region opened
     * by a matching {@link TransformBeginPayload}. Carries the
     * {@code ownerPath} for balance verification.
     *
     * @param ownerPath semantic path of the owning container; matches the
     *                  begin payload that opened the transform region
     */
    public record TransformEndPayload(String ownerPath) {
        /**
         * Normalizes the owner path.
         */
        public TransformEndPayload {
            ownerPath = ownerPath == null ? "" : ownerPath;
        }
    }

    /**
     * PDF payload for a resolved image fragment.
     *
     * @param imageData image source data
     * @param fitMode image fit policy used inside the resolved fragment
     * @param linkOptions optional fragment-level link metadata
     * @param bookmarkOptions optional fragment-level bookmark metadata
     */
    public record ImageFragmentPayload(
            ImageData imageData,
            DocumentImageFitMode fitMode,
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
    ) implements PdfSemanticFragmentPayload {
        /**
         * Normalizes optional fit policy.
         */
        public ImageFragmentPayload {
            fitMode = fitMode == null ? DocumentImageFitMode.STRETCH : fitMode;
        }
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
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
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
            DocumentLinkOptions linkOptions,
            DocumentBookmarkOptions bookmarkOptions
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
                            node.cornerRadius().radius(),
                            node.linkOptions(),
                            node.bookmarkOptions())));
        }
    }

    private static final class SpacerDefinition implements NodeDefinition<SpacerNode> {
        @Override
        public Class<SpacerNode> nodeType() {
            return SpacerNode.class;
        }

        @Override
        public PreparedNode<SpacerNode> prepare(SpacerNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(
                    node.width() + node.padding().horizontal(),
                    node.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(SpacerNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<SpacerNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            return List.of();
        }
    }

    private static final class LineDefinition implements NodeDefinition<LineNode> {
        @Override
        public Class<LineNode> nodeType() {
            return LineNode.class;
        }

        @Override
        public PreparedNode<LineNode> prepare(LineNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(
                    node.width() + node.padding().horizontal(),
                    node.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(LineNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<LineNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            LineNode node = prepared.node();
            double width = Math.max(0.0, placement.width() - node.padding().horizontal());
            double height = Math.max(0.0, placement.height() - node.padding().vertical());
            if (width <= EPS && height <= EPS) {
                return List.of();
            }
            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    node.padding().left(),
                    node.padding().bottom(),
                    width,
                    height,
                    new LineFragmentPayload(
                            toStroke(node.stroke()),
                            node.startX(),
                            node.startY(),
                            node.endX(),
                            node.endY(),
                            node.linkOptions(),
                            node.bookmarkOptions())));
        }
    }

    private static final class EllipseDefinition implements NodeDefinition<EllipseNode> {
        @Override
        public Class<EllipseNode> nodeType() {
            return EllipseNode.class;
        }

        @Override
        public PreparedNode<EllipseNode> prepare(EllipseNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(
                    node.width() + node.padding().horizontal(),
                    node.height() + node.padding().vertical()));
        }

        @Override
        public PaginationPolicy paginationPolicy(EllipseNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<EllipseNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            EllipseNode node = prepared.node();
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
                    new EllipseFragmentPayload(
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
                    new ImageFragmentPayload(
                            toImageData(node.imageData()),
                            node.fitMode(),
                            node.linkOptions(),
                            node.bookmarkOptions())));
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
            ContainerNode node = prepared.node();
            return emitDecorationFragment(
                    node.fillColor() == null ? null : node.fillColor().color(),
                    toStroke(node.stroke()),
                    node.cornerRadius().radius(),
                    toSideBorders(node.borders()),
                    placement);
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
            SectionNode node = prepared.node();
            return emitDecorationFragment(
                    node.fillColor() == null ? null : node.fillColor().color(),
                    toStroke(node.stroke()),
                    node.cornerRadius().radius(),
                    toSideBorders(node.borders()),
                    placement);
        }
    }

    private static final class RowDefinition implements NodeDefinition<RowNode> {
        @Override
        public Class<RowNode> nodeType() {
            return RowNode.class;
        }

        @Override
        public PreparedNode<RowNode> prepare(RowNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.composite(
                    node,
                    measureRow(node, toPadding(node.padding()), ctx, constraints),
                    new CompositeLayoutSpec(node.gap(), CompositeLayoutSpec.Axis.HORIZONTAL, node.weights()));
        }

        @Override
        public PaginationPolicy paginationPolicy(RowNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<DocumentNode> children(RowNode node) {
            return node.children();
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<RowNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            RowNode node = prepared.node();
            return emitDecorationFragment(
                    node.fillColor() == null ? null : node.fillColor().color(),
                    toStroke(node.stroke()),
                    node.cornerRadius().radius(),
                    toSideBorders(node.borders()),
                    placement);
        }
    }

    /**
     * Prepared layout payload attached to {@link LayerStackNode} prepared nodes.
     *
     * <p>Carries per-layer alignment plus an on-screen offset (positive
     * {@code offsetX} = right, positive {@code offsetY} = down) and an
     * explicit per-layer {@code zIndex}. The layout compiler stable-sorts
     * layers by ascending {@code zIndex} before emitting fragments, so a
     * later-declared layer with {@code zIndex = 10} renders on top of an
     * earlier-declared layer with {@code zIndex = 5}.</p>
     *
     * @param alignments per-layer alignments resolved from the source order
     * @param offsetsX per-layer horizontal offsets from the alignment anchor
     * @param offsetsY per-layer vertical offsets from the alignment anchor
     * @param zIndices per-layer render-order keys (defaults are {@code 0})
     */
    public record PreparedStackLayout(
            List<LayerAlign> alignments,
            List<Double> offsetsX,
            List<Double> offsetsY,
            List<Integer> zIndices) implements PreparedNodeLayout {
        /**
         * Creates a prepared stack layout payload with frozen alignment, offset,
         * and z-index metadata.
         */
        public PreparedStackLayout {
            alignments = List.copyOf(alignments);
            offsetsX = List.copyOf(offsetsX);
            offsetsY = List.copyOf(offsetsY);
            zIndices = List.copyOf(zIndices);
            if (offsetsX.size() != alignments.size()
                    || offsetsY.size() != alignments.size()
                    || zIndices.size() != alignments.size()) {
                throw new IllegalArgumentException(
                        "PreparedStackLayout: alignments/offsets/zIndices size mismatch ("
                                + alignments.size() + "/" + offsetsX.size() + "/"
                                + offsetsY.size() + "/" + zIndices.size() + ")");
            }
        }

        /**
         * Backward-compatible 3-arg constructor — defaults zIndices to all
         * zeros so layers render in source order.
         *
         * @param alignments per-layer alignments
         * @param offsetsX per-layer horizontal offsets
         * @param offsetsY per-layer vertical offsets
         */
        public PreparedStackLayout(List<LayerAlign> alignments,
                                   List<Double> offsetsX,
                                   List<Double> offsetsY) {
            this(alignments, offsetsX, offsetsY, zeroInts(alignments.size()));
        }

        /**
         * Backward-compatible factory for callers that only carry alignments;
         * fills both offset lists with zeros and zIndices with zeros.
         *
         * @param alignments per-layer alignments
         */
        public PreparedStackLayout(List<LayerAlign> alignments) {
            this(alignments, zeros(alignments.size()), zeros(alignments.size()), zeroInts(alignments.size()));
        }

        private static List<Double> zeros(int size) {
            Double[] out = new Double[size];
            java.util.Arrays.fill(out, 0.0);
            return List.of(out);
        }

        private static List<Integer> zeroInts(int size) {
            Integer[] out = new Integer[size];
            java.util.Arrays.fill(out, 0);
            return List.of(out);
        }
    }

    private static final class ShapeContainerDefinition implements NodeDefinition<ShapeContainerNode> {
        @Override
        public Class<ShapeContainerNode> nodeType() {
            return ShapeContainerNode.class;
        }

        @Override
        public PreparedNode<ShapeContainerNode> prepare(ShapeContainerNode node, PrepareContext ctx, BoxConstraints constraints) {
            // The bounding box is dictated by the outline plus padding — children
            // do not influence the container size (unlike LayerStackNode where
            // bbox = max(child outer size)). We still PRE-PREPARE children so
            // their own prepared payload is cached for compileStackedLayer.
            ShapeOutline outline = node.outline();
            Padding padding = toPadding(node.padding());
            double measureWidth = outline.width() + padding.horizontal();
            double measureHeight = outline.height() + padding.vertical();
            double innerWidthForChildren = outline.width();
            for (LayerStackNode.Layer layer : node.layers()) {
                DocumentNode child = layer.node();
                double childInner = Math.max(0.0, innerWidthForChildren - child.margin().horizontal());
                ctx.prepare(child, BoxConstraints.natural(childInner));
            }

            int n = node.layers().size();
            List<LayerAlign> alignments = new ArrayList<>(n);
            List<Double> offsetsX = new ArrayList<>(n);
            List<Double> offsetsY = new ArrayList<>(n);
            List<Integer> zIndices = new ArrayList<>(n);
            for (LayerStackNode.Layer layer : node.layers()) {
                alignments.add(layer.align());
                offsetsX.add(layer.offsetX());
                offsetsY.add(layer.offsetY());
                zIndices.add(layer.zIndex());
            }

            return PreparedNode.composite(
                    node,
                    new MeasureResult(measureWidth, measureHeight),
                    new PreparedStackLayout(alignments, offsetsX, offsetsY, zIndices),
                    new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
        }

        @Override
        public PaginationPolicy paginationPolicy(ShapeContainerNode node) {
            // SHAPE_ATOMIC reuses the atomic page-break path (shape + every
            // layer stay together), but distinguishes itself from plain
            // ATOMIC for downstream consumers — render handlers that need
            // to apply a clip path, and snapshots that mark "this is a
            // shape-clipped atom, not a bbox-only atom".
            return PaginationPolicy.SHAPE_ATOMIC;
        }

        @Override
        public List<DocumentNode> children(ShapeContainerNode node) {
            return node.children();
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<ShapeContainerNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            ShapeContainerNode node = prepared.node();
            ShapeOutline outline = node.outline();
            // Outline is rendered inside the container's inner box: that's
            // placement minus padding, which by construction equals outline.size().
            double padLeft = node.padding().left();
            double padBottom = node.padding().bottom();
            double width = outline.width();
            double height = outline.height();
            if (width <= EPS || height <= EPS) {
                return List.of();
            }
            Color awtFill = node.fillColor() == null ? null : node.fillColor().color();
            Stroke stroke = toStroke(node.stroke());
            LayoutFragment outlineFragment = switch (outline) {
                case ShapeOutline.Ellipse ignored -> new LayoutFragment(
                        placement.path(),
                        0,
                        padLeft,
                        padBottom,
                        width,
                        height,
                        new EllipseFragmentPayload(awtFill, stroke, null, null));
                case ShapeOutline.Rectangle ignored -> new LayoutFragment(
                        placement.path(),
                        0,
                        padLeft,
                        padBottom,
                        width,
                        height,
                        new ShapeFragmentPayload(awtFill, stroke, 0.0, null, null, null));
                case ShapeOutline.RoundedRectangle r -> new LayoutFragment(
                        placement.path(),
                        0,
                        padLeft,
                        padBottom,
                        width,
                        height,
                        new ShapeFragmentPayload(awtFill, stroke, r.cornerRadius(), null, null, null));
            };
            // Three independent bracketing concerns:
            //   * Transform — wraps EVERYTHING (outline + clip + layers) so
            //     the composite rotates / scales as one unit. Goes first
            //     and matches a transform-end emitted from
            //     emitOverlayFragments after every layer fragment.
            //   * Outline — the visible fill / stroke.
            //   * Clip — wraps only the layers (not the outline draw),
            //     so the outline's own fill always shows but children are
            //     restricted to the path.
            // For OVERFLOW_VISIBLE we drop both clip markers; for an
            // identity transform we drop both transform markers. Either
            // marker type pairs by ownerPath so multiple containers nest
            // safely.
            List<LayoutFragment> opening = new ArrayList<>(4);
            boolean hasTransform = !node.transform().isIdentity();
            if (hasTransform) {
                opening.add(new LayoutFragment(
                        placement.path(),
                        0,
                        padLeft,
                        padBottom,
                        width,
                        height,
                        new TransformBeginPayload(node.transform(), placement.path())));
            }
            opening.add(outlineFragment);
            if (node.clipPolicy() != com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE) {
                opening.add(new LayoutFragment(
                        placement.path(),
                        1,
                        padLeft,
                        padBottom,
                        width,
                        height,
                        new ShapeClipBeginPayload(outline, node.clipPolicy(), placement.path())));
            }
            return List.copyOf(opening);
        }

        @Override
        public List<LayoutFragment> emitOverlayFragments(PreparedNode<ShapeContainerNode> prepared,
                                                         FragmentContext ctx,
                                                         FragmentPlacement placement) {
            ShapeContainerNode node = prepared.node();
            boolean hasClip = node.clipPolicy() != com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE;
            boolean hasTransform = !node.transform().isIdentity();
            if (!hasClip && !hasTransform) {
                return List.of();
            }
            // End markers are graphics-state restore ops — no spatial
            // extent. The owning placement contributes page index + path
            // so begin and end land on the same page surface. Order is
            // the inverse of opening (innermost-first close): clip-end
            // first, then transform-end.
            List<LayoutFragment> closing = new ArrayList<>(2);
            if (hasClip) {
                closing.add(new LayoutFragment(
                        placement.path(),
                        2,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        new ShapeClipEndPayload(placement.path())));
            }
            if (hasTransform) {
                closing.add(new LayoutFragment(
                        placement.path(),
                        3,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        new TransformEndPayload(placement.path())));
            }
            return List.copyOf(closing);
        }
    }

    private static final class LayerStackDefinition implements NodeDefinition<LayerStackNode> {
        @Override
        public Class<LayerStackNode> nodeType() {
            return LayerStackNode.class;
        }

        @Override
        public PreparedNode<LayerStackNode> prepare(LayerStackNode node, PrepareContext ctx, BoxConstraints constraints) {
            int n = node.layers().size();
            List<LayerAlign> alignments = new java.util.ArrayList<>(n);
            List<Double> offsetsX = new java.util.ArrayList<>(n);
            List<Double> offsetsY = new java.util.ArrayList<>(n);
            List<Integer> zIndices = new java.util.ArrayList<>(n);
            for (LayerStackNode.Layer layer : node.layers()) {
                alignments.add(layer.align());
                offsetsX.add(layer.offsetX());
                offsetsY.add(layer.offsetY());
                zIndices.add(layer.zIndex());
            }
            return PreparedNode.composite(
                    node,
                    measureStack(node, toPadding(node.padding()), ctx, constraints),
                    new PreparedStackLayout(alignments, offsetsX, offsetsY, zIndices),
                    new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
        }

        @Override
        public PaginationPolicy paginationPolicy(LayerStackNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<DocumentNode> children(LayerStackNode node) {
            return node.children();
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<LayerStackNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            return List.of();
        }
    }

    private static MeasureResult measureStack(LayerStackNode node,
                                              Padding padding,
                                              PrepareContext ctx,
                                              BoxConstraints constraints) {
        double availableWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        double maxOuterWidth = 0.0;
        double maxOuterHeight = 0.0;
        for (LayerStackNode.Layer layer : node.layers()) {
            DocumentNode child = layer.node();
            double childInner = Math.max(0.0, availableWidth - child.margin().horizontal());
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
            double outerWidth = childPrepared.measureResult().width() + child.margin().horizontal();
            double outerHeight = childPrepared.measureResult().height() + child.margin().vertical();
            if (outerWidth > maxOuterWidth) {
                maxOuterWidth = outerWidth;
            }
            if (outerHeight > maxOuterHeight) {
                maxOuterHeight = outerHeight;
            }
        }
        double width = Math.min(constraints.availableWidth(), padding.horizontal() + maxOuterWidth);
        return new MeasureResult(width, padding.vertical() + maxOuterHeight);
    }

    private static MeasureResult measureRow(RowNode node,
                                            Padding padding,
                                            PrepareContext ctx,
                                            BoxConstraints constraints) {
        double availableWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        int n = node.children().size();
        double gap = node.gap();
        double totalGap = n > 1 ? gap * (n - 1) : 0.0;
        double slotsTotal = Math.max(0.0, availableWidth - totalGap);
        double[] slotWidths = new double[n];
        if (node.weights().isEmpty()) {
            double share = n > 0 ? slotsTotal / n : 0.0;
            for (int i = 0; i < n; i++) {
                slotWidths[i] = share;
            }
        } else {
            double total = 0.0;
            for (Double w : node.weights()) {
                total += w;
            }
            for (int i = 0; i < n; i++) {
                slotWidths[i] = total > 0.0 ? slotsTotal * (node.weights().get(i) / total) : (n > 0 ? slotsTotal / n : 0.0);
            }
        }

        double maxChildHeight = 0.0;
        for (int i = 0; i < n; i++) {
            DocumentNode child = node.children().get(i);
            double childInner = Math.max(0.0, slotWidths[i] - child.margin().horizontal());
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
            double childOuter = childPrepared.measureResult().height() + child.margin().vertical();
            if (childOuter > maxChildHeight) {
                maxChildHeight = childOuter;
            }
        }

        return new MeasureResult(constraints.availableWidth(), padding.vertical() + maxChildHeight);
    }

    private static final class TableDefinition implements NodeDefinition<TableNode> {
        @Override
        public Class<TableNode> nodeType() {
            return TableNode.class;
        }

        @Override
        public PreparedNode<TableNode> prepare(TableNode node, PrepareContext ctx, BoxConstraints constraints) {
            TableLayoutSupport.ResolvedTableLayout layout = TableLayoutSupport.resolveTableLayout(
                    node, ctx.textMeasurement(), constraints.availableWidth());
            MeasureResult measure = new MeasureResult(
                    layout.finalWidth() + node.padding().horizontal(),
                    layout.totalHeight() + node.padding().vertical());
            return PreparedNode.leaf(node, measure,
                    new TableLayoutSupport.PreparedTableLayout(layout, node.bookmarkOptions() != null));
        }

        @Override
        public PaginationPolicy paginationPolicy(TableNode node) {
            return PaginationPolicy.SPLITTABLE;
        }

        @Override
        public PreparedSplitResult<TableNode> split(PreparedNode<TableNode> prepared, SplitRequest request) {
            TableNode node = prepared.node();
            TableLayoutSupport.ResolvedTableLayout layout = prepared
                    .requirePreparedLayout(TableLayoutSupport.PreparedTableLayout.class)
                    .resolvedLayout();
            double innerAvailableHeight = Math.max(0.0, request.remainingHeight() - node.padding().vertical());

            int totalRows = node.rows().size();
            int headerCount = node.repeatedHeaderRowCount();
            // Defensive clamp — TableNode's invariant already ensures
            // headerCount <= totalRows, but it does not guarantee that
            // BOTH leading rows are present after a continuation split.
            // (A tail slice retains the original headerCount even if its
            // rows were trimmed for some reason.)
            if (headerCount > totalRows) {
                headerCount = totalRows;
            }

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
            if (rowCount >= totalRows) {
                return PreparedSplitResult.whole(prepared);
            }

            // Only meaningful to split if there's at least one body row
            // ON the head AFTER the header. Otherwise no progress and we
            // would loop forever.
            if (headerCount > 0 && rowCount <= headerCount) {
                return new PreparedSplitResult<>(null, prepared);
            }

            PreparedNode<TableNode> head = TableLayoutSupport.sliceTablePreparedNode(
                    node, layout, 0, rowCount, true, false);
            // Tail re-emits the header rows on the next page. The slice
            // helper prepends rows [0, headerCount) from the source
            // (which themselves are the header in a previously-built
            // tail because each split preserves the prefix invariant).
            PreparedNode<TableNode> tail = TableLayoutSupport.sliceTablePreparedNode(
                    node, layout, rowCount, totalRows, false, true, headerCount);
            return new PreparedSplitResult<>(head, tail);
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<TableNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            TableNode node = prepared.node();
            TableLayoutSupport.PreparedTableLayout preparedLayout = prepared
                    .requirePreparedLayout(TableLayoutSupport.PreparedTableLayout.class);
            TableLayoutSupport.ResolvedTableLayout layout = preparedLayout.resolvedLayout();

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

    private static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                               Stroke stroke,
                                                               double cornerRadius,
                                                               FragmentPlacement placement) {
        return emitDecorationFragment(fillColor, stroke, cornerRadius, null, placement);
    }

    private static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                               Stroke stroke,
                                                               double cornerRadius,
                                                               SideBorders sideBorders,
                                                               FragmentPlacement placement) {
        boolean hasFill = fillColor != null;
        boolean hasStroke = stroke != null
                && stroke.strokeColor() != null
                && stroke.strokeColor().color() != null
                && stroke.width() > 0;
        boolean hasSideBorders = sideBorders != null && sideBorders.hasAny();
        if ((!hasFill && !hasStroke && !hasSideBorders) || placement.width() <= EPS || placement.height() <= EPS) {
            return List.of();
        }

        return List.of(new LayoutFragment(
                placement.path(),
                0,
                0.0,
                0.0,
                placement.width(),
                placement.height(),
                new ShapeFragmentPayload(fillColor, stroke, cornerRadius, null, null, sideBorders)));
    }

    private static SideBorders toSideBorders(DocumentBorders borders) {
        if (borders == null || !borders.hasAny()) {
            return null;
        }
        return new SideBorders(
                toStroke(borders.top()),
                toStroke(borders.right()),
                toStroke(borders.bottom()),
                toStroke(borders.left()));
    }

    private static BarcodeData toBarcodeData(DocumentBarcodeOptions options) {
        DocumentBarcodeType type = options.getType() == null ? DocumentBarcodeType.QR_CODE : options.getType();
        return BarcodeData.of(
                options.getContent(),
                switch (type) {
                    case CODE_128 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_128;
                    case CODE_39 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_39;
                    case EAN_13 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_13;
                    case EAN_8 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_8;
                    case UPC_A -> com.demcha.compose.engine.components.content.barcode.BarcodeType.UPC_A;
                    case PDF_417 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.PDF_417;
                    case DATA_MATRIX -> com.demcha.compose.engine.components.content.barcode.BarcodeType.DATA_MATRIX;
                    case QR_CODE -> com.demcha.compose.engine.components.content.barcode.BarcodeType.QR_CODE;
                },
                options.getForeground() == null ? Color.BLACK : options.getForeground().color(),
                options.getBackground() == null ? Color.WHITE : options.getBackground().color(),
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
        } else if (node.scale() != null) {
            width = intrinsicWidth * node.scale();
            height = intrinsicHeight * node.scale();
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
                    BoxConstraints.natural(Math.max(0.0, innerWidth - child.margin().horizontal())));
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
