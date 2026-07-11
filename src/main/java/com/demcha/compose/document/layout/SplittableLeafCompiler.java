package com.demcha.compose.document.layout;

import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toMargin;
import static com.demcha.compose.document.layout.DocumentNodeAdapters.toPadding;

/**
 * Places a {@link com.demcha.compose.document.node.PaginationPolicy#SPLITTABLE}
 * leaf across pages. The leaf is measured, and while it does not fit in the
 * remaining page height it is split through its {@code NodeDefinition.split};
 * each piece is emitted on the page it lands on, advancing the page cursor as
 * needed, until the tail is exhausted. A single {@link PlacedNode} spanning the
 * first to the last touched page is recorded for the whole leaf.
 *
 * <p>The compiler cursor ({@link CompilerState}) is mutated as pages advance;
 * the caller owns the {@code nodes} / {@code fragments} lists this appends to.
 * Package-private — engine surface, not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class SplittableLeafCompiler {

    private static final double EPS = 1e-6;

    private SplittableLeafCompiler() {
        // Utility class, no instantiation.
    }

    /**
     * Compiles a splittable leaf, paginating it across as many pages as needed.
     *
     * @param prepared        the prepared leaf (measured at its region width)
     * @param definition      the leaf's node definition (supplies split + emit)
     * @param path            the leaf's layout path
     * @param semanticName    the leaf's semantic name
     * @param parentPath      the parent's layout path
     * @param childIndex      the leaf's index among its siblings
     * @param depth           the leaf's tree depth
     * @param regionX         the content region's left edge
     * @param availableWidth  the content width available to the leaf
     * @param ctx             the cursor + emission context (mutated as pages advance)
     * @throws AtomicNodeTooLargeException if a piece cannot fit on an empty page
     */
    static void compile(PreparedNode<DocumentNode> prepared,
                        NodeDefinition<DocumentNode> definition,
                        String path,
                        String semanticName,
                        String parentPath,
                        int childIndex,
                        int depth,
                        double regionX,
                        double availableWidth,
                        CompileContext ctx) {
        CompilerState state = ctx.state();
        PrepareContext prepareContext = ctx.prepareContext();
        FragmentContext fragmentContext = ctx.fragmentContext();
        List<PlacedNode> nodes = ctx.nodes();
        List<PlacedFragment> fragments = ctx.fragments();

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
            double fullPageOuterHeight = state.activeInnerHeight();

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
                fragments.addAll(CompositeDecoration.toPlacedFragments(
                        definition.emitFragments(current, fragmentContext, placement), placement));

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
                throw AtomicNodeTooLargeException.forNode(path, pieceOuterHeight, fullPageOuterHeight);
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
                throw AtomicNodeTooLargeException.forNode(path, headOuterHeight, fullPageOuterHeight);
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
            fragments.addAll(CompositeDecoration.toPlacedFragments(
                    definition.emitFragments(head, fragmentContext, placement), placement));

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
}
