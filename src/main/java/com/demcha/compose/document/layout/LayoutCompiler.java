package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageBreakNode;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toMargin;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toPadding;

/**
 * Compiles a semantic document graph into a canonical fixed-layout graph.
 */
public final class LayoutCompiler {
    private static final Logger LAYOUT_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.layout");
    private static final Logger PAGINATION_LOG = LoggerFactory.getLogger("com.demcha.compose.engine.pagination");
    private static final double EPS = 1e-6;

    /**
     * Tolerance applied when comparing a node's required outer height
     * against the full page capacity. EPS (1e-6) is floating-point noise
     * tolerance; CAPACITY_TOLERANCE absorbs the discrepancy between
     * rounded human input (e.g. {@code 842.0} for an A4 height) and the
     * exact PDF point value ({@code DocumentPageSize.A4.height() ==
     * 841.88977}). 0.5 pt ≈ 0.18 mm — visually indistinguishable, while
     * a > 1 pt overflow still throws {@link AtomicNodeTooLargeException}.
     *
     * <p>Use EPS for floating-point noise inside split / remaining-height
     * decisions; reserve CAPACITY_TOLERANCE for the "does this fit on a
     * full page at all?" check.</p>
     */
    private static final double CAPACITY_TOLERANCE = 0.5;

    /**
     * Identifies the kind of fixed slot a child is being compiled into,
     * so the validator can distinguish "child of a horizontal row band"
     * (where a nested {@code Row} would create real composition conflict)
     * from "child of a {@link com.demcha.compose.document.node.LayerStackNode}
     * layer" (where a nested {@code Row} is a normal column-row inside an
     * already-fixed layer rectangle).
     *
     * <p>{@link #compileNodeInFixedSlot} takes the kind as a parameter
     * and propagates it down recursive calls so the validator can
     * relax just for STACK layer parents.</p>
     */
    private enum FixedSlotKind {
        /** Child sits inside a horizontal row band (column of a row). */
        ROW_SLOT,
        /** Child sits inside a {@link LayerStackNode} layer rectangle. */
        STACK_LAYER_SLOT
    }

    private final NodeRegistry registry;

