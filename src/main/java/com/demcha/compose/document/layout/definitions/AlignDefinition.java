package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.PreparedStackLayout;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;

import java.util.List;

/**
 * Layout definition for {@link AlignNode}: a wrapper that fills the available
 * content width and seats its single child left / centre / right. It reuses
 * the stack placement machinery (one layer, one anchor) — the only difference
 * from a {@code LayerStackNode} is that the box is measured to the full
 * available width instead of shrink-wrapping to the child, which is exactly
 * what makes horizontal alignment visible against the page.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class AlignDefinition implements NodeDefinition<AlignNode> {

    /**
     * Creates the align layout definition.
     */
    public AlignDefinition() {
    }

    @Override
    public Class<AlignNode> nodeType() {
        return AlignNode.class;
    }

    @Override
    public PreparedNode<AlignNode> prepare(AlignNode node, PrepareContext ctx, BoxConstraints constraints) {
        DocumentNode child = node.child();
        double childInner = Math.max(0.0, constraints.availableWidth() - child.margin().horizontal());
        PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
        double height = childPrepared.measureResult().height() + child.margin().vertical();
        // Fill the width (so the anchor has room to centre / right-align);
        // height tracks the child so the wrapper adds no vertical space.
        return PreparedNode.composite(
                node,
                new MeasureResult(constraints.availableWidth(), height),
                new PreparedStackLayout(
                        List.of(toAnchor(node.align())),
                        List.of(0.0),
                        List.of(0.0),
                        List.of(0)),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.STACK));
    }

    /**
     * Maps a block alignment to the top-seated stack anchor. The wrapper's box
     * height equals the child's, so the vertical band of the anchor is moot;
     * only the horizontal placement matters.
     */
    private static LayerAlign toAnchor(HorizontalAlign align) {
        return switch (align) {
            case LEFT -> LayerAlign.TOP_LEFT;
            case CENTER -> LayerAlign.TOP_CENTER;
            case RIGHT -> LayerAlign.TOP_RIGHT;
        };
    }

    @Override
    public PaginationPolicy paginationPolicy(AlignNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(AlignNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<AlignNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return List.of();
    }
}
