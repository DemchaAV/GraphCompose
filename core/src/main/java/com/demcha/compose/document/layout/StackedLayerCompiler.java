package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

/**
 * Places a {@link com.demcha.compose.document.node.LayerStackNode} layer stack:
 * admits the stack band onto the page, renders each layer into the stack's inner
 * rectangle in stable z-index order, then emits the fill / overlay decoration and
 * records the stack's placed node.
 *
 * <p>The per-layer placement recurses back through the host compiler
 * ({@code host.placeStackLayer(...)} → {@code compileNodeInFixedSlot}), so
 * {@link #compile} takes the {@link LayoutCompiler} as a host argument and calls
 * back into it rather than owning the recursion. Package-private — engine surface,
 * not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class StackedLayerCompiler {

    private StackedLayerCompiler() {
        // Utility class, no instantiation.
    }

    /**
     * Compiles a layer stack onto the current page.
     *
     * @param host          the compiler that owns the per-layer recursion and the
     *                      atomic-block admission guard
     * @param prepared      the prepared stack node (carries its {@link PreparedStackLayout})
     * @param definition    the stack's node definition
     * @param path          the stack's layout path
     * @param semanticName  the stack's semantic name
     * @param parentPath    the parent's layout path
     * @param childIndex    the stack's index among its siblings
     * @param depth         the stack's tree depth
     * @param regionX       the content region's left edge
     * @param margin        the stack's margin
     * @param padding       the stack's padding
     * @param naturalMeasure the stack's measured size
     * @param ctx           the cursor + emission context (mutated as the stack is placed)
     */
    static void compile(LayoutCompiler host,
                        PreparedNode<DocumentNode> prepared,
                        NodeDefinition<DocumentNode> definition,
                        String path,
                        String semanticName,
                        String parentPath,
                        int childIndex,
                        int depth,
                        double regionX,
                        Margin margin,
                        Padding padding,
                        MeasureResult naturalMeasure,
                        CompileContext ctx) {
        CompilerState state = ctx.state();
        PrepareContext prepareContext = ctx.prepareContext();
        FragmentContext fragmentContext = ctx.fragmentContext();
        List<PlacedNode> nodes = ctx.nodes();
        List<PlacedFragment> fragments = ctx.fragments();

        DocumentNode node = prepared.node();
        double stackOuterHeight = naturalMeasure.height() + margin.vertical();
        host.admitAtomicBlock(stackOuterHeight, path, state);

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
        int[] iterationOrder = LayerStackGeometry.zOrder(stackLayout.zIndices());
        PlacementContext layerHostCtx = new FixedSlotPlacementContext(
                state.pageIndex, state.canvas, prepareContext, fragmentContext, nodes, fragments);
        for (int slot = 0; slot < iterationOrder.length; slot++) {
            int index = iterationOrder[slot];
            host.placeStackLayer(
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

        // Overlay fragments arrive AFTER children — they are the
        // "after the body" half of paired begin/end markers (e.g. the
        // graphics-state restore of a ShapeContainerNode clip path).
        List<PlacedFragment> overlayFragments = CompositeDecoration.overlay(
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
}
