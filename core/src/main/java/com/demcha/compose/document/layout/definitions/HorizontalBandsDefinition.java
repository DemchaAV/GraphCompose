package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.HorizontalBandsNode;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

/**
 * Layout definition for {@link HorizontalBandsNode}: lays the row out unchanged.
 *
 * <p>The wrapper contributes nothing of its own — no size, no spacing, no fragment. It
 * measures to the row and passes the available width straight through, so a row inside one
 * is laid out exactly as the same row without one. The publication happens in the compiler,
 * where the row's slots are resolved; there is nowhere else it could happen, because the
 * arithmetic that produces them lives there and nowhere else.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class HorizontalBandsDefinition implements NodeDefinition<HorizontalBandsNode> {

    /**
     * Creates the horizontal-bands layout definition.
     */
    public HorizontalBandsDefinition() {
    }

    @Override
    public Class<HorizontalBandsNode> nodeType() {
        return HorizontalBandsNode.class;
    }

    @Override
    public PreparedNode<HorizontalBandsNode> prepare(HorizontalBandsNode node, PrepareContext ctx,
                                                     BoxConstraints constraints) {
        DocumentNode child = node.child();
        double childInner = Math.max(0.0, constraints.availableWidth() - child.margin().horizontal());
        PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
        double width = childPrepared.measureResult().width() + child.margin().horizontal();
        double height = childPrepared.measureResult().height() + child.margin().vertical();
        return PreparedNode.composite(node, new MeasureResult(width, height),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.VERTICAL));
    }

    @Override
    public PaginationPolicy paginationPolicy(HorizontalBandsNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(HorizontalBandsNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<HorizontalBandsNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return List.of();
    }
}