    /**
     * Creates a compiler with the node definition registry used for measurement,
     * splitting, and fragment emission.
     *
     * @param registry semantic node definition registry
     */
    public LayoutCompiler(NodeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Compiles semantic roots into placed nodes and renderable fragments.
     *
     * @param graph semantic document graph
     * @param prepareContext node preparation context
     * @param fragmentContext fragment emission context
     * @return fixed-layout graph ready for rendering or snapshot extraction
     */
    public LayoutGraph compile(DocumentGraph graph, PrepareContext prepareContext, FragmentContext fragmentContext) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(prepareContext, "prepareContext");
        Objects.requireNonNull(fragmentContext, "fragmentContext");

        long startNanos = System.nanoTime();
        LayoutCanvas canvas = prepareContext.canvas();
        LAYOUT_LOG.debug("layout.compile.start roots={} canvas={}x{}", graph.roots().size(), Math.round(canvas.width()), Math.round(canvas.height()));
        PAGINATION_LOG.debug("pagination.compile.start roots={} innerHeight={}", graph.roots().size(), Math.round(canvas.innerHeight()));
        CompilerState state = new CompilerState(canvas);
        List<PlacedNode> nodes = new ArrayList<>();
        List<PlacedFragment> fragments = new ArrayList<>();

        for (int index = 0; index < graph.roots().size(); index++) {
            DocumentNode root = graph.roots().get(index);
            compileNode(
                    prepareForRegionWidth(prepareContext, root, canvas.innerWidth()),
                    null,
                    index,
                    1,
                    canvas.margin().left(),
                    canvas.innerWidth(),
                    state,
                    prepareContext,
                    fragmentContext,
                    nodes,
                    fragments);
        }

        int totalPages = nodes.isEmpty() && fragments.isEmpty()
                ? 0
                : state.maxTouchedPage + 1;
        LayoutGraph layoutGraph = new LayoutGraph(canvas, totalPages, nodes, fragments);
        PAGINATION_LOG.debug(
                "pagination.compile.end roots={} pages={} nodes={} fragments={} durationMs={}",
                graph.roots().size(),
                totalPages,
                nodes.size(),
                fragments.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        LAYOUT_LOG.debug(
                "layout.compile.end roots={} pages={} nodes={} fragments={} durationMs={}",
                graph.roots().size(),
                totalPages,
                nodes.size(),
                fragments.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        return layoutGraph;
    }

    private void compileNode(PreparedNode<DocumentNode> prepared,
                             String parentPath,
                             int childIndex,
                             int depth,
                             double regionX,
                             double regionWidth,
                             CompilerState state,
                             PrepareContext prepareContext,
                             FragmentContext fragmentContext,
                             List<PlacedNode> nodes,
                             List<PlacedFragment> fragments) {
        DocumentNode node = prepared.node();
        @SuppressWarnings("unchecked")
        NodeDefinition<DocumentNode> definition = (NodeDefinition<DocumentNode>) registry.definitionFor(node);
        String path = pathFor(node, parentPath, childIndex);
        String semanticName = semanticName(node);
        Margin margin = toMargin(node.margin());
        Padding padding = toPadding(node.padding());
        double availableWidth = childAvailableWidth(regionWidth, node);

        if (node instanceof PageBreakNode) {
            compilePageBreak(path, semanticName, parentPath, childIndex, depth, regionX, state, nodes);
            return;
        }

        if (availableWidth <= EPS) {
            throw new IllegalStateException("Node '" + path
                    + "' has no horizontal layout space. "
                    + "Reduce padding or margin on the parent, or increase the page width.");
        }

        MeasureResult naturalMeasure = prepared.measureResult();
        if (naturalMeasure.width() > availableWidth + EPS) {
            throw new IllegalStateException("Node '" + path + "' measured width " + naturalMeasure.width()
                    + " exceeds available width " + availableWidth + ". "
                    + "Reduce the node width, shorten inline content, or wrap content in a smaller container.");
        }

        if (prepared.isComposite()) {
            compileComposite(prepared, definition, path, semanticName, parentPath, childIndex, depth, regionX, regionWidth,
                    state, prepareContext, fragmentContext, nodes, fragments);
            return;
        }

        if (definition.paginationPolicy(node) == PaginationPolicy.SPLITTABLE) {
            compileSplittableLeaf(prepared, definition, path, semanticName, parentPath, childIndex, depth, regionX,
                    availableWidth, state, prepareContext, fragmentContext, nodes, fragments);
            return;
        }

        compileAtomicLeaf(prepared, definition, path, semanticName, parentPath, childIndex, depth, regionX,
                availableWidth, state, prepareContext, fragmentContext, nodes, fragments);
    }

    private void compilePageBreak(String path,
                                  String semanticName,
                                  String parentPath,
                                  int childIndex,
                                  int depth,
                                  double regionX,
                                  CompilerState state,
                                  List<PlacedNode> nodes) {
        boolean touchedDocument = state.maxTouchedPage >= 0 || state.usedHeight > EPS;
        if (touchedDocument) {
            state.touchPage();
            state.newPage();
        }

        double placementX = regionX;
        double placementY = state.pageTop();
        nodes.add(new PlacedNode(
                path,
                semanticName,
                PageBreakNode.class.getSimpleName(),
                parentPath,
                childIndex,
                depth,
                depth,
                placementX,
                placementY,
                placementX,
                placementY,
                0.0,
                0.0,
                state.pageIndex,
                state.pageIndex,
                0.0,
                0.0,
                Margin.zero(),
                Padding.zero()));
    }

    private void compileComposite(PreparedNode<DocumentNode> prepared,
                                  NodeDefinition<DocumentNode> definition,
                                  String path,
                                  String semanticName,
                                  String parentPath,
                                  int childIndex,
                                  int depth,
                                  double regionX,
                                  double regionWidth,
                                  CompilerState state,
                                  PrepareContext prepareContext,
                                  FragmentContext fragmentContext,
                                  List<PlacedNode> nodes,
                                  List<PlacedFragment> fragments) {
        DocumentNode node = prepared.node();
        Margin margin = toMargin(node.margin());
        Padding padding = toPadding(node.padding());
        double availableWidth = childAvailableWidth(regionWidth, node);
        CompositeLayoutSpec layoutSpec = prepared.requireCompositeLayout();
        MeasureResult naturalMeasure = prepared.measureResult();

        if (layoutSpec.axis() == CompositeLayoutSpec.Axis.HORIZONTAL) {
            compileHorizontalRow(prepared, definition, path, semanticName, parentPath, childIndex, depth,
                    regionX, regionWidth, state, prepareContext, fragmentContext, nodes, fragments,
                    margin, padding, availableWidth, layoutSpec, naturalMeasure);
            return;
        }

        if (layoutSpec.axis() == CompositeLayoutSpec.Axis.STACK) {
            compileStackedLayer(prepared, definition, path, semanticName, parentPath, childIndex, depth,
                    regionX, state, prepareContext, fragmentContext, nodes, fragments,
                    margin, padding, availableWidth, naturalMeasure);
            return;
        }

        // Opt-in keep-together: a node that requests it relocates whole to the
        // next page when it does not fit in the remaining space but would fit on a
        // fresh page — avoiding an orphaned heading above atomic content (e.g. a
        // chart). Nodes taller than a full page must still flow. Default-off, so
        // existing layouts are unchanged.
        double outerHeight = naturalMeasure.height() + margin.vertical();
        boolean keepWhole = node.keepTogether()
                && outerHeight <= state.canvas.innerHeight() + CAPACITY_TOLERANCE;
        double startReservation = margin.top() + padding.top();
        if (keepWhole && outerHeight > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        } else if (startReservation > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();

        int startPage = state.pageIndex;
        double placementX = regionX + margin.left();
        double placementTopY = state.pageTop() - state.usedHeight - margin.top();
        double placementY = placementTopY - naturalMeasure.height();
        int decorationInsertIndex = fragments.size();
        int nodeIndex = nodes.size();
        nodes.add(null);

        advanceSpace(startReservation, state);
        List<DocumentNode> children = definition.children(node);
        double childRegionX = placementX + padding.left();
        double childRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());

        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            compileNode(
                    prepareForRegionWidth(prepareContext, child, childRegionWidth),
                    path,
                    index,
                    depth + 1,
                    childRegionX,
                    childRegionWidth,
                    state,
                    prepareContext,
                    fragmentContext,
                    nodes,
                    fragments);
            if (index < children.size() - 1) {
                advanceSpace(layoutSpec.spacing(), state);
            }
        }

        advanceSpace(padding.bottom() + margin.bottom(), state);
        int endPage = state.pageIndex;
        double endPageBottomY = state.pageTop() - state.usedHeight + margin.bottom();
        List<PlacedFragment> decorationFragments = compositeDecorationFragments(
                prepared,
                definition,
                path,
                parentPath,
                childIndex,
                depth,
                placementX,
                placementTopY,
                endPageBottomY,
                naturalMeasure.width(),
                startPage,
                endPage,
                margin,
                padding,
                state.canvas,
                fragmentContext);
        if (!decorationFragments.isEmpty()) {
            fragments.addAll(decorationInsertIndex, decorationFragments);
        }
        nodes.set(nodeIndex, new PlacedNode(
                path,
                semanticName,
                node.nodeKind(),
                parentPath,
                childIndex,
                depth,
                depth,
                placementX,
                placementY,
                placementX,
                placementY,
                naturalMeasure.width(),
                naturalMeasure.height(),
                startPage,
                endPage,
                naturalMeasure.width(),
                naturalMeasure.height(),
                margin,
                padding));
    }

    private void compileHorizontalRow(PreparedNode<DocumentNode> prepared,
                                      NodeDefinition<DocumentNode> definition,
                                      String path,
                                      String semanticName,
                                      String parentPath,
                                      int childIndex,
                                      int depth,
                                      double regionX,
                                      double regionWidth,
                                      CompilerState state,
                                      PrepareContext prepareContext,
                                      FragmentContext fragmentContext,
                                      List<PlacedNode> nodes,
                                      List<PlacedFragment> fragments,
                                      Margin margin,
                                      Padding padding,
                                      double availableWidth,
                                      CompositeLayoutSpec layoutSpec,
                                      MeasureResult naturalMeasure) {
        DocumentNode node = prepared.node();
        double rowOuterHeight = naturalMeasure.height() + margin.vertical();
        double fullPageHeight = state.canvas.innerHeight();
        if (rowOuterHeight > fullPageHeight + CAPACITY_TOLERANCE) {
            throw atomicTooLarge(path, rowOuterHeight, fullPageHeight);
        }
        if (rowOuterHeight > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();

        int startPage = state.pageIndex;
        double placementX = regionX + margin.left();
        double placementTopY = state.pageTop() - state.usedHeight - margin.top();
        double placementY = placementTopY - naturalMeasure.height();
        int decorationInsertIndex = fragments.size();
        int rowNodeIndex = nodes.size();
        nodes.add(null);

        List<DocumentNode> children = definition.children(node);
        double childRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());
        double rowInnerY = placementTopY - padding.top();

        if (!children.isEmpty()) {
            double[] slotWidths = distributeRowSlotWidths(children, layoutSpec.weights(),
                    layoutSpec.spacing(), childRegionWidth);
            double cursorX = placementX + padding.left();

            for (int index = 0; index < children.size(); index++) {
                DocumentNode child = children.get(index);
                Margin childMargin = toMargin(child.margin());
                double slotWidth = slotWidths[index];
                double childRegionX = cursorX + childMargin.left();
                double childInnerWidth = Math.max(0.0, slotWidth - childMargin.horizontal());

                PreparedNode<DocumentNode> childPrepared =
                        prepareForRegionWidth(prepareContext, child, childInnerWidth);
                MeasureResult childMeasure = childPrepared.measureResult();
                @SuppressWarnings("unchecked")
                NodeDefinition<DocumentNode> childDefinition =
                        (NodeDefinition<DocumentNode>) registry.definitionFor(child);
                if (childMeasure.height() > naturalMeasure.height() - padding.vertical() + EPS) {
                    throw new IllegalStateException("Row '" + path + "' child '" + child.nodeKind()
                            + "' measured height " + childMeasure.height() + " exceeds row inner height. "
                            + "Reduce the child height, shorten its content, or increase the row height.");
                }

                if (childPrepared.isComposite()) {
                    PlacementContext slotCtx = new FixedSlotPlacementContext(
                            state.pageIndex, state.canvas, prepareContext, fragmentContext, nodes, fragments);
                    // Column of a horizontal row band — keep the strict
                    // ROW_SLOT validator so a nested horizontal row is
                    // rejected (would compete with the parent row band).
                    compileNodeInFixedSlot(
                            childPrepared,
                            path,
                            index,
                            depth + 1,
                            cursorX,
                            rowInnerY,
                            slotWidth,
                            FixedSlotKind.ROW_SLOT,
                            slotCtx);
                    cursorX += slotWidth + layoutSpec.spacing();
                    continue;
                }

                String childPath = pathFor(child, path, index);
                String childSemanticName = semanticName(child);
                double childTopY = rowInnerY - childMargin.top();
                double childPlacementY = childTopY - childMeasure.height();
                Padding childPadding = toPadding(child.padding());

                FragmentPlacement childPlacement = new FragmentPlacement(
                        childPath,
                        path,
                        index,
                        depth + 1,
                        state.pageIndex,
                        childRegionX,
                        childPlacementY,
                        childMeasure.width(),
                        childMeasure.height(),
                        state.pageIndex,
                        state.pageIndex,
                        childMargin,
                        childPadding);
                addPlacedFragments(childDefinition.emitFragments(childPrepared, fragmentContext, childPlacement),
                        childPlacement, fragments);

                nodes.add(new PlacedNode(
                        childPath,
                        childSemanticName,
                        child.nodeKind(),
                        path,
                        index,
                        depth + 1,
                        depth + 1,
                        childRegionX,
                        childPlacementY,
                        childRegionX,
                        childPlacementY,
                        childMeasure.width(),
                        childMeasure.height(),
                        state.pageIndex,
                        state.pageIndex,
                        childMeasure.width(),
                        childMeasure.height(),
                        childMargin,
                        childPadding));

                cursorX += slotWidth + layoutSpec.spacing();
            }
        }

        int endPage = state.pageIndex;
        double endPageBottomY = placementTopY - naturalMeasure.height() + margin.bottom();
        List<PlacedFragment> decorationFragments = compositeDecorationFragments(
                prepared,
                definition,
                path,
                parentPath,
                childIndex,
                depth,
                placementX,
                placementTopY,
                endPageBottomY,
                naturalMeasure.width(),
                startPage,
                endPage,
                margin,
                padding,
                state.canvas,
                fragmentContext);
        if (!decorationFragments.isEmpty()) {
            fragments.addAll(decorationInsertIndex, decorationFragments);
        }

        nodes.set(rowNodeIndex, new PlacedNode(
                path,
                semanticName,
                node.nodeKind(),
                parentPath,
                childIndex,
                depth,
                depth,
                placementX,
                placementY,
                placementX,
                placementY,
                naturalMeasure.width(),
                naturalMeasure.height(),
                startPage,
                endPage,
                naturalMeasure.width(),
                naturalMeasure.height(),
                margin,
                padding));

        state.usedHeight += rowOuterHeight;
    }

    private void compileStackedLayer(PreparedNode<DocumentNode> prepared,
                                     NodeDefinition<DocumentNode> definition,
                                     String path,
                                     String semanticName,
                                     String parentPath,
                                     int childIndex,
                                     int depth,
                                     double regionX,
                                     CompilerState state,
                                     PrepareContext prepareContext,
                                     FragmentContext fragmentContext,
                                     List<PlacedNode> nodes,
                                     List<PlacedFragment> fragments,
                                     Margin margin,
                                     Padding padding,
                                     double availableWidth,
                                     MeasureResult naturalMeasure) {
        DocumentNode node = prepared.node();
        double stackOuterHeight = naturalMeasure.height() + margin.vertical();
        double fullPageHeight = state.canvas.innerHeight();
        if (stackOuterHeight > fullPageHeight + CAPACITY_TOLERANCE) {
            throw atomicTooLarge(path, stackOuterHeight, fullPageHeight);
        }
        if (stackOuterHeight > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();

        int startPage = state.pageIndex;
        double placementX = regionX + margin.left();
        double placementTopY = state.pageTop() - state.usedHeight - margin.top();
        double placementY = placementTopY - naturalMeasure.height();
        int decorationInsertIndex = fragments.size();
        int stackNodeIndex = nodes.size();
        nodes.add(null);

        PreparedStackLayout stackLayout =
                prepared.requirePreparedLayout(PreparedStackLayout.class);
        List<DocumentNode> children = definition.children(node);
        double innerWidth = Math.max(0.0, naturalMeasure.width() - padding.horizontal());
        double innerHeight = Math.max(0.0, naturalMeasure.height() - padding.vertical());
        double innerStartX = placementX + padding.left();
        double innerTopY = placementTopY - padding.top();

        // Sort layers by ascending zIndex (stable — equal zIndex keeps
        // source order). Iteration order then determines render order:
        // earlier in the list → drawn first / behind. childIndex below
        // is the SOURCE index so semantic paths stay stable for tests
        // and snapshots; only the iteration order shifts.
        int[] iterationOrder = stableZIndexOrder(stackLayout.zIndices());
        PlacementContext layerHostCtx = new FixedSlotPlacementContext(
                state.pageIndex, state.canvas, prepareContext, fragmentContext, nodes, fragments);
        for (int slot = 0; slot < iterationOrder.length; slot++) {
            int index = iterationOrder[slot];
            placeStackLayer(
                    children.get(index),
                    index,
                    path,
                    depth,
                    innerStartX,
                    innerTopY,
                    innerWidth,
                    innerHeight,
                    stackLayout.alignments().get(index),
                    stackLayout.offsetsX().get(index),
                    stackLayout.offsetsY().get(index),
                    layerHostCtx);
        }

        int endPage = state.pageIndex;
        double endPageBottomY = placementTopY - naturalMeasure.height() + margin.bottom();
        List<PlacedFragment> decorationFragments = compositeDecorationFragments(
                prepared,
                definition,
                path,
                parentPath,
                childIndex,
                depth,
                placementX,
                placementTopY,
                endPageBottomY,
                naturalMeasure.width(),
                startPage,
                endPage,
                margin,
                padding,
                state.canvas,
                fragmentContext);
        if (!decorationFragments.isEmpty()) {
            fragments.addAll(decorationInsertIndex, decorationFragments);
        }

        // Overlay fragments arrive AFTER children — they are the
        // "after the body" half of paired begin/end markers (e.g. the
        // graphics-state restore of a ShapeContainerNode clip path).
        List<PlacedFragment> overlayFragments = compositeOverlayFragments(
                prepared,
                definition,
                path,
                parentPath,
                childIndex,
                depth,
                placementX,
                placementTopY,
                endPageBottomY,
                naturalMeasure.width(),
                startPage,
                endPage,
                margin,
                padding,
                state.canvas,
                fragmentContext);
        if (!overlayFragments.isEmpty()) {
            fragments.addAll(overlayFragments);
        }

        nodes.set(stackNodeIndex, new PlacedNode(
                path,
                semanticName,
                node.nodeKind(),
                parentPath,
                childIndex,
                depth,
                depth,
                placementX,
                placementY,
                placementX,
                placementY,
                naturalMeasure.width(),
                naturalMeasure.height(),
                startPage,
                endPage,
                naturalMeasure.width(),
                naturalMeasure.height(),
                margin,
                padding));

        state.usedHeight += stackOuterHeight;
    }

    /**
     * Returns an iteration order over {@code zIndices} that is stable on
     * ties. Layers with equal {@code zIndex} keep their source order, so
     * the default of all-zero zIndices yields the identity permutation
     * {@code [0, 1, ..., n-1]} and existing snapshots stay deterministic.
     *
     * @param zIndices per-layer render-order keys (in source order)
     * @return source indices sorted by ascending {@code zIndex}, stable
     *         on ties
     */
    private static int[] stableZIndexOrder(java.util.List<Integer> zIndices) {
        int n = zIndices.size();
        if (n <= 1) {
            return identityOrder(n);
        }
        // Common case: every layer uses the same zIndex (typically 0). A
        // stable sort would preserve source order anyway, so skip the boxed
        // array allocation and the full sort.
        int firstZ = zIndices.get(0);
        boolean allEqual = true;
        for (int i = 1; i < n; i++) {
            if (zIndices.get(i) != firstZ) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            return identityOrder(n);
        }
        Integer[] boxed = new Integer[n];
        for (int i = 0; i < n; i++) {
            boxed[i] = i;
        }
        // Comparator.comparingInt + java.util.Arrays.sort on boxed array is
        // documented stable; primitive int[] sort is not.
        java.util.Arrays.sort(boxed, java.util.Comparator.comparingInt(zIndices::get));
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = boxed[i];
        }
        return order;
    }

    private static int[] identityOrder(int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        return order;
    }

    private static double horizontalLayerOffset(com.demcha.compose.document.node.LayerAlign align,
                                                double innerWidth,
                                                double childOuterWidth) {
        return switch (align) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0.0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> Math.max(0.0, (innerWidth - childOuterWidth) / 2.0);
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> Math.max(0.0, innerWidth - childOuterWidth);
        };
    }

