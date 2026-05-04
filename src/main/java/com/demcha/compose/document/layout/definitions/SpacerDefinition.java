package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.SpacerNode;

import java.util.List;

/**
 * Layout definition for {@link SpacerNode}: an invisible atomic block whose
 * intrinsic size is the spacer's declared width and height plus its padding.
 * Emits no fragments — the spacer occupies space in the placement graph but
 * has no visual representation.
 */
public final class SpacerDefinition implements NodeDefinition<SpacerNode> {

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
    public List<LayoutFragment> emitFragments(PreparedNode<SpacerNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return List.of();
    }
}
