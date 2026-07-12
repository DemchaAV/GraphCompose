package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.PageBreakNode;

import java.util.List;

/**
 * Layout definition for {@link PageBreakNode}: a zero-size atomic node whose
 * sole purpose is to mark the boundary the compiler must respect when
 * advancing pages. Emits no fragments; the page advance happens because
 * pagination policy is {@link PaginationPolicy#ATOMIC} and the next leaf is
 * placed on a fresh page.
 *
 * @author Artem Demchyshyn
 */
public final class PageBreakDefinition implements NodeDefinition<PageBreakNode> {

    /**
     * Creates the page-break layout definition.
     */
    public PageBreakDefinition() {
    }

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
