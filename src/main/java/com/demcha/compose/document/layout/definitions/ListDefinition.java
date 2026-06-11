package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.ListNode;

import java.util.List;

import static com.demcha.compose.document.layout.TextFlowSupport.*;

/**
 * Layout definition for {@link ListNode}: lays out list items as a column of
 * paragraph fragments and emits a per-item fragment so individual items can
 * paginate independently. Delegates the heavy lifting to
 * {@link com.demcha.compose.document.layout.TextFlowSupport}.
 *
 * @author Artem Demchyshyn
 */
public final class ListDefinition implements NodeDefinition<ListNode> {

    /**
     * Creates the list layout definition.
     */
    public ListDefinition() {
    }

    @Override
    public Class<ListNode> nodeType() {
        return ListNode.class;
    }

    @Override
    public PreparedNode<ListNode> prepare(ListNode node, PrepareContext ctx, BoxConstraints constraints) {
        return prepareList(node, ctx, constraints);
    }

    @Override
    public PaginationPolicy paginationPolicy(ListNode node) {
        return PaginationPolicy.SPLITTABLE;
    }

    @Override
    public PreparedSplitResult<ListNode> split(PreparedNode<ListNode> prepared, SplitRequest request) {
        return splitList(prepared, request);
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ListNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return emitListFragments(prepared, placement);
    }
}
