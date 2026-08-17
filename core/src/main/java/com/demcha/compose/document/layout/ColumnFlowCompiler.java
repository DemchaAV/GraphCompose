package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

/**
 * Places a {@code ColumnFlowNode}: each column flows down its own column and
 * continues on the next page, and the flow ends on the last page any column
 * reached.
 *
 * <p>There is no split contract here, and there does not need to be. A
 * vertical composite already spans pages — not by splitting itself, but
 * because {@code LayoutCompiler} places its children one at a time against a
 * shared page cursor, and any child may advance that cursor to a new page. A
 * column is exactly such a flow. What a column flow adds is that its columns
 * do not share one cursor: each gets its own, forked from the flow's entry
 * position, and the flow rejoins them at the end.</p>
 *
 * <pre>
 *   fork:  every column starts at (entryPage, entryUsed)
 *   flow:  column i is compiled through the ordinary vertical path, so
 *          everything inside it — paragraphs, tables, nested sections —
 *          breaks and continues exactly as it does anywhere else
 *   join:  page   = max over columns of the page each ended on
 *          used   = max over the columns that ended on that page
 * </pre>
 *
 * <p>The join takes the height only from columns that ended on the joined
 * page: a shorter column that finished two pages earlier says nothing about
 * how much of the last page is occupied, and taking its cursor would let the
 * next sibling overwrite the longer column.</p>
 *
 * <p>Column widths are resolved once, at entry, from the flow's weights.
 * Re-resolving them per page would let a column change width halfway down the
 * document, and would also break the layout fixed point, which requires the
 * geometry to be a pure function of the entry state. A document with per-page
 * margins therefore keeps the entry page's column widths on the pages the flow
 * continues onto — the same rule every nested composite already follows, since
 * only a page-level column re-reads the region per page.</p>
 *
 * <p>Package-private — engine surface, not public API. Mirrors
 * {@link StackedLayerCompiler} in taking the host compiler and calling back
 * into it rather than owning the recursion.</p>
 *
 * @author Artem Demchyshyn
 */
final class ColumnFlowCompiler {

    private ColumnFlowCompiler() {
        // Utility class, no instantiation.
    }

    /**
     * Compiles a column flow starting at the current cursor.
     *
     * @param host           the compiler that owns the per-column recursion
     * @param prepared       the prepared flow node
     * @param definition     the flow's node definition
     * @param path           the flow's layout path
     * @param semanticName   the flow's semantic name
     * @param parentPath     the parent's layout path
     * @param childIndex     the flow's index among its siblings
     * @param depth          the flow's tree depth
     * @param regionX        the content region's left edge
     * @param state          the page cursor, mutated to the joined position
     * @param prepareContext preparation context for per-column measurement
     * @param fragmentContext fragment emission context
     * @param nodes          placed-node sink
     * @param fragments      placed-fragment sink
     * @param margin         the flow's margin
     * @param padding        the flow's padding
     * @param availableWidth the width available to the flow's content
     * @param layoutSpec     the flow's child-layout contract (gap + weights)
     * @param naturalMeasure the flow's measured size
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
        List<DocumentNode> children = definition.children(node);

        // The flow opens like any composite: reserve its top edge, which may
        // itself spill to the next page. Every column then starts from there.
        double startReservation = margin.top() + padding.top();
        if (startReservation > state.remainingHeight() + NodeDefinitionSupport.EPS
            && state.usedHeight > NodeDefinitionSupport.EPS) {
            state.newPage();
        }
        state.touchPage();

        int startPage = state.pageIndex;
        double placementX = regionX + margin.left();
        double placementTopY = state.pageTop() - state.usedHeight - margin.top();
        double placementY = placementTopY - naturalMeasure.height();
        int nodeIndex = nodes.size();
        nodes.add(null);

        state.advanceSpace(startReservation);

        int endPage = state.pageIndex;
        double endUsed = state.usedHeight;
        int maxTouched = state.maxTouchedPage;

        if (!children.isEmpty()) {
            double childRegionWidth = Math.max(0.0, availableWidth - padding.horizontal());
            RowSlots.SlotLayout slotLayout = RowSlots.resolveLayout(
                    node, children, layoutSpec, childRegionWidth, prepareContext, semanticName);
            double[] slotWidths = slotLayout.widths();
            double cursorX = placementX + padding.left();

            for (int index = 0; index < children.size(); index++) {
                DocumentNode child = children.get(index);
                Margin childMargin = LayoutInsets.toMargin(child.margin());
                double slotWidth = slotWidths[index];
                double childInnerWidth = Math.max(0.0, slotWidth - childMargin.horizontal());

                // Each column places against its own cursor, forked from the
                // flow's entry position. Fragments and placed nodes still go to
                // the shared sinks in column order, so a column's decoration
                // stays behind its own children.
                CompilerState columnState = state.forkAtCurrentPosition();
                PreparedNode<DocumentNode> childPrepared =
                        host.prepareForRegionWidth(prepareContext, child, childInnerWidth);
                host.compileNode(
                        childPrepared,
                        path,
                        index,
                        depth + 1,
                        cursorX + childMargin.left(),
                        childInnerWidth,
                        columnState,
                        prepareContext,
                        fragmentContext,
                        nodes,
                        fragments);

                if (columnState.pageIndex > endPage) {
                    endPage = columnState.pageIndex;
                    endUsed = columnState.usedHeight;
                } else if (columnState.pageIndex == endPage) {
                    endUsed = Math.max(endUsed, columnState.usedHeight);
                }
                maxTouched = Math.max(maxTouched, columnState.maxTouchedPage);

                cursorX += slotWidth + layoutSpec.spacing();
            }
        }

        state.rejoinAt(endPage, endUsed, maxTouched);
        state.closeBottomSpace(padding.bottom() + margin.bottom());

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
                state.pageIndex,
                naturalMeasure.width(),
                naturalMeasure.height(),
                margin,
                padding));
    }
}
