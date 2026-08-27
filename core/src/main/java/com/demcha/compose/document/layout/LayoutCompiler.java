package com.demcha.compose.document.layout;

import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
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
     * @param graph           semantic document graph
     * @param prepareContext  node preparation context
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
        CompilerState state = new CompilerState(canvas, prepareContext.pageGeometry());
        List<PlacedNode> nodes = new ArrayList<>();
        List<PlacedFragment> fragments = new ArrayList<>();

        for (int index = 0; index < graph.roots().size(); index++) {
            DocumentNode root = graph.roots().get(index);
            // Each top-level block is measured at the content width of the page it
            // begins on. The start page is carried over from the previous layout
            // pass (the per-page-margin fixed point); on the first pass it falls back
            // to the page the previous block ended on. With no per-page margins the
            // path lookup is skipped and the width is the single canvas width, so the
            // layout — and the work done — is byte-identical.
            int startPage = state.pageIndex;
            if (state.hasPageGeometry()) {
                startPage = prepareContext.assignedStartPage(pathFor(root, null, index), state.pageIndex);
            }
            double regionWidth = state.innerWidthForPage(startPage);
            double regionX = state.marginLeftForPage(startPage);
            compileNode(
                    prepareForRegionWidth(prepareContext, root, regionWidth),
                    null,
                    index,
                    1,
                    regionX,
                    regionWidth,
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

    void compileNode(PreparedNode<DocumentNode> prepared,
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
            SplittableLeafCompiler.compile(prepared, definition, path, semanticName, parentPath, childIndex, depth, regionX,
                    availableWidth, new CompileContext(state, prepareContext, fragmentContext, nodes, fragments));
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

        if (layoutSpec.axis() == CompositeLayoutSpec.Axis.COLUMN_FLOW) {
            // Columns that each flow down their own column across pages. No
            // admitAtomicBlock: unlike a row band, this node is page-spanning by
            // construction, so there is no height it must fit inside.
            ColumnFlowCompiler.compile(this, prepared, definition, path, semanticName, parentPath,
                    childIndex, depth, regionX, state, prepareContext, fragmentContext, nodes,
                    fragments, margin, padding, availableWidth, layoutSpec, naturalMeasure);
            return;
        }

        if (layoutSpec.axis() == CompositeLayoutSpec.Axis.STACK) {
            StackedLayerCompiler.compile(this, prepared, definition, path, semanticName, parentPath, childIndex,
                    depth, regionX, margin, padding, naturalMeasure,
                    new CompileContext(state, prepareContext, fragmentContext, nodes, fragments));
            return;
        }

        // Opt-in keep-together: a node that requests it relocates whole to the
        // next page when it does not fit in the remaining space but would fit on a
        // fresh page — avoiding an orphaned heading above atomic content (e.g. a
        // chart). Nodes taller than a full page must still flow. Default-off, so
        // existing layouts are unchanged.
        double outerHeight = naturalMeasure.height() + margin.vertical();
        boolean keepWhole = node.keepTogether()
                            && outerHeight <= state.activeInnerHeight() + CAPACITY_TOLERANCE;
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

        state.advanceSpace(startReservation);
        List<DocumentNode> children = definition.children(node);
        double childRegionX = placementX + padding.left();
        double childRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());
        // At the root level the page margin defines the document's content column, so
        // each direct child is measured at the content width of the page it begins on
        // (carried over from the previous pass via the fixed point). Nested composites
        // keep their parent-allocated geometry. With no per-page margins both branches
        // resolve to the same width, so the layout is byte-identical.
        boolean pageColumn = depth == 1 && state.hasPageGeometry();

        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            double thisChildRegionX = childRegionX;
            double thisChildRegionWidth = childRegionWidth;
            if (pageColumn) {
                int childStartPage = prepareContext.assignedStartPage(
                        pathFor(child, path, index), state.pageIndex);
                double pageRegionWidth = state.innerWidthForPage(childStartPage);
                thisChildRegionWidth = Math.max(0.0, (pageRegionWidth - margin.horizontal()) - padding.horizontal());
                thisChildRegionX = state.marginLeftForPage(childStartPage) + margin.left() + padding.left();
            }
            PreparedNode<DocumentNode> childPrepared =
                    prepareForRegionWidth(prepareContext, child, thisChildRegionWidth);

            // Opt-in keep-with-next: at the start of a run of consecutive
            // keep-with-next siblings, ensure the whole run plus the first line of the
            // block it introduces shares one page. Hoisting the break to before the
            // run's first member lets a multi-part heading (rule + banner + rule)
            // relocate as a unit, so no part is stranded above its body. Inert when
            // the run ends the flow (nothing to introduce) and best-effort when the
            // run plus one line cannot fit even on a fresh page — matching
            // keepTogether's fallback. Gated on keepWithNext(), so layouts that do not
            // opt in are byte-identical.
            if (child.keepWithNext()
                && (index == 0 || !children.get(index - 1).keepWithNext())
                && state.usedHeight > EPS) {
                int runEnd = index;
                while (runEnd < children.size() && children.get(runEnd).keepWithNext()) {
                    runEnd++;
                }
                if (runEnd < children.size()) {
                    double needed = 0.0;
                    for (int k = index; k < runEnd; k++) {
                        PreparedNode<DocumentNode> memberPrepared = k == index
                                ? childPrepared
                                : prepareForRegionWidth(prepareContext, children.get(k), childRegionWidth);
                        needed += memberPrepared.measureResult().height()
                                  + toMargin(children.get(k).margin()).vertical()
                                  + layoutSpec.spacing();
                    }
                    needed += leadingUnitHeight(children.get(runEnd), childRegionWidth, prepareContext,
                                               state.activeInnerHeight());
                    // Relocate only when the run + first line genuinely fits on a fresh
                    // page (EPS, not CAPACITY_TOLERANCE): a run that would still overflow
                    // the next page by a hair should stay put rather than strand there and
                    // waste this page too.
                    if (needed > state.remainingHeight() + EPS
                        && needed <= state.activeInnerHeight() + EPS) {
                        state.newPage();
                    }
                }
            }

            compileNode(
                    childPrepared,
                    path,
                    index,
                    depth + 1,
                    thisChildRegionX,
                    thisChildRegionWidth,
                    state,
                    prepareContext,
                    fragmentContext,
                    nodes,
                    fragments);
            if (index < children.size() - 1) {
                state.advanceSpace(layoutSpec.spacing());
            }
        }

        state.closeBottomSpace(padding.bottom() + margin.bottom());
        int endPage = state.pageIndex;
        double endPageBottomY = state.pageTop() - state.usedHeight + margin.bottom();

        // Content bleed extends the decoration box to the trimmed page edge on the
        // declared edges while children stay in the content region; without bleed the
        // box is the in-margin geometry (see CompositeDecoration#resolveBleedBox).
        DocumentBleed bleed = node.bleed();
        CompositeDecoration.BleedBox decor = CompositeDecoration.resolveBleedBox(
                bleed, placementX, naturalMeasure.width(), placementTopY, endPageBottomY, state.canvas);
        List<PlacedFragment> decorationFragments = CompositeDecoration.fill(
                prepared,
                definition,
                path,
                parentPath,
                childIndex,
                depth,
                decor.x(),
                decor.topY(),
                decor.bottomY(),
                decor.width(),
                startPage,
                endPage,
                margin,
                padding,
                state.canvas,
                bleed,
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
        admitAtomicBlock(rowOuterHeight, path, state);

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
            RowSlots.SlotLayout slotLayout = RowSlots.resolveLayout(
                    node, children, layoutSpec, childRegionWidth, prepareContext, semanticName);
            double[] slotWidths = slotLayout.widths();
            double flexLeading = slotLayout.leading();
            double flexExtraGap = slotLayout.extraGap();
            RowVerticalAlign verticalAlign = node instanceof RowNode rowNode
                    ? rowNode.verticalAlign() : RowVerticalAlign.TOP;
            double bandContentHeight = naturalMeasure.height() - padding.vertical();
            double cursorX = placementX + padding.left() + flexLeading;

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

                // Cross-axis seating within the row band. TOP yields offset 0.0,
                // so the band-top expression below is byte-identical to the
                // pre-verticalAlign placement; only CENTER/BOTTOM shift the child.
                // The slack is never negative: the measure phase sets the band to
                // the tallest child's margin-box, so bandContentHeight >=
                // childMargin.vertical() + childMeasure.height() for every child.
                double verticalOffset = verticalAlign == RowVerticalAlign.TOP
                        ? 0.0
                        : (bandContentHeight - childMargin.vertical() - childMeasure.height())
                          * (verticalAlign == RowVerticalAlign.CENTER ? 0.5 : 1.0);

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
                            rowInnerY - verticalOffset,
                            slotWidth,
                            FixedSlotKind.ROW_SLOT,
                            slotCtx);
                    cursorX += slotWidth + layoutSpec.spacing()
                               + (index < children.size() - 1 ? flexExtraGap : 0.0);
                    continue;
                }

                String childPath = pathFor(child, path, index);
                String childSemanticName = semanticName(child);
                double childTopY = rowInnerY - childMargin.top() - verticalOffset;
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

                cursorX += slotWidth + layoutSpec.spacing()
                           + (index < children.size() - 1 ? flexExtraGap : 0.0);
            }
        }

        int endPage = state.pageIndex;
        double endPageBottomY = placementTopY - naturalMeasure.height() + margin.bottom();
        List<PlacedFragment> decorationFragments = CompositeDecoration.fill(
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
                DocumentBleed.none(),
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
        admitAtomicBlock(outerHeight, path, state);

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
     * coordinates. Shared between {@link StackedLayerCompiler} (mutating
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
    void placeStackLayer(DocumentNode child,
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
                              + LayerStackGeometry.horizontalOffset(align, innerWidth, childOuterWidth)
                              + layerOffsetX;
        double alignedSlotTopY = innerTopY
                                 - LayerStackGeometry.verticalOffset(align, innerHeight, childOuterHeight)
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

    /**
     * Lays out a prepared sub-tree inside a fixed rectangle and returns the
     * fragments it produced, without touching the surrounding document's
     * placement state.
     *
     * <p>Used by primitives that own a child sub-tree inside their own
     * geometry — a {@code TableNode} cell built with
     * {@link com.demcha.compose.document.table.DocumentTableCell#node} — and
     * therefore have to place that sub-tree themselves during fragment
     * emission. Dispatching to the child's own
     * {@link NodeDefinition#emitFragments} is not enough for a composite
     * child: a composite emits only its decoration and leaves its children
     * to the compiler, so the sub-tree would be measured, reserved, and then
     * silently dropped. Routing through {@link #compileNodeInFixedSlot} gives
     * such a child the same column / row / stack layout it gets anywhere else
     * in the document.</p>
     *
     * <p>The placed nodes the walk produces are discarded: the box already
     * exists inside its owner's geometry, so re-publishing its interior into
     * the document's semantic node list would double-count it.</p>
     *
     * @param prepared        prepared sub-tree root
     * @param parentPath      path of the node that owns the box
     * @param childIndex      index of the box within its owner
     * @param depth           depth of the box within its owner
     * @param slotX           left edge of the box, in absolute page coordinates
     * @param slotTopY        top edge of the box, in absolute page coordinates
     * @param slotWidth       width of the box
     * @param pageIndex       page the box lands on
     * @param canvas          active layout canvas
     * @param prepareContext  prepare context used to (re)measure descendants
     * @param fragmentContext fragment context forwarded to descendant definitions
     * @return fragments placed inside the box, in absolute page coordinates
     */
    List<PlacedFragment> compileFixedBoxSubtree(PreparedNode<DocumentNode> prepared,
                                                String parentPath,
                                                int childIndex,
                                                int depth,
                                                double slotX,
                                                double slotTopY,
                                                double slotWidth,
                                                int pageIndex,
                                                LayoutCanvas canvas,
                                                PrepareContext prepareContext,
                                                FragmentContext fragmentContext) {
        List<PlacedNode> discardedNodes = new ArrayList<>();
        List<PlacedFragment> fragments = new ArrayList<>();
        PlacementContext ctx = new FixedSlotPlacementContext(
                pageIndex, canvas, prepareContext, fragmentContext, discardedNodes, fragments);
        compileNodeInFixedSlot(
                prepared,
                parentPath,
                childIndex,
                depth,
                slotX,
                slotTopY,
                slotWidth,
                FixedSlotKind.FIXED_BOX_SLOT,
                ctx);
        return List.copyOf(fragments);
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
     * node, used by the caller's local y-cursor
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
            // A column flow advances pages; a fixed slot pins one. Placed here
            // it would quietly stack its columns down the slot and run past the
            // band, overlapping whatever follows — so it is rejected in both
            // slot kinds rather than rendered wrong.
            if (layoutSpec.axis() == CompositeLayoutSpec.Axis.COLUMN_FLOW) {
                throw new IllegalStateException("Node '" + path
                        + "' cannot contain a column flow: a "
                        + (kind == FixedSlotKind.ROW_SLOT ? "row slot" : "stack layer")
                        + " is pinned to one page, and a column flow spans pages. "
                        + "Put the column flow on the page flow, or use a row for "
                        + "side-by-side content that fits one page.");
            }

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

                // Same z-index iteration order as StackedLayerCompiler
                // (root-level case). Source-order semantic paths are
                // preserved — only render order shifts.
                int[] iterationOrder = LayerStackGeometry.zOrder(stackLayout.zIndices());
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

                List<PlacedFragment> stackDecorations = CompositeDecoration.fill(
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
                        DocumentBleed.none(),
                        fragmentContext);
                if (!stackDecorations.isEmpty()) {
                    fragments.addAll(decorationInsertIndex, stackDecorations);
                }

                List<PlacedFragment> stackOverlays = CompositeDecoration.overlay(
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

            if (layoutSpec.axis() == CompositeLayoutSpec.Axis.HORIZONTAL) {
                placeRowBandInFixedSlot(
                        node,
                        children,
                        layoutSpec,
                        semanticName,
                        path,
                        depth,
                        childRegionX,
                        childTopY,
                        childRegionWidth,
                        measure.height() - padding.vertical(),
                        ctx);
            } else {
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
            }

            double endPageBottomY = placementY + margin.bottom();
            List<PlacedFragment> decorationFragments = CompositeDecoration.fill(
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
                    DocumentBleed.none(),
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

    /**
     * Seats a horizontal composite's children side by side inside a fixed slot.
     *
     * <p>The fixed-slot walk is otherwise a vertical y-cursor, which is the
     * right model for a section or a container but the wrong one for a row: a
     * row's children share one band and split its width. Without this branch a
     * row nested in a fixed rectangle — a
     * {@link com.demcha.compose.document.node.LayerStackNode} layer, or a
     * {@code TableNode} cell built with
     * {@link com.demcha.compose.document.table.DocumentTableCell#node} — stacks
     * its children downwards and overflows the rectangle by the height of every
     * child after the first, because the band was measured as one row tall.</p>
     *
     * <p>Slot widths come from the same {@link RowSlots#resolveLayout} the
     * page-flow row band uses, so a fixed-slot row honours weights, fixed
     * columns, flex arrangement and vertical alignment identically. Children are
     * compiled as {@link FixedSlotKind#ROW_SLOT} so a nested horizontal row is
     * rejected here exactly as it is at page level.</p>
     *
     * @param node              the horizontal composite being seated
     * @param children          its children, in source order
     * @param layoutSpec        the composite's resolved layout spec
     * @param semanticName      name used in the slot-resolution diagnostics
     * @param path              layout path of the composite
     * @param depth             depth of the composite; children sit one below
     * @param bandStartX        left edge of the band's content area
     * @param bandTopY          top edge of the band's content area
     * @param bandWidth         width available to the children
     * @param bandContentHeight height of the band, used to seat non-TOP children
     * @param ctx               placement context the children append to
     */
    private void placeRowBandInFixedSlot(DocumentNode node,
                                         List<DocumentNode> children,
                                         CompositeLayoutSpec layoutSpec,
                                         String semanticName,
                                         String path,
                                         int depth,
                                         double bandStartX,
                                         double bandTopY,
                                         double bandWidth,
                                         double bandContentHeight,
                                         PlacementContext ctx) {
        if (children.isEmpty()) {
            return;
        }
        PrepareContext prepareContext = ctx.prepareContext();
        RowSlots.SlotLayout slotLayout = RowSlots.resolveLayout(
                node, children, layoutSpec, bandWidth, prepareContext, semanticName);
        double[] slotWidths = slotLayout.widths();
        RowVerticalAlign verticalAlign = node instanceof RowNode rowNode
                ? rowNode.verticalAlign() : RowVerticalAlign.TOP;
        double cursorX = bandStartX + slotLayout.leading();

        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            Margin childMargin = toMargin(child.margin());
            double slotWidth = slotWidths[index];
            double childInnerWidth = Math.max(0.0, slotWidth - childMargin.horizontal());
            PreparedNode<DocumentNode> childPrepared =
                    prepareForRegionWidth(prepareContext, child, childInnerWidth);

            // Cross-axis seating, identical to the page-level row band: TOP
            // yields offset 0.0, so a TOP row places exactly where the plain
            // vertical walk used to put its first child.
            double verticalOffset = verticalAlign == RowVerticalAlign.TOP
                    ? 0.0
                    : (bandContentHeight - childMargin.vertical() - childPrepared.measureResult().height())
                      * (verticalAlign == RowVerticalAlign.CENTER ? 0.5 : 1.0);

            compileNodeInFixedSlot(
                    childPrepared,
                    path,
                    index,
                    depth + 1,
                    cursorX,
                    bandTopY - verticalOffset,
                    slotWidth,
                    FixedSlotKind.ROW_SLOT,
                    ctx);

            cursorX += slotWidth + layoutSpec.spacing()
                       + (index < children.size() - 1 ? slotLayout.extraGap() : 0.0);
        }
    }

    PreparedNode<DocumentNode> prepareForRegionWidth(PrepareContext prepareContext,
                                                             DocumentNode node,
                                                             double regionWidth) {
        return prepareContext.prepare(node, BoxConstraints.natural(childAvailableWidth(regionWidth, node)));
    }

    private double childAvailableWidth(double regionWidth, DocumentNode node) {
        Margin margin = toMargin(node.margin());
        return Math.max(0.0, regionWidth - margin.horizontal());
    }

    /**
     * Best-effort height a node consumes to place its first flow line: the top
     * reservation (margin-top + padding-top) plus the leading unit of the content
     * below it &mdash; the first child's leading unit for a vertical composite, the
     * first visual line for a paragraph, or the whole outer height for any other
     * leaf or an atomic (row / stack) composite that cannot split.
     *
     * <p>Used only by the opt-in {@link DocumentNode#keepWithNext()} lookahead to
     * decide whether a heading would strand apart from the block it introduces. A
     * small over- or under-estimate only shifts a cosmetic page break by one line;
     * it can never affect placement correctness, since the value gates a
     * {@code newPage()} decision, not any geometry.</p>
     *
     * @param node           the following sibling whose first line anchors the heading
     * @param regionWidth    the content-region width the sibling is measured at
     * @param prepareContext measurement context
     * @param pageInnerHeight the active page's inner height, used to tell a honoured
     *                        keep-together request from one the compiler will ignore
     * @return the height consumed down to the bottom of the node's first flow line
     */
    private double leadingUnitHeight(DocumentNode node, double regionWidth, PrepareContext prepareContext,
                                     double pageInnerHeight) {
        Margin margin = toMargin(node.margin());
        Padding padding = toPadding(node.padding());
        PreparedNode<DocumentNode> prepared = prepareForRegionWidth(prepareContext, node, regionWidth);
        double topReservation = margin.top() + padding.top();

        // A node that asked to be kept whole has no first line to seam after: when
        // the request will actually be honoured, its whole outer height IS its
        // leading unit, because it relocates entire rather than splitting. Mirrors
        // the keepWhole test in compileNode — a node taller than the page has its
        // keep-together ignored and flows, so the first-slice estimate stays right
        // there. Without this a heading above a boxed callout is told "one line
        // fits", stays put, and is stranded when the whole box moves.
        double outerHeight = prepared.measureResult().height() + margin.vertical();
        if (node.keepTogether() && outerHeight <= pageInnerHeight + CAPACITY_TOLERANCE) {
            return outerHeight;
        }

        if (prepared.isComposite()) {
            CompositeLayoutSpec layoutSpec = prepared.requireCompositeLayout();
            @SuppressWarnings("unchecked")
            NodeDefinition<DocumentNode> definition = (NodeDefinition<DocumentNode>) registry.definitionFor(node);
            List<DocumentNode> children = definition.children(node);
            // A horizontal row or layer stack is atomic (never splits) and an empty
            // vertical box has no first line — the whole box is the leading unit.
            // A column flow is neither: it spans pages by construction, so its
            // measured height is not what a preceding keep-with-next run has to
            // fit beside. Its leading unit is its first column's, exactly as a
            // vertical box's is its first child's — otherwise a heading above a
            // multi-page flow hoists itself to the next page and wastes this one.
            boolean columnFlow = layoutSpec.axis() == CompositeLayoutSpec.Axis.COLUMN_FLOW;
            if ((layoutSpec.axis() != CompositeLayoutSpec.Axis.VERTICAL && !columnFlow)
                || children.isEmpty()) {
                return prepared.measureResult().height() + margin.vertical();
            }
            double availableWidth = childAvailableWidth(regionWidth, node);
            double innerRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());
            if (columnFlow) {
                // The first column occupies its slot, not the whole flow: asking a
                // paragraph how tall its first line is at four times its real width
                // would answer for a line it never gets.
                innerRegionWidth = RowSlots.distributeRowSlotWidths(
                        children, layoutSpec.weights(), layoutSpec.spacing(), innerRegionWidth)[0];
            }
            return topReservation + leadingUnitHeight(children.get(0), innerRegionWidth, prepareContext,
                                                      pageInnerHeight);
        }

        // The leading unit of a splittable leaf is its first slice: a paragraph's
        // first visual line, a table's repeated header rows plus first body row, a
        // list's first item. An indivisible (atomic) leaf has no smaller unit, so
        // the default seam returns the whole content height and the block is kept
        // whole. Delegating keeps each node type's split granularity in its own
        // definition instead of special-casing types here.
        @SuppressWarnings("unchecked")
        NodeDefinition<DocumentNode> definition = (NodeDefinition<DocumentNode>) registry.definitionFor(node);
        return topReservation + definition.firstSliceHeight(prepared);
    }

    private void addPlacedFragments(List<LayoutFragment> emitted,
                                    FragmentPlacement placement,
                                    List<PlacedFragment> fragments) {
        fragments.addAll(CompositeDecoration.toPlacedFragments(emitted, placement));
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

    /**
     * Admits a non-splitting block — a horizontal row band, a layer stack, or an
     * atomic leaf — onto the page: rejects a block taller than a full page, moves
     * to a fresh page when it does not fit in the remaining height, and marks the
     * landing page touched.
     *
     * @param outerHeight the block's outer (margin-box) height
     * @param path        the block's layout path, for the too-large error message
     * @param state       the compiler cursor (mutated: may advance to a new page)
     * @throws AtomicNodeTooLargeException if the block cannot fit on a full page
     */
    void admitAtomicBlock(double outerHeight, String path, CompilerState state) {
        double fullPageHeight = state.activeInnerHeight();
        if (outerHeight > fullPageHeight + CAPACITY_TOLERANCE) {
            throw AtomicNodeTooLargeException.forNode(path, outerHeight, fullPageHeight);
        }
        if (outerHeight > state.remainingHeight() + EPS && state.usedHeight > EPS) {
            state.newPage();
        }
        state.touchPage();
    }

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
        /**
         * Child sits inside a horizontal row band (column of a row).
         */
        ROW_SLOT,
        /**
         * Child sits inside a {@link LayerStackNode} layer rectangle.
         */
        STACK_LAYER_SLOT,
        /**
         * Child sits inside a rectangle a primitive owns within its own
         * geometry — a composed {@code TableNode} cell. Like a stack layer,
         * the surrounding rectangle is already fixed, so a horizontal row
         * inside it is a normal column-row rather than a band competing with
         * a parent row.
         */
        FIXED_BOX_SLOT
    }

}