    private static double verticalLayerOffset(com.demcha.compose.document.node.LayerAlign align,
                                              double innerHeight,
                                              double childOuterHeight) {
        return switch (align) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0.0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> Math.max(0.0, (innerHeight - childOuterHeight) / 2.0);
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.max(0.0, innerHeight - childOuterHeight);
        };
    }

    private static double[] distributeRowSlotWidths(List<DocumentNode> children,
                                                    List<Double> weights,
                                                    double gap,
                                                    double innerWidth) {
        int n = children.size();
        double available = Math.max(0.0, innerWidth - gap * Math.max(0, n - 1));
        double[] slots = new double[n];
        if (weights == null || weights.isEmpty()) {
            double share = n > 0 ? available / n : 0.0;
            for (int i = 0; i < n; i++) {
                slots[i] = share;
            }
            return slots;
        }
        RowSlots.validateWeightsMatchChildren(weights, n);
        double total = 0.0;
        for (double w : weights) {
            total += w;
        }
        if (total <= 0.0) {
            double share = n > 0 ? available / n : 0.0;
            for (int i = 0; i < n; i++) {
                slots[i] = share;
            }
            return slots;
        }
        for (int i = 0; i < n; i++) {
            slots[i] = available * (weights.get(i) / total);
        }
        return slots;
    }

    private void compileAtomicLeaf(PreparedNode<DocumentNode> prepared,
                                   NodeDefinition<DocumentNode> definition,
                                   String path,
                                   String semanticName,
                                   String parentPath,
                                   int childIndex,
                                   int depth,
                                   double regionX,
                                   double availableWidth,
                                   CompilerState state,
                                   PrepareContext prepareContext,
                                   FragmentContext fragmentContext,
                                   List<PlacedNode> nodes,
                                   List<PlacedFragment> fragments) {
        DocumentNode node = prepared.node();
        Margin margin = toMargin(node.margin());
        Padding padding = toPadding(node.padding());
        MeasureResult naturalMeasure = prepared.measureResult();
        double outerHeight = naturalMeasure.height() + margin.vertical();
        double fullPageHeight = state.canvas.innerHeight();

        if (outerHeight > fullPageHeight + CAPACITY_TOLERANCE) {
            throw atomicTooLarge(path, outerHeight, fullPageHeight);
        }
        if (outerHeight > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();

        double placementX = regionX + margin.left();
        double placementY = state.pageTop() - state.usedHeight - margin.top() - naturalMeasure.height();

        PlacementContext leafCtx = new FixedSlotPlacementContext(
                state.pageIndex, state.canvas, prepareContext, fragmentContext, nodes, fragments);
        placeAtomicLeafFragments(
                prepared, definition, path, semanticName, parentPath, childIndex, depth,
                placementX, placementY, margin, padding, naturalMeasure, leafCtx);
        state.usedHeight += outerHeight;
    }

    /**
     * Builds the fragment placement, emits the fragments, and records the
     * placed-node entry for an atomic leaf. Shared between
     * {@link #compileAtomicLeaf} (mutating placement path that updates
     * {@code CompilerState}) and the atomic branch of
     * {@link #compileNodeInFixedSlot} (non-mutating placement inside a
     * row slot). Pure plumbing &mdash; no pagination state is touched
     * here; callers own page/usedHeight bookkeeping.
     */
    private void placeAtomicLeafFragments(PreparedNode<DocumentNode> prepared,
                                          NodeDefinition<DocumentNode> definition,
                                          String path,
                                          String semanticName,
                                          String parentPath,
                                          int childIndex,
                                          int depth,
                                          double placementX,
                                          double placementY,
                                          Margin margin,
                                          Padding padding,
                                          MeasureResult naturalMeasure,
                                          PlacementContext ctx) {
        DocumentNode node = prepared.node();
        int pageIndex = ctx.pageIndex();
        FragmentPlacement placement = new FragmentPlacement(
                path,
                parentPath,
                childIndex,
                depth,
                pageIndex,
                placementX,
                placementY,
                naturalMeasure.width(),
                naturalMeasure.height(),
                pageIndex,
                pageIndex,
                margin,
                padding);
        addPlacedFragments(definition.emitFragments(prepared, ctx.fragmentContext(), placement), placement, ctx.fragments());
        ctx.nodes().add(new PlacedNode(
                path,
                semanticName,
                node.nodeKind(),
                parentPath,
                childIndex,
                depth,
                depth,
                placementX,
                placementY,
                placementX,
                placementY,
                naturalMeasure.width(),
                naturalMeasure.height(),
                pageIndex,
                pageIndex,
                naturalMeasure.width(),
                naturalMeasure.height(),
                margin,
                padding));
    }

    /**
     * Places a single layer inside a stack composite at the given inner-box
     * coordinates. Shared between {@link #compileStackedLayer} (mutating
     * placement path) and the STACK branch of {@link #compileNodeInFixedSlot}
     * (non-mutating placement path). Both feed per-layer offsets from
     * {@link PreparedStackLayout}, so a {@code position(node, dx, dy, align)}
     * layer is nudged from its anchor identically whether the stack is placed
     * at the root or nested inside a fixed slot (a row column or outer layer).
     *
     * <p>Layer placement is delegated to {@link #compileNodeInFixedSlot}
     * because each layer occupies a single fixed page slot whose origin
     * has already been resolved by the alignment + offset math here.</p>
     */
    private void placeStackLayer(DocumentNode child,
                                 int sourceIndex,
                                 String parentPath,
                                 int parentDepth,
                                 double innerStartX,
                                 double innerTopY,
                                 double innerWidth,
                                 double innerHeight,
                                 com.demcha.compose.document.node.LayerAlign align,
                                 double layerOffsetX,
                                 double layerOffsetY,
                                 PlacementContext ctx) {
        PreparedNode<DocumentNode> childPrepared =
                prepareForRegionWidth(ctx.prepareContext(), child, innerWidth);
        MeasureResult childMeasure = childPrepared.measureResult();
        Margin childMargin = toMargin(child.margin());
        double childOuterWidth = childMeasure.width() + childMargin.horizontal();
        double childOuterHeight = childMeasure.height() + childMargin.vertical();

        // Anchor placement, then apply on-screen offsets:
        // offsetX > 0 nudges the layer right; offsetY > 0 nudges it down
        // (PDF y grows upward, so "down" subtracts from the top-Y).
        double alignedSlotX = innerStartX
                + horizontalLayerOffset(align, innerWidth, childOuterWidth)
                + layerOffsetX;
        double alignedSlotTopY = innerTopY
                - verticalLayerOffset(align, innerHeight, childOuterHeight)
                - layerOffsetY;

        // Layers always paint into a fixed page band — even when the
        // surrounding flow is mutating, the overlay is pinned to ctx's
        // current page, so we narrow to a fixed-slot context here.
        PlacementContext layerCtx = ctx instanceof FixedSlotPlacementContext fixed
                ? fixed
                : new FixedSlotPlacementContext(
                        ctx.pageIndex(),
                        ctx.canvas(),
                        ctx.prepareContext(),
                        ctx.fragmentContext(),
                        ctx.nodes(),
                        ctx.fragments());

        // Child sits inside a LayerStack layer rectangle — the validator
        // can relax for STACK_LAYER_SLOT because there is no competing
        // horizontal row band, only the layer's own fixed area.
        compileNodeInFixedSlot(
                childPrepared,
                parentPath,
                sourceIndex,
                parentDepth + 1,
                alignedSlotX,
                alignedSlotTopY,
                childOuterWidth,
                FixedSlotKind.STACK_LAYER_SLOT,
                layerCtx);
    }

    private void compileSplittableLeaf(PreparedNode<DocumentNode> prepared,
                                       NodeDefinition<DocumentNode> definition,
                                       String path,
                                       String semanticName,
                                       String parentPath,
                                       int childIndex,
                                       int depth,
                                       double regionX,
                                       double availableWidth,
                                       CompilerState state,
                                       PrepareContext prepareContext,
                                       FragmentContext fragmentContext,
                                       List<PlacedNode> nodes,
                                       List<PlacedFragment> fragments) {
        Margin originalMargin = toMargin(prepared.node().margin());
        Padding originalPadding = toPadding(prepared.node().padding());

        PreparedNode<DocumentNode> current = prepared;
        double firstPlacementX = Double.NaN;
        double firstPlacementY = Double.NaN;
        int startPage = -1;
        int endPage = -1;

        while (current != null) {
            DocumentNode currentNode = current.node();
            Margin currentMargin = toMargin(currentNode.margin());
            Padding currentPadding = toPadding(currentNode.padding());
            MeasureResult pieceMeasure = current.measureResult();
            double pieceOuterHeight = pieceMeasure.height() + currentMargin.vertical();
            double fullPageOuterHeight = state.canvas.innerHeight();

            if (pieceOuterHeight <= state.remainingHeight() + EPS) {
                state.touchPage();
                if (startPage < 0) {
                    startPage = state.pageIndex;
                }
                double placementX = regionX + currentMargin.left();
                double placementY = state.pageTop() - state.usedHeight - currentMargin.top() - pieceMeasure.height();
                FragmentPlacement placement = new FragmentPlacement(
                        path,
                        parentPath,
                        childIndex,
                        depth,
                        state.pageIndex,
                        placementX,
                        placementY,
                        pieceMeasure.width(),
                        pieceMeasure.height(),
                        startPage,
                        state.pageIndex,
                        currentMargin,
                        currentPadding);
                addPlacedFragments(definition.emitFragments(current, fragmentContext, placement), placement, fragments);

                if (Double.isNaN(firstPlacementX)) {
                    firstPlacementX = placementX;
                    firstPlacementY = placementY;
                }
                endPage = state.pageIndex;
                state.usedHeight += pieceOuterHeight;
                current = null;
                continue;
            }

            double remainingBoxHeight = Math.max(0.0, state.remainingHeight() - currentMargin.vertical());
            if (remainingBoxHeight <= EPS && state.usedHeight > EPS) {
                state.newPage();
                continue;
            }

            SplitRequest splitRequest = new SplitRequest(
                    new BoxConstraints(availableWidth, remainingBoxHeight),
                    remainingBoxHeight,
                    Math.max(0.0, fullPageOuterHeight - currentMargin.vertical()),
                    prepareContext);
            PreparedSplitResult<DocumentNode> splitResult = definition.split(current, splitRequest);
            PreparedNode<DocumentNode> head = splitResult.head();
            PreparedNode<DocumentNode> tail = splitResult.tail();

            if (head == null) {
                if (state.usedHeight > EPS) {
                    state.newPage();
                    continue;
                }
                throw atomicTooLarge(path, pieceOuterHeight, fullPageOuterHeight);
            }
            if (tail != null && tail.equals(current)) {
                throw new IllegalStateException("Split did not make progress for node '" + path
                        + "'. The node's NodeDefinition.split() returned the original input as the tail — "
                        + "check the definition for an infinite split loop and ensure each split advances.");
            }

            DocumentNode headNode = head.node();
            Margin headMargin = toMargin(headNode.margin());
            Padding headPadding = toPadding(headNode.padding());
            MeasureResult headMeasure = head.measureResult();
            double headOuterHeight = headMeasure.height() + headMargin.vertical();

            if (headOuterHeight > state.remainingHeight() + EPS) {
                if (state.usedHeight > EPS) {
                    state.newPage();
                    continue;
                }
                throw atomicTooLarge(path, headOuterHeight, fullPageOuterHeight);
            }

            state.touchPage();
            if (startPage < 0) {
                startPage = state.pageIndex;
            }

            double placementX = regionX + headMargin.left();
            double placementY = state.pageTop() - state.usedHeight - headMargin.top() - headMeasure.height();
            FragmentPlacement placement = new FragmentPlacement(
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    state.pageIndex,
                    placementX,
                    placementY,
                    headMeasure.width(),
                    headMeasure.height(),
                    startPage,
                    state.pageIndex,
                    headMargin,
                    headPadding);
            addPlacedFragments(definition.emitFragments(head, fragmentContext, placement), placement, fragments);

            if (Double.isNaN(firstPlacementX)) {
                firstPlacementX = placementX;
                firstPlacementY = placementY;
            }
            endPage = state.pageIndex;
            state.usedHeight += headOuterHeight;

            current = tail;
            if (current != null) {
                state.newPage();
            }
        }

        MeasureResult originalMeasure = prepared.measureResult();
        nodes.add(new PlacedNode(
                path,
                semanticName,
                prepared.node().nodeKind(),
                parentPath,
                childIndex,
                depth,
                depth,
                firstPlacementX,
                firstPlacementY,
                firstPlacementX,
                firstPlacementY,
                originalMeasure.width(),
                originalMeasure.height(),
                startPage,
                endPage,
                originalMeasure.width(),
                originalMeasure.height(),
                originalMargin,
                originalPadding));
    }

    /**
     * Compiles a composite or leaf node inside a fixed slot.
     *
     * <p>The slot is constrained to a single page: composite children are
     * laid out with a local top-down y-cursor. No global pagination state
     * is mutated; the parent's outer height check guarantees the column
     * fits.</p>
     *
     * <p>{@code kind} identifies whether the slot is a horizontal row
     * band ({@link FixedSlotKind#ROW_SLOT}) or a
     * {@link com.demcha.compose.document.node.LayerStackNode} layer
     * rectangle ({@link FixedSlotKind#STACK_LAYER_SLOT}). The validator
     * uses it to relax the "no nested horizontal row" rule for stack
     * layers, where a {@code Row} is a normal column-row inside an
     * already-fixed layer rectangle rather than a competing horizontal
     * band.</p>
     *
     * @return outer height (measured height + vertical margin) consumed by the
     *         node, used by the caller's local y-cursor
     */
    private double compileNodeInFixedSlot(PreparedNode<DocumentNode> prepared,
                                          String parentPath,
                                          int childIndex,
                                          int depth,
                                          double slotX,
                                          double slotTopY,
                                          double slotWidth,
                                          FixedSlotKind kind,
                                          PlacementContext ctx) {
        // Alias locals so the body keeps the same names it had before the
        // PlacementContext refactor; the context is the only authoritative
        // source of pagination state for this branch.
        int pageIndex = ctx.pageIndex();
        PrepareContext prepareContext = ctx.prepareContext();
        FragmentContext fragmentContext = ctx.fragmentContext();
        LayoutCanvas canvas = ctx.canvas();
        List<PlacedNode> nodes = ctx.nodes();
        List<PlacedFragment> fragments = ctx.fragments();

        DocumentNode node = prepared.node();
        @SuppressWarnings("unchecked")
        NodeDefinition<DocumentNode> definition = (NodeDefinition<DocumentNode>) registry.definitionFor(node);
        String path = pathFor(node, parentPath, childIndex);
        String semanticName = semanticName(node);
        Margin margin = toMargin(node.margin());
        Padding padding = toPadding(node.padding());
        double availableWidth = childAvailableWidth(slotWidth, node);
        MeasureResult measure = prepared.measureResult();
        double placementX = slotX + margin.left();
        double placementTopY = slotTopY - margin.top();
        double placementY = placementTopY - measure.height();

        if (prepared.isComposite()) {
            CompositeLayoutSpec layoutSpec = prepared.requireCompositeLayout();
            // Horizontal rows remain forbidden inside a ROW_SLOT (column
            // of a row band) because they would compete with the parent
            // row's horizontal band. Inside a STACK_LAYER_SLOT, however,
            // the surrounding rectangle is already fixed by the layer's
            // alignment — a "horizontal row" there is just a normal
            // column-row inside the layer area, not a competing band.
            // STACK composites (e.g. LayerStackNode) are always allowed
            // because they are atomic and anchor their children inside
            // the existing slot.
            if (layoutSpec.axis() == CompositeLayoutSpec.Axis.HORIZONTAL
                    && kind == FixedSlotKind.ROW_SLOT) {
                throw new IllegalStateException("Row '" + path
                        + "' cannot contain a nested horizontal row. "
                        + "Wrap the inner row in a LayerStack layer (allowed since v1.6.2), "
                        + "or stack horizontal content as sections inside a vertical column.");
            }

            int decorationInsertIndex = fragments.size();
            int nodeIndex = nodes.size();
            nodes.add(null);

            List<DocumentNode> children = definition.children(node);

            if (layoutSpec.axis() == CompositeLayoutSpec.Axis.STACK) {
                PreparedStackLayout stackLayout =
                        prepared.requirePreparedLayout(PreparedStackLayout.class);
                double stackInnerWidth = Math.max(0.0, measure.width() - padding.horizontal());
                double stackInnerHeight = Math.max(0.0, measure.height() - padding.vertical());
                double stackInnerStartX = placementX + padding.left();
                double stackInnerTopY = placementTopY - padding.top();

                // Same z-index iteration order as compileStackedLayer
                // (root-level case). Source-order semantic paths are
                // preserved — only render order shifts.
                int[] iterationOrder = stableZIndexOrder(stackLayout.zIndices());
                for (int slot = 0; slot < iterationOrder.length; slot++) {
                    int i = iterationOrder[slot];
                    placeStackLayer(
                            children.get(i),
                            i,
                            path,
                            depth,
                            stackInnerStartX,
                            stackInnerTopY,
                            stackInnerWidth,
                            stackInnerHeight,
                            stackLayout.alignments().get(i),
                            stackLayout.offsetsX().get(i),
                            stackLayout.offsetsY().get(i),
                            ctx);
                }

                List<PlacedFragment> stackDecorations = compositeDecorationFragments(
                        prepared,
                        definition,
                        path,
                        parentPath,
                        childIndex,
                        depth,
                        placementX,
                        placementTopY,
                        placementY + margin.bottom(),
                        measure.width(),
                        pageIndex,
                        pageIndex,
                        margin,
                        padding,
                        canvas,
                        fragmentContext);
                if (!stackDecorations.isEmpty()) {
                    fragments.addAll(decorationInsertIndex, stackDecorations);
                }

                List<PlacedFragment> stackOverlays = compositeOverlayFragments(
                        prepared,
                        definition,
                        path,
                        parentPath,
                        childIndex,
                        depth,
                        placementX,
                        placementTopY,
                        placementY + margin.bottom(),
                        measure.width(),
                        pageIndex,
                        pageIndex,
                        margin,
                        padding,
                        canvas,
                        fragmentContext);
                if (!stackOverlays.isEmpty()) {
                    fragments.addAll(stackOverlays);
                }

                nodes.set(nodeIndex, new PlacedNode(
                        path,
                        semanticName,
                        node.nodeKind(),
                        parentPath,
                        childIndex,
                        depth,
                        depth,
                        placementX,
                        placementY,
                        placementX,
                        placementY,
                        measure.width(),
                        measure.height(),
                        pageIndex,
                        pageIndex,
                        measure.width(),
                        measure.height(),
                        margin,
                        padding));

                return measure.height() + margin.vertical();
            }

            double childRegionX = placementX + padding.left();
            double childRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());
            double childTopY = placementTopY - padding.top();

            for (int i = 0; i < children.size(); i++) {
                DocumentNode child = children.get(i);
                PreparedNode<DocumentNode> childPrepared =
                        prepareForRegionWidth(prepareContext, child, childRegionWidth);
                // Propagate the parent's slot kind so a STACK layer
                // descendant (column → row → ...) keeps the relaxed
                // validation policy all the way down.
                double consumed = compileNodeInFixedSlot(
                        childPrepared,
                        path,
                        i,
                        depth + 1,
                        childRegionX,
                        childTopY,
                        childRegionWidth,
                        kind,
                        ctx);
                childTopY -= consumed;
                if (i < children.size() - 1) {
                    childTopY -= layoutSpec.spacing();
                }
            }

            double endPageBottomY = placementY + margin.bottom();
            List<PlacedFragment> decorationFragments = compositeDecorationFragments(
                    prepared,
                    definition,
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    placementX,
                    placementTopY,
                    endPageBottomY,
                    measure.width(),
                    pageIndex,
                    pageIndex,
                    margin,
                    padding,
                    canvas,
                    fragmentContext);
            if (!decorationFragments.isEmpty()) {
                fragments.addAll(decorationInsertIndex, decorationFragments);
            }

            nodes.set(nodeIndex, new PlacedNode(
                    path,
                    semanticName,
                    node.nodeKind(),
                    parentPath,
                    childIndex,
                    depth,
                    depth,
                    placementX,
                    placementY,
                    placementX,
                    placementY,
                    measure.width(),
                    measure.height(),
                    pageIndex,
                    pageIndex,
                    measure.width(),
                    measure.height(),
                    margin,
                    padding));
            return measure.height() + margin.vertical();
        }

        placeAtomicLeafFragments(
                prepared, definition, path, semanticName, parentPath, childIndex, depth,
                placementX, placementY, margin, padding, measure, ctx);
        return measure.height() + margin.vertical();
    }

    private List<PlacedFragment> compositeDecorationFragments(PreparedNode<DocumentNode> prepared,
                                                              NodeDefinition<DocumentNode> definition,
                                                              String path,
                                                              String parentPath,
                                                              int childIndex,
                                                              int depth,
                                                              double placementX,
                                                              double startPageTopY,
                                                              double endPageBottomY,
                                                              double placementWidth,
                                                              int startPage,
                                                              int endPage,
                                                              Margin margin,
                                                              Padding padding,
                                                              LayoutCanvas canvas,
                                                              FragmentContext fragmentContext) {
        List<PlacedFragment> placed = new ArrayList<>();
        double pageTopY = canvas.height() - canvas.margin().top();
        double pageBottomY = canvas.margin().bottom();

        for (int pageIndex = startPage; pageIndex <= endPage; pageIndex++) {
            double segmentTopY = pageIndex == startPage ? startPageTopY : pageTopY;
            double segmentBottomY = pageIndex == endPage ? endPageBottomY : pageBottomY;
            segmentTopY = Math.min(segmentTopY, pageTopY);
            segmentBottomY = Math.max(pageBottomY, Math.min(segmentBottomY, segmentTopY));
            double segmentHeight = segmentTopY - segmentBottomY;
            if (segmentHeight <= EPS) {
                continue;
            }

            FragmentPlacement placement = new FragmentPlacement(
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    pageIndex,
                    placementX,
                    segmentBottomY,
                    placementWidth,
                    segmentHeight,
                    startPage,
                    endPage,
                    margin,
                    padding);
            placed.addAll(toPlacedFragments(definition.emitFragments(prepared, fragmentContext, placement), placement));
        }

        return placed;
    }

    private List<PlacedFragment> compositeOverlayFragments(PreparedNode<DocumentNode> prepared,
                                                           NodeDefinition<DocumentNode> definition,
                                                           String path,
                                                           String parentPath,
                                                           int childIndex,
                                                           int depth,
                                                           double placementX,
                                                           double startPageTopY,
                                                           double endPageBottomY,
                                                           double placementWidth,
                                                           int startPage,
                                                           int endPage,
                                                           Margin margin,
                                                           Padding padding,
                                                           LayoutCanvas canvas,
                                                           FragmentContext fragmentContext) {
        List<PlacedFragment> placed = new ArrayList<>();
        double pageTopY = canvas.height() - canvas.margin().top();
        double pageBottomY = canvas.margin().bottom();

        for (int pageIndex = startPage; pageIndex <= endPage; pageIndex++) {
            double segmentTopY = pageIndex == startPage ? startPageTopY : pageTopY;
            double segmentBottomY = pageIndex == endPage ? endPageBottomY : pageBottomY;
            segmentTopY = Math.min(segmentTopY, pageTopY);
            segmentBottomY = Math.max(pageBottomY, Math.min(segmentBottomY, segmentTopY));
            double segmentHeight = segmentTopY - segmentBottomY;
            if (segmentHeight <= EPS) {
                continue;
            }

            FragmentPlacement placement = new FragmentPlacement(
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    pageIndex,
                    placementX,
                    segmentBottomY,
                    placementWidth,
                    segmentHeight,
                    startPage,
                    endPage,
                    margin,
                    padding);
            placed.addAll(toPlacedFragments(definition.emitOverlayFragments(prepared, fragmentContext, placement), placement));
        }

        return placed;
    }

    private PreparedNode<DocumentNode> prepareForRegionWidth(PrepareContext prepareContext,
                                                             DocumentNode node,
                                                             double regionWidth) {
        return prepareContext.prepare(node, BoxConstraints.natural(childAvailableWidth(regionWidth, node)));
    }

    private double childAvailableWidth(double regionWidth, DocumentNode node) {
        Margin margin = toMargin(node.margin());
        return Math.max(0.0, regionWidth - margin.horizontal());
    }

    private void addPlacedFragments(List<LayoutFragment> emitted,
                                    FragmentPlacement placement,
                                    List<PlacedFragment> fragments) {
        fragments.addAll(toPlacedFragments(emitted, placement));
    }

    private List<PlacedFragment> toPlacedFragments(List<LayoutFragment> emitted,
                                                   FragmentPlacement placement) {
        List<PlacedFragment> placed = new ArrayList<>(emitted.size());
        int fragmentIndex = 0;
        for (LayoutFragment fragment : emitted) {
            LayoutFragment normalized = new LayoutFragment(
                    placement.path(),
                    fragmentIndex++,
                    fragment.localX(),
                    fragment.localY(),
                    fragment.width(),
                    fragment.height(),
                    fragment.payload());
            placed.add(PlacedFragment.from(normalized, placement));
        }
        return placed;
    }

    private void advanceSpace(double amount, CompilerState state) {
        if (amount <= EPS) {
            return;
        }
        if (amount > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();
        state.usedHeight = Math.min(state.canvas.innerHeight(), state.usedHeight + amount);
    }

    private String pathFor(DocumentNode node, String parentPath, int childIndex) {
        String base = semanticName(node);
        if (base == null || base.isBlank()) {
            base = node.nodeKind();
        }
        String segment = base.trim()
                .replace('\\', '_')
                .replace('/', '_') + "[" + childIndex + "]";
        return parentPath == null ? segment : parentPath + "/" + segment;
    }

    private String semanticName(DocumentNode node) {
        if (node.name() == null || node.name().isBlank()) {
            return null;
        }
        return node.name().trim();
    }

    private AtomicNodeTooLargeException atomicTooLarge(String path, double outerHeight, double pageHeight) {
        return new AtomicNodeTooLargeException(
                "Node '" + path + "' requires outer height " + outerHeight
                        + " but page capacity is " + pageHeight + ". "
                        + "Reduce the node height, split content into multiple atomic blocks, "
                        + "or increase the page size. Differences under 0.5 pt are tolerated as "
                        + "rounding noise (v1.6.2+).");
    }

}

