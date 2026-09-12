package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.HorizontalBandContentNode;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

/**
 * Layout definition for {@link HorizontalBandContentNode}: lays the child out unchanged, in
 * whatever width it is handed.
 *
 * <p>The band is not read here. By the time this runs the compiler has already narrowed the
 * region to the published column, so measuring against the width it was given is measuring
 * against the band — which is what makes wrapping, height and pagination all follow from one
 * number rather than from a second copy of it.</p>
 *
 * <p>Vertical, and therefore splittable: that is the entire reason this node exists rather
 * than putting the content in the row.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class HorizontalBandContentDefinition implements NodeDefinition<HorizontalBandContentNode> {

    /**
     * Creates the band-content layout definition.
     */
    public HorizontalBandContentDefinition() {
    }

    @Override
    public Class<HorizontalBandContentNode> nodeType() {
        return HorizontalBandContentNode.class;
    }

    @Override
    public PreparedNode<HorizontalBandContentNode> prepare(HorizontalBandContentNode node, PrepareContext ctx,
                                                           BoxConstraints constraints) {
        DocumentNode child = node.child();
        double childInner = Math.max(0.0, constraints.availableWidth() - child.margin().horizontal());
        PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
        double height = childPrepared.measureResult().height() + child.margin().vertical();
        // Fills the band rather than shrinking to the child. The band is the claim — content
        // laid out in a column occupies that column — and a wrapper that reported its child's
        // width instead would make the geometry unreadable from the outside: two different
        // bands holding the same short line would look identical.
        return PreparedNode.composite(node, new MeasureResult(constraints.availableWidth(), height),
                new CompositeLayoutSpec(0.0, CompositeLayoutSpec.Axis.VERTICAL));
    }

    @Override
    public PaginationPolicy paginationPolicy(HorizontalBandContentNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(HorizontalBandContentNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<HorizontalBandContentNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        return List.of();
    }
}
