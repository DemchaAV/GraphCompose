package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.ColumnFlowNode;
import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

/**
 * Layout definition for {@link ColumnFlowNode}: columns measured in weighted
 * slots, each flowing down its own column across pages.
 *
 * @author Artem Demchyshyn
 */
public final class ColumnFlowDefinition implements NodeDefinition<ColumnFlowNode> {

    /**
     * Creates the column-flow layout definition.
     */
    public ColumnFlowDefinition() {
    }

    @Override
    public Class<ColumnFlowNode> nodeType() {
        return ColumnFlowNode.class;
    }

    @Override
    public PreparedNode<ColumnFlowNode> prepare(ColumnFlowNode node, PrepareContext ctx,
                                                BoxConstraints constraints) {
        return PreparedNode.composite(
                node,
                measureColumnFlow(node, toPadding(node.padding()), ctx, constraints),
                new CompositeLayoutSpec(node.gap(), CompositeLayoutSpec.Axis.COLUMN_FLOW,
                        node.weights()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code ATOMIC} like every other composite. The compiler dispatches a
     * composite on its axis before it ever reads this policy
     * ({@code LayoutCompiler.compileNode}), so the value is inert here exactly
     * as it is for {@code SectionNode} — which also spans pages. Splitting is
     * a leaf contract; a composite spans pages by placing its children one at
     * a time, which is what this node does per column.</p>
     */
    @Override
    public PaginationPolicy paginationPolicy(ColumnFlowNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(ColumnFlowNode node) {
        return node.children();
    }

    /**
     * No decoration of its own. A column that wants a panel is a section with
     * a fill, which the engine already repeats on every page that section
     * spans; chrome that must reach the page edge belongs in a page
     * background. Keeping the flow itself undecorated is what avoids the
     * question of what a rounded corner means halfway down a document.
     */
    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ColumnFlowNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return List.of();
    }
}
