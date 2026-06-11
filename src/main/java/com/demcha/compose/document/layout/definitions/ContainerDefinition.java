package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

/**
 * Layout definition for {@link ContainerNode}: a vertical atomic composite
 * that may emit a decoration behind its children.
 *
 * @author Artem Demchyshyn
 */
public final class ContainerDefinition implements NodeDefinition<ContainerNode> {

    /**
     * Creates the container layout definition.
     */
    public ContainerDefinition() {
    }

    @Override
    public Class<ContainerNode> nodeType() {
        return ContainerNode.class;
    }

    @Override
    public PreparedNode<ContainerNode> prepare(ContainerNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.composite(
                node,
                measureComposite(node.children(), node.spacing(), toPadding(node.padding()), ctx, constraints),
                new CompositeLayoutSpec(node.spacing()));
    }

    @Override
    public PaginationPolicy paginationPolicy(ContainerNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<DocumentNode> children(ContainerNode node) {
        return node.children();
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ContainerNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        ContainerNode node = prepared.node();
        return emitDecorationFragment(
                node.fillColor() == null ? null : node.fillColor().color(),
                toStroke(node.stroke()),
                node.cornerRadius(),
                toSideBorders(node.borders()),
                placement);
    }
}
