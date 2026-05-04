package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.emitDecorationFragment;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.measureRow;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toPadding;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toSideBorders;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toStroke;

/**
 * Layout definition for {@link RowNode}: a horizontal atomic composite whose
 * children are measured in weighted slots.
 *
 * @author Artem Demchyshyn
 */
public final class RowDefinition implements NodeDefinition<RowNode> {
    @Override
    public Class<RowNode> nodeType() {
        return RowNode.class;
    }

    @Override
    public PreparedNode<RowNode> prepare(RowNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.composite(
                node,
                measureRow(node, toPadding(node.padding()), ctx, constraints),
                new CompositeLayoutSpec(node.gap(), CompositeLayoutSpec.Axis.HORIZONTAL, node.weights()));
    }

    @Override
    public PaginationPolicy paginationPolicy(RowNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(RowNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<RowNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        RowNode node = prepared.node();
        return emitDecorationFragment(
                node.fillColor() == null ? null : node.fillColor().color(),
                toStroke(node.stroke()),
                node.cornerRadius(),
                toSideBorders(node.borders()),
                placement);
    }
}
